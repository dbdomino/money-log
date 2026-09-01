-- ============================================================================
-- 04_constraints.sql — Hibernate가 만들지 못하는 제약·시퀀스
--
-- 대상 : moneylogdb / 스키마 moneylog
-- 실행 : moneyloguser 로 moneylogdb 에 접속해 실행한다.
--        psql -h localhost -U moneyloguser -d moneylogdb -f sql/04_constraints.sql
--
-- 실행 시점 : money-backend-app 을 한 번 기동해 ddl-auto:update 가 테이블을
--             만든 뒤에 실행한다. 테이블이 없으면 ALTER 가 실패한다.
--
-- ----------------------------------------------------------------------------
-- 규칙 1 — 이 스크립트는 반복 실행된다 (MUST)
--
-- ddl-auto:update 환경이라 앱을 띄울 때마다 다시 돌리게 된다. 모든 문장은
-- IF NOT EXISTS 를 쓰거나, 아래 duplicate_object 패턴으로 감싸야 한다.
-- 두 번 연속 실행해도 오류가 없어야 한다 (tasks.md T058 이 검증한다).
--
-- 규칙 2 — 여기에 무엇을 넣는가
--
--   O  부분 유니크 인덱스 (WHERE 절이 붙은 것)  ← Hibernate 가 못 만든다
--   O  CHECK 제약
--   O  시퀀스 (할부 그룹)
--   X  테이블 / 컬럼 / FK / 일반 인덱스 / 조건 없는 UNIQUE
--      → 전부 Entity 에 선언하고 Hibernate 가 만들게 둔다
--
-- 규칙 3 — 채우는 순서
--
-- 5개 User Story 가 자기 절만 덧붙인다. 절 제목을 지우거나 순서를 바꾸지 않는다.
--   US1 회원·세션        (tasks.md T019)
--   US2 수단·지출유형    (tasks.md T027)
--   US3 지출·소득·할부   (tasks.md T035)
--   US4 고정지출         (tasks.md T043)
--   US5 목표금액·통계    (tasks.md T054)
--
-- 정의 근거 : specs/001-backend-db-schema/contracts/naming-and-constraints.md §4·§5
-- ============================================================================

SET client_encoding = 'UTF8';
SET search_path TO moneylog;

-- ----------------------------------------------------------------------------
-- 멱등 패턴 (복사해서 쓴다)
-- ----------------------------------------------------------------------------
--
-- 부분 유니크 인덱스 — IF NOT EXISTS 로 충분하다:
--
--   CREATE UNIQUE INDEX IF NOT EXISTS ux_example
--       ON tbl_example (col) WHERE flag = false;
--
-- CHECK 제약 — ALTER TABLE ADD CONSTRAINT 에는 IF NOT EXISTS 가 없다.
-- 중복 추가를 예외로 삼켜야 한다:
--
--   DO $$
--   BEGIN
--       ALTER TABLE tbl_example
--           ADD CONSTRAINT ck_example CHECK (col > 0);
--   EXCEPTION
--       WHEN duplicate_object THEN NULL;
--   END $$;
--
-- 시퀀스 — IF NOT EXISTS 로 충분하다:
--
--   CREATE SEQUENCE IF NOT EXISTS seq_example;
--
-- ============================================================================


-- ============================================================================
-- US1 — 회원 · 세션 (tasks.md T019)
-- ============================================================================
-- 이메일은 "값이 있을 때만" 유일하다. 이메일은 선택 항목이라 비어 있는 회원이
-- 여럿일 수 있어, 조건 없는 UNIQUE 로는 표현할 수 없다. (FR-012)
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_email
    ON tbl_user (email) WHERE email IS NOT NULL;

-- 회원당 폐기되지 않은 세션은 동시에 1건뿐이다. 폐기된 세션은 행으로 남으므로
-- (FR-016) 조건 없는 UNIQUE 를 걸면 재로그인 자체가 막힌다. (FR-017)
-- 애플리케이션 검사만으로는 동시 로그인 레이스를 막을 수 없어 DB 가 강제한다.
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_session_active
    ON tbl_user_session (id_key) WHERE revoked = false;

-- 권한은 관리자(1)와 일반(3) 둘뿐이다. (FR-013)
DO $$
BEGIN
    ALTER TABLE tbl_user
        ADD CONSTRAINT ck_user_role CHECK (role IN (1, 3));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;


