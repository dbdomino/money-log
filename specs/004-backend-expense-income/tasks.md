---
description: "Task list for 004-backend-expense-income"
---

# Tasks: 지출·소득 등록과 할부·엑셀 일괄 등록

**Input**: `/specs/004-backend-expense-income/` 의 설계 산출물

**Prerequisites**: plan.md · spec.md · research.md · data-model.md · contracts/ (api-contract.md · excel-contract.md) · quickstart.md

**Tests**: 포함한다. quickstart.md §3 이 시나리오 **51건**을 `#N` 으로 매기고 §4 가 그것을 완료 판정으로 쓰므로, 통합 테스트가 곧 인수 기준이다. 002·003 과 같은 방식이다.

**Organization**: User Story 단위로 묶어 각 스토리를 독립적으로 구현·검증·인도할 수 있게 한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완 작업에 의존하지 않음)
- **[Story]**: 이 작업이 속한 User Story (US1~US4)
- 설명에 정확한 파일 경로를 적는다

## Path Conventions

Spring Boot 멀티모듈이다. 저장소 루트 기준 경로를 쓴다.

- 백엔드 메인: `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/`
- 백엔드 테스트: `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/`
- 공통: `common-mod/src/main/java/com/dbdomino/moneylog/common/`
- 데이터: `data-mod/src/main/java/com/dbdomino/moneylog/data/`

**모듈별 `build.gradle` 이 없다.** 의존성은 루트 `build.gradle` 의 `project(':app-mod:money-backend-app')` 블록에서 고친다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 착수 전 확인과 새 의존성·설정. **T001·T002·T060 은 코드보다 먼저** 한다(헌장 원칙 V)

