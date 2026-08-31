# Data Model: 001-backend-db-schema

Schema: `moneylog` · Naming: `tbl_be_*` · PK/FK 회원 키: `member_id VARCHAR(20)`

공통 감사(별도 표기 없으면): `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`

---

## Entity Relationship (요약)

```text
tbl_be_member 1──N tbl_be_member_session
tbl_be_member 1──N tbl_be_login_history
tbl_be_member 1──N tbl_be_payment_method
tbl_be_member 1──N tbl_be_expend_group
tbl_be_member 1──N tbl_be_expense
tbl_be_member 1──N tbl_be_income
tbl_be_member 1──N tbl_be_fixed_expense
tbl_be_member 1──N tbl_be_fixed_expense_monthly
tbl_be_member 1──N tbl_be_expend_target_default
tbl_be_member 1──N tbl_be_expend_target_monthly
tbl_be_member 1──N tbl_be_stat_monthly

tbl_be_fixed_expense 1──N tbl_be_fixed_expense_monthly  (CASCADE delete)

tbl_be_stat_monthly 1──N tbl_be_stat_weekly
tbl_be_stat_monthly 1──N tbl_be_stat_group
tbl_be_stat_monthly 1──N tbl_be_stat_method

tbl_be_expense.payment_method_id → tbl_be_payment_method (FK RESTRICT)
tbl_be_expense.expend_group_id   → tbl_be_expend_group   (FK RESTRICT)
tbl_be_income.payment_method_id  → tbl_be_payment_method (FK RESTRICT)
```

고정지출·목표·통계의 `expend_group_id` / 일부 `payment_method_id`는 **논리 참조**(FK 없음) — research §8 · Clarification Q1.

**고정지출 저장 3분할** (Clarification Session 2026-08-29): `tbl_be_fixed_expense`(고정지출 관리) / `tbl_be_fixed_expense_monthly`(월별 고정지출 내역) / `tbl_be_expense`·`tbl_be_income`(월별 수입·지출 내역). 고정지출 행은 지출 내역 테이블에 **섞여 들어가지 않으며**, 월별 조회는 두 축을 분리해서 읽는다.

---

## 1. tbl_be_member

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | **PK**. 로그인 id. 4~20, `[A-Za-z0-9_]` |
| pw | VARCHAR(100) | N | bcrypt 해시만 |
| nickname | VARCHAR(20) | N | 2~20 |
| email | VARCHAR(255) | Y | 부분 유니크(값 있을 때) |
| phone | VARCHAR(20) | Y | 숫자만 |
| intro | VARCHAR(1000) | Y | |
| role | SMALLINT | N | 1 또는 3. 기본 3 |
| active | BOOLEAN | N | 기본 true |

**Indexes**: PK; `UNIQUE (email) WHERE email IS NOT NULL`

**Validation**: role ∈ {1,3}; member_id 패턴; phone digits-only(앱)

---

## 2. tbl_be_member_session

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| session_id | UUID | N | **PK**. JWT `sid` |
| member_id | VARCHAR(20) | N | FK → member RESTRICT |
| access_token_hash | VARCHAR(128) | Y | 폐기 시 NULL |
| refresh_token_hash | VARCHAR(128) | Y | 폐기 시 NULL |
| access_expires_at | TIMESTAMPTZ | N | |
| refresh_expires_at | TIMESTAMPTZ | N | |
| revoked | BOOLEAN | N | 기본 false |
| created_at | TIMESTAMPTZ | N | |
| last_accessed_at | TIMESTAMPTZ | Y | |

**Indexes**: PK; `UNIQUE (member_id) WHERE revoked = false`; `idx (refresh_token_hash)` WHERE NOT NULL

**State**: active(`revoked=false`, hashes set) → revoked(hashes NULL, `revoked=true`). 행 삭제 없음.

---

## 3. tbl_be_login_history

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| login_history_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| login_at | TIMESTAMPTZ | N | |
| login_ip | VARCHAR(45) | N | IPv4/IPv6 |

감사 `updated_at` 없음.

---

