---
description: "Task list for 006-backend-target-statistics"
---

# Tasks: 지출유형별 목표금액과 월별 통계

**Input**: `/specs/006-backend-target-statistics/` 의 설계 산출물

**Prerequisites**: plan.md · spec.md · research.md · data-model.md · contracts/ (api-contract.md · target-amount.md · statistics-snapshot.md) · quickstart.md

**Tests**: 포함한다. quickstart.md §3 이 시나리오 **57건**(1~54 + 30-1·46-1·46-2)을 `#N` 으로 매기고 §4 가 그것을 완료 판정으로 쓰므로, 통합 테스트가 곧 인수 기준이다

**Organization**: User Story 단위로 묶어 각 스토리를 독립적으로 구현·검증·인도할 수 있게 한다

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완 작업에 의존하지 않음)
- **[Story]**: 이 작업이 속한 User Story (US1~US3)
- 설명에 정확한 파일 경로를 적는다

## Path Conventions

Spring Boot 멀티모듈이다. 저장소 루트 기준 경로를 쓴다.

- 백엔드 메인: `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/`
- 백엔드 테스트: `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/`
- 공통: `common-mod/src/main/java/com/dbdomino/moneylog/common/`
- 데이터: `data-mod/src/main/java/com/dbdomino/moneylog/data/`

**새 의존성이 없다.** 루트 `build.gradle` 을 건드리지 않는다. `sql/schema-moneylogdb.sql` 도 **변경 없음**이 이 기능의 전제다 — 001 이 목표금액 2종·통계 4종의 테이블과 제약을 이미 만들어 두었다.

## 이 기능이 마지막이다

006 이 끝나면 백엔드 API 56건이 전부 선다(002 16 · 003 13 · 004 12 · 005 9 · 006 6). `openapi.yaml` 과 `docs/API-문서.md` 도 그때 완성된다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 착수 전 문서 확정. **T001·T002 는 코드보다 반드시 먼저** 한다(헌장 원칙 V). 새 의존성·설정 변경은 없다

