# Implementation Plan: 고정지출 관리와 월별 가계부 목록

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-backend-ledger-fixed-expense/spec.md`

## Summary

Phase 4 고정지출·가계부 API 9건(4.1~4.9)을 `money-backend-app`에 올린다.
`001`(저장 구조) · `002`(인증·기반) · `003`(수단·지출유형) · `004`(지출·소득)이 선행이다.

**앞의 기능들과 성격이 근본적으로 다르다.** 002~004는 "요청이 오면 저장하고 돌려준다"였는데,
005는 **조회가 쓰기를 일으킨다.**

| 축 | 002~004 | 005 |
|---|---|---|
| GET의 부작용 | 없다 | **있다** — 4.5·4.8이 월별 내역을 생성·저장한다 |
| 저장 단위 | 요청 1건 = 행 1건(또는 N건) | **설정과 그 달의 값이 분리** |
| 목록 | 단일 테이블 | **4개 출처를 합쳐 한 목록으로** |

이 세 가지가 이 기능의 전부다.

**① lazy 생성** — 월별 내역은 그 달을 **처음 조회할 때** 설정에서 복사돼 만들어진다(FR-406).
등록 시점에 적용 기간 전체를 미리 만들지 않는다(FR-402). GET이 쓰기를 하므로
**동시 요청 경합**이 실재하고(SC-403), DB 유니크 제약이 최종 방어선이다.

**② 두 층의 분리** — 설정(`tbl_fixed_expense`)과 그 달의 값(`tbl_fixed_expense_monthly`)이
따로 있다. 설정을 고쳤을 때 어느 달까지 따라가는가가 규칙이 되고(FR-412),
`modified` 플래그가 "사용자가 직접 손댄 달"을 표시해 자동 반영에서 제외한다.

**③ 통합 목록** — 4.8이 일반 지출·할부·소득·고정지출 네 출처를 한 응답으로 합친다.
저장 단위가 아니라 **조회 시점에 조립되는 형태**라 정렬·페이징·식별자 규칙을 새로 정해야 한다.

기술 결정은 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Spring Data JPA · Spring AOP ·
Bean Validation · Lombok · MapStruct 1.6.3 · PostgreSQL JDBC.
`002`의 Spring Security·jjwt, `004`의 POI를 그대로 쓴다.
**이 기능이 새로 추가하는 의존성은 없다.**

**Storage**: PostgreSQL 18 · 스키마 `moneylog`.
쓰는 테이블 **2개** — `tbl_fixed_expense`, `tbl_fixed_expense_monthly`.
읽기만 하는 테이블 **4개** — `tbl_expense`·`tbl_income`(가계부 목록),
`tbl_user_payment_method`·`tbl_user_expend_group`(검증·현재 이름).
**스키마 변경 없음.**

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test`.
**동시성 테스트가 필요하다**(SC-403) — 같은 달을 두 스레드가 동시에 처음 여는 시나리오다.
`data-mod`에 이미 `FixedExpenseMonthlyConcurrencyIT`가 있으므로 그 방식을 참고한다.

**Target Platform**: JVM 서버. 백엔드 `:8081`, `/api/v1/*`.

**Project Type**: Spring Boot 멀티모듈 웹 서비스(백엔드 API).

**Performance Goals**: 정하지 않는다. 다만 **4.8의 성격은 기록한다** — 한 달치를 전부
돌려주고 페이징이 없으며(FR-422), 4개 출처를 합친 뒤 메모리에서 정렬한다. 한 회원의
한 달 거래 건수가 규모의 상한이다.

**Constraints**:
- 9건 전부 `{ resCode, data }`. **래퍼 예외가 없다** — 003·004와 다르다.
- 삭제는 **물리 삭제**(FR-416). 004와 같다.
- 이름은 **저장하지 않는다** — 고정지출 계열은 어디서도 스냅샷을 두지 않는다(FR-405·425).
  004의 지출·소득과 정반대다.
- `PUT` 금지. 4.4·4.6은 `PATCH`. 4.9는 `POST`이고 **연·월을 Body로 받는다**(FR-421).
- 페이징은 **4.2만**. 4.5·4.8·4.9는 두지 않는다(FR-422).
- 연·월 비교는 전부 `연 × 12 + 월` 합성값(FR-404·412).
- 결제일 말일 보정은 **생성 시 한 번**만. 조회 때 다시 계산하지 않는다(FR-409).

**Scale/Scope**: API 9건 · FR 25건(FR-401~425) · SC 9건 · 에러코드 9개 ·
쓰는 테이블 2개 + 읽는 테이블 4개. User Story 5개(P1 3개 · P2 2개).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. `money-app`은 건드리지 않는다. 새 클래스는
      `money-backend-app`과 `data-mod`(Repository 메서드)에 들어간다.
