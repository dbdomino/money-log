---

description: "Task list for 001-backend-db-schema"
---

# Tasks: 백엔드 API를 지탱하는 DB 테이블 구성

**Input**: Design documents from `/specs/001-backend-db-schema/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: 포함한다. plan.md의 Testing이 `@SpringBootTest` 통합 테스트를 지정했고, quickstart.md §3이 검증 시나리오 20건을 확정했다. 각 스토리의 테스트는 그 스토리의 제약이 **실제로 DB에서 강제되는지**를 확인하는 것이지 애플리케이션 로직을 검사하는 것이 아니다.

**Organization**: 작업은 spec.md의 User Story 5개로 묶었다. 각 스토리는 Entity → Repository → 보조 DDL → 반영·검증 순으로 완결된다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 미완료 작업에 의존하지 않음)
- **[Story]**: 해당 User Story (US1~US5)
- 모든 작업에 정확한 파일 경로를 적는다

## Path Conventions

이 기능은 **멀티모듈 백엔드의 영속 계층**만 다룬다(plan.md Structure Decision).

- Entity·Repository·설정: `data-mod/src/main/java/com/dbdomino/moneylog/data/`
- 검증 테스트: `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/`
- 보조 DDL·덤프: `sql/`
- 스키마 반영 기동: `app-mod/money-backend-app`

Controller·Service·DTO는 이 기능에서 만들지 않는다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 패키지 구조와 빌드 의존성 준비

- [ ] T001 `data-mod/src/main/java/com/dbdomino/moneylog/data/` 아래에 `entity/`, `repository/`, `config/` 디렉터리를 만들고 각각 `package-info.java`를 둔다
- [ ] T002 [P] `data-mod/build.gradle`에 `spring-boot-starter-data-jpa`와 PostgreSQL 드라이버 의존성이 있는지 확인하고 없으면 추가한다
- [ ] T003 [P] `data-mod/build.gradle`에 테스트 의존성(`spring-boot-starter-test`, JUnit 5)을 확인하고 `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/` 디렉터리를 만든다
- [ ] T004 `sql/04_constraints.sql`을 멱등 골격으로 새로 만든다 — 파일 머리에 "이 스크립트는 반복 실행된다. 모든 문장은 `IF NOT EXISTS` 또는 존재 확인 후 `ALTER`여야 한다"는 규칙 주석과 CHECK 추가용 `DO $$ ... EXCEPTION WHEN duplicate_object THEN NULL; END $$;` 패턴 예시를 넣는다

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 15개 Entity 전부가 상속·참조하는 공통 기반. 회원 테이블은 나머지 14개 테이블의 FK 대상이라 여기 둔다

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 어떤 User Story도 시작할 수 없다

- [ ] T005 `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/BaseAuditEntity.java`에 `@MappedSuperclass` 공통 상위 클래스를 만든다 — `created_at`, `updated_at`, `created_by`, `updated_by` 4컬럼. `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy`. **기본키는 넣지 않는다**(`tbl_user`만 PK 이름이 `id_key`라서). 규칙 출처: contracts/naming-and-constraints.md §2
- [ ] T006 `data-mod/src/main/java/com/dbdomino/moneylog/data/config/JpaAuditingConfig.java`에 `@EnableJpaAuditing`과 `AuditorAware<Long>` 빈을 만든다. 인증 필터가 아직 없으므로 **빈 `Optional`을 반환하는 임시 구현**으로 두고, Phase 1(회원·인증) 구현에서 `SecurityContext`의 `id_key`를 공급하도록 바꾼다는 TODO 주석을 남긴다
- [ ] T007 `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/User.java`에 `tbl_user` Entity를 만든다 — PK `id_key`(BIGINT IDENTITY), `user_id`(VARCHAR(20) NOT NULL), `pw`, `nickname`, `email`(nullable), `phone`(nullable), `intro`(nullable), `role`(SMALLINT DEFAULT 3), `active`(BOOLEAN DEFAULT true). `BaseAuditEntity` 상속. **`created_by`만 nullable로 재정의**한다(가입 시점에 자기 `id_key`가 없음). 컬럼 상세: data-model.md §1
- [ ] T008 `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserRepository.java`를 만든다 — `findByUserId`, `existsByUserId`, `existsByEmail`
- [ ] T009 `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/AbstractSchemaIT.java`에 검증 테스트 공통 기반을 만든다 — `@SpringBootTest`, `@ActiveProfiles("postgresql")`, 회원 1건을 만드는 헬퍼, 제약 위반을 `DataIntegrityViolationException`으로 잡는 assert 헬퍼, `created_by`/`updated_by`를 명시적으로 채우는 헬퍼(임시 `AuditorAware`가 값을 주지 않으므로 필요)

**Checkpoint**: 회원 테이블과 감사 기반이 생겼다. US1~US5를 시작할 수 있다

---

## Phase 3: User Story 1 - 회원·세션·로그인 이력을 저장할 수 있다 (Priority: P1) 🎯 MVP

**Goal**: 회원가입·로그인·토큰 갱신·로그아웃·관리자 회원 관리(1.1~1.16)가 쓸 저장 구조를 완성한다. 회원 1명에게 폐기되지 않은 세션은 언제나 1건뿐이고, 폐기된 세션은 흔적으로 남는다

**Independent Test**: 회원 1건을 저장하고 세션을 만든 뒤, 같은 회원으로 두 번째 세션을 만들면서 첫 세션을 폐기 처리하면 활성 세션이 1건만 남는지 확인한다

### Tests for User Story 1

> 먼저 작성하고, 구현 전에 **실패하는 것**을 확인한다

- [ ] T010 [P] [US1] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/UserConstraintIT.java` — 같은 `user_id` 2건은 실패(quickstart #1), `email = NULL` 2건은 둘 다 성공(#2), 같은 이메일 값 2건은 실패(#3)
- [ ] T011 [P] [US1] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/UserSessionConstraintIT.java` — 한 회원에 `revoked = false` 세션 2건은 실패(#4), 첫 세션을 폐기(해시 NULL + `revoked = true`)한 뒤 새 세션 INSERT는 성공하고 활성 1건만 남음(#5)
- [ ] T012 [P] [US1] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/UserCheckConstraintIT.java` — `role = 2`로 INSERT 시 CHECK 위반(#17)

### Implementation for User Story 1

- [ ] T013 [P] [US1] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserSession.java`에 `tbl_user_session` Entity를 만든다 — `session_id`(UUID NOT NULL), `access_token_hash`(nullable), `refresh_token_hash`(nullable), `access_expires_at`, `refresh_expires_at`, `revoked`(DEFAULT false), `last_accessed_at`(nullable), `id_key` FK RESTRICT. 컬럼 상세: data-model.md §2
- [ ] T014 [P] [US1] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserLoginHistory.java`에 `tbl_user_login_history` Entity를 만든다 — `login_at`, `login_ip`(VARCHAR(45), nullable), `id_key` FK RESTRICT, `@Table(indexes = @Index(name = "ix_user_login_history_at", columnList = "id_key, login_at"))`. 상세: data-model.md §3
- [ ] T015 [P] [US1] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserSessionRepository.java`를 만든다 — `findBySessionId`, `findByIdKeyAndRevokedFalse`
- [ ] T016 [P] [US1] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserLoginHistoryRepository.java`를 만든다 — `findByIdKeyOrderByLoginAtDesc`
- [ ] T017 [US1] `sql/04_constraints.sql`에 회원·세션 제약을 추가한다 — 부분 유니크 `ux_user_email`(`WHERE email IS NOT NULL`), `ux_user_session_active`(`WHERE revoked = false`), CHECK `ck_user_role`(`role IN (1,3)`). Hibernate가 부분 유니크를 만들지 못하므로 여기 있어야 한다. 정의: contracts/naming-and-constraints.md §4·§5
- [ ] T018 [US1] 스키마를 반영하고 검증한다 — `./gradlew :app-mod:money-backend-app:bootRun`으로 기동해 테이블 생성 확인 후 종료, `psql -f sql/04_constraints.sql` 적용, T010~T012가 통과하는지 실행

**Checkpoint**: 회원·세션·로그인 이력 저장 구조가 독립적으로 동작·검증된다. Phase 1 API 구현에 필요한 저장 구조가 갖춰졌다

---

## Phase 4: User Story 2 - 지출·소득 수단과 지출유형을 저장할 수 있다 (Priority: P2)

**Goal**: 수단·지출유형 CRUD(2.1~2.13)가 쓸 저장 구조를 완성한다. 둘 다 삭제 표시로 관리되어 과거 데이터의 참조가 끊기지 않는다

**Independent Test**: 한 회원의 수단·지출유형을 등록·수정·삭제 표시한 뒤, "사용 중이고 삭제되지 않은 것만" 골라내는 조회와 "삭제된 것까지 전부" 조회가 서로 다른 결과를 내는지 확인한다

### Tests for User Story 2

- [ ] T019 [P] [US2] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/ExpendGroupConstraintIT.java` — 같은 회원·같은 이름 지출유형 2건은 실패, 다른 회원은 같은 이름 성공(quickstart #7). 삭제 표시된 유형과 같은 이름도 실패하는지 확인(research §5 — 부분 유니크를 쓰지 않는다)
- [ ] T020 [P] [US2] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/SoftDeleteIT.java` — 수단·지출유형을 `deleted = true`로 UPDATE해도 행이 남고 다시 읽히는지 확인
- [ ] T021 [P] [US2] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/PaymentMethodConstraintIT.java` — `type`이 `CARD`/`ACCOUNT` 밖이면 CHECK 위반, `purpose`가 `EXPENSE`/`INCOME` 밖이면 CHECK 위반

### Implementation for User Story 2

- [ ] T022 [P] [US2] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserPaymentMethod.java`에 `tbl_user_payment_method` Entity를 만든다 — `name`, `type`, `purpose`, `in_use`(DEFAULT true), `card_expiry`(CHAR(7) nullable), `deleted`(DEFAULT false), `id_key` FK RESTRICT, 인덱스 `ix_user_payment_method_active`(`id_key, purpose, in_use, deleted`). 상세: data-model.md §4
- [ ] T023 [P] [US2] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserExpendGroup.java`에 `tbl_user_expend_group` Entity를 만든다 — `name`, `in_use`(DEFAULT true), `default_group`(DEFAULT false), `icon_filename`(nullable), `deleted`(DEFAULT false), `id_key` FK RESTRICT, UNIQUE `ux_user_expend_group_name`(`id_key, name`), 인덱스 `ix_user_expend_group_active`(`id_key, in_use, deleted`). 상세: data-model.md §5
- [ ] T024 [P] [US2] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserPaymentMethodRepository.java`와 `UserExpendGroupRepository.java`를 만든다 — 사용 중 목록 조회(`findByIdKeyAndPurposeAndInUseTrueAndDeletedFalse`, `findByIdKeyAndInUseTrueAndDeletedFalse`), 이름 중복 확인(`existsByIdKeyAndName`)
- [ ] T025 [US2] `sql/04_constraints.sql`에 수단 CHECK 2건을 추가한다 — `ck_payment_method_type`, `ck_payment_method_purpose`
- [ ] T026 [US2] 스키마를 반영하고 T019~T021을 실행해 통과를 확인한다

**Checkpoint**: 지출·소득이 참조할 두 축이 준비됐다. US3·US4·US5의 목표금액이 이 단계에 의존한다

---

## Phase 5: User Story 3 - 지출·소득과 할부 내역을 저장할 수 있다 (Priority: P3)

**Goal**: 지출·소득 CRUD와 할부 일괄 등록·중도상환·엑셀 일괄 등록(3.1~3.12)이 쓸 저장 구조를 완성한다. 등록 당시 수단·유형 이름이 각 건에 보존된다

**Independent Test**: 일시불 지출 1건, 12개월 할부 1건(12행), 소득 1건을 저장한 뒤, 할부 12행이 같은 그룹 식별자를 공유하고 1~12 순번을 갖는지, 결제일이 오늘보다 뒤인 회차만 골라 제거할 수 있는지 확인한다

**Depends on**: US2 (수단·지출유형 FK 대상)

### Tests for User Story 3

- [ ] T027 [P] [US3] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/ExpenseSnapshotIT.java` — 지출 저장 뒤 원본 수단 이름을 바꿔도 `payment_method_name`이 등록 당시 값 그대로인지, 수단을 `deleted = true`로 해도 이름이 읽히는지(quickstart #6)
- [ ] T028 [P] [US3] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/InstallmentIT.java` — 같은 `installment_group_id`로 12행 INSERT, `installment_index` 1~12·`installment_total` 12 확인(#9). 할부 3컬럼이 모두 NULL인 일시불 행도 함께 저장되는지 확인
- [ ] T029 [P] [US3] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/InstallmentSettleIT.java` — `WHERE installment_group_id = ? AND payment_date > CURRENT_DATE` 삭제 후 오늘·과거 결제일 회차가 남는지(#10)

### Implementation for User Story 3

- [ ] T030 [P] [US3] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserExpense.java`에 `tbl_user_expense` Entity를 만든다 — `payment_method_idx` FK RESTRICT, `payment_method_name`, `amount`, `payment_date`, `place`, `content`, `expend_group_idx` FK RESTRICT, `expend_group_name`, `installment_group_id`/`installment_index`/`installment_total`(전부 nullable), 인덱스 `ix_user_expense_date`(`id_key, payment_date`)·`ix_user_expense_installment`(`installment_group_id, payment_date`). 상세: data-model.md §6
- [ ] T031 [P] [US3] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserIncome.java`에 `tbl_user_income` Entity를 만든다 — `payment_method_idx` FK RESTRICT, `payment_method_name`, `amount`, `payment_date`, `content`(nullable), 인덱스 `ix_user_income_date`(`id_key, payment_date`). 장소·지출유형·할부 컬럼은 **없다**. 상세: data-model.md §7
- [ ] T032 [US3] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserExpenseRepository.java`와 `UserIncomeRepository.java`를 만든다 — 월 범위 조회(`findByIdKeyAndPaymentDateBetween`), 할부 그룹 조회, 중도상환용 `deleteByInstallmentGroupIdAndPaymentDateAfter`, 지출유형 사용 이력 확인용 `existsByExpendGroupIdx`(2.12의 `3106` 판정에 쓰인다)
- [ ] T033 [US3] `sql/04_constraints.sql`에 시퀀스와 CHECK를 추가한다 — `seq_installment_group`(`CREATE SEQUENCE IF NOT EXISTS`), `ck_expense_amount`(`> 0`), `ck_expense_installment_index`(`IS NULL OR >= 1`), `ck_expense_installment_total`(`IS NULL OR >= 2`), `ck_income_amount`(`> 0`)
- [ ] T034 [US3] 스키마를 반영하고 T027~T029를 실행해 통과를 확인한다

**Checkpoint**: 가계부 핵심 데이터가 저장 가능하다. 고정지출을 섞지 않는 구조가 성립했다

---

## Phase 6: User Story 4 - 고정지출 관리와 월별 고정지출 내역을 분리해 저장할 수 있다 (Priority: P4)

**Goal**: 고정지출 관리 CRUD(4.1~4.4·4.7), 월별 내역 조회·단건 수정·수동 재작성(4.5·4.6·4.9), 월별 가계부 목록(4.8)이 쓸 저장 구조를 완성한다. 관리·월별 내역·월별 수입/지출이 서로 다른 테이블에 있다

**Independent Test**: 고정지출 1건을 시작·종료 연월과 함께 저장하고 그 기간 중 두 달의 내역을 각각 만든 뒤, 한 달의 금액만 고쳐도 다른 달이 영향받지 않고, 같은 달을 또 만들려 하면 유일 제약이 막는지 확인한다

**Depends on**: US2 (수단·지출유형 FK 대상)

### Tests for User Story 4

- [ ] T035 [P] [US4] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/FixedExpenseMonthlyUniqueIT.java` — 같은 `(fixed_expense_idx, year, month)` 2건은 실패(quickstart #11). `month = 13`은 CHECK 위반(#19)
- [ ] T036 [P] [US4] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/FixedExpenseMonthlyConcurrencyIT.java` — 같은 조합을 별도 트랜잭션 2개에서 `INSERT ... ON CONFLICT DO NOTHING`으로 동시 시도해도 1건만 남는지(#12). `@Transactional` **밖에서** 실행한다
- [ ] T037 [P] [US4] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/FixedExpenseCascadeIT.java` — 고정지출 관리 행을 DELETE하면 그 월별 내역이 지난 달 포함 전부 사라지는지(#13)

### Implementation for User Story 4

- [ ] T038 [US4] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserFixedExpense.java`에 `tbl_user_fixed_expense` Entity를 만든다 — `name`, `payment_method_idx` FK RESTRICT, `amount`, `payment_day_of_month`, `content`, `expend_group_idx` FK RESTRICT, `start_year`/`start_month`/`end_year`/`end_month`, 인덱스 `ix_user_fixed_expense_period`. **수단·유형 이름 스냅샷 컬럼을 두지 않는다**(FR-050). 상세: data-model.md §8
- [ ] T039 [US4] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserFixedExpenseMonthly.java`에 `tbl_user_fixed_expense_monthly` Entity를 만든다 — `fixed_expense_idx` FK **CASCADE**, `year`, `month`, `amount`, `payment_date`(말일 보정이 끝난 완전한 날짜), `content`, `payment_method_idx` FK RESTRICT, `expend_group_idx` FK RESTRICT, `modified`(DEFAULT false), UNIQUE `ux_user_fixed_expense_monthly`(`fixed_expense_idx, year, month`), 인덱스 `ix_user_fixed_monthly_ym`. T038에 의존한다. 상세: data-model.md §9
- [ ] T040 [P] [US4] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserFixedExpenseRepository.java`와 `UserFixedExpenseMonthlyRepository.java`를 만든다 — 적용 기간에 걸리는 관리 행 조회(`year*12+month` 합성값 비교 JPQL), 그 달 내역 목록, 수동 재작성용 `deleteByFixedExpenseIdxInAndYearAndMonth`, lazy 생성용 `ON CONFLICT DO NOTHING` 네이티브 INSERT
- [ ] T041 [US4] `sql/04_constraints.sql`에 고정지출 CHECK를 추가한다 — `ck_fixed_expense_amount`, `ck_fixed_expense_day`(1~31), `ck_fixed_expense_start_month`·`ck_fixed_expense_end_month`(1~12), `ck_fixed_expense_period`(`end_year*12+end_month >= start_year*12+start_month`), `ck_fixed_monthly_amount`, `ck_fixed_monthly_month`
- [ ] T042 [US4] 스키마를 반영하고 T035~T037을 실행해 통과를 확인한다

**Checkpoint**: 고정지출 3분할 구조가 완성됐다. 월별 가계부 목록이 세 테이블을 합쳐 만들어질 수 있다

---

## Phase 7: User Story 5 - 목표금액과 월별 통계 스냅샷을 저장할 수 있다 (Priority: P5)

**Goal**: 기본·월별 목표금액(5.1~5.4)과 저장 시점 계산 결과를 보존하는 월별 통계 스냅샷(5.5~5.6)이 쓸 저장 구조를 완성한다

**Independent Test**: 한 지출유형에 기본 목표금액과 특정 연·월 목표금액을 각각 저장해 두 값이 독립적으로 읽히는지 확인한다. 통계를 저장한 뒤 원본을 바꿔도 저장 값이 변하지 않고, 같은 달을 다시 저장해도 행이 늘지 않는지 확인한다

**Depends on**: US2 (목표금액의 지출유형 FK). 통계 4개 테이블은 `tbl_user` 외에 외부 FK가 없어 Foundational만 있으면 만들 수 있다

### Tests for User Story 5

- [ ] T043 [P] [US5] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/ExpendTargetIT.java` — 지출유형을 `deleted = true`로 해도 목표금액 행과 참조가 유지되는지(quickstart #8), 같은 `(id_key, expend_group_idx)` 2건은 실패, 같은 `(id_key, year, month, expend_group_idx)` 2건은 실패, `target_amount = 100000001`은 CHECK 위반(#18)
- [ ] T044 [P] [US5] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/StatisticsUniqueIT.java` — 같은 `(id_key, year, month)` 통계 2건은 실패(#14)
- [ ] T045 [P] [US5] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/StatisticsCascadeIT.java` — 통계 스냅샷을 DELETE하면 상세 3종이 함께 사라지는지(#16)
- [ ] T046 [P] [US5] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/StatisticsBrokenRefIT.java` — 통계 상세에 **실재하지 않는** `expend_group_idx`·`payment_method_idx` 값을 넣어도 INSERT가 성공하는지(#15, FR-078a — FK가 없어야 통과한다)

### Implementation for User Story 5

- [ ] T047 [P] [US5] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserExpendTargetDefault.java`에 `tbl_user_expend_target_default` Entity를 만든다 — `expend_group_idx` FK RESTRICT, `target_amount`, UNIQUE `ux_user_target_default`(`id_key, expend_group_idx`). 상세: data-model.md §10
- [ ] T048 [P] [US5] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserExpendTargetMonthly.java`에 `tbl_user_expend_target_monthly` Entity를 만든다 — `year`, `month`, `expend_group_idx` FK RESTRICT, `target_amount`, UNIQUE `ux_user_target_monthly`(`id_key, year, month, expend_group_idx`). 상세: data-model.md §11
- [ ] T049 [US5] `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/UserStatistics.java`에 `tbl_user_statistics` Entity를 만든다 — `year`, `month`, `saved_at`, `income_total`, `expense_total`, `fixed_amount`, `regular_amount`, `fixed_percent`/`regular_percent`(NUMERIC(5,2)), UNIQUE `ux_user_statistics`(`id_key, year, month`). 상세: data-model.md §12
- [ ] T050 [US5] 통계 상세 3종 Entity를 만든다. T049에 의존한다
  - `entity/UserStatisticsWeekly.java` — `statistics_idx` FK CASCADE, `week_index`, `week_start`, `week_end`, `amount`, UNIQUE(`statistics_idx, week_index`)
  - `entity/UserStatisticsExpendGroup.java` — `statistics_idx` FK CASCADE, `expend_group_idx`(**FK 없이 값만**), `expend_group_name`, `amount`, `target_amount`, `usage_rate`(NUMERIC(6,2)), `status`, UNIQUE(`statistics_idx, expend_group_idx`)
  - `entity/UserStatisticsPaymentMethod.java` — `statistics_idx` FK CASCADE, `payment_method_idx`(**FK 없이 값만**), `payment_method_name`, `amount`, UNIQUE(`statistics_idx, payment_method_idx`)
  - 상세: data-model.md §13~§15. 두 상세의 유형·수단 컬럼에 `@ManyToOne`이나 `@JoinColumn`을 쓰지 않는다 — Hibernate가 FK를 만들어 버린다
- [ ] T051 [P] [US5] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/` 에 목표금액 2개·통계 4개 Repository를 만든다 — 목표금액은 upsert 판정용 조회(`findByIdKeyAndExpendGroupIdx`, `findByIdKeyAndYearAndMonthAndExpendGroupIdx`), 통계는 `findByIdKeyAndYearAndMonth`와 상세 일괄 삭제
- [ ] T052 [US5] `sql/04_constraints.sql`에 목표금액·통계 CHECK를 추가한다 — `ck_target_default_amount`·`ck_target_monthly_amount`(0~100000000), `ck_target_monthly_month`, `ck_statistics_month`, `ck_stat_weekly_index`(`>= 1`), `ck_stat_group_status`(`IN ('UNDER','OK','OVER')`)
- [ ] T053 [US5] 스키마를 반영하고 T043~T046을 실행해 통과를 확인한다

**Checkpoint**: 15개 저장 단위가 모두 만들어졌다. 5개 User Story 전부 독립 검증 가능하다

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 전 테이블에 걸친 검증, 명세 정합, 덤프 재생성

- [ ] T054 `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/AuditColumnIT.java` — 15개 테이블 전부가 감사 컬럼 4종을 갖는지 `information_schema.columns`로 확인(quickstart §2-4), `created_by` 없이 INSERT하면 NOT NULL 위반(`tbl_user` 제외, #20)
- [ ] T055 [P] `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/SchemaStructureIT.java` — quickstart §2의 나머지 SQL 검증을 테스트로 옮긴다: `tbl_user%` 테이블 15개(§2-1), 레거시 이름 0건(§2-2), PK 규칙 — `tbl_user`만 `id_key`(§2-3), 부분 유니크 2건 존재(§2-5), 통계 상세의 유형·수단 FK 부재(§2-6)
- [ ] T056 `sql/04_constraints.sql`을 **두 번 연속 실행**해 두 번째에도 오류가 없는지 확인한다. 실패하면 해당 문장을 `IF NOT EXISTS` 또는 `DO $$ ... EXCEPTION WHEN duplicate_object` 패턴으로 고친다
- [ ] T057 [P] `프로젝트설계/기능명세상세-백엔드/phase2-수단-지출유형/2.1-PaymentMethodCreate.md`와 `2.4-PaymentMethodUpdate.md`의 Body 표에 `purpose`(`EXPENSE`\|`INCOME`)를 추가하고, 용도 변경은 그 수단을 참조하는 지출·소득·고정지출이 0건일 때만 허용한다는 규칙을 비고에 적는다(research §13-1)
- [ ] T058 [P] `프로젝트설계/기능명세상세-백엔드/phase1-회원/1.2-MemberSignup.md`의 비고에서 "식비, 교통, 주거 등"을 확정 10종(식비·교통·주거·통신·쇼핑·장보기·의료·교육·문화·기타)으로 바꾸고 30×30 기본 아이콘이 함께 생성된다는 점을 적는다(research §13-2)
- [ ] T059 [P] `프로젝트설계/기능명세상세-백엔드/_공통.md`의 `tbl_member_session` 절 제목과 `tbl_member.pw` 표기를 `tbl_user_session`·`tbl_user.pw`로 갱신한다(research §13-3)
- [ ] T060 `sql/schema-moneylogdb.sql`을 재생성한다 — 헌장 원칙 VI의 `pg_dump` 명령을 옵션 그대로 실행. **시드 데이터 덧붙이기는 하지 않는다**(기본 지출유형 10종은 회원마다 생기는 데이터). 재생성 후 `CREATE TABLE` 15건·`INSERT INTO` 0건을 확인하고 **같은 커밋에 포함**한다
- [ ] T061 `specs/001-backend-db-schema/quickstart.md` §5 완료 판정 9개 항목을 처음부터 끝까지 실행해 전부 통과하는지 확인한다

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존 없음 — 즉시 시작
- **Foundational (Phase 2)**: Setup 완료 후. **모든 User Story를 막는다** — 14개 테이블이 `tbl_user.id_key`를 참조하고 15개 Entity가 `BaseAuditEntity`를 상속한다
- **US1 (Phase 3)**: Foundational 완료 후. 다른 스토리에 의존하지 않음
- **US2 (Phase 4)**: Foundational 완료 후. US1과 **병렬 가능**
- **US3 (Phase 5)**: **US2 완료 후** — 지출·소득이 수단·지출유형을 FK로 참조
- **US4 (Phase 6)**: **US2 완료 후** — 고정지출이 수단·지출유형을 FK로 참조. US3과 **병렬 가능**
- **US5 (Phase 7)**: **US2 완료 후**(목표금액의 지출유형 FK). 통계 4개 테이블만 먼저 만들 거라면 Foundational만으로 충분하다. US3·US4와 **병렬 가능**
- **Polish (Phase 8)**: T054~T056·T060·T061은 US1~US5 전부 완료 후. T057~T059(문서 개정)는 **언제든 병렬 가능**

### 스토리 독립성에 대한 정직한 메모

이 기능은 하나의 스키마를 만드는 작업이라 스토리 사이에 **실제 FK 의존이 있다.** 템플릿이 전제하는 "모든 스토리가 완전 독립"은 여기서 성립하지 않는다. 대신 각 스토리는 **자기 테이블의 제약이 DB에서 강제되는지를 독립적으로 검증**할 수 있고, 그 지점에서 멈춰도 앞 스토리가 깨지지 않는다.

### Within Each User Story

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- Entity → Repository → 보조 DDL → 반영·검증 순서
- 보조 DDL 작업(T017·T025·T033·T041·T052)은 전부 `sql/04_constraints.sql` **같은 파일**을 고치므로 서로 병렬 실행하지 않는다

### Parallel Opportunities

- Setup: T002·T003
- US1: 테스트 T010~T012 동시 / Entity·Repository T013~T016 동시
- US2: 테스트 T019~T021 동시 / T022·T023 동시
- US3: 테스트 T027~T029 동시 / T030·T031 동시
- US4: 테스트 T035~T037 동시. **T038 → T039는 순차**(CASCADE FK 대상)
- US5: 테스트 T043~T046 동시 / T047·T048 동시. **T049 → T050은 순차**
- Polish: T055·T057·T058·T059 동시
- 인원이 있으면 Foundational 이후 **US1과 US2를 동시에**, US2 이후 **US3·US4·US5를 동시에** 진행할 수 있다

---

## Parallel Example: User Story 2

```bash
# 테스트 3건을 함께 작성 (구현 전 실패 확인)
Task: "ExpendGroupConstraintIT — 회원 안 이름 유일, 삭제분 포함"
Task: "SoftDeleteIT — deleted=true 후 행 잔존"
Task: "PaymentMethodConstraintIT — type·purpose CHECK"

# Entity 2건을 함께 작성
Task: "UserPaymentMethod entity in data-mod/.../entity/UserPaymentMethod.java"
Task: "UserExpendGroup entity in data-mod/.../entity/UserExpendGroup.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 Setup 완료
2. Phase 2 Foundational 완료 — **여기가 막히면 아무것도 못 한다**
3. Phase 3 US1 완료
4. **멈추고 검증**: 회원 1건 + 세션 폐기·재발급 흐름이 DB 제약으로 강제되는지 확인
5. 이 시점에서 Phase 1(회원·인증) API 구현을 시작할 수 있다

### Incremental Delivery

1. Setup + Foundational → 기반 완성
2. US1 → 회원·인증 API 착수 가능 (MVP)
3. US2 → 수단·지출유형 API 착수 가능
4. US3 → 지출·소득·할부·엑셀 API 착수 가능
5. US4 → 고정지출·월별 가계부 API 착수 가능
6. US5 → 목표금액·통계 API 착수 가능
7. Polish → 덤프 재생성·명세 정합으로 마감

각 스토리 완료 시점이 곧 **백엔드 구현 Phase의 착수 조건**이다.

### Parallel Team Strategy

1. Setup + Foundational을 함께 끝낸다
2. 개발자 A: US1 / 개발자 B: US2
3. US2가 끝나면 — 개발자 A: US3 / B: US4 / C: US5
4. 문서 개정(T057~T059)은 아무 때나 별도로 진행

---

## Notes

- [P] = 다른 파일, 의존 없음
- `sql/04_constraints.sql`은 5개 스토리가 나눠 채우는 **공유 파일**이다. 각 스토리는 자기 절만 덧붙이고, 최종 멱등성은 T056이 확인한다
- Hibernate `ddl-auto: update`는 부분 유니크 인덱스와 CHECK를 만들지 못한다. 그것들이 보조 DDL로 빠진 이유이며, Entity에 `@Table(uniqueConstraints=...)`로 부분 유니크를 흉내 내려 하면 조건 없는 유니크가 만들어져 **이메일 NULL 다건 저장이 깨진다**
- 통계 상세 2종의 유형·수단 컬럼에 JPA 연관 매핑을 쓰지 않는다 — FK가 생기면 FR-078a(끊긴 참조 허용) 위반이고 T046이 실패한다
- 각 작업 또는 논리 묶음 단위로 커밋한다. 스키마가 바뀐 커밋에는 재생성한 덤프를 함께 넣는다(헌장 VI)
- 어느 Checkpoint에서 멈춰도 그때까지의 스토리는 독립적으로 검증된 상태다