## 4. tbl_be_payment_method

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| payment_method_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| name | VARCHAR(100) | N | 회원 내 중복 허용 |
| type | VARCHAR(20) | N | `CARD` \| `ACCOUNT` |
| purpose | VARCHAR(20) | N | `EXPENSE` \| `INCOME` |
| in_use | BOOLEAN | N | |
| card_expiry | CHAR(7) | Y | `YYYY-MM`, ACCOUNT면 NULL |
| deleted | BOOLEAN | N | soft delete. 기본 false |
| created_at / updated_at | TIMESTAMPTZ | N | |

**Indexes**: PK; `idx (member_id, purpose, deleted, in_use)`

**Rules**: purpose 변경은 해당 수단을 참조하는 expense/income 0건일 때만(앱). 물리 DELETE 없음.

---

## 5. tbl_be_expend_group

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| expend_group_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| name | VARCHAR(100) | N | |
| in_use | BOOLEAN | N | |
| default_group | BOOLEAN | N | 시드 true |
| icon_filename | VARCHAR(255) | Y | 예: `user01_식비.png` |
| created_at / updated_at | TIMESTAMPTZ | N | |

**Indexes**: PK; `UNIQUE (member_id, name)`

**Delete**: 물리 삭제. **차단 조건(앱+FK)**: `tbl_be_expense` 참조 존재 또는 `default_group=true`. 고정지출·목표는 DB FK로 막지 않음.

**Seed (가입 시)**: 식비, 교통, 주거, 통신, 쇼핑, 장보기, 의료, 교육, 문화, 기타 — 각 `default_group=true`, `in_use=true`, 아이콘 파일 복사 후 `icon_filename` 설정.

---

## 6. tbl_be_expense (월별 지출 내역)

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| expense_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| payment_method_id | BIGINT | N | FK → payment_method RESTRICT |
| payment_method_name | VARCHAR(100) | N | 스냅샷 |
| amount | BIGINT | N | > 0, 원 |
| payment_date | DATE | N | |
| place | VARCHAR(200) | N | |
| content | VARCHAR(500) | N | |
| expend_group_id | BIGINT | N | FK → expend_group RESTRICT |
| expend_group_name | VARCHAR(100) | N | 스냅샷 |
| installment_group_id | BIGINT | Y | NULL=일시불 |
| installment_index | INT | Y | 1-based |
| installment_total | INT | Y | |
| created_at / updated_at | TIMESTAMPTZ | N | |

**Role**: 월별 **일반 지출** 내역. 고정지출 행은 여기에 저장하지 않는다(§9로 분리).

**Indexes**: PK; `idx (member_id, payment_date)`; `idx (installment_group_id)`; `idx (member_id, expend_group_id)`

**Rules**: 할부 N건 동일 `installment_group_id`(시퀀스 발급). 중도상환: `payment_date > CURRENT_DATE` 행 DELETE. 업무 키 유니크 없음(중복 허용).

**Sequence**: `tbl_be_expense_installment_group_seq`

---

## 7. tbl_be_income (월별 수입 내역)

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| income_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| payment_method_id | BIGINT | N | FK → payment_method RESTRICT |
| payment_method_name | VARCHAR(100) | N | 스냅샷 |
| amount | BIGINT | N | > 0 |
| payment_date | DATE | N | |
| content | VARCHAR(500) | Y | |
| created_at / updated_at | TIMESTAMPTZ | N | |

**Indexes**: PK; `idx (member_id, payment_date)`

---

## 8. tbl_be_fixed_expense (고정지출 관리)

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| fixed_expense_id | BIGSERIAL | N | **PK** |
| member_id | VARCHAR(20) | N | FK → member |
| name | VARCHAR(100) | N | 중복 허용 |
| payment_method_id | BIGINT | N | 논리 참조(또는 FK RESTRICT to method; soft-delete와 양립) |
| amount | BIGINT | N | 기본 금액 |
| payment_day_of_month | SMALLINT | N | 1~31 |
| content | VARCHAR(500) | N | |
| expend_group_id | BIGINT | N | **논리 참조** (FK 없음) |
| start_year | INT | N | |
| start_month | SMALLINT | N | 1~12 |
| end_year | INT | N | |
| end_month | SMALLINT | N | 1~12; (end > start) 기간 |
| created_at / updated_at | TIMESTAMPTZ | N | |

