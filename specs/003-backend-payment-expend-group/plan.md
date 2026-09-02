# Implementation Plan: 지출·소득 수단과 지출유형 관리

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-backend-payment-expend-group/spec.md`

## Summary

Phase 2 수단·지출유형 API 13건(2.1~2.13)을 `money-backend-app`에 올린다.
저장 구조는 `001`이, 인증·응답 규격·에러 처리·로깅 기반은 `002`가 만든다.
**이 기능은 순수하게 유스케이스만 얹는다** — 새로 세울 공통 기반이 없다.

002와 성격이 다른 점이 셋이다.

| | 002 (회원·인증) | 003 (수단·지출유형) |
|---|---|---|
| 무게 중심 | 공통 기반 5종 신설 | 유스케이스 13건 |
| 새 의존성 | Spring Security · jjwt | **없음** |
| 응답 규격 | 16건 전부 `{ resCode, data }` | **12건 래퍼 + 1건 바이너리 예외** |

세 번째가 이 기능의 유일한 구조적 특이점이다. 아이콘 조회(2.10)는 프로젝트에서
`{ resCode, data }` 래퍼를 쓰지 않는 **유일한 API**라, 전역 예외 처리·AOP 로깅·응답 조립이
이 하나를 어떻게 비켜 가는지를 계획 단계에서 정해 두어야 한다.

나머지 12건은 CRUD 형태지만 **판정이 애플리케이션에 몰려 있다.** DB가 막아주는 것은
지출유형 이름 유일성(`ux_user_expend_group_name`)과 `type`·`purpose` 값 범위(CHECK 2건)뿐이고,
삭제 표시가 UPDATE라 FK RESTRICT가 삭제를 대신 막아주지 못한다. 참조 0건 검사(`3005`),
사용 이력 검사(`3106`), 기본 유형 보호(`3105`·`3107`), 재삭제 판정(`3004`·`3108`)이 전부
서비스 코드의 책임이다.

기술 결정은 [research.md](./research.md)에 있다. 요지는 **아이콘 파일 I/O를 서비스에서 분리해
`IconStorage` 한 곳에 가두고, 파일 쓰기를 트랜잭션 커밋 뒤로 미룬다**는 것이다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Spring Data JPA · Spring AOP ·
Bean Validation · Lombok · MapStruct 1.6.3 · PostgreSQL JDBC.
`002`가 추가하는 Spring Security와 jjwt를 그대로 쓴다.
**이 기능이 새로 추가하는 의존성은 없다.** 이미지 형식 판정은 JDK의
`javax.imageio.ImageIO`·`Files.probeContentType`으로 충분하다([research.md §3](./research.md)).

**Storage**: PostgreSQL 18 · 스키마 `moneylog`.
쓰는 테이블 **2개** — `tbl_user_payment_method`, `tbl_user_expend_group`.
읽기만 하는 테이블 4개 — `tbl_expense`·`tbl_income`·`tbl_fixed_expense`·`tbl_fixed_expense_monthly`
(참조 0건·사용 이력 검사용).
**스키마 변경 없음.** 필요한 컬럼·제약·인덱스가 001에 전부 있다.

추가로 **서버 로컬 디스크**를 쓴다. 아이콘 파일 저장소이며 DB가 아니다.

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test`.
통합 테스트는 실 PostgreSQL을 쓴다. 아이콘 업로드는 `MockMultipartFile`로 낸다.

**Target Platform**: JVM 서버. 백엔드 `:8081`, `/api/v1/*`.

**Project Type**: Spring Boot 멀티모듈 웹 서비스(백엔드 API).

**Performance Goals**: 정하지 않는다. FR-217이 "본인 보유 수가 제한적이라 페이징을 두지 않는다"고
근거를 적었고, 목록 2건은 `ix_user_payment_method_active`·`ix_user_expend_group_active`가
이미 커버한다. 근거 없는 수치를 적지 않는다.

**Constraints**:
- 12건은 `{ resCode, data }`. **2.10만 예외**로 이미지 바이너리를 직접 돌려준다(FR-216).
- 목록 2건(2.2·2.8)과 사용 중 목록 2건(2.6·2.13)은 `data.list` 하나만 담는다.
  **`totalCount`를 싣지 않는다**(FR-217) — `002`의 관리자 목록과 의도적으로 다르다.
- 삭제는 전부 **삭제 표시**(UPDATE). 물리 삭제 0건.
- `PUT` 금지. 2.4·2.11은 `PATCH`이고 omit한 필드는 유지.
- 2.7·2.11은 `multipart/form-data`다. 나머지는 JSON.
- 아이콘 파일명은 `{id_key}_{expendGroupId}.{확장자}`. 유형 이름을 넣지 않는다(FR-224).
- 아이콘은 `png`·`jpg`·`gif` · 1MB 이하. **형식 판정은 확장자가 아니라 파일 내용으로**(FR-219).

