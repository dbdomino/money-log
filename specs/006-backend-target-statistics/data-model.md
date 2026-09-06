# Data Model: 지출유형별 목표금액과 월별 통계

**Feature**: `006-backend-target-statistics` | **Date**: 2026-09-02 | **Phase**: 1

**이 기능은 스키마를 바꾸지 않는다.** 테이블·컬럼·CHECK·유니크·CASCADE가 `001`에 전부 있다.
이 문서는 **006이 그 구조를 어떻게 쓰는지**를 적는다.

단일 참조점은 `sql/schema-moneylogdb.sql`이다.

**15개 테이블 중 6개를 이 기능이 처음으로 쓴다.** 001이 만들어 두고 지금까지 비어 있던
것들이다.

---

## 쓰는 테이블

| 대상 | 이 기능에서 | 쓰는 API |
|---|---|---|
| `tbl_expend_target_default` | 읽기·쓰기 | 5.1·5.2·5.3 |
| `tbl_expend_target_monthly` | 읽기·쓰기 | 5.1·5.2·5.4 |
| `tbl_statistics` | 읽기·쓰기 | 5.5·5.6 |
| `tbl_statistics_weekly` | 읽기·쓰기 | 5.5·5.6 |
| `tbl_statistics_expend_group` | 읽기·쓰기 | 5.5·5.6 |
| `tbl_statistics_payment_method` | 읽기·쓰기 | 5.5·5.6 |
| `tbl_expense` · `tbl_income` · `tbl_fixed_expense_monthly` | **읽기만** (집계 원본) | 5.5·5.6 |
| `tbl_user_expend_group` · `tbl_user_payment_method` | **읽기만** (이름·사용 여부) | 5.1~5.6 |

---

## 1. 목표금액 — 두 층

```text
┌──────────────────────────────────┐      ┌──────────────────────────────────┐
│ tbl_expend_target_default        │      │ tbl_expend_target_monthly        │
│  회원 × 지출유형 당 1건            │      │  회원 × 연·월 × 지출유형 당 1건    │
│  "식비는 평소 40만원"              │      │  "2026-12 식비만 60만원"          │
└──────────────────────────────────┘      └──────────────────────────────────┘
              └──────────────┬───────────────────────┘
                             ▼
                  적용 금액 = 월별 값 ?? 기본 값
```

**둘은 독립이다**(FR-505). 기본을 바꿔도 이미 저장된 월별 값은 변하지 않고,
기본을 월별로 복사해 두지도 않는다.

### `tbl_expend_target_default`

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `idx` | BIGINT | PK, IDENTITY |
| `id_key` | BIGINT | NOT NULL, FK |
| `expend_group_idx` | BIGINT | NOT NULL, FK |
| `target_amount` | BIGINT | NOT NULL, CHECK `ck_target_default_amount` (0 ~ 100,000,000) |

**UNIQUE `ux_target_default (id_key, expend_group_idx)`** — 회원·유형당 1건(FR-502).

### `tbl_expend_target_monthly`

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `idx` | BIGINT | PK, IDENTITY |
| `id_key` | BIGINT | NOT NULL, FK |
| `year` · `month` | INT | NOT NULL, CHECK `ck_target_monthly_month` (월 1~12) |
| `expend_group_idx` | BIGINT | NOT NULL, FK |
| `target_amount` | BIGINT | NOT NULL, CHECK `ck_target_monthly_amount` (0 ~ 100,000,000) |

**UNIQUE `ux_target_monthly (id_key, year, month, expend_group_idx)`** — 조합당 1건(FR-503).

### `null` vs `0` — 의도된 비대칭

**이 기능에서 가장 틀리기 쉬운 규칙이다.**

| 필드 | 행이 없으면 | 뜻 |
|---|---|---|
| `monthlyTargetAmount` | **`null`** | "기본값을 쓰겠다" |
| `defaultTargetAmount` | **`0`** | "한도를 정한 적 없다" |

월별에만 두 상태가 필요하다 — `null`(기본 사용)과 `0`(그 달엔 안 쓴다)이 다르다.
기본은 "미설정"과 "0으로 설정"을 구분할 실익이 없다.

**이 비대칭이 적용 금액을 항상 숫자로 만든다.** 월별이 `null`이면 기본으로 떨어지고,
기본은 절대 `null`이 아니므로 결과가 `null`이 되지 않는다.
`tbl_statistics_expend_group.target_amount`가 NOT NULL이라 이게 필요하다.

**대칭으로 만들면 깨진다.** 기본도 `null`로 하면 적용 금액이 `null`이 될 수 있고
통계 저장이 불가능해진다.

### 사용하지 않는 유형 (FR-509·510·512)

| API | `in_use=false`인 유형 |
|---|---|
| 5.1 목록 | **제외**하고 돌려준다 |
| 5.2 단건 조회 | **`3601`로 거절** |
| 5.3·5.4 변경 | **`3601`로 거절** |