**이름 스냅샷 컬럼 없음** (조회 시 원본 join).

**Role**: 고정지출의 **설정(템플릿)만** 보관한다. 실제 달마다의 값은 §9 `tbl_be_fixed_expense_monthly` 행이 갖는다.

**CHECK**: `payment_day_of_month BETWEEN 1 AND 31`; month 1~12; 종료가 시작보다 뒤(연·월 합성 비교).

---

## 9. tbl_be_fixed_expense_monthly (월별 고정지출 내역)

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| fixed_expense_id | BIGINT | N | PK·FK → fixed_expense **ON DELETE CASCADE** |
| year | INT | N | PK |
| month | SMALLINT | N | PK, 1~12 |
| member_id | VARCHAR(20) | N | FK → member RESTRICT. 월별 조회용 비정규화 |
| payment_method_id | BIGINT | N | 논리 참조. 생성 시 설정에서 복사 |
| amount | BIGINT | N | > 0. 생성 시 설정 기본 금액 복사 |
| payment_date | DATE | N | `payment_day_of_month`를 **해당 월 말일로 보정**해 확정 |
| content | VARCHAR(500) | N | 생성 시 설정에서 복사 |
| expend_group_id | BIGINT | N | 논리 참조. 생성 시 설정에서 복사 |
| modified | BOOLEAN | N | 기본 false. 사용자가 그 달 값을 직접 고치면 true |
| created_at / updated_at | TIMESTAMPTZ | N | |

**PK**: `(fixed_expense_id, year, month)`

**Indexes**: PK; `idx (member_id, year, month)` — 월별 고정지출 내역 조회; `idx (member_id, payment_date)`

**이름 스냅샷 컬럼 없음** — 수단·유형 이름은 조회 시 원본 join(§8과 동일 규칙, Clarification Q4).

**생성(lazy materialize)**: 그 달을 **처음 조회할 때** 생성한다. 회원의 고정지출 중 적용 기간이 `(year, month)`를 포함하는데 행이 없는 건을 설정값으로 INSERT한다. 동시 요청 대비 `INSERT ... ON CONFLICT (fixed_expense_id, year, month) DO NOTHING`. 적용 기간 밖 연월은 생성하지 않는다.

**수정**: 그 달의 `amount` / `payment_date` / `content` / `payment_method_id`를 직접 UPDATE하고 `modified = true`로 표시한다. 별도 예외 테이블 없음.

**설정 변경 시 자동 반영**: `modified = false`이면서 **아직 오지 않은 달**(현재 연월 초과) 행만 설정값으로 갱신(또는 삭제 후 재생성)한다. `modified = true` 행과 과거·현재 달 행은 보존한다.

**수동 재작성 (FR-049)**: 지정한 `(member_id, year, month)` 하나를 관리 테이블 기준으로 다시 맞춘다. 과거 달에도 쓸 수 있다.

| 대상 | 처리 |
|------|------|
| 적용 기간에 걸리는데 행 없음(신규 등록 포함) | INSERT |
| 행 있음 · `modified = false` | 관리 테이블 값으로 UPDATE |
| 행 있음 · `modified = true` | 기본 보존. `overwriteModified=true`면 UPDATE + `modified=false` |
| 관리 행 삭제됨 | CASCADE로 이미 사라짐 |
| 적용 기간이 그 연·월을 더는 포함하지 않음 | DELETE |

**단건 수정 (FR-046)**: `(fixed_expense_id, year, month)` 한 행의 `amount` / `payment_date` / `content` / `payment_method_id`를 UPDATE하고 `modified = true`로 표시한다.

**Delete**: 설정 삭제 시 CASCADE로 그 설정의 월별 내역 전부 삭제(과거 달 포함).

**CHECK**: `month BETWEEN 1 AND 12`; `amount > 0`

---

## 10. tbl_be_expend_target_default

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| expend_group_id | BIGINT | N | PK, 논리 참조 |
| default_target_amount | BIGINT | N | 0 ~ 100_000_000 |
| created_at / updated_at | TIMESTAMPTZ | N | |

**PK**: `(member_id, expend_group_id)` · FK member RESTRICT

---

