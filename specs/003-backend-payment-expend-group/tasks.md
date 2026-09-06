---

description: "Task list for 003-backend-payment-expend-group"
---

# Tasks: 지출·소득 수단과 지출유형 관리

**Input**: Design documents from `/specs/003-backend-payment-expend-group/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**선행 기능**: **`002-backend-member-auth` 가 서 있어야 한다.** 13건 전부 로그인이 필요하고 응답 규격·전역 예외 처리·AOP 로깅·`AuditorAware`·`IconStorage.copyFromSeed` 를 전부 002 가 만든다. 002 는 74/74 완료됐다.

**Tests**: 포함한다. plan.md 의 Testing 이 실 PostgreSQL 통합 테스트를 지정했고 quickstart.md §3 이 검증 시나리오 42건을 확정했다. 테스트 이름의 `#N` 은 그 번호와 1:1로 맞춘다 — 001·002 에서 쓴 방식이고 빠진 번호를 바로 찾을 수 있다.

**Organization**: spec.md 의 User Story 4개(P1·P2·P2·P3)로 묶었다. 각 스토리는 Repository → Service → DTO·Mapper → Controller 순으로 완결된다.

**이 기능은 스키마를 바꾸지 않는다.** 필요한 컬럼·CHECK 2건·유니크 1건·인덱스 2건이 001 에 전부 있다. 끝나고 `git diff sql/schema-moneylogdb.sql` 이 비어 있어야 한다(T059).

**새 의존성이 없다.** 이미지 형식 판정은 JDK 의 `javax.imageio.ImageIO` 로 충분하다(research.md §3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 미완료 작업에 의존하지 않음)
- **[Story]**: 해당 User Story (US1~US4)
- 모든 작업에 정확한 파일 경로를 적는다

## Path Conventions

002 가 세운 배치를 그대로 따른다.

- `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/` — Controller·Service·DTO·Mapper·`storage`
- `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java` — 코드 상수 (이미 30xx·31xx 가 들어가 있다)
- `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/` — 조회 메서드 추가
- `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/` — `paymentmethod/`·`expendgroup/`·`icon/`
- 설계 명세: `프로젝트설계/기능명세상세-백엔드/phase2-수단-지출유형/`

`core-mod` 와 `money-app`(프론트)은 건드리지 않는다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 명세 선행 개정(원칙 V)과, 이 기능이 쓰는 설정·패키지 확인

- [X] T001 **착수 전 명세 개정 (원칙 V)** — 코드보다 먼저 한다. 세 묶음이다.
  1. **`iconFile` 파트 설명 2건.** 두 파일의 현재 문구가 **서로 다르다** — `2.7-ExpendGroupCreate.md:42` 는 `아이콘 이미지 (png, jpg, gif 등)`, `2.11-ExpendGroupUpdate.md:46` 은 `새 아이콘 (교체)` 다. **둘 다** "`png`·`jpg`·`gif` 세 형식, 1MB 이하. 형식은 확장자가 아니라 파일 내용으로 판정한다" 로 고친다(2.11 은 앞에 "기존 아이콘을 교체한다. " 를 덧붙인다). "등"으로 열어 두는 것도, 형식·크기를 아예 적지 않는 것도 원칙 V("설명 칸은 그 칸만 보고 의미가 읽혀야 한다")에 걸린다. FR-219 가 규격을 확정했으므로 반영만 하면 된다
  2. **응답 표의 빈 설명 칸 8곳.** 원칙 V 는 빈 칸을 **누락으로 규정**한다 — `2.2:54` `list[].cardExpiry`("카드 유효기간 `YYYY-MM`. 계좌면 `null`"), `2.3:53·54·55` `inUse`("사용 여부. `false` 면 입력 화면 목록에서 빠진다")·`cardExpiry`(위와 같은 문구)·`deleted`("삭제 표시 여부. `true` 여도 관리 목록에는 남는다"), `2.7:55` `inUse`, `2.8:51` `list[].inUse`, `2.13:51·52` `list[].iconUrl`("아이콘 조회 경로. 아이콘이 없으면 `null`")·`list[].defaultGroup`("가입 시 생성된 기본 유형 여부. `true` 면 이름 변경·삭제가 막힌다")
  3. **(완료 — 2026-09-06)** **plan.md·research.md 의 낡은 서술.** 002 가 이미 구현한 것을 003 이 신규로 만드는 것처럼 적혀 있다 — ① `plan.md` § Source Code 의 `IconStorageProperties.java +` → `IconProperties.java ~ 최대 크기(maxFileSize) 바인딩 추가 (002 가 만든 클래스)`, `IconStorage.java +` → `~ save·read·exists 추가 (002 가 만든 파일)`, `research.md` §4 의 `IconStorageProperties` → `IconProperties` ② `plan.md:101·148` 의 `ErrorCode 상수 11개` → **13개**(실제 개수) ③ `plan.md:143` 머리말 "002 가 만드는 것에 의존하되 **고치지 않는다**" 와 § Constitution Check 재평가 `I` 의 "`common-mod` 추가분은 `ErrorCode` 상수뿐" 을 실제 수정 범위(002 파일 5건, `common-mod` 는 `ErrorCode`·`ApiLoggingAspect`·`GlobalExceptionHandler`)에 맞게 고친다
  - 검증: `grep -rn 'png, jpg, gif 등\|새 아이콘 (교체)' 프로젝트설계/기능명세상세-백엔드/phase2-수단-지출유형/` → **0건** · `grep -rnE '\|\s*\|\s*$' 프로젝트설계/기능명세상세-백엔드/phase2-수단-지출유형/` → **0건** · `grep -rn 'IconStorageProperties\|상수 11개' specs/003-backend-payment-expend-group/` → **0건**
