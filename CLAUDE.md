# CLAUDE.md

이 저장소에서 작업할 때 지켜야 하는 팀 계약이다. 원문 규범은
[.specify/memory/constitution.md](.specify/memory/constitution.md)이며, 두 문서가 어긋나면
**헌장이 기준**이다. 이 파일은 실제 작업에서 자주 걸리는 규칙과 함정을 추린 것이다.

## 프로젝트

Spring Boot 멀티모듈 가계부. 프론트(Thymeleaf)와 백엔드 API가 분리되어 있다.

| 모듈 | 역할 |
|------|------|
| `app-mod/money-app` | Thymeleaf UI (:8080). **DB 직접 접근 없음**, 백엔드 API만 호출 |
| `app-mod/money-backend-app` | REST API (:8081, `/api/v1/*`) |
| `core-mod` | DB 사용 모델의 추상 설계 |
| `data-mod` | Entity·Repository·DataSource/JPA 설정 |
| `common-mod` | 응답 규격, 에러코드, 예외, AOP, 공통 유틸 |

의존은 아래로만 흐른다: `money-app → common-mod` / `money-backend-app → core-mod, data-mod, common-mod` / `core-mod → data-mod, common-mod` / `data-mod → common-mod`. 역방향 참조는 금지다.

| 항목 | 값 |
|------|-----|
| Java | 17 |
| Spring Boot | 4.1.0 |
| Gradle | 9.6.1 (Wrapper) |
| DB | PostgreSQL 18 — `moneylogdb`, 스키마 `moneylog` |
| API Base | `http://localhost:8081/api/v1` |

**모듈별 `build.gradle`이 없다.** 의존성은 전부 루트 `build.gradle`의 `project(':...')` 블록에서 고친다.

## 반드시 지키는 것

### 레이어와 경계

- 호출은 `Controller → Service → 모델 → Repository(data-mod)` **한 방향만**. Controller가 Repository를 직접 부르지 않는다.
- **Entity를 API·화면에 노출하지 않는다.** 경계를 넘는 데이터는 Request/Response/Service DTO로 용도별로 분리한다.
- Mapper는 Entity ↔ DTO 변환만 한다. 비즈니스 로직을 담지 않는다.

### API 응답

성공·실패 모두 이 형태다.

```json
{ "resCode": 200, "data": {} }
```

- `resCode`는 숫자. 성공 `200`, 실패는 **정수 4자리** 에러코드.
- 에러코드는 **Enum으로 정의**하고 비즈니스·검증 실패는 커스텀 예외로 던진다. `RuntimeException`에 문자열만 담아 던지지 않는다.
- Controller에서 try-catch로 응답을 제각각 만들지 않고 전역 예외 처리에 위임한다.
- HTTP 메서드: **GET** 조회 · **POST** 생성 · **PATCH** 수정(omit=유지) · **DELETE** 삭제. **PUT은 쓰지 않는다.**
- 목록 응답은 `data.list`(object 배열)로 통일한다. 페이징은 `offset`·`limit`·`totalCount`.

### 로깅

- 콘솔+파일 동시 기록. `System.out.println` 금지, SLF4J와 파라미터 바인딩(`log.info("...{}", v)`)을 쓴다.
- 백엔드의 요청~응답 로깅은 **AOP로** 한다. Controller마다 진입/종료 로그를 수동 작성하지 않는다.
- **비밀번호·토큰은 마스킹**한다.

### 보안

- 비밀번호는 **bcrypt 해시만** 저장한다. 평문 저장 금지.
- 인증은 JWT(Access 1일 + Refresh 7일) + DB 세션. 서명·만료 검증과 **DB 대조를 모두** 수행한다.
- 권한은 관리자 `1`, 일반 `3`(가입 기본값). 사용자는 **본인 데이터만** 접근한다.
- 회원당 활성 세션은 1건. 새 로그인 시 기존 세션을 폐기한다.

### 명세 우선

