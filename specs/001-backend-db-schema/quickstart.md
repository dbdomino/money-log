# Quickstart: 저장 구조 검증

**재작성**: 2026-08-31

이 기능이 "끝났다"를 확인하는 절차다. API는 아직 없으므로 **스키마 반영 + SQL 검증 + Entity 매핑 테스트**로 확인한다. 구현 순서는 `tasks.md`(`/speckit-tasks`)가 정한다.

## 사전 조건

| 항목 | 확인 |
|------|------|
| PostgreSQL 18 기동 | `moneylogdb` / 사용자 `moneyloguser`. **스키마 `moneylog`는 없어도 된다** — 앱이 만든다 |
| 초기 스크립트 | `sql/01_create_user.sql` → `02_create_database.sql` 실행 완료. `03_create_schema.sql`은 psql로 직접 붙을 때만 필요하다(`search_path` 설정) |
| JDK 17 | `java -version` |
| 아이콘 템플릿 | `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/` 에 10개 PNG (커밋되어 있음) |

## 1. 스키마 반영

**앱을 한 번 기동하면 끝난다.** 뒤이어 실행할 스크립트가 없다(2026-09-02 개정).

```bash
./gradlew :app-mod:money-backend-app:bootRun
# 기동 로그에 create schema / create table / comment on table 확인 후 종료
```

Hibernate가 만드는 것 — 스키마, 테이블 15개, 컬럼·PK·FK·일반 인덱스·조건 없는 UNIQUE, **CHECK 20건**, **부분 유니크 2건**, **시퀀스 `seq_installment_group`**, **테이블 주석 15건**.

`ddl-auto`가 `create`라 **기동할 때마다 전 테이블이 drop 후 재생성**된다. 개발 단계 설정이며, 검증 시나리오는 매번 빈 스키마에서 시작한다.

기동 로그의 `GenerationTarget encountered exception`을 확인한다 — **이것도 검증 항목이다.** DDL 실패는 WARN으로만 찍히고 앱은 정상 기동하므로, "Started ..." 만 보고 넘어가면 테이블이 하나도 없는 상태를 놓친다.

허용되는 것은 **스키마가 없는 첫 기동의 `drop schema moneylog` 1건뿐**이다. `ddl-auto: create`가 생성 전에 drop을 시도하는데 지울 것이 없어 나는 것이라 무해하고, 두 번째 기동부터는 사라진다. **`create table`·`create index`·`comment on` 단계의 실패는 0건이어야 한다.**

## 2. 스키마 검증 (SQL)

### 2-1. 테이블 15개

```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'moneylog'
ORDER BY tablename;
```

기대: 15행. 목록은 [contracts/table-inventory.md](./contracts/table-inventory.md)와 일치.

### 2-2. 레거시 이름 비겹침

```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'moneylog'
  AND tablename IN ('tbl_member','tbl_login_history','tbl_payment_method',
                    'tbl_card','tbl_expend','tbl_expend_group','tbl_expend_fix',
                    'tbl_ammount','tbl_system_stat');
```

기대: **0행** (이 기능은 레거시 이름을 만들지 않는다).

### 2-3. 기본키 규칙

```sql
SELECT c.relname AS table_name, a.attname AS pk_column
FROM pg_index i
JOIN pg_class c ON c.oid = i.indrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
WHERE i.indisprimary AND n.nspname = 'moneylog'
ORDER BY 1;
```

기대: `tbl_user` → `id_key`, 나머지 14개 → `idx`.

### 2-4. 감사 컬럼 4종

```sql
SELECT table_name, count(*) FILTER (
  WHERE column_name IN ('created_at','updated_at','created_by','updated_by')
) AS audit_cols
FROM information_schema.columns
WHERE table_schema = 'moneylog'
GROUP BY table_name HAVING count(*) FILTER (
  WHERE column_name IN ('created_at','updated_at','created_by','updated_by')
) <> 4;
```

기대: **0행** (모든 테이블이 4개를 다 갖는다).

### 2-5. 부분 유니크 인덱스 2건

```sql
SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname = 'moneylog' AND indexdef LIKE '%WHERE%';
```

기대: `ux_user_email`(`WHERE email IS NOT NULL`), `ux_user_session_active`(`WHERE revoked = false`).

### 2-6. 통계 상세의 FK 부재

```sql
SELECT conname, conrelid::regclass FROM pg_constraint
WHERE contype = 'f'
  AND conrelid::regclass::text LIKE '%statistics_expend_group%'
   OR conrelid::regclass::text LIKE '%statistics_payment_method%';
```

기대: `statistics_idx` FK 각 1건씩만. `expend_group_idx`·`payment_method_idx` FK는 **없어야** 한다(FR-078a).

## 3. 동작 검증 시나리오 (SQL 또는 통합 테스트)

Spec의 Acceptance Scenarios를 저장 구조 수준에서 확인한다. 각 항목은 통합 테스트 1건으로 옮길 수 있다.