- [X] T002 `app-mod/money-backend-app/src/main/resources/application.yml` 에 `icon.storage.max-file-size: 1MB` 와 `spring.servlet.multipart.max-file-size: 1MB`·`max-request-size` 를 추가한다. `icon.storage.dir` 은 **002 가 이미 넣었다**(`${ICON_STORAGE_DIR}`, 기본값 없음). 서블릿 계층에도 크기 제한을 거는 이유는 애플리케이션 검사만 두면 1MB 초과 파일이 **일단 전부 업로드된 뒤에** 거절되기 때문이다(icon-storage.md §2)
- [X] T003 [P] `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java` 에 이 기능이 쓰는 코드 **13개**(`3001`~`3005` 5개 · `3101`~`3108` 8개)가 전부 있는지 확인한다. **002 의 T007 에서 003~006 대역을 미리 넣었으므로 추가할 것이 없어야 한다**(확인 결과 13개 모두 존재). 빠진 것이 있으면 spec.md § 이 기능이 쓰는 에러코드 표 그대로 채운다. 같은 수치를 `11개`로 잘못 적은 plan.md 두 곳은 T001-3 이 고친다
- [X] T004 [P] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/` 아래에 `paymentmethod/`·`expendgroup/`·`icon/` 디렉터리를 만든다. 테스트 기반은 002 의 `AbstractApiIT` 를 상속해 쓴다 — 로그인 헬퍼·`resCode` 추출·정리(cleanup)가 이미 있다. **정리 대상을 확장한다**(`AbstractApiIT.cleanUpTestUsers`): 003 은 `tbl_user_payment_method` 에 처음으로 행을 만들고, T046·T047·T029 가 참조 검사를 위해 `tbl_expense`·`tbl_income`·`tbl_fixed_expense`·`tbl_fixed_expense_monthly`·`tbl_expend_target_default`·`tbl_expend_target_monthly` 에 직접 행을 넣는다. **삭제는 자식 → 부모 순서**여야 한다 — 이 6개가 `tbl_user_payment_method(idx)`·`tbl_user_expend_group(idx)` 로 FK 를 걸고 있어(덤프에 참조 9건), 부모를 먼저 지우면 **FK 위반으로 정리 전체가 실패하고 이후 테스트가 오염된 DB 에서 돈다**. 순서: 6개 자식 → `tbl_user_payment_method` → 기존 `tbl_user_expend_group` → `tbl_user_session`·`tbl_user_login_history` → `tbl_user`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 네 스토리가 공통으로 기대는 조회 메서드와 아이콘 저장 기반

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 어떤 User Story 도 시작할 수 없다

- [X] T005 [P] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserPaymentMethodRepository.java` 에 조회 메서드를 추가한다 — 소유자 기준 전체 목록(2.2, 삭제분 포함, 정렬 고정), 소유자+`idx` 단건(2.3~2.5), 소유자+`purpose`+`inUse=true`+`deleted=false` 사용 중 목록(2.6). **정렬을 `order by idx asc` 로 고정한다**(목록 4건 전부 동일) — 없으면 PostgreSQL 이 호출마다 다른 순서를 줄 수 있어 화면의 목록이 흔들린다. `idx` 를 쓰는 이유는 등록 순서가 곧 사용자가 기대하는 순서이고, 이름 정렬은 이름 변경(2.4·2.11)에 목록 순서가 따라 흔들리기 때문이다. 별도 정렬 컬럼은 두지 않는다(data-model.md §4)
- [X] T006 [P] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserExpendGroupRepository.java` 에 조회 메서드를 보강한다 — 소유자 기준 전체 목록(2.8, 삭제분 포함), 소유자+`idx` 단건(2.9·2.11·2.12), 사용 중 목록(2.13, `inUse=true`+`deleted=false`). **정렬은 T005 와 같이 `order by idx asc` 로 고정한다.** 기존 `existsByUserIdKeyAndName`(이름 유일성)·`findByIdxAndUserIdKey` 는 이미 있으므로 재사용한다. **이름 유일성 검사에 `deleted` 조건을 넣지 않는다** — 유니크 제약 `ux_user_expend_group_name` 에 `WHERE` 절이 없어 DB 도 삭제분을 센다(FR-209)
- [X] T007 [P] `data-mod/.../repository/UserExpenseRepository.java`·`UserIncomeRepository.java`·`UserFixedExpenseRepository.java`·`UserFixedExpenseMonthlyRepository.java` 에 **참조 존재 검사** 메서드를 추가한다 — 수단 `idx` 로 `existsByPaymentMethodIdx`(4개 전부), 지출유형 `idx` 로 `existsByExpendGroupIdx`(지출만). **`count` 가 아니라 `exists` 를 쓴다**(research.md §6) — 필요한 답이 "있느냐"인데 세면 전 행을 훑고, 수가 많을수록 느려진다. 004·005 가 아직 API 를 만들지 않았어도 테이블과 Entity 는 001 에 있으므로 지금 쓸 수 있다
- [X] T008 `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/storage/ImageTypeDetector.java` 를 만든다 — 업로드 바이트를 `javax.imageio.ImageIO.read()` 로 **실제 디코딩**해 `png`·`jpg`·`gif` 중 무엇인지 판정하고, 셋 중 어느 것도 아니면 빈 결과를 돌려준다. **확장자를 믿지 않는다**(FR-219) — 2.10 이 저장된 확장자로 `Content-Type` 을 정하므로, 확장자만 `png` 로 바꾼 파일이 통과하면 서버가 `image/png` 라고 말하면서 다른 바이트를 내보낸다. 크기 검사는 여기서 하지 않는다(디코딩 전에 호출자가 먼저 막는다)
- [X] T009 `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/storage/IconStorage.java` 에 `save(idKey, expendGroupId, extension, bytes) → filename`·`read(filename) → Optional<byte[]>`·`exists(filename)` 를 추가한다. `copyFromSeed` 는 **002 가 이미 만들었으므로 재사용하고 새로 만들지 않는다** — 두 곳에서 파일명을 조립하면 규칙이 갈린다. `read`·`exists` 는 **경로 정규화를 반드시 한다**: `filename` 은 Path Variable 이라 `..` 이 들어올 수 있고, 저장 루트 밖을 벗어나면 읽지 않는다(icon-storage.md §3 주의 4). FR-224 가 막은 것은 서버가 **만드는** 파일명이고 조회 요청의 값은 클라이언트가 보내는 것이라 신뢰할 수 없다. T008 에 의존
- [X] T010 `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/config/IconProperties.java` 에 `maxFileSize` 를 추가한다(002 가 만든 클래스다). 기동 시점에 값이 있는지 검증하고, `DataSize` 로 바인딩해 `1MB` 표기를 그대로 쓴다. T002 에 의존
- [X] T011 `common-mod/src/main/java/com/dbdomino/moneylog/common/logging/ApiLoggingAspect.java` 가 **바이너리 응답과 파일 파라미터를 로그에 찍지 않도록** 보강한다 — `byte[]`·`Resource`·`ResponseEntity<Resource>` 반환은 크기만 남기고, `MultipartFile` 파라미터는 파일명·크기만 남긴다(api-contract.md §7). 지금은 반환값에서 `resCode` 만 뽑고 나머지는 `?` 로 남기므로 큰 사고는 없지만, `MultipartFile` 인자는 `SensitiveMasker.describe` 가 리플렉션으로 훑어 내용이 통째로 들어갈 수 있다. 002 가 만든 파일을 고치는 유일한 작업이다

**Checkpoint**: 조회·파일 기반이 섰다. US1~US4 를 시작할 수 있다

---

## Phase 3: User Story 1 - 지출·소득 수단을 관리한다 (Priority: P1) 🎯 MVP

**Goal**: 카드·계좌를 등록(2.1)하고 관리 목록·상세로 보고(2.2·2.3) 고치고(2.4) 삭제 표시한다(2.5)

**Independent Test**: 수단을 등록·수정·삭제 표시한 뒤 관리 목록에는 삭제분까지 나오고 사용 중 목록에는 빠지는지 확인한다. 다른 Phase 없이 완결된다

### Tests for User Story 1

> 먼저 작성하고, 구현 전에 **실패하는 것**을 확인한다

- [X] T012 [P] [US1] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/paymentmethod/PaymentMethodCreateIT.java` — quickstart #1(`CARD`+`EXPENSE` 등록 시 본인 소유·`deleted=false`)·#2(`ACCOUNT` 는 `cardExpiry` 가 `null`)·#3(`type` 허용 값 밖 `3001`)·#4(`purpose` 허용 값 밖 `3001`)·#5(`cardExpiry` 가 `YYYY-MM` 아님 `3002`). 소유자가 **토큰으로 정해지는지**도 본다 — Body 에 남의 `idKey` 를 실어도 무시되어야 한다(FR-201)
- [X] T013 [P] [US1] `.../paymentmethod/PaymentMethodDeleteIT.java` — #6(삭제 후 관리 목록에 `deleted=true` 로 남는다 — SC-204)·#8(이미 삭제된 수단 재삭제 `3004`)·#10(**삭제 표시된 수단도 수정된다** — 삭제는 읽기 전용이 아니다). **#7(삭제 후 사용 중 목록에서 빠진다)은 여기 두지 않는다** — 2.6 이 US2(T025·T026)에서야 생기므로 이 Phase 에서는 404 로 실패한다. T023 이 맡는다
- [X] T014 [P] [US1] `.../paymentmethod/PaymentMethodOwnershipIT.java` — #9(남의 수단 ID 로 상세·수정·삭제 전부 `3003` — SC-207). **"없음"과 "타인 소유"가 같은 코드**인지 확인한다(FR-201) — 코드가 갈리면 남의 자원이 존재한다는 사실이 새어 나간다

