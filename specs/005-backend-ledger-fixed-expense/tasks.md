---
description: "Task list for 005-backend-ledger-fixed-expense"
---

# Tasks: 고정지출 관리와 월별 가계부 목록

**Input**: `/specs/005-backend-ledger-fixed-expense/` 의 설계 산출물

**Prerequisites**: plan.md · spec.md · research.md · data-model.md · contracts/ (api-contract.md · monthly-lifecycle.md · ledger-list.md) · quickstart.md

**Tests**: 포함한다. quickstart.md §3 이 시나리오 **57건**(1~55 + 22-1·22-2)을 `#N` 으로 매기고 §4 가 그것을 완료 판정으로 쓰므로, 통합 테스트가 곧 인수 기준이다

**Organization**: User Story 단위로 묶어 각 스토리를 독립적으로 구현·검증·인도할 수 있게 한다

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완 작업에 의존하지 않음)
- **[Story]**: 이 작업이 속한 User Story (US1~US5)
- 설명에 정확한 파일 경로를 적는다

## Path Conventions

Spring Boot 멀티모듈이다. 저장소 루트 기준 경로를 쓴다.

- 백엔드 메인: `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/`
- 백엔드 테스트: `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/`
- 공통: `common-mod/src/main/java/com/dbdomino/moneylog/common/`
- 데이터: `data-mod/src/main/java/com/dbdomino/moneylog/data/`

**새 의존성이 없다.** 루트 `build.gradle` 을 건드리지 않는다. `sql/schema-moneylogdb.sql` 도 **변경 없음**이 이 기능의 전제다 — 001 이 두 테이블·유니크 제약·CASCADE·CHECK 을 이미 만들어 두었다.

## Phase 순서를 우선순위로 잡은 이유

spec.md 의 스토리 순서(US1~US5)가 아니라 **우선순위 순서**(P1 셋 → P2 둘)로 배열했다. US5(4.8 가계부 목록)가 P1 이면서 **사용자가 실제로 여는 화면**이고, US2 의 lazy 생성 위에만 얹히면 되어 US3·US4 를 기다릴 이유가 없다.

`Phase 3(US1) → Phase 4(US2) → Phase 5(US5) → Phase 6(US3) → Phase 7(US4)`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 착수 전 문서 확정. **T001 은 코드보다 반드시 먼저** 한다(헌장 원칙 V). 새 의존성·설정 변경은 없다

