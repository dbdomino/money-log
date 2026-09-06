# Implementation Plan: 지출·소득 등록과 할부·엑셀 일괄 등록

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-backend-expense-income/spec.md`

## Summary

Phase 3 지출·소득 API 12건(3.1~3.12)을 `money-backend-app`에 올린다.
저장 구조는 `001`이, 인증·응답 규격·에러 처리·로깅은 `002`가, 참조 대상인 수단·지출유형은
`003`이 만든다. 004는 **가계부의 실제 기록**을 얹는다.

003과 성격이 갈리는 지점이 셋이다.

| | 003 (수단·지출유형) | 004 (지출·소득) |
|---|---|---|
| 삭제 | 삭제 표시(행 보존) | **물리 삭제** |
| 이름 | 원본만 있고 스냅샷 없음 | **등록 당시 이름을 스냅샷으로 복제** |
| 새 의존성 | 없음 | **Apache POI** (`.xlsx` 읽기·쓰기) |

두 번째가 이 기능의 개념적 핵심이다. `tbl_expense`에는 `payment_method_idx`(참조)와
`payment_method_name`(스냅샷)이 **둘 다** 있다. 수단 이름을 바꿔도 과거 지출은 그때 이름으로
남아야 하기 때문인데(FR-302), 그러면 "언제 스냅샷을 갱신하는가"가 규칙이 된다 —
**참조 자체가 바뀔 때만**이다(FR-304).

작업의 무게 중심은 API 12건이 아니라 **세 덩어리**다.

| 덩어리 | 무엇이 어려운가 |
|---|---|
| **할부** (3.5·3.6) | N개 행을 한 트랜잭션에 만들고 시퀀스로 그룹을 묶는다. 중도상환의 날짜 경계가 틀리면 과거 합계가 소급해 바뀐다 |
| **엑셀** (3.11·3.12) | 새 의존성 · 래퍼 예외 1건 · 전체 롤백 · 행별 오류 위치 안내 |
| **스냅샷** (전 API) | 등록·수정 경로마다 갱신 조건이 같아야 한다. 한 곳만 어긋나도 과거 데이터가 오염된다 |

기술 결정은 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Spring Data JPA · Spring AOP ·
Bean Validation · Lombok · MapStruct 1.6.3 · PostgreSQL JDBC.
`002`의 Spring Security·jjwt, `003`의 `IconStorage`를 그대로 쓴다.
**이 기능이 새로 추가하는 의존성: `org.apache.poi:poi-ooxml`** (`.xlsx` 전용).
루트 `build.gradle`의 `project(':app-mod:money-backend-app')` 블록에 넣는다 —
모듈별 `build.gradle`이 없다.

**Storage**: PostgreSQL 18 · 스키마 `moneylog`.
쓰는 테이블 **2개** — `tbl_expense`, `tbl_income`.
읽기만 하는 테이블 2개 — `tbl_user_payment_method`, `tbl_user_expend_group`(참조 검증·이름 스냅샷).
**시퀀스 1개** — `seq_installment_group`(할부 그룹 식별자).
**스키마 변경 없음.**

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test`.
엑셀 업로드는 `MockMultipartFile`로 내고, 검증용 `.xlsx`는 테스트가 POI로 직접 만든다
(고정 파일을 리소스에 두면 컬럼 정의가 바뀔 때 같이 안 바뀐다).

**Target Platform**: JVM 서버. 백엔드 `:8081`, `/api/v1/*`.

**Project Type**: Spring Boot 멀티모듈 웹 서비스(백엔드 API).

**Performance Goals**: 정하지 않는다. 다만 **규모 상한이 명세에 하나 있다** — 엑셀 업로드
최대 300행(FR-319). 이건 성능 목표가 아니라 입력 제한이고, 300행이면 한 트랜잭션에 넣어도
문제가 되지 않는 크기라 배치 분할을 설계하지 않는다.

**Constraints**:
- 11건은 `{ resCode, data }`. **3.11(양식 다운로드)만 예외**로 `.xlsx` 바이너리를 돌려준다(FR-322).
  **3.12(업로드)는 예외가 아니다** — 파일을 돌려주지 않고 래퍼를 쓴다.
- 삭제는 전부 **물리 삭제**(FR-308). 003과 정반대다.
- `PUT` 금지. 3.3·3.9는 `PATCH`. **3.6(중도상환)도 `PATCH`** — 자원 1건 삭제가 아니라
  그룹의 일정 처리이기 때문이다(FR-316).
- 금액은 `BIGINT` 원 단위 정수. 0보다 커야 한다.
- 이름 스냅샷은 **참조가 바뀔 때만** 갱신(FR-304).
- 할부 3개 컬럼은 **셋 다 비거나 셋 다 채워져야** 한다. DB가 막지 않는다.
- 새 참조는 **사용 중**(`in_use=true`·`deleted=false`)만 허용(FR-325). 단 **기존 행은
  참조가 나중에 죽어도 정상 동작**해야 한다(FR-326).

