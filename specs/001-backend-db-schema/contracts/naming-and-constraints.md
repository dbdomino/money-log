# Contract: Naming and Constraints

## Naming

| Rule | Value |
|------|-------|
| Schema | `moneylog` |
| Table prefix | `tbl_be_` |
| Column style | snake_case |
| Member natural key | `member_id` VARCHAR(20) |
| Money | `BIGINT` (원, 정수) |
| Timestamps | `TIMESTAMPTZ` |
| Enums-as-string | `VARCHAR` + 앱 검증 (`CARD`/`ACCOUNT`, `EXPENSE`/`INCOME`, `UNDER`/`OK`/`OVER`) |

## Required FK (DB-enforced)

| Child | Parent | On delete |
|-------|--------|-----------|
| `tbl_be_member_session.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_login_history.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_payment_method.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_expend_group.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_expense.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_expense.payment_method_id` | `tbl_be_payment_method` | RESTRICT |
| `tbl_be_expense.expend_group_id` | `tbl_be_expend_group` | RESTRICT |
| `tbl_be_income.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_income.payment_method_id` | `tbl_be_payment_method` | RESTRICT |
| `tbl_be_fixed_expense.member_id` | `tbl_be_member` | RESTRICT |
| `tbl_be_fixed_expense_monthly.fixed_expense_id` | `tbl_be_fixed_expense` | **CASCADE** |
| `tbl_be_fixed_expense_monthly.member_id` | `tbl_be_member` | RESTRICT |
| target/stat `member_id` | `tbl_be_member` | RESTRICT |

## Logical references (no FK)

- `tbl_be_fixed_expense.expend_group_id`
- `tbl_be_fixed_expense_monthly.expend_group_id`
- `tbl_be_fixed_expense_monthly.payment_method_id` (설정과 동일 규칙 — 수단 soft-delete와 양립)
- `tbl_be_expend_target_default.expend_group_id`
- `tbl_be_expend_target_monthly.expend_group_id`
- `tbl_be_stat_group.expend_group_id`
- `tbl_be_stat_method.payment_method_id` (선택: method FK RESTRICT 가능하나 soft-delete·스냅샷과 충돌 시 논리 참조)

근거: Clarification — 지출유형 삭제는 **지출**만 차단.

## Partial unique (PostgreSQL)

```sql
-- email
CREATE UNIQUE INDEX uq_be_member_email
  ON moneylog.tbl_be_member (email)
  WHERE email IS NOT NULL;

-- one active session
CREATE UNIQUE INDEX uq_be_member_active_session
  ON moneylog.tbl_be_member_session (member_id)
  WHERE revoked = false;
```

Hibernate `ddl-auto: update`가 부분 유니크를 못 만들면 `sql/04_be_partial_indexes.sql`로 보완하고 덤프에 포함.

## Lazy materialize — 월별 고정지출 내역

그 달을 처음 조회할 때 `tbl_be_fixed_expense_monthly` 행을 만든다. 동시 요청에서 중복 INSERT가 나지 않도록 PK에 대한 `ON CONFLICT DO NOTHING`을 쓴다.

```sql
INSERT INTO moneylog.tbl_be_fixed_expense_monthly
       (fixed_expense_id, year, month, member_id, payment_method_id,
        amount, payment_date, content, expend_group_id, modified,
        created_at, updated_at)
SELECT f.fixed_expense_id, :year, :month, f.member_id, f.payment_method_id,
       f.amount,
       -- 말일 보정: 결제일 31 + 2월 → 그 달 마지막 날
       make_date(:year, :month, 1)
         + (LEAST(f.payment_day_of_month,
                  EXTRACT(DAY FROM (make_date(:year, :month, 1)
                                    + INTERVAL '1 month - 1 day'))::int) - 1),
       f.content, f.expend_group_id, false, now(), now()
  FROM moneylog.tbl_be_fixed_expense f
 WHERE f.member_id = :memberId
   AND (f.start_year * 12 + f.start_month) <= (:year * 12 + :month)
   AND (:year * 12 + :month) <= (f.end_year * 12 + f.end_month)
ON CONFLICT (fixed_expense_id, year, month) DO NOTHING;
```

연·월 비교는 `year * 12 + month` 합성값을 쓴다(research §5와 동일 규칙).

### 수동 재작성 (FR-049)

같은 `(member_id, year, month)`에 대해 위 INSERT에 더해 UPDATE·DELETE를 한 트랜잭션으로 수행한다.

```sql
-- 1) 관리 테이블 값으로 되맞추기 (modified 행은 :overwriteModified 일 때만)
UPDATE moneylog.tbl_be_fixed_expense_monthly m
   SET amount            = f.amount,
       content           = f.content,
       payment_method_id = f.payment_method_id,
       expend_group_id   = f.expend_group_id,
       payment_date      = make_date(m.year, m.month, 1)
                             + (LEAST(f.payment_day_of_month,
                                      EXTRACT(DAY FROM (make_date(m.year, m.month, 1)
                                                        + INTERVAL '1 month - 1 day'))::int) - 1),
       modified          = false,
       updated_at        = now()
  FROM moneylog.tbl_be_fixed_expense f
 WHERE f.fixed_expense_id = m.fixed_expense_id
   AND m.member_id = :memberId AND m.year = :year AND m.month = :month
   AND (m.modified = false OR :overwriteModified);

-- 2) 적용 기간에서 빠진 달의 내역 제거 (관리 행 삭제분은 FK CASCADE가 처리)
DELETE FROM moneylog.tbl_be_fixed_expense_monthly m
 USING moneylog.tbl_be_fixed_expense f
 WHERE f.fixed_expense_id = m.fixed_expense_id
   AND m.member_id = :memberId AND m.year = :year AND m.month = :month
   AND (   (:year * 12 + :month) < (f.start_year * 12 + f.start_month)
        OR (:year * 12 + :month) > (f.end_year   * 12 + f.end_month));

-- 3) 신규/누락분 채우기 → 위 "Lazy materialize" INSERT 재사용
```

세 단계의 대상 집합은 서로 겹치지 않는다(UPDATE·DELETE는 기존 행, INSERT는 없는 행 / DELETE는 기간 밖, INSERT는 기간 안). 다만 **한 트랜잭션**으로 묶어 중간 상태가 조회되지 않게 한다.

## Soft delete

| Table | Mechanism |
|-------|-----------|
| `tbl_be_payment_method` | `deleted BOOLEAN` — 물리 DELETE 금지 |
| Others (expense, income, group, fixed…) | 물리 DELETE |

## Snapshot columns (required where listed)

| Table | Snapshot columns |
|-------|------------------|
| `tbl_be_expense` | `payment_method_name`, `expend_group_name` |
| `tbl_be_income` | `payment_method_name` |
| `tbl_be_stat_group` | `expend_group_name` |
| `tbl_be_stat_method` | `payment_method_name` |
| `tbl_be_fixed_expense` | **없음** |
| `tbl_be_fixed_expense_monthly` | **없음** — 조회 시 원본 수단·유형 이름 join |

## Parallel spec revision (not schema DDL)

- `PaymentMethodCreate` / `Update`: Body `purpose` (`EXPENSE`|`INCOME`)
- purpose 변경: 참조 expense/income 0건일 때만