- [X] T001 `프로젝트설계/기능명세상세-백엔드/phase4-가계부/` 의 **4.1·4.2·4.3·4.4** 응답 설명에서 이름 2개를 **"현재 이름(스냅샷 아님)"으로 고친다** — FR-405 의 대상이 넷이다. 4.1·4.3 은 "스냅샷"이라 적고 있었고, 4.2 는 `list` 설명이 이름 필드를 아예 언급하지 않았으며, 4.4 는 "갱신된 FixedExpense" 한 줄뿐이라 원칙 V 의 "설명 칸은 그 칸만 보고 읽혀야 한다"에 미달이었다. 검증은 quickstart §0 의 `grep`(부정문 `스냅샷 아님` 을 걸러낸 뒤 0건). **빠뜨리면 구현자가 `tbl_fixed_expense` 에 이름 컬럼 2개를 추가하려 들고, 그건 스키마 무변경 전제를 깬다**
- [X] T001a 4.1 의 실패 표를 완성한다 — `3401` 조건이 "기간·결제일 검증 실패"뿐이라 **금액 0 이하와 수단 용도 불일치가 빠져 있었다**. api-contract §6 의 판정 순서(참조 `3003`/`3103` → 용도 `3401` → 값 `3401`)와 quickstart #11 이 갈 곳이 없다. `3003`·`3103` 조건도 "없음"에서 "없거나 타인 소유이거나 사용 중이 아님"으로 넓혔다. 4.4 에는 **자동 반영이 응답에 드러나지 않는 부작용**이라는 문단을 더했다(FR-412)
- [X] T002 `specs/005-backend-ledger-fixed-expense/contracts/ledger-list.md` § 남은 판단의 2건을 **결정으로 확정**한다 — ① `type` 필터가 걸렸을 때의 `expenseTotal`·`incomeTotal` 은 **필터 무관 전체 기준**(문서가 이미 ②를 권한다. 합계는 화면 상단 요약이고 필터는 목록을 좁히는 도구다), ② `keyword` 부분 일치는 **대소문자를 무시한다**. 표를 "남은 판단"에서 "정한 것"으로 바꾸고 §6 응답 형태 절에 한 줄로 반영한다 — 미정인 채 두면 T046 과 T054 가 서로 다른 답을 낸다
- [X] T003 `/speckit-analyze` 가 적용한 명세 개정 4건이 그대로 있는지 확인한다 — ① `spec.md` 에 **FR-426**(자동 생성은 참조의 사용 가능 여부를 다시 묻지 않는다)이 있고 FR-419 가 원칙 문장으로 좁혀졌는지, ② `contracts/monthly-lifecycle.md` §1 에 **"참조를 다시 묻지 않는다"** 절이 있는지, ③ `contracts/api-contract.md` §6 의 4.6 판정 4번에 **"사용 중인 수단이 아님"** 이 있는지, ④ `quickstart.md` 에 시나리오 **22-1·22-2** 가 있고 §4 완료 판정이 **SC-401~409 (9건)** 인지. **없으면 그 자리에서 되돌린 것이므로 원인을 찾는다** — 이 넷이 T010·T060 의 규칙을 정하는 근거다
- [X] T004 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java` 에 이 기능이 쓰는 코드 **5개**(`3401` `3402` `3403` `3405` `3501`)가 있고 **`3404` 가 결번 주석으로 남아 있는지** 확인한다 — 001·002 가 이미 넣어 둔 것으로 보이므로 **없는 것만 추가**한다. `3404` 는 폐기된 구 `FixedExpenseMonthlyOverrideUpsert` 의 코드이니 **되살려 쓰지 않는다**(api-contract §5)

**Checkpoint**: 설계 명세와 계약 문서가 구현과 어긋나지 않고, 에러코드가 서 있다

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story 가 공유하는 것. **여기가 끝나기 전에는 어떤 스토리도 시작할 수 없다**

- [X] T005 [P] `.../backend/support/YearMonthValue.java` 를 만든다 — `연 × 12 + 월` 합성 비교를 **한 곳에 가둔다**. `of(int year, int month)` · `value()` · `isAfter`/`isBefore` · `isWithin(start, end)` · `require(year, month, ErrorCode)`(범위 검증. 실패 코드를 호출자가 정한다 — 4.5·4.6·4.9 는 `3403`, 4.8 은 `3501`) · `paymentDateOn(int dayOfMonth)`(말일 보정한 `LocalDate`) · `firstDay()`/`lastDay()`(4.8 의 결제일 범위) · `contains(LocalDate)`(4.6 의 "다른 달" 판정). 이 비교가 **FR-404**(등록 검증) · **FR-408**(생성 대상 판정) · **FR-412**(자동 반영 범위) · **FR-413**(재작성 대상) 네 곳에 나오고, DB CHECK `ck_fixed_expense_period` 도 같은 식을 쓴다. **`java.time.YearMonth` 를 쓰지 않는다** — 저장 형태가 `year`·`month` INT 두 개라 변환이 곳곳에 생기고 DB CHECK 과의 대응이 흐려진다(research §4)
- [X] T006 [P] `.../backend/service/ReferenceResolver.java` 를 확장한다 — 004 의 `requireUsablePaymentMethod` 는 **용도 불일치를 `3003` 으로 뭉뚱그리지만**(존재 여부가 새어 나가는 것을 막으려고), 005 의 4.1·4.6 은 **용도 불일치를 `3401` 로 낸다**(api-contract §6: 수단 없음·타인 소유 → `3003`, purpose 불일치 → `3401`). 소유·사용 중까지만 보는 `requireOwnedUsablePaymentMethod(principal, id)` 와 용도만 보는 `requirePurpose(method, purpose, ErrorCode)` 를 더한다. **004 가 쓰는 기존 메서드의 시그니처·동작은 그대로 둔다**
- [X] T007 [P] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserFixedExpenseRepository.java` 를 확인·보강한다 — `findApplicableTo(idKey, yearMonth)`(합성값으로 기간 포함 판정) · `findByIdxAndUserIdKey` · `findByUserIdKeyOrderByIdxAsc` 는 001 이 이미 넣었다. **4.2 의 페이징용** `Page<UserFixedExpense> findByUserIdKey(Long, Pageable)` 하나만 더한다 — `totalCount` 는 `Page#getTotalElements()` 라 별도 count 메서드가 필요 없다
- [X] T008 [P] `data-mod/.../repository/UserFixedExpenseMonthlyRepository.java` 를 확인·보강한다 — `insertIfAbsent`(`INSERT ... ON CONFLICT DO NOTHING`, 감사 컬럼을 직접 채운다) · `findByUserIdKeyAndYearAndMonth` · `findByFixedExpenseIdxAndYearAndMonth` · `deleteByFixedExpenseIdxInAndYearAndMonth` 는 001 이 이미 넣었다. **FR-412 자동 반영용** "그 고정지출의 미래 달이면서 `modified=false` 인 행" 조회만 더한다
- [X] T009 [P] `data-mod/.../repository/UserExpenseRepository.java` · `UserIncomeRepository.java` 의 `findByUserIdKeyAndPaymentDateBetween` 이 4.8 이 쓸 수 있는 형태인지 확인한다 — **읽기 전용이며 004 가 쓰는 메서드를 바꾸지 않는다**. 그대로 쓸 수 있으면 추가 없이 넘어간다
- [X] T010 [P] `.../backend/service/FixedExpenseMonthlyFactory.java` 를 만든다 — **설정 1건 + 연·월 → 월별 내역 1행의 값**(금액·결제일·내용·수단·유형)을 만드는 **단 하나의 자리**다. 결제일은 T005 의 `paymentDateOn` 으로 **만들 때 한 번 말일 보정해 저장**한다(FR-409, 조회 때 다시 계산하지 않는다). **생성 지점이 셋**(4.5 FR-406 · 4.8 FR-418 · 4.9 FR-414)이라 각자 만들면 같은 달이 어느 API 로 처음 열렸는지에 따라 값이 갈린다
- [X] T010a [P] T010 의 팩토리에 **참조를 다시 묻지 않는다**를 못박는다(FR-426) — 설정의 `payment_method_idx`·`expend_group_idx` 를 **그대로 복사하고 사용 가능 여부를 검증하지 않는다**. **여기서 T006 의 `requireUsableExpendGroup`·`requireOwnedUsablePaymentMethod` 를 재사용하면 안 된다** — 003 의 지출유형 삭제는 일반 지출 참조만 보므로(`ExpendGroupService.delete`) 고정지출만 쓰던 유형은 막히지 않고 삭제 표시되고, 그러면 **평범한 달 조회(4.5·4.8)가 `3103` 으로 실패해 사용자가 그 달을 영영 열지 못한다**. 검증은 **사용자가 참조를 직접 고르는 경로**(T027 의 4.1 · T028 의 4.4 · T060 의 4.6)에만 건다. 이 대비를 클래스 javadoc 에 적는다(monthly-lifecycle.md §1)
- [X] T011 [P] `.../backend/dto/request/FixedExpenseListQuery.java` 를 만든다 — 4.2 의 `offset`·`limit` **필수** 검증. `limit <= 0` · `offset < 0` · `offset % limit != 0` 이면 **`9001`**. 002 의 `AdminMemberListQuery` 와 같은 규칙이므로 그 파일을 본떠 만든다(api-contract §3)
- [X] T012 `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/AbstractApiIT.java` 에 005 가 쓸 헬퍼를 더한다 — 고정지출 설정을 만드는 `createFixedExpense(token, ...)`, 월별 내역 행을 세는 `countMonthly(member, year, month)`, 한 행을 읽는 `monthlyRowOf(member, fixedExpenseId, year, month)`. **`jdbc` 갱신은 `tx.executeWithoutResult` 안에서** 한다(datasource 가 `auto-commit: false` 라 트랜잭션 밖 갱신은 커밋되지 않고 조용히 사라진다)
- [X] T013 [P] 테스트 패키지 4개의 공통 기반을 만든다 — `.../backend/fixedexpense/AbstractFixedExpenseIT.java` · `.../backend/monthly/AbstractMonthlyIT.java` · `.../backend/sync/AbstractSyncIT.java` · `.../backend/ledger/AbstractLedgerIT.java`. 모두 T012 의 `AbstractApiIT` 를 상속한다. **날짜 의존 시험이 썩지 않게** 지난 달·이번 달·미래 달을 `YearMonth.now()` 기준 상대값으로 잡는 헬퍼를 `AbstractSyncIT` 에 둔다(004 의 `startYearMonthSoThatTodayIs` 와 같은 처방)