**Scale/Scope**: API 12건 · FR 26건(FR-301~326) · SC 10건 · 에러코드 18개 ·
쓰는 테이블 2개 + 읽는 테이블 2개 + 시퀀스 1개. User Story 4개를 P1→P3로 자른다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. `money-app`은 건드리지 않는다. 새 클래스는
      `money-backend-app`과 `data-mod`(Repository 메서드)에 들어간다. `common-mod` 추가분은
      `ErrorCode` 상수뿐이다. **POI는 `money-backend-app`에만 넣는다** — `common-mod`에 넣으면
      프론트까지 엑셀 라이브러리를 끌고 간다.
- [x] **II. 레이어 흐름** — PASS. `Controller → Service → Repository`. Entity는 경계를 넘지 않는다.
      **엑셀 파싱·생성을 Service에 직접 두지 않고 `ExcelWorkbook*`으로 분리**한다.
- [x] **III. 응답 규격** — PASS(예외 1건 명시). 11건은 `{ resCode, data }`, 3.11은 FR-322가
      명세에 적어 둔 예외다 → Complexity Tracking. `PUT` 0건.
- [x] **IV. 로깅** — PASS. `002`의 AOP 로깅을 쓴다. **3.11의 응답 본문과 3.12의 업로드 파일은
      로그에 찍지 않는다** — 계약에 명시했다.
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `phase3-지출-소득/` 12건이 있고 커밋 `84ad88c`에서
      개정됐다. **착수 전 개정 3건**이 남아 있다 → 아래 참고.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql`로 확인했고 **스키마 변경 없음**이다.
      두 테이블·CHECK 4건·인덱스 3건·시퀀스 1건이 이미 있다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 POI는 `money-backend-app`의 `excel` 패키지에만 나타난다. 역방향 의존 0건.
- [x] **II** — [contracts/](./contracts/)의 12건 모두 Controller가 Service만 부른다.
      POI API는 `excel` 패키지 밖으로 새지 않는다.
- [x] **III** — 12건의 성공·실패 응답을 코드까지 적었다. 3.11의 예외 경로를
      [contracts/excel-contract.md](./contracts/excel-contract.md)에 정했다.
- [x] **IV** — 3.11 응답·3.12 요청을 AOP 로깅에서 제외하는 규칙을 계약에 넣었다.
- [x] **V** — 선행 개정 3건을 quickstart의 착수 전 절차로 넣었다.
- [x] **VI** — 스키마 무변경. 완료 판정에 덤프 diff 확인을 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

스펙이 결론을 이미 적어 두었으므로 반영만 하면 된다.

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `3.5-ExpenseCreateInstallment.md` 비고 | "매월 1일 (또는 정책일)" → **"(또는 정책일)" 삭제** | FR-324. 요청이 일(day)을 받지 않고 수단에 결제일 컬럼도 없다 |
| 2 | `3.6-ExpenseSettle...md` 비고 | "오늘 이후(또는 미결제)" → **`payment_date > today`** | FR-315. "미결제"는 판정할 컬럼이 없다 |
| 3 | `3.1`·`3.7` 실패 표 | `3003`·`3103` 설명 "없음·비활성"에 **삭제 표시 포함** | FR-325 |

1·2는 커밋 `84ad88c`에서 이미 반영됐다(`3.5`·`3.6` 본문). 3도 같은 커밋에서 반영됐다.
**착수 시점에 `grep`으로 재확인**하고, 남아 있으면 먼저 고친다 → [quickstart.md](./quickstart.md).

## Project Structure

### Documentation (this feature)

```text
specs/004-backend-expense-income/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 10건
├── data-model.md        # Phase 1 — 쓰는 테이블 2개와 할부 불변식
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-contract.md      # API 12건의 규칙과 실패 코드 매핑
│   └── excel-contract.md    # 양식 컬럼 정의·업로드 검증·래퍼 예외
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물
```

### Source Code (repository root)

`+`는 신규, `~`는 수정이다.

```text
build.gradle                                     ~ money-backend-app 에 poi-ooxml 추가

common-mod/src/main/java/com/dbdomino/moneylog/common/
└── error/ErrorCode.java                         ~ 32xx·33xx·35xx 코드 13개 추가

