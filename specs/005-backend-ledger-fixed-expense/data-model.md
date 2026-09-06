# Data Model: 고정지출 관리와 월별 가계부 목록

**Feature**: `005-backend-ledger-fixed-expense` | **Date**: 2026-09-02 | **Phase**: 1

**이 기능은 스키마를 바꾸지 않는다.** 테이블·컬럼·CHECK·유니크·CASCADE·인덱스가 `001`에
전부 있다. 이 문서는 **005가 그 구조를 어떻게 쓰는지**를 적는다.

단일 참조점은 `sql/schema-moneylogdb.sql`이다.

---

## 쓰는 테이블

| 대상 | 이 기능에서 | 쓰는 API |
|---|---|---|
| `tbl_fixed_expense` | 읽기·쓰기 | 4.1~4.4·4.7 |
| `tbl_fixed_expense_monthly` | 읽기·쓰기 | 4.5·4.6·4.8·4.9 |
| `tbl_expense` | **읽기만** | 4.8 |
| `tbl_income` | **읽기만** | 4.8 |
| `tbl_user_payment_method` | **읽기만** (검증 + 현재 이름) | 4.1·4.4·4.6·4.5·4.8 |
| `tbl_user_expend_group` | **읽기만** (검증 + 현재 이름) | 4.1·4.4·4.5·4.8 |

---

## 0. 이 기능의 핵심 개념 — 두 층

고정지출은 **설정**과 **그 달의 값**이 서로 다른 저장 단위다.

```text
┌───────────────────────────────────────┐
│ tbl_fixed_expense  (설정)              │  "월세 50만원, 매달 25일,
│  ─ 기준값과 적용 기간                    │   2026-11 ~ 2027-02"
│  ─ 등록 시 이 행 1건만 만든다 (FR-402)   │
└──────────────┬────────────────────────┘
               │  그 달을 처음 조회할 때 복사 (lazy, FR-406)
               │  ON DELETE CASCADE
               ▼
┌───────────────────────────────────────┐
│ tbl_fixed_expense_monthly  (그 달의 값) │  "2026-12는 55만원으로 냈다"
│  ─ 연·월마다 1건                        │
│  ─ modified 로 직접 수정 여부 표시        │
└───────────────────────────────────────┘
```

**왜 나눴나**: 한 달치 금액만 고치는 일이 흔하다. 한 테이블이었다면 "3월만 5만원"을
표현할 자리가 없다.

---

## 1. `tbl_fixed_expense` — 고정지출 관리(설정)

Entity: `data-mod/.../entity/UserFixedExpense.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 005에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | API의 `fixedExpenseId` |
| `id_key` | BIGINT | NOT NULL, FK | 소유자. 토큰이 정한다 |
| `name` | VARCHAR(50) | NOT NULL | 고정지출 이름 (예: 월세) |
| `payment_method_idx` | BIGINT | NOT NULL, FK | 수단 **참조**. 이름은 저장하지 않는다 |
| `expend_group_idx` | BIGINT | NOT NULL, FK | 지출유형 **참조**. 이름은 저장하지 않는다 |
| `amount` | BIGINT | NOT NULL, CHECK `ck_fixed_expense_amount` (> 0) | 기본 금액 |
| `payment_day_of_month` | INT | NOT NULL, CHECK `ck_fixed_expense_day` (1~31) | 매달 결제일. **보정 전 값** |
| `content` | VARCHAR(255) | NOT NULL | 내용 |
| `start_year`·`start_month` | INT | NOT NULL, CHECK (월 1~12) | 적용 시작 |
| `end_year`·`end_month` | INT | NOT NULL, CHECK (월 1~12) | 적용 종료 |

**CHECK `ck_fixed_expense_period`**:
`(end_year * 12 + end_month) >= (start_year * 12 + start_month)`

**이 CHECK이 `연 × 12 + 월` 합성 비교의 근거다.** 001이 이미 이 식을 스키마에 넣었으므로
애플리케이션도 같은 식을 써야 한다. 연과 월을 따로 비교하면 해를 넘기는 구간에서 어긋난다
(2026-12 vs 2027-01).

**INDEX**: `ix_fixed_expense_period (id_key, start_year, start_month, end_year, end_month)` —
"이 연·월에 걸리는 고정지출"을 찾는 조회에 맞춘 것이다.

### 이름 컬럼이 없다

`payment_method_name`·`expend_group_name`이 **없다**(덤프 확인).
테이블 주석도 "이름 스냅샷을 두지 않는다"다.

응답의 `paymentMethodName`·`expendGroupName`은 **조회 시점에 원본에서 읽는다**(FR-405).
수단 이름을 바꾸면 기존 설정의 응답도 새 이름으로 바뀐다 —
**지금 유효한 설정이지 과거 기록이 아니기 때문이다.**

`004`의 지출·소득과 정반대다. 대비는 §5에 있다.

### 애플리케이션이 지켜야 하는 것

| 규칙 | 코드 | 왜 DB가 못 막나 |
|---|---|---|
| 참조가 본인 소유인가 | `3003`·`3103` | FK는 존재만 본다 |
| 수단이 `purpose=EXPENSE`인가 | `3401` | FK가 용도를 모른다 |
| 본인 소유 설정만 접근 | `3402` | FK는 남의 행 읽기를 막지 않는다 |
| 한 달짜리(시작=종료) 허용 | — | CHECK이 `>=`라 이미 허용된다(FR-404) |

---

## 2. `tbl_fixed_expense_monthly` — 월별 고정지출 내역

Entity: `data-mod/.../entity/UserFixedExpenseMonthly.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 005에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | 내부 대리키. **API에 노출하지 않는다** |
| `id_key` | BIGINT | NOT NULL, FK | 소유자 |
| `fixed_expense_idx` | BIGINT | NOT NULL, FK **ON DELETE CASCADE** | 부모 설정 |
| `year`·`month` | INT | NOT NULL, CHECK (월 1~12) | 그 달 |
| `amount` | BIGINT | NOT NULL, CHECK (> 0) | 그 달 금액 |
| `payment_date` | DATE | NOT NULL | **말일 보정이 끝난** 실제 날짜 |
| `content` | VARCHAR(255) | NOT NULL | 그 달 내용 |
| `payment_method_idx` | BIGINT | NOT NULL, FK | 그 달 수단 |
| `expend_group_idx` | BIGINT | NOT NULL, FK | 그 달 지출유형 |
| `modified` | BOOLEAN | NOT NULL, 기본 `false` | **직접 수정 여부** |