**Scale/Scope**: API 13건 · FR 24건(FR-201~224) · SC 12건 · 에러코드 15개 ·
쓰는 테이블 2개 + 읽는 테이블 4개. User Story 4개를 P1→P3로 자른다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. `money-app`은 건드리지 않는다. 새 클래스는 전부
      `money-backend-app`(Controller·Service·DTO·`IconStorage`)과 `data-mod`(Repository 메서드)에
      들어간다. `common-mod`에 추가할 것은 에러코드 Enum 상수뿐이고 이는 DB를 모른다.
- [x] **II. 레이어 흐름** — PASS. `Controller → Service → Repository`. Entity는 경계를 넘지 않는다.
      **파일 I/O를 Service에 직접 두지 않고 `IconStorage`로 분리**해 Service가 유스케이스만 담게 한다.
- [x] **III. 응답 규격** — PASS(예외 1건 명시). 12건은 `{ resCode, data }`, 2.10은 FR-216이
      **명세에 적어 둔** 예외다. 원칙 III의 "성공·실패 모두 이 형태"를 어기는 것이 아니라
      명세가 승인한 단일 예외이므로 Complexity Tracking에 근거를 기록했다. `PUT` 0건.
- [x] **IV. 로깅** — PASS. `002`가 만든 AOP 로깅을 그대로 쓴다. **2.10의 응답 본문은 로그에
      찍지 않는다**(이미지 바이너리다) — 그 처리를 계약에 명시했다.
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `phase2-수단-지출유형/` 13건이 있고
      커밋 `84ad88c`에서 개정됐다. **착수 전 개정 1건**이 남아 있다 → 아래 참고.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql`로 확인했고 **스키마 변경 없음**이다.
      두 테이블의 컬럼·CHECK 2건·유니크 1건·인덱스 2건이 이미 있다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 `common-mod` 추가분은 `ErrorCode` 상수 11개뿐이다. 역방향 의존 0건.
- [x] **II** — [contracts/](./contracts/)의 13건 모두 Controller가 Service만 부른다.
      파일 I/O는 `IconStorage`에만 있다.
- [x] **III** — 13건의 성공·실패 응답을 코드까지 적었다. 2.10의 예외 처리 경로를
      [contracts/icon-storage.md](./contracts/icon-storage.md)에 따로 정했다.
- [x] **IV** — 2.10을 AOP 로깅의 응답 본문 기록에서 제외하는 규칙을 계약에 넣었다.
- [x] **V** — 선행 개정 1건을 quickstart의 착수 전 절차로 넣었다.
- [x] **VI** — 스키마 무변경. 완료 판정에 `git diff sql/schema-moneylogdb.sql` → 변경 없음을 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 상태 |
|---|---|---|---|
| 1 | `_공통.md` § 코드 블록 배정·대표 코드표 | `3108` 추가 | **커밋 `84ad88c`에서 완료** |
| 2 | `2.12-ExpendGroupDelete.md` 실패 표 | `3108` 추가 | **완료** |
| 3 | `2.8`·`2.9` 응답 필드 표 | `deleted` 추가 | **완료** |
| 4 | `2.10`·`1.2` 아이콘 파일명 | ID 기반 규칙 | **완료** |
| 5 | `2.7`·`2.11` 요청 표 | `iconFile` 파트의 **허용 형식·최대 크기**(png·jpg·gif · 1MB) 명시 | **남아 있다** |

5번만 남았다. clarify에서 규격을 확정했지만(`003` FR-219) 설계 명세의 `2.7`·`2.11` 요청 표는
아직 `iconFile | file | ❌ | 아이콘 이미지 (png, jpg, gif 등)`로 "등"을 열어 둔 상태다.
헌장 원칙 V의 "설명 칸은 그 칸만 보고 의미가 읽혀야 한다"에 걸린다.

## Project Structure

### Documentation (this feature)

```text
specs/003-backend-payment-expend-group/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 9건
├── data-model.md        # Phase 1 — 쓰는 테이블 2개와 상태 전이
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-contract.md      # API 13건의 규칙과 실패 코드 매핑
│   └── icon-storage.md      # 아이콘 저장·조회·래퍼 예외 처리
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물
```

### Source Code (repository root)

`+`는 신규, `~`는 수정이다. **`002`가 만드는 것에 의존하되 고치지 않는다.**

```text
common-mod/src/main/java/com/dbdomino/moneylog/common/
└── error/
    └── ErrorCode.java                          ~ 30xx·31xx 코드 11개 추가

