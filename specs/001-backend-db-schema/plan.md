# Implementation Plan: 백엔드 API를 지탱하는 DB 테이블 구성

**Branch**: `develop` | **Date**: 2026-08-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-backend-db-schema/spec.md`

## Summary

백엔드 56개 API(Phase 1~5)가 읽고 쓸 **15종 저장 단위**를 PostgreSQL `moneylog` 스키마에 물리 테이블로 확정·반영한다. JPA Entity·Repository는 `data-mod`에 두고, API Controller·Service 구현은 범위 밖이다.

기술 접근: Spring Data JPA Entity(`ddl-auto: update`)로 테이블·FK·일반 인덱스를 만들고, Hibernate가 표현하지 못하는 **부분 유니크 인덱스 2건과 CHECK 20건, 할부 시퀀스**만 멱등 보조 DDL(`sql/04_constraints.sql`)로 적용한다. 감사 4컬럼은 `@MappedSuperclass` + `AuditingEntityListener`로 15개 Entity에 상속시킨다. 반영 후 `sql/schema-moneylogdb.sql`을 재생성한다.

**2026-08-31 전면 개정** — spec 재작성과 clarification 4건을 반영해 이전 판 설계를 다음과 같이 바꾼다.

| 항목 | 이전 판 | 이번 판 | 근거 |
|------|---------|---------|------|
| 테이블 명명 | `tbl_be_*` 접두사 | `tbl_user` + `tbl_user_*` 접두사 | FR-009 |
| 기본키 | 회원은 로그인 아이디 자연키, 자식은 그 문자열 FK | 전부 대리키. 회원만 `id_key`, 나머지 `idx`, 소유자는 `id_key` | FR-008 |
| 지출유형 삭제 | 물리 삭제 | **삭제 표시(soft delete)** | FR-037 |
| 유형 참조 FK | 고정지출·목표·통계는 FK 없이 논리 참조 | 고정지출·목표는 **FK RESTRICT**, 통계만 FK 없음 | FR-038·FR-078a |
| 감사 컬럼 | `created_at`·`updated_at` | + `created_by`·`updated_by` (전 테이블) | FR-004 |
| 세션·이력 보존 | 미정 | 무기한 보존, 정리 배치 없음 | FR-019a |

지출유형이 물리 삭제에서 삭제 표시로 바뀐 것이 설계를 가장 크게 단순화했다 — 이전 판이 "유형이 사라질 수 있어서" 고정지출·목표금액의 유형 참조에 FK를 못 걸고 논리 참조로 남겼던 회피책이 통째로 없어졌다. 이제 통계 상세 2건만 FK 없는 예외다.

## Technical Context

**Language/Version**: Java 17 (OpenJDK), Spring Boot 4.1.0, Gradle 9.6.1

**Primary Dependencies**: Spring Data JPA(Hibernate), Spring Data JPA Auditing, Lombok, PostgreSQL JDBC

**Storage**: PostgreSQL 18 — DB `moneylogdb`, 스키마 `moneylog` (`hibernate.default_schema`, Entity에 하드코딩 금지). 아이콘 이미지는 파일시스템, DB에는 파일명만

**Testing**: JUnit 5 + `@SpringBootTest` 통합 테스트 — Entity 매핑·유니크·CHECK·FK 동작·CASCADE를 [quickstart.md](./quickstart.md) §3의 20개 시나리오로 검증

**Target Platform**: 로컬/서버 JVM — `data-mod`(Entity·Repository) + `money-backend-app`(기동 주체)

**Project Type**: 멀티모듈 웹 API 백엔드의 **영속 계층(데이터 모델) 선행 구축**

**Performance Goals**: 회원 1명의 월 단위 조회(가계부 목록·고정지출 내역·통계)가 인덱스를 타는 것까지. 사용자 수가 많지 않다는 spec 전제(FR-019a 근거)에 따라 TPS·동시성 목표는 두지 않는다

**Constraints**:
- 테이블 이름이 레거시 `money-app` 9개와 겹치지 않을 것 — PostgreSQL 소문자 접힘 포함(FR-009)
- 기본키 `idx`, 회원만 `id_key` + `user_id`, 자식은 `id_key`만 참조(FR-008)
- Entity를 API·화면에 노출하지 않을 것(헌장 II)
- 스키마 변경 시 `sql/schema-moneylogdb.sql` 재생성 필수(헌장 VI)
- 덤프에 회원·거래 실데이터 및 시드 데이터 없음
- 보조 DDL은 멱등할 것 — `ddl-auto: update` 환경에서 반복 실행됨

**Scale/Scope**: 물리 테이블 15개 + 시퀀스 1개 + 부분 유니크 2건 + CHECK 20건 + 인덱스 8건. Entity 15개 + 공통 상위 클래스 1개 + Repository 15개. 회원당 기본 지출유형 10건·아이콘 10파일

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

- [x] **I. 모듈 경계** — Entity·Repository는 `data-mod`에만 둔다. `money-app`(프론트)은 이 테이블을 참조하지 않고 레거시 Entity를 그대로 유지한다. 의존 방향 역행 없음. **PASS**
- [x] **II. 레이어 흐름** — 본 기능은 Entity·Repository까지만 만든다. Controller·Service·DTO를 만들지 않으므로 Entity가 경계를 넘는 일이 없다. **PASS**
- [x] **III. 응답 규격** — API 엔드포인트를 추가하지 않는다. 해당 없음. **PASS**
- [x] **IV. 로깅** — API·AOP를 추가하지 않는다. 해당 없음. **PASS**
- [x] **V. 명세 우선** — spec은 현행 `프로젝트설계/` 문서를 다시 읽고 재작성했다. 아직 어긋나는 문서 3건은 [research.md §13](./research.md)에 병행 개정 과제로 명시했고, Phase 2 구현 전 완료를 조건으로 둔다. **PASS**
- [x] **VI. 스키마 덤프** — 현재 덤프에 테이블이 0건임을 확인하고 설계했다. 반영 후 재생성을 완료 조건에 포함하고([quickstart.md](./quickstart.md) §4), 손 편집 금지·실데이터 미포함 규칙을 [contracts/naming-and-constraints.md](./contracts/naming-and-constraints.md) §8에 명문화했다. **PASS**

**Post-design re-check (Phase 1 완료 후)**: 6개 항목 PASS 유지. Complexity Tracking 위반 없음.

설계 중 헌장과 부딪힐 뻔한 지점 1건과 해소:

> `_공통.md`가 `tbl_member.pw`·`tbl_member_session`을 지목하는데 확정 이름은 `tbl_user`·`tbl_user_session`이다. 헌장 원칙 V(명세 우선)에 따라 구현을 임의로 맞추지 않고 **명세를 개정**하는 것으로 처리한다 — research §13 과제 3번.

## Project Structure

### Documentation (this feature)

```text
specs/001-backend-db-schema/
├── plan.md                          # 이 파일
├── research.md                      # Phase 0 — 결정 13건
├── data-model.md                    # Phase 1 — 테이블 15개 컬럼 정의
├── quickstart.md                    # Phase 1 — 검증 절차·시나리오 20건
├── contracts/
│   ├── table-inventory.md           # 테이블·시퀀스 확정 목록, 레거시 비겹침 표
│   └── naming-and-constraints.md    # 명명·FK·UNIQUE·CHECK·인덱스·덤프 규칙
├── checklists/
│   └── requirements.md              # spec 품질 체크리스트 (16/16)
└── tasks.md                         # Phase 2 — /speckit-tasks 가 생성 (미생성)
```

### Source Code (repository root)

```text
data-mod/
└── src/main/java/com/dbdomino/moneylog/data/
    ├── entity/
    │   ├── BaseAuditEntity.java          # @MappedSuperclass — created_at/by, updated_at/by
    │   ├── User.java                     # tbl_user (PK id_key)
    │   ├── UserSession.java              # tbl_user_session
    │   ├── UserLoginHistory.java         # tbl_user_login_history
    │   ├── UserPaymentMethod.java        # tbl_user_payment_method
    │   ├── UserExpendGroup.java          # tbl_user_expend_group
    │   ├── UserExpense.java              # tbl_user_expense
    │   ├── UserIncome.java               # tbl_user_income
    │   ├── UserFixedExpense.java         # tbl_user_fixed_expense
    │   ├── UserFixedExpenseMonthly.java  # tbl_user_fixed_expense_monthly
    │   ├── UserExpendTargetDefault.java  # tbl_user_expend_target_default
    │   ├── UserExpendTargetMonthly.java  # tbl_user_expend_target_monthly
    │   ├── UserStatistics.java           # tbl_user_statistics
    │   ├── UserStatisticsWeekly.java     # tbl_user_statistics_weekly
    │   ├── UserStatisticsExpendGroup.java     # tbl_user_statistics_expend_group
    │   └── UserStatisticsPaymentMethod.java   # tbl_user_statistics_payment_method
    ├── repository/                        # Spring Data JPA Repository 15개
    ├── config/
    │   └── JpaAuditingConfig.java         # @EnableJpaAuditing + AuditorAware<Long>
    └── package-info.java