- [X] T001 명세 선행 개정 3건이 반영되어 있는지 확인한다 — `프로젝트설계/기능명세상세-백엔드/phase3-지출-소득/` 에서 ① `grep -rn '또는 정책일'` **0건**(FR-324) ② `grep -rn '오늘 이후(또는 미결제)'` **0건**(FR-315) ③ `3.1-ExpenseCreate.md`·`3.7-IncomeCreate.md` 의 `3003`·`3103` 설명에 **"삭제 표시"가 포함**(FR-325). **확인 결과 3건 모두 커밋 `84ad88c` 에서 반영됐고 표의 빈 설명 칸도 0건이다** — 남아 있으면 코드를 쓰기 전에 먼저 고친다. 아울러 `_공통.md` 의 `3502` 결번 철회(259행)도 함께 확인한다
- [X] T002 `specs/004-backend-expense-income/contracts/api-contract.md` §2 의 3.5 응답 설명을 정정한다 — "`createdCount`만"으로 적혀 있으나 설계 명세 `3.5-ExpenseCreateInstallment.md` 는 `installmentGroupId`·`createdCount` **두 필드**를 돌려준다. 명세가 기준이므로(원칙 V) 계약 문서를 "**지출 목록을 싣지 않는다**(`installmentGroupId`·`createdCount` 두 칸)"로 고친다. quickstart #24 의 "`createdCount`만"도 같은 뜻이므로 함께 다듬는다. **이 한 줄을 그대로 두면 구현자가 필드를 하나만 만들어 3.5 응답이 명세와 어긋난다**
- [X] T003 루트 `build.gradle` 의 `project(':app-mod:money-backend-app')` 블록에 `implementation 'org.apache.poi:poi-ooxml:5.5.1'` 을 추가한다 — **버전을 문자열로 고정한다**(Spring Boot BOM 이 관리하지 않아 안 적으면 해석이 갈린다). `5.5.1` 은 현재 릴리스이며 Java 11+ 를 요구하므로 이 프로젝트(Java 17)에서 쓸 수 있다. 착수 시 `./gradlew :app-mod:money-backend-app:dependencies --configuration runtimeClasspath` 로 해석되는지 확인한다. `poi` (구형 `.xls`)는 넣지 않는다: FR-319 가 `.xlsx` 만 허용하므로 의존성이 줄고 실수로 `.xls` 가 파싱되는 일도 없다. **`common-mod` 에 넣지 않는다** — 프론트 `money-app` 이 `common-mod` 를 의존하므로 거기 넣으면 프론트 빌드가 엑셀 라이브러리를 끌고 간다(research.md §1)
- [X] T004 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java` 에 이 기능이 쓰는 코드 **13개**(`3201`~`3207` 7개 · `3301`~`3302` 2개 · `3502`~`3505` 4개)가 전부 있는지 확인한다. **002 의 T007 에서 002~006 대역을 미리 넣었으므로 추가할 것이 없어야 한다**(확인 결과 13개 모두 존재). `3003`·`3103` 은 003 이 소유한 코드이며 여기서 재사용만 한다 — 다시 정의하지 않는다
- [X] T005 `app-mod/money-backend-app/src/main/resources/application.yml` 의 `spring.servlet.multipart` 한도를 엑셀 기준으로 올린다(`max-file-size`·`max-request-size`). 현재 값 `1MB`/`2MB` 는 003 의 아이콘 기준이라 그대로 두면 엑셀 업로드가 서블릿에서 먼저 잘린다. **아이콘은 애플리케이션 검사(`ExpendGroupIconService`)가 1MB 로 계속 좁히므로 003 의 quickstart #29 는 깨지지 않는다**(excel-contract.md §5). 300행짜리 `.xlsx` 는 수십 KB 라 **`max-file-size: 10MB` · `max-request-size: 12MB`** 로 잡는다 — 상한을 크게 두어도 실제 방어선은 애플리케이션의 300행 판정(FR-319)이다. **주석에 "이 값을 올려도 아이콘 1MB 판정은 애플리케이션이 한다"를 남긴다** — 안 남기면 다음 사람이 003 의 계약이 깨졌다고 오해한다
- [X] T006 T005 의 부작용을 막는다 — `common-mod/.../error/GlobalExceptionHandler.java` 의 `MaxUploadSizeExceededException` → **`3102`(아이콘)** 매핑은 003 이 유일한 업로드 API 였을 때의 판단이다. 엑셀 업로드가 생기면 **엑셀 크기 초과에도 아이콘 코드가 나간다**. 요청 경로로 갈라 아이콘 경로는 `3102`, 그 밖은 `9001`(잘못된 요청)로 내보내거나, 갈림이 과하다면 **핸들러 주석에 "004 의 엑셀은 서블릿 한도를 넘지 않는 크기 상한(300행)을 따로 가지므로 이 갈래에 거의 닿지 않는다"를 명시**한다. **어느 쪽이든 결정을 문서로 남긴다** — 003 의 T054 가 이 매핑을 넣은 이유가 주석에 있으므로 그 옆에 적는다

**Checkpoint**: 의존성·설정이 서고, 명세와 계약 문서가 구현과 어긋나지 않는다

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story 가 공유하는 것. **여기가 끝나기 전에는 어떤 스토리도 시작할 수 없다**

- [X] T007 [P] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserExpenseRepository.java` 에 004 가 쓸 조회를 추가한다 — ① `Optional<UserExpense> findByIdxAndUserIdKey(Long, Long)` **이미 있다**(확인) ② `List<UserExpense> findByInstallmentGroupIdAndUserIdKeyOrderByInstallmentIndexAsc(Long, Long)` **추가**: 기존 `findByInstallmentGroupIdOrderByInstallmentIndexAsc` 는 소유자 조건이 없어 남의 그룹을 집어온다 — 3.6 의 `3206` 판정이 이걸 쓴다 ③ `long countByInstallmentGroupIdAndPaymentDateAfter(Long, LocalDate)` **추가**: `3207`(남은 회차 0건) 판정을 삭제 전에 해야 하므로 세는 쿼리가 따로 필요하다. `deleteByInstallmentGroupIdAndPaymentDateAfter` 는 **이미 있다**(확인)
- [X] T008 [P] `data-mod/.../repository/UserExpenseRepository.java` 의 javadoc 에서 **기능번호 오기를 고친다** — "중도상환(3.7)"로 적혀 있으나 중도상환은 **3.6** 이고 3.7 은 `IncomeCreate` 다. 004 를 구현하며 이 주석을 근거로 삼으면 엉뚱한 API 를 찾게 된다. `FR-045`·`FR-046`·`FR-037`·`FR-205` 참조도 001·003 시절 번호이므로 현행 FR 로 맞춘다. T007 과 **같은 파일이라 순차**
- [X] T009 [P] `data-mod/.../repository/UserExpenseRepository.java` 에 할부 그룹 시퀀스 채번을 추가한다 — `@Query(value = "select nextval('moneylog.seq_installment_group')", nativeQuery = true) Long nextInstallmentGroupId();`. **`@GeneratedValue` 로 쓸 수 없다**: `installment_group_id` 는 PK 가 아니라 일반 컬럼이고 N개 행이 **공유**하는 값이라 어느 Entity 의 식별자 생성기도 아니다(research.md §4, 001 이 이 시퀀스를 `AdditionalMappingContributor` 로 따로 만든 이유). **스키마 이름을 하드코딩하는 것이 여기서는 맞다** — 001 의 `MoneylogSchemaContributor` 와 달리 이건 런타임 조회라 `${schema}` 자리표시자가 없다. T007·T008 과 같은 파일이라 순차
- [X] T010 [P] `.../backend/service/ReferenceResolver.java` 를 만든다 — **이 기능에서 가장 중요한 단일 지점**이다. "수단·지출유형이 **사용 중**(`in_use=true`·`deleted=false`)인지 확인하고 그 시점 이름을 돌려준다"를 담당하며 **3.1·3.3·3.7·3.9·3.12 다섯 경로**가 전부 이것만 쓴다. 실패는 수단 `3003`·지출유형 `3103`(FR-325). **소유자 조건을 조회에 함께 건다** — 남의 수단으로 내 지출을 만들 수 없어야 한다. 각 경로가 각자 구현하면 한 곳만 빠뜨려도 과거 데이터가 오염된다(plan.md · research.md §2·§3). T007~T009 에 의존
- [X] T011 [P] `.../backend/service/ReferenceResolver.java` 에 **스냅샷 갱신 3갈래 판정**을 넣는다(api-contract.md §4) — ① 요청이 참조 필드를 **omit** → 참조·스냅샷 둘 다 건드리지 않고 **검증도 하지 않는다** ② 보냈는데 **값이 기존과 같음** → 건드리지 않는다 ③ 보냈고 **값이 다름** → 새 참조를 검증하고 그 **현재** 이름으로 스냅샷 갱신. **①이 FR-326 을 성립시킨다** — 죽은 수단을 쓰던 과거 지출의 금액만 고치는 수정이 성공해야 한다. **②를 빠뜨리면** 같은 수단을 유지한 채 금액만 고쳤을 때 이름이 조용히 바뀌어 003 의 SC-205 가 깨진다. T010 과 같은 파일이라 순차
- [X] T012 [P] `.../backend/service/InstallmentColumns.java` 에 **할부 3컬럼 불변식**을 둔다 — **독립 클래스로 만든다**(`ExpenseService` 의 헬퍼로 두지 않는다): 저장 진입점이 `ExpenseService`(3.1)·`InstallmentService`(3.5)·`ExcelImportService`(3.12) **세 클래스로 갈리므로**, 한 서비스 안에 두면 나머지 둘이 그 서비스를 부르거나 규칙을 복제하게 된다 — `installment_group_id`·`installment_index`·`installment_total` 이 **셋 다 NULL(일시불)이거나 셋 다 채워짐(할부, `index` 1..N · `total` N≥2)** 이어야 한다. **DB 가 막지 않는다**: CHECK 2건이 각 컬럼의 범위만 보고 세 컬럼의 동시성은 검사하지 않아 `installment_index` 만 채운 행을 받아들인다(research.md §5). 저장 진입점 **셋**(3.1 일시불 · 3.5 할부 · 3.12 엑셀)이 이걸 통과하게 만든다. **DB CHECK 을 추가하지 않는다** — 스키마 변경이고 이 기능은 무변경이 전제다(원칙 VI)
- [X] T013 [P] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/AbstractApiIT.java` 에 004 가 쓸 헬퍼를 더한다 — 지출·소득을 만들 때 필요한 **사용 중 수단·지출유형을 003 의 API 로 준비하는** 헬퍼(`createExpenseMethod`·`createIncomeMethod`·`createExpendGroup`)와 `postMultipart` 계열. `TABLES_IN_DELETE_ORDER` 에 `tbl_expense`·`tbl_income` 이 **이미 있다**(확인) — 004 는 새 테이블을 만들지 않으므로 정리 목록은 그대로다. 003 의 `AbstractExpendGroupIT` 가 이미 가입·multipart 헬퍼를 갖고 있으므로 **중복 정의하지 않고 재사용 경로를 정한다**

**Checkpoint**: 참조 검증·스냅샷 판정·할부 불변식·채번이 서고, 모든 스토리가 이 위에 얹힌다

---

## Phase 3: User Story 1 - 일시불 지출을 기록한다 (Priority: P1) 🎯 MVP

**Goal**: 지출을 등록(3.1)하고 상세를 보고(3.2) 고치고(3.3) 지운다(3.4)

**Independent Test**: 수단·유형을 하나씩 만들어 두고 지출을 등록·조회·수정·삭제한다. 할부·소득·엑셀 없이 완결된다

### Tests for User Story 1

> 먼저 작성하고, 구현 전에 **실패하는 것**을 확인한다

- [X] T014 [P] [US1] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/expense/ExpenseCreateIT.java` — quickstart #1(사용 중 수단·유형으로 등록하면 **이름 2개가 스냅샷으로** 저장 — FR-302)·#4(금액 0 이하 `3201`)·#5(**장소 101자·내용 256자 `3201`** — `9000` 이 아니다, FR-305)·#12(같은 날짜·금액·수단으로 두 건 등록해도 **둘 다 성공**, FR-309)·#13(사용 안 함·삭제 표시된 수단 `3003` — SC-310)·#14(같은 상태의 유형 `3103`). **#5 가 이 클래스의 핵심**이다 — 길이 검사를 빠뜨리면 DB 오류가 `9000`(서버 장애)으로 새어 나가는데 실제로는 사용자 입력 문제다
- [X] T015 [P] [US1] `.../expense/ExpenseSnapshotIT.java` — **#6·#7·#8 이 한 묶음**이며 FR-304 의 세 갈래를 전부 덮는다: #6(수정에서 `paymentMethodId` 를 **바꾸면** 스냅샷이 **새 수단의 현재 이름으로 갱신**)·#7(**omit** 하고 금액만 바꾸면 스냅샷이 **그대로**)·#8(**같은 값**을 보내도 스냅샷이 **그대로**). 여기에 #2(등록 후 수단 이름을 바꾸고 조회하면 **등록 당시 이름 그대로**, SC-305)를 더한다. **#7·#8 을 빠뜨리면 "수정 요청이 왔으니 최신화한다"는 잘못된 구현이 그대로 통과한다**
- [X] T016 [P] [US1] `.../expense/ExpenseReferenceLifecycleIT.java` — **참조 비대칭(FR-325 ↔ FR-326)** 을 본다: #3(등록 후 그 수단을 **삭제 표시**하고 조회하면 정상적으로 읽히고 이름도 남는다)·#9(**삭제 표시된 수단을 쓰던 지출의 금액만 수정** → 성공한다)·#11(삭제 후 재조회 → 없다, **물리 삭제** — FR-308). #9 가 함정이다 — 수정 경로에서 참조 검증을 무조건 돌리면 죽은 수단을 쓰던 과거 지출을 영영 못 고치게 된다
- [X] T017 [P] [US1] `.../expense/ExpenseOwnershipIT.java` — #10(남의 지출 ID 로 조회·수정·삭제 전부 `3202` — FR-301·SC-307). **"없음"과 "타인 소유"가 같은 코드**인지 확인한다(api-contract.md §1) — 코드가 갈리면 ID 를 훑는 것만으로 남의 지출이 존재한다는 사실이 새어 나간다. 003 의 `PaymentMethodOwnershipIT`(`3003`)·`ExpendGroupOwnershipIT`(`3103`)와 같은 형태다

