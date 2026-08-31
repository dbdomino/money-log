# Research: 001-backend-db-schema

## 1. 물리 테이블 명명 (레거시 비겹침)

**Decision**: 신규 테이블은 `tbl_be_<domain>` 형식을 쓴다 (`be` = backend API 영속 계층).

| 논리 단위 | 물리 테이블 | 레거시(충돌 회피 대상) |
|-----------|-------------|------------------------|
| 회원 | `tbl_be_member` | `tbl_member` |
| 회원 세션 | `tbl_be_member_session` | (없음) |
| 로그인 이력 | `tbl_be_login_history` | `tbl_login_history` |
| 수단 | `tbl_be_payment_method` | `tbl_payment_Method` → PG 소문자 `tbl_payment_method` |
| 지출유형 | `tbl_be_expend_group` | `tbl_expend_group` |
| 월별 지출 내역 | `tbl_be_expense` | `tbl_expend` |
| 월별 수입 내역 | `tbl_be_income` | (없음, 예방적 분리) |
| 고정지출 관리 | `tbl_be_fixed_expense` | `tbl_expend_fix` |
| 월별 고정지출 내역 | `tbl_be_fixed_expense_monthly` | (없음) |
| 기본 목표금액 | `tbl_be_expend_target_default` | (없음) |
| 월별 목표금액 | `tbl_be_expend_target_monthly` | (없음) |
| 월별 통계 | `tbl_be_stat_monthly` | `expend_statistics` |
| 통계 주별 | `tbl_be_stat_weekly` | (없음) |
| 통계 유형별 | `tbl_be_stat_group` | `expend_statistics_detail` |
| 통계 수단별 | `tbl_be_stat_method` | (없음) |

**Rationale**: FR-008 — `tbl_` 접두사 + 레거시와 이름 비겹침. PostgreSQL은 따옴표 없는 식별자를 소문자로 접으므로 `tbl_payment_Method`와 `tbl_payment_method`는 동일 객체다. `tbl_be_`로 레거시와 확실히 분리한다.

**Alternatives considered**:
- `tbl_v2_*` — 버전 숫자는 의미 전달이 약함
- 레거시 이름 재사용·컬럼 교체 — 레거시 Entity와 `ddl-auto: update` 충돌
- 스키마 분리(`moneylog_be`) — 헌장이 `moneylog` 단일 스키마를 전제

---

## 2. 회원 식별자 (PK / FK)

**Decision**: 로그인 아이디(`member_id` VARCHAR(20))를 **자연키 PK**로 쓴다. 자식 테이블 FK도 동일 문자열을 참조한다.

**Rationale**: API·명세의 `memberId`가 로그인 id이며 변경 API가 없다(FR-010). 대리키를 두면 전 계층에 매핑이 늘고, 본 기능은 저장 구조만 다루므로 자연키가 단순하다.

**Alternatives considered**:
- BIGINT 대리키 + UNIQUE(login_id) — 표준적이지만 본 스펙 가정과 API 필드가 문자열 id에 맞춰져 있음
- UUID PK — 불필요하게 길고 JOIN·인덱스 비용만 증가

---

## 3. 이메일 유니크 (NULL 다중 허용)

**Decision**: 부분 유니크 인덱스 `UNIQUE (email) WHERE email IS NOT NULL` (PostgreSQL).

**Rationale**: FR-012·Edge Cases — 빈 이메일은 여러 명 허용, 값이 있으면 전역 유일.

**Alternatives considered**:
- 빈 문자열 `''` 저장 후 UNIQUE — NULL과 빈 문자열이 혼재하면 버그
- 애플리케이션만 검사 — 동시성에서 레이스 가능

---

## 4. 활성 세션 1건 제약

**Decision**: 부분 유니크 `UNIQUE (member_id) WHERE revoked = false`. 폐기 세션은 행을 남기고 `revoked=true`, 토큰 해시 NULL.

**Rationale**: FR-016·017. DB가 “활성 세션 최대 1건”을 강제하면 로그인 레이스에서도 안전하다.

**Alternatives considered**:
- 앱 로직만으로 기존 세션 revoke — 동시 로그인 시 이중 활성 가능
- 회원당 세션 1행만 upsert — 폐기 이력·`1006` 구분이 어려움(FR-016)

---

## 5. 연·월 저장 형태

**Decision**: 연·월이 키/기간인 곳은 `year INT` + `month INT`(1~12). 결제 일자는 `DATE` (`payment_date`).

적용: 고정지출 `start_year`/`start_month`/`end_year`/`end_month`, 월별 고정지출 내역·목표·통계 키.

기간 포함 판정은 `year * 12 + month` 합성값 비교로 한다(월별 고정지출 내역 생성·수동 재작성 공통).

