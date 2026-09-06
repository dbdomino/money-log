# Implementation Plan: 지출유형별 목표금액과 월별 통계

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-backend-target-statistics/spec.md`

## Summary

Phase 5 목표금액·통계 API 6건(5.1~5.6)을 `money-backend-app`에 올린다.
`001`~`005`가 전부 선행이다 — 통계가 집계하는 대상이 `004`(지출·소득)와 `005`(고정지출)이고,
목표금액이 붙는 대상이 `003`(지출유형)이다. **백엔드 Phase의 마지막 기능**이다.

앞의 기능들과 갈리는 축이 셋이다.

| 축 | 002~005 | 006 |
|---|---|---|
| 쓰기의 성격 | 사용자 입력을 저장 | **계산 결과를 저장** |
| 참조 무결성 | FK로 묶는다 | **통계 상세 2종은 FK가 없다** |
| 조회 결과 | 항상 현재 상태 | **저장본 vs 즉석 계산 두 갈래** |

이 셋이 이 기능의 전부다.

**① 스냅샷** — 통계는 저장 시점의 값을 굳힌다. 저장 후 원본 지출을 고쳐도 저장본은
변하지 않는다(FR-518). 그래서 조회가 "저장본이 있으면 그것, 없으면 즉석 계산"으로 갈리고,
`view=live`가 그 분기를 강제로 넘기는 스위치가 된다.

**② FK 없는 참조** — `tbl_statistics_expend_group`·`tbl_statistics_payment_method`의
지출유형·수단 참조에 **FK가 없다**(덤프 확인 — 두 테이블의 FK는 `statistics_idx`와 `id_key`뿐).
원본이 사라져도 기록이 남아야 하기 때문이고, 화면 복원은 **함께 저장한 이름 스냅샷**이 맡는다.

**③ 목표금액 2층** — 기본과 월별이 독립이다. 적용 금액은 `월별 값 ?? 기본 값`인데,
**두 필드의 미설정 표현이 의도적으로 다르다** — 월별은 `null`, 기본은 `0`이다.

기술 결정은 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Spring Data JPA · Spring AOP ·
Bean Validation · Lombok · MapStruct 1.6.3 · PostgreSQL JDBC.
`002`~`005`가 추가한 것을 그대로 쓴다. **이 기능이 새로 추가하는 의존성은 없다.**

**Storage**: PostgreSQL 18 · 스키마 `moneylog`.
쓰는 테이블 **6개** — `tbl_expend_target_default`, `tbl_expend_target_monthly`,
`tbl_statistics`, `tbl_statistics_weekly`, `tbl_statistics_expend_group`,
`tbl_statistics_payment_method`.
읽기만 하는 테이블 **5개** — `tbl_expense`·`tbl_income`·`tbl_fixed_expense_monthly`(집계 원본),
`tbl_user_expend_group`·`tbl_user_payment_method`(이름·사용 여부).
**스키마 변경 없음.** 15개 테이블 중 6개를 이 기능이 처음으로 쓴다.

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test`.
**저장본 불변 검증이 핵심이다** — 저장 → 원본 수정 → 재조회로 값이 그대로인지 본다.

**Target Platform**: JVM 서버. 백엔드 `:8081`, `/api/v1/*`.

**Project Type**: Spring Boot 멀티모듈 웹 서비스(백엔드 API).

**Performance Goals**: 정하지 않는다. 다만 **성격은 기록한다** — 5.5의 즉석 계산은
한 회원의 한 달치 지출·소득·고정지출을 전부 읽어 집계한다. 규모 상한이 `005`의 4.8과 같다.

**Constraints**:
- 6건 전부 `{ resCode, data }`. **래퍼 예외가 없다** — `005`와 같다.
- 목표금액 저장은 **upsert(`PATCH`)**. 최초 설정과 변경을 같은 경로로 처리한다(FR-512).
- `PUT` 금지. `POST`는 5.6 하나뿐이다.
- 페이징은 **5.1만**(FR-526). 통계 응답의 배열 3종은 목록 API가 아니다.
- 연 `2000~2100` · 월 `1~12`. 어긋나면 `3603`(FR-525).
- 저장본은 **불변**(FR-518). `view=live`도 저장본을 읽지도 쓰지도 않는다(FR-514).
- 통계 상세의 지출유형·수단 참조에 **FK를 두지 않는다**(FR-519).
- 합계·비율 6값이 전부 NOT NULL — 빈 달도 0으로 채워 저장한다(FR-528).

**Scale/Scope**: API 6건 · FR 29건(FR-501~528, FR-521a 포함) · SC 11건 · 에러코드 7개 ·
쓰는 테이블 6개 + 읽는 테이블 5개. User Story 3개(P1 2개 · P2 1개).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. `money-app`은 건드리지 않는다. 새 클래스는
      `money-backend-app`과 `data-mod`(Repository 메서드)에 들어간다.