### Implementation for User Story 1

- [X] T015 [P] [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/PaymentMethodCreateRequest.java` 를 만든다 — `name`·`type`·`purpose`·`cardExpiry`(선택)·`inUse`(기본 `true`). `type`·`purpose` 는 **허용 값 밖이면 `3001`** 이라 Bean Validation(`9001`)으로 막지 않고 서비스가 검사한다. 002 의 `SignupRequest` 가 비밀번호 규칙을 같은 이유로 서비스에 둔 것과 같은 판단이다
- [X] T016 [P] [US1] `.../dto/response/PaymentMethodResponse.java` 를 만든다 — `paymentMethodId`·`name`·`type`·`purpose`·`cardExpiry`·`inUse`·`deleted`. **`deleted` 를 싣는다**(2.2·2.3) — 관리 목록이 삭제분까지 돌려주므로 이 필드가 없으면 화면이 구분할 수 없다. **Entity 를 그대로 내보내지 않는다**(헌장 원칙 II)
- [X] T017 [P] [US1] `.../dto/response/PaymentMethodListResponse.java` 를 만든다 — `list` **하나만** 담는다. `offset`·`limit`·`totalCount` 를 싣지 않는다(FR-217). 002 의 관리자 회원 목록과 형태가 다른 것은 의도된 차이이며, 페이징 없는 목록에 "전체 건수"를 붙이면 무의미한 필드가 생긴다
- [X] T018 [US1] `.../mapper/PaymentMethodMapper.java` 를 MapStruct 로 만든다 — Entity ↔ DTO 변환만 한다. `idx` → `paymentMethodId` 이름 매핑이 필요하다. 비즈니스 로직을 담지 않는다(헌장 원칙 II). T016 에 의존
- [X] T019 [US1] `.../service/PaymentMethodService.java` 에 등록(2.1)·목록(2.2)·상세(2.3)를 구현한다 — 소유자는 `AuthPrincipal.idKey()` 로만 정하고 요청이 지정할 수 없다(FR-201). `type`·`purpose` 값 검증(`3001`), `cardExpiry` 형식 검증(`3002`), **`type=ACCOUNT` 면 `cardExpiry` 를 `null` 로 강제**(FR-204). 조회는 소유자 불일치를 `3003` 으로 묶는다. T005·T015~T018 에 의존
- [X] T020 [US1] `.../service/PaymentMethodService.java` 에 수정(2.4)을 구현한다 — api-contract.md §5 의 판정 순서를 그대로 따른다: 대상 조회(`3003`) → 보낸 필드 값 검증(`3001`·`3002`) → **`purpose` 를 실제로 보냈고 값이 바뀔 때만** 참조 검사 → UPDATE. 참조 검사는 **4개 테이블 전부**를 본다(지출·소득·고정지출·월별 고정지출, FR-205) — 하나라도 빠뜨리면 "소득 수단으로 낸 지출"이 만들어져 월별 집계와 통계 수단별 요약이 어긋난다. 참조가 1건이라도 있으면 `3005`. omit 규칙은 002 의 `PatchFields` 를 재사용한다. T019·T007 에 의존(같은 파일이라 순차)
- [X] T021 [US1] `.../service/PaymentMethodService.java` 에 삭제(2.5)를 구현한다 — **삭제 표시**(`deleted=true` UPDATE)이며 물리 삭제하지 않는다(FR-206). 이미 `deleted=true` 면 `3004` 로 거절한다 — 멱등 성공으로 흘리면 화면이 "방금 지웠다"와 "이미 지워져 있었다"를 구분할 수 없다. T020 과 같은 파일이라 순차
- [X] T022 [US1] `.../controller/PaymentMethodController.java` 를 만들고 2.1~2.5 를 붙인다 — `POST·GET /api/v1/payment-methods`, `GET·PATCH·DELETE /api/v1/payment-methods/{paymentMethodId}`. 삭제는 `DELETE` 지만 **동작은 UPDATE** 다(삭제 표시). 인가는 002 의 `SecurityFilterChain` 기본값(`authenticated`)이 처리하므로 여기에 애너테이션을 붙이지 않는다. Controller 는 Service 만 부른다

**Checkpoint**: 수단 관리가 독립적으로 동작한다. quickstart **#1~#6·#8~#10** 이 통과하면 MVP 로 인도할 수 있다(**#7 은 2.6 을 부르므로 US2 에서 확인한다**)

---

## Phase 4: User Story 2 - 입력 화면이 고를 수 있는 것만 고른다 (Priority: P2)

**Goal**: 지출·소득 입력 화면이 쓸 사용 중 수단 목록(2.6)과 사용 중 지출유형 목록(2.13)을 제공한다

**Independent Test**: 용도·사용 여부·삭제 표시를 다르게 한 수단 4건을 만들고 사용 중 목록이 정확히 1건만 돌려주는지 확인한다

### Tests for User Story 2

- [X] T023 [P] [US2] `.../paymentmethod/PaymentMethodActiveListIT.java` — #7(삭제 표시된 수단이 사용 중 목록에서 빠진다 — US1 에서 옮겨 왔다. 2.6 이 여기서야 서기 때문이다)·#11(지출용·소득용·미사용·삭제된 수단 4건 중 `purpose=EXPENSE` 사용 중 목록이 **정확히 1건** — SC-202)·#13(`purpose` 허용 값 밖이면 `3001`). **네 조건이 전부 걸리는지**가 이 시나리오의 핵심이다 — 하나만 빠져도 입력 화면에 소득 수단이나 삭제된 수단이 섞여 나온다
- [X] T024 [P] [US2] `.../expendgroup/ExpendGroupActiveListIT.java` — #12(`inUse=false` 인 유형이 사용 중 목록에서 빠진다). **#14(목록 4건의 `data` 에 `list` 만 있다)는 여기 두지 않는다** — 그 4건에 2.8 이 들어 있고 2.8 은 US3(T034·T037)에서야 생긴다. 네 API 가 모두 선 뒤인 Phase 7(T055)에서 한 자리에 모아 본다

### Implementation for User Story 2

- [X] T025 [US2] `.../service/PaymentMethodService.java` 에 사용 중 목록(2.6)을 구현한다 — **세 조건을 모두** 건다: `purpose` 일치 · `in_use=true` · `deleted=false`(FR-207). `purpose` 가 허용 값 밖이면 `3001`. 이 값은 **Path Variable** 이다(`/payment-methods/active/{purpose}`) — Query 가 아니다. 인덱스 `ix_user_payment_method_active` 가 이 조합을 커버한다. T021 과 같은 파일이라 순차
- [X] T026 [US2] `.../controller/PaymentMethodController.java` 에 `GET /api/v1/payment-methods/active/{purpose}`(2.6)를 추가한다. **경로 순서에 주의한다** — `/{paymentMethodId}` 와 `/active/{purpose}` 가 같은 깊이라, 스프링이 `active` 를 ID 로 해석하지 않도록 매핑을 확인한다. T022 와 같은 파일이라 순차
- [X] T027 [US2] `.../service/ExpendGroupService.java` 에 사용 중 목록(2.13)을 구현한다 — **두 조건만** 건다: `in_use=true` · `deleted=false`. **`purpose` 조건이 없다** — 지출유형에는 용도 구분이 없고, 인덱스 `ix_user_expend_group_active (id_key, in_use, deleted)` 에도 `purpose` 가 없어 그 사실이 DB 에 드러나 있다. T006 에 의존

**Checkpoint**: 입력 화면이 쓸 두 목록이 정확한 필터로 동작한다

---

## Phase 5: User Story 3 - 지출유형을 관리한다 (Priority: P2)

**Goal**: 지출유형을 등록(2.7)하고 목록·상세를 보고(2.8·2.9) 고치고(2.11) 삭제 표시한다(2.12)

**Independent Test**: 유형을 등록·수정·삭제 표시한 뒤 이름 유일성과 삭제 차단 조건이 실제로 막는지 확인한다

### Tests for User Story 3

- [ ] T028 [P] [US3] `.../expendgroup/ExpendGroupNameUniqueIT.java` — #15(같은 회원이 같은 이름으로 두 번 등록하면 `3101` — SC-203)·#16(**다른 회원이면 둘 다 성공** — SC-203)·#17(삭제 표시 후 **같은 이름으로 재등록해도 `3101`**). #17 이 유니크 제약에 `WHERE` 조건이 없다는 사실을 실제로 검증하는 자리다
- [ ] T029 [P] [US3] `.../expendgroup/ExpendGroupDeleteIT.java` — #18(그 유형을 쓴 지출이 있으면 `3106`)·#19(기본 유형이면 `3107`)·#20(이미 삭제된 유형 재삭제 `3108` — SC-210)·#23(삭제 표시 후에도 그 유형을 참조하던 목표금액 행·참조가 **유지된다** — FR-211). #20 은 **순서 검증**이기도 하다 — 이미 삭제된 **기본** 유형을 다시 삭제하면 `3108` 이지 `3107` 이 아니다
- [ ] T030 [P] [US3] `.../expendgroup/ExpendGroupUpdateIT.java` — #21(기본 유형의 **이름** 변경 `3105`)·#22(기본 유형의 **사용 여부·아이콘** 변경은 **성공** — SC-208·FR-220)·#24(2.8 관리 목록이 삭제분을 포함하고 각 행에 `deleted` 가 있다 — FR-223). **#21·#22 가 한 쌍이다** — 판정 순서를 뒤집으면 기본 유형이 아무것도 못 바꾸게 되어 #22 가 깨진다

- [ ] T053 [P] [US3] `.../expendgroup/ExpendGroupOwnershipIT.java` — 남의 지출유형 ID 로 상세(2.9)·수정(2.11)·삭제(2.12)를 전부 불러 **셋 다 `3103`** 인지 본다. **존재하지 않는 ID 도 같은 `3103`** 이어야 한다 — 코드가 갈리면 남의 자원이 존재한다는 사실이 새어 나간다(FR-201). T014 의 수단판(`3003`)과 한 쌍이며, 이 둘이 함께 있어야 SC-207("수단 `3003`, 지출유형 `3103` 100% 거부")이 검증된다. **ID 는 뒤에 붙었지만 실행 위치는 여기다**(기존 52건의 상호 참조를 밀지 않으려고 번호만 이어 붙였다)

### Implementation for User Story 3

- [ ] T031 [P] [US3] `.../dto/request/ExpendGroupCreateRequest.java` 를 만든다 — `name`·`inUse`(필수). **`defaultGroup` 을 받지 않는다** — 받으면 사용자가 임의로 기본 유형을 만들어 `3107`(삭제 불가) 보호를 스스로에게 걸 수 있다. 기본 10종은 002 의 가입 흐름만 만든다(api-contract.md §6). 아이콘은 `multipart` 파트라 이 DTO 에 넣지 않는다
- [ ] T032 [P] [US3] `.../dto/response/ExpendGroupResponse.java`·`ExpendGroupListResponse.java` 를 만든다 — `expendGroupId`·`name`·`inUse`·`defaultGroup`·`deleted`·`iconUrl`. **`deleted` 를 싣는다**(FR-223, 2.8·2.9). **`iconUrl` 은 값이 없어도 필드를 생략하지 않고 `null` 로 내려보낸다**(SC-209) — Jackson 설정이 `null` 을 빼지 않는지 확인한다. 프론트가 `'iconUrl' in obj` 로 분기하면 생략과 `null` 이 다른 결과를 낸다. 목록은 `list` 하나만 담는다 **일부는 Phase 4 에서 이미 만들었다** — 2.13 을 부를 수 없으면 T024 가 성립하지 않아 `ExpendGroupActiveResponse`(2.13 의 좁은 항목: `expendGroupId`·`name`·`iconUrl`·`defaultGroup`)와 제네릭 껍데기 `ExpendGroupListResponse<T>` 를 먼저 만들었다. 여기서는 **2.8·2.9 가 쓰는 넓은 `ExpendGroupResponse`**(`inUse`·`deleted` 포함)만 추가한다
- [ ] T033 [US3] `.../mapper/ExpendGroupMapper.java` 를 MapStruct 로 만든다 — Entity ↔ DTO 변환에 더해 **`icon_filename` → `iconUrl` 조립**을 여기서 한다(research.md §5). 경로 앞부분(`/api/v1/expend-groups/icons/`)을 붙이는 일을 Service 에 두지 않는다 — Service 가 HTTP 경로 문자열을 알 이유가 없고, 응답을 만드는 지점이 Mapper 다. 파일명이 `null` 이면 `iconUrl` 도 `null` 이다. T032 에 의존 **Phase 4 에서 이미 만들었다** — `iconUrl` 조립(`ICON_URL_PREFIX` + 파일명, 파일명이 `null` 이면 `null`)과 `toActiveResponse` 가 들어 있다. 여기서는 넓은 `ExpendGroupResponse` 로 가는 매핑만 추가한다
- [ ] T034 [US3] `.../service/ExpendGroupService.java` 에 등록(2.7)·목록(2.8)·상세(2.9)를 구현한다 — 소유자는 토큰의 `id_key`, `default_group` 은 **`false` 고정**, `deleted=false`. 이름 유일성은 **선검사 + 유니크 위반 처리 양쪽**을 둔다(research.md §7) — 선검사만으로는 동시 등록 창을 닫지 못한다. 위반은 `3101` 이며 **삭제 표시된 행도 센다**(FR-209). 조회는 소유자 불일치를 `3103` 으로 묶는다. T006·T027·T031~T033 에 의존(T027 과 같은 파일이라 순차)
- [ ] T035 [US3] `.../service/ExpendGroupService.java` 에 수정(2.11)을 구현한다 — api-contract.md §5 의 순서: 대상 조회(`3103`) → **`name` 을 보냈는가**를 먼저 보고 → 보냈고 기본 유형이면 `3105` → 보냈고 이름이 중복이면 `3101` → `iconFile` 을 보냈고 형식·크기 위반이면 `3102` → UPDATE. **"필드가 왔는가"를 기본 유형 여부보다 먼저 본다** — 순서를 뒤집으면 기본 유형에 `inUse` 만 보낸 요청까지 막혀 SC-208 후반이 깨진다. T034 와 같은 파일이라 순차
- [ ] T036 [US3] `.../service/ExpendGroupService.java` 에 삭제(2.12)를 구현한다 — 순서가 곧 응답 코드다: 대상 조회(`3103`) → 이미 `deleted=true`(`3108`) → `default_group=true`(`3107`) → **그 유형을 쓴 지출이 있는가**(`3106`) → `deleted=true` UPDATE. **`tbl_expense` 하나만 본다** — 고정지출·목표금액·통계는 참조를 유지하므로(FR-211) 삭제를 막지 않는다. 수단의 `purpose` 변경이 4개 테이블을 보는 것과 다른 점이다. T035 와 같은 파일이라 순차
- [ ] T037 [US3] `.../controller/ExpendGroupController.java` 를 만들고 2.7~2.9·2.11~2.13 을 붙인다 — 2.7·2.11 은 **`multipart/form-data`**(`iconFile` 파트 포함), 나머지는 JSON 이다. `GET /expend-groups/active`(2.13)와 `GET /expend-groups/{expendGroupId}`(2.9)의 매핑 충돌에 주의한다. 아이콘 조회(2.10)는 **여기 두지 않는다** — 응답 규격이 달라 Controller 를 분리한다(T041) **파일은 Phase 4 에서 이미 만들었다** — `GET /expend-groups/active`(2.13) 하나만 붙어 있다. 여기서는 나머지 5건을 추가하며, `/active` 리터럴이 `/{expendGroupId}` 템플릿보다 **위에 있어야** 한다(스프링이 리터럴을 먼저 고르지만 그 우선순위는 코드에 보이지 않는다)

**Checkpoint**: US1~US3 이 각각 독립적으로 동작한다. 아이콘을 빼면 13건 중 12건이 선다

---

## Phase 6: User Story 4 - 지출유형 아이콘을 주고받는다 (Priority: P3)

**Goal**: 아이콘을 등록·수정 요청의 파트로 올리고 조회 전용 API(2.10)로 내려받는다

**Independent Test**: 아이콘을 올린 유형의 `iconUrl` 로 이미지를 받아 바이너리가 오는지, 토큰 없이 부르면 막히는지 확인한다

**⚠️ 이 스토리가 프로젝트에서 유일하게 응답 규격의 예외를 만든다.** 2.10 은 **성공 = 바이너리 / 비즈니스 실패 = 래퍼 / 인증 실패 = 래퍼 없는 401** 세 갈래다

### Tests for User Story 4

- [ ] T038 [P] [US4] `.../icon/IconUploadIT.java` — #27(`png`·`jpg`·`gif` 아닌 파일 `3102` — SC-211)·**#28(확장자만 `.png` 로 바꾼 텍스트 파일도 `3102`)**·#29(1MB 초과 `3102`)·#37(2.11 에서 `iconFile` omit 시 기존 아이콘 유지 — FR-218). **#28 이 FR-219 의 핵심이다** — 확장자만 검사하는 구현은 이 시험에서만 걸린다. `MockMultipartFile` 로 낸다
- [ ] T039 [P] [US4] `.../icon/IconDownloadIT.java` — #30(Bearer 없이 부르면 **래퍼 없는 HTTP 401**)·#31(성공은 `{ resCode, data }` 가 **아니라** 이미지 바이너리 — SC-201)·#32(없는 파일명은 `3104` 이고 **래퍼를 쓴다**)·#36(`filename` 에 `..` 을 넣어도 저장 루트 밖을 읽지 않는다). **#30·#31·#32 가 함께 세 갈래 응답을 검증한다** — 하나라도 어긋나면 2.10 의 계약이 깨진 것이다
- [ ] T040 [P] [US4] `.../icon/IconLifecycleIT.java` — #25(아이콘 있는 유형의 `iconUrl` 이 경로 문자열)·#26(**아이콘 없는 유형도 `iconUrl` 필드가 존재하고 `null`** — SC-209)·#33(유형을 삭제 표시해도 **파일은 남는다** — FR-215)·#34(유형 이름을 바꿔도 아이콘이 그대로 유효 — 파일명이 이름을 담지 않는다)·#35(저장된 파일명 전수 검사 — 전부 `{id_key}_{expendGroupId}.{확장자}` 이고 유형 이름 문자열이 한 건도 없다 — SC-212)

### Implementation for User Story 4

- [ ] T041 [US4] `.../service/ExpendGroupIconService.java` 를 만든다 — 업로드(크기 → 형식 판정 → 저장 → `icon_filename` UPDATE)와 조회(파일명으로 읽기, 없으면 `3104`)를 담당한다. **검증 순서를 지킨다**: ① 크기(1MB 초과 → `3102`) ② 내용 기반 형식 판정(→ `3102`) ③ 확장자 결정. **크기를 디코딩보다 먼저 본다** — 큰 파일을 먼저 디코딩하면 메모리를 그만큼 쓴다. T008·T009·T010 에 의존
- [ ] T054 [US4] `common-mod/src/main/java/com/dbdomino/moneylog/common/error/GlobalExceptionHandler.java` 에 `MaxUploadSizeExceededException` → **`3102`**(`EXPEND_GROUP_ICON_INVALID`) 매핑을 추가한다. T002 가 `spring.servlet.multipart.max-file-size: 1MB` 를 걸면 1MB 초과 요청은 **Controller 에 닿기 전에** 이 예외로 끝나므로, T041 의 애플리케이션 크기 검사는 그 요청을 보지 못한다. 매핑이 없으면 현재 핸들러의 `Exception` 갈래를 타 **`9000` + HTTP 500** 이 나가고 quickstart #29·SC-211("1MB 초과 100% `3102`")이 깨진다. **002 가 만든 파일을 고치는 작업 중 하나다** — T051 의 회귀 확인에 포함한다. **ID 는 뒤에 붙었지만 실행 위치는 T041 다음이다**
- [ ] T042 [US4] `.../service/ExpendGroupService.java` 의 등록(2.7)·수정(2.11)에서 아이콘 저장을 **커밋 이후로 미룬다** — ① 행 INSERT(PK 획득) → ② 커밋 → ③ 파일 저장 → ④ `icon_filename` UPDATE(research.md §2 · icon-storage.md §2). 파일 시스템은 트랜잭션에 참여하지 않으므로 순서를 뒤집으면 **주인 없는 파일이 영영 남는다**(파일명의 PK 가 재사용되지 않는다). 반대로 DB 가 먼저면 최악이 "`icon_filename` 이 `null` 인 행"인데 그건 **정상 상태**다 — 아이콘 없는 유형은 원래 허용된다. T036·T041 에 의존(같은 파일이라 순차)
- [ ] T043 [US4] `.../controller/ExpendGroupIconController.java` 를 만들고 2.10 을 붙인다 — `GET /api/v1/expend-groups/icons/{filename}`. **래퍼를 쓰지 않는 유일한 Controller** 다. 성공은 `ResponseEntity<byte[]>` 에 `Content-Type` 을 **저장된 확장자에서** 정해 실어 보내고(T008 의 내용 기반 판정이 확장자를 정확히 만들어 두었으므로 이 시점에는 믿어도 된다), 파일 없음은 `3104` 래퍼로 나간다. T041 에 의존
- [ ] T044 [US4] 2.10 의 **인증 실패만 래퍼 없는 401** 로 나가도록 `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/security/RestAuthEntryPoint.java` 에 이 경로 예외를 넣는다(icon-storage.md §3 주의 2). 002 의 기본 동작은 `SecurityResponseWriter` 를 타는 `{ resCode, data }` + **HTTP 200** 이고, 코드는 `TokenAuthenticationFilter` 가 요청 속성에 담아 둔 값(`1001` 토큰 없음 · `1004` 비활성 계정 · `1006` 세션 무효)에서 온다. 이 경로만 **그 분기 전체를 건너뛰고** 본문 없는 401 로 끝나야 한다 — 4자리 코드를 실을 JSON 본문 자체가 없기 때문이다(US4 시나리오 4). **경로 판정을 한 곳에만 둔다** — 상수를 `ExpendGroupIconController` 가 소유하고 EntryPoint 가 참조하게 해서 경로 문자열이 두 곳에 복제되지 않게 한다
- [ ] T045 [US4] `GlobalExceptionHandler` 가 2.10 의 **성공 응답을 가로채지 않는지** 확인한다(icon-storage.md §3 주의 1) — 성공은 예외가 아니라 정상 반환이므로 원래 닿지 않지만, `produces` 협상이나 공통 처리가 `Content-Type` 을 덮어쓰지 않는지 실제 응답 헤더로 검증한다. 완료 기준은 실제 응답 헤더 3건이다 — ① 성공: `Content-Type: image/png`(또는 `image/jpeg`·`image/gif`) + 본문이 이미지 바이트 ② 파일 없음: `Content-Type: application/json` + `{ resCode: 3104, data: { message } }` + HTTP 200 ③ 인증 실패: HTTP 401 + 본문 0바이트. 세 줄이 다 맞으면 코드 변경 없이 끝난다. 하나라도 어긋나면 `GlobalExceptionHandler` 또는 `produces` 협상이 원인이므로 T043 으로 돌아간다

**Checkpoint**: API 13건이 전부 선다. 2.10 의 세 갈래 응답이 계약대로 나온다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 스토리 경계를 넘는 검증과 완료 판정. quickstart.md §3 의 "스냅샷·경계"와 §4 에 대응한다

- [ ] T046 `.../paymentmethod/PaymentMethodPurposeChangeIT.java` — #39(참조 1건 이상인 수단의 `purpose` 변경 `3005` — SC-206). **`@ParameterizedTest` 로 4번 반복한다** — 참조원을 `tbl_expense`·`tbl_income`·`tbl_fixed_expense`·`tbl_fixed_expense_monthly` 로 하나씩 바꿔 가며 매번 `3005` 가 나오는지 본다(quickstart #39 가 명시적으로 요구한다). **한 테이블만 시험하면 나머지 3개를 빠뜨린 구현이 그대로 통과한다** — T020 이 경고한 바로 그 실패 모드를 잡으라고 있는 시험이다. ·#40(`purpose` 를 **omit** 하고 다른 필드만 수정하면 성공하고 **참조 검사를 하지 않는다**)·#41(참조 0건이면 `purpose` 변경 성공). 참조를 만들려면 `tbl_expense` 에 행이 필요한데 004 의 API 가 아직 없으므로 **Repository 로 직접 넣는다** — 검증 대상은 003 의 판정이지 004 의 등록 흐름이 아니다
- [ ] T047 `.../expendgroup/NameSnapshotIT.java` — #38(수단·유형 이름을 바꾼 뒤 과거 지출을 조회하면 스냅샷 이름이 **바뀌지 않는다** — SC-205·FR-208). 위와 같이 지출 행은 Repository 로 직접 만든다. 이 시험이 지키는 것은 "이름은 참조가 아니라 그 시점의 값"이라는 규칙이다
- [ ] T048 `.../ApiResponseContractIT.java`(002 가 만든 파일)에 003 의 13건을 추가하거나 `.../expendgroup/ExpendGroupResponseContractIT.java` 를 새로 만든다 — #42(13건 전부 호출해 **12건은 `{ resCode, data }`, 2.10 만 예외** — SC-201). 2.10 을 예외로 인정하는 것과 나머지를 규격으로 묶는 것이 한 자리에 있어야 그 예외가 하나뿐임이 드러난다
- [ ] T055 목록 4건(2.2·2.6·2.8·2.13)을 전부 호출해 `data` 에 **`list` 만** 있고 `offset`·`limit`·`totalCount` 가 없는지 본다 — quickstart #14(FR-217). **네 API 가 모두 선 뒤에야 한 자리에서 볼 수 있어 Phase 7 에 둔다**(US2 에 두면 2.8 이 아직 없다). 002 의 관리자 회원 목록(1.13)이 페이징 3필드를 싣는 것과 형태가 다른 것이 **의도**임을 여기서 고정한다
- [ ] T056 **FR-212 는 이 기능에서 구현하지 않는다는 것을 확인한다** — `inUse=false` 로 바꾸는 동작 자체는 T035(2.11)가 만들고, 그 값을 보고 `3601` 로 거절하는 쪽은 `006-backend-target-statistics` 의 목표금액 API 다(spec.md § 에러코드 · data-model.md §2). 003 이 할 일은 `in_use` 값을 정확히 저장하는 것뿐이며 `3601` 을 이 기능에서 쓰지 않는다. 검증: `grep -rn '3601' app-mod/money-backend-app/src` → **0건**. FR-201~224 중 유일하게 대응 작업이 없는 항목이라, 그 이유를 여기 남겨 두지 않으면 다음 사람이 누락으로 오해한다
- [ ] T049 로그를 **실측**한다 — 2.7·2.11 요청 로그에 파일 내용이 아니라 파일명·크기만 남는지, 2.10 응답 로그에 바이너리가 찍히지 않는지 확인한다(api-contract.md §7). T011 이 실제로 걸렸는지는 로그를 봐야 안다
- [ ] T050 `git diff sql/schema-moneylogdb.sql` 이 **비어 있는지** 확인한다 — 이 기능은 스키마를 바꾸지 않는다. 덤프가 바뀌었다면 Entity 를 의도치 않게 건드린 것이므로 원인을 찾는다(원칙 VI). 덤프 파일을 손으로 편집하지 않는다
- [ ] T051 `./gradlew :data-mod:test :app-mod:money-backend-app:test` 로 두 모듈을 돌리고, quickstart.md §3 의 시나리오 **42건**이 모두 `#N` 으로 대응되는지 대조한다. **002 의 테스트가 계속 통과해야 한다** — 003 은 002 가 만든 파일 다섯을 고친다(T009 `IconStorage`·T010 `IconProperties`·T011 `ApiLoggingAspect`·T054 `GlobalExceptionHandler`·T044 `RestAuthEntryPoint`). 특히 앞의 둘은 002 의 가입 흐름(1.2)이 직접 쓰므로 회귀 위험이 가장 크다. `./gradlew test`(전체)는 쓰지 않는다(`money-app` 레거시 3건이 깨져 있다)
- [ ] T052 커밋 전 자가 점검(CLAUDE.md) — ① 응답이 `{ resCode, data }` 인가(2.10 예외 1건은 명세가 승인했다) ② **Entity 가 노출되지 않는가** ③ 명세 표의 설명 칸이 비어 있지 않은가(`phase2-수단-지출유형/` 문서의 빈칸 확인 — 현재 8곳 남아 있다) ④ DB 구조가 바뀌지 않았는가(T050) ⑤ `System.out.println` 이 없는가 ⑥ Controller 가 Repository 를 직접 부르지 않는가 ⑦ `PUT` 이 0건인가

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1, T001~T004)**: 의존 없음. **T001(명세 개정)은 코드보다 먼저** 한다(원칙 V)
- **Foundational (Phase 2, T005~T011)**: Setup 완료 후. **모든 User Story 를 막는다**
- **US1 (Phase 3, T012~T022)**: Foundational 완료 후. 다른 스토리에 의존하지 않는다
- **US2 (Phase 4, T023~T027)**: 수단 부분(T023·T025·T026)은 US1 과 **같은 파일**(`PaymentMethodService`·`PaymentMethodController`)을 고치므로 US1 뒤에 온다. 지출유형 부분(T024·T027)은 `ExpendGroupService` 를 **US3 의 T034 와 공유**하므로 **US3 보다 먼저** 끝내야 한다
- **US3 (Phase 5, T028~T037·T053)**: Foundational 과 **T027 완료 후**. US1(수단)과는 파일이 겹치지 않아 병행할 수 있지만, `ExpendGroupService.java` 를 T027 과 공유하므로 그 하나만 선행이다
- **US4 (Phase 6, T038~T045)**: **US3 이 먼저** 서야 한다 — 아이콘은 지출유형에 붙는 것이라 등록·수정 흐름이 있어야 얹을 수 있다
- **Polish (Phase 7, T046~T052)**: 인도하려는 스토리가 전부 끝난 뒤

