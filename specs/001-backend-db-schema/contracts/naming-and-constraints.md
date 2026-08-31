# Contract: 명명 규칙과 제약

**재작성**: 2026-08-31

이 기능이 만드는 모든 DB 객체가 지켜야 하는 규칙이다. 위반은 리뷰에서 되돌린다.

## 1. 명명 규칙

| 대상 | 규칙 | 예 |
|------|------|-----|
| 테이블 | `tbl_user` + 회원 소유 저장 단위는 `tbl_user_<자원명>` | `tbl_user_expend_group` |
| 자원명 | 명세의 API 자원 이름을 snake_case로 | `/expend-groups` → `expend_group` |
| 기본키 | `idx`. **`tbl_user`만 `id_key`** | `idx`, `id_key` |
| 소유자 | `id_key` | `id_key` |
| 다른 테이블 참조 | `<대상 자원명>_idx` | `expend_group_idx`, `statistics_idx` |
| 이름 스냅샷 | `<대상 자원명>_name` | `payment_method_name` |
| 불리언 | 형용사·과거분사 단독 | `active`, `deleted`, `in_use`, `revoked`, `modified` |
| 시각 | `_at` 접미사 | `created_at`, `saved_at`, `login_at` |
| 날짜 | `_date` 접미사 또는 의미 그대로 | `payment_date`, `week_start` |
| 금액 | `amount` 또는 `<수식어>_amount` | `amount`, `target_amount`, `fixed_amount` |
| 시퀀스 | `seq_<용도>` | `seq_installment_group` |
| 인덱스 | `ix_<테이블>_<컬럼들>` | `ix_user_expense_id_key_payment_date` |
| 유니크 인덱스 | `ux_<테이블>_<컬럼들>` | `ux_user_session_active` |
| CHECK | `ck_<테이블>_<컬럼>` | `ck_user_role` |

**금지**

- 레거시 테이블 이름 재사용 — [table-inventory.md](./table-inventory.md) 비겹침 표 참조
- 자식 테이블에 `user_id`(로그인 아이디) 복사 — 소유자는 `id_key`만
- 컬럼 이름에 테이블 이름 반복 (`tbl_user_expense.expense_amount` ✗ → `amount` ✓)
- 따옴표가 필요한 식별자(대문자·공백) — PostgreSQL 소문자 접힘 사고의 원인

## 2. 모든 테이블 공통 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|:----:|------|
| `idx` / `id_key` | BIGINT IDENTITY | ✗ | 기본키 |
| `id_key` | BIGINT | ✗ | 소유 회원 (`tbl_user` 제외) |
| `created_at` | TIMESTAMPTZ | ✗ | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | ✗ | 최종 수정 시각 |
| `created_by` | BIGINT | ✗¹ | 만든 회원의 `id_key` |
| `updated_by` | BIGINT | ✗¹ | 마지막으로 고친 회원의 `id_key` |

¹ `tbl_user`에서만 두 컬럼이 NULL 허용 — 회원가입은 자기 자신을 만드는 행위라 INSERT 시점에 자기 `id_key`가 없다. NULL은 본인 가입/수정, 값이 있으면 그 관리자가 손댄 것.

`created_by`/`updated_by`에는 FK를 걸지 않는다. 감사 기록은 대상 회원의 존재와 무관하게 남아야 하고, `tbl_user`가 자기 자신을 참조하는 순환을 피한다.

## 3. FK 정책

| 관계 | 동작 |
|------|------|
| 모든 자식 → `tbl_user(id_key)` | `RESTRICT` |
| 지출·소득·고정지출(관리·월별) → 수단 | `RESTRICT` |
| 지출·고정지출(관리·월별) → 지출유형 | `RESTRICT` |
| 목표금액(기본·월별) → 지출유형 | `RESTRICT` |
| 월별 고정지출 내역 → 고정지출 관리 | **`CASCADE`** |
| 통계 상세 3종 → 통계 스냅샷 | **`CASCADE`** |
| 통계 상세 → 지출유형·수단 | **FK 없음** |
| `created_by` / `updated_by` | FK 없음 |