- [x] **II. 레이어 흐름** — PASS. `Controller → Service → Repository`. Entity는 경계를 넘지 않는다.
      **집계 계산을 Service에 직접 두지 않고 `StatisticsCalculator`로 분리**한다 —
      즉석 계산(5.5)과 저장(5.6)이 **같은 계산기를 써야** 두 경로의 값이 갈리지 않는다.
- [x] **III. 응답 규격** — PASS. 6건 전부 `{ resCode, data }`. **래퍼 예외가 없다.**
      Complexity Tracking에 적을 예외가 없다. `PUT` 0건.
- [x] **IV. 로깅** — PASS. `002`의 AOP 로깅을 그대로 쓴다. 바이너리 응답도 민감정보도 없어
      추가 규칙이 없다.
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `phase5-목표-통계/` 6건이 있고 커밋 `84ad88c`에서
      개정됐다. **착수 전 개정 2건**이 남아 있다 → 아래 참고.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql`로 확인했고 **스키마 변경 없음**이다.
      6개 테이블·CHECK 5건·유니크 6건·CASCADE FK 3건이 이미 있다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 `common-mod` 추가분은 `ErrorCode` 상수 4개뿐이다. 역방향 의존 0건.
- [x] **II** — 계산을 `StatisticsCalculator` 한 곳에 두어 5.5·5.6이 공유한다.
      Controller는 Service만 부른다.
- [x] **III** — 6건의 성공·실패 응답을 코드까지 적었다. 예외 없이 전부 래퍼다.
- [x] **IV** — 추가 규칙 없음.
- [x] **V** — 선행 개정 2건을 quickstart의 착수 전 절차로 넣었다.
- [x] **VI** — 스키마 무변경. 완료 판정에 덤프 diff 확인을 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `5.6-StatisticsMonthlySave.md` 실패 표 | `3604` 설명의 **"(정책에 따라)"** 삭제, "현재 연월 초과"로 확정 | FR-527. clarify에서 확정했다 |
| 2 | `5.5-StatisticsMonthlyGet.md` | 수단별 요약의 **모집단**을 명시 | FR-521a. 예시에 0원 행이 있지만 어느 수단까지 넣는지 규칙이 없다 |

2번이 특히 필요하다. 그대로 두면 구현자가 "회원 소유 전부"로 읽어 **버린 카드의 0원 행이
매달 쌓인다.** clarify에서 정한 규칙은 "그 달 지출이 있는 수단은 상태 무관 전부 +
0원 행은 저장 시점 사용 중인 `EXPENSE` 수단만"이다.

## Project Structure

### Documentation (this feature)

```text
specs/006-backend-target-statistics/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 10건
├── data-model.md        # Phase 1 — 6개 테이블과 스냅샷 규칙
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-contract.md          # API 6건의 규칙과 실패 코드 매핑
│   ├── target-amount.md         # 목표금액 2층 구조와 null vs 0
│   └── statistics-snapshot.md   # 집계 규칙·저장본 불변·view 분기
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물
```

### Source Code (repository root)

`+`는 신규, `~`는 수정이다.

```text
common-mod/src/main/java/com/dbdomino/moneylog/common/
└── error/ErrorCode.java                          ~ 36xx 코드 4개 추가 (3601·3602·3603·3604)

app-mod/money-backend-app/src/main/
├── java/com/dbdomino/moneylog/backend/
│   ├── controller/
│   │   ├── ExpendTargetController.java           + 5.1~5.4
│   │   └── StatisticsController.java             + 5.5·5.6
│   ├── service/
│   │   ├── ExpendTargetService.java              + 목표금액 4건 (upsert 포함)
│   │   ├── StatisticsQueryService.java           + 5.5 (저장본 / 즉석 분기)
│   │   ├── StatisticsSaveService.java            + 5.6 (재저장 = 상세 삭제 후 삽입)
│   │   └── statistics/
│   │       ├── StatisticsCalculator.java         + 집계 계산 — 5.5·5.6 이 공유한다
│   │       ├── WeekBoundaryResolver.java         + 월요일 시작 주 경계
│   │       └── TargetResolver.java               + 적용 금액(월별 ?? 기본) 판정
│   ├── support/
│   │   └── YearMonthValue.java                   ~ 005 가 만든 값 객체를 재사용
│   ├── dto/
│   │   ├── request/                              + 목표금액 upsert·통계 저장 Request DTO
│   │   └── response/                             + ExpendTargetDto·StatisticsDto 등
│   └── mapper/
│       ├── ExpendTargetMapper.java               + Entity ↔ DTO
│       └── StatisticsMapper.java                 + Entity ↔ DTO
└── resources/application.yml                     (변경 없음)