**Checkpoint**: 합성 비교·행 생성 규칙·참조 검증·페이징 검증·시험 기반이 서고, 모든 스토리가 이 위에 얹힌다

---

## Phase 3: User Story 1 - 고정지출을 설정한다 (Priority: P1) 🎯 MVP

**Goal**: 고정지출의 기준값과 적용 기간을 등록(4.1)하고, 목록·상세를 보고(4.2·4.3), 고치고(4.4), 지운다(4.7)

**Independent Test**: 고정지출을 등록·조회·수정·삭제하고 목록을 조회한다. 월별 내역·가계부 없이 완결된다

### 테스트 (구현보다 먼저 쓰고 실패를 확인한다)

- [X] T014 [P] [US1] `.../backend/fixedexpense/FixedExpenseCreateIT.java` — quickstart #1·#2·#3·#4·#5·#11. **#1 이 이 스토리의 핵심**이다: 등록하면 **관리 행 1건만** 생기고 월별 내역은 **0건**이다(FR-402). #4(시작=종료 한 달짜리)와 #5(2026-12 시작 → 2027-01 종료)가 합성 비교의 경계다. #11 은 `purpose=INCOME` 수단으로 등록하면 `3401`
- [X] T015 [P] [US1] `.../backend/fixedexpense/FixedExpenseListIT.java` — quickstart #6·#7. `data.list` 가 object 배열이고 `offset`·`limit`·`totalCount` 가 **`list` 와 같은 레벨**이다(FR-422·FR-423). #7 은 `offset` 이 `limit` 의 배수가 아니면 `9001`
- [X] T016 [P] [US1] `.../backend/fixedexpense/FixedExpenseCurrentNameIT.java` — quickstart #8·#9 (SC-409 의 4.3 몫). **#9 가 004 와의 차이를 드러낸다**: 수단 이름을 바꾸고 같은 설정을 재조회하면 **새 이름이 나온다**. 004 의 지출은 스냅샷이라 바뀌지 않는데 005 의 설정은 바뀐다(FR-405)
- [X] T017 [P] [US1] `.../backend/fixedexpense/FixedExpenseOwnershipIT.java` — quickstart #10. 남의 설정 ID 로 4.3·4.4·4.7 을 부르면 전부 `3402` 다. **"없음"과 "타인 소유"를 같은 코드로 낸다**(FR-401)
- [X] T018 [P] [US1] `.../backend/fixedexpense/FixedExpenseDeleteIT.java` — quickstart #12 (SC-407). **삭제 전에 월별 내역을 몇 달치 만들어 두고** 삭제 후 그 고정지출의 월별 내역이 **0건**인지 본다(지난 달 포함). `ON DELETE CASCADE` 검증이며 **막는 조건이 없다** — 003 의 지출유형 삭제(사용 이력이 있으면 `3106` 으로 막음)와 다르다
- [X] T019 [P] [US1] `.../backend/fixedexpense/FixedExpenseUpdateIT.java` — 4.4 의 PATCH omit 규칙(보낸 필드만 갱신), 값 오류 `3401`, 소유자 `3402`. **자동 반영 범위(FR-412)의 검증은 US4 의 T063 이 맡는다** — 여기서는 설정 행 자체가 올바로 갱신되는지만 본다

### 구현

- [X] T020 [P] [US1] `.../backend/dto/request/FixedExpenseCreateRequest.java` — `name` · `paymentMethodId` · `expendGroupId` · `amount` · `paymentDayOfMonth` · `content` · `startYear` · `startMonth` · `endYear` · `endMonth`
- [X] T021 [P] [US1] `.../backend/dto/request/FixedExpenseUpdateRequest.java` — 004 의 `PatchFields` 를 써서 **omit 과 명시적 null 을 구분**한다. 전송한 필드만 갱신한다(api-contract §7)
- [X] T022 [P] [US1] `.../backend/dto/response/FixedExpenseResponse.java` — 설정 1건. `paymentMethodName`·`expendGroupName` 은 **현재 이름**이다(FR-405)
- [X] T023 [P] [US1] `.../backend/dto/response/FixedExpenseListResponse.java` — `list` + `offset` · `limit` · `totalCount` 를 **형제 필드**로 둔다
- [X] T024 [P] [US1] `.../backend/dto/response/FixedExpenseDeleteResponse.java` — 삭제 결과
- [X] T025 [US1] `.../backend/mapper/FixedExpenseMapper.java` — Entity ↔ DTO. **004 의 `ExpenseMapper` 와 정반대로 매핑한다**: 004 는 Entity 자신의 이름 컬럼에서 읽지만(스냅샷) 005 는 **연관(`paymentMethod.name`·`expendGroup.name`)에서 읽는다**(현재 이름). `tbl_fixed_expense` 에는 이름 컬럼이 아예 없다. 이 대비를 javadoc 에 적어 둔다 — 두 매퍼를 나란히 보는 사람이 한쪽을 다른 쪽에 맞추려 드는 것을 막는다
- [X] T026 [US1] `.../backend/service/FixedExpenseFieldRules.java` — 값 검증을 한 곳에 둔다. `paymentDayOfMonth` 1~31 · `amount > 0` · `startMonth`·`endMonth` 1~12 · 종료 연월이 시작보다 앞서지 않음(T005 의 합성 비교). 전부 **`3401`** 이다(FR-403·FR-404)
- [X] T027 [US1] `.../backend/service/FixedExpenseService.java` — 4.1 등록 · 4.2 목록 · 4.3 상세. 등록의 판정 순서는 **참조 검증(`3003`/`3103`) → 용도(`3401`) → 값 검증(`3401`) → 관리 행 1건 INSERT** 다(api-contract §6). **적용 기간 전체의 월별 내역을 만들지 않는다**(FR-402)
- [X] T028 [US1] `.../backend/service/FixedExpenseService.java` 에 4.4 수정 · 4.7 삭제를 더한다 — 4.4 는 `3402` → `3401` → UPDATE, 4.7 은 `3402` → DELETE(CASCADE 가 월별 내역을 지운다)
- [X] T029 [US1] `.../backend/service/FixedExpenseSyncService.java` 에 자동 반영 `propagate(fixedExpense)` 를 만들고 T028 의 4.4 가 부르게 한다 — **미래 달이면서 `modified=false` 인 행만** 갱신한다(FR-412). 미래 달은 **서버 기준 현재 연월을 초과**하는 달이고 **이번 달은 포함하지 않는다** — 포함하면 월세를 올렸을 때 사용자가 이미 본 이번 달 숫자가 소급해 바뀐다(004 의 중도상환 경계 `> today` 와 같은 성격). **월별 내역이 아직 없는 US2 이전에는 아무 일도 하지 않는 no-op 이고, 네 갈래 검증은 US4 의 T063 이 한다**
- [X] T030 [US1] `.../backend/controller/FixedExpenseController.java` — 4.1 `POST /api/v1/fixed-expenses` · 4.2 `GET` · 4.3 `GET /{fixedExpenseId}` · 4.4 `PATCH /{fixedExpenseId}` · 4.7 `DELETE /{fixedExpenseId}`. **Repository 를 직접 부르지 않고** Service 에만 의존한다(헌장 원칙 II)