**Rationale**: FR-041 “연과 월을 각각 비교·정렬”. INT 비교가 `YYYY-MM` 문자열보다 명확하고 CHECK로 1~12를 걸기 쉽다.

**Alternatives considered**:
- `CHAR(7) YYYY-MM` — API와 동일하나 범위 비교·CHECK가 번거로움
- `DATE`의 1일만 사용 — 의미상 과잉, day 무시 규칙이 필요

---

## 6. 할부 그룹 식별자

**Decision**: `installment_group_id BIGINT` — DB 시퀀스 `tbl_be_expense_installment_group_seq`에서 발급. 그룹 전용 테이블은 두지 않음(스펙).

**Rationale**: FR-033~035. N개 지출 행이 같은 id를 공유. 별도 그룹 테이블은 스펙이 금지에 가깝게 “두지 않음”.

**Alternatives considered**:
- UUID — 문자열·용량 증가, 이점 적음
- 첫 지출 PK를 그룹 id로 — 생성 순서·롤백 시 복잡도

---

## 7. 스냅샷 vs 조인

**Decision**:
- **지출·소득·통계 요약**: 이름 컬럼 스냅샷 저장
- **고정지출 관리 + 월별 고정지출 내역**: 참조만, 조회 시 원본 이름(Clarification Q4)

**Rationale**: 거래·통계는 시점 보존(FR-031·058). 고정지출은 관리 설정에서 파생된 값이므로 내역 행도 현재 이름을 반영한다(Clarification). 이름을 스냅샷으로 굳히면 수동 재작성(FR-049) 후 같은 달의 이름이 갱신 여부에 따라 갈리는 문제가 생긴다.

---

## 8. FK / 삭제 정책

**Decision**:

| 관계 | DB 동작 |
|------|---------|
| 자식 → 회원 | `ON DELETE RESTRICT` (회원 행 삭제 없음; 정지는 `active` 플래그) |
| 지출/소득 → 수단 | `ON DELETE RESTRICT` (수단은 soft-delete만) |
| 지출 → 지출유형 | `ON DELETE RESTRICT` (지출 있으면 유형 삭제 거부 = FR-027) |
| 고정지출 → 유형/수단 | `ON DELETE RESTRICT` 권장. **유형 삭제는 앱이 지출만 검사**(Clarification Q1)이므로, 고정지출이 남아 있으면 DB RESTRICT가 삭제를 막을 수 있음 → **앱 삭제 전에 고정지출 참조를 끊거나, 유형 FK를 DB에서 빼고 앱 검증만 할 경우 스펙과 불일치** |

**Clarification 정합**: Q1은 “고정지출·목표가 있어도 유형 삭제 허용”. 따라서:

- `tbl_be_expense.expend_group_id` → FK RESTRICT (지출만 차단)
- `tbl_be_fixed_expense.expend_group_id`, `tbl_be_expend_target_* .expend_group_id`, 통계 요약의 group id → **FK 없이 논리 참조** 또는 `ON DELETE SET NULL`이 필요하나 NOT NULL이면 SET NULL 불가

**Final**: 고정지출(관리·월별 내역)·목표·통계의 `expend_group_id`는 **FK 제약 없이 BIGINT 논리 참조**로 둔다. 지출만 FK RESTRICT. 고아 참조는 애플리케이션이 조회 시 처리(스펙이 DB 차단을 요구하지 않음).

`tbl_be_fixed_expense_monthly.payment_method_id`도 같은 이유로 논리 참조다. 반면 `tbl_be_fixed_expense_monthly.fixed_expense_id`는 **FK CASCADE** — 관리 행이 사라지면 그 달 내역이 남을 이유가 없다(FR-048).

수단 soft-delete: 물리 DELETE 없음 → 지출 FK RESTRICT와 양립.

**Alternatives considered**:
- 모든 참조에 FK CASCADE — Clarification Q1·소프트 삭제와 충돌
- 유형 삭제 시 고정지출 cascade — 스펙 비의도

---

## 9. 감사 컬럼

**Decision**: 대부분 테이블에 `created_at`, `updated_at` TIMESTAMPTZ NOT NULL. 세션은 `created_at` + `last_accessed_at`(nullable). 로그인 이력은 `login_at`만.

**Rationale**: FR-004.

---

## 10. 토큰 해시·비밀번호

**Decision**: `pw` / token hash는 `VARCHAR(100)` (bcrypt ~60, SHA-256 hex 64). 해시 알고리즘 구현은 인증 기능 범위; 컬럼 길이만 확보.

**Rationale**: `_공통.md` bcrypt·세션 해시 규격.

---

## 11. 기본 지출유형·아이콘

**Decision**: 가입 시 10종 INSERT + 템플릿 PNG를 `{memberId}_{유형이름}.png`로 복사. 템플릿 경로: `classpath:seed/expend-group-icons/*.png` (이미 커밋됨). DB에는 `icon_filename` 저장.

