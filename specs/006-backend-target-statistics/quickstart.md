# Quickstart: 지출유형별 목표금액과 월별 통계

**Feature**: `006-backend-target-statistics` | **Date**: 2026-09-02 | **Phase**: 1

이 기능이 실제로 동작하는지 확인하는 **검증 가이드**다. 구현 코드는 담지 않는다.

---

## 0. 착수 전

### 선행 기능 — 전부 필요하다

**`002`~`005`가 모두 서야 한다.** 백엔드 Phase의 마지막 기능이라 앞의 것을 다 쓴다.

| 선행 | 006이 쓰는 것 |
|---|---|
| `002` | 인증 · 응답 규격 · `GlobalExceptionHandler` · AOP 로깅 · `AuditorAware` |
| `003` | `tbl_user_expend_group`(목표금액 대상, `in_use` 판정) · `tbl_user_payment_method` |
| `004` | **`tbl_expense`·`tbl_income`** — 집계 원본 |
| `005` | **`tbl_fixed_expense_monthly`** — 고정지출 집계 원본. `YearMonthValue`도 재사용 |

004·005 없이 통계를 검증하면 합계가 전부 0이라 의미가 없다.

### 명세 선행 개정 (원칙 V)

| # | 대상 | 고칠 내용 |
|---|---|---|
| 1 | `5.6-StatisticsMonthlySave.md` 실패 표 | `3604` 설명의 **"(정책에 따라)"** 삭제 → "현재 연월 초과" |
| 2 | `5.5-StatisticsMonthlyGet.md` | 수단별 요약의 **모집단** 명시 (FR-521a) |

확인:

```bash
grep -n '정책에 따라' 프로젝트설계/기능명세상세-백엔드/phase5-목표-통계/
# → 0건이어야 한다
```

**2번을 빠뜨리면 위험하다.** 그대로 두면 구현자가 "회원 소유 수단 전부"로 읽어
**버린 카드의 0원 행이 매달 쌓인다.** 확정된 규칙은 "그 달 지출이 있는 수단은 상태 무관 전부
+ 0원 행은 저장 시점 사용 중인 `EXPENSE` 수단만"이다.

---

## 1. 전제

| 항목 | 값 |
|---|---|
| PostgreSQL 18 | `localhost:5432` · DB `moneylogdb` · 스키마 `moneylog` |
| 스키마 | 15개 테이블. **이 기능은 스키마를 바꾸지 않는다** |
| 백엔드 | `:8081` · `/api/v1` |
| 데이터 | `002` 가입 → `003` 수단·유형 → `004` 지출·소득 → `005` 고정지출 |

제약 확인 — **FK 0건이 이 기능의 핵심 전제다**:

```bash
psql -h localhost -U moneyloguser -d moneylogdb -c \
  "select conname, conrelid::regclass::text
     from pg_constraint
    where contype='f'
      and conrelid::regclass::text in ('moneylog.tbl_statistics_expend_group',
                                       'moneylog.tbl_statistics_payment_method');"
# → 각 테이블당 2건씩만 (statistics_idx · id_key).
#   expend_group_idx · payment_method_idx 로 나가는 FK 가 있으면 안 된다 (SC-509)

psql -h localhost -U moneyloguser -d moneylogdb -c \
  "select column_name, is_nullable from information_schema.columns
    where table_schema='moneylog' and table_name='tbl_statistics'
      and column_name in ('income_total','expense_total','fixed_amount',
                          'regular_amount','fixed_percent','regular_percent');"
# → 여섯 값 전부 NO (빈 달 저장의 근거, FR-528)

psql -h localhost -U moneyloguser -d moneylogdb -c \
  "select column_name, data_type, numeric_precision, numeric_scale
     from information_schema.columns
    where table_schema='moneylog' and table_name='tbl_statistics_expend_group'
      and column_name='usage_rate';"
# → numeric(6,2) — 상한 9999.99 (FR-522)
```

**주의 — `ddl-auto`가 `create`다.** 기동할 때마다 전 테이블이 drop 후 재생성된다.

---

## 2. 실행

```bash
./gradlew :app-mod:money-backend-app:bootRun     # 백엔드 (:8081)

./gradlew :data-mod:test                          # 스키마 IT 77건
./gradlew :app-mod:money-backend-app:test         # 002~006
```

---

## 3. 검증 시나리오