**Checkpoint**: 고정지출 설정 CRUD 가 독립적으로 동작한다. **여기까지가 MVP** 다

---

## Phase 4: User Story 2 - 그 달의 고정지출 내역을 본다 (Priority: P1)

**Goal**: 연·월로 월별 고정지출 내역을 조회하고(4.5), 그 달을 처음 열면 설정에서 복사해 만든다

**Independent Test**: 적용 기간 중 한 달을 두 번 조회해 행이 늘지 않는지, 기간 밖 달은 비어 있는지 확인한다

### 테스트

- [ ] T031 [P] [US2] `.../backend/monthly/MonthlyLazyCreateIT.java` — quickstart #13·#16·#17·#20. #13 은 처음 조회 시 만들어져 **저장되고 함께 돌아온다**, #16 은 기간 **밖**이면 만들어지지 않는다(FR-408), #17 은 대상이 없으면 `list` 가 **빈 배열**, #20 은 `2026-11 ~ 2027-02` 의 2027-01 이 기간 안이다(해를 넘겨도)
- [ ] T032 [P] [US2] `.../backend/monthly/MonthlyIdempotencyIT.java` — quickstart #14 (SC-402). 같은 달을 **100번** 조회해도 행은 **1건**이다
- [ ] T033 [P] [US2] `.../backend/monthly/MonthlyConcurrencyIT.java` — quickstart #15 (SC-403). **이 기능에서 가장 중요한 동시성 시험**이다. 같은 달을 동시에 두 요청이 처음 열어도 행은 **1건**이고 **어느 쪽도 오류로 끝나지 않는다** — SC-403 의 뒷부분까지 단언한다. 한쪽이 유니크 위반으로 `9000` 을 내면 실패다. `data-mod` 의 `FixedExpenseMonthlyConcurrencyIT` 방식을 참고한다
- [ ] T034 [P] [US2] `.../backend/monthly/MonthlyPaymentDateIT.java` — quickstart #18·#19 (SC-404). 결제일 31 인 고정지출의 2026-02 내역 결제일이 **2026-02-28**, 2028-02 는 **2028-02-29**(윤년)다. **저장된 값**을 DB 에서 직접 읽어 확인한다 — 응답만 보면 조회 때마다 계산하는 구현도 통과해 버린다
- [ ] T034a [P] [US2] `.../backend/monthly/MonthlyCurrentNameIT.java` — quickstart **#22-1** (SC-409 의 4.5 몫). 수단·지출유형 이름을 바꾸고 같은 달 4.5 를 재조회하면 **둘 다 새 이름**이다. **SC-409 는 4.3·4.5·4.8 세 다리를 요구**하는데 T016 이 4.3, T044 가 4.8 을 덮으므로 이것이 남은 하나다 — 월별 내역에도 이름 컬럼이 없다는 사실을 확인하는 자리다(FR-405)
- [ ] T034b [P] [US2] `.../backend/monthly/MonthlyDeadReferenceIT.java` — quickstart **#22-2** (FR-426). 고정지출만 쓰던 지출유형을 삭제 표시한 뒤 다음 달을 **처음** 조회하면 **내역이 만들어진다**(`3103` 이 아니다). 수단을 사용 안 함으로 돌린 경우도 같이 건다. **T010a 가 지키는 지점**이며, 실패하면 사용자가 아무 잘못 없이 그 달을 열지 못한다
- [ ] T035 [P] [US2] `.../backend/monthly/MonthlyFilterOrderIT.java` — quickstart #21·#22. **한 쌍이다**: `paymentMethodId` 필터를 걸고 그 달을 **처음** 열면 그 달 대상 **전체**가 생성되고 결과만 좁혀지며(#21), 직후 필터 **없이** 같은 달을 열면 나머지가 **이미 있다**(#22). 순서를 뒤집으면 같은 달의 내역이 "언제 어떤 필터로 처음 열었는가"에 따라 달라진다(FR-406)

### 구현

- [ ] T036 [P] [US2] `.../backend/dto/request/FixedExpenseMonthlyListQuery.java` — `year`·`month` 필수(범위 오류는 **`3403`**), `paymentMethodId`·`expendGroupId` 선택 필터
- [ ] T037 [P] [US2] `.../backend/dto/response/FixedExpenseMonthlyResponse.java` — 월별 내역 1건. 이름 2개는 **현재 이름**이다
- [ ] T038 [P] [US2] `.../backend/dto/response/FixedExpenseMonthlyListResponse.java` — `list` + `year` · `month` · `total`(그 달 고정지출 합계)를 **형제 필드**로 둔다(api-contract §3)
- [ ] T039 [US2] `.../backend/mapper/FixedExpenseMonthlyMapper.java` — Entity ↔ DTO. T025 와 같이 **연관에서 현재 이름을 읽는다**
- [ ] T040 [US2] `.../backend/service/FixedExpenseMonthlyService.java` 의 lazy 생성 — 그 연·월에 걸리는 설정을 `findApplicableTo` 로 모으고, T010 의 팩토리로 값을 만들어 **`insertIfAbsent`(`ON CONFLICT DO NOTHING`)로 삽입한 뒤 다시 조회**한다. 애플리케이션의 "있으면 건너뛴다" 검사만으로 끝내지 않는다 — 두 트랜잭션이 같은 순간 "없음"을 보는 창이 열려 하나가 유니크 위반으로 실패한다(research §2)
- [ ] T041 [US2] `.../backend/service/FixedExpenseMonthlyService.java` 에서 **필터를 생성 뒤에** 적용한다(FR-406) — 생성을 전부 끝낸 다음 결과를 좁힌다. T035 가 지키는 지점이다
- [ ] T042 [US2] `.../backend/controller/FixedExpenseMonthlyController.java` — 4.5 `GET /api/v1/fixed-expenses/monthly`