- 구현 전에 `프로젝트설계/` 명세가 먼저 확정된다. 명세와 구현이 어긋나면 구현을 임의로 바꾸지 않고 **명세를 먼저 개정**한다.
- 명세 표의 **설명 칸은 그 칸만 보고 의미가 읽혀야 한다.** "요청 에코", "동일", "위와 같음", "생략", 빈 칸은 누락으로 본다.

### 스키마 덤프

`sql/schema-moneylogdb.sql`은 현재 DB 구조의 **단일 참조점**이다.

- DB 영향도 판단은 **이 파일을 먼저 읽어서** 한다. 실제 DB에 접속하지 않는다.
- 스키마가 바뀌면 `pg_dump`로 **재생성해 같은 커밋에 포함**한다. 명령은 헌장 원칙 VI 참고.
- 이 파일을 **손으로 편집하지 않는다.**
- 회원·거래 실데이터는 넣지 않는다.

## DB 저장 구조 규칙

`specs/001-backend-db-schema/` 에서 확정한 규칙이다. 상세는
[data-model.md](specs/001-backend-db-schema/data-model.md)·[contracts/](specs/001-backend-db-schema/contracts/).

- 테이블 이름: 회원은 `tbl_user`, 회원 소유 저장 단위는 **`tbl_user_<자원명>`**. 레거시 `money-app` 테이블 이름(`tbl_member`, `tbl_expend`, `tbl_payment_Method` 등)과 **겹치지 않는다**. PostgreSQL은 식별자를 소문자로 접으므로 대소문자 차이는 회피 근거가 되지 않는다.
- 기본키: 전부 대리키 **`idx`**. **`tbl_user`만 `id_key`**.
- 소유자 항목: **`id_key`만** 참조한다. 로그인 아이디(`user_id`)를 자식 테이블에 복사하지 않는다.
- 감사 컬럼: 모든 테이블에 `created_at`·`updated_at`·`created_by`·`updated_by`. `created_by`/`updated_by`는 회원 `id_key`이고 **FK를 걸지 않는다**. `tbl_user`만 두 컬럼이 nullable(가입은 자기 자신을 만드는 행위라 INSERT 시점에 자기 `id_key`가 없다).
- 삭제 정책: 수단·지출유형은 **삭제 표시**(행 보존), 지출·소득·고정지출은 물리 삭제.
- 금액은 원 단위 정수(`BIGINT`), 시각은 `TIMESTAMPTZ`, 연·월은 `INT` 두 개.

## 빌드와 실행

```bash
./gradlew :app-mod:money-backend-app:bootRun   # 백엔드 (:8081)
./gradlew :app-mod:money-app:bootRun           # 프론트 (:8080)

./gradlew :data-mod:test                       # 스키마 검증 통합 테스트
./gradlew :app-mod:money-backend-app:test
```

## 알아둘 함정

이 저장소에서 실제로 시간을 잡아먹었던 것들이다.

**`./gradlew test`(전체)는 실패한다.** `money-app`의 레거시 테스트 3건이 깨져 있다 — `app-mod/money-app/src/test/resources/application.yml`이 메인 설정을 덮어쓰는데 `profiles.active`도 없고 H2 datasource 줄이 전부 주석 처리돼 있어 datasource가 없다(`Failed to determine a suitable driver class`). `init` 커밋부터 그런 상태다. 모듈별로 나눠 돌린다.

**스키마 반영은 앱 기동 한 번이다.** 스키마·테이블·컬럼·FK·인덱스·CHECK·부분 유니크·시퀀스·테이블 주석을 전부 Hibernate가 만든다. 실행할 보조 DDL 스크립트가 없다(`sql/04_constraints.sql`은 2026-09-02에 삭제).