- [X] T001 `프로젝트설계/기능명세상세-백엔드/phase5-목표-통계/5.6-StatisticsMonthlySave.md`(65행) 실패 표의 `3604` 설명에서 **"(정책에 따라)"를 지우고 "현재 연월 초과"로 바꾼다** — 확정된 규칙(clarify 세션)은 "현재 연월을 **초과**하는 달만 거절하고 이번 달은 저장할 수 있다"이다. `grep -rn '정책에 따라' 프로젝트설계/기능명세상세-백엔드/phase5-목표-통계/` 가 **0건**이 되어야 한다(quickstart §0). 그대로 두면 구현자가 "미래 월"의 경계를 `>=` 로 잡아 **이번 달 저장이 막힌다**
- [X] T002 `프로젝트설계/기능명세상세-백엔드/phase5-목표-통계/5.5-StatisticsMonthlyGet.md` 에 **수단별 요약의 모집단**을 명시한다(FR-521a) — 확정된 규칙은 **두 집합의 합집합**이다: ① 그 달 지출이 1건 이상인 수단은 **상태와 무관하게 전부**, ② 지출 0원 행은 **저장 시점에 사용 중인 `purpose=EXPENSE` 수단만**. **빠뜨리면 위험하다** — 구현자가 "회원 소유 수단 전부"로 읽으면 버린 카드의 0원 행이 매달 쌓인다
- [X] T003 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java` 에 이 기능이 쓰는 코드 **4개**(`3601` `3602` `3603` `3604`)가 있는지 확인한다 — 001·002 가 이미 넣어 둔 것으로 보이므로 **없는 것만 추가**한다. `3603` 은 연·월뿐 아니라 **`view` 값 오류에도 쓴다**(api-contract §6)
- [X] T004 `data-mod/.../repository/` 의 006 용 조회 6종이 그대로 쓸 수 있는지 확인한다 — `UserExpendTargetDefaultRepository`(회원 전체·회원+유형) · `UserExpendTargetMonthlyRepository`(회원+연월·회원+연월+유형) · `UserStatisticsRepository`(회원+연월) · 통계 상세 3종(`findByStatisticsIdx...` · `deleteByStatisticsIdx`)을 001 이 이미 넣었다. **부족한 것만** 더한다

**Checkpoint**: 설계 명세와 에러코드가 서고, 001 이 깔아 둔 자산의 범위가 확인된다

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story 가 공유하는 것. **여기가 끝나기 전에는 어떤 스토리도 시작할 수 없다**

- [X] T005 `.../backend/support/YearMonthValue.java` 에 **연 범위를 호출자가 정할 수 있게** 한다 — 005 가 만든 이 값 객체는 `MIN_YEAR=1900`·`MAX_YEAR=9999` 로 고정돼 있는데 **006 은 2000~2100 이다**(FR-525). plan.md 는 "005 가 만든 것을 재사용한다"고 적었으나 그대로는 `year=1999` 가 통과한다. `require(year, month, ErrorCode, minYear, maxYear)` 오버로드를 더하고 기존 4-인자 호출은 005 의 범위를 그대로 쓰게 둔다 — **005 의 시그니처·동작을 바꾸지 않는다**
- [X] T006 [P] `.../backend/support/StatisticsYearMonth.java` 또는 006 전용 상수를 두어 `2000`·`2100` 을 <b>한 곳</b>에 가둔다 — 이 범위가 **5.1·5.2·5.4·5.5·5.6 다섯 곳**에 걸린다(api-contract §5). 각 DTO 가 숫자를 직접 적으면 한 곳만 고쳐도 나머지 넷이 갈린다. **현재 연도 기준 상대 범위를 쓰지 않는다** — 경계가 해마다 움직이면 경계 시험이 시간에 의존한다
- [X] T007 [P] `.../backend/service/statistics/TargetResolver.java` 를 만든다 — **적용 금액은 `월별 값 ?? 기본 값`** 이며 그 판정이 **5.1·5.2·5.5 세 곳**에 필요하다(FR-507). `null` 과 `0` 의 비대칭을 여기 가둔다: 월별이 `null` 이면 "저장한 적 없음"이라 기본으로 떨어지고, `0` 이면 "그 달엔 쓰지 않겠다"라 **0 이 그대로 적용 금액**이다. 둘을 같게 다루면 0원 목표가 기본값으로 덮인다
- [X] T008 [P] `.../backend/service/statistics/WeekBoundaryResolver.java` 를 만든다 — **월요일 시작**이며 그 달 1일이 월요일이 아니면 첫 주는 **1일부터 첫 일요일까지**, 마지막 주는 **말일에서 끊는다**(FR-520). 즉석 계산은 매번 만들고 저장본은 저장된 경계를 그대로 읽으므로, 계산 규칙이 한 곳에 있어야 두 경로의 주 구분이 갈리지 않는다
- [X] T009 `.../backend/service/statistics/StatisticsCalculator.java` 를 만든다 — **이 기능에서 가장 중요한 분리다.** 5.5(즉석 계산)와 5.6(저장)이 **같은 계산기를 써야** 한다. 각자 구현하면 "지금 보이는 값"과 "저장된 값"이 달라지는데 그게 바로 이 기능이 방지하려는 상황이다. 합계·비율 6값 · 주별 · 유형별 · 수단별을 한 번에 낸다
- [X] T010 T009 의 계산기가 **`005` 의 lazy 생성을 일으키지 않게** 한다 — 고정지출 합계는 `UserFixedExpenseMonthlyRepository.findByUserIdKeyAndYearAndMonth` 로 **읽기만** 한다. `FixedExpenseMonthlyService.ensureMonthlyRows` 를 부르면 안 된다: 통계 조회가 다른 테이블에 쓰는 부작용이 하나 더 생기고, 005·006 양쪽 Assumptions 이 "생성을 일으키는 것은 4.5·4.8·4.9 셋뿐"으로 못박았다. **한 번도 열지 않은 달의 고정지출 합계는 0 이다**(시나리오 #36)
- [X] T011 [P] T009 에 **유형별과 수단별의 비대칭**을 넣는다 — 유형별은 그 달 지출이 **0원인 유형을 빼고**(FR-521), 수단별은 **0원도 넣는다**(FR-521a). 수단별 모집단은 **두 집합의 합집합**이다: ① 그 달 지출이 1건 이상인 수단은 상태 무관 전부, ② 0원 행은 저장 시점 사용 중인 `purpose=EXPENSE` 수단만. ②의 조건을 빠뜨리면 버린 카드의 0원 행이 매달 쌓인다
- [X] T012 [P] T009 의 **사용률에 상한을 건다** — `usage_rate` 가 `numeric(6,2)` 라(덤프 확인) 최대 `9999.99` 다. 목표 1,000원에 지출 1,000만원이면 1,000,000% 가 나오는데 그대로 저장하면 **DB 오류가 `9000` 으로 새어 나간다**. 넘으면 `9999.99` 로 자른다(FR-522). 목표가 0 이면 나눗셈이 성립하지 않으므로 사용률은 **0** 이다
- [X] T012a [P] T009 에 **목표 대비 상태 판정**을 넣는다(FR-523) — `UNDER`(사용률 **90% 미만**) · `OK`(**90 이상 110 이하**) · `OVER`(**110% 초과**) 셋뿐이다(statistics-snapshot.md §5). **빠뜨리면 저장이 통째로 실패한다** — `tbl_statistics_expend_group.status` 가 `varchar(10) NOT NULL` 이고 `ck_stat_group_status` 가 그 세 값만 허용하므로(덤프 확인), 채우지 않거나 다른 문자열을 넣으면 DB 오류가 **`9000`** 으로 새어 나간다. **목표가 0 이면 사용률이 0 이므로 `UNDER`** 다. 기준이 나중에 바뀌어도 **이미 저장된 행은 다시 계산하지 않는다**
- [X] T013 [P] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/AbstractApiIT.java` 에 006 이 쓸 헬퍼를 더한다 — 목표금액을 저장하는 `putDefaultTarget`·`putMonthlyTarget`, 통계 행을 세는 `countStatistics(member, year, month)`, 통계 상세 행 수를 읽는 `countStatisticsDetails(...)`, 저장된 `savedAt` 을 읽는 헬퍼. **`jdbc` 갱신은 `tx.executeWithoutResult` 안에서** 한다
- [X] T014 [P] 테스트 패키지 2개의 공통 기반을 만든다 — `.../backend/target/AbstractTargetIT.java` · `.../backend/statistics/AbstractStatisticsIT.java`. 둘 다 T013 의 `AbstractApiIT` 를 상속한다. **미래 월 판정(FR-527)이 시간에 의존하므로** 이번 달·다음 달을 `YearMonth.now()` 기준 상대값으로 잡는 헬퍼를 `AbstractStatisticsIT` 에 둔다(005 의 `AbstractSyncIT` 와 같은 처방)

**Checkpoint**: 계산기·주 경계·목표 판정·연 범위가 서고, 두 스토리가 이 위에 얹힌다