data-mod/src/test/java/.../             # 스키마 검증 통합 테스트 (quickstart §3)

sql/
├── 04_constraints.sql                  # 신규 — 부분 유니크·CHECK·시퀀스 (멱등)
└── schema-moneylogdb.sql               # 반영 후 재생성

app-mod/money-backend-app/
└── src/main/resources/seed/expend-group-icons/   # 기본 유형 30×30 PNG 10개 (커밋 완료)

app-mod/money-app/.../entity/           # 레거시 — 손대지 않음
```

**Structure Decision**: 영속 계층만 `data-mod`에 추가한다. `money-backend-app`은 스키마 반영을 위해 기동하는 역할만 하고 Controller·Service를 만들지 않는다. `money-app`(프론트)과 레거시 Entity는 건드리지 않으며, 테이블 이름이 겹치지 않아 두 세계가 같은 스키마에서 공존한다.

`AuditorAware<Long>`만 `SecurityContext`에 의존하는데, 인증 필터가 아직 없는 이 시점에는 **빈 Optional을 반환하는 임시 구현**으로 두고 Phase 1(회원·인증)에서 실제 `id_key`를 공급하도록 채운다. 그 사이 테스트는 `created_by`/`updated_by`를 명시적으로 넣어 NOT NULL을 만족시킨다.

## 진행 순서 (개요)

세부 작업 분해는 `/speckit-tasks`가 만든다. 여기서는 의존 순서만 남긴다.

1. **공통 기반** — `BaseAuditEntity`, `JpaAuditingConfig`(임시 `AuditorAware`)
2. **회원 축** — `User` → `UserSession`, `UserLoginHistory` (이후 모든 Entity가 `id_key`를 참조하므로 먼저)
3. **기준 데이터** — `UserPaymentMethod`, `UserExpendGroup`
4. **거래** — `UserExpense`(+ 할부 컬럼), `UserIncome`
5. **고정지출** — `UserFixedExpense` → `UserFixedExpenseMonthly`(CASCADE)
6. **목표금액** — `UserExpendTargetDefault`, `UserExpendTargetMonthly`
7. **통계** — `UserStatistics` → 상세 3종(CASCADE)
8. **Repository 15개**
9. **보조 DDL** `sql/04_constraints.sql` 작성·적용(멱등 확인)
10. **검증 테스트** — quickstart §3 시나리오 20건
11. **덤프 재생성** — `sql/schema-moneylogdb.sql`, 같은 커밋에 포함

2~7은 FK 방향에 따른 순서이고, 각 단계 안에서는 병렬로 만들 수 있다.

## Complexity Tracking

> 위반 없음 — 본 섹션 비워 둔다.