app-mod/money-backend-app/src/main/
├── java/com/dbdomino/moneylog/backend/
│   ├── config/
│   │   └── IconStorageProperties.java          + 저장 디렉터리·최대 크기 바인딩
│   ├── controller/
│   │   ├── PaymentMethodController.java        + 2.1~2.6
│   │   ├── ExpendGroupController.java          + 2.7~2.9·2.11~2.13
│   │   └── ExpendGroupIconController.java      + 2.10 (래퍼를 쓰지 않는 유일한 Controller)
│   ├── service/
│   │   ├── PaymentMethodService.java           + 수단 6건
│   │   ├── ExpendGroupService.java             + 지출유형 6건
│   │   └── ExpendGroupIconService.java         + 아이콘 저장·조회 유스케이스
│   ├── storage/
│   │   ├── IconStorage.java                    + 파일 I/O 를 가두는 유일한 지점
│   │   └── ImageTypeDetector.java              + 내용 기반 형식 판정
│   ├── dto/
│   │   ├── request/                            + 등록·수정 Request DTO
│   │   └── response/                           + PaymentMethodDto·ExpendGroupDto 등
│   └── mapper/
│       ├── PaymentMethodMapper.java            + Entity ↔ DTO
│       └── ExpendGroupMapper.java              + Entity ↔ DTO (iconUrl 조립 포함)
└── resources/
    └── application.yml                         ~ icon.storage.* 프로퍼티 추가

data-mod/src/main/java/com/dbdomino/moneylog/data/repository/
├── UserPaymentMethodRepository.java            ~ 소유자·용도·상태 조회 메서드
├── UserExpendGroupRepository.java              ~ 소유자·이름 유일성·상태 조회 메서드
├── UserExpenseRepository.java                  ~ 참조 존재 검사(exists) 메서드
├── UserIncomeRepository.java                   ~ 참조 존재 검사
├── UserFixedExpenseRepository.java             ~ 참조 존재 검사
└── UserFixedExpenseMonthlyRepository.java      ~ 참조 존재 검사

app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/
├── paymentmethod/  + 수단 관리·사용 중 목록 (US1·US2)
├── expendgroup/    + 지출유형 관리 (US3)
└── icon/           + 아이콘 업로드·조회·래퍼 예외 (US4)
```

**Structure Decision**: 기존 구조를 그대로 쓴다. 새 모듈은 만들지 않는다.

003이 002와 다르게 새로 만드는 구조는 **`storage` 패키지 하나**다.

- `IconStorage` — 파일 읽기·쓰기·존재 확인. **파일 시스템을 만지는 유일한 지점**이다.
  Service가 `Files.write`를 직접 부르면 저장 경로 규칙이 여러 곳으로 흩어지고,
  경로 조립 실수 하나가 곧 경로 탈출 취약점이 된다.
- `ImageTypeDetector` — 업로드 파일의 **내용**으로 형식을 판정한다(FR-219).
  `IconStorage`와 분리한 이유는 판정이 저장과 독립적이기 때문이다 — 판정에 실패하면
  저장 자체를 시작하지 않는다.

`core-mod`는 이번에도 건드리지 않는다. 넣을 만한 "DB 사용 모델의 추상 설계"가 없다.

## Complexity Tracking

> 헌장 원칙 III의 응답 규격에 **명세가 승인한 예외 1건**이 있다. 위반이 아니라 기록이다.

| 항목 | 원칙이 요구하는 것 | 이 기능이 하는 것 | 근거 |
|---|---|---|---|
| 2.10 아이콘 조회 응답 | 성공·실패 모두 `{ resCode, data }` | 성공 시 **이미지 바이너리**를 직접 반환 | FR-216. JSON에 base64를 넣으면 응답이 33% 커지고, 프론트가 `<img>`에 바로 물릴 수 없다. 설계 명세 `2.10`이 처음부터 이 형태로 정의했다 |
| 2.10 인증 실패 응답 | `1001` + 래퍼 | **래퍼 없는 HTTP 401** | 스펙 US4 시나리오 4. 이 API는 4자리 코드를 실을 JSON 본문 자체가 없다 |
| 2.10 파일 없음 | — | `3104` + 래퍼 | 설계 명세 `2.10`이 `3104`를 배정했다. **성공만 바이너리이고 실패는 래퍼**라는 뜻이다 |

세 줄이 함께 있어야 의미가 완결된다. **2.10은 "성공 = 바이너리 / 비즈니스 실패 = 래퍼 /
인증 실패 = 래퍼 없는 401"이라는 세 갈래 응답을 갖는다.** 프로젝트에서 유일하다.

기각한 대안 2가지.

- **아이콘도 base64로 감싸 규격을 통일한다** — 원칙 III에 예외가 사라져 깔끔해 보이지만,
  응답 크기가 커지고 브라우저 캐시를 못 쓴다. 목록 화면이 유형 10개의 아이콘을 한꺼번에
  받으면 그 비용이 매번 발생한다. 설계 명세가 이미 반대 방향으로 확정했다.
- **아이콘을 정적 리소스로 열어 인증을 빼고 규격 논의 자체를 없앤다** — 회원별 아이콘이라
  URL을 아는 사람이 남의 파일을 받을 수 있게 된다. FR-216이 "인증은 다른 API와 같이 요구한다"로
  못박은 이유다.