## 11. tbl_be_expend_target_monthly

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| year | INT | N | PK |
| month | SMALLINT | N | PK |
| expend_group_id | BIGINT | N | PK, 논리 참조 |
| monthly_target_amount | BIGINT | N | 0 ~ 100_000_000 (0과 “행 없음” 구분) |
| created_at / updated_at | TIMESTAMPTZ | N | |

**PK**: `(member_id, year, month, expend_group_id)`

---

## 12. tbl_be_stat_monthly

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| year | INT | N | PK |
| month | SMALLINT | N | PK |
| saved_at | TIMESTAMPTZ | N | |
| income_total | BIGINT | N | |
| expense_total | BIGINT | N | |
| fixed_amount | BIGINT | N | |
| regular_amount | BIGINT | N | |
| fixed_percent | NUMERIC(5,2) | N | |
| regular_percent | NUMERIC(5,2) | N | |
| created_at / updated_at | TIMESTAMPTZ | N | |

**PK**: `(member_id, year, month)` — 재저장 시 UPDATE

---

## 13. tbl_be_stat_weekly

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| year | INT | N | PK |
| month | SMALLINT | N | PK |
| week_index | INT | N | PK, 1-based |
| week_start | DATE | N | |
| week_end | DATE | N | |
| amount | BIGINT | N | |

FK 논리: 부모 스냅샷과 동일 키. 부모 재저장 시 자식 삭제 후 재삽입(앱 트랜잭션).

---

## 14. tbl_be_stat_group

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| year | INT | N | PK |
| month | SMALLINT | N | PK |
| expend_group_id | BIGINT | N | PK (논리, 0원 유형 제외) |
| expend_group_name | VARCHAR(100) | N | 스냅샷 |
| amount | BIGINT | N | |
| target | BIGINT | N | 적용 목표 |
| usage_rate | NUMERIC(8,2) | N | |
| status | VARCHAR(10) | N | `UNDER` \| `OK` \| `OVER` |

---

## 15. tbl_be_stat_method

| Column | Type | Null | Notes |
|--------|------|:----:|-------|
| member_id | VARCHAR(20) | N | PK |
| year | INT | N | PK |
| month | SMALLINT | N | PK |
| payment_method_id | BIGINT | N | PK (논리, 0원 포함) |
| payment_method_name | VARCHAR(100) | N | 스냅샷 |
| amount | BIGINT | N | |

---

## Validation Rules (교차)

| Rule | Enforcement |
|------|-------------|
| 회원당 활성 세션 1 | Partial unique index |
| 이메일 유니크(NULL 제외) | Partial unique index |
| 지출유형 이름 회원 내 유일 | UNIQUE(member_id, name) |
| 금액 정수·원 | BIGINT, 앱에서 >0 또는 목표 0~1억 |
| 할부 원자성 | 앱 트랜잭션 |
| purpose 변경 가드 | 앱(참조 count=0) |
| 유형 삭제 가드 | FK(expense)+앱(default_group) |
| 중도상환 대상 | 앱: payment_date > today |
| 고정지출 내역 연월당 1건 | PK(fixed_expense_id, year, month) + `ON CONFLICT DO NOTHING` |
| 고정지출 내역 생성 범위 | 앱: 설정 적용 기간(시작~종료 연월) 안의 연월만 |
| 설정 변경 자동 반영 대상 | 앱: `modified = false` AND 현재 연월 초과 행 |
| 수동 재작성 대상 | 앱: 지정 `(member_id, year, month)` 1개월 · 과거 달 허용 · `modified=true`는 기본 보존 |
| 고정지출·일반지출 분리 | 물리 테이블 분리(§9 vs §6) — 조회에서 합치지 않음 |

## State Transitions

- **Member.active**: true → false (정지). 데이터·세션 revoke는 앱.
- **Session**: issued → revoked (해시 NULL).
- **PaymentMethod.deleted**: false → true (복구 정책 없음/명세 외).
- **FixedExpenseMonthly**: 행 없음 → 그 달 최초 조회 시 생성(`modified=false`) → 사용자가 그 달 값을 고치면 `modified=true`(이후 설정 변경 재동기화 대상에서 제외).
- **Stat monthly**: insert or upsert by (member, year, month); details replace.