**UNIQUE `ux_fixed_expense_monthly (fixed_expense_idx, year, month)`** —
한 고정지출의 한 연·월에 내역이 최대 1건임을 DB가 강제한다(FR-407).
**lazy 생성의 동시 경합에서 최종 방어선**이다.

**INDEX**: `ix_fixed_monthly_ym (id_key, year, month)` — 그 달의 내역 전체를 읽는 조회용.

**FK `fk_fixed_monthly_fixed_expense ... ON DELETE CASCADE`** —
설정을 지우면 월별 내역이 따라 삭제된다(FR-416). 다른 FK 3개(회원·수단·지출유형)에는
CASCADE가 없다.

### `payment_date`는 보정된 값이다

설정의 `payment_day_of_month`가 31이어도, 2월 내역의 `payment_date`는 그 달 말일이다.
**만들 때 한 번 보정해 저장하고 조회 때 다시 계산하지 않는다**(FR-409).

조회 시 계산하면 4.5와 4.8이 각자 계산하다 한쪽만 윤년 처리를 빠뜨리면 같은 달의
결제일이 두 화면에서 다르게 보인다. 저장해 두면 출처가 하나다.

### `modified` — 자동 반영에서 제외하는 표시

| 값 | 뜻 | 설정 수정(4.4)의 자동 반영 |
|---|---|---|
| `false` | 설정에서 복사된 그대로 | **미래 달이면 갱신된다** |
| `true` | 사용자가 4.6으로 직접 고쳤다 | **갱신하지 않는다** |

`true`를 `false`로 되돌리는 유일한 경로는 **4.9를 `overwriteModified=true`로 부르는 것**이다
(SC-406). 4.6은 항상 `true`로 세운다.

---

## 3. 월별 내역의 생명주기

```text
              ┌── 4.5 그 달 처음 조회 ──┐
              │   4.8 그 달 처음 조회    │  lazy 생성 (FR-406·418)
              │   4.9 재작성 ①          │  적용 기간 안일 때만 (FR-408)
              ▼                        │
     ┌──────────────────┐              │
     │  modified=false  │◀─────────────┘
     │  설정에서 복사됨   │
     └────────┬─────────┘
              │
              │ ① 4.6 단건 수정          → modified=true
              │ ② 4.4 설정 수정          → 미래 달이면 값 갱신 (modified 는 그대로 false)
              │ ③ 4.9 재작성 ②           → 관리 값으로 갱신
              ▼
     ┌──────────────────┐
     │  modified=true   │  4.4 의 자동 반영에서 제외된다 (FR-412)
     │  직접 수정됨       │  4.9 재작성 ③ 에서 보존 (keptCount)
     └────────┬─────────┘
              │ 4.9 재작성 + overwriteModified=true
              ▼  값을 관리 값으로 되돌리고 modified=false 로 내린다
     ┌──────────────────┐
     │  modified=false  │
     └──────────────────┘

  삭제 경로 둘
     ─ 4.7 설정 삭제        → CASCADE 로 그 고정지출의 월별 내역 전부 (지난 달 포함)
     ─ 4.9 재작성 ④         → 기간이 그 연·월을 더는 포함하지 않는 행만
```