data-mod/src/main/java/com/dbdomino/moneylog/data/repository/
├── UserExpendTargetDefaultRepository.java        ~ 회원·유형 조회 (upsert 용)
├── UserExpendTargetMonthlyRepository.java        ~ 회원·연월·유형 조회
├── UserStatisticsRepository.java                 ~ 회원·연월 조회
├── UserStatisticsWeeklyRepository.java           ~ 통계별 삭제·삽입
├── UserStatisticsExpendGroupRepository.java      ~ 통계별 삭제·삽입
├── UserStatisticsPaymentMethodRepository.java    ~ 통계별 삭제·삽입
├── UserExpenseRepository.java                    ~ 월별 집계 조회 (읽기만)
├── UserIncomeRepository.java                     ~ 월별 집계 조회 (읽기만)
└── UserFixedExpenseMonthlyRepository.java        ~ 월별 집계 조회 (읽기만)

app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/
├── target/         + 목표금액 2층 (US1)
└── statistics/     + 조회 분기·저장·불변 (US2·US3)
```

**Structure Decision**: 기존 구조를 그대로 쓴다. 새 모듈은 만들지 않는다.

006이 새로 만드는 구조는 **`statistics` 하위 패키지** 하나다.

- **`StatisticsCalculator`** — 이 기능에서 가장 중요한 분리다. 5.5(즉석 계산)와
  5.6(저장)이 **같은 계산기를 써야** 한다. 각자 구현하면 "지금 보이는 값"과 "저장된 값"이
  달라지는데, 그게 바로 이 기능이 방지하려는 상황이다.
- **`WeekBoundaryResolver`** — 주 경계 계산(월요일 시작, 1일이 월요일이 아니면 첫 주는
  1일부터 첫 일요일까지, 마지막 주는 말일에서 끊음)이 즉석 계산과 저장본 읽기 양쪽에 걸린다.
  저장본은 경계를 저장해 두고 그대로 쓰지만(FR-520), 즉석 계산은 매번 만든다.
- **`TargetResolver`** — 적용 금액(`월별 ?? 기본`) 판정이 5.1·5.2·5.5 세 곳에 필요하다.
  `null`과 `0`의 비대칭을 한 곳에 가둔다.

`YearMonthValue`는 `005`가 만든 것을 재사용한다. 연·월 범위 검증(FR-525)과 미래 월 판정
(FR-527)이 같은 합성 비교를 쓴다.

`core-mod`는 이번에도 건드리지 않는다.

## Complexity Tracking

> 헌장 원칙 위반은 없다. 다만 **DB 설계의 통념과 어긋나는 결정 1건**을 기록한다.

| 항목 | 통상적인 기대 | 이 기능이 하는 것 | 이유 |
|---|---|---|---|
| 통계 상세의 지출유형·수단 참조 | FK로 무결성을 보장한다 | **FK를 두지 않는다.** ID는 값으로만 저장하고 이름 스냅샷을 함께 남긴다 | FR-519. 001의 결정이며 덤프로 확인했다 |

`001`이 이미 스키마로 확정한 것이라 006이 새로 만드는 위반이 아니다. 다만 구현자가
"FK가 빠진 실수"로 오해해 추가하려 들 수 있어 근거를 남긴다.

**왜 FK가 없나**: 통계는 **그 달에 일어난 일의 기록**이다. 나중에 지출유형을 삭제 표시하거나
수단을 정리해도 과거 통계는 남아야 한다. FK RESTRICT면 원본 삭제가 막히고,
FK CASCADE면 과거 통계가 함께 사라진다. 둘 다 통계의 목적과 어긋난다.

**대가**: 응답의 유형·수단 ID가 **실재하지 않을 수 있다.** 화면 복원은 함께 저장한
`expend_group_name`·`payment_method_name`이 맡는다. 두 컬럼이 NOT NULL인 이유가 이것이다.

덤프에서 확인한 사실: `tbl_statistics_expend_group`의 FK는
`fk_stat_group_statistics`(CASCADE)와 `fk_stat_group_user` 둘뿐이고
`expend_group_idx`로 나가는 FK가 없다. `tbl_statistics_payment_method`도 같다.
**SC-509가 이걸 테스트로 고정한다.**

기각한 대안 2가지.

- **FK를 추가하고 원본을 물리 삭제하지 않는 것으로 방어** — `003`이 이미 삭제 표시를 쓰므로
  당장은 성립한다. 그러나 FK가 있으면 "언젠가 원본을 정리한다"는 선택지가 영영 막힌다.
  통계는 원본과 수명이 다른 기록이다.
- **이름 스냅샷 없이 ID만 저장하고 조회 시 조인** — 원본이 사라지면 이름을 복원할 수 없다.
  `004`의 지출이 스냅샷을 두는 것과 같은 이유다.