### US1 — 목표금액 (P1, MVP)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 1 | 목표가 없던 유형에 기본 목표 저장 | 새로 만들어진다 (upsert) | US1-1 |
| 2 | 기본 목표가 있을 때 다시 저장 | 같은 행이 갱신되고 **행이 늘지 않는다** | US1-2 |
| 3 | 기본·월별이 다 있을 때 **기본만** 변경 | 월별 값은 **그대로** | US1-3·FR-505 |
| 4 | 월별을 저장한 뒤 기본 조회 | **바뀌지 않았다** | US1-4 |
| 5 | 그 달 월별을 저장한 적 없을 때 조회 | `monthlyTargetAmount`가 **`null`**, `defaultTargetAmount`는 별도 필드 | US1-5 |
| 6 | 월별을 **0원**으로 저장한 뒤 조회 | `monthlyTargetAmount`가 **`0`** (`null`이 아니다) | US1-6·FR-506 |
| 7 | 기본 목표를 저장한 적 없을 때 조회 | `defaultTargetAmount`가 **`0`** (`null`이 아니다) | FR-507 |
| 8 | 5번·7번 응답의 필드 존재 | `monthlyTargetAmount` 필드가 **생략되지 않고 `null`로 온다** | FR-507 |
| 9 | 1억 초과 목표 저장 | `3602` | US1-7·SC-504 |
| 10 | 0원 목표 저장 | **성공** (유효한 값) | FR-504 |
| 11 | `in_use=false` 유형의 단건 조회·변경 | `3601` | US1-8 |
| 12 | `in_use=false` 유형이 5.1 목록에 | **제외된다** | US1-8·FR-509 |
| 13 | 삭제 표시된 유형의 목표금액 행 조회 | 행과 참조가 **유지된다** | US1-9·FR-511 |
| 14 | **남의** 유형 ID로 접근 (그게 `in_use=false`여도) | **`3103`** (`3601`이 아니다) | api-contract §7 |
| 15 | 5.1의 `offset`이 `limit`의 배수가 아님 | `9001` | FR-526 |
| 16 | 5.1의 `totalCount` | **사용 중 유형만** 센다 | FR-526 |

**5·6·7·8이 한 묶음이다.** `null` vs `0` 비대칭을 전부 덮는다.
8번을 빠뜨리면 Jackson이 `null` 필드를 빼도 통과한다.

**14번이 판정 순서를 검증한다.** 존재 여부를 노출하지 않으려면 소유자 검사가 먼저다.

### US2 — 통계 조회 (P1)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 17 | 저장본 없는 달 조회 | 즉석 계산, `source=CALCULATED` | US2-1 |
| 18 | 저장본 있는 달을 **기본** 조회 | **저장본**, `source=SAVED` | US2-2 |
| 19 | 저장본 있는 달을 **`view=saved`**로 조회 | 18번과 **같은 동작** | FR-514 |
| 20 | 저장본 있는 달을 **`view=live`**로 조회 | 저장본 무시, 즉석 계산, `source=CALCULATED` | US2-3 |
| 21 | `view=live`인데 저장본이 있음 | `savedAt`이 함께 실린다 | US2-4·FR-515 |
| 22 | `view=live` 조회 **후** 저장본 확인 | **변경되지 않았다** | US2-5·SC-507 |
| 23 | `view`에 `saved`·`live` 밖의 값 | `3603` | FR-514 |
| 24 | 그 달 1일이 월요일이 아닌 달의 주별 | 첫 주가 **1일부터 첫 일요일까지** | US2-6 |
| 25 | 그 달 마지막 주 | **말일에서 끊긴다** | FR-520 |
| 26 | 지출 0원인 지출유형 | 유형별 요약에 **없다** | US2-7·FR-521 |
| 27 | 지출 0원인 **사용 중** 수단 | 수단별 요약에 **있다** | US2-8·FR-521a |
| 28 | 지출 0원인 **삭제 표시된** 수단 | 수단별 요약에 **없다** | FR-521a ② |
| 29 | 그 달 지출이 **있는** 삭제 표시된 수단 | 수단별 요약에 **있다** | FR-521a ① |
| 30 | 목표가 0원인 유형의 사용률 | **`0`** | US2-9·FR-522 |
| 31 | 목표 1,000원에 지출 1,000만원 | 사용률이 **`9999.99`로 잘린다** (저장 실패 아님) | FR-522 |
| 32 | 지출 합계가 0인 달의 비율 | `fixedPercent`·`regularPercent` 둘 다 **`0`** | FR-513 |
| 33 | 월에 13 | `3603` | US2-10 |
| 34 | 연에 1999 · 2101 | `3603` | SC-510·FR-525 |
| 35 | 응답의 합계·비율 6값 | 전부 실린다 (`incomeTotal`·`expenseTotal`·`fixedAmount`·`regularAmount`·두 비율) | FR-513 |
| 36 | 한 번도 열지 않은 달의 통계 | 고정지출 합계가 **0**이다 (lazy 생성을 일으키지 않는다) | statistics-snapshot §2 |

**27·28·29가 수단별 모집단 규칙(FR-521a)을 전부 덮는다.** 셋을 함께 봐야 두 집합의
합집합이 확인된다.

**31번을 빠뜨리면 런타임에 DB 오류가 난다.** `numeric(6,2)` 상한을 넘는 값을 저장하려 하면
`9000`으로 새어 나간다.

**36번이 `005`와의 경계다.** 통계 조회가 월별 내역을 만들지 않는다는 결정을 검증한다.

