<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0 (원칙 1개 추가 = MINOR)
- Modified principles: 없음 (I~V 유지)
- Added principles:
  - VI. 스키마 덤프를 DB 단일 참조점으로
- Added sections: 없음 (기술 스택 및 제약 / 개발 워크플로 섹션에 덤프 항목 반영)
- Removed sections: 없음
- Templates:
  - ✅ .specify/templates/plan-template.md — Constitution Check 에 원칙 VI 게이트 추가
  - ✅ .specify/templates/spec-template.md — 변경 불필요 (요구사항 구조가 원칙과 충돌 없음)
  - ✅ .specify/templates/tasks-template.md — 변경 불필요 (덤프 갱신은 구현 작업의 완료 조건으로 처리)
  - ✅ README.md — sql 스크립트 표에 schema-moneylogdb.sql 추가
  - ✅ .cursor/rules/db-schema-dump.mdc — 원칙 VI의 실행 규칙 신규 작성
- Deferred TODOs: 없음
- 참고: 최초 제정(1.0.0)은 기존 `.cursor/rules/*.mdc` 규칙을 원칙 I~V로 승격한 것이다.
-->

# money-log Constitution

## Core Principles

### I. 모듈 경계와 단방향 의존

모듈은 정해진 역할만 수행하고, 의존은 아래 방향으로만 흐른다.

```
money-app → common-mod, (백엔드 API HTTP 호출)
money-backend-app → core-mod, data-mod, common-mod
core-mod → data-mod, common-mod
data-mod → common-mod
```

- `money-app`(프론트)은 DB와 `data-mod`를 **직접 참조하지 않는다(MUST NOT)**. 필요한 데이터는 백엔드 API로 요청한다.
- `money-backend-app`만 DB에 접근하며, `data-mod` 또는 구체 모델을 통해서만 접근한다.
- `core-mod`는 DB 사용 모델의 추상 설계를 두고, 구체화는 `money-backend-app`에서 한다.
- 공통 유틸·상수·에러코드·예외·AOP는 `common-mod`에 두고 전 모듈이 공유한다.
- 상위 모듈이 하위 앱에 의존하는 역방향 참조는 금지한다.

근거: 프론트가 DB에 직접 붙으면 API 계약이 우회되어 권한·검증·로깅이 무력화된다.

### II. 레이어 단방향 흐름과 DTO 경계

호출은 `Controller → Service → 모델 → Repository(data-mod)` 한 방향만 허용한다.

- Controller는 HTTP 요청/응답, 입력 검증, DTO 변환만 담당한다. 비즈니스·DB 로직을 두지 않는다.
- Service는 유스케이스 오케스트레이션을 담당한다. Controller가 Repository를 직접 호출하지 않는다.
- Entity는 API·화면에 **직접 노출하지 않는다(MUST NOT)**. 경계를 넘는 데이터는 Request/Response/Service DTO로 용도별로 분리한다.
- Mapper(MapStruct 등)는 Entity ↔ DTO 변환만 수행하고 비즈니스 로직을 담지 않는다.

근거: 레이어를 건너뛰면 검증과 트랜잭션 경계가 흩어지고, Entity 노출은 스키마 변경이 곧 API 파괴로 이어진다.

### III. 단일 API 응답 규격과 중앙 예외 처리

성공·실패를 포함한 모든 API 응답은 아래 형태를 지킨다.

```json
{ "resCode": 200, "data": {} }
```

- `resCode`는 숫자이며 성공은 `200`, 비즈니스·검증 실패는 **정수 4자리** 에러코드다.
- `data`는 object이며 실패 시 메시지·상세를 담을 수 있다.
- 에러코드는 **Enum으로 정의(MUST)** 하고, 비즈니스·검증 실패는 커스텀 예외로 throw 한다. `RuntimeException`에 문자열 메시지를 담아 던지지 않는다.
- 에러코드·커스텀 예외·전역 핸들러는 `common-mod`에 두고 앱 모듈이 재사용한다.
- Controller에서 try-catch로 응답을 제각각 만들지 않고 전역 예외 처리에 위임한다.
- HTTP 메서드는 **GET** 조회 · **POST** 생성·로그인 · **PATCH** 수정(omit=유지) · **DELETE** 삭제로 고정하며 **PUT은 사용하지 않는다**.