- [x] **II. 레이어 흐름** — PASS. `Controller → Service → Repository`. Entity는 경계를 넘지 않는다.
      **4개 출처를 합치는 조립을 Controller가 아니라 Service에 둔다** — Controller가
      네 Repository를 직접 부르면 원칙 II 위반이다.
- [x] **III. 응답 규격** — PASS. 9건 전부 `{ resCode, data }`. **래퍼 예외가 하나도 없다.**
      003(아이콘)·004(엑셀 양식)와 달리 Complexity Tracking에 적을 예외가 없다. `PUT` 0건.
- [x] **IV. 로깅** — PASS. `002`의 AOP 로깅을 그대로 쓴다. **바이너리 응답이 없어**
      003·004처럼 로깅 제외 규칙을 추가할 것도 없다.
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `phase4-가계부/` 9건이 있고 커밋 `84ad88c`에서
      개정됐다. **착수 전 개정 1건**이 남아 있다 → 아래 참고.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql`로 확인했고 **스키마 변경 없음**이다.
      두 테이블·CHECK 7건·유니크 1건·CASCADE FK 1건·인덱스 2건이 이미 있다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 `common-mod` 추가분은 `ErrorCode` 상수 5개뿐이다. 역방향 의존 0건.
- [x] **II** — 4.8의 조립을 `LedgerAssembler`(Service 계층)에 두었다.
      Controller는 `LedgerService` 하나만 부른다.
- [x] **III** — 9건의 성공·실패 응답을 코드까지 적었다. 예외 없이 전부 래퍼다.
- [x] **IV** — 추가 규칙 없음. 002의 설정을 그대로 쓴다.
- [x] **V** — 선행 개정 1건을 quickstart의 착수 전 절차로 넣었다.
- [x] **VI** — 스키마 무변경. 완료 판정에 덤프 diff 확인을 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `4.1-FixedExpenseCreate.md` · `4.3-FixedExpenseGet.md` | 응답 설명의 **"스냅샷"** → **"현재 이름"** | FR-405. `tbl_fixed_expense`에 이름 컬럼이 없고(덤프 확인) 4.5·4.8은 이미 "현재 이름(스냅샷 아님)"으로 적혀 있다. 두 문서만 표기가 어긋난다 |

이 개정이 중요한 이유는 그대로 두면 **구현자가 `001` 스키마에 이름 컬럼 2개를 추가하려 들기
때문**이다. 그건 스키마 변경이고 이 기능의 전제(무변경)를 깬다.

## Project Structure

### Documentation (this feature)

```text
specs/005-backend-ledger-fixed-expense/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 10건
├── data-model.md        # Phase 1 — 두 층의 분리와 lazy 생성
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-contract.md          # API 9건의 규칙과 실패 코드 매핑
│   ├── monthly-lifecycle.md     # lazy 생성·설정 반영·재작성
│   └── ledger-list.md           # 4.8 통합 목록의 조립 규칙
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물
```

### Source Code (repository root)

`+`는 신규, `~`는 수정이다.

```text
common-mod/src/main/java/com/dbdomino/moneylog/common/
└── error/ErrorCode.java                          ~ 34xx·35xx 코드 5개 추가

app-mod/money-backend-app/src/main/
├── java/com/dbdomino/moneylog/backend/
│   ├── controller/
│   │   ├── FixedExpenseController.java           + 4.1~4.4·4.7
│   │   ├── FixedExpenseMonthlyController.java    + 4.5·4.6·4.9
│   │   └── LedgerController.java                 + 4.8
│   ├── service/
│   │   ├── FixedExpenseService.java              + 설정 CRUD (4.1~4.4·4.7)
│   │   ├── FixedExpenseMonthlyService.java       + lazy 생성·단건 수정 (4.5·4.6)
│   │   ├── FixedExpenseSyncService.java          + 재작성 4단계 (4.9)
│   │   ├── LedgerService.java                    + 4.8 유스케이스
│   │   └── ledger/
│   │       ├── LedgerAssembler.java              + 4개 출처를 한 목록으로
│   │       └── LedgerItemFactory.java            + type 별 행 변환·ledgerItemId 생성
│   ├── support/
│   │   └── YearMonthValue.java                   + 연×12+월 합성 비교를 가두는 값 객체
│   ├── dto/
│   │   ├── request/                              + 등록·수정·재작성 Request DTO
│   │   └── response/                             + FixedExpenseDto·MonthlyDto·LedgerItemDto
│   └── mapper/
│       ├── FixedExpenseMapper.java               + Entity ↔ DTO (현재 이름 조립)
│       └── FixedExpenseMonthlyMapper.java        + Entity ↔ DTO
└── resources/application.yml                     (변경 없음)

