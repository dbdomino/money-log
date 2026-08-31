# Quickstart: 001-backend-db-schema

영속 계층(Entity·제약·덤프)이 스펙대로인지 검증하는 실행 가이드. API 구현·전체 테스트 스위트는 포함하지 않는다.

## Prerequisites

- PostgreSQL 18 로컬: DB `moneylogdb`, user `moneyloguser`, schema `moneylog`
- 초기 스크립트 적용됨: `sql/01_create_user.sql` → `02_create_database.sql` → `03_create_schema.sql`
- JDK 17, Gradle Wrapper
- 상세 모델: [data-model.md](./data-model.md) · 계약: [contracts/](./contracts/)

## Setup

1. `data-mod`에 `tbl_be_*` Entity·Repository 추가 (plan 구조).
2. `money-backend-app`을 `spring.profiles.active=postgresql`로 기동해 `ddl-auto: update` 반영.
3. 부분 유니크 인덱스가 자동 생성되지 않으면 [contracts/naming-and-constraints.md](./contracts/naming-and-constraints.md) SQL을 실행.
4. 스키마 덤프 재생성 (헌장 VI):

```powershell
$env:PGPASSWORD='1q2w3e4r'
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U moneyloguser -d moneylogdb `
  --schema=moneylog --schema-only --no-owner --no-privileges --encoding=UTF8 `
  --restrict-key=moneylogdumpkey -f sql\schema-moneylogdb.sql
```

## Validation scenarios

### V1 — 테이블 인벤토리

**Steps**: 덤프 또는 `\dt moneylog.tbl_be_*`로 15개 테이블 존재 확인.

**Expect**: [contracts/table-inventory.md](./contracts/table-inventory.md) 목록과 일치. 레거시 이름과 충돌 없음.

### V2 — 회원·이메일·세션 제약

**Steps** (psql 또는 통합 테스트):

1. `member_id=alice` 회원 insert 성공.
2. 동일 `member_id` 재 insert → 실패.
3. `email=NULL`인 회원 2명 insert → 성공.
4. 동일 non-null email 2번째 → 실패.
5. 활성 세션 2건 동일 member → 두 번째 실패(또는 첫 revoke 후 성공).

**Expect**: FR-010·012·017.

### V3 — 지출유형 유일·삭제

**Steps**:

1. 같은 member에 name=`식비` 두 번 → 두 번째 실패.
2. 지출 없이 비기본 유형 delete → 성공.
3. 지출이 참조하는 유형 delete → FK/앱 거부.
4. 고정지출(관리·월별 내역)만 참조하는 유형 delete → **허용**(Clarification Q1; 논리 참조).

### V4 — 스냅샷 vs 고정지출

**Steps**:

1. 지출 저장 후 수단 이름 변경 → 지출 행의 `payment_method_name` 불변.
2. 고정지출 설정 후 수단 이름 변경 → 조회 시 **새 이름**(관리·월별 내역 모두 스냅샷 컬럼 없음).

### V4-1 — 고정지출 3분할·월별 내역

**Steps**:

1. 적용 기간 2026-01~2026-12인 고정지출 1건 저장 → `tbl_be_fixed_expense_monthly` **0행**.
2. 2026-03 조회(=materialize) → 내역 1행. `payment_date`가 2026-03 안의 날짜, `modified=false`.
3. 같은 2026-03을 다시 조회 → 여전히 1행(`ON CONFLICT DO NOTHING`).
4. 매달 결제일 31인 고정지출로 2026-02 materialize → `payment_date = 2026-02-28`.
5. 2025-12(기간 밖) materialize → 그 고정지출 행 없음.
6. 2026-03 내역의 금액 수정 → `modified=true`. 관리 테이블 금액 변경 → 2026-03은 **불변**, 아직 안 만든 2026-04는 새 금액으로 생성.
7. 2026-03에 수동 재작성(기본) → `modified=true` 행 보존. `overwriteModified=true` → 관리 값으로 되돌아가고 `modified=false`.
8. 고정지출을 새로 1건 등록한 뒤 이미 연 2026-03에 수동 재작성 → 그 달 내역 1행 추가.
9. 관리 행 삭제 → 그 고정지출의 월별 내역 전부 삭제(CASCADE).

**Expect**: FR-042~FR-049.

### V4-2 — 고정지출·일반지출 분리 조회

**Steps**: 같은 회원·같은 달에 일반 지출 2건(`tbl_be_expense`), 소득 1건(`tbl_be_income`), 고정지출 내역 2건(`tbl_be_fixed_expense_monthly`) 저장.

**Expect**: 세 테이블을 각각 `member_id + 연월`로 조회해 2 / 1 / 2건이 나온다. `tbl_be_expense`에 고정지출 행이 **섞여 있지 않다**(FR-042).

### V5 — 할부·중도상환

**Steps**:

1. 12개월 할부 → 12행, 동일 `installment_group_id`.
2. `payment_date > today` 행만 삭제하는 중도상환 → 오늘·과거 행 잔존.

### V6 — 아이콘 시드 파일

**Steps**: `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/`에 10개 PNG, 각 30×30.

**Expect**: 식비·교통·주거·통신·쇼핑·장보기·의료·교육·문화·기타.

### V7 — 덤프 청결

**Steps**: `schema-moneylogdb.sql`에서 `INSERT INTO moneylog.tbl_be_member` 등 실데이터 검색.

**Expect**: 매칭 없음(또는 마스터 시드만; 회원·거래 없음).

## Out of scope here

- REST API 호출·JWT 발급 E2E (월별 고정지출 내역 **수동 재작성**·**단건 수정** API 구현 포함 — 본 기능은 그 저장 구조만 확정)
- `PaymentMethodCreate` 명세 개정 문서 작업(병행 과제)
- 고정지출 4.x API 명세 개정(병행 과제, research §13·§14)
- Flyway 마이그레이션 도입

## Next

`/speckit-tasks`로 Entity·인덱스·덤프 작업을 태스크로 분해한다.