-- ============================================================================
-- US2 — 지출·소득 수단 · 지출유형 (tasks.md T027)
-- ============================================================================
-- 종류는 카드·계좌 둘뿐이다. (FR-030)
DO $$
BEGIN
    ALTER TABLE tbl_user_payment_method
        ADD CONSTRAINT ck_payment_method_type CHECK (type IN ('CARD', 'ACCOUNT'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 용도는 지출·소득 둘뿐이며 한 수단은 한쪽만 갖는다. (FR-030·FR-033)
-- "사용 중인 수단 목록"(2.6)이 이 값으로 지출용·소득용을 가르므로, 값이 어긋나면
-- 입력 화면에 엉뚱한 수단이 뜬다.
DO $$
BEGIN
    ALTER TABLE tbl_user_payment_method
        ADD CONSTRAINT ck_payment_method_purpose CHECK (purpose IN ('EXPENSE', 'INCOME'));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;


-- ============================================================================
-- US3 — 지출 · 소득 · 할부 (tasks.md T035)
-- ============================================================================
-- 할부 그룹 식별자는 한 행이 아니라 그 할부의 N개 행이 함께 쓰는 값이다. 행마다
-- 발급되면 안 되므로 Entity 의 @GeneratedValue 가 아니라 시퀀스에서 미리 받아
-- 12개 행에 같이 넣는다. (FR-044)
CREATE SEQUENCE IF NOT EXISTS seq_installment_group;

-- 금액은 원 단위 양의 정수다. 0 이나 음수는 환불·정정이지 지출이 아니다. (FR-040)
DO $$
BEGIN
    ALTER TABLE tbl_user_expense
        ADD CONSTRAINT ck_expense_amount CHECK (amount > 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 할부 3개 컬럼은 모두 비면 일시불, 모두 차면 할부다. 아래 둘은 "값이 있다면
-- 어떤 값이어야 하는가"만 본다 — 셋의 동시 유무는 컬럼 단위 CHECK 으로 표현할 수
-- 없어 애플리케이션이 맡는다. (FR-043)
DO $$
BEGIN
    ALTER TABLE tbl_user_expense
        ADD CONSTRAINT ck_expense_installment_index
        CHECK (installment_index IS NULL OR installment_index >= 1);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 1개월 할부는 일시불과 같다. 할부로 적히면 중도상환·통계가 헛돈다.
DO $$
BEGIN
    ALTER TABLE tbl_user_expense
        ADD CONSTRAINT ck_expense_installment_total
        CHECK (installment_total IS NULL OR installment_total >= 2);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 수입 금액도 양의 정수다. (FR-046)
DO $$
BEGIN
    ALTER TABLE tbl_user_income
        ADD CONSTRAINT ck_income_amount CHECK (amount > 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;


-- ============================================================================
-- US4 — 고정지출 (tasks.md T043)
-- ============================================================================
-- 기본 금액은 원 단위 양의 정수다. (FR-050)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense
        ADD CONSTRAINT ck_fixed_expense_amount CHECK (amount > 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 결제일은 1~31 이다. 31 을 넣어도 2월에는 그런 날이 없지만, 말일 보정은 월별 내역을
-- 만들 때 하고(FR-055) 여기 값은 보정 전 설정값이라 31 까지 허용한다. (FR-050)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense
        ADD CONSTRAINT ck_fixed_expense_day
        CHECK (payment_day_of_month BETWEEN 1 AND 31);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 적용 시작·종료 월은 1~12 다. (FR-051)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense
        ADD CONSTRAINT ck_fixed_expense_start_month
        CHECK (start_month BETWEEN 1 AND 12);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense
        ADD CONSTRAINT ck_fixed_expense_end_month
        CHECK (end_month BETWEEN 1 AND 12);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 종료 연월이 시작 연월보다 앞설 수 없다. 연과 월을 따로 비교하면 해를 넘기는
-- 구간에서 조건이 어긋나므로 year * 12 + month 합성값 하나로 본다 — 예를 들어
-- 2027-02 시작 / 2026-11 종료는 월만 보면 11 > 2 라 통과해 버린다. 같은 회원의
-- 기간 조회 JPQL 도 같은 식을 쓴다. 시작과 종료가 같은 한 달짜리는 허용된다. (FR-051)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense
        ADD CONSTRAINT ck_fixed_expense_period
        CHECK (end_year * 12 + end_month >= start_year * 12 + start_month);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 그 달 금액도 양의 정수다. (FR-053)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense_monthly
        ADD CONSTRAINT ck_fixed_monthly_amount CHECK (amount > 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 월은 1~12 다. 유일 제약이 (fixed_expense_idx, year, month) 라 월 값이 범위를
-- 벗어나면 같은 달을 두 칸에 적을 수 있게 된다. (FR-053, research §6)
DO $$
BEGIN
    ALTER TABLE tbl_user_fixed_expense_monthly
        ADD CONSTRAINT ck_fixed_monthly_month CHECK (month BETWEEN 1 AND 12);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;


-- ============================================================================
-- US5 — 목표금액 · 통계 (tasks.md T054)
-- ============================================================================
-- ck_target_default_amount : 0 ~ 100000000                      — FR-070
-- ck_target_monthly_amount : 0 ~ 100000000                      — FR-071
-- ck_target_monthly_month  : month 1~12                         — FR-071
-- ck_statistics_month      : month 1~12                         — FR-074
-- ck_stat_weekly_index     : week_index >= 1                    — FR-076
-- ck_stat_group_status     : status IN ('UNDER','OK','OVER')    — FR-076
-- (T054 에서 채운다)