app-mod/money-backend-app/src/main/
├── java/com/dbdomino/moneylog/backend/
│   ├── controller/
│   │   ├── ExpenseController.java               + 3.1~3.6
│   │   ├── IncomeController.java                + 3.7~3.10
│   │   └── ExpenseIncomeExcelController.java    + 3.11·3.12
│   ├── service/
│   │   ├── ExpenseService.java                  + 지출 단건 4건
│   │   ├── InstallmentService.java              + 할부 등록·중도상환
│   │   ├── IncomeService.java                   + 소득 4건
│   │   ├── ExcelTemplateService.java            + 양식 생성 (3.11)
│   │   ├── ExcelImportService.java              + 업로드 파싱·검증·저장 (3.12)
│   │   └── ReferenceResolver.java               + 수단·유형 검증 + 이름 스냅샷 획득
│   ├── excel/
│   │   ├── ExcelColumn.java                     + 컬럼 정의 (양식·업로드 공용)
│   │   ├── ExcelTemplateWriter.java             + POI 로 .xlsx 생성
│   │   └── ExcelRowReader.java                  + POI 로 .xlsx 파싱
│   ├── dto/
│   │   ├── request/                             + 등록·수정 Request DTO
│   │   └── response/                            + ExpenseDto·IncomeDto·ExcelImportResultDto
│   └── mapper/
│       ├── ExpenseMapper.java                   + Entity ↔ DTO
│       └── IncomeMapper.java                    + Entity ↔ DTO
└── resources/application.yml                    ~ multipart max-file-size (엑셀)

data-mod/src/main/java/com/dbdomino/moneylog/data/repository/
├── UserExpenseRepository.java                   ~ 소유자 조회·할부 그룹 조회·중도상환 삭제
└── UserIncomeRepository.java                    ~ 소유자 조회

app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/
├── expense/       + 일시불 지출 (US1)
├── income/        + 소득 (US2)
├── installment/   + 할부 등록·중도상환 (US3)
└── excel/         + 양식·업로드 (US4)
```

**Structure Decision**: 기존 구조를 그대로 쓴다. 새 모듈은 만들지 않는다.

004가 새로 만드는 구조는 **`excel` 패키지**와 **`ReferenceResolver`** 둘이다.

- `excel` — POI API를 가두는 경계다. `Workbook`·`Row`·`Cell` 타입이 Service로 새어 나가면
  Service가 라이브러리에 묶이고, 나중에 형식이 바뀔 때(`.csv` 지원 등) 손댈 곳이 흩어진다.
  `ExcelColumn`을 양식 생성과 업로드 파싱이 **공유**하는 것이 FR-318("양식과 업로드의 컬럼
  정의가 일치")을 구조로 보장하는 방법이다.
- `ReferenceResolver` — "수단·유형이 사용 중인가 확인하고 그 시점 이름을 돌려준다"는 동작이
  **3.1·3.3·3.7·3.9·3.12 다섯 경로**에서 똑같이 필요하다. 각자 구현하면 한 곳만 빠뜨려도
  스냅샷 규칙이 깨진다. 한 곳에 모아 FR-302~305·FR-325를 함께 강제한다.

`core-mod`는 이번에도 건드리지 않는다.

## Complexity Tracking

> 원칙 III의 응답 규격에 **명세가 승인한 예외 1건**이 있다. 위반이 아니라 기록이다.

| 항목 | 원칙이 요구하는 것 | 이 기능이 하는 것 | 근거 |
|---|---|---|---|
| 3.11 양식 다운로드 | 성공·실패 모두 `{ resCode, data }` | 성공 시 **`.xlsx` 바이너리** + `Content-Disposition: attachment` | FR-322. 엑셀 파일을 base64로 감싸면 프론트가 다시 디코딩해 Blob을 만들어야 하고 응답이 33% 커진다 |
| 3.11 실패 | — | **래퍼를 쓴다** (`1001`·`9000`) | FR-322 후반 — "인증 실패처럼 래퍼로 답할 수 있는 상황에서는 래퍼를 쓴다" |
| 3.12 업로드 | `{ resCode, data }` | **예외가 아니다.** 성공·실패 모두 래퍼 | FR-322. 리뷰에서 잡힌 오기를 clarify로 확정했다 |

003의 아이콘 조회(2.10)와 다른 점이 하나 있다. **2.10은 인증 실패도 래퍼를 쓰지 않지만
(래퍼 없는 401), 3.11은 인증 실패에 래퍼를 쓴다.** 2.10은 `<img>`/fetch로 받는 이미지라
본문 없이 상태 코드만으로 판정하지만, 3.11은 사용자가 다운로드 버튼을 누르는 흐름이라
실패 사유를 화면에 띄워야 한다.

기각한 대안 2가지.

- **양식을 base64로 감싸 규격을 통일한다** — 원칙 III에 예외가 사라지지만 응답이 커지고
  프론트가 디코딩·Blob 변환을 직접 해야 한다. 설계 명세 `3.11`이 처음부터 파일 반환으로 정의했다.
- **양식을 정적 파일로 미리 만들어 두고 내려준다** — 예외 자체가 없어지지만 FR-317이
  "본인의 사용 중 수단·지출유형이 데이터 유효성 목록으로 포함되어야 한다"고 요구한다.
  양식은 **회원마다 내용이 다르다.** 정적 파일로는 불가능하다.
