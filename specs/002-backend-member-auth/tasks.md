---

description: "Task list for 002-backend-member-auth"
---

# Tasks: 회원 가입·인증·토큰·관리자 회원 관리

**Input**: Design documents from `/specs/002-backend-member-auth/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: 포함한다. plan.md의 Testing이 JUnit 5 + 실 PostgreSQL 통합 테스트를 지정했고, quickstart.md §3이 검증 시나리오 41건을 확정했다. 각 스토리의 테스트는 그 스토리의 **응답 코드와 DB 상태**가 계약대로 나오는지를 확인한다. 테스트 이름의 `#N`은 quickstart 시나리오 번호와 1:1로 맞춘다 — `001`에서 쓴 방식이고, 빠진 번호를 바로 찾을 수 있다.

**Organization**: 작업은 spec.md의 User Story 4개(P1~P4)로 묶었다. 각 스토리는 테스트 → Repository → Service → DTO → Controller 순으로 완결된다.

**이 기능은 스키마를 바꾸지 않는다.** Entity·컬럼·제약을 건드리는 작업은 하나도 없어야 하며, 끝나고 `git diff sql/schema-moneylogdb.sql`이 비어 있어야 한다(T071).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 미완료 작업에 의존하지 않음)
- **[Story]**: 해당 User Story (US1~US4)
- 모든 작업에 정확한 파일 경로를 적는다

## Path Conventions

배치 기준은 **"DB를 아는가"** 하나다(plan.md Structure Decision · research.md §5).

- `common-mod/src/main/java/com/dbdomino/moneylog/common/` — DB를 모르는 것만: 에러코드·예외·전역 처리·로깅 Aspect·`JwtTokenProvider`
- `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/` — 세션·회원을 읽고 쓰는 전부: 인증 필터·세션 서비스·Controller·Service·DTO
- `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/` — 스토리별 통합 테스트(`auth/`·`member/`·`recovery/`·`admin/`)
- `data-mod/src/main/java/com/dbdomino/moneylog/data/` — Repository 조회 메서드 추가와 임시 조치 제거만
- 빌드 설정: 루트 `build.gradle` (**모듈별 build.gradle이 없다**)
- 설계 명세: `프로젝트설계/기능명세상세-백엔드/`

`core-mod`는 건드리지 않는다. `money-app`(프론트)도 이 기능의 범위 밖이다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 명세 선행 개정(원칙 V)과, 구현이 기대는 의존성·패키지·설정 준비