`003`의 FR-212가 이 조건을 만들고, `3601`은 006이 정의한다 —
003의 에러코드 표가 "이 기능은 거절 조건만 만들고 코드는 006이 소유한다"고 넘겼다.

**삭제 표시된 유형의 목표금액 행은 유지된다**(FR-511·003의 FR-211).
FK가 있으므로 원본이 물리 삭제되지 않는 한 참조가 끊기지 않는다.

---

## 2. `tbl_statistics` — 월별 통계 스냅샷

| 컬럼 | 타입 | 제약 | 값 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | 상세 3종이 참조 |
| `id_key` | BIGINT | NOT NULL, FK | 소유자 |
| `year` · `month` | INT | NOT NULL, CHECK `ck_statistics_month` (월 1~12) | 대상 연·월 |
| `income_total` | BIGINT | **NOT NULL** | 소득 합계 |
| `expense_total` | BIGINT | **NOT NULL** | 지출 합계 = 일반 + 할부 + 고정 |
| `fixed_amount` | BIGINT | **NOT NULL** | 고정지출 금액 |
| `regular_amount` | BIGINT | **NOT NULL** | 일반(+할부) 금액 |
| `fixed_percent` | NUMERIC(5,2) | **NOT NULL** | 고정 비율 |
| `regular_percent` | NUMERIC(5,2) | **NOT NULL** | 일반 비율 |
| `saved_at` | TIMESTAMPTZ | NOT NULL | 저장 시각. 재저장마다 갱신 |

**UNIQUE `ux_statistics (id_key, year, month)`** — 회원·연월당 1건(FR-516).

**여섯 값이 전부 NOT NULL이다.** 이게 빈 달도 저장할 수 있게 하는 근거다(FR-528) —
0으로 채울 자리가 이미 있다.

**`ck_statistics_month`는 월만 검사한다.** 연도 범위(2000~2100)는 DB가 막지 않으므로
애플리케이션이 판정한다(FR-525).

`fixed_percent`·`regular_percent`가 `NUMERIC(5,2)`라 상한이 `999.99`인데,
비율은 정의상 100을 넘지 않아 문제없다. **`expense_total`이 0이면 두 비율을 0으로 둔다** —
0으로 나눌 수 없고 컬럼이 NOT NULL이다.

---

## 3. 통계 상세 3종

세 테이블 모두 `statistics_idx`로 부모를 참조하고 **`ON DELETE CASCADE`**가 걸려 있다.

### `tbl_statistics_weekly` — 주별 지출

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `statistics_idx` | BIGINT | NOT NULL, FK **CASCADE** |
| `week_index` | INT | NOT NULL, CHECK `ck_stat_weekly_index` (≥ 1) |
| `week_start` · `week_end` | DATE | NOT NULL |
| `amount` | BIGINT | NOT NULL |

**UNIQUE `ux_stat_weekly (statistics_idx, week_index)`**

**`week_start`·`week_end`가 있다는 것이 "경계를 저장한다"의 근거다**(FR-520).
저장본을 읽을 때는 저장된 경계를 그대로 쓰고 다시 계산하지 않는다.

### `tbl_statistics_expend_group` — 지출유형별 요약

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `statistics_idx` | BIGINT | NOT NULL, FK **CASCADE** |
| `expend_group_idx` | BIGINT | NOT NULL, **FK 없음** |
| `expend_group_name` | VARCHAR(30) | **NOT NULL** — 이름 스냅샷 |
| `amount` | BIGINT | NOT NULL |
| `target_amount` | BIGINT | **NOT NULL** — 그 달의 적용 금액 |
| `usage_rate` | NUMERIC(6,2) | NOT NULL — 최대 `9999.99` |
| `status` | VARCHAR(10) | NOT NULL, CHECK `ck_stat_group_status` (`UNDER`/`OK`/`OVER`) |

**UNIQUE `ux_stat_group (statistics_idx, expend_group_idx)`**

### `tbl_statistics_payment_method` — 수단별 요약

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `statistics_idx` | BIGINT | NOT NULL, FK **CASCADE** |
| `payment_method_idx` | BIGINT | NOT NULL, **FK 없음** |
| `payment_method_name` | VARCHAR(50) | **NOT NULL** — 이름 스냅샷 |
| `amount` | BIGINT | NOT NULL |

**UNIQUE `ux_stat_method (statistics_idx, payment_method_idx)`**

---

## 4. FK가 없다 — 이 기능의 구조적 특이점

`tbl_statistics_expend_group`·`tbl_statistics_payment_method`의 **지출유형·수단 참조에
FK가 없다**(FR-519).

덤프에서 확인한 사실:

| 테이블 | 존재하는 FK | 없는 FK |
|---|---|---|
| `tbl_statistics_expend_group` | `fk_stat_group_statistics`(CASCADE) · `fk_stat_group_user` | **`expend_group_idx` → `tbl_user_expend_group`** |
| `tbl_statistics_payment_method` | `fk_stat_method_statistics`(CASCADE) · `fk_stat_method_user` | **`payment_method_idx` → `tbl_user_payment_method`** |