---

## Phase 3: User Story 1 - 지출유형별 목표를 정한다 (Priority: P1) 🎯 MVP

**Goal**: 기본 목표를 정하고(5.3), 특정 달만 다르게 정하고(5.4), 목록·단건으로 확인한다(5.1·5.2)

**Independent Test**: 한 유형에 기본과 월별 목표를 각각 저장해 두 값이 독립적으로 읽히는지 확인한다. 통계 API 없이 완결된다

### 테스트 (구현보다 먼저 쓰고 실패를 확인한다)

- [X] T015 [P] [US1] `.../backend/target/TargetUpsertIT.java` — quickstart #1·#2·#10. 목표가 없던 유형에 저장하면 새로 만들어지고(upsert), 다시 저장하면 **같은 행이 갱신되어 행이 늘지 않는다**. **0원 저장은 성공**이다 — 유효한 값이다
- [X] T016 [P] [US1] `.../backend/target/TargetTwoLayerIT.java` — quickstart #3·#4 (SC-502). **두 층이 독립임을 건다**: 기본을 바꿔도 저장된 월별 값이 그대로이고, 월별을 저장해도 기본이 바뀌지 않는다. 기본을 월별로 복사해 두지 않는다는 것이 요점이다(FR-505)
- [X] T017 [P] [US1] `.../backend/target/TargetNullVsZeroIT.java` — quickstart #5·#6·#7·#8 (SC-503). **네 개가 한 묶음이다.** 월별을 저장한 적 없으면 `monthlyTargetAmount` 가 **`null`**, 0원으로 저장했으면 **`0`**, 기본을 저장한 적 없으면 `defaultTargetAmount` 가 **`0`**(`null` 이 아니다). **#8 이 가장 놓치기 쉽다** — `monthlyTargetAmount` 필드가 **생략되지 않고 `null` 로 와야** 한다. Jackson 이 `null` 필드를 빼면 프론트가 "없음"과 "필드 자체가 없음"을 구분할 수 없다
- [X] T018 [P] [US1] `.../backend/target/TargetRangeIT.java` — quickstart #9 (SC-504). 1억 초과는 `3602` 다. 경계값 **정확히 1억은 성공**, 음수는 거절. 범위가 `0 ~ 100,000,000` 이다(FR-504)
- [X] T019 [P] [US1] `.../backend/target/TargetGroupStateIT.java` — quickstart #11·#12·#13·#14. `in_use=false` 유형의 단건 조회·변경은 `3601`(FR-510), 목록에서는 **제외**(FR-509), 삭제 표시된 유형의 목표 행은 **유지**(FR-511). **#14 가 판정 순서를 검증한다** — 남의 유형 ID 는 그것이 `in_use=false` 여도 **`3103`** 이다(`3601` 이 아니다). 순서가 뒤집히면 남의 유형이 실재함이 코드 차이로 새어 나간다
- [X] T020 [P] [US1] `.../backend/target/TargetListIT.java` — quickstart #15·#16. `offset` 이 `limit` 의 배수가 아니면 `9001`, `totalCount` 는 **사용 중 유형만** 센다(FR-526). 연·월이 **필수**인 이유는 기본만 보는 화면도 조회 연·월을 넘겨 응답 형태를 하나로 유지하기 때문이다

### 구현

- [X] T021 [P] [US1] `.../backend/dto/request/ExpendTargetListQuery.java` — 5.1 의 `year`·`month`·`offset`·`limit` **전부 필수**. 연·월 오류는 `3603`, 페이징 오류는 `9001` 이다. 두 코드가 다른 것에 주의한다
- [X] T022 [P] [US1] `.../backend/dto/request/ExpendTargetDefaultUpsertRequest.java` · `ExpendTargetMonthlyUpsertRequest.java` — **둘로 나눈다**. 5.3 의 몸통 필드는 `defaultTargetAmount`, 5.4 는 `monthlyTargetAmount` 라 이름이 다르다(각 설계 명세의 Body 표). 두 층이 독립이라 요청에서도 어느 층을 건드리는지가 이름으로 드러나야 하고, 하나로 합치면 URL 만으로 층을 구분해야 해 프론트가 경로를 잘못 짚어도 몸통이 그대로 통과한다. `0 ~ 100,000,000` 검증은 `ExpendTargetFieldRules` 가 `3602` 로 하며 **누락도 같은 코드**다(필드가 하나뿐이라 둘을 가르는 정보 이득이 없다)
- [X] T023 [P] [US1] `.../backend/dto/response/ExpendTargetResponse.java` — `expendGroupId` · `expendGroupName`(**현재** 이름) · `defaultTargetAmount`(항상 숫자) · `monthlyTargetAmount`(**`null` 가능**) 넷이다 — **적용 금액 필드는 넣지 않는다**(5.1·5.2 필드 표 · FR-507 이 두 값만 정했다. 화면이 `monthly ?? default` 로 낸다). **`monthlyTargetAmount` 를 `Long` 으로 둔다** — `long` 이면 `null` 을 표현할 수 없어 "없음"이 `0` 으로 뭉개진다
- [X] T024 [P] [US1] `.../backend/dto/response/ExpendTargetListResponse.java` — `list` + `year` · `month` · `offset` · `limit` · `totalCount` 를 **형제 필드**로 둔다(FR-526)
- [X] T025 [US1] `.../backend/mapper/ExpendTargetMapper.java` — Entity ↔ DTO. **이름은 연관에서 읽는 현재 이름이다**(target-amount.md §8) — 목표금액은 "지금 유효한 설정"이라 005 의 고정지출과 같은 규칙이고 004 의 스냅샷과 반대다
- [X] T026 [US1] `.../backend/service/ExpendTargetService.java` — 5.1 목록 · 5.2 단건. 목록은 **사용 중 유형만**이고, 월별 목표는 그 달 행을 한 번에 읽어 유형 PK 로 맞춘다(유형마다 단건 조회하면 N+1 이 된다). **`TargetResolver` 는 여기서 쓰지 않는다** — 적용 금액 필드가 응답에 없어 5.5·5.6 전용이다
- [X] T027 [US1] `.../backend/service/ExpendTargetService.java` 에 5.3·5.4 upsert 를 더한다 — **판정 순서는 `3103` → `3601` → `3602`** 다(api-contract §7). 소유자 판정이 가장 먼저이고, 그다음 사용 여부, 마지막이 금액 범위다. 순서가 뒤집히면 남의 유형에 잘못된 금액을 보냈을 때 `3103` 이 아니라 `3602` 가 나가 그 ID 가 실재함이 드러난다
- [X] T028 [US1] T027 의 upsert 를 **동시 요청에 안전하게** 한다 — 유니크 제약이 **`ux_target_default (id_key, expend_group_idx)`** · **`ux_target_monthly (id_key, year, month, expend_group_idx)`** 다(덤프 확인. 이름에 `expend_` 가 없다 — 테이블 이름과 달라 찾을 때 헷갈린다). "조회해서 없으면 INSERT" 만 두면 두 요청이 같은 순간 "없음"을 보고 하나가 유니크 위반으로 실패한다. 005 의 `insertIfAbsent` 와 같은 처방을 쓰거나 위반을 갱신으로 흡수한다(target-amount.md §4)
- [X] T029 [US1] `.../backend/controller/ExpendTargetController.java` — 5.1 `GET /api/v1/expend-targets` · 5.2 `GET /{year}/{month}/{expendGroupId}` · 5.3 `PATCH /default/{expendGroupId}` · 5.4 `PATCH /monthly/{year}/{month}/{expendGroupId}`. **Repository 를 직접 부르지 않는다**(헌장 원칙 II)