**Rationale**: FR-019 Clarification Q5. 복사 로직은 회원 가입 구현(Phase 1 API)에서 수행; 본 기능은 컬럼·시드 파일·문서화까지.

---

## 12. Entity 배치 vs Flyway

**Decision**: 기존과 동일하게 **JPA `ddl-auto: update`**로 스키마 반영. 별도 Flyway/Liquibase는 도입하지 않음. 반영 후 `pg_dump`로 `schema-moneylogdb.sql` 재생성.

**Rationale**: 헌장·현재 `application-postgresql.yml`과 일치. 부분 유니크·CHECK는 Hibernate가 약할 수 있어 **보조 SQL 스크립트**(`sql/04_be_constraints.sql` 등) 또는 `@Table(indexes=...)` + 수동 DDL을 tasks에서 명시.

**Alternatives considered**:
- Flyway 전면 도입 — 본 기능 범위를 넘김
- 순수 수동 DDL만 — Entity와 이중 관리

---

## 13. 병행 과제 (명세 개정)

**Decision**: `PaymentMethodCreate`/`Update` Body에 `purpose`(`EXPENSE`|`INCOME`) 추가, 용도 변경 시 참조 검사 규칙을 `프로젝트설계/기능명세상세-백엔드/`에 반영. **스키마 구현과 병행·선행**하되 Entity는 `purpose` NOT NULL로 둔다.

추가(Session 2026-08-29): 고정지출 4.x 명세에 **월별 고정지출 내역 수동 재작성**·**월별 고정지출 내역 단건 수정** 두 API를 넣고, 기존 "월별 예외 저장" API를 후자로 대체한다(§14).

**Rationale**: Clarification·FR-023. 스키마만 만들고 API 명세가 비면 Phase 2 구현이 막힘.

---

## 14. 고정지출 저장 3분할 (Session 2026-08-29)

**Decision**: 고정지출을 **관리(설정) / 월별 내역 / 월별 수입·지출 내역** 세 저장 단위로 나눈다.

| 저장 단위 | 테이블 | 성격 |
|-----------|--------|------|
| 고정지출 관리 | `tbl_be_fixed_expense` | 달마다 반복되는 **설정 템플릿** |
| 월별 고정지출 내역 | `tbl_be_fixed_expense_monthly` | 그 달에 실제로 잡히는 **한 건** |
| 월별 수입·지출 내역 | `tbl_be_income` · `tbl_be_expense` | 사용자가 직접 넣는 **일반 거래** |

고정지출 행은 `tbl_be_expense`에 **섞이지 않는다**. 월별 조회에서 두 축을 분리해 읽는 것이 목적(사용자 요구).

**생성 시점 — lazy materialize**: 그 연·월을 **처음 조회할 때** 관리 테이블에서 복사해 INSERT한다. PK `(fixed_expense_id, year, month)` + `ON CONFLICT DO NOTHING`으로 동시 요청에서도 1건만 남는다.

**Alternatives considered**:
- **등록 시 기간 전체 일괄 생성** — 조회는 가장 단순하나, 설정 수정·기간 변경 때마다 미래 행 대량 재작성이 필요하고 12개월 등록에 즉시 12행이 쌓임
- **월초 배치 생성** — 조회가 순수 읽기가 되지만 배치 운영·멱등성·과거 달 보정 경로가 따로 필요. 이 규모에 과함
- **저장 없이 조회 시 전개**(직전 설계) — 그 달 값을 고치려면 별도 "예외" 저장 단위가 필요하고, 고정지출 내역에 안정적인 식별자가 없어 단건 수정 API(FR-046)를 만들기 어려움

**`modified` 플래그**: 사용자가 그 달 값을 직접 고쳤는지 표시한다. 설정 변경 자동 반영(FR-047)과 수동 재작성(FR-049)이 **덮어써도 되는 행**을 구분하는 유일한 근거다. 이 컬럼이 없으면 설정을 바꿀 때 사용자가 손댄 달까지 되돌아간다.

**예외 테이블 폐기**: `tbl_be_fixed_expense_override`는 만들지 않는다. 월별 내역 행 자체가 그 달의 값이므로, 예외를 따로 두면 같은 정보가 두 곳에 생겨 동기화 규칙이 필요해진다.

**수동 재작성이 필요한 이유**: lazy 생성은 "그 달을 처음 열 때" 한 번만 돈다. 이미 연 달에 대해 고정지출을 새로 등록·수정·삭제해도 그 달 내역은 자동으로 따라오지 않는다(FR-047은 미래 달만). 사용자가 버튼으로 그 달을 다시 맞출 수 있는 경로가 필요하다 — FR-049.