**SC-509가 이걸 테스트로 고정한다** — "통계 상세 2종에 지출유형·수단으로 나가는 FK가 0건".

### 왜 없나

통계는 **그 달에 일어난 일의 기록**이다. 나중에 지출유형을 삭제하거나 수단을 정리해도
과거 통계는 남아야 한다.

| FK를 걸면 | 결과 |
|---|---|
| RESTRICT | 원본 삭제가 막힌다. 통계 때문에 유형을 정리할 수 없다 |
| CASCADE | 과거 통계가 함께 사라진다 |

둘 다 통계의 목적과 어긋난다.

### 대가

**응답의 유형·수단 ID가 실재하지 않을 수 있다.** 화면 복원은 함께 저장한
`expend_group_name`·`payment_method_name`이 맡는다 — **두 컬럼이 NOT NULL인 이유가
이것이다.**

이름 없이 ID만 저장했다면 원본이 사라졌을 때 화면을 복원할 방법이 없다.
`004`의 지출이 이름 스냅샷을 두는 것과 같은 이유다.

---

## 5. 저장본은 불변이다

```text
5.6 저장
    │
    ▼
┌────────────────────────────────────────┐
│ tbl_statistics + 상세 3종               │
│ 저장 시점의 값이 굳는다                   │
└────────────────────────────────────────┘
    │
    ├─ 원본 지출을 고친다        →  저장본은 변하지 않는다 (FR-518·SC-506)
    ├─ 지출유형을 삭제 표시한다   →  유형별 요약이 그대로 남는다 (SC-508)
    ├─ 목표금액을 바꾼다         →  저장본의 목표·사용률·상태는 저장 당시 값
    ├─ status 판정 기준이 바뀐다  →  이미 저장된 행은 다시 계산하지 않는다
    │
    ├─ 5.5 기본 조회            →  저장본을 돌려준다 (source=SAVED)
    ├─ 5.5 view=live            →  저장본을 읽지도 쓰지도 않고 즉석 계산
    │                              (source=CALCULATED, savedAt 은 함께 싣는다)
    └─ 5.6 재저장               →  통계 행 갱신 + saved_at 갱신
                                   상세 3종은 전부 삭제 후 재삽입 (FR-517)
```

**재저장이 상세를 지웠다 다시 넣는 이유**: 행 집합 자체가 달라진다. 새 유형에 지출이
생기면 행이 늘고, 어떤 유형의 지출이 전부 삭제되면 행이 줄어든다. 갱신으로 맞추려면
"있는데 없어진 것"을 찾아 지우는 로직이 따로 필요하다.

**통계 행 자체는 갱신한다** — 지우고 다시 만들면 `idx`가 바뀌어 상세의 `statistics_idx`를
전부 다시 써야 하고, `ux_statistics` 때문에 삭제와 삽입 사이에 창이 생긴다.

---

## 6. 집계 원본 — 무엇을 세는가

| 합계 | 출처 |
|---|---|
| `income_total` | `tbl_income` |
| `regular_amount` | `tbl_expense` (일반 + 할부) |
| `fixed_amount` | `tbl_fixed_expense_monthly` |
| `expense_total` | 위 둘의 합 |

**고정지출은 `tbl_fixed_expense`(설정)가 아니라 `tbl_fixed_expense_monthly`(그 달의 값)에서
읽는다.** 설정은 기준값일 뿐이고 그 달에 실제로 나간 금액은 월별 내역에 있다.

**여기서 `005`와의 경계가 생긴다.** 006의 통계 조회는 **월별 내역의 lazy 생성을 일으키지
않는다.** 생성을 일으키는 것은 `005`의 4.5·4.8·4.9뿐이다(양쪽 Assumptions에 명시).

따라서 **한 번도 열지 않은 달의 통계는 고정지출 합계가 0으로 나온다.**
화면이 그 달을 채우려면 4.8이나 4.9를 먼저 호출한다.

이건 버그가 아니라 결정이다 — 006까지 GET이 다른 테이블에 쓰게 하면 부작용이 하나 더 늘고,
어느 API가 무엇을 만드는지 추적하기 어려워진다.

---

## 7. 이 기능이 만들지 않는 저장 단위

| 개념 | 왜 만들지 않나 |
|---|---|
| 통계 계산 로그·이력 | 저장본 자체가 이력이다. `saved_at`이 시점을 남긴다 |
| 목표금액 변경 이력 | 명세에 요구가 없다. 현재 값만 관리한다 |
| 연간 통계 | 명세의 범위는 월별이다 |
| 상태 판정 기준 테이블 | `UNDER`/`OK`/`OVER`의 경계(90%·110%)가 코드 상수다. 바뀌어도 저장본은 다시 계산하지 않는다 |
| 통계 상세의 FK | §4 — 001이 의도적으로 뺐다 |