### Implementation for User Story 1

- [X] T018 [P] [US1] `.../backend/dto/request/ExpenseCreateRequest.java` 를 만든다 — `paymentMethodId`·`expendGroupId`·`amount`·`paymentDate`·`place`·`content`. **값 범위·길이 검증을 Bean Validation 에 전부 맡기지 않는다**: 허용 밖은 `3201` 인데 Bean Validation 실패는 전역 처리에서 `9001` 로 나간다. 누락은 `9001`(`@NotNull`), **값·길이 오류는 `3201`(서비스)** 로 갈라 붙인다 — 002 의 `SignupRequest`, 003 의 `PaymentMethodCreateRequest` 와 같은 판단이다. **할부 3필드를 받지 않는다** — 일시불 등록은 세 컬럼이 전부 NULL 이다(api-contract.md §8)
- [X] T019 [P] [US1] `.../backend/dto/response/ExpenseResponse.java` 를 만든다 — `expenseId`·`paymentMethodId`·`paymentMethodName`·`expendGroupId`·`expendGroupName`·`amount`·`paymentDate`·`place`·`content`·`installmentGroupId`·`installmentIndex`·`installmentTotal`. **참조 ID 와 이름 스냅샷을 둘 다 싣는다** — 앞은 "지금 이 수단은 무엇인가", 뒤는 "**등록 당시** 뭐라고 불렸나"에 답한다(data-model.md §1). 할부 3필드는 일시불이면 `null` 이며 **필드를 생략하지 않는다**(3.2 가 할부 회차도 읽으므로 같은 타입을 쓴다). **Entity 를 그대로 내보내지 않는다**(원칙 II)
- [X] T020 [P] [US1] `.../backend/dto/response/ExpenseDeleteResponse.java` 를 만든다 — 설계 명세 `3.4-ExpenseDelete.md` 의 성공 `data` 필드 표를 그대로 따른다. **003 의 삭제 응답을 베끼지 않는다** — 003 은 삭제 표시라 `deleted: true` 를 싣지만 004 는 **물리 삭제**라 그 필드가 의미를 갖지 않는다(data-model.md §0)
- [X] T021 [US1] `.../backend/mapper/ExpenseMapper.java` 를 MapStruct 로 만든다 — Entity ↔ DTO 변환만 한다. `idx` → `expenseId`, 연관의 `idx` → `paymentMethodId`·`expendGroupId` 이름 매핑이 필요하다. **소유자(`user`)를 매핑하지 않는다** — 대상 타입에 필드가 없어 매핑할 자리도 없다. 비즈니스 로직을 담지 않는다(원칙 II). T019·T020 에 의존
- [X] T022 [US1] `.../backend/service/ExpenseService.java` 에 등록(3.1)·상세(3.2)를 구현한다 — 판정 순서는 api-contract.md §6 그대로 **`9001`(필수 누락) → `3003`/`3103`(참조 검증) → `3201`(값·길이) → 스냅샷 획득 + INSERT**(FR-301·FR-302·FR-305). 참조 검증과 스냅샷은 **`ReferenceResolver` 만** 부른다(T010·T011). 할부 3컬럼은 **전부 NULL** 로 강제한다(T012). 조회는 `findByIdxAndUserIdKey` 로 소유자를 조건에 걸어 `3202` 로 묶는다. T007~T012·T018~T021 에 의존
- [X] T023 [US1] `.../backend/service/ExpenseService.java` 에 수정(3.3)을 구현한다 — 순서: **대상 조회(`3202`) → 할부 개월·시작 연월 변경 시도(`3203`) → 참조 변경 검증(`3003`/`3103`) → 값 검증(`3201`) → UPDATE**. **`3203` 이 참조·값 검증보다 먼저다** — 할부 구조를 바꾸려는 요청은 다른 필드가 아무리 정상이어도 거절이며 "바꾸려면 삭제 후 재등록한다"(FR-314). omit 규칙은 002 의 `PatchFields` 를 재사용하고, **스냅샷은 `ReferenceResolver` 의 3갈래 판정에 맡긴다**(T011) — 여기서 다시 구현하지 않는다. T022 와 같은 파일이라 순차
- [X] T024 [US1] `.../backend/service/ExpenseService.java` 에 삭제(3.4)를 구현한다 — **물리 삭제**(`DELETE`)다(FR-308). 003 의 삭제 표시와 정반대이며 `tbl_expense` 에 `deleted` 컬럼이 **없다**(덤프 확인). 대상이 없거나 남의 것이면 `3202`. **"이미 삭제됨" 코드가 없다** — 물리 삭제라 두 번째 요청은 그냥 `3202` 다(003 의 `3004`·`3108` 과 다른 점). T023 과 같은 파일이라 순차
- [X] T025 [US1] `.../backend/controller/ExpenseController.java` 를 만들고 3.1~3.4 를 붙인다 — `POST·GET·PATCH·DELETE /api/v1/expenses` 와 `/{expenseId}`. 인가는 002 의 `SecurityFilterChain` 기본값(`authenticated`)이 처리하므로 애너테이션을 붙이지 않는다. Controller 는 Service 만 부른다(원칙 II). 예외를 잡지 않는다 — 실패 응답 변환은 전역 처리의 몫이다(원칙 III). **`PUT` 을 쓰지 않는다**

**Checkpoint**: 일시불 지출이 독립적으로 동작한다. quickstart **#1~#14** 가 통과하면 MVP 로 인도할 수 있다

---

## Phase 4: User Story 2 - 소득을 기록한다 (Priority: P2)

**Goal**: 소득을 등록(3.7)하고 조회·수정·삭제한다(3.8~3.10)

**Independent Test**: 소득용 수단으로 소득을 등록·조회·수정·삭제한다. 지출 API 없이 완결된다

### Tests for User Story 2