**Checkpoint**: 설정이 그 달의 실제 금액으로 드러나고, 동시 조회에도 행이 1건이다

---

## Phase 5: User Story 5 - 한 달 가계부를 한 목록으로 본다 (Priority: P1)

**Goal**: 일반 지출·할부·소득·고정지출을 한 응답으로 합쳐 돌려준다(4.8)

**Independent Test**: 네 종류를 각각 한 건씩 만들고 월별 목록이 넷을 모두 담는지, `type` 필터가 갈라내는지 확인한다

**Dependency**: US2(T040)의 lazy 생성을 **그대로 재사용**한다(FR-418). 4.8 이 따로 만들면 두 API 가 만드는 결과가 갈린다

### 테스트

- [ ] T043 [P] [US5] `.../backend/ledger/LedgerAssembleIT.java` — quickstart #39·#40·#49·#52 (SC-408). 네 종류가 각 1건인 달을 조회하면 **4건**이 한 목록에 나오고 `type` 이 정확하다. #40 은 그 달 고정지출 내역이 없을 때 **4.5 와 같은 규칙으로 만들어 저장한 뒤** 목록에 넣는다. #49 는 `year`·`month`·`expenseTotal`·`incomeTotal` 이 `list` 와 **같은 레벨**, #52 는 페이징 필드가 **하나도 없다**(FR-422)
- [ ] T044 [P] [US5] `.../backend/ledger/LedgerNameRulesIT.java` — quickstart #42·#43·#44 (SC-409 의 4.8 몫). **#44 가 이 목록에서 가장 헷갈리는 지점**이다: 수단 이름을 바꾸고 같은 달을 재조회하면 **`FIXED` 행만 새 이름**이고 `EXPENSE`·`INCOME` 은 옛 이름이다. **한 응답에 두 이름 규칙이 섞인다**(FR-419·FR-425)
- [ ] T045 [P] [US5] `.../backend/ledger/LedgerFilterSortIT.java` — quickstart #45·#46·#47·#48. #45 는 `type=EXPENSE,INSTALLMENT` **콤마 복수 지정**, #46 은 `dateFrom`·`dateTo` 가 그 달 **안에서** 좁힌다, #47 은 `expendGroupId` 필터가 걸리면 **`INCOME` 행이 전부 빠진다**(소득에 지출유형이 없다), #48 은 `sort`·`order` 를 생략하면 `paymentDate` `desc`
- [ ] T046 [P] [US5] `.../backend/ledger/LedgerTotalsIT.java` — quickstart #50. `expenseTotal` 은 **일반 + 할부 + 고정** 합계다. T002 에서 확정한 대로 **`type` 필터가 걸려 있어도 전체 기준**임을 함께 단언한다
- [ ] T047 [P] [US5] `.../backend/ledger/LedgerItemIdIT.java` — quickstart #41·#51. `expense:{id}` · `income:{id}` · `fixed:{id}:{year}:{month}` 형식이고 원본 PK 는 `sourceId` 로 따로 실린다. **`FIXED` 만 세 조각인 이유**는 `fixedExpenseId` 만으로는 월별 1행이 특정되지 않기 때문이다(FR-424). #41 은 할부 행이 `type=INSTALLMENT` 이고 할부 그룹 식별자를 갖는다
- [ ] T048 [P] [US5] `.../backend/ledger/LedgerBoundaryIT.java` — quickstart #53·#54. **#53 이 lazy 생성의 경계를 드러낸다**: 이미 연 달에 고정지출을 **새로 등록**하고 재조회해도 **자동으로 따라오지 않는다**(4.9 를 먼저 불러야 한다). #54 는 연·월 오류가 **`3501`** 이다 — 4.5·4.6·4.9 의 `3403` 과 다르며 자원별 대역 규칙(`34xx` 고정지출 / `35xx` 가계부)의 결과다

### 구현

- [ ] T049 [P] [US5] `.../backend/dto/request/LedgerMonthlyListQuery.java` — `year`·`month` 필수(범위 오류는 **`3501`**), `type`(콤마 복수) · `paymentMethodId` · `expendGroupId` · `dateFrom` · `dateTo` · `keyword` · `sort`(`paymentDate`|`amount`, 기본 `paymentDate`) · `order`(`asc`|`desc`, 기본 `desc`)
- [ ] T050 [P] [US5] `.../backend/dto/response/LedgerItemResponse.java` — `ledgerItemId` · `type` · `sourceId` · `paymentDate` · `amount` · `paymentMethodName` · `expendGroupName` · `place` · `content` · 할부 식별자. **출처마다 없는 칸이 있다**(소득은 지출유형·장소가 없다)
- [ ] T051 [P] [US5] `.../backend/dto/response/LedgerMonthlyListResponse.java` — `list` + `year` · `month` · `expenseTotal` · `incomeTotal` 을 **형제 필드**로 둔다. **페이징 필드를 넣지 않는다**
- [ ] T052 [US5] `.../backend/service/ledger/LedgerItemFactory.java` — 네 종류를 각각 `LedgerItemResponse` 로 바꾸고 `ledgerItemId` 를 만든다. **이름 규칙이 여기서 갈린다**: `FIXED` 는 연관에서 현재 이름을, `EXPENSE`·`INSTALLMENT`·`INCOME` 은 Entity 의 이름 컬럼(스냅샷)을 읽는다
- [ ] T053 [US5] `.../backend/service/ledger/LedgerAssembler.java` — 네 출처를 **각각 조회한 뒤 애플리케이션에서 합친다**. SQL `UNION` 을 쓰지 않는다 — 컬럼 구성이 달라 없는 칸을 `NULL` 로 채우면 "값이 없다"와 "컬럼이 아예 없다"가 구분되지 않고, 고정지출 행의 이름은 조인해서 현재 값을 읽어야 한다(research §8). 일반 지출과 할부는 `installment_group_id` 의 유무로 가른다
- [ ] T054 [US5] `.../backend/service/ledger/LedgerAssembler.java` 에 필터·정렬을 더한다 — **조립 후에 적용한다**. `expendGroupId` 는 `INCOME` 행을 전부 떨어뜨리고, `keyword` 는 장소·내용을 보되 **소득에는 장소가 없으므로 내용만** 보며 **대소문자를 무시한다**(T002 확정). 정렬 기본은 `paymentDate desc`
- [ ] T055 [US5] `.../backend/service/LedgerService.java` — **lazy 생성을 먼저 전부 끝낸 뒤** 조립한다(FR-418, ledger-list §2). T040 의 서비스를 재사용하며 **필터가 생성 대상을 좁히지 않는다**. 합계는 T002 확정대로 **필터 무관 전체 기준**으로 낸다
- [ ] T056 [US5] `.../backend/controller/LedgerController.java` — 4.8 `GET /api/v1/ledger/monthly`

