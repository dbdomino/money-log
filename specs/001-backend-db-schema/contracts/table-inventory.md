# Contract: Table Inventory

본 기능이 `moneylog` 스키마에 존재해야 하는 물리 객체 목록. 구현 완료 후 `sql/schema-moneylogdb.sql`에 모두 나타나야 한다.

## Tables (15)

| # | Table | PK | Owner FK |
|---|-------|----|----------|
| 1 | `tbl_be_member` | `member_id` | — |
| 2 | `tbl_be_member_session` | `session_id` | `member_id` |
| 3 | `tbl_be_login_history` | `login_history_id` | `member_id` |
| 4 | `tbl_be_payment_method` | `payment_method_id` | `member_id` |
| 5 | `tbl_be_expend_group` | `expend_group_id` | `member_id` |
| 6 | `tbl_be_expense` | `expense_id` | `member_id` |
| 7 | `tbl_be_income` | `income_id` | `member_id` |
| 8 | `tbl_be_fixed_expense` | `fixed_expense_id` | `member_id` |
| 9 | `tbl_be_fixed_expense_monthly` | `(fixed_expense_id, year, month)` | `member_id` (+ parent CASCADE) |
| 10 | `tbl_be_expend_target_default` | `(member_id, expend_group_id)` | `member_id` |
| 11 | `tbl_be_expend_target_monthly` | `(member_id, year, month, expend_group_id)` | `member_id` |
| 12 | `tbl_be_stat_monthly` | `(member_id, year, month)` | `member_id` |
| 13 | `tbl_be_stat_weekly` | `(member_id, year, month, week_index)` | `member_id` |
| 14 | `tbl_be_stat_group` | `(member_id, year, month, expend_group_id)` | `member_id` |
| 15 | `tbl_be_stat_method` | `(member_id, year, month, payment_method_id)` | `member_id` |

### 고정지출 3분할 (Clarification Session 2026-08-29)

| 역할 | 테이블 |
|------|--------|
| 고정지출 관리(설정·템플릿) | `tbl_be_fixed_expense` (#8) |
| 월별 고정지출 내역 | `tbl_be_fixed_expense_monthly` (#9) |
| 월별 수입·지출 내역 | `tbl_be_income` (#7) · `tbl_be_expense` (#6) |

`tbl_be_fixed_expense_override`는 **더 이상 만들지 않는다**. #9가 그 역할을 대체한다(그 달 행을 직접 수정).

## Sequences

| Sequence | Purpose |
|----------|---------|
| `tbl_be_expense_installment_group_seq` | 할부 그룹 id 발급 |
| (identity/serial) | 각 BIGSERIAL PK |

## Must NOT appear as new collisions

레거시와 동일 이름 생성 금지: `tbl_member`, `tbl_expend`, `tbl_expend_group`, `tbl_payment_method` / `tbl_payment_Method`, `tbl_login_history`, `tbl_expend_fix`, `tbl_card`, `tbl_Ammount`, `tbl_system_stat`.

## Dump exclusions

- `INSERT` of member/session/expense/income operational rows: **금지**
- Seed icon **files** live under classpath resources (not SQL)
- Optional: no SQL seed for default groups (created per signup)

## Acceptance probe (post-dump)

```text
schema-moneylogdb.sql 에 대해:
  - CREATE TABLE moneylog.tbl_be_member ... (및 위 15개) 존재
  - CREATE UNIQUE INDEX ... email ... WHERE (또는 동등 partial unique) 존재 또는 문서화된 보조 DDL
  - 활성 세션 partial unique 존재 또는 보조 DDL
  - INSERT INTO moneylog.tbl_be_member 실데이터 없음
```