| # | 시나리오 | 기대 | Spec |
|---|----------|------|------|
| 1 | 같은 `user_id`로 회원 2건 INSERT | 두 번째 실패 | US1-2 |
| 2 | `email = NULL` 회원 2건 INSERT | 둘 다 성공 | US1-3, Edge |
| 3 | 같은 이메일 값으로 회원 2건 | 두 번째 실패 | US1-3 |
| 4 | 한 회원에 `revoked=false` 세션 2건 | 두 번째 실패 | US1-5, FR-017 |
| 5 | 세션 폐기 후 새 세션 INSERT | 성공, 활성 1건 | US1-5 |
| 6 | 수단 `deleted=true`로 UPDATE 후 그 수단을 쓴 지출 조회 | `payment_method_name` 그대로 읽힘 | US2-3 |
| 7 | 같은 회원·같은 이름 지출유형 2건 | 두 번째 실패. 다른 회원은 성공 | US2-5 |
| 8 | 지출유형 `deleted=true` 후 목표금액 조회 | 목표금액 행·참조 유지 | US2-8, FR-038 |
| 9 | 12개월 할부 12행 INSERT (같은 group id, index 1~12) | 성공 | US3-4 |
| 10 | `payment_date > CURRENT_DATE` 조건 삭제 | 오늘·과거 회차는 남음 | US3-5, FR-045 |
| 11 | 같은 `(fixed_expense_idx, year, month)` 2건 | 두 번째 실패 | US4-3, FR-053 |
| 12 | 같은 조합 동시 INSERT (`ON CONFLICT DO NOTHING`) | 1건만 남음 | Edge, FR-054 |
| 13 | 고정지출 관리 행 DELETE | 그 월별 내역 전부 함께 삭제 | US4-6, FR-059 |
| 14 | 같은 `(id_key, year, month)` 통계 2건 | 두 번째 실패 | US5-6, FR-074 |
| 15 | 통계 저장 후 지출유형 행 DELETE 시도 | 지출 FK RESTRICT로 막히거나, 통계 요약은 무관하게 남음 | FR-078a |
| 16 | 통계 스냅샷 DELETE | 상세 3종 함께 삭제 | CASCADE |
| 17 | `role = 2`로 회원 INSERT | CHECK 위반 | FR-013 |
| 18 | `target_amount = 100000001` INSERT | CHECK 위반 | FR-070 |
| 19 | `month = 13` INSERT | CHECK 위반 | research §6 |
| 20 | 어떤 테이블이든 `created_by` 없이 INSERT | NOT NULL 위반 (`tbl_user` 제외) | FR-004 |

**권장 형태**: `@SpringBootTest` + `@Transactional` 통합 테스트. 제약 위반은 `DataIntegrityViolationException`으로 잡는다. 동시 INSERT(#12)는 별도 트랜잭션 2개가 필요하므로 `@Transactional` 밖에서 돌린다.

## 4. 덤프 재생성 (헌장 VI)

```powershell
$env:PGPASSWORD='1q2w3e4r'
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U moneyloguser -d moneylogdb `
  --schema=moneylog --schema-only --no-owner --no-privileges --encoding=UTF8 `
  --restrict-key=moneylogdumpkey -f sql\schema-moneylogdb.sql
```

**시드 데이터 덧붙이기는 하지 않는다** — 기본 지출유형 10종은 회원마다 생기는 데이터라 마스터가 아니다.

재생성 후 확인:

```powershell
Select-String -Path sql\schema-moneylogdb.sql -Pattern 'CREATE TABLE' | Measure-Object
# 15
Select-String -Path sql\schema-moneylogdb.sql -Pattern 'INSERT INTO' | Measure-Object
# 0  ← 실데이터·시드 없음
```

## 5. 완료 판정

**판정 완료 — 2026-09-02.** 9개 항목 전부 통과했다. §2의 SQL 검증은 손으로 돌리는 대신
`SchemaStructureIT`·`AuditColumnIT`로 옮겨 `:data-mod:test`가 매번 확인한다 — 손으로만 두면
규칙이 깨진 순간 아무도 이 문서를 다시 펼치지 않아 검증이 실행되지 않는다.

- [x] `tbl_user*` 테이블 15개 + 시퀀스 `seq_installment_group` 생성됨 — `SchemaStructureIT`
- [x] 레거시 이름 테이블 0건 (§2-2) — `SchemaStructureIT`
- [x] PK 규칙 통과 — `tbl_user`만 `id_key` (§2-3) — `SchemaStructureIT`
- [x] 15개 전부 감사 컬럼 4종 보유 (§2-4) — `AuditColumnIT`. `tbl_user`만 `created_by`·`updated_by`가 NULL 허용인 것도 함께 확인한다
- [x] 부분 유니크 2건 존재 (§2-5) — `SchemaStructureIT`가 인덱스 정의의 `WHERE` 절까지 본다
- [x] 통계 상세의 유형·수단 FK 없음 (§2-6) — `SchemaStructureIT`(FK 개수)와 `StatisticsBrokenRefIT`(FK 대상)가 각각 확인한다
- [x] 기동 로그의 DDL 실패가 첫 기동의 `drop schema` 1건뿐 — 생성 단계는 0건. 보조 DDL 스크립트가 없어졌으므로 이 항목이 종전의 "`04_constraints.sql` 2회 실행" 검증을 대신한다
- [x] 테이블 주석 15건 존재 — `pg_description`(`objsubid = 0`)
- [x] §3 시나리오 20건 통과 — 20건이 모두 `@DisplayName("#N …")`으로 표시된 테스트에 대응한다. `:data-mod:test` 77건 전부 통과
- [x] `sql/schema-moneylogdb.sql` 재생성 + 같은 커밋 포함, `INSERT INTO` 0건 — `CREATE TABLE` 15건, `CREATE SEQUENCE` 1건, `INSERT INTO` 0건