### 파일 충돌로 순차가 강제되는 것

| 파일 | 순차로 묶이는 작업 |
|---|---|
| `PaymentMethodService.java` | T019 → T020 → T021 → T025 |
| `PaymentMethodController.java` | T022 → T026 |
| `ExpendGroupService.java` | T027 → T034 → T035 → T036 → T042 |
| `ExpendGroupController.java` | T037 단독 |
| `application.yml` | T002 단독 |
| `IconStorage.java` | T009 단독 (002 가 만든 파일에 연산 추가) |

### Within Each User Story

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- Repository → Service → DTO·Mapper → Controller 순
- 스토리를 끝내고 다음 우선순위로 넘어간다

### Parallel Opportunities

- Setup 의 T002·T003·T004 가 동시에 가능하다. **T001(명세 개정)만 코드 작업보다 먼저** 끝내야 한다(원칙 V) — 나머지 셋과 파일이 겹치지 않으므로 순서 제약이 아니라 착수 시점 제약이다
- Foundational 에서 T005·T006·T007 세 건이 동시에 가능하다(다른 Repository 파일이다)
- 각 스토리의 테스트 작성은 전부 `[P]` 다 — US1 3건, US2 2건, US3 3건, US4 3건
- **US1(수단)과 US3(지출유형)을 서로 다른 사람이 동시에** 진행할 수 있다. 파일이 겹치지 않는다
- DTO 는 대부분 `[P]` 다 — T015·T016·T017·T031·T032 는 각자 다른 파일이다