**Checkpoint**: 목표금액 두 층이 독립적으로 동작한다. **여기까지가 MVP** 다

---

## Phase 4: User Story 2 - 이번 달 통계를 본다 (Priority: P1)

**Goal**: 연·월로 통계를 조회한다(5.5). 저장본이 있으면 그것을, 없으면 지금 계산한 값을 돌려준다

**Independent Test**: 지출·소득·고정지출을 몇 건 만든 뒤 통계를 조회해 합계·비율·주별·유형별·수단별이 나오는지 확인한다. **저장(US3) 없이 완결된다**

### 테스트

- [X] T030 [P] [US2] `.../backend/statistics/StatisticsCalculateIT.java` — quickstart #17·#32·#35·#36. 저장본이 없으면 즉석 계산이고 `source=CALCULATED` 다. 합계·비율 **6값**(`incomeTotal`·`expenseTotal`·`fixedAmount`·`regularAmount`·두 비율)이 전부 실린다. 지출 합계가 0이면 두 비율이 **0** 이다. **#36 이 005 와의 경계다** — 한 번도 열지 않은 달의 고정지출 합계가 **0** 이고 조회가 월별 내역을 만들지 않는다
- [X] T031 [P] [US2] `.../backend/statistics/StatisticsWeekBoundaryIT.java` — quickstart #24·#25. 그 달 1일이 월요일이 아니면 첫 주는 **1일부터 첫 일요일까지**이고 마지막 주는 **말일에서 끊긴다**. 1일이 월요일인 달도 함께 걸어 두 경우가 다 맞는지 본다
- [X] T032 [P] [US2] `.../backend/statistics/StatisticsGroupSummaryIT.java` — quickstart #26·#30·#31. 지출 0원인 유형은 유형별 요약에 **없다**(FR-521). 목표가 0원이면 사용률이 **0**, 목표 1,000원에 지출 1,000만원이면 **`9999.99` 로 잘린다**. **#31 을 빠뜨리면 런타임에 DB 오류가 `9000` 으로 새어 나간다**
- [X] T033 [P] [US2] `.../backend/statistics/StatisticsMethodSummaryIT.java` — quickstart #27·#28·#29 (FR-521a). **셋이 한 묶음이다.** 지출 0원인 **사용 중** 수단은 **있고**, 지출 0원인 **삭제 표시된** 수단은 **없고**, 그 달 지출이 **있는** 삭제 표시된 수단은 **있다**. 셋을 함께 봐야 두 집합의 합집합이 확인된다 — 유형별과 정반대라 헷갈리는 지점이다
- [X] T032a [P] [US2] `.../backend/statistics/StatisticsStatusIT.java` — quickstart **#30-1** (FR-523). `status` 의 **경계값 넷**을 건다 — 사용률 89.99 → `UNDER`, **90.00 → `OK`**, **110.00 → `OK`**, 110.01 → `OVER`. 경계를 한쪽만 잘못 잡으면(`<` 를 `<=` 로) 90% 와 110% 정확히 걸린 유형이 반대로 분류되는데, 응답이 성공이라 **조용히 틀린다**. **목표가 0 인 유형은 사용률 0 이므로 `UNDER`** 임도 함께 본다
- [X] T034 [P] [US2] `.../backend/statistics/StatisticsViewIT.java` — quickstart #18·#19·#20·#21·#22·#23 (SC-507). 기본 조회와 `view=saved` 가 **같은 동작**, `view=live` 는 저장본을 무시하고 즉석 계산하며 `source=CALCULATED` 에 **`savedAt` 이 함께 실린다**(FR-515). **`view=live` 조회가 DB 스냅샷을 바꾸지 않는다**는 것을 저장본 재확인으로 단언한다. `view` 에 다른 값을 주면 `3603` 이다
- [X] T035 [P] [US2] `.../backend/statistics/StatisticsYearMonthIT.java` — quickstart #33·#34 (SC-510). 월 0·13 과 연 1999·2101 이 **`3603`** 이다. 경계값 **2000·2100 은 성공**. 연·월을 받는 **다섯 API 전부**에 같은 규칙이 걸리는지 함께 본다 — 한 곳만 빠뜨리기 쉽다