**Checkpoint**: 사용자가 실제로 여는 화면이 선다. **P1 세 스토리가 여기서 끝난다**

---

## Phase 6: User Story 3 - 한 달만 다르게 고친다 (Priority: P2)

**Goal**: 그 달의 금액·결제일·내용·수단을 직접 고친다(4.6)

**Independent Test**: 두 달치 내역을 만든 뒤 한 달만 고쳐 다른 달이 영향받지 않는지 확인한다

### 테스트

- [ ] T057 [P] [US3] `.../backend/monthly/MonthlyUpdateIT.java` — quickstart #23·#24·#29. 두 달치 중 한 달의 금액만 고치면 다른 달은 그대로이고(#23), 그 행이 `modified=true` 가 되며(#24), **이름·지출유형은 이 경로로 바뀌지 않는다**(#29) — 바꿀 수 있는 것은 금액·결제일·내용·수단 넷뿐이다(FR-410)
- [ ] T058 [P] [US3] `.../backend/monthly/MonthlyUpdateRejectIT.java` — quickstart #25·#26·#27·#28. #25 는 아직 만들어지지 않은 달이면 **`3405`**(lazy 생성 모델의 대가다 — 열어 본 적 없는 달은 고칠 수 없다), #26 은 **바꿀 필드를 하나도 안 보내면 `3401`**, #27 은 `paymentDate` 가 Path 의 연·월과 다른 달이면 `3401`, #28 은 `paymentMethodId` 가 `purpose=INCOME` 이면 `3401`. **#26 을 빠뜨리기 쉽다** — PATCH omit 규칙상 빈 Body 가 "아무것도 안 바꾼다"로 읽히지만 설계 명세는 이를 거절한다. 의미 없는 요청이 `modified=true` 만 세우는 것을 막는 장치다

### 구현

- [ ] T059 [P] [US3] `.../backend/dto/request/FixedExpenseMonthlyUpdateRequest.java` — `PatchFields` 로 omit 을 구분하고 **전부 omit 이면 `3401`**. 대상은 `amount`·`paymentDate`·`content`·`paymentMethodId` 넷뿐이다. **"전부 omit" 판정은 이 네 필드에 대해서만 한다** — 요청에 `name`·`expendGroupId` 같은 대상 밖 필드만 담겨 있으면 Jackson 이 그것들을 버리므로(`fail-on-unknown-properties` 는 기본 `false`) 네 필드가 전부 omit 이라 `3401` 이다. quickstart #29 와 #26 이 같은 답으로 이어지는 지점이다
- [ ] T060 [US3] `.../backend/service/FixedExpenseMonthlyService.java` 에 4.6 의 **판정 순서**를 넣는다 — `3402`(설정 없음·타인) → `3403`(Path 연·월 범위) → `3405`(그 연·월 내역 없음) → `3401`(값 검증). **순서가 결과를 바꾼다**(api-contract §6). 수단은 **본인 소유 · `purpose=EXPENSE` · 사용 중**(`in_use=true`, `deleted=false`)까지 전부 봐서 아니면 `3401` 을 낸다 — T006 의 `requireOwnedUsablePaymentMethod` + `requirePurpose(..., FIXED_EXPENSE_FIELD_INVALID)` 조합이다. **T010a 와 방향이 반대인 것에 주의한다**: 자동 생성은 사용 가능 여부를 묻지 않지만 4.6 은 **사용자가 수단을 직접 고르는 경로**라 죽은 수단으로 갈아타는 것을 막아야 한다(FR-426)
- [ ] T061 [US3] `.../backend/service/FixedExpenseMonthlyService.java` 에서 UPDATE 후 **`modified = true`** 로 표시한다 — 이 표시가 FR-412 의 자동 반영과 FR-414 의 ③보존을 가르는 유일한 근거다
- [ ] T062 [US3] `.../backend/controller/FixedExpenseMonthlyController.java` 에 4.6 을 더한다 — `PATCH /api/v1/fixed-expenses/monthly/{year}/{month}/{fixedExpenseId}`

**Checkpoint**: 설정과 월별을 나눈 이유가 실제로 쓰인다 — 한 달만 다르게 고칠 수 있다

---

## Phase 7: User Story 4 - 설정을 바꿨을 때 어디까지 따라가는가 (Priority: P2)

**Goal**: 설정 수정(4.4)의 자동 반영 범위를 검증하고, 수동 재작성(4.9)으로 다시 맞추는 경로를 만든다

**Independent Test**: 지난 달·이번 달·미래 달 내역을 만들고 그중 하나를 직접 수정한 뒤, 설정을 바꿔 어느 달이 따라가는지 확인한다

### 테스트

- [ ] T063 [P] [US4] `.../backend/sync/PropagationRangeIT.java` — quickstart #30·#31·#32·#33 (SC-405). **T029 가 만든 자동 반영의 네 갈래를 전부 건다**: 미래 달 `modified=false` 는 **따라가고**(#30), 미래 달 `modified=true` 는 **안 바뀌고**(#31), **지난 달**은 안 바뀌고(#32), **이번 달**도 안 바뀐다(#33). **#33 이 판단이 필요했던 지점**이다 — 포함하면 월세를 올렸을 때 이번 달 금액이 소급해 바뀐다
- [ ] T064 [P] [US4] `.../backend/sync/SyncRewriteIT.java` — quickstart #34·#36·#37. 지난 달을 재작성하면 관리 값으로 갱신되고(#34, FR-413), 응답에 **건수 4개 + 결과 목록**이 함께 오며(#36, FR-415), 적용 기간을 줄인 뒤 기간 밖 달을 재작성하면 그 행이 **삭제되고 `deletedCount` 에 잡힌다**(#37). **#37 이 ④를 검증한다** — 자동 반영은 값 갱신만 하고 삭제하지 않으므로 재작성이 그 정리를 맡는다
- [ ] T065 [P] [US4] `.../backend/sync/SyncOverwriteIT.java` — quickstart #35 (SC-406). `overwriteModified=true` 면 직접 수정분이 **관리 값으로 되돌아가고 `modified=false`** 가 된다. ③보존이 ②갱신으로 넘어가는 갈래다
- [ ] T066 [P] [US4] `.../backend/sync/SyncMethodIT.java` — quickstart #38. 4.9 는 연·월을 **Body 로만** 받는다. Query·Path 로 보내면 받지 않는다(FR-421)