근거: 클라이언트가 단일 파싱 규칙만 유지하면 되고, 에러코드가 Enum이면 명세와 구현의 불일치를 컴파일 시점에 줄일 수 있다.

### IV. 관측 가능한 로깅

`money-app`과 `money-backend-app` 모두 **콘솔(stdout)과 파일에 동시에** 로그를 남긴다.

- Logback에 ConsoleAppender + FileAppender(또는 RollingFileAppender)를 함께 설정한다.
- `System.out.println`을 쓰지 않고 SLF4J Logger와 파라미터 바인딩(`log.info("...{}", v)`)을 사용한다.
- `money-backend-app`의 요청 수신부터 응답 반환까지는 **AOP로 로깅한다(MUST)**. AOP 구현은 `common-mod`에 두고 백엔드 앱에서 활성화한다.
- 최소 기록 항목: HTTP method, URI, 요청 파라미터/바디(민감정보 마스킹), 응답 `resCode`·요약, 처리 시간.
- Controller 메서드마다 동일한 진입/종료 로그를 수동으로 중복 작성하지 않는다.

근거: 비밀번호·토큰이 그대로 남으면 로그 자체가 사고 원인이 되고, 수동 로깅은 누락되는 지점이 반드시 생긴다.

### V. 명세 우선과 설명 완전성

구현 전에 `프로젝트설계/` 명세가 먼저 확정되며, 명세가 API·화면의 기준이다.

- Path / Query / Body / 응답 `data` 표의 **설명 칸은 그 칸만 보고 의미가 읽혀야 한다(MUST)**.
- `_공통.md`에 같은 필드가 있어도 상세 문서 표에 의미를 다시 적는다. 공통 문서는 표준·환산·검증용이다.
- "요청 에코", "동일", "위와 같음", "공통 참고", "생략", 빈 칸, 한 단어 대명사만 적는 것은 누락으로 간주한다. 단위·제약을 포함한 구체적 의미를 적는다.
- 명세와 구현이 어긋나면 구현을 임의로 바꾸지 않고 명세를 먼저 개정한다.

근거: 상세 문서만 열고 작업하는 상황이 기본이므로, 그 문서 안에서 의미가 닫혀 있지 않으면 필드 해석이 사람마다 갈린다.

### VI. 스키마 덤프를 DB 단일 참조점으로

`sql/schema-moneylogdb.sql`은 PostgreSQL `moneylog` 스키마의 현재 상태를 담은 덤프이며, DB 구조를
확인할 때의 **단일 참조점(MUST)** 이다.

- 포함 대상: 테이블 구조, 컬럼 타입·제약, PK/FK, 인덱스, 시퀀스, 코멘트, 그리고 코드·마스터 성격의 **시드 데이터**.
- 제외 대상: 회원·거래 등 **운영/사용자 실데이터**. 개인정보와 비밀번호 해시를 저장소에 남기지 않는다.
- DB 영향도를 판단할 때는 **이 파일을 먼저 읽는다(MUST)**. 컬럼 존재 여부·타입·널 허용·인덱스·제약을
  실제 DB에 접속하지 않고 이 파일로 판단하고, 파일과 코드가 어긋나면 덤프가 낡은 것으로 보고 재생성한다.
- Entity·DDL·시드 데이터가 바뀌면 스키마를 DB에 반영한 뒤 덤프를 **재생성해 같은 커밋에 포함한다(MUST)**.
- 이 파일을 **손으로 편집하지 않는다(MUST NOT)**. 항상 실제 DB에서 재생성한다.

재생성(구조):

```powershell
$env:PGPASSWORD='1q2w3e4r'
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U moneyloguser -d moneylogdb `
  --schema=moneylog --schema-only --no-owner --no-privileges --encoding=UTF8 `
  --restrict-key=moneylogdumpkey -f sql\schema-moneylogdb.sql
```

시드 데이터가 있는 테이블은 아래를 이어서 실행해 같은 파일에 덧붙인다.

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U moneyloguser -d moneylogdb `
  --schema=moneylog --data-only --column-inserts --no-owner --no-privileges --encoding=UTF8 `
  --restrict-key=moneylogdumpkey --table=moneylog.<시드_테이블> >> sql\schema-moneylogdb.sql