### 구현

- [X] T036 [P] [US2] `.../backend/dto/request/StatisticsViewQuery.java` — 5.5 의 `view`. **생략·`saved`·`live` 셋만** 허용하고 그 밖은 `3603` 이다(FR-514). 생략과 `saved` 는 같은 동작이다. 조용히 무시하면 오타(`view=lives`)가 "기본 동작"으로 읽혀 사용자가 최신값을 본다고 믿는다
- [X] T037 [P] [US2] `.../backend/dto/response/StatisticsResponse.java` — 합계·비율 6값 + `source`(`SAVED`/`CALCULATED`) + `savedAt`(**저장본이 있을 때만**) + 배열 3종(주별·유형별·수단별). **배열 3종은 `data.list` 규칙의 적용 대상이 아니다**(FR-526·시나리오 #54) — 목록 API 가 아니라 통계 객체의 구성 요소다
- [X] T038 [P] [US2] `.../backend/dto/response/` 에 상세 3종 응답 DTO 를 만든다 — 주별(`weekIndex`·기간·금액) · 유형별(`expendGroupId`·**저장 당시 이름**·지출·목표·사용률·상태) · 수단별(`paymentMethodId`·이름·금액)
- [X] T039 [US2] `.../backend/mapper/StatisticsMapper.java` — Entity ↔ DTO. **통계 상세의 이름은 저장 당시 스냅샷이다**(FR-519) — 목표금액(현재 이름)과 정반대이며, 원본이 사라져도 화면을 복원해야 하기 때문이다. 이 대비를 javadoc 에 적는다
- [X] T040 [US2] `.../backend/service/StatisticsQueryService.java` — 5.5. **저장본 / 즉석 분기**를 여기 둔다(FR-513). 기본은 저장본이 있으면 `SAVED`, 없으면 `CALCULATED` 다
- [X] T041 [US2] T040 에 `view=live` 갈래를 더한다 — 저장본을 **무시하고** 즉석 계산하되, 저장본이 있으면 **`savedAt` 을 함께 싣는다**(FR-515). 프론트가 "저장본 있음 / 지금 최신"을 한 응답으로 구분한다. **DB 를 건드리지 않는다** — 읽기 전용 경로다
- [X] T042 [US2] `.../backend/controller/StatisticsController.java` — 5.5 `GET /api/v1/statistics/monthly/{year}/{month}`. 연·월은 Path, `view` 는 Query 다

**Checkpoint**: 사용자가 실제로 여는 화면이 선다. **저장 없이도 통계가 보인다**

---

## Phase 5: User Story 3 - 지난달 통계를 확정해 남긴다 (Priority: P2)

**Goal**: 그 달 통계를 스냅샷으로 저장한다(5.6)

**Independent Test**: 통계를 저장한 뒤 원본 지출을 고치고 다시 조회해 저장본이 변하지 않는지 확인한다

### 테스트

- [X] T043 [P] [US3] `.../backend/statistics/StatisticsSaveIT.java` — quickstart #37·#38. 저장 응답에 **`savedAt`·`source=SAVED`** 가 실려 재조회 없이 화면을 갱신할 수 있다(FR-415 와 같은 성격). 저장 후 기본 조회가 `source=SAVED` 다
- [X] T044 [P] [US3] `.../backend/statistics/StatisticsImmutableIT.java` — quickstart #39·#43·#44 (SC-506·SC-508). **저장본 불변의 세 갈래다** — 원본 **지출**을 고쳐도, 참조한 **지출유형**을 삭제 표시해도, **목표금액**을 바꿔도 저장본은 그대로다. 셋 다 걸어야 FR-518 이 확인된다
- [X] T045 [P] [US3] `.../backend/statistics/StatisticsResaveIT.java` — quickstart #40·#41·#42 (SC-505). **#46-2 도 여기서 건다** — 상세 3종에 유니크 제약이 있어(`ux_stat_group`·`ux_stat_method`·`ux_stat_weekly`) 삭제와 삽입의 순서가 어긋나면 두 번째 저장이 `9000` 이 된다. 같은 달을 10번 저장해도 통계 행은 **1건**이고 `savedAt` 이 갱신된다. **재저장은 상세를 지웠다 다시 넣는다**(FR-517) — **#42 가 핵심이다**: 갱신으로 구현하면 재저장 전후로 유형별 행 수가 줄었을 때 **없어진 유형의 행이 남는다**
- [X] T046 [P] [US3] `.../backend/statistics/StatisticsFutureMonthIT.java` — quickstart #45·#46 (FR-527). **미래 월**은 `3604`, **이번 달은 성공**이다. 경계가 `>` 이며 `>=` 로 잡으면 이번 달 저장이 막힌다. 비교는 `연 × 12 + 월` 합성값으로 한다
- [X] T046a [P] [US3] `.../backend/statistics/StatisticsSaveMethodIT.java` — quickstart **#46-1** (FR-524). 5.6 은 연·월을 **Body 로만** 받는다. Query·Path 로 보내면 Body 가 비어 **`3603`** 이고, Body 와 Query 를 함께 보내면 **Body 를 따른다**. 조용히 Query 를 읽어 주면 입력 경로가 둘이 되고 둘이 다른 값을 담았을 때 어느 쪽을 따르는지가 구현에 숨는다. `GET`·`PATCH`·`PUT` 으로는 부를 수 없다 — **HTTP 상태가 아니라 `resCode` 로 단언한다**(이 앱은 실패도 HTTP 200 이다)
- [X] T047 [P] [US3] `.../backend/statistics/StatisticsEmptyMonthIT.java` — quickstart #47·#48·#49 (SC-511·FR-528). 지출·소득이 **한 건도 없는 달**도 저장할 수 있고, 합계·비율 6값이 전부 **0** 이며 유형별 상세는 **빈 배열**이다. 저장 후 조회하면 `source=SAVED` 다

### 구현

- [X] T048 [P] [US3] `.../backend/dto/request/StatisticsSaveRequest.java` — 5.6 의 `year`·`month`. **Body 로 받는다**(FR-524) — POST 는 Path·Query 를 쓰지 않는다. `Integer` 로 받아 누락을 `3603` 으로 거절한다(원시 타입이면 Jackson 이 `0` 을 채워 "보내지 않았다"를 가릴 수 없다)
- [X] T049 [P] [US3] `.../backend/dto/response/StatisticsSaveResponse.java` — `year` · `month` · `savedAt` · `source=SAVED` · `message` **다섯 필드뿐**이다(5.6 필드 표 · api-contract §8). 통계 본문을 싣지 않는다 — 사용자가 5.5 를 보다가 저장을 누르는 흐름이라 화면에 숫자가 이미 있고, 필요한 것은 "저장됨 · 그 시각" 배지를 바꾸는 것뿐이다. 본문까지 실으면 같은 값이 두 응답에 흩어져 갈릴 여지만 생긴다
- [X] T050 [US3] `.../backend/service/StatisticsSaveService.java` — 5.6. **T009 의 계산기를 그대로 쓴다** — 5.5 와 다른 계산기를 쓰면 "지금 보이는 값"과 "저장된 값"이 달라진다
- [X] T051 [US3] T050 에 **미래 월 거절**을 넣는다(FR-527) — 현재 연월을 **초과**하면 `3604` 다. **이번 달은 저장할 수 있다**. 판정 순서는 `3603`(연·월 범위) → `3604`(미래) 다(api-contract §7)
- [X] T052 [US3] T050 의 **재저장을 "상세 삭제 후 삽입"으로** 한다(FR-517) — 통계 행은 upsert 하되 상세 3종은 `deleteByStatisticsIdx` 로 지우고 새로 넣는다. **한 트랜잭션**이며, 갱신으로 구현하면 없어진 유형의 행이 남는다
- [X] T052a [US3] **삭제와 삽입 사이에 `flush()` 를 명시한다.** 상세 3종에 유니크 제약이 있고(`ux_stat_group (statistics_idx, expend_group_idx)` · `ux_stat_method` · `ux_stat_weekly`, 덤프 확인), **Hibernate 의 `ActionQueue` 는 한 flush 안에서 INSERT 를 DELETE 보다 먼저 실행한다.** 그래서 "지우고 새로 넣기"를 그대로 쓰면 같은 키의 INSERT 가 먼저 나가 **유니크 위반이 `9000`** 으로 새어 나간다 — 같은 달을 두 번 저장할 때만 터지므로 첫 저장만 거는 시험으로는 드러나지 않는다. 삭제 직후 `flush()` 하거나 파생 삭제 대신 `@Modifying` 벌크 DELETE 를 쓴다. **005 의 4.9 가 같은 자리에서 다른 방식으로 터졌다** — 그쪽은 지운 Entity 를 `save` 대상에 넣어 `ObjectDeletedException` 이 났다. 파생 삭제와 삽입이 만나는 지점은 두 번 다 문제였다
- [X] T053 [US3] `.../backend/controller/StatisticsController.java` 에 5.6 을 더한다 — `POST /api/v1/statistics/monthly/save`. 연·월을 **Body 로만** 받는다

**Checkpoint**: 과거 월이 스냅샷으로 보존되고 이후 원본 변경에 흔들리지 않는다

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 규격·구조·문서의 최종 정합. **006 이 마지막 기능이므로 백엔드 전체를 여기서 닫는다**

- [ ] T054 [P] `.../backend/TargetStatisticsResponseContractIT.java` — quickstart #53 (SC-501). **6건 전부가 `{ resCode, data }` 이고 래퍼 예외가 없다.** 005 와 같이 파일을 돌려주는 API 도, 본문 없는 응답도 없다. 005 의 `LedgerFixedExpenseResponseContractIT` 를 본떠 만들되 "예외를 인정하는 시험"은 두지 않는다. 미인증 6건이 전부 `1001` 이고 그것도 래퍼임을 함께 단언한다
- [ ] T055 [P] `.../backend/statistics/StatisticsStructureIT.java` — quickstart #51·#52·#54 (SC-509). **통계 상세 2종에 지출유형·수단으로 나가는 FK 가 0건**임을 `jdbc` 로 직접 확인한다(API 로는 볼 수 없다). 상세의 이름 컬럼이 **NOT NULL 로 채워져** 있고, 5.5 의 배열 3종이 `data.list` 규칙의 대상이 **아님**을 본다. **구현자가 "FK 가 빠진 실수"로 오해해 추가하면 이 시험이 잡는다**
- [ ] T056 [P] AOP 요청~응답 로깅이 006 의 컨트롤러 둘에 걸리는지 확인한다 — 포인트컷이 `within(@RestController *)` 라 자동으로 걸린다. **제외 대상이 없다**(api-contract §10) — 바이너리 응답이 없다
- [ ] T057 [P] `specs/006-backend-target-statistics/plan.md` § Source Code 의 파일 목록을 실제 구현과 맞춘다 — 특히 **`YearMonthValue` 를 "005 것 재사용"으로만 적어 둔 부분**은 T005 의 연 범위 확장을 반영해야 한다. 응답 DTO 이름도 `*Dto` 가 아니라 `*Response` 다(005 에서 같은 드리프트를 겪었다)
- [ ] T058 `app-mod/money-backend-app/openapi.yaml` 과 `docs/API-문서.md` 에 006 의 6건을 더한다 — 50 → **56개 오퍼레이션**. `OpenApiDocumentIT` 의 양방향 대조가 빠뜨리면 먼저 깨지므로 **각 Phase 안에서 그 Phase 가 연 API 를 함께 적는다** — 목표금액 4건은 Phase 3 에서 이미 들어가 54 가 됐고, 여기서는 통계 2건을 더해 56 을 채운다. `REPRESENTATIVE_PATHS` 에 목표금액·통계를 하나씩 더하고 상태표의 006 행을 ✅ 로 바꾼다
- [ ] T059 `git diff --stat sql/schema-moneylogdb.sql` 이 **비어 있는지** 확인한다(헌장 원칙 VI). 덤프가 바뀌었다면 원인을 찾는다 — 특히 **통계 상세에 FK 를 추가하려는 시도**나 **`usage_rate` 정밀도를 늘리려는 시도**가 있었는지 본다. 둘 다 001 의 결정을 번복하는 변경이며, 후자는 T012 의 상한 처리를 빠뜨렸을 때 나오는 반응이다
- [ ] T060 `./gradlew :data-mod:test` 를 돌려 **80건**이 그대로 통과하는지 확인한다 — 006 은 `data-mod` 의 Entity·제약·시험을 바꾸지 않는다. 기준 수치는 005 까지의 결과다
- [ ] T061 `./gradlew :app-mod:money-backend-app:test` 를 돌려 **002~005 기존과 006 신규가 모두** 통과하는지 확인한다. **`./gradlew test`(전체)는 쓰지 않는다** — `money-app` 의 레거시 시험 3건이 `init` 커밋부터 깨져 있다
- [ ] T062 quickstart.md §4 완료 판정 표의 전 항목과 plan.md § Constitution Check 의 헌장 게이트 6개를 훑는다 — 시나리오 1~54, SC-501~511, `null` vs `0` 비대칭(#5·6·7·8), 수단별 모집단 두 집합(#27·28·29), 저장본 불변 3갈래(#39·43·44), 사용률 상한(#31), FK 0건(#51), 005 와의 경계(#36)
- [ ] T063 **백엔드 Phase 완료를 기록한다** — quickstart.md §5 가 이 지점을 가리킨다. 002~006 의 API 56건이 전부 섰고 `sql/schema-moneylogdb.sql` 은 001 이후 한 번도 바뀌지 않았다. `docs/API-문서.md` 의 상태표에서 006 을 ✅ 로 바꾸고 "006 이 붙으면…" 안내 문장을 마무리 문장으로 고친다

---

## Dependencies & Execution Order

### Phase 의존

```text
Phase 1 (Setup)
   └─> Phase 2 (Foundational)
          └─> Phase 3 (US1, P1)  ── MVP
                 └─> Phase 4 (US2, P1)
                        └─> Phase 5 (US3, P2)
                               └─> Phase 6 (Polish)
```

- **T001·T002 가 모든 코드 작업보다 먼저**다(헌장 원칙 V). 특히 T002 를 빠뜨리면 T011 이 수단별 모집단을 틀리게 만든다
- **Phase 2 가 끝나기 전에는 어떤 스토리도 시작할 수 없다** — T009(계산기)·T007(목표 판정)이 두 스토리에 걸린다
- **US1 → US2 는 진짜 의존**이다. 통계의 유형별 요약이 목표금액을 읽어 사용률·상태를 낸다
- **US2 → US3 도 진짜 의존**이다. 저장은 조회가 쓰는 것과 **같은 계산기**로 값을 만든다
- **US3 는 US2 없이 성립하지 않는다** — 저장할 값을 만들 방법이 없다. 005 처럼 스토리를 재배열할 여지가 없어 spec 순서를 그대로 따른다

### User Story 안의 순서

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- DTO·Mapper → Service → Controller 순. Repository 는 001 이 이미 깔았다

### Parallel Opportunities

- Setup 의 T001~T004 는 서로 다른 파일이라 함께 할 수 있으나, **T001·T002 는 코드 작업 전에 끝낸다**
- Foundational 에서 T006·T007·T008·T011·T012·T013·T014 가 병렬이다. **T005(YearMonthValue)와 T009(계산기)는 단독**이다 — T006 이 T005 를 쓰고, T010~T012 가 T009 를 고친다
- 각 스토리의 테스트 작성은 전부 `[P]` 다 — US1 6건, US2 6건, US3 5건
- DTO 는 대부분 `[P]` 다 — T021~T024 · T036~T038 · T048·T049 가 각자 다른 파일이다
- **T029(5.1~5.4)와 T042·T053(5.5·5.6)이 다른 컨트롤러**라 US1 과 US2·US3 의 컨트롤러 작업은 겹치지 않는다. 다만 T042 와 T053 은 같은 파일이라 순차로 한다
- Polish 의 T054~T057 이 병렬이다. T058~T063 은 순차다

---

## Parallel Example: User Story 2

```bash
# US2 의 통합 테스트 6건을 함께 작성한다:
Task: "StatisticsCalculateIT — quickstart #17·#32·#35·#36 (005 와의 경계)"
Task: "StatisticsWeekBoundaryIT — quickstart #24·#25 (월요일 시작)"
Task: "StatisticsGroupSummaryIT — quickstart #26·#30·#31 (사용률 상한)"
Task: "StatisticsMethodSummaryIT — quickstart #27·#28·#29 (모집단 두 집합)"
Task: "StatisticsViewIT — quickstart #18~#23 (saved / live 분기)"
Task: "StatisticsYearMonthIT — quickstart #33·#34 (2000~2100)"

# 그다음 DTO 3건을 함께 만든다:
Task: "StatisticsViewQuery"
Task: "StatisticsResponse"
Task: "상세 3종 응답 DTO"
```

---

## Implementation Strategy

### MVP 우선

**US1(Phase 1~3, T001~T029)만으로 인도할 수 있다.** "지출유형별 한 달 한도를 정한다"는 가치가 목표금액 CRUD 하나로 성립한다. 통계 없이 완결된다.

### 점진 인도

| 단계 | 누적 범위 | 인도되는 가치 |
|---|---|---|
| Setup + Foundational + US1 | 5.1~5.4 | 지출유형별 목표를 정하고 확인한다 (**MVP**) |
| + US2 | 5.5 | **사용자가 실제로 여는 화면** — 이번 달 통계를 본다 |
| + US3 | 5.6 | 지난달을 확정해 남긴다 |
| + Polish | — | 규격·구조·문서 확인, **백엔드 Phase 완료** |

### 이 기능에서 가장 틀리기 쉬운 것 다섯

구현 중 막히면 여기를 먼저 본다.

| # | 함정 | 지키는 장치 |
|---|---|---|
| 1 | **`null` 과 `0` 을 같게 다룬다** — 0원 목표가 기본값으로 덮여 "그 달엔 쓰지 않겠다"가 사라진다 | T007 의 `TargetResolver` · T023 의 `Long` · 시험 #5·6·7·8 |
| 2 | **수단별 모집단을 "회원 소유 전부"로 읽는다** — 버린 카드의 0원 행이 매달 쌓인다 | T002 의 명세 개정 · T011 · 시험 #27·28·29 |
| 3 | **5.5 와 5.6 이 각자 계산한다** — "지금 보이는 값"과 "저장된 값"이 달라진다 | T009 의 단일 계산기 · T050 |
| 4 | **재저장을 갱신으로 구현한다** — 없어진 유형의 행이 남는다 | T052 의 "지웠다 다시 넣기" · 시험 #42 |
| 5 | **사용률 상한을 잊는다** — `numeric(6,2)` 를 넘겨 DB 오류가 `9000` 으로 새어 나간다 | T012 의 `9999.99` 자르기 · 시험 #31 |
| 6 | **`status` 를 채우지 않는다** — `varchar(10) NOT NULL` + `ck_stat_group_status` 라 저장이 통째로 `9000` 이 된다 | T012a · 시험 #30-1(경계 넷) |
| 7 | **상세를 지운 뒤 바로 넣는다** — Hibernate 가 INSERT 를 DELETE 보다 먼저 내 유니크 위반이 `9000` 이 된다 | T052a 의 `flush()` · 시험 #46-2 |

6·7 은 **두 번째 저장에서만** 터진다 — 첫 저장만 거는 시험은 통과한다. 005 의 4.9 가 정확히 그 구조에서 여덟 단계를 통과한 뒤 검토에서 잡혔다.

1·2·4 는 **조용히 틀린다** — 예외도 오류 응답도 나지 않고 데이터만 어긋난다. 3·5 는 시끄럽게 틀리지만 원인이 엉뚱한 곳에 있다.

### 연·월 오류 코드가 기능마다 다르다

`3403`(005 의 4.5·4.6·4.9) · `3501`(005 의 4.8) · **`3603`(006 전부)** 이 같은 뜻이다. `_공통.md` 의 자원별 코드 블록 배정(고정지출 `34xx` / 가계부 `35xx` / 목표·통계 `36xx`)의 결과이며 **의도된 차이**다. 006 은 여기에 더해 **`view` 값 오류에도 `3603`** 을 쓴다.

### 005 에서 배운 것을 가져온다

- **파생 삭제 뒤에 저장하지 않는다.** T052 의 재저장이 정확히 그 구조다 — 005 의 4.9 가 같은 자리에서 `ObjectDeletedException` 으로 `9000` 을 냈다. 값을 실제로 바꾼 행만 저장 대상에 담는다.
- **처리가 섞이는 조합을 시험한다.** 하나씩만 거는 시험은 통과하고 조합에서 터진다.
- **시험은 저장소의 응답 규격을 따른다.** 이 앱은 실패도 HTTP 200 이므로 HTTP 상태가 아니라 `resCode` 로 단언한다.