**②가 `modified`를 건드리지 않는 것**에 주의한다. 설정 수정이 값을 덮어써도 그 행은 여전히
"직접 수정된 적 없는" 행이다.

---

## 4. 설정 수정의 자동 반영 범위 (FR-412)

```text
서버 기준 현재 연월을 now 라 할 때 (연 × 12 + 월 합성값)

    지난 달  (ym <  now)   →  갱신하지 않는다
    이번 달  (ym == now)   →  갱신하지 않는다      ← 이미 본 숫자를 바꾸지 않는다
    미래 달  (ym >  now)
        ├ modified=false   →  갱신한다
        └ modified=true    →  갱신하지 않는다
```

**이번 달을 포함하지 않는 것이 판단이 필요했던 지점이다.** 포함하면 월세를 올렸을 때
이번 달 가계부 금액이 소급해 바뀐다. `004`의 중도상환 경계(`payment_date > today`)와
같은 성격의 결정이다.

지난 달을 새 설정값으로 맞추고 싶으면 **4.9를 명시적으로 부른다.**
자동과 수동의 경계가 여기다.

---

## 5. `004`와 정반대인 것 — 이름

구현자가 가장 혼동하기 쉬운 지점이다.

| | `004` 지출·소득 | `005` 고정지출 |
|---|---|---|
| 이름 컬럼 | `payment_method_name`·`expend_group_name` **있다** | **없다** |
| 이름의 성격 | 등록 당시 **스냅샷** | 조회 시점 **현재 이름** |
| 원본 이름을 바꾸면 | 과거 행은 **그대로** | 응답이 **새 이름으로 바뀐다** |
| 무엇인가 | **과거 기록** | **지금 유효한 설정** |

**4.8이 두 규칙을 한 응답에 섞는다**(FR-419·425). 같은 목록 안에서
`FIXED` 행은 현재 이름, `EXPENSE`·`INCOME` 행은 스냅샷이다. **행 종류로 구분한다.**

**개정 필요**: `4.1`·`4.3`이 두 이름을 "스냅샷"으로 적고 있다(FR-405).
그대로 두면 구현자가 001 스키마에 이름 컬럼을 추가하려 든다.

---

## 6. 가계부 목록 행 — 저장 단위가 아니다

4.8이 돌려주는 목록은 **테이블이 아니다.** 조회 시점에 4개 출처를 합쳐 만든다.

| `type` | 출처 | 조건 |
|---|---|---|
| `EXPENSE` | `tbl_expense` | 할부 3컬럼이 전부 NULL |
| `INSTALLMENT` | `tbl_expense` | 할부 3컬럼이 전부 채워짐 |
| `INCOME` | `tbl_income` | — |
| `FIXED` | `tbl_fixed_expense_monthly` | 그 연·월 |

`EXPENSE`와 `INSTALLMENT`가 **같은 테이블**에서 나온다. 할부 3컬럼의 채움 여부로 가른다 —
`004`가 그 불변식(셋 다 비거나 셋 다 채움)을 지켜야 이 판정이 성립한다.

### `ledgerItemId` (FR-424)

```text
EXPENSE · INSTALLMENT →  expense:{expenseId}
INCOME                →  income:{incomeId}
FIXED                 →  fixed:{fixedExpenseId}:{year}:{month}
```

네 출처의 PK가 서로 다른 테이블에서 나오므로 숫자만으로는
`expense.idx = 5`와 `income.idx = 5`가 구분되지 않는다.

**`FIXED`만 세 조각인 이유**: `sourceId`(= `fixedExpenseId`)만으로는 1행이 특정되지 않는다.
같은 고정지출이 여러 달에 걸쳐 행을 갖기 때문이다. 유니크 제약
`(fixed_expense_idx, year, month)`와 같은 조합이 필요하다.

`tbl_fixed_expense_monthly.idx`를 쓰지 않는 것은, 프론트가 그 값으로 4.6(수정)을 부를 수
없기 때문이다 — 4.6의 Path가 `/monthly/{year}/{month}/{fixedExpenseId}`다.

---

## 7. 이 기능이 만들지 않는 저장 단위

| 개념 | 왜 만들지 않나 |
|---|---|
| 가계부 테이블 | 4.8은 조회 시점 조립이다. 저장하면 원본과 어긋날 여지가 생긴다 |
| 월별 예외 테이블 | 폐기됐다. `modified` 플래그가 그 역할을 한다(`3404`가 그때의 결번이다) |
| 재작성 이력 | FR-414는 건수만 돌려준다. 이력 저장 요구가 없다 |
| 고정지출 이름 스냅샷 | §5 — 001이 스키마로 확정했다 |
| 삭제 표시 컬럼 | 물리 삭제다(FR-416). `deleted` 컬럼이 없다 |