- [ ] T026 [P] [US2] `.../income/IncomeCreateIT.java` — #15(`purpose=INCOME` 수단으로 등록하면 수단 이름 스냅샷과 함께 저장 — FR-303)·#16(`content` 를 **비우고** 등록해도 성공 — FR-307, 지출과 다르다)·#17(Body 에 `place`·`expendGroupId`·할부 필드를 실어도 **무시**되고 저장·응답 어디에도 없다 — FR-306)·#20(금액 0 이하 → **`3301`**, `3201` 이 **아니다** — FR-305). **#20 이 지출·소득의 코드 대역이 갈린다는 것을 못박는다** — 한 서비스에서 코드를 공유하면 여기서 걸린다
- [ ] T027 [P] [US2] `.../income/IncomeOwnershipIT.java` — #19(남의 소득 ID 로 조회·수정·삭제 전부 `3302` — SC-307). #18(소득 수단을 바꾸면 이름 스냅샷 갱신)도 여기서 본다. **`3202`(지출)와 섞이지 않는지** 확인한다

### Implementation for User Story 2

- [ ] T028 [P] [US2] `.../backend/dto/request/IncomeCreateRequest.java` 를 만든다 — `paymentMethodId`·`amount`·`paymentDate`·`content`(**선택**). **`place`·`expendGroupId`·할부 필드를 아예 두지 않는다** — `tbl_income` 에 대응 컬럼이 없다(FR-306). 필드가 없으면 실어 보내도 조용히 무시되며(#17), "비워 두는 것"이 아니라 "**컬럼이 아예 없는 것**"이라는 구조가 타입으로 드러난다
- [ ] T029 [P] [US2] `.../backend/dto/response/IncomeResponse.java`·`IncomeDeleteResponse.java` 를 만든다 — `incomeId`·`paymentMethodId`·`paymentMethodName`·`amount`·`paymentDate`·`content`. **DB 컬럼은 `payment_date` 지만 응답 필드는 `paymentDate` 다**(입금일을 뜻한다, data-model.md §2) — 이름을 `incomeDate` 로 바꾸지 않는다. 삭제 응답은 설계 명세 `3.10-IncomeDelete.md` 의 표를 따른다
- [ ] T030 [US2] `.../backend/mapper/IncomeMapper.java` 를 MapStruct 로 만든다 — `idx` → `incomeId`, 연관의 `idx` → `paymentMethodId`. `ExpenseMapper` 를 상속·재사용하지 않는다: 필드 구성이 달라 공통 상위를 만들면 없는 필드를 매핑하려다 막힌다. T029 에 의존
- [ ] T031 [US2] `.../backend/service/IncomeService.java` 에 3.7~3.10 을 구현한다 — 구조는 `ExpenseService` 와 같지만 **코드가 다르다**: 값 오류 `3301`, 없음·타인 소유 `3302`. 참조 검증·스냅샷은 `ReferenceResolver` 를 그대로 쓰되 **지출유형은 부르지 않는다**. 삭제는 **물리 삭제**. `content` 는 `null` 허용이므로 PATCH 에서 `null` 을 보내면 **비운다**(`PatchFields` 의 omit ≠ null 구분이 여기서 실제로 쓰인다 — 지출의 `content` 는 NOT NULL 이라 그 구분이 없다). T010·T011·T028~T030 에 의존
- [ ] T032 [US2] `.../backend/controller/IncomeController.java` 를 만들고 3.7~3.10 을 붙인다 — `POST·GET·PATCH·DELETE /api/v1/incomes` 와 `/{incomeId}`. `ExpenseController` 와 **자원을 나눈다** — 구조가 달라 별도 자원으로 다룬다(FR-306). `PUT` 을 쓰지 않는다

**Checkpoint**: 소득이 독립적으로 동작하고 지출과 코드 대역이 갈린다

---

## Phase 5: User Story 3 - 할부를 등록하고 중도상환한다 (Priority: P2)

**Goal**: N개월 할부를 한 번에 등록(3.5)하고 남은 회차를 정리한다(3.6)

**Independent Test**: 12개월 할부를 등록해 12행이 같은 그룹 식별자와 1~12 순번으로 생기는지, 중도상환이 미래 회차만 지우는지 확인한다

### Tests for User Story 3

- [ ] T033 [P] [US3] `.../installment/InstallmentCreateIT.java` — #21(12개월 할부 → **12개 행**, 같은 `installmentGroupId`, 회차 1~12 — SC-302)·#22(`startYearMonth=2026-07` → 결제일이 `2026-07-01`~`2027-06-01` **매월 1일** — SC-308·FR-324)·#24(응답에 `installmentGroupId`·`createdCount` 만 있고 **지출 목록이 없다**)·#25(할부 회차 하나를 **3.2 로** 상세 조회 — 일시불과 같은 API)·#28(`months=1` → `3204`, 1개월은 일시불이다 — FR-311). **#22 가 말일 보정 문제를 없앤 근거를 지킨다** — 1일 고정이 아니면 31일 시작 할부의 2월 회차에서 갈린다
- [ ] T034 [P] [US3] `.../installment/InstallmentRollbackIT.java` — #23(12행 중 1건 저장 실패 → **전부 롤백**, 한 행도 남지 않는다, `3205` — FR-310·SC-303). 실패는 주입해야 한다: 마지막 회차의 `content` 를 255자 초과로 만들거나 Repository 를 스파이로 감싼다. **부분 생성되면 사용자가 재등록할 때 앞부분이 중복된다** — 004 는 업무 유일 제약을 두지 않으므로(FR-309) DB 가 막아주지 않는다
- [ ] T035 [P] [US3] `.../installment/InstallmentSettleIT.java` — **#29 가 이 기능에서 가장 중요한 경계 시나리오다**: 과거 2·**오늘 1**·미래 9회차인 할부를 중도상환하면 **미래 9건만 사라지고 오늘 회차는 남는다**(FR-315·SC-304·SC-309). **오늘 날짜의 회차를 반드시 만들어 둔다** — 경계를 `>=` 로 잡은 구현은 이 하나에서만 걸린다. 여기에 #30(그룹 두 개 중 하나만 상환해도 다른 그룹은 영향 없음)·#31(미래 회차 0건인 그룹 재상환 → `3207`, **멱등 성공이 아니다**)·#32(남의 `installmentGroupId` → `3206`)를 더한다. **결제일이 매월 1일이라 "오늘 회차"를 만들려면 `startYearMonth` 를 오늘 기준으로 계산하거나 행을 JDBC 로 직접 손봐야 한다** — 어느 쪽이든 테스트가 실행 날짜에 흔들리지 않게 만든다
- [ ] T036 [P] [US3] `.../installment/InstallmentUpdateIT.java` — #26(할부 회차 하나를 수정하면 **그 달 1건만** 바뀐다 — FR-313)·#27(할부 개월 수·시작 연월 변경 시도 → `3203` — FR-314). #27 은 **할부 건이 아닌 일시불 지출에 그 필드를 보내도 `3203`** 인지 함께 본다(api-contract.md §6 의 "할부 건이든 아니든")
- [ ] T037 [P] [US3] `.../installment/InstallmentInvariantIT.java` — #33(할부 3컬럼 전수 검사: **셋 다 비거나 셋 다 채워짐**, 부분 채움 0건). **API 로 확인할 수 없으므로 SQL 로 직접 본다**: `select count(*) from moneylog.tbl_expense where (installment_group_id is null) <> (installment_index is null) or (installment_group_id is null) <> (installment_total is null)` → `0`. 일시불 등록(3.1)·할부 등록(3.5)을 모두 거친 뒤에 센다

### Implementation for User Story 3

- [ ] T038 [P] [US3] `.../backend/dto/request/InstallmentCreateRequest.java` 를 만든다 — `paymentMethodId`·`monthlyAmount`·`installmentMonths`·`startYearMonth`(`YYYY-MM`)·`place`·`content`·`expendGroupId`. **일(day)을 받지 않는다** — 회차 결제일은 매월 1일로 고정하며(FR-324), `tbl_user_payment_method` 에 결제일 컬럼이 없어 카드별 결제일을 쓸 수 없다(덤프 확인). `installmentMonths` 의 하한(2)은 **서비스가 `3204` 로** 검사한다 — Bean Validation 이면 `9001` 이 나간다
- [ ] T039 [P] [US3] `.../backend/dto/response/InstallmentCreateResponse.java`·`InstallmentSettleResponse.java` 를 만든다 — 등록은 `installmentGroupId`·`createdCount`(T002 가 계약 문서를 이 두 필드로 정정한다), 중도상환은 `installmentGroupId`·`settledCount`·`message`. **지출 목록을 싣지 않는다** — 개별 회차는 3.2 로 읽는다. 목록 응답이 아니므로 `data.list` 규칙이 적용되지 않는다(api-contract.md §2)
- [ ] T040 [US3] `.../backend/service/InstallmentService.java` 에 할부 등록(3.5)을 구현한다 — 순서는 **`3204`(개월 수 <2 · 금액 ≤0) → `3003`/`3103`(참조 검증) → 시퀀스 채번 → N개 행 한 트랜잭션 INSERT → 실패 시 전체 롤백 `3205`**. 회차 n 의 결제일은 `startYearMonth` 에 **n-1개월을 더한 달의 1일**이다. **시퀀스는 그룹당 한 번만 뽑는다**(FR-312) — 행마다 뽑으면 그룹이 흩어져 중도상환이 아무것도 찾지 못한다. 할부 3컬럼을 **전부 채운다**(T012). T009~T012·T038·T039 에 의존
- [ ] T041 [US3] `.../backend/service/InstallmentService.java` 에 중도상환(3.6)을 구현한다 — 순서: **그룹 조회(소유자 포함, 없음·타인 → `3206`) → `payment_date > today` 인 회차 수를 센다(0건 → `3207`) → 그 회차들만 물리 삭제**. **경계가 `>` 다** — `>=` 로 잡으면 오늘 결제된 회차까지 사라져 **이번 달 합계가 소급해 줄어들고 사용자가 이미 본 숫자가 바뀐다**. `3207` 을 **멱등 성공으로 흘리지 않는다** — "방금 정리했다"와 "이미 정리되어 있었다"를 화면이 구분해야 한다. `ix_expense_installment (installment_group_id, payment_date)` 가 이 조회를 그대로 덮는다. T040 과 같은 파일이라 순차
- [ ] T042 [US3] `.../backend/controller/ExpenseController.java` 에 3.5·3.6 을 추가한다 — `POST /api/v1/expenses/installments`, **`PATCH`** `/api/v1/expenses/installments/{installmentGroupId}/remainder`. **중도상환이 `DELETE` 가 아닌 이유를 주석에 남긴다**: 삭제되는 것이 `installmentGroupId` 가 가리키는 자원이 아니라 **그 그룹의 미래 회차 일부**라, `DELETE` 로 표현하면 "그룹을 지운다"로 읽혀 과거 회차까지 사라지는 것으로 오해된다(FR-316). **경로 깊이를 확인한다** — `/installments` 와 `/{expenseId}` 가 같은 깊이이므로 리터럴을 위에 둔다(003 의 `/active` 와 같은 상황이다). T025 와 같은 파일이라 순차

**Checkpoint**: 할부 등록·중도상환이 동작하고 날짜 경계와 그룹 불변식이 지켜진다

---

## Phase 6: User Story 4 - 엑셀로 한 번에 올린다 (Priority: P3)

**Goal**: 양식을 내려받아(3.11) 채운 뒤 일괄 등록한다(3.12)

**Independent Test**: 양식을 받아 몇 행을 채우고 업로드해 지출·소득으로 저장되는지, 잘못된 행 하나가 전체를 롤백시키는지 확인한다

**⚠️ 3.11 이 이 기능에서 유일하게 응답 규격의 예외다.** 성공은 `.xlsx` 바이너리, **인증 실패·생성 오류는 래퍼**다 — 003 의 아이콘(2.10)이 인증 실패에도 래퍼를 쓰지 않는 것과 **다르다**

### Tests for User Story 4

- [ ] T043 [P] [US4] `.../excel/ExcelTemplateIT.java` — #34(양식을 열면 본인 **사용 중** 수단·지출유형이 드롭다운으로 들어 있다 — FR-317)·#35(성공은 `{ resCode, data }` 가 **아니라** 파일이고 `Content-Disposition: attachment` — SC-301)·#36(**Bearer 없이 부르면 래퍼를 쓴다**, `1001`)·#37(사용 안 함·삭제 표시된 수단은 드롭다운에 **없다**). **#35·#36 이 한 쌍이다** — 003 의 2.10 은 인증 실패도 래퍼를 쓰지 않으므로 헷갈리기 쉽다. 응답을 POI 로 다시 읽어 데이터 유효성 목록을 확인한다
- [ ] T044 [P] [US4] `.../excel/ExcelUploadIT.java` — #38(각 행이 지출·소득으로 저장되고 별도 "가계부" 행이 없다 — **업로드 이력도 저장하지 않는다**, FR-321)·#39(성공 응답이 **래퍼**다 — 파일을 돌려주지 않는다)·#45(같은 내용의 행이 두 번 → **둘 다 저장**, FR-309)·#47(업로드가 만든 행의 이름 스냅샷이 3.1·3.7 과 **같은 규칙**)·#48(업로드가 만든 지출 행의 할부 3컬럼이 **전부 NULL** — 양식에 할부 컬럼이 없다). **검증용 `.xlsx` 는 테스트가 POI 로 직접 만든다** — 고정 파일을 리소스에 두면 컬럼 정의가 바뀔 때 같이 안 바뀐다(plan.md)
- [ ] T045 [P] [US4] `.../excel/ExcelValidationIT.java` — #40(중간 한 행 오류 → `3502` + **전체 롤백** + `errors[]` 에 `row`·`column`·`message` — FR-320·SC-306)·#41(오류가 **여러 행**에 있으면 `errors[]` 에 **전부** 담긴다 — 첫 오류에서 멈추지 않는다)·#46(파일의 수단 이름이 DB 에 없으면 그 행을 오류로 보고 전체 롤백)·#49(**소득 행에 지출유형·장소가 채워져 있으면** 오류 → `3502`, FR-306). **#41 이 "오류를 전부 모은다"를 지킨다** — 첫 오류에서 멈추면 사용자가 파일을 여러 번 왕복하며 고치게 된다
- [ ] T046 [P] [US4] `.../excel/ExcelFileLevelIT.java` — #42(301행 → `3504`)·#43(`.xlsx` 가 아닌 파일 → `3503`)·#44(헤더만 있고 데이터 0행 → `3505`). **행 검증에 들어가기 전에 판정되는지 본다** — 300행 초과 파일에 행 오류가 섞여 있어도 `3502` 가 아니라 **`3504`** 가 나와야 한다(FR-319). 프론트가 "파일을 다시 고르세요"와 "표의 N행을 고치세요"를 다르게 안내해야 해서 코드를 나눠 두었다

### Implementation for User Story 4

- [ ] T047 [P] [US4] `.../backend/excel/ExcelColumn.java` 를 만든다 — A~G 7개 열의 **헤더 문자열·순서·필수 여부**를 한 곳에 담는다(excel-contract.md §1). **양식 생성과 업로드 파싱이 이것을 공유한다** — 두 곳에 각자 적으면 컬럼을 추가할 때 한쪽만 고치는 일이 생기고 FR-318("양식과 업로드의 컬럼 정의가 일치")이 깨진다. 공유하면 **일치가 구조로 보장된다**. **필수 여부가 A열 값에 따라 달라진다**: `EXPENSE` 면 E·F·G 가 전부 필수, `INCOME` 이면 E·F 는 **비어 있어야** 하고 G 는 선택이다
- [ ] T048 [P] [US4] `.../backend/excel/ExcelTemplateWriter.java` 를 만든다 — POI 로 `.xlsx` 를 만들고 D·E열에 `XSSFDataValidation` 으로 드롭다운을 넣는다. 목록은 **003 의 2.6·2.13 과 같은 필터**다 — 수단은 `purpose` 구분 **없이** 사용 중 전부(양식 A열이 지출·소득을 모두 담으므로), 지출유형은 사용 중 전부(excel-contract.md §2). **POI 타입(`Workbook`·`Row`·`Cell`)을 이 패키지 밖으로 내보내지 않는다**(원칙 II) — Service 는 평범한 DTO 만 받는다. T047 에 의존
- [ ] T049 [P] [US4] `.../backend/excel/ExcelRowReader.java` 를 만든다 — `.xlsx` 를 파싱해 **평범한 행 DTO 목록**을 돌려준다. 형식·행 수 판정에 필요한 정보(데이터 행 수)도 함께 낸다. 여기서 업무 검증을 하지 않는다 — 읽기와 검증을 나눠야 파일 단위(`3503`·`3504`·`3505`)와 행 단위(`3502`)의 판정 순서를 지킬 수 있다. T047 에 의존
- [ ] T050 [US4] `.../backend/service/ExcelTemplateService.java` 에 3.11 을 구현한다 — 회원의 사용 중 수단·지출유형을 읽어 `ExcelTemplateWriter` 에 넘긴다. **양식을 정적 파일로 미리 만들 수 없다** — FR-317 이 본인 목록을 요구하므로 **회원마다 내용이 다르다**. 생성 실패는 `9000` 이며 **래퍼로 나간다**. T048 에 의존
- [ ] T051 [US4] `.../backend/service/ExcelImportService.java` 에 3.12 를 구현한다 — 3단계다: **[1] 파일 단위**(`.xlsx` 아님 `3503` · 300행 초과 `3504` · 0행 `3505`) → **[2] 행 단위 전수 검증**(오류를 **전부 모아** `3502`, 아무것도 저장하지 않는다) → **[3] 저장**(한 트랜잭션, 스냅샷은 3.1·3.7 과 동일 규칙). **검증을 저장보다 앞에 완전히 끝낸다** — 저장하다 실패해 롤백하는 것이 아니다. 그래야 `errors[]` 에 전체 목록을 담을 수 있다(research.md §8). 수단·유형은 **이름으로** 찾는다(양식에 ID 가 없다). 할부 3컬럼은 **전부 NULL**. T010~T012·T049 에 의존
- [ ] T052 [US4] **수단 이름 중복을 어떻게 다룰지 정하고 `ExcelImportService` 에 반영한다**(excel-contract.md § 남은 판단) — 003 은 수단 이름에 유일 제약을 두지 않아 "국민카드"를 두 개 만들 수 있다. 업로드가 이름으로 찾으므로 **어느 쪽을 고를지 갈린다**. ① 첫 매치 ② 그 행을 `3502` 오류로 본다 중 하나를 고르고 **고른 이유를 코드 주석과 excel-contract.md 에 남긴다**. ②가 안전하지만 불친절하다 — 003 에 수단 이름 유일 제약을 넣을지와 함께 판단한다. **정하지 않고 두면 같은 파일이 실행마다 다른 수단에 붙는다**. T051 과 같은 파일이라 순차
- [ ] T053 [US4] `.../backend/controller/ExpenseIncomeExcelController.java` 를 만들고 3.11·3.12 를 붙인다 — `GET /api/v1/expense-incomes/excel/template`, `POST /api/v1/expense-incomes/excel/upload`(파트 이름 **`file`**). **3.11 은 `produces` 를 지정하지 않고** `ResponseEntity<byte[]>` 에 `Content-Type`·`Content-Disposition: attachment; filename="expense_income_template.xlsx"` 를 직접 싣는다. **3.12 는 예외가 아니므로 래퍼를 쓴다** — 003 의 `ExpendGroupIconController` 를 베끼지 말 것: 저쪽은 인증 실패도 래퍼를 벗지만 **3.11 은 인증 실패에 래퍼를 쓴다**(FR-322). `RestAuthEntryPoint` 에 이 경로를 **추가하지 않는다**. T050·T052 에 의존
- [ ] T054 [US4] `common-mod/.../logging/ApiLoggingAspect.java` 가 004 의 두 경우를 이미 덮는지 확인한다 — ① 3.11 응답의 `byte[]` → `resCode=binary(NB)` ② 3.12 요청의 `MultipartFile` → `MultipartFile{name=..., size=...}`. **003 의 T011 에서 이미 넣었으므로 코드 변경 없이 통과해야 한다**(api-contract.md §9). 통과하지 못하면 그때 고친다 — 엑셀 바이너리가 로그에 통째로 들어가면 로그 파일이 깨지고 용량이 폭증한다

**Checkpoint**: API 12건이 전부 선다. 3.11 의 두 갈래 응답이 계약대로 나온다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 스토리 경계를 넘는 검증과 완료 판정(T061 포함). quickstart.md §3 의 "응답 규격"과 §4 에 대응한다

- [ ] T055 `.../ExpenseIncomeResponseContractIT.java` — #50(12건 전부 호출해 **11건은 `{ resCode, data }`, 3.11 만 파일** — SC-301)·#51(비즈니스 실패는 HTTP 200 + 4자리 `resCode`). **예외를 인정하는 시험과 나머지를 규격으로 묶는 시험이 한 자리에 있어야 그 예외가 하나뿐임이 드러난다** — 003 의 `ExpendGroupResponseContractIT` 와 같은 형태다. **004 에는 목록 API 가 없으므로 `data.list` 검증은 하지 않는다**(월별 목록은 005 의 몫이다, api-contract.md §2)
- [ ] T056 로그를 **실측**한다 — 3.11 응답 로그에 `.xlsx` 바이너리가 찍히지 않는지(크기만), 3.12 요청 로그에 파일 내용이 아니라 파일명·크기만 남는지 `app-mod/money-backend-app/logs/money-backend-app.log` 에서 확인한다. T054 가 실제로 걸렸는지는 로그를 봐야 안다. `errors[]` 는 **찍어도 된다** — 사용자 입력 오류 위치라 민감정보가 아니다(excel-contract.md §6)
- [ ] T057 `git diff sql/schema-moneylogdb.sql` 이 **비어 있는지** 확인한다 — 이 기능은 스키마를 바꾸지 않는다. 덤프가 바뀌었다면 Entity 를 의도치 않게 건드린 것이므로 원인을 찾는다(원칙 VI). **특히 할부 3컬럼 불변식(T012·#33)을 DB CHECK 으로 해결하려는 시도가 있었는지 본다** — 그건 스키마 변경이고 이 기능의 범위 밖이다. 덤프 파일을 손으로 편집하지 않는다
- [ ] T058 `./gradlew :data-mod:test :app-mod:money-backend-app:test` 로 두 모듈을 돌리고 quickstart.md §3 의 시나리오 **51건**이 모두 `#N` 으로 대응되는지 대조한다. **002·003 의 테스트가 계속 통과해야 한다** — 004 는 002·003 이 만든 파일 **여섯**을 건드린다(T005 `application.yml` · T006·T064 `GlobalExceptionHandler` · T013 `AbstractApiIT` · T008 `UserExpenseRepository` · T062·T063 `UserPaymentMethodRepository`·`UserExpendGroupRepository`). **T005·T006·T064 가 회귀 위험이 가장 크다**: 003 의 아이콘 1MB 판정(quickstart #29·SC-211)이 서블릿 한도에 기대고 있었다면 그 시험이 먼저 깨지고, T064 는 002 가 만든 전역 핸들러의 **실패 변환 경로**를 건드려 002·003 의 모든 실패 응답에 영향이 간다. `./gradlew test`(전체)는 쓰지 않는다(`money-app` 레거시 3건이 깨져 있다)
- [ ] T059 커밋 전 자가 점검(CLAUDE.md) — ① 응답이 `{ resCode, data }` 인가(3.11 예외 1건은 명세가 승인했다) ② **Entity 가 노출되지 않는가** ③ 명세 표의 설명 칸이 비어 있지 않은가(`phase3-지출-소득/` — T001 확인 결과 현재 0곳) ④ DB 구조가 바뀌지 않았는가(T057) ⑤ `System.out.println` 이 없는가 ⑥ Controller 가 Repository 를 직접 부르지 않는가 ⑦ `PUT` 이 0건인가 ⑧ **POI 타입이 `excel` 패키지 밖으로 새지 않았는가**(`grep -rn 'org.apache.poi' app-mod/money-backend-app/src/main --include=*.java` 가 `backend/excel/` 안에만 나와야 한다)
- [X] T060 **quickstart.md §3 US1 표의 대응 오기 2건을 고친다** — `#11`(삭제 후 재조회, 물리 삭제)이 `SC-306` 으로, `#12`(같은 날짜·금액·수단 두 건 등록)가 `SC-303` 으로 적혀 있으나 **둘 다 다른 스토리의 SC 다**: SC-303 은 "할부 등록 중 실패를 주입하면 남는 행이 0건", SC-306 은 "오류 행이 포함된 엑셀을 올리면 저장된 행이 0건"이다. `#11` → `US1-7·FR-308`, `#12` → `US1-8·FR-309` 로 고친다. **그대로 두면 완료 판정에서 US1 만 통과시켜 놓고 SC-303·SC-306 이 검증됐다고 착각한다** — 두 SC 는 각각 T034(할부 롤백)·T045(엑셀 롤백)가 맡는다. **ID 는 뒤에 붙었지만 실행 위치는 T002 옆**이다(기존 59건의 상호 참조를 밀지 않으려고 번호만 이어 붙였다)
- [ ] T061 **FR-323 은 이 기능에서 구현하지 않는다는 것을 확인한다** — "고정지출 행은 004 가 만들지 않는다. `005-backend-ledger-fixed-expense` 가 소유한다". 004 가 쓰는 테이블은 `tbl_expense`·`tbl_income` 둘뿐이고 `tbl_fixed_expense`·`tbl_fixed_expense_monthly` 는 **건드리지 않는다**(data-model.md § 쓰는 테이블). 검증: `grep -rn 'FixedExpense' app-mod/money-backend-app/src/main --include=*.java` 가 **004 가 만든 파일에서 0건**이어야 한다(003 의 `PaymentMethodService` 가 `purpose` 변경 참조 검사로 쓰는 것은 남는다). FR-301~326 중 **대응 작업이 없는 유일한 항목**이라, 그 이유를 여기 남겨 두지 않으면 다음 사람이 누락으로 오해한다. **실행 위치는 Polish 다**
- [X] T062 [P] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserPaymentMethodRepository.java` 에 **양식 드롭다운용 조회**를 추가한다 — `List<UserPaymentMethod> findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(Long idKey)`. **기존 두 메서드로는 3.11 을 만들 수 없다**: `findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalseOrderByIdxAsc` 는 `purpose` 를 **필수로** 받는데 양식 A열이 지출·소득을 모두 담아 **purpose 구분 없이 사용 중 전부**가 필요하고(excel-contract.md §2), `findByUserIdKeyOrderByIdxAsc` 는 **삭제 표시된 수단까지** 돌려줘 FR-317("사용 중"만)을 어긴다. 지출유형 쪽은 003 이 만든 `UserExpendGroupRepository.findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc` 를 그대로 쓴다 — **추가할 것이 없다**. **ID 는 뒤에 붙었지만 실행 위치는 Foundational(T007 옆)이다**. T048·T050 이 이 조회에 의존한다
- [X] T063 [P] `data-mod/.../repository/UserPaymentMethodRepository.java`·`UserExpendGroupRepository.java` 에 **이름 기반 조회**를 추가한다 — 3.12 는 양식이 드롭다운으로 이름을 넣게 하므로 파일에 ID 가 없고 **이름으로 찾아야 한다**(excel-contract.md §4). 반환 타입이 갈리는 이유가 **DB 제약에 있다**:
      · 수단 → `List<UserPaymentMethod> findByUserIdKeyAndNameAndInUseTrueAndDeletedFalseOrderByIdxAsc(Long, String)`. **`List` 다** — 덤프에 수단 이름 유니크 제약이 **없어**("국민카드"를 두 개 만들 수 있다) 여러 건이 나올 수 있다. `List` 로 받으면 T052 가 ① 첫 매치 ② `3502` 오류 **어느 쪽을 고르든 시그니처를 바꾸지 않는다**
      · 지출유형 → `Optional<UserExpendGroup> findByUserIdKeyAndNameAndInUseTrueAndDeletedFalse(Long, String)`. **`Optional` 이다** — `ux_user_expend_group_name (id_key, name)` 유니크가 회원 안에서 이름을 하나로 강제한다. 기존 `existsByUserIdKeyAndName` 은 `boolean` 이라 **Entity 를 가져올 수 없어** 스냅샷·FK 를 채우지 못한다
      **ID 는 뒤에 붙었지만 실행 위치는 Foundational(T062 옆)이다**. T051·T052 가 이 조회에 의존한다
- [ ] T064 [US4] `.../backend/dto/response/ExcelImportResponse.java`·`ExcelRowError.java` 를 만들고 **`3502` 가 `errors[]` 를 실을 통로**를 낸다 — 성공은 `importedCount`·`expenseCount`·`incomeCount`·`message`, 실패는 `message`·`errors[]`(`row`·`column`·`message`)다. **통로가 지금 없다**: `BusinessException` 은 `ErrorCode` 와 문자열 하나만 들고 다니고 `RestResponseDto.fail(...)` 은 `Map<String,String>` 을 돌려줘 배열을 실을 자리가 없다. 헌장 원칙 III 이 "실패는 커스텀 예외로 던지고 전역 처리에 위임한다"고 정했으므로 **Controller 가 실패 응답을 직접 조립하지 않는다** — `common-mod` 에 상세를 들고 다니는 예외(예: `BusinessException` 을 상속해 `Object details` 를 갖는 것)와 `GlobalExceptionHandler` 의 분기를 더해 `{ resCode: 3502, data: { message, errors[] } }` 가 나가게 한다. **`ExcelRowError` 는 `money-backend-app` 에 두고 `common-mod` 는 타입을 모른 채 실어 보낸다** — 반대로 하면 공통 모듈이 004 를 알게 되어 원칙 I 의 단방향 의존이 깨진다. **ID 는 뒤에 붙었지만 실행 위치는 T050 앞**이며, `common-mod` 를 고치므로 **T058 의 002·003 회귀 확인 대상에 포함된다**. T045·T051 이 이 타입에 의존한다
- [ ] T065 [US4] **양식의 시트 이름과 설명 행을 어떻게 둘지 정하고 `ExcelColumn`·`ExcelRowReader` 에 반영한다**(excel-contract.md § 남은 판단 두 번째) — ① 헤더만 둔다 ② 사용법 안내 행을 추가한다(읽을 때 **건너뛰어야 한다**) 중 하나를 고르고 **고른 이유를 excel-contract.md 에 남긴다**. **정하지 않으면 T046 의 #44(헤더만 있고 데이터 0행 → `3505`)가 흔들린다** — 안내 행을 데이터로 세면 빈 양식이 `3505` 가 아니라 `3502` 로 나간다. 데이터 행 수 판정(FR-319 의 300행·0행)이 전부 이 결정에 매달려 있다. **ID 는 뒤에 붙었지만 실행 위치는 T047 앞이다**

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1, T001~T006)**: 의존 없음. **T001(명세 확인)·T002(계약 정정)는 코드보다 먼저** 한다(원칙 V). **T005·T006 은 짝이다** — 서블릿 한도를 올리면 003 의 예외 매핑이 함께 걸리므로 따로 하지 않는다
- **Foundational (Phase 2, T007~T013·T062·T063)**: Setup 완료 후. **모든 User Story 를 막는다**. 특히 T010·T011(`ReferenceResolver`)은 네 스토리 중 **셋**(US1·US2·US4)이 직접 쓴다
- **US1 (Phase 3, T014~T025)**: Foundational 완료 후. 다른 스토리에 의존하지 않는다
- **US2 (Phase 4, T026~T032)**: Foundational 완료 후. **US1 과 파일이 겹치지 않아 병행할 수 있다** — 지출과 소득은 자원이 갈린다
- **US3 (Phase 5, T033~T042)**: **US1 이 먼저** 서야 한다 — T042 가 `ExpenseController` 에 붙고, #25(할부 회차를 3.2 로 조회)가 US1 의 상세 조회를 부른다
- **US4 (Phase 6, T043~T054·T064·T065)**: **US1·US2 가 먼저** 서야 한다 — 업로드가 만드는 행의 규칙이 3.1·3.7 과 같아야 하고(excel-contract.md §4), 스냅샷·참조 검증을 그대로 재사용한다. US3(할부)에는 의존하지 않는다 — 양식에 할부 컬럼이 없다
- **Polish (Phase 7, T055~T059·T061)**: 인도하려는 스토리가 전부 끝난 뒤. **T060 은 번호만 뒤에 붙었고 실행 위치는 Setup(T002 옆)이다** — 문서 정정이라 코드보다 먼저 한다

### 파일 충돌로 순차가 강제되는 것

| 파일 | 순차로 묶이는 작업 |
|---|---|
| `UserExpenseRepository.java` | T007 → T008 → T009 |
| `UserPaymentMethodRepository.java` | T062 → T063 (003 이 만든 파일에 조회 추가) |
| `UserExpendGroupRepository.java` | T063 단독 (003 이 만든 파일에 조회 추가) |
| `ReferenceResolver.java` | T010 → T011 |
| `ExpenseService.java` | T022 → T023 → T024 |
| `ExpenseController.java` | T025 → T042 |
| `IncomeService.java` | T031 단독 |
| `InstallmentService.java` | T040 → T041 |
| `ExcelImportService.java` | T051 → T052 |
| `application.yml` | T005 단독 |
| `GlobalExceptionHandler.java` | T006 → T064 (003 이 만든 매핑을 고치고, 상세 실패 분기를 더한다) |
| `AbstractApiIT.java` | T013 단독 (002 가 만든 파일에 헬퍼 추가) |

### Within Each User Story

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- Repository → Service → DTO·Mapper → Controller 순
- 스토리를 끝내고 다음 우선순위로 넘어간다

### Parallel Opportunities

- Setup 의 T003·T004 가 동시에 가능하다. **T001·T002·T060(문서 확인·정정)만 코드 작업보다 먼저** 끝내면 된다
- Foundational 에서 T007(`UserExpenseRepository`)·T062·T063(`UserPaymentMethodRepository`·`UserExpendGroupRepository`)·T010(`ReferenceResolver`)·T012(불변식)·T013(테스트 바탕)이 서로 다른 파일이라 동시에 가능하다
- 각 스토리의 테스트 작성은 전부 `[P]` 다 — US1 4건, US2 2건, US3 5건, US4 4건
- **US1(지출)과 US2(소득)를 서로 다른 사람이 동시에** 진행할 수 있다. 파일이 겹치지 않는다
- DTO 는 대부분 `[P]` 다 — T018·T019·T020·T028·T029·T038·T039 는 각자 다른 파일이다
- `excel` 패키지는 T065(양식 구조 결정) → T047 → (T048 · T049) 순이며 T048·T049 가 병렬이다

---

## Parallel Example: User Story 1

```bash
# US1 의 통합 테스트 4건을 함께 작성한다:
Task: "ExpenseCreateIT — quickstart #1·#4·#5·#12·#13·#14"
Task: "ExpenseSnapshotIT — quickstart #2·#6·#7·#8 (FR-304 세 갈래)"
Task: "ExpenseReferenceLifecycleIT — quickstart #3·#9·#11 (FR-325 ↔ FR-326)"
Task: "ExpenseOwnershipIT — quickstart #10 (3202)"

# 그다음 DTO 3건을 함께 만든다:
Task: "ExpenseCreateRequest"
Task: "ExpenseResponse"
Task: "ExpenseDeleteResponse"
```

---

## Implementation Strategy

### MVP 우선

**US1(Phase 1~3, T001~T025)만으로 인도할 수 있다.** "쓴 돈을 적는다"는 가계부의 핵심 가치가 그것 하나로 성립한다. 소득·할부·엑셀은 전부 그 위에 얹는 것이다.

### 점진 인도

| 단계 | 누적 범위 | 인도되는 가치 |
|---|---|---|
| Setup + Foundational + US1 | 3.1~3.4 | 지출을 적고 고치고 지운다 (**MVP**) |
| + US2 | 3.7~3.10 | 월별 수지를 낼 재료가 갖춰진다 |
| + US3 | 3.5·3.6 | 카드 할부를 다룬다 |
| + US4 | 3.11·3.12 | 과거 내역을 옮겨 담는다 |
| + Polish | — | 규격·로그·스키마 무변경 확인 |

### 이 기능에서 가장 틀리기 쉬운 것 셋

구현 중 막히면 여기를 먼저 본다.

| # | 함정 | 지키는 장치 |
|---|---|---|
| 1 | **스냅샷을 수정 때마다 갱신한다** — 같은 수단을 유지한 채 금액만 고쳤는데 이름이 조용히 바뀐다 | T011 의 3갈래 판정 · 시험 #6·#7·#8 |
| 2 | **중도상환 경계를 `>=` 로 잡는다** — 오늘 결제된 회차가 사라져 이번 달 합계가 소급해 줄어든다 | T041 · 시험 #29(오늘 회차를 반드시 만든다) |
| 3 | **수정 경로에서 참조 검증을 무조건 돌린다** — 죽은 수단을 쓰던 과거 지출을 영영 못 고친다 | T011 의 omit 갈래 · 시험 #9 |

셋 다 **조용히 틀린다.** 예외도 오류 응답도 나지 않고 데이터만 어긋나므로, 위 시험들이 없으면 한참 뒤에 발견된다.
