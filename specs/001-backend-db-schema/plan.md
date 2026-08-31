# Implementation Plan: 백엔드 API를 지탱하는 DB 테이블 구성

**Branch**: `develop` | **Date**: 2026-08-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-backend-db-schema/spec.md`

## Summary

백엔드 56개 API(Phase 1~5)가 읽고 쓸 **15종 저장 단위**를 PostgreSQL `moneylog` 스키마에 물리 테이블로 확정·반영한다. 레거시 `money-app` 테이블과 **이름 충돌 없이** `tbl_` 접두사로 공존시키고, JPA Entity·Repository는 `data-mod`에 둔다. 스키마 반영 후 `sql/schema-moneylogdb.sql` 덤프를 재생성한다. API Controller/Service 구현은 범위 밖이다.

기술 접근: Spring Data JPA Entity(`ddl-auto: update`) + 부분 유니크 인덱스(이메일·활성 세션) + 기본 지출유형 아이콘 템플릿 시드 파일(이미 제공됨).

**2026-08-29 개정** — 고정지출 저장을 3분할한다: `tbl_be_fixed_expense`(관리·설정) / `tbl_be_fixed_expense_monthly`(월별 고정지출 내역, 그 달 최초 조회 시 lazy 생성) / `tbl_be_expense`·`tbl_be_income`(월별 수입·지출 내역). 고정지출 행은 지출 내역 테이블에 섞지 않는다. `tbl_be_fixed_expense_override`는 폐기하고 월별 내역 행 직접 수정으로 대체한다. 테이블 총 개수는 15개로 동일(교체). 근거: research §14.

## Technical Context

**Language/Version**: Java 17 (OpenJDK), Spring Boot 4.1.0, Gradle 9.6.1

**Primary Dependencies**: Spring Data JPA, Hibernate, Lombok, MapStruct(경계 변환 시), PostgreSQL JDBC

**Storage**: PostgreSQL 18 — DB `moneylogdb`, schema `moneylog` (`hibernate.default_schema`). 아이콘 파일은 파일시스템(`seed/expend-group-icons` 템플릿 → 런타임 `{memberId}_{name}.png`)

**Testing**: JUnit 5 + Spring Boot Test — Entity 매핑·유니크 제약·관계·시드 규칙을 통합 테스트로 검증(앱 기동 + 스키마 반영)

**Target Platform**: 로컬/서버 JVM — `money-backend-app` + `data-mod`

**Project Type**: 멀티모듈 웹 API 백엔드의 **영속 계층(데이터 모델) 선행 구축**

**Performance Goals**: 단일 회원 월간 가계부·통계 조회에 필요한 인덱스(회원+연월, 결제일, 할부 그룹) 확보. 대량 TPS 목표는 본 기능 범위 밖

**Constraints**:
- 신규 테이블명 `tbl_` 접두사 + 레거시 테이블명과 **비겹침** (PostgreSQL 식별자 소문자 접힘 포함)
- Entity는 API에 미노출(헌장 II)
- 스키마 변경 시 `sql/schema-moneylogdb.sql` 재생성 필수(헌장 VI)
- 회원·거래 실데이터는 덤프에 포함하지 않음
- `purpose` 반영을 위한 기능명세 개정은 병행 과제

**Scale/Scope**: 논리 저장 단위 15종 → 물리 테이블 15개 + 시퀀스/인덱스. 회원당 기본 지출유형 10건·아이콘 10파일

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

- [x] **I. 모듈 경계** — Entity/Repository는 `data-mod`에 둔다. `money-app`은 신규 테이블을 직접 쓰지 않으며 레거시 Entity만 유지. 의존 방향 역행 없음.
- [x] **II. 레이어 흐름** — 본 기능은 Repository·Entity만 도입. Controller/Service는 만들지 않음 → Entity API 노출 없음.
- [x] **III. 응답 규격** — API 엔드포인트 추가 없음. 해당 없음(PASS).
- [x] **IV. 로깅** — API AOP 추가 없음. 해당 없음(PASS).
- [x] **V. 명세 우선** — `spec.md` Clarifications 반영. `PaymentMethodCreate`에 `purpose` 추가 등 상세 명세 개정은 **병행 과제**로 명시(구현 전 완료).
- [x] **VI. 스키마 덤프** — 완료 조건에 `sql/schema-moneylogdb.sql` 재생성 포함. 실데이터 미포함.

**Post-design re-check (Phase 1)**: PASS 유지. Complexity Tracking 위반 없음.

## Project Structure

### Documentation (this feature)

```text
specs/001-backend-db-schema/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── table-inventory.md
│   └── naming-and-constraints.md
└── tasks.md                 # /speckit-tasks (미생성)
```

### Source Code (repository root)

```text
data-mod/
└── src/main/java/com/dbdomino/moneylog/data/
    ├── entity/              # 신규 JPA Entity (tbl_be_*)
    ├── repository/          # Spring Data JPA Repository
    └── package-info.java

app-mod/money-backend-app/
└── src/main/resources/seed/expend-group-icons/   # 기본 유형 30×30 PNG (완료)

sql/
└── schema-moneylogdb.sql    # 스키마 반영 후 재생성

app-mod/money-app/.../entity/   # 레거시 유지 (수정하지 않음)
```

**Structure Decision**: 영속성만 `data-mod`에 추가한다. 백엔드 앱은 이후 Phase에서 Entity를 사용한다. 프론트·레거시 Entity는 건드리지 않아 공존 규칙을 지킨다.

## Complexity Tracking

> 위반 없음 — 본 섹션 비워 둔다.