### 구현

- [ ] T067 [P] [US4] `.../backend/dto/request/FixedExpenseMonthlySyncRequest.java` — `year` · `month` · `overwriteModified`(기본 `false`). **연·월을 Body 에 둔다**(FR-421). 범위 오류는 **`3403`**
- [ ] T068 [P] [US4] `.../backend/dto/response/FixedExpenseMonthlySyncResponse.java` — `list` + `year` · `month` · `createdCount` · `updatedCount` · `keptCount` · `deletedCount` 를 **형제 필드**로 둔다
- [ ] T069 [US4] `.../backend/service/FixedExpenseSyncService.java` 에 재작성의 **①생성 ②갱신**을 넣는다 — ① 기간에 걸리는데 그 연·월 행이 없으면 T010 의 팩토리로 만들고, ② 행이 있고 `modified=false` 면 관리 값으로 갱신한다(FR-414)
- [ ] T070 [US4] `.../backend/service/FixedExpenseSyncService.java` 에 **③보존 ④삭제**와 `overwriteModified` 를 더한다 — ③ 행이 있고 `modified=true` 면 그대로 두되 `overwriteModified=true` 면 ②로 넘기고 `modified` 표시를 내린다, ④ 행이 있는데 적용 기간이 그 연·월을 더는 포함하지 않으면 삭제한다
- [ ] T071 [US4] `.../backend/service/FixedExpenseSyncService.java` 의 재작성을 **한 트랜잭션**으로 묶고 결과 목록을 함께 돌려준다(FR-415) — 호출 후 재조회가 필요 없어야 화면이 한 번의 왕복으로 끝난다
- [ ] T072 [US4] `.../backend/controller/FixedExpenseMonthlyController.java` 에 4.9 를 더한다 — `POST /api/v1/fixed-expenses/monthly/sync`

**Checkpoint**: 자동과 수동의 경계가 서고, 지난 달도 설정대로 다시 맞출 수 있다

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 규격·로그·스키마와 문서의 최종 정합