data-mod/src/main/java/com/dbdomino/moneylog/data/repository/
├── UserFixedExpenseRepository.java               ~ 기간 포함 조회·소유자 조회
├── UserFixedExpenseMonthlyRepository.java        ~ 연·월 조회·존재 확인·충돌 무시 삽입
├── UserExpenseRepository.java                    ~ 월별 조회 (4.8용, 읽기만)
└── UserIncomeRepository.java                     ~ 월별 조회 (4.8용, 읽기만)

app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/
├── fixedexpense/    + 설정 CRUD (US1)
├── monthly/         + lazy 생성·동시성·단건 수정 (US2·US3)
├── sync/            + 설정 반영 범위·재작성 (US4)
└── ledger/          + 통합 목록 (US5)
```

**Structure Decision**: 기존 구조를 그대로 쓴다. 새 모듈은 만들지 않는다.

005가 새로 만드는 구조는 셋이다.

- **`ledger` 하위 패키지** — 4.8의 조립은 "네 출처를 읽고, 공통 형태로 바꾸고, 합치고,
  정렬한다"는 네 단계다. `LedgerService`에 다 넣으면 한 메서드가 비대해지고 `type`이
  늘어날 때(예: 나중에 이체) 손댈 곳이 흩어진다. 조립(`LedgerAssembler`)과
  행 변환(`LedgerItemFactory`)을 나눈다.
- **`FixedExpenseSyncService` 분리** — 재작성(4.9)은 생성·갱신·보존·삭제 **네 처리**를
  한 트랜잭션에서 하고 각 건수를 센다. 설정 CRUD와 성격이 달라 서비스를 나눈다.
- **`YearMonthValue`** — `연 × 12 + 월` 합성 비교가 FR-404·408·412·413에 걸쳐 반복된다.
  각자 계산하면 한 곳만 틀려도 해를 넘기는 구간에서 조용히 어긋난다.
  DB CHECK `ck_fixed_expense_period`도 같은 식을 쓰므로 값 객체로 가둬 규칙을 하나로 만든다.

`core-mod`는 이번에도 건드리지 않는다.

## Complexity Tracking

> 헌장 원칙 위반은 없다. 다만 **일반 상식과 어긋나는 설계 결정 1건**을 기록한다.

| 항목 | 통상적인 기대 | 이 기능이 하는 것 | 이유 |
|---|---|---|---|
| GET의 부작용 | GET은 조회다. 상태를 바꾸지 않는다 | **4.5·4.8(GET)이 월별 내역을 생성·저장한다** | FR-406·418. 설계 명세가 정한 lazy 생성 모델이다 |

원칙 III의 "GET 조회"는 **HTTP 메서드 선택 규칙**이지 부작용 금지 규정이 아니므로
위반은 아니다. 그러나 구현자와 리뷰어가 놀랄 지점이라 근거를 남긴다.

**왜 lazy인가**: 대안은 등록 시점에 적용 기간 전체의 월별 내역을 미리 만드는 것인데,
FR-402가 명시적으로 금지한다. 적용 기간이 10년이면 등록 한 번에 120행이 생기고,
그중 사용자가 실제로 여는 달은 몇 개뿐이다. 게다가 설정을 고칠 때마다 미래 120행을
전부 손봐야 한다.

**대가**: GET이 쓰기를 하므로 동시 요청 경합이 실재한다. 같은 달을 두 요청이 동시에 처음
열면 둘 다 "없음"을 보고 삽입을 시도한다. DB 유니크 제약
`ux_fixed_expense_monthly (fixed_expense_idx, year, month)`가 1건을 강제하고,
애플리케이션은 **충돌을 무시**해 어느 쪽도 오류로 끝나지 않게 한다(SC-403).
자세한 방식은 [research.md §2](./research.md).

기각한 대안 2가지.

- **등록 시 전 기간 생성** — FR-402가 금지한다. 위 이유.
- **생성을 별도 POST API로 빼고 GET은 순수 조회로 둔다** — 원칙적으로 깔끔하지만
  프론트가 "먼저 생성 API를 부르고 그다음 조회"를 매번 해야 한다. 화면 한 번에 왕복이 둘로
  늘고, 생성 호출을 빠뜨리면 빈 목록이 나온다. 4.9(재작성)가 이미 그 명시적 경로를
  제공하므로 자동 생성까지 없앨 이유가 없다.