- [X] T001 **착수 전 명세 개정 4건** — quickstart.md §0이 지정한 3건에 실제 잔존 1건을 더한다. ① `프로젝트설계/기능명세상세-백엔드/_공통.md` 156·161~163행의 구현 위치 가이드에서 `common-mod/.../security/MemberSessionService.java`·`TokenAuthenticationFilter.java`를 `app-mod/money-backend-app/.../service/`·`.../security/` 경로로 고친다(`JwtTokenProvider`는 `common-mod`에 그대로 둔다 — 근거는 plan.md Complexity Tracking). ② `phase1-회원/1.2-MemberSignup.md` 123행 `tbl_member.pw` → `tbl_user.pw`. ③ `phase1-회원/1.3-MemberLogin.md` 121행 `tbl_member_session` → `tbl_user_session`. ④ **quickstart가 적지 않은 잔존** — `phase1-회원/1.6-MemberTokenRevoke.md` 62행 `tbl_member_session` → `tbl_user_session`. 검증: `grep -rn 'tbl_member' 프로젝트설계/기능명세상세-백엔드/` → **0건**. 코드보다 먼저 한다
- [X] T002 루트 `build.gradle`에 의존성을 추가한다 — `project(':app-mod:money-backend-app')` 블록에 `implementation 'org.springframework.boot:spring-boot-starter-security'`, `project(':common-mod')` 블록에 `implementation 'io.jsonwebtoken:jjwt-api:0.12.6'` + `runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'` + `runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'`. **jjwt가 `common-mod`에 붙는 이유**는 `JwtTokenProvider`가 거기 있어서다(research.md §5). 버전을 명시하는 이유는 Spring Boot BOM이 jjwt를 관리하지 않아서다(research.md §2). `common-mod`에는 `spring-boot-starter-security`를 넣지 않는다 — `BCryptPasswordEncoder`는 백엔드에서만 쓴다
- [X] T003 [P] `common-mod/src/main/java/com/dbdomino/moneylog/common/` 아래에 `error/`·`logging/`·`security/` 디렉터리를 만들고 각각 `package-info.java`를 둔다. 각 문서에 **"이 패키지는 DB를 모른다"**는 배치 기준과 그 근거(`common-mod → data-mod` 의존은 헌장 원칙 I 역행)를 적는다
- [X] T004 [P] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/` 아래에 `config/`·`security/`·`service/`·`dto/request/`·`dto/response/`·`mapper/` 디렉터리를 만들고 `package-info.java`를 둔다. `security/`의 문서에는 "세션을 읽는 코드는 전부 여기 있다"를, `dto/`에는 "Entity는 이 경계를 넘지 않는다"를 적는다
- [X] T005 [P] `app-mod/money-backend-app/src/main/resources/application.yml`에 `jwt.secret`(`${JWT_SECRET}` 환경변수 주입, **커밋하지 않는다**)·`jwt.access-token-validity-seconds: 86400`·`jwt.refresh-token-validity-seconds: 604800`을 추가한다(auth-pipeline.md §6). 이 파일 상단 주석이 경고하듯 `spring.jpa.*`는 여기 적어도 조용히 무시되므로 **jwt 설정만** 넣는다. T074의 `moneylog.common.web.enabled: true`도 여기 둔다. `jwt.secret`이 없으면 기동이 실패하므로 quickstart.md §1의 전제대로 `JWT_SECRET`을 환경변수로 넣고 `bootRun`한다 — **테스트는 환경변수를 요구하지 않는다**(T020이 프로퍼티로 직접 싣는다)
- [X] T006 [P] `app-mod/money-backend-app/src/main/resources/logback-spring.xml`을 새로 만든다 — 콘솔 + 파일 동시 기록, 롤링 정책 포함(헌장 원칙 IV). `app-mod/money-app/src/main/resources/logback.xml`의 패턴을 기준으로 맞추되 파일명은 `logback-spring.xml`로 한다(`logback.xml`은 Spring이 개입하기 전에 로드돼 `<springProfile>`을 못 쓴다 — research.md §9)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: API 16건 전부가 기대는 공통 기반 5종. plan.md Summary가 "작업의 무게 중심은 API 16건이 아니라 아직 없는 공통 기반"이라고 적은 그 부분이다

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 어떤 User Story도 시작할 수 없다

- [X] T007 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/ErrorCode.java`에 에러코드 Enum을 만든다 — 코드(int)와 기본 메시지를 갖는다. **002가 쓰는 13개**(`1001`·`1002`·`1003`·`1004`·`1005`·`1006`·`2001`·`2002`·`2003`·`2004`·`2005`·`9001`·`9000`)를 spec.md § 이 기능이 쓰는 에러코드 표 그대로 넣고, **003~006의 `30xx`~`36xx` 대역도 함께** 넣는다(각 스펙에 표가 이미 있어 한 번에 옮길 수 있다 — research.md §7). 대역 주석: 인증·권한 `10xx`, 회원 `20xx`, 공통·서버 `90xx`
- [X] T008 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/BusinessException.java`에 `ErrorCode`를 들고 다니는 `RuntimeException`을 만든다. **코드별 예외 클래스를 만들지 않는다**(research.md §7 — 40여 개가 전부 같은 일을 한다). 메시지 오버라이드 생성자를 하나 둔다. T007에 의존
- [X] T009 `common-mod/src/main/java/com/dbdomino/moneylog/common/api/RestResponseDto.java`의 `fail(int, String)`을 `fail(ErrorCode)`·`fail(ErrorCode, String)` 팩토리로 정리한다. 기존 `ok(T)`는 그대로 둔다. 정수 리터럴을 호출부에 흩지 않는 것이 목적이다. T007에 의존
- [X] T010 `common-mod/src/main/java/com/dbdomino/moneylog/common/error/GlobalExceptionHandler.java`에 `@RestControllerAdvice`를 만든다 — ① `BusinessException` → **HTTP 200** + 그 `ErrorCode`, ② `MethodArgumentNotValidException`·`ConstraintViolationException`·`HttpMessageNotReadableException` → **HTTP 200** + `9001`, ③ `Exception` → **HTTP 500** + `9000`. **비즈니스 실패가 HTTP 200인 것이 이 계약의 특이점이다**(SC-101 · api-contract.md §2). ③만 스택트레이스를 `error` 레벨로 남기고 ①②는 `warn`으로 코드와 경로만 남긴다. T008·T009에 의존
- [X] T011 [P] `common-mod/src/main/java/com/dbdomino/moneylog/common/logging/SensitiveMasker.java`를 만든다 — 이름 기반으로 값 **전체**를 `***`로 대체한다. 대상은 api-contract.md §8 표 그대로: 요청 Body `password`·`passwordConfirm`·`newPassword`·`refreshToken`, 요청 헤더 `Authorization`, 응답 Body `accessToken`·`refreshToken`, 방어용 `pw`. **앞뒤 일부를 남기지 않는다**(토큰 길이·비밀번호 첫 글자가 로그에 남는다). 아이디 찾기 응답의 마스킹(T052)과는 목적이 달라 규칙을 공유하지 않는다
- [X] T012 `common-mod/src/main/java/com/dbdomino/moneylog/common/logging/ApiLoggingAspect.java`에 요청~응답 AOP 로깅을 만든다 — `@RestController` 대상 포인트컷 하나로 진입·종료·소요 시간을 남기고 인자·반환값은 `SensitiveMasker`를 거친다. **Controller마다 수동 로그를 쓰지 않는다**(헌장 원칙 IV). 서블릿 필터에서 body를 읽는 방식은 기각했다(research.md §8 — `ContentCachingRequestWrapper`가 전 요청 body를 메모리에 복사한다). T011에 의존
- [X] T074 **`common-mod`의 빈을 백엔드 컨텍스트에 등록한다** — `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/MoneyBackendApplication.java`의 `scanBasePackages`에 `"com.dbdomino.moneylog.common"`을 추가한다. 현재 값은 `{"com.dbdomino.moneylog.backend", "com.dbdomino.moneylog.data.config"}` 둘뿐이라 **T010의 `GlobalExceptionHandler`(`@RestControllerAdvice`)와 T012의 `ApiLoggingAspect`(`@Aspect`)가 빈으로 등록되지 않는다** — 클래스를 만들어도 예외는 Spring 기본 500 JSON으로 나가고 AOP 로그는 한 줄도 찍히지 않아 헌장 원칙 III·IV의 MUST와 SC-101이 런타임에 성립하지 않는다. **001의 T008이 `data-mod`에서 겪은 것과 같은 함정이다**(그때는 `@EntityScan`이 빠져 Entity를 만들어도 테이블이 생기지 않았다). 함께 처리할 것: `GlobalExceptionHandler`·`ApiLoggingAspect`에 `@ConditionalOnProperty(name = "moneylog.common.web.enabled", havingValue = "true")`를 걸고 백엔드 `application.yml`에서만 `true`로 켠다 — **`money-app`(프론트)의 스캔 범위가 `com.dbdomino.moneylog` 전체**라 이 조건이 없으면 프론트에도 자동 등록되어 기존 `ControllerExceptionHandler`와 advice 2개가 공존한다(`@RestControllerAdvice`는 `@Component` 메타 애너테이션이라 `@Import` 방식으로 바꿔도 프론트 스캔은 피하지 못한다). 확인: 없는 아이디로 로그인해 **HTTP 200 + `resCode 1003`** 이 나오면 걸린 것이다(등록되지 않았다면 HTTP 500이 나간다). **번호는 뒤에 붙었지만 실행 위치는 Phase 2다** — 기존 73건의 상호 참조를 밀지 않으려고 ID만 이어 붙였다. T010·T012에 의존하며 어떤 스토리 테스트보다 먼저 끝나 있어야 한다
- [X] T013 [P] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/config/JwtProperties.java`에 `@ConfigurationProperties("jwt")`를 만든다 — `secret`·`accessTokenValiditySeconds`·`refreshTokenValiditySeconds`. **`secret` 길이(256비트 이상)를 여기서 검증해 기동에서 막는다**(auth-pipeline.md §6 — 짧은 키의 실패를 런타임까지 미루지 않는다). T005에 의존
- [X] T014 [P] `common-mod/src/main/java/com/dbdomino/moneylog/common/security/JwtTokenProvider.java`를 만든다 — HS256 서명·파싱, 클레임 `sub`(memberId)·`role`·`sid`(sessionId UUID)·`exp`·`iat`(auth-pipeline.md §0). **서명키와 만료 초를 생성자 인자로 받는다** — `common-mod`는 `JwtProperties`(백엔드)를 모르므로 빈 등록은 T018의 `SecurityConfig`가 한다. 서명 실패·형식 오류·만료를 구분해 던지되 **DB는 모른다**
- [X] T015 [P] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/security/AuthPrincipal.java`를 만든다 — `memberId`(=`user_id`)·`idKey`·`role`·`sessionId`. **`sub`는 `memberId`이지 `id_key`가 아니다**(auth-pipeline.md §0). 필터가 환산한 `idKey`를 함께 실어야 `BackendAuditorAware`(T019)와 소유자 조회가 성립한다
- [X] T016 [P] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/security/RestAuthEntryPoint.java`를 만든다 — Spring Security 기본 401 JSON 대신 `{ resCode: 1001, data: { message } }`를 HTTP 200으로 내보낸다(api-contract.md §2 · quickstart 시나리오 #37)
- [X] T017 [P] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/security/RestAccessDeniedHandler.java`를 만든다 — 기본 403 대신 `{ resCode: 1002 }`를 HTTP 200으로 내보낸다. **이걸 빠뜨리면 SC-106("관리자 API 5건 전부 `1002`")이 깨진다**(quickstart 시나리오 #38)
- [X] T018 `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/config/SecurityConfig.java`에 `SecurityFilterChain`을 만든다 — 인가 경계는 api-contract.md §1 표를 **그대로** 옮기고, **`anyRequest().authenticated()`를 기본값으로 둔다**(003~006이 경로를 빠뜨려도 열리지 않게 하려는 것이다. 반대로 두면 빠뜨린 API가 조용히 공개된다). `csrf` 비활성·`SessionCreationPolicy.STATELESS`, `BCryptPasswordEncoder` 빈, `JwtTokenProvider` 빈 등록(`JwtProperties` 주입), T016·T017 연결. **인가 규칙을 Controller 애너테이션으로 흩지 않는다**. T013~T017에 의존
- [X] T019 **`AuditorAware`를 실 구현으로 교체한다 — 세 파일을 한 번에 고쳐야 한다.** ① `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/config/BackendAuditorAware.java`를 만들어 `SecurityContext`의 `AuthPrincipal.idKey`를 돌려준다(비로그인 경로는 빈 `Optional`). ② `data-mod/src/main/java/com/dbdomino/moneylog/data/config/JpaAuditingConfig.java`의 임시 `auditorAware()` 빈과 그 TODO 주석을 지운다(`auditingDateTimeProvider()`는 **남긴다** — `OffsetDateTime` 변환 때문에 필요하다). ③ `data-mod/src/test/java/com/dbdomino/moneylog/data/DataModTestApplication.java`에 테스트용 `AuditorAware<Long>` 빈(고정 `id_key`)을 둔다. **②만 하면 `data-mod` 테스트 컨텍스트에 `AuditorAware`가 없어져 NOT NULL 감사 컬럼이 깨지고, ①만 하면 빈이 2개가 되어 기동이 실패한다.** `BaseAuditEntity`의 `@Setter`와 `stampAudit()` 제거는 T066~T068에서 따로 한다 — 호출부가 많아 이 작업과 섞으면 실패 원인이 갈린다. T015에 의존
- [X] T020 `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/AbstractApiIT.java`에 통합 테스트 공통 기반을 만든다 — `@SpringBootTest`·`@AutoConfigureMockMvc`(Boot 4에서는 `org.springframework.boot.webmvc.test.autoconfigure` 패키지이고 `spring-boot-starter-webmvc-test`가 이미 들어가 있다)·`@ActiveProfiles("postgresql")`, 회원/관리자 1명을 만드는 헬퍼, 로그인해 토큰을 얻는 헬퍼, `resCode`를 뽑는 assert 헬퍼, 테스트 회원 정리 헬퍼. 정리는 **트랜잭션 안에서** 한다 — datasource가 `auto-commit: false`라 트랜잭션 밖 `JdbcTemplate` 갱신은 커밋되지 않고 조용히 사라진다. 테스트 아이디 접두사에 `_`를 쓰지 않는다(SQL `LIKE`에서 한 글자 와일드카드다). 두 함정 모두 `001`에서 실제로 시간을 잃은 것이다. **`jwt.secret`을 이 클래스에서 공급한다** — 메인 `application.yml`이 `${JWT_SECRET}`을 요구하는데 환경변수 없이 도는 실행에서는 `JwtProperties`(T013)가 기동을 막아 이 클래스를 상속한 통합 테스트 12개 클래스가 전부 컨텍스트 로딩에서 깨진다. `@SpringBootTest(properties = "jwt.secret=<테스트 전용 48자 이상 문자열>")`로 클래스에 직접 싣는다(`@SpringBootTest`는 `@Inherited`라 하위 테스트가 그대로 물려받는다). 값은 HS256이라 **256비트(32바이트) 이상**이어야 하고 테스트 전용임이 이름에서 드러나야 한다. **`src/test/resources/application.yml`을 만들지 않는다** — 테스트 클래스패스의 같은 이름 파일이 메인 설정을 *대체*해 `spring.profiles.active: postgresql`까지 사라지며, 그게 `money-app`의 레거시 테스트 3건이 `init` 커밋부터 깨져 있는 원인이다(`Failed to determine a suitable driver class`)

**Checkpoint**: 응답 규격·에러코드·로깅·인가 경계가 섰다. 이 시점에 보호 API를 인증 없이 부르면 이미 `{ resCode: 1001 }`이 나온다(quickstart #37). US1~US4를 시작할 수 있다

---

## Phase 3: User Story 1 - 인증 기반이 선다 (Priority: P1) 🎯 MVP

**Goal**: 헬스체크(1.1) 위에 토큰 발급·검증·갱신·폐기(1.3~1.6)를 올린다. 이 넷이 서면 나머지 API의 "권한 = 로그인"이 의미를 갖는다

**Independent Test**: 회원 1명을 DB에 직접 넣고 로그인해 토큰을 받은 뒤, 검증 API가 통과시키고, 갱신 API가 새 토큰 쌍을 주며, 폐기 후에는 같은 토큰이 거부되는지 확인한다. 다른 스토리의 API 없이 완결된다

### Tests for User Story 1

> 먼저 작성하고, 구현 전에 **실패하는 것**을 확인한다

- [X] T021 [P] [US1] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/auth/TokenLifecycleIT.java` — quickstart #1(로그인 시 Access 1일·Refresh 7일 발급, 세션 1건 저장)·#3(`/auth/validate`가 `valid=true`와 남은 만료 시간)·#5(갱신이 새 쌍을 주고 **`session_id`는 그대로**)·#8(로그인→검증→갱신→로그아웃 한 바퀴, SC-102). #5는 **새 행이 생기지 않았는지와 UUID가 같은지**까지 본다 — 바뀌었다면 Rotation이 아니라 재로그인이다
- [X] T022 [P] [US1] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/auth/SessionSingleActiveIT.java` — #2(재로그인 시 기존 세션의 해시 2개가 NULL·`revoked=true`, 활성 세션은 새 것 1건 — SC-103)·#4(폐기된 토큰으로 아무 API나 부르면 `1006` — SC-104)·#6(로그아웃 후 그 Refresh로 갱신하면 **`1005`**, `1006`이 아니다 — api-contract.md §5)
- [X] T023 [P] [US1] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/auth/LoginFailureAndHistoryIT.java` — #7(비활성 계정 로그인 `1004`)·#31(성공 1회 + 비밀번호 오류 1회 → 이력 각 1건, `success`가 `true`·`false` — SC-108)·#32(**존재하지 않는 아이디는 이력 행이 늘지 않는다** — SC-110·FR-127)·#33(이력 테이블 전 행에 비밀번호·토큰 값이 없다 — SC-111). #32가 FR-127의 핵심이라 행 수를 전후로 세어 비교한다

### Implementation for User Story 1

- [X] T024 [US1] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserSessionRepository.java`에 `findByRefreshTokenHash(String)`을 추가한다 — 갱신 2단계가 **해시로 세션을 찾는다**. 이 조회 하나 때문에 저장 해시가 bcrypt가 아니라 SHA-256이다(research.md §3 — bcrypt는 salt 때문에 같은 입력이 매번 다른 값을 내 조회 자체가 성립하지 않는다). 기존 `findBySessionId`·`findByUserIdKeyAndRevokedFalse`는 그대로 쓴다
- [X] T025 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/MemberSessionService.java`를 만든다 — 세션 생성·조회·갱신·폐기와 **토큰 해시 계산(SHA-256 hex 소문자 64자)을 이 클래스 한 곳에 가둔다**(auth-pipeline.md §5 — 알고리즘을 바꿀 때 고칠 자리가 하나여야 한다). Refresh Token은 `SecureRandom` 32바이트 → Base64URL(43자)로 만든다. **폐기는 두 해시를 NULL로 만들고 `revoked=true`를 함께 세우는 것**이다 — 해시만 비우면 부분 유니크 인덱스가 그 행을 활성으로 봐서 다음 로그인이 유니크 위반으로 막히고, `revoked`만 세우면 해시가 남아 검증 7단계를 통과한다. 행은 삭제하지 않는다(FR-111). T024에 의존
- [X] T026 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/LoginHistoryService.java`를 만든다 — 성공·실패 이력 1건을 남긴다(`id_key`·`login_at`·`login_ip`·`success`). **회원이 특정될 때만 행을 만든다**(FR-127): 존재하지 않는 아이디는 `id_key`를 채울 수 없으므로 행 없이 애플리케이션 로그로만 남기고 **그 아이디를 마스킹**한다. `login_ip`는 확보하지 못하면 비운다(IPv6까지 들어가는 `varchar(45)`)
- [X] T027 [P] [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/`에 `LoginRequest`(memberId·password)·`RefreshRequest`(refreshToken)를 만든다. Bean Validation으로 필수 필드를 막고, 누락은 T010이 `9001`로 변환한다
- [X] T028 [P] [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/response/`에 토큰 응답 DTO를 만든다 — `TokenResponse`(`accessToken`·`tokenType`=`Bearer` 고정·`expiresIn`·`refreshToken`·`refreshExpiresIn`), `LoginResponse`(TokenResponse + `memberId`·`nickname`·`role`), `TokenValidateResponse`(`valid` + 남은 만료 시간). **1.3과 1.5의 토큰 필드명을 동일하게 맞춘다**(api-contract.md §3 — 갈리면 프론트가 분기해야 한다). `expiresIn`은 설정값을 그대로 쓰는 것이 아니라 **발급 시각 기준 남은 초**로 계산한다
- [X] T029 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 로그인(1.3)을 구현한다 — auth-pipeline.md §1의 8단계 순서 그대로: 회원 조회(없음 → `1003`, **이력 없음**) → bcrypt `matches`(불일치 → `1003` + 이력 `false`) → `active`(false → `1004` + 이력 `false`) → 활성 세션 폐기 → `session_id` 생성 → 토큰 2종 발급 → 세션 INSERT → 이력 `true`. **폐기와 삽입이 한 트랜잭션**이어야 한다(폐기만 되고 삽입이 실패하면 회원이 로그아웃된 채로 남는다). `ux_user_session_active` 위반은 **1회 짧게 재시도**하고 그래도 실패하면 `9000`으로 흘린다(research.md §10 — 애플리케이션 검사만으로는 동시 로그인 창을 닫을 수 없다). T025~T028에 의존
- [X] T030 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 토큰 검증(1.4)과 폐기(1.6)를 구현한다 — 1.4는 필터가 이미 통과시킨 요청이므로 `valid=true`와 남은 만료 시간을 돌려준다. 1.6은 현재 세션을 폐기하고, **이미 폐기된 세션의 재로그아웃은 `1006`으로 거절한다**(성공으로 흘리지 않는다 — auth-pipeline.md §4). T029와 같은 파일이라 순차
- [X] T031 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 토큰 갱신(1.5 Rotation)을 구현한다 — auth-pipeline.md §3의 7단계: `refreshToken` 누락 `9001` → 해시로 세션 조회(없음 `1005`) → 해시가 NULL·빈값 `1005`(**로그아웃한 경우다**) → 불일치 `1005` → `refresh_expires_at` 지남 `1005` → `active` false `1004` → **Access·Refresh를 둘 다 새로 발급**하고 같은 행의 해시 2개·만료 2개를 갱신한다. **`session_id`는 바꾸지 않는다** — 새 행을 만들면 부분 유니크 인덱스에 걸리고, 폐기 후 삽입하면 "같은 로그인 세션"이라는 의미가 끊긴다. 다른 곳에서 로그인해 세션이 교체된 경우만 `1006`이다. T030과 같은 파일이라 순차
- [X] T032 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/security/TokenAuthenticationFilter.java`를 만든다 — auth-pipeline.md §2의 **10단계를 순서 그대로** 옮긴다. 순서가 바뀌면 응답 코드가 바뀐다. 놓치기 쉬운 네 가지: ① 3단계(JWT `exp`)와 8단계(`access_expires_at`)를 **둘 다** 하고 어느 하나라도 지나면 `1001`, ② 5단계는 `sub`(memberId)를 `id_key`로 **환산**해야 조회 조건이 성립, ③ 6단계(해시 NULL 검사)를 7단계(해시 비교)보다 먼저, ④ 9단계 `active` 검사는 세션 폐기와 **중복 방어**다(관리자가 방금 정지한 회원의 토큰이 아직 살아 있을 수 있다). 통과하면 `AuthPrincipal`을 `SecurityContext`에 싣는다. **`permitAll` 경로에서도 이 필터는 돌지만 실패시키지 않는다** — 낡은 토큰을 들고 로그인하러 온 사용자가 막히면 안 된다. 10단계 `last_accessed_at` 갱신은 선택이라 넣지 않는다(어느 SC에도 쓰이지 않고 매 요청 UPDATE 비용만 든다). T025·T029에 의존
- [X] T033 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/config/SecurityConfig.java`에 `TokenAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록한다. T018·T032에 의존(T018과 같은 파일이라 순차)
- [X] T034 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/AuthController.java`를 만들고 1.3~1.6을 붙인다 — `POST /api/v1/auth/login`·`GET /api/v1/auth/validate`·`POST /api/v1/auth/refresh`·`POST /api/v1/auth/revoke`. **Controller는 Service만 부른다**(Repository 직접 호출 금지 — 헌장 원칙 II). try-catch로 응답을 제각각 만들지 않고 T010에 위임한다. 진입·종료 로그를 수동으로 쓰지 않는다(T012가 한다)
- [X] T035 [US1] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/HealthController.java`(1.1)가 `{ resCode: 200, data }`를 내는지 확인하고 T009로 정리된 `RestResponseDto` 팩토리를 쓰도록 맞춘다. 기존 `HealthControllerTest`가 계속 통과해야 한다 — **이 API는 `permitAll`이라 인증 도입 후에도 토큰 없이 200이어야 한다**

**Checkpoint**: 인증 기반이 독립적으로 동작한다. quickstart #1~#8·#31~#33·#37이 통과하고 MVP로 인도할 수 있다

---

## Phase 4: User Story 2 - 회원가입과 본인 정보 관리 (Priority: P2)

**Goal**: 가입(1.2)하고, 본인 정보를 보고(1.7) 고친다(1.8)

**Independent Test**: 가입 API로 계정을 만들고, 그 계정으로 로그인해 본인 정보를 조회·수정한 뒤 값이 바뀌었는지 다시 조회해 확인한다

### Tests for User Story 2

- [X] T036 [P] [US2] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/member/SignupIT.java` — #9(가입 시 `role=3` 고정, `pw`는 bcrypt 해시로만 저장)·#11(중복 아이디 `2002`)·#12(**이메일 없이 두 명 가입 → 둘 다 성공**. 부분 유니크 `ux_user_email`의 실제 동작을 확인하는 자리다)·#13(비밀번호 확인 불일치 `2005`). 비밀번호 규칙 위반 `2004`(8자 미만·2종류 이하)도 함께 본다
- [X] T037 [P] [US2] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/member/DefaultExpendGroupIT.java` — #10(가입 직후 그 회원의 지출유형이 **정확히 10건**, 전부 `defaultGroup=true`, 아이콘 파일명이 `{id_key}_{expendGroupId}.png` 형태 — SC-105). 이름 10종(식비·교통·주거·통신·쇼핑·장보기·의료·교육·문화·기타)이 다 있는지도 본다
- [X] T038 [P] [US2] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/member/MemberMeIT.java` — #14(본인 정보 조회 응답에 비밀번호가 **없다** — SC-107)·#15(수정에서 일부 필드를 omit하면 그 필드는 유지). omit과 명시적 `null`이 **다르게** 동작하는지도 확인한다 — `"email": null`은 값을 지우고 필드 부재는 유지다(api-contract.md §4)

### Implementation for User Story 2

- [X] T039 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/`에 `SignupRequest`·`MemberUpdateRequest`를 만들고 Bean Validation을 건다 — `memberId` 4~20자 영문·숫자·`_`(FR-102), `nickname` 2~20자, `password` 8자 이상 + 영문 대문자·소문자·숫자·특수문자 중 **3종류 이상**(FR-103, 커스텀 검증), `phone`은 하이픈 없이 숫자만(FR-107). **DB가 막아주지 않는 규칙들이다**(data-model.md §1 표) — 위반은 `2004`(비밀번호)·`9001`(그 밖)로 나간다
- [X] T040 [US2] PATCH omit 수신 방식을 **한 가지로 정하고** `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/`에 공통 유틸로 고정한다 — `JsonNullable` 래퍼 또는 `Map<String,Object>` + `containsKey` 중 하나(api-contract.md §4). 필드 부재 = 유지, 명시적 `null` = 값 지움. **이 구분이 없으면 "이메일을 지우는" 조작이 불가능해진다.** 1.8과 1.15가 같은 방식을 써야 하므로 US4보다 먼저 정한다. T039와 같은 패키지라 순차
- [X] T041 [P] [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/response/MemberResponse.java`를 만든다(본인 정보·관리자 상세 공용) — **`pw`를 필드로 두지 않는다.** 응답에 비밀번호가 없는 것을 DTO 구조가 보장한다(SC-107은 검사이지 방어가 아니다)
- [X] T042 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/mapper/MemberMapper.java`를 MapStruct로 만든다 — `User` Entity ↔ DTO 변환만 한다. **비즈니스 로직을 담지 않는다**(헌장). `pw` 매핑은 명시적으로 막는다. T041에 의존
- [X] T043 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/DefaultExpendGroupService.java`를 만든다 — 기본 지출유형 10종을 `tbl_user_expend_group`에 직접 넣는다(`003`의 API를 부르지 않는다 — research.md §11: 백엔드가 자기 자신에게 HTTP를 보내는 꼴이 되고 가입 트랜잭션과 갈린다). **저장 순서가 강제된다**: ① 행을 저장해 PK(`idx`)를 받고 → ② `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/{이름}.png`를 회원별 복사본 `{id_key}_{idx}.png`로 복사하고 → ③ `icon_filename`을 UPDATE. 순서를 뒤집으면 파일명을 정할 수 없다. `in_use=true`·`default_group=true`·`deleted=false`, 감사 컬럼은 **방금 만든 그 회원의 `id_key`**(관리자 추가여도 회원 본인 — research.md §6). DB에는 파일명만 저장하고 경로·Base URL은 저장하지 않는다
- [X] T044 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 가입(1.2)을 구현한다 — 아이디 중복 선검사 `2002`, 이메일 중복 선검사 `2003`(값이 있을 때만), 비밀번호 확인 불일치 `2005`, `role=3` 고정(요청이 지정할 수 없다 — FR-105), bcrypt 해시 저장. **선검사와 유니크 위반 처리를 양쪽 다 둔다** — 선검사만으로는 동시 가입 창을 닫지 못한다(data-model.md §1). 가입 트랜잭션 안에서 T043을 불러 10종을 만든다. 만든 `tbl_user` 행의 `created_by`/`updated_by`는 **`null`**이다(본인 가입 — 이 테이블만 두 컬럼이 nullable이다). T031·T039·T043에 의존(같은 파일이라 순차)
- [X] T045 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/AuthController.java`에 `POST /api/v1/auth/signup`(1.2)을 추가한다. T034와 같은 파일이라 순차
- [X] T046 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/MemberService.java`를 만든다 — 본인 조회(1.7)와 수정(1.8). **대상은 토큰의 회원으로 정해지며 요청이 지정할 수 없다**(FR-116). 수정은 omit 규칙(T040)을 따르고, 이메일을 바꾸면 중복 검사 `2003`, 비밀번호를 바꾸면 규칙 검증 `2004` + bcrypt 재해시 + **그 회원의 활성 세션 폐기**(FR-119). `user_id`는 `updatable=false`라 바뀌지 않는다. T040·T042에 의존
- [X] T047 [US2] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/MemberController.java`를 만들고 1.7·1.8을 붙인다 — `GET·PATCH /api/v1/members/me`. **`PUT`은 쓰지 않는다.** 경로에 회원 식별자를 두지 않는다(대상은 토큰이 정한다)

**Checkpoint**: US1과 US2가 각각 독립적으로 동작한다. 사람이 계정을 만들고 본인 정보를 관리할 수 있다

---

## Phase 5: User Story 3 - 아이디·비밀번호 찾기 (Priority: P3)

**Goal**: 로그인하지 못하는 사람이 아이디를 찾고(1.9), 비밀번호를 재설정한다(1.10·1.11)

**Independent Test**: 가입한 계정의 정보로 아이디를 찾고, 비밀번호 찾기를 거쳐 재설정한 뒤 새 비밀번호로 로그인되는지 확인한다

### Tests for User Story 3

- [X] T048 [P] [US3] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/recovery/FindIdIT.java` — #16(가입 이메일로 아이디 찾기 성공, 응답 `memberId`가 **원문과 다르고** `masked=true` — SC-109)·#17(없는 이메일 `2001`). 이메일 형식 오류가 `9001`인 것도 함께 본다(형식은 `9001`, 형식은 맞는데 회원이 없으면 `2001` — api-contract.md §5)
- [X] T049 [P] [US3] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/recovery/ResetPasswordIT.java` — #18(재설정 후 옛 비밀번호로 로그인하면 `1003`)·#19(재설정하면 기존 세션이 폐기되어 있다)·#20(**1.10을 건너뛰고 1.11만 직접 호출해도 판정이 같다**). #20이 "재설정 토큰을 두지 않는다"는 결정을 실제로 검증하는 자리다 — 상태를 들고 다니지 않으므로 단독 호출이 성공해야 한다

### Implementation for User Story 3

- [X] T050 [US3] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserRepository.java`에 `findByEmail(String)`을 추가한다. 기존 `existsByEmail`이 파생 쿼리를 피해 `@Query`로 술어를 고정한 것과 **같은 이유**로, 이메일이 `null`인 요청이 들어와도 회원을 찾지 못하게 한다(선택 항목이라 비어 있는 회원이 여럿이다)
- [X] T051 [P] [US3] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/`에 `FindIdRequest`(email)·`FindPasswordRequest`(memberId·nickname)·`ResetPasswordRequest`(memberId·nickname·newPassword·passwordConfirm)를, `dto/response/FindIdResponse.java`에 `maskedMemberId`·`masked`를 만든다
- [X] T052 [P] [US3] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/MemberIdMasker.java`에 아이디 마스킹 규칙을 만든다 — 예: 앞 3자 + `***` + 뒤 2자. **결과가 원문과 반드시 달라야 한다**(SC-109). 아이디가 4자로 짧아도 같아지지 않도록 짧은 길이의 처리를 명시한다. T011의 로그 마스킹(`***` 전체 대체)과 **목적이 다르므로 규칙을 공유하지 않는다**(api-contract.md §6)
- [X] T053 [US3] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 아이디 찾기(1.9)를 구현한다 — 이메일 형식 오류 `9001`, 일치 회원 없음 `2001`, 성공 시 마스킹한 아이디 + `masked=true`. **계정 존재 여부를 감추는 통일 응답은 쓰지 않는다**(스펙 Edge Case에서 확정). T044·T050·T052에 의존(같은 파일이라 순차)
- [X] T054 [US3] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AuthService.java`에 비밀번호 찾기(1.10)와 재설정(1.11)을 구현한다 — 본인 확인은 `memberId`+`nickname` 대조 **하나뿐**이고, **1.11이 같은 두 값을 다시 검증한다**(재설정 토큰·인증코드·만료 시간이 존재하지 않는다 — FR-124). 불일치 `2001`, 비활성 `1004`, 새 비밀번호 규칙 위반 `2004`, 확인 불일치 `2005`. 재설정 성공 시 bcrypt 재해시 + **그 회원의 활성 세션 폐기**(US3 시나리오 3). T053과 같은 파일이라 순차
- [X] T055 [US3] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/AuthController.java`에 1.9~1.11을 추가한다 — `POST /api/v1/auth/find-id`·`find-password`·`reset-password`. 셋 다 `permitAll`이다(FR-122). T045와 같은 파일이라 순차

**Checkpoint**: US1~US3이 각각 독립적으로 동작한다. 비밀번호를 잊은 사용자가 돌아올 수 있다

---

## Phase 6: User Story 4 - 관리자 회원 관리 (Priority: P4)

**Goal**: 관리자가 회원을 추가하고(1.12) 목록·상세를 보고(1.13·1.14) 수정하거나(1.15) 정지한다(1.16)

**Independent Test**: 일반 회원 토큰으로 관리자 API를 불러 `1002`로 막히는지 확인하고, 관리자 토큰으로는 통과하는지 확인한다

### Tests for User Story 4

- [ ] T056 [P] [US4] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/admin/AdminAuthorizationIT.java` — #21(일반 회원 토큰으로 관리자 API **5건 전부 `1002`** — SC-106)·#38(그 응답이 Spring 기본 403 JSON이 아니라 `{ resCode: 1002 }` 규격이다). T017을 실제로 검증하는 자리다
- [ ] T057 [P] [US4] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/admin/AdminMemberListIT.java` — #22(`data.list`가 object 배열이고 `offset`·`limit`·`totalCount`가 함께 오며 **`page`·`totalPages`가 없다**)·#23(`offset`이 `limit`의 배수가 아니면 `9001`)·#24(**`offset`·`limit` 누락도 `9001`** — 기본값으로 통과하지 않는다). #24가 `Pageable` 자동 바인딩을 쓰지 않기로 한 결정을 검증한다(research.md §12). `memberId`+`nickname` 검색이 AND로 묶이는 것과 `totalCount`가 페이지 건수가 아닌 전체 건수인 것도 본다
- [ ] T058 [P] [US4] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/admin/AdminMemberDeactivateIT.java` — #25(정지 후 그 회원 로그인 `1004`, **회원 행과 그 회원의 데이터는 남아 있다**)·#26(정지 시 그 회원 세션이 폐기됨)·#27(이미 정지된 회원 재정지 `9001`)·#28(관리자가 자기 계정 정지 → 거절 `9001`)
- [ ] T059 [P] [US4] `app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/admin/AdminMemberCreateIT.java` — #29(관리자가 만든 회원 행의 `created_by`가 **관리자의 `id_key`**. 본인 가입이 `null`인 것과 대비해 확인한다 — FR-121)·#30(관리자 추가 직후 그 회원의 지출유형도 **정확히 10건** — SC-105)

### Implementation for User Story 4

- [ ] T060 [US4] `data-mod/src/main/java/com/dbdomino/moneylog/data/repository/UserRepository.java`에 관리자 목록 조회를 추가한다 — `memberId`·`nickname` **부분 일치**(둘 다 주어지면 AND), `offset`/`limit` 페이징, 조건에 걸린 **전체 건수**를 세는 count 쿼리. Repository 층에서 `Pageable`을 인자로 받는 것은 무방하다 — 금지 대상은 **Controller의 자동 바인딩**이다(값이 빠졌을 때 기본값으로 조용히 통과해 `9001`이 나가지 않는다 — research.md §12). T050과 같은 파일이라 순차
- [ ] T061 [US4] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/AdminMemberListQuery.java`에 페이징 파라미터와 그 검증을 둔다 — `offset`·`limit` **필수**, `limit <= 0` · `offset < 0` · `offset % limit != 0` 중 하나라도 걸리면 `9001`(FR-120). 세 조건을 각각 테스트할 수 있도록 한 메서드로 모은다
- [ ] T062 [P] [US4] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/dto/request/`·`dto/response/`에 관리자 DTO를 만든다 — `AdminMemberCreateRequest`(`role` 값이 1·3이 아니면 `9001`)·`AdminMemberUpdateRequest`(omit 규칙은 T040과 **같은 방식**)·`AdminMemberListResponse`(`list`·`offset`·`limit`·`totalCount`). 상세 응답은 T041의 `MemberResponse`를 재사용한다
- [ ] T063 [US4] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AdminMemberService.java`에 회원 추가(1.12)·목록(1.13)·상세(1.14)·수정(1.15)을 구현한다 — 추가는 1.2와 **똑같이** 기본 지출유형 10종을 만들고(T043 재사용, FR-106), 만든·고친 `tbl_user` 행의 감사 컬럼에는 **관리자의 `id_key`** 가 들어간다(T019의 `BackendAuditorAware`가 자동으로 채운다). 없는 회원은 `2001`, 아이디 중복 `2002`, 이메일 중복 `2003`, 비밀번호 규칙 `2004`. T060~T062에 의존
- [ ] T064 [US4] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/service/AdminMemberService.java`에 회원 정지(1.16)를 구현한다 — `active=false`로 **표시만** 하고 행·데이터를 지우지 않는다(FR-118). 그 회원의 활성 세션을 폐기한다(FR-119). **이미 `active=false`인 회원의 재정지는 `9001`**(성공으로 흘리지 않는다), **관리자가 자기 계정을 정지하는 것도 `9001`** — 설계 명세의 실패 표에 자기 정지 코드가 배정되어 있지 않아 api-contract.md §7에서 `9001`로 정했다(둘 다 "이 정지 요청은 성립하지 않는다"는 같은 성격이고 프론트의 조치도 같다). 마지막 관리자가 스스로를 잠그면 되살릴 방법이 없다(재활성화 API는 이 기능의 범위 밖이다). T063과 같은 파일이라 순차
- [ ] T065 [US4] `app-mod/money-backend-app/src/main/java/com/dbdomino/moneylog/backend/controller/AdminMemberController.java`를 만들고 1.12~1.16을 붙인다 — `POST·GET /api/v1/admin/members`, `GET·PATCH /api/v1/admin/members/{memberId}`, `PATCH /api/v1/admin/members/{memberId}/deactivate`. 인가는 T018의 `SecurityFilterChain`이 `/api/v1/admin/**`에 걸어 둔 것을 쓰고 **Controller에 권한 애너테이션을 흩지 않는다**. 정지가 `PATCH`인 것에 유의한다(`PUT` 금지)

**Checkpoint**: 네 스토리 전부가 독립적으로 동작한다. API 16건이 모두 서 있다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 임시 조치 제거와 전체 규격 검증. quickstart.md §3의 "회귀"·"응답 규격" 절과 §4 완료 판정에 대응한다

- [ ] T066 `data-mod/src/main/java/com/dbdomino/moneylog/data/entity/BaseAuditEntity.java`에서 임시 `@Setter`와 그 TODO 주석 블록을 제거한다(quickstart #40). 감사 값은 `AuditingEntityListener`만 채워야 하며, 공개 세터가 열려 있으면 `created_by`를 임의로 덮어쓸 수 있어 감사 기록의 신뢰도가 떨어진다(`@Column(updatable=false)`는 UPDATE만 막고 INSERT 시점 위조는 못 막는다). T019가 먼저 끝나 있어야 한다
- [ ] T067 `data-mod/src/test/java/com/dbdomino/moneylog/data/schema/AbstractSchemaIT.java`의 `stampAudit()`과 그 호출부를 전부 제거한다(quickstart #41) — `newExpendGroup`·`newPaymentMethod` 등 헬퍼 여러 곳에서 부른다. T019에서 넣은 테스트용 `AuditorAware`가 값을 공급하므로 더는 직접 채울 필요가 없다. T066과 한 묶음이다(세터가 사라지면 컴파일이 깨진다)
- [ ] T068 `./gradlew :data-mod:test`를 돌려 기존 스키마 IT(quickstart 기준 **77건**)가 전부 통과하는지 확인한다(quickstart #39). 통과해야 `AuditorAware` 실 구현이 제 역할을 한다는 뜻이다. **`./gradlew test`(전체)는 쓰지 않는다** — `money-app`의 레거시 테스트 3건이 `init` 커밋부터 깨져 있다(테스트용 `application.yml`이 메인 설정을 덮어쓰는데 datasource가 없다)
- [ ] T069 로그 마스킹을 **실측**한다(quickstart #34) — 로그인·갱신을 호출한 뒤 `logback-spring.xml`이 만든 로그 파일에서 `password`·`accessToken`·`refreshToken`·`Authorization` 값이 `***`인지 확인한다. 단위 테스트로는 Aspect가 실제 요청 경로에 걸렸는지까지 보이지 않는다
- [ ] T070 응답 규격을 전수 확인한다(quickstart #35~#38) — 16건 전부 성공은 HTTP 200 + `resCode 200`, 비즈니스·검증 실패는 **HTTP 200** + 4자리 코드, 서버 오류만 HTTP 500 + `9000`(SC-101). 목록(1.13)만 `data.list` 형태이고 `page`·`totalPages`가 없다
- [ ] T071 `git diff sql/schema-moneylogdb.sql`이 **비어 있는지** 확인한다 — 이 기능은 스키마를 바꾸지 않는다. 덤프가 바뀌었다면 의도치 않게 Entity를 건드린 것이므로 원인을 찾는다(원칙 VI의 이 기능판이다). 덤프 파일을 손으로 편집하지 않는다
- [ ] T072 `./gradlew :app-mod:money-backend-app:test`로 이 기능의 테스트를 전부 돌리고, quickstart.md §3의 시나리오 **41건**이 모두 대응되는지 `#N` 번호로 대조한다. 빠진 번호가 있으면 그 시나리오가 검증되지 않은 것이다
- [ ] T073 커밋 전 자가 점검(CLAUDE.md) — ① 응답이 `{ resCode, data }`인가, ② **Entity가 API·화면에 노출되지 않는가**(Controller 시그니처와 응답 DTO를 훑는다), ③ 명세 표의 설명 칸이 비어 있지 않은가, ④ DB 구조가 바뀌지 않았는가(T071), ⑤ `System.out.println`이 없는가, ⑥ Controller가 Repository를 직접 부르지 않는가, ⑦ `PUT`이 0건인가

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1, T001~T006)**: 의존 없음. **단 T001(명세 선행 개정)은 코드보다 먼저** 한다(헌장 원칙 V)
- **Foundational (Phase 2, T007~T020 + T074)**: Setup 완료 후. **모든 User Story를 막는다**. T074는 ID만 뒤에 붙었을 뿐 실행 위치는 T012 다음이다
- **US1 (Phase 3, T021~T035)**: Foundational 완료 후. 다른 스토리에 의존하지 않는다
- **US2 (Phase 4, T036~T047)**: Foundational 완료 후. 본인 정보 API(1.7·1.8)를 **호출**하려면 US1의 토큰이 필요하지만, 가입(1.2) 자체는 US1 없이 동작한다
- **US3 (Phase 5, T048~T055)**: Foundational 완료 후. 검증 시 US2의 가입으로 계정을 만드는 것이 편하지만, 회원을 DB에 직접 넣으면 US2 없이도 완결된다
- **US4 (Phase 6, T056~T065)**: Foundational 완료 후. 인가 검증(#21)에 US1의 토큰이 필요하다
- **Polish (Phase 7, T066~T073)**: 인도하려는 스토리가 전부 끝난 뒤

### 파일 충돌로 순차가 강제되는 것

`[P]`가 붙지 않은 이유는 대부분 이것이다.

| 파일 | 순차로 묶이는 작업 |
|---|---|
| 루트 `build.gradle` | T002 단독 |
| `MoneyBackendApplication.java` | T074 단독 (T010·T012 뒤) |
| `SecurityConfig.java` | T018 → T033 |
| `AuthService.java` | T029 → T030 → T031 → T044 → T053 → T054 |
| `AuthController.java` | T034 → T045 → T055 |
| `UserRepository.java` | T050 → T060 |
| `AdminMemberService.java` | T063 → T064 |
| `BaseAuditEntity`·`AbstractSchemaIT` | T019 → T066 → T067 → T068 |

### Within Each User Story

- 테스트를 먼저 쓰고 **실패를 확인한 뒤** 구현한다
- Repository → Service → DTO → Controller 순
- 스토리를 끝내고 다음 우선순위로 넘어간다

### Parallel Opportunities

- Setup의 T003~T006 4건은 서로 다른 파일이라 동시에 가능하다(T001·T002가 먼저)
- Foundational에서 T011·T013·T014·T015·T016·T017 6건이 동시에 가능하다
- 각 스토리의 테스트 작성은 전부 `[P]`다 — US1 3건, US2 3건, US3 2건, US4 4건
- Foundational이 끝나면 **US1~US4를 서로 다른 사람이 동시에** 진행할 수 있다. 다만 `AuthController`·`AuthService`를 US1·US2·US3이 공유하므로 그 셋은 한 사람이 맡는 편이 충돌이 적다

---

## Parallel Example: User Story 1

```bash
# US1의 통합 테스트 3건을 함께 작성한다:
Task: "TokenLifecycleIT — quickstart #1·#3·#5·#8"
Task: "SessionSingleActiveIT — #2·#4·#6"
Task: "LoginFailureAndHistoryIT — #7·#31·#32·#33"
```

```bash
# Foundational 의 독립 파일 6건을 함께 만든다:
Task: "SensitiveMasker (common-mod/logging)"
Task: "JwtProperties (backend/config)"
Task: "JwtTokenProvider (common-mod/security)"
Task: "AuthPrincipal (backend/security)"
Task: "RestAuthEntryPoint (backend/security)"
Task: "RestAccessDeniedHandler (backend/security)"
```

---

## Implementation Strategy

### MVP First (User Story 1만)

1. Phase 1 Setup — 명세 개정과 의존성
2. Phase 2 Foundational — **전 스토리를 막는 단계다**
3. Phase 3 US1 — 인증 기반
4. **여기서 멈추고 검증한다**: quickstart #1~#8·#31~#33·#37이 통과하면 인증이 선 것이다
5. 이 시점의 가치: 나머지 53건의 API가 기댈 "권한 = 로그인"이 실제로 동작한다

### Incremental Delivery

1. Setup + Foundational → 응답 규격·에러코드·로깅·인가 경계가 선다
2. US1 → 검증 → **MVP 인도**
3. US2 → 가입과 본인 정보 → 인도
4. US3 → 찾기·재설정 → 인도
5. US4 → 관리자 회원 관리 → 인도
6. Polish → 임시 조치 제거와 전수 검증

### Parallel Team Strategy

1. Setup + Foundational을 함께 끝낸다
2. 그 뒤:
   - 개발자 A: US1 → US2 → US3 (`AuthController`·`AuthService`를 공유하므로 한 사람이 맡는다)
   - 개발자 B: US4 (파일이 겹치지 않는다. 다만 인가 검증에 US1의 토큰이 필요하므로 US1 완료를 기다린다)
3. Polish는 다시 함께

---

## Notes

- `[P]` = 다른 파일, 미완료 작업에 의존하지 않음
- `[Story]` 라벨로 작업과 User Story를 잇는다
- 구현 전에 테스트가 **실패하는 것**을 확인한다
- 작업 단위 또는 논리적 묶음마다 커밋한다
- 각 Checkpoint에서 멈춰 스토리를 독립적으로 검증할 수 있다
- **이 기능에서 스키마는 바뀌지 않는다.** Entity를 고치고 싶어지면 그 자체가 신호다 — 먼저 왜 필요한지 확인한다