- [ ] T073 [P] `.../backend/LedgerFixedExpenseResponseContractIT.java` — quickstart #55 (SC-401). **9건 전부가 `{ resCode, data }` 이고 래퍼 예외가 하나도 없다**. 003 은 아이콘(2.10), 004 는 엑셀 양식(3.11)이 예외였지만 **005 에는 없다** — 파일을 돌려주는 API 도, 본문 없는 응답도 없다. 004 의 `ExpenseIncomeResponseContractIT` 를 본떠 만들되 "예외를 인정하는 시험"은 두지 않는다. **토큰 없이 9건을 부르면 전부 `1001` 이고 그것도 래퍼**임을 함께 단언한다 — quickstart 에 미인증 시나리오가 따로 없어 여기가 유일한 자리다
- [ ] T074 [P] AOP 요청~응답 로깅이 005 의 3개 컨트롤러에 걸리는지 확인한다 — **제외 대상이 없다**(api-contract §9). 003·004 와 달리 바이너리 응답이 없어 로깅에서 뺄 API 가 하나도 없다. 컨트롤러마다 진입/종료 로그를 수동으로 쓰지 않는다(헌장 원칙 IV)
- [ ] T075 [P] `specs/005-backend-ledger-fixed-expense/plan.md` § Source Code 의 파일 목록이 **실제로 만든 클래스와 일치하는지** 확인한다 — `/speckit-analyze` 가 `FixedExpenseFieldRules`·`FixedExpenseMonthlyFactory`·query DTO 3종·`ReferenceResolver` 수정 표시를 이미 더했고 `FixedExpenseSyncService` 설명도 "자동 반영 + 재작성"으로 고쳤다. **구현하며 클래스를 더 쪼갰거나 이름을 바꿨다면 그 차이를 여기 반영한다** — plan 의 파일 목록이 실제와 갈리면 다음 기능의 착수 조사가 틀린 그림에서 시작한다
- [ ] T076 `git diff --stat sql/schema-moneylogdb.sql` 이 **비어 있는지** 확인한다 — 이 기능은 스키마를 바꾸지 않는다(헌장 원칙 VI). 덤프가 바뀌었다면 원인을 찾는다. 특히 **T001 의 명세 개정을 빠뜨려 `tbl_fixed_expense` 에 이름 컬럼을 추가하려는 시도가 있었는지** 본다 — 그건 이 기능의 전제를 깨는 변경이다
- [ ] T077 `./gradlew :data-mod:test` 를 돌려 **77건**이 그대로 통과하는지 확인한다 — 005 는 `data-mod` 의 Entity·제약을 바꾸지 않으므로 건수가 줄면 무언가를 건드린 것이다
- [ ] T078 `./gradlew :app-mod:money-backend-app:test` 를 돌려 **002~004 기존 시험과 005 신규 시험이 모두** 통과하는지 확인한다. **`./gradlew test`(전체)는 쓰지 않는다** — `money-app` 의 레거시 시험 3건이 `init` 커밋부터 깨져 있다
- [ ] T079 quickstart.md §4 완료 판정 표의 전 항목과 plan.md § Constitution Check 의 헌장 게이트 6개를 훑는다 — 시나리오 1~55 와 22-1·22-2(**57건**), SC-401~409, 동시성(#15 의 뒷부분), 말일 보정(#18·#19 윤년), SC-409 의 세 다리(#9·**#22-1**·#44), 참조 사후 무효화(**#22-2**), 자동 반영 4갈래(#30~#33), 재작성 4처리(#34·#35·#37), 두 이름 규칙 공존(#44). **Complexity Tracking 에 적을 위반이 없다** — 005 에는 래퍼 예외도 로깅 제외도 없다

---

## Dependencies & Execution Order

### Phase 의존

```text
Phase 1 (Setup)
   └─> Phase 2 (Foundational)
          └─> Phase 3 (US1, P1)  ── MVP
                 └─> Phase 4 (US2, P1)
                        ├─> Phase 5 (US5, P1)
                        ├─> Phase 6 (US3, P2)
                        └─> Phase 7 (US4, P2)
                               └─> Phase 8 (Polish)
```

- **T001 이 모든 코드 작업보다 먼저**다(헌장 원칙 V). T002 도 T046·T054 보다 앞서야 한다
- **Phase 2 가 끝나기 전에는 어떤 스토리도 시작할 수 없다** — T005(합성 비교)·T010(행 생성 규칙)이 네 스토리에 전부 걸린다
- **US1 → US2 는 진짜 의존**이다. 월별 내역을 만들 원본(설정)이 없으면 US2 가 성립하지 않는다
- **US2 → US5·US3·US4 도 진짜 의존**이다. US5 는 lazy 생성을 재사용하고(FR-418), US3 는 고칠 행이 있어야 하고, US4 는 반영할 행이 있어야 한다
- **US5·US3·US4 는 서로 독립**이다. US2 가 끝난 뒤 셋을 병렬로 진행할 수 있다

### User Story 안의 순서

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- DTO·Mapper → Service → Controller 순. Repository 는 Phase 2 에서 이미 섰다

### Parallel Opportunities

- Setup 의 T001~T004 는 서로 다른 파일이라 함께 할 수 있으나, **T001 만은 코드 작업 전에 끝낸다**
- Foundational 에서 T005·T006·T007·T008·T009·T010·T011·T013 이 병렬이다. **T012(`AbstractApiIT` 수정)만 단독**이다 — T013 의 네 Abstract 가 이 파일을 상속하므로 T012 가 먼저다
- 각 스토리의 테스트 작성은 전부 `[P]` 다 — US1 6건, US2 **7건**(T034a·T034b 포함), US5 6건, US3 2건, US4 4건
- **T010a 는 T010 과 같은 파일**이라 `[P]` 로 표시했어도 T010 다음에 이어서 한다
- DTO 는 대부분 `[P]` 다 — T020~T024 · T036~T038 · T049~T051 · T067·T068 이 각자 다른 파일이다
- **US2 가 끝나면 US5·US3·US4 를 서로 다른 사람이 동시에** 진행할 수 있다. 다만 T062(4.6)와 T072(4.9)가 `FixedExpenseMonthlyController.java` 를 함께 건드리므로 그 둘만 순차로 한다
- Polish 의 T073·T074·T075 가 병렬이다. T076~T079 는 순차다

---

## Parallel Example: User Story 5

```bash
# US5 의 통합 테스트 6건을 함께 작성한다:
Task: "LedgerAssembleIT — quickstart #39·#40·#49·#52 (SC-408)"
Task: "LedgerNameRulesIT — quickstart #42·#43·#44 (한 응답에 두 이름 규칙)"
Task: "LedgerFilterSortIT — quickstart #45·#46·#47·#48"
Task: "LedgerTotalsIT — quickstart #50 (일반+할부+고정)"
Task: "LedgerItemIdIT — quickstart #41·#51 (fixed 만 세 조각)"
Task: "LedgerBoundaryIT — quickstart #53·#54 (3501)"

# 그다음 DTO 3건을 함께 만든다:
Task: "LedgerMonthlyListQuery"
Task: "LedgerItemResponse"
Task: "LedgerMonthlyListResponse"
```

---

## Implementation Strategy

### MVP 우선

**US1(Phase 1~3, T001~T030)만으로 인도할 수 있다.** "매달 나가는 돈 목록"이라는 가치가 설정 CRUD 하나로 성립한다. 월별 내역·가계부 없이 완결된다.

### 점진 인도

| 단계 | 누적 범위 | 인도되는 가치 |
|---|---|---|
| Setup + Foundational + US1 | 4.1~4.4 · 4.7 | 고정지출을 설정하고 고치고 지운다 (**MVP**) |
| + US2 | 4.5 | 설정이 그 달의 실제 금액으로 드러난다 |
| + US5 | 4.8 | **사용자가 실제로 여는 화면**이 선다 — 한 달을 한 목록으로 본다 |
| + US3 | 4.6 | 한 달만 다르게 고친다 |
| + US4 | 4.9 | 설정 변경의 반영 범위가 정해지고 지난 달도 다시 맞춘다 |
| + Polish | — | 규격·로그·스키마 무변경 확인 |

### 이 기능에서 가장 틀리기 쉬운 것 다섯

구현 중 막히면 여기를 먼저 본다. 1~4 는 **조용히 틀린다** — 예외도 오류 응답도 나지 않고 데이터만 어긋난다. 5 는 시끄럽게 틀리지만 **원인이 엉뚱한 곳에 있다.**

| # | 함정 | 지키는 장치 |
|---|---|---|
| 1 | **고정지출에도 이름 스냅샷을 둔다** — 004 를 그대로 따라 하다 `tbl_fixed_expense` 에 이름 컬럼을 추가하려 든다 | T001 의 명세 개정 · T025 의 매퍼 주석 · T076 의 스키마 무변경 확인 |
| 2 | **애플리케이션 검사만으로 중복 생성을 막는다** — 두 트랜잭션이 같은 순간 "없음"을 보고 하나가 유니크 위반으로 죽는다 | T040 의 `insertIfAbsent` · 시험 #15(SC-403 의 **뒷부분**까지) |
| 3 | **자동 반영에 이번 달을 포함한다** — 월세를 올렸더니 사용자가 이미 본 이번 달 금액이 소급해 바뀐다 | T029 의 `> 현재 연월` 경계 · 시험 #33 |
| 4 | **필터를 생성 대상에도 적용한다** — 같은 달의 내역이 "언제 어떤 필터로 처음 열었는가"에 따라 달라진다 | T041 · T055 · 시험 #21·#22 (한 쌍) |
| 5 | **생성 경로에서 참조를 다시 검증한다** — 삭제 표시된 유형을 쓰던 고정지출 때문에 **평범한 달 조회가 `3103` 으로 죽는다.** 조회가 실패하는데 원인은 몇 주 전의 유형 삭제다 | T010a 의 "다시 묻지 않는다" · 시험 #22-2 (FR-426) |

**3·5 가 짝을 이룬다.** 둘 다 "어느 경로가 무엇을 다시 판단해도 되는가"의 문제다 — 자동으로 일어나는 일(생성·반영)은 사용자가 이미 정한 것을 그대로 펼치고, 사용자가 값을 직접 고르는 경로(4.1·4.4·4.6)만 새로 검증한다.

### 연·월 오류 코드가 API 마다 다르다

`3403`(4.5·4.6·4.9)과 `3501`(4.8)은 **같은 뜻인데 코드가 다르다.** `_공통.md` 의 자원별 코드 블록 배정(고정지출 `34xx` / 가계부 목록 `35xx`) 때문이며 **의도된 차이**다. 006 의 통계는 또 다른 `3603` 을 쓴다. 실수처럼 보여 통일하고 싶어지지만 통일하면 규칙이 깨진다.