`RESTRICT`가 안전한 이유: 회원·수단·지출유형은 전부 물리 삭제 경로가 없다(정지 플래그 또는 삭제 표시). 삭제 차단 규칙 중 DB가 막을 수 없는 것 — 지출유형의 `3106`·`3107` — 은 애플리케이션이 판정한다.

## 4. UNIQUE 제약

| 이름 | 테이블 | 대상 | 근거 |
|------|--------|------|------|
| `ux_user_user_id` | `tbl_user` | `(user_id)` | FR-010 |
| `ux_user_email` | `tbl_user` | `(email) WHERE email IS NOT NULL` | FR-012 |
| `ux_user_session_id` | `tbl_user_session` | `(session_id)` | FR-015 |
| `ux_user_session_active` | `tbl_user_session` | `(id_key) WHERE revoked = false` | FR-017 |
| `ux_user_expend_group_name` | `tbl_user_expend_group` | `(id_key, name)` | FR-035 |
| `ux_user_fixed_expense_monthly` | `tbl_user_fixed_expense_monthly` | `(fixed_expense_idx, year, month)` | FR-053 |
| `ux_user_target_default` | `tbl_user_expend_target_default` | `(id_key, expend_group_idx)` | FR-070 |
| `ux_user_target_monthly` | `tbl_user_expend_target_monthly` | `(id_key, year, month, expend_group_idx)` | FR-071 |
| `ux_user_statistics` | `tbl_user_statistics` | `(id_key, year, month)` | FR-074 |
| `ux_user_stat_weekly` | `tbl_user_statistics_weekly` | `(statistics_idx, week_index)` | FR-077 |
| `ux_user_stat_group` | `tbl_user_statistics_expend_group` | `(statistics_idx, expend_group_idx)` | FR-077 |
| `ux_user_stat_method` | `tbl_user_statistics_payment_method` | `(statistics_idx, payment_method_idx)` | FR-077 |

**부분 유니크 2건**(`ux_user_email`, `ux_user_session_active`)은 Hibernate가 만들지 못한다 → §7 보조 DDL.

지출유형 이름 유일성은 삭제 표시된 행을 **포함**한다. 부분 유니크로 삭제분을 빼면 아이콘 파일명 `{user_id}_{유형이름}.png`가 충돌한다.

## 5. CHECK 제약

| 이름 | 대상 | 조건 |
|------|------|------|
| `ck_user_role` | `tbl_user.role` | `IN (1, 3)` |
| `ck_payment_method_type` | `tbl_user_payment_method.type` | `IN ('CARD','ACCOUNT')` |
| `ck_payment_method_purpose` | `tbl_user_payment_method.purpose` | `IN ('EXPENSE','INCOME')` |
| `ck_expense_amount` | `tbl_user_expense.amount` | `> 0` |
| `ck_expense_installment_index` | `tbl_user_expense.installment_index` | `IS NULL OR >= 1` |
| `ck_expense_installment_total` | `tbl_user_expense.installment_total` | `IS NULL OR >= 2` |
| `ck_income_amount` | `tbl_user_income.amount` | `> 0` |
| `ck_fixed_expense_amount` | `tbl_user_fixed_expense.amount` | `> 0` |
| `ck_fixed_expense_day` | `tbl_user_fixed_expense.payment_day_of_month` | `BETWEEN 1 AND 31` |
| `ck_fixed_expense_start_month` | `tbl_user_fixed_expense.start_month` | `BETWEEN 1 AND 12` |
| `ck_fixed_expense_end_month` | `tbl_user_fixed_expense.end_month` | `BETWEEN 1 AND 12` |
| `ck_fixed_expense_period` | `tbl_user_fixed_expense` | `end_year * 12 + end_month >= start_year * 12 + start_month` |
| `ck_fixed_monthly_amount` | `tbl_user_fixed_expense_monthly.amount` | `> 0` |
| `ck_fixed_monthly_month` | `tbl_user_fixed_expense_monthly.month` | `BETWEEN 1 AND 12` |
| `ck_target_default_amount` | `tbl_user_expend_target_default.target_amount` | `BETWEEN 0 AND 100000000` |
| `ck_target_monthly_amount` | `tbl_user_expend_target_monthly.target_amount` | `BETWEEN 0 AND 100000000` |
| `ck_target_monthly_month` | `tbl_user_expend_target_monthly.month` | `BETWEEN 1 AND 12` |
| `ck_statistics_month` | `tbl_user_statistics.month` | `BETWEEN 1 AND 12` |
| `ck_stat_weekly_index` | `tbl_user_statistics_weekly.week_index` | `>= 1` |
| `ck_stat_group_status` | `tbl_user_statistics_expend_group.status` | `IN ('UNDER','OK','OVER')` |