---

## Parallel Example: User Story 1

```bash
# US1 의 통합 테스트 3건을 함께 작성한다:
Task: "PaymentMethodCreateIT — quickstart #1~#5"
Task: "PaymentMethodDeleteIT — #6·#8·#10"
Task: "PaymentMethodOwnershipIT — #9"

# US1 의 DTO 3건을 함께 만든다:
Task: "PaymentMethodCreateRequest (dto/request)"
Task: "PaymentMethodResponse (dto/response)"
Task: "PaymentMethodListResponse (dto/response)"
```

```bash
# Foundational 의 Repository 3건을 함께 고친다:
Task: "UserPaymentMethodRepository — 소유자·용도·상태 조회"
Task: "UserExpendGroupRepository — 소유자·이름 유일성·상태 조회"
Task: "참조 존재 검사 4개 Repository — existsBy 계열"
```

---

## Implementation Strategy

### MVP First (User Story 1만)

1. Phase 1 Setup — 명세 개정과 설정
2. Phase 2 Foundational — **전 스토리를 막는 단계다**
3. Phase 3 US1 — 수단 관리
4. **여기서 멈추고 검증한다**: quickstart #1~#10 이 통과하면 수단이 선 것이다
5. 이 시점의 가치: 004·005 의 지출·소득·고정지출이 참조할 FK 대상이 생긴다