```

`--no-owner --no-privileges`와 고정 `--restrict-key`는 의미 없는 diff를 줄이기 위한 것이다. 옵션을
임의로 바꾸면 스키마가 그대로인데도 파일 전체가 변경된 것처럼 보인다.

근거: 스키마는 `ddl-auto: update`로 코드에서 파생되므로, 코드를 전부 읽지 않고 현재 DB 구조를 확인할
수 있는 지점이 없으면 변경 영향도를 추측으로 판단하게 된다.

## 기술 스택 및 제약

| 항목 | 값 |
|------|-----|
| Java | OpenJDK 17 |
| Spring Boot | 4.1.0 |
| Gradle | 9.6.1 (Wrapper 사용) |
| DB | PostgreSQL 18 — `moneylogdb`, 스키마 `moneylog` (`public` 미사용) |
| 프론트 | Thymeleaf |
| 영속성 | Spring Data JPA, MyBatis 4.0.0, MapStruct, Lombok |
| API Base URL | `http://localhost:8081/api/v1` |

- DB 접속·JPA 설정은 `data-mod/src/main/resources/application-postgresql.yml`에 두고, 앱은 `spring.profiles.active=postgresql`로 활성화한다. 앱 모듈에 datasource 설정을 중복 정의하지 않는다.
- 테이블은 `moneylog` 스키마에 생성한다. Entity에 스키마를 하드코딩하지 않고 `hibernate.default_schema`로 제어한다.
- 로컬 DB 초기 생성은 `sql/` 스크립트를 순서대로 실행한다. 생성 절차를 GUI 조작으로만 남기지 않는다.
- 현재 스키마 스냅샷은 `sql/schema-moneylogdb.sql`에 덤프로 유지한다(원칙 VI).
- 인증은 JWT(Access + Refresh)이며 서버가 DB에서 현재 세션·토큰 해시를 관리한다. 서명·만료 검증과 DB 대조를 **모두** 수행한다.
- 비밀번호는 **평문 저장 금지**. bcrypt 단방향 해시만 저장한다.
- 권한은 관리자 `1`, 일반 `3`(가입 기본값)이며, 사용자는 본인 데이터만 접근한다. 관리자 전용 API는 권한 `1`을 요구한다.

## 개발 워크플로와 품질 게이트

- 기능 작업은 Spec Kit 흐름을 따른다: `/speckit-constitution` → `/speckit-specify` → (`/speckit-clarify`) → `/speckit-plan` → `/speckit-tasks` → (`/speckit-analyze`) → `/speckit-implement`.
- 백엔드 구현은 README의 Phase 0~6 순서를 따른다. Phase 0(공통: `RestResponseDto`, `ErrorCode`, AOP, Entity 이전)을 건너뛰고 기능 구현을 시작하지 않는다.
- AI 스킬의 소스 오브 트루스는 `.agent/skills/`다. Cursor 네이티브 경로(`.cursor/skills/`)에 두는 사본은 실행용이며, 스킬 추가·수정 시 `.agent/skills/README.md` 목록을 함께 갱신한다.
- 커밋·완료 전 자가 점검: 응답이 `{ resCode, data }` 규격인지, Entity가 노출되지 않는지, 명세 표의 설명 칸이 비어 있지 않은지, DB 구조가 바뀌었다면 `sql/schema-moneylogdb.sql`을 재생성했는지 확인한다.
- 원칙 위반이 필요한 경우 `plan.md`의 Complexity Tracking에 위반 항목·필요 이유·기각한 단순 대안을 기록한다. 기록 없는 위반은 허용하지 않는다.

## Governance

- 본 문서는 프로젝트의 다른 관행보다 **우선한다**. 코드 리뷰·구현 계획은 본 문서 준수를 확인한다.
- 개정 절차: 변경 제안 → 영향 범위(원칙·템플릿·`.cursor/rules`) 확인 → 문서 수정과 Sync Impact Report 갱신 → 관련 템플릿·규칙 동기화.
- 버전 정책은 유의적 버전을 따른다. MAJOR는 원칙 제거·비호환 재정의, MINOR는 원칙·섹션 추가나 실질적 확장, PATCH는 표현·오타 등 비의미적 수정이다.
- `.cursor/rules/*.mdc`는 본 문서의 요약·실행 규칙이다. 두 문서가 어긋나면 본 문서를 기준으로 규칙을 맞춘다.
- 준수 검토는 `/speckit-analyze`와 `plan.md`의 Constitution Check 게이트에서 수행한다.

**Version**: 1.1.0 | **Ratified**: 2026-08-05 | **Last Amended**: 2026-08-05