**금액 상한**: 목표금액에만 명세상 상한(1억)이 있다. 지출·소득·고정지출 금액에는 상한을 두지 않는다(FR·spec Assumptions).

## 6. 인덱스

| 이름 | 테이블 | 컬럼 | 쓰이는 API |
|------|--------|------|-----------|
| `ix_user_expense_date` | `tbl_user_expense` | `(id_key, payment_date)` | 4.8, 5.5 |
| `ix_user_expense_installment` | `tbl_user_expense` | `(installment_group_id, payment_date)` | 3.6 |
| `ix_user_income_date` | `tbl_user_income` | `(id_key, payment_date)` | 4.8, 5.5 |
| `ix_user_fixed_expense_period` | `tbl_user_fixed_expense` | `(id_key, start_year, start_month, end_year, end_month)` | 4.5, 4.9 |
| `ix_user_fixed_monthly_ym` | `tbl_user_fixed_expense_monthly` | `(id_key, year, month)` | 4.5, 4.8, 4.9 |
| `ix_user_payment_method_active` | `tbl_user_payment_method` | `(id_key, purpose, in_use, deleted)` | 2.6 |
| `ix_user_expend_group_active` | `tbl_user_expend_group` | `(id_key, in_use, deleted)` | 2.13 |
| `ix_user_login_history_at` | `tbl_user_login_history` | `(id_key, login_at)` | 이력 조회 |

`keyword`(장소·내용 부분 일치)용 인덱스는 두지 않는다 — 월 범위로 먼저 좁히므로 대상 행이 적다.

## 7. 스키마 반영 경계

| 만드는 주체 | 대상 |
|-------------|------|
| **Hibernate `ddl-auto: update`** | 테이블, 컬럼, 타입, NOT NULL, PK, FK, 일반 인덱스, 조건 없는 UNIQUE |
| **보조 DDL `sql/04_constraints.sql`** | 부분 유니크 2건, CHECK 20건, 시퀀스 `seq_installment_group` |

보조 DDL은 **멱등**해야 한다 — `CREATE UNIQUE INDEX IF NOT EXISTS`, `CREATE SEQUENCE IF NOT EXISTS`, CHECK는 존재 확인 후 `ALTER TABLE ... ADD CONSTRAINT`. `ddl-auto: update` 환경에서 반복 실행되기 때문이다.

반영 순서: 앱 기동(Hibernate) → `04_constraints.sql` 실행 → `pg_dump` 재생성.

## 8. 덤프 규칙 (헌장 VI)

- 스키마가 바뀌면 `sql/schema-moneylogdb.sql`을 재생성해 **같은 커밋에 포함**한다
- 재생성 명령과 옵션은 헌장 원칙 VI를 그대로 따른다(`--no-owner --no-privileges --restrict-key=moneylogdumpkey`)
- 이 파일을 손으로 편집하지 않는다
- **시드 데이터 없음** — 기본 지출유형 10종은 회원마다 생기는 데이터라 `--data-only` 덧붙이기를 하지 않는다
- 회원·거래 실데이터는 한 건도 포함하지 않는다