- CHECK과 주석은 **JPA 3.2 표준**으로 적는다 — `@Table(comment = "...", check = @CheckConstraint(name = "...", constraint = "..."))`. Hibernate의 `@Check`·`@Comment`는 Hibernate 7에서 deprecated다.
- 애너테이션으로 표현 못 하는 **부분 유니크 2건과 시퀀스 1건**만 `MoneylogSchemaContributor`(Hibernate `AdditionalMappingContributor` SPI, `META-INF/services` 등록)가 만든다. 여기 SQL은 스키마를 **`${schema}` 자리표시자**로 적어야 한다 — 이름을 직접 읽으면 기여 시점엔 비어 있어 SQL이 스키마 없이 나가고, 접속의 `search_path`를 타고 엉뚱한 스키마에 만들어진다.
- 스키마 생성은 `hibernate.hbm2ddl.create_namespaces: true`가 한다. 이게 없으면 스키마가 없을 때 CREATE TABLE 15건이 전부 실패하는데, **그 실패가 WARN으로만 찍히고 앱은 정상 기동한다** — "기동 성공 + 테이블 0개"가 된다.
- `sql/03_create_schema.sql`에는 `ALTER ROLE ... SET search_path` 한 줄만 남아 있다. 앱은 필요 없고 psql로 직접 붙을 때 쓴다.

**`ddl-auto`가 `create`다.** 개발 단계 설정이라 **기동할 때마다 전 테이블이 drop 후 재생성**된다. `update`로 두면 이미 있는 테이블에 주석이 붙지 않아서다(`comment on table`은 `create table`과 함께만 나간다). 개발용 데이터를 남겨야 하면 `update`로 되돌린다.

**테스트에서 `JdbcTemplate` 갱신은 트랜잭션 안에서 한다.** datasource가 `auto-commit: false`라 트랜잭션 밖 갱신은 **커밋되지 않고 조용히 사라진다.** 정리(cleanup) 코드가 아무 일도 안 하는 것처럼 보이면 이걸 의심한다.

**Spring Boot 4에서 패키지가 옮겨졌다.** `@EntityScan`은 `org.springframework.boot.persistence.autoconfigure`(모듈 `spring-boot-persistence`)에 있다. `@AutoConfigureMockMvc`는 `spring-boot-starter-test`가 더는 끌어오지 않아 `spring-boot-starter-webmvc-test`를 따로 넣어야 한다.

**`data-mod`에는 `@SpringBootConfiguration`이 없다.** 라이브러리 모듈이라 `@SpringBootTest`가 설정을 찾지 못한다. 테스트 전용 부트 클래스(`DataModTestApplication`)가 그래서 있다.

**`money-backend-app`의 컴포넌트 스캔은 `...backend`로 한정돼 있다.** `data-mod`의 Entity·Repository는 `@EntityScan`·`@EnableJpaRepositories`로 따로 지정한다. 이게 빠지면 Entity를 만들어도 테이블이 생기지 않는다.

**JPA Auditing의 기본 시각 제공자는 `LocalDateTime`을 내놓는다.** 감사 시각이 `OffsetDateTime`이라 변환에 실패하므로 전용 `DateTimeProvider`를 물려 둔 상태다.

## 작업 흐름

- 기능 작업은 Spec Kit 흐름을 따른다: `/speckit-constitution` → `/speckit-specify` → (`/speckit-clarify`) → `/speckit-plan` → `/speckit-tasks` → (`/speckit-analyze`) → `/speckit-implement`.
- AI 스킬의 소스 오브 트루스는 **`.agent/skills/`** 다. `.claude/skills/`·`.cursor/skills/`는 gitignore 대상 정션이며 `pwsh .agent/scripts/link-skills.ps1`로 다시 만든다. 스킬을 추가·수정하면 `.agent/skills/README.md` 목록도 갱신한다.
- 커밋 전 자가 점검: 응답이 `{ resCode, data }`인가, Entity가 노출되지 않는가, 명세 표의 설명 칸이 비어 있지 않은가, DB 구조가 바뀌었다면 덤프를 재생성했는가.
- 원칙을 어겨야 한다면 `plan.md`의 Complexity Tracking에 위반 항목·이유·기각한 대안을 기록한다. 기록 없는 위반은 허용하지 않는다.