### Incremental Delivery

1. Setup + Foundational → 조회·파일 기반이 선다
2. US1 → 수단 관리 → **MVP 인도**
3. US2 → 입력 화면용 두 목록 → 인도
4. US3 → 지출유형 관리 → 인도 (여기까지면 13건 중 12건)
5. US4 → 아이콘 → 인도 (응답 규격 예외 1건이 여기서 생긴다)
6. Polish → 경계 검증과 완료 판정

### Parallel Team Strategy

1. Setup + Foundational 을 함께 끝낸다
2. 그 뒤:
   - 개발자 A: US1 → US2 의 수단 부분(T023·T025·T026)
   - 개발자 B: **US2 의 지출유형 부분(T024·T027)** → US3 → US4
3. Polish 는 다시 함께

---

## Notes

- `[P]` = 다른 파일, 미완료 작업에 의존하지 않음
- `[Story]` 라벨로 작업과 User Story 를 잇는다
- 구현 전에 테스트가 **실패하는 것**을 확인한다
- 작업 단위 또는 논리적 묶음마다 커밋한다
- **이 기능에서 스키마는 바뀌지 않는다.** Entity 를 고치고 싶어지면 그 자체가 신호다
- **002 가 만든 파일을 고치는 작업은 다섯이다** — T009(`IconStorage`)·T010(`IconProperties`)·T011(`ApiLoggingAspect`)·T054(`GlobalExceptionHandler`)·T044(`RestAuthEntryPoint`). 앞의 둘은 002 의 **가입 흐름(1.2, 기본 유형 10종 + 시드 아이콘 복사)** 이 직접 쓰므로 회귀 위험이 가장 크다. 다섯 건 전부 T051 에서 002 테스트와 함께 돌린다