### US3 — 통계 저장 (P2)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 37 | 통계 저장 | 응답에 **`savedAt`·`source=SAVED`**가 실려 재조회 없이 화면 갱신 가능 | US3-1 |
| 38 | 저장 후 기본 조회 | `source=SAVED` | US3-1 |
| 39 | 저장 후 **그 달 지출을 고치고** 기본 조회 | **저장본이 변하지 않았다** | US3-2·SC-506 |
| 40 | 같은 달을 **다시** 저장 | 통계 행이 **1건**이고 `savedAt`이 갱신됨 | US3-3·SC-505 |
| 41 | 재저장 후 상세 확인 | 이전 상세가 **지워지고** 새로 계산된 것으로 채워짐 | US3-4·FR-517 |
| 42 | 재저장 전후로 유형별 행 수가 달라지는 경우 | 늘거나 줄어든 대로 반영 | FR-517 |
| 43 | 저장 후 그 지출유형을 **삭제 표시**하고 통계 조회 | 유형별 요약이 **그대로 남고** 저장 당시 이름으로 읽힌다 | US3-5·SC-508 |
| 44 | 저장 후 **목표금액을 바꾸고** 통계 조회 | 저장본의 목표·사용률·상태는 **저장 당시 값** | Edge Case |
| 45 | **미래 월** 저장 시도 | `3604` | FR-527 |
| 46 | **이번 달** 저장 | **성공** (초과가 아니다) | FR-527 |
| 47 | 지출·소득이 **한 건도 없는 달** 저장 | **성공.** 합계·비율 6값이 전부 `0` | SC-511·FR-528 |
| 48 | 47번 저장 후 조회 | `source=SAVED`이고 값이 전부 0 | SC-511 |
| 49 | 47번의 유형별 상세 | **빈 배열** | FR-528 |
| 50 | 저장본과 `view=live`를 나란히 비교 | 원본을 고쳤다면 두 값이 다르다 | US3-6 |

**39·43·44가 저장본 불변(FR-518)의 세 갈래다.** 원본 지출·유형·목표금액 어느 것이
바뀌어도 저장본은 그대로다.

**41·42가 "상세는 지웠다 다시 넣는다"를 검증한다.** 갱신으로 구현하면 42번에서
없어진 유형의 행이 남는다.

### 구조 불변

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 51 | 통계 상세 2종의 FK | 지출유형·수단으로 나가는 FK가 **0건** | SC-509 |
| 52 | 통계 상세의 이름 | `expendGroupName`·`payment_method_name`이 **NOT NULL로 채워져 있다** | FR-519 |
| 53 | 6건 전부 호출 | **전부** `{ resCode, data }`. 래퍼 예외가 없다 | SC-501 |
| 54 | 5.5의 배열 3종 | `data.list` 규칙의 적용 대상이 **아니다** (통계 객체의 구성 요소) | FR-526 |

51번은 API로 확인할 수 없으므로 §1의 `psql` 질의로 본다.
**구현자가 "FK가 빠진 실수"로 오해해 추가하면 이 시나리오가 잡는다.**

---

## 4. 완료 판정

| 항목 | 확인 수단 |
|---|---|
| 명세 선행 개정 | §0의 `grep` |
| API 6건 동작 | 시나리오 1~54 |
| SC-501~511 (11건) | 위 표의 "대응" 열 |
| `null` vs `0` 비대칭 | 시나리오 5·6·7·8 |
| 수단별 모집단 두 집합 | 시나리오 27·28·29 |
| 저장본 불변 3갈래 | 시나리오 39·43·44 |
| 사용률 상한 | 시나리오 31 |
| FK 0건 | 시나리오 51 (`psql` 직접 확인) |
| `005`와의 경계 | 시나리오 36 |
| 스키마 무변경 | `git diff sql/schema-moneylogdb.sql` → 변경 없음 |
| `:data-mod:test` | 77건 통과 |
| `:app-mod:money-backend-app:test` | 002~005 기존 + 006 신규 통과 |
| 헌장 게이트 6개 | [plan.md § Constitution Check](./plan.md#constitution-check) |

**스키마 무변경 확인이 원칙 VI의 이 기능판이다.** 덤프가 바뀌었다면 원인을 찾는다 —
특히 통계 상세에 FK를 추가하려는 시도나, `usage_rate` 정밀도를 늘리려는 시도가 있었는지
본다. 둘 다 001의 결정을 번복하는 변경이다.

---

## 5. 백엔드 Phase 완료

006이 백엔드의 마지막 기능이다. 여기까지 끝나면 **API 56건**이 전부 선다.

| 기능 | API | 누적 |
|---|---:|---:|
| 002 회원·인증 | 16 | 16 |
| 003 수단·지출유형 | 13 | 29 |
| 004 지출·소득 | 12 | 41 |
| 005 고정지출·가계부 | 9 | 50 |
| 006 목표·통계 | 6 | **56** |

`001`이 만든 **15개 테이블이 전부 쓰인다** — 006이 마지막 6개(목표금액 2종·통계 4종)를
처음으로 쓴다.
