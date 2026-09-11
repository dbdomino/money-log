---
description: "Task list for 007-frontend-foundation"
---

# Tasks: 프론트 공통 기반 — 화면 모듈 재구성

**Input**: `/specs/007-frontend-foundation/` 의 설계 산출물

**Prerequisites**: plan.md · spec.md · research.md · data-model.md · contracts/ (api-client.md · session-auth.md · screen-shell.md · url-map.md) · quickstart.md

**Tests**: 포함한다. quickstart.md §3 이 US1~US4 의 검증 항목을 표로 매기고 §4 가 그것을 완료 판정으로 쓴다. 그리고 **깨져 있던 레거시 시험을 정리해 전 모듈 테스트를 통과시키는 것 자체가 요구사항이다**(FR-608·SC-603)

**Organization**: User Story 단위로 묶어 각 스토리를 독립적으로 구현·검증·인도할 수 있게 한다

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완 작업에 의존하지 않음)
- **[Story]**: 이 작업이 속한 User Story (US1~US4)
- 설명에 정확한 파일 경로를 적는다

## Path Conventions

Spring Boot 멀티모듈이다. 저장소 루트 기준 경로를 쓴다.

- 프론트 메인(신규): `app-mod/money-app/src/main/java/com/dbdomino/moneylog/front/`
- 프론트 메인(레거시, 삭제 대상): `app-mod/money-app/src/main/java/com/dbdomino/moneylog/`
- 프론트 테스트: `app-mod/money-app/src/test/java/com/dbdomino/moneylog/front/`
- 프론트 자원: `app-mod/money-app/src/main/resources/`
- 화면기획 산출물(원본): `프로젝트설계/화면기획/`

**모듈별 `build.gradle` 이 없다.** 의존성은 루트 `build.gradle` 의 `project(':app-mod:money-app')` 블록에서 고친다.

**`sql/schema-moneylogdb.sql` 은 변경 없음이 이 기능의 전제다.** 화면 모듈이 DB 를 떠나는 작업이라 스키마 영향도가 구조적으로 0 이다(원칙 VI).

## 이 기능은 뺄셈이 먼저다

002~006 은 없던 것을 더했지만 007 은 **있던 것을 걷어내고** 통로를 놓는다. 그래서 Phase 2 에서
의존성과 레거시를 **먼저** 지운다 — 통로를 먼저 만들고 나중에 지우면 두 경로가 공존하는 구간이
생겨 "화면이 어느 쪽으로 데이터를 얻는지" 알 수 없다. `data-mod` 의존을 먼저 끊으면
**컴파일러가 레거시를 전부 짚어 준다.**

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 착수 전 문서 확정. **T001~T004 는 코드보다 반드시 먼저** 한다(헌장 원칙 V)

- [X] T001 [P] `프로젝트설계/기능명세상세-프론트엔드/phase1-회원/1.6-ErrorForbidden.md` 메타의 **레이아웃을 `main` 에서 `auth` 로** 고친다 — `화면기획/01-화면구성.md` 의 표와 `화면기획/proto/error-forbidden.html`(`class="layout-auth"`) 이 둘 다 auth 다. 1.6 은 **미로그인도 들어오는 화면**이라 로그인 후 사이드바를 붙일 수 없다. 그대로 두면 구현자가 main 레이아웃을 붙여 **사이드바가 빈 채로 뜨는 화면**을 만든다
- [X] T002 같은 문서의 「화면 구성」·「동작」·「검증·안내」 세 표에서 **`(작성 예정)` 을 없앤다** — 007 범위의 화면이 1.6 하나다. 영역(제목·안내 문구·버튼 둘), 동작(「가계부로」→`/ledger`, 「로그인」→`/auth/login`), 검증(호출 API 없음·미로그인 접근 가능)을 적는다. `grep -n "작성 예정"` 이 **0건**이 되어야 한다(quickstart §0)
- [X] T003 [P] `프로젝트설계/기능명세상세-프론트엔드/_공통.md` § 표시방식의 구 URL redirect 문장을 [contracts/url-map.md](./contracts/url-map.md) 기준으로 고친다 — 현재 예시가 **실재한 적 없는 주소**(`/payments/new`·`/ledger/expenses/new`)뿐이라 FR-634 의 대상이 무엇인지 문서만 보고 알 수 없다. 실재 레거시(`/mem/login`·`/mem/ind`·`/api/ammounts/**`)의 처리를 함께 적는다
- [X] T004 [P] 같은 문서 § 인증·토큰 의 「세션 만료」 행에 **`server.servlet.session.timeout` 값(30분)** 을 적는다 — 명세가 값을 비워 둔 탓에 현재 설정 **60초**(단위 없는 `60`)가 "의도된 값"으로 읽혔다. Access 토큰이 1일인데 화면 세션이 1분이면 FR-623 이 "1분마다 재로그인"으로 관측되고 재발급 흐름(FR-615)은 실제로 일어나지 않는다
- [X] T005 착수 전 상태를 기록한다(quickstart §1) — `sed -n '/money-app/,/^}/p' build.gradle` 의 의존 6건, `grep -rn "data-mod\|jakarta.persistence\|mybatis\|@Entity" app-mod/money-app/src/main/java | wc -l` 의 건수, `ls app-mod/money-app/src/main/resources/logback*.xml`. 이 값들이 완료 판정(SC-602)의 "전" 값이다

**Checkpoint**: 명세가 서고, 걷어낼 대상의 규모가 숫자로 확인된다

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 레거시를 걷어내고 최소 기동 상태를 만든다. **여기가 끝나기 전에는 어떤 스토리도 시작할 수 없다** — 이 단계가 끝나면 `money-app` 은 화면이 하나도 없지만 DB 없이 뜬다

**⚠️ 순서가 중요하다**: T006(의존 제거) → T007~T009(레거시 삭제) 순서를 지킨다. 반대로 하면 무엇이 남았는지 사람이 세야 한다

- [X] T006 루트 `build.gradle` 의 `project(':app-mod:money-app')` 블록에서 의존 **6건을 뺀다** — `project(':core-mod')` · `project(':data-mod')` · `spring-boot-starter-data-jpa` · `mybatis-spring-boot-starter` · `org.postgresql:postgresql` · `spring-boot-starter-aspectj`. 남기는 것은 `common-mod`(에러코드 Enum) · `starter-web` · `starter-thymeleaf` · `starter-validation` · `devtools`. **이 한 줄이 FR-601·602 를 동시에 이룬다** — 의존이 없으면 DB 로 가는 우회로를 남기려야 남길 수 없다. **`core-mod` 도 뺀다** — spec FR-601 은 적지 않았지만 원칙 I 의 허용 목록은 `money-app → common-mod` 뿐이다(plan.md Complexity Tracking). 시험에 `testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'` 를 더한다 — Boot 4 에서 `starter-test` 가 `@AutoConfigureMockMvc` 를 더는 끌어오지 않는다
- [X] T007 `app-mod/money-app/src/main/java/com/dbdomino/moneylog/` 의 레거시 자바 **53개를 지운다** — `entity/`(**12** — 11 + `superclass/CuDate`) · `repository/`(**6**, `MemberRepository` 가 `repository/` 와 `repository/itf/` 에 둘 있다) · `mapper/`(9) · `dto/`(**13** — 루트 9 + `response/` 2 + `service/` 2) · `service/`(2) · `controller/`(2) · `domain/`(3) · `common/{aop,constants,exception,util}/`(5) · `VanillaApplication.java`(1). 합 **53**. **`common/` 을 반드시 지운다** — `com.dbdomino.moneylog.common` 은 `common-mod` 와 **같은 패키지 이름**이라 split package 다(research 13)
- [X] T008 프론트 시험을 정리한다(FR-608) — `app-mod/money-app/src/test/resources/application.yml` · `src/test/java/com/dbdomino/moneylog/VanillaApplicationTests.java` · `.../service/MemberServiceTest.java` 를 지운다. **`test/resources/application.yml` 이 원인이다**: 메인 설정을 덮어쓰는데 `profiles.active` 도 없고 H2 datasource 줄이 전부 주석이라 `Failed to determine a suitable driver class` 가 난다. `init` 커밋부터 깨져 있었다
- [X] T009 `app-mod/money-app/src/main/java/com/dbdomino/moneylog/front/MoneyAppApplication.java` 를 만든다 — `@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog.front")`. **범위를 좁히는 것이 요점이다.** 지금은 `com.dbdomino.moneylog` 전체를 스캔해 `common-mod` 의 `GlobalExceptionHandler`(`@RestControllerAdvice`)와 `ApiLoggingAspect` 가 후보로 들어오고, `moneylog.common.web.enabled` 프로퍼티 하나로만 막혀 있다. `@RestControllerAdvice` 가 프론트에 붙으면 **화면 오류가 JSON 으로 나간다**
- [X] T010 `app-mod/money-app/src/main/resources/application.yml` 을 다시 쓴다 — `spring.profiles.active: postgresql` **삭제**(DB 프로필을 타지 않는다), JPA·MyBatis·Hibernate 로그 설정 삭제, `spring.application.name: money-app`, `server.servlet.session.timeout: 30m`(현재 `60`=60초), multipart 설정 **유지**(009 아이콘·010 엑셀 업로드가 폼으로 들어온다), `server.error.whitelabel.enabled: false` 유지. `moneylog.backend.base-url: http://localhost:8081/api/v1` · `connect-timeout: 3s` · `read-timeout: 10s` 를 더한다(api-client.md §8)
- [X] T011 [P] `app-mod/money-app/src/main/resources/application-h2.yml` · `application-mysql.yml` 을 지운다 — 쓰지 않는 DB 프로필이고, 남겨 두면 "DB 를 다시 붙일 수 있다"는 오해를 남긴다
- [X] T012 [P] `logback.xml` 을 **`logback-spring.xml` 로 옮기고** 전체를 감싼 `<springProfile name="default">` 를 **없앤다** — 지금 프론트 로깅은 **조용히 죽어 있다.** ① `<springProfile>` 은 Spring 확장 태그라 `logback.xml` 에서는 해석되지 않고(Spring 개입 전에 로드된다) ② 앱이 `postgresql` 프로필로 떠서 조건도 맞지 않는다. **어느 쪽이든 appender 가 하나도 붙지 않고 오류도 나지 않는다.** `app-mod/money-backend-app/src/main/resources/logback-spring.xml` 의 골격(STDOUT + RollingFileAppender, `LOG_DIR=logs`)을 그대로 가져온다(원칙 IV)
- [X] T013 [P] 레거시 템플릿·정적 자원을 지운다 — `templates/{home,index,signin}.html` · `templates/page/signupsample.html` · `templates/target/default.html` · `templates/layout/base.html` · `static/css/bootstrap.css` · `static/css/bootstrap.rtl.css` · `static/js/bootstrap.js`. `templates/error.html` 은 **남긴다**(US1 의 T027 이 고쳐 쓴다). `templates/` 아래 화면별 빈 디렉터리 10개는 그대로 둔다 — 008~012 가 채운다
- [X] T014 `./gradlew :app-mod:money-app:compileJava` 와 `bootRun` 으로 **DB 없이 뜨는지** 본다 — datasource 관련 로그가 한 줄도 없어야 한다. 화면은 아직 하나도 없다(404 가 정상). `logs/money-app.log` 파일이 생기는지도 함께 본다(T012 검증)

**Checkpoint**: `money-app` 이 `common-mod` 만 의존하고 DB 없이 뜬다. SC-601·SC-602 의 조건이 이 시점에 성립한다

---

## Phase 3: User Story 1 - 화면이 DB 대신 백엔드 API 를 본다 (Priority: P1) 🎯 MVP

**Goal**: 화면 모듈이 데이터를 백엔드 API 로만 얻는다. `{ resCode, data }` 봉투를 **한 곳에서** 풀어 값 또는 실패 사유를 화면으로 넘긴다

**Independent Test**: DB 를 내린 채 기동해 정상인지 보고, `MockRestServiceServer` 로 성공 봉투·실패 봉투·바이너리·연결 실패 넷을 세워 각각의 결과를 확인한다. **화면 없이 완결된다**

### Tests for User Story 1

- [X] T015 [P] [US1] `app-mod/money-app/src/test/java/com/dbdomino/moneylog/front/MoneyAppApplicationTests.java` — `@SpringBootTest` 로 컨텍스트가 뜨는지 본다. **datasource 없이 뜨면 DB 를 모른다는 뜻**이라 이 시험 하나가 SC-601 을 대신한다(PostgreSQL 을 실제로 내리지 않아도 된다)
- [X] T016 [P] [US1] `.../front/client/BackendApiClientTest.java` 를 만든다 — `MockRestServiceServer.bindTo(RestClient.builder())` 로 응답을 세운다. 다섯 갈래: ① `{"resCode":200,"data":{...}}` → 값 반환 ② `{"resCode":3003,"data":{"message":"..."}}` → `BackendApiException(3003)` ③ 연결 거부 → `BackendUnavailableException` ④ `<html>...` (봉투 아님) → `BackendUnavailableException` ⑤ `getBinary` 에 `image/png` → `BinaryPayload`. **②는 코드가 `3003` **그대로**여야 한다**(SC-609)
- [X] T017 [P] [US1] `.../front/web/PagingTest.java` — `page × limit` 환산, `limit` 변경 시 `page` 0 복귀, 총 페이지 수 `ceil(totalCount / limit)`. **`offset` 을 직접 받는 생성자가 없다는 것**도 시험한다(있으면 배수가 아닌 값이 만들어질 수 있다)

### Implementation for User Story 1

- [X] T018 [P] [US1] `.../front/client/BackendApiProperties.java` — `@ConfigurationProperties("moneylog.backend")`. `baseUrl` · `connectTimeout` · `readTimeout`. **주소를 코드에 적지 않는다**
- [X] T019 [P] [US1] `.../front/client/ApiEnvelope.java` — `{ resCode, data }` **역직렬화 전용** 타입. **`common-mod` 의 `RestResponseDto` 를 쓰지 않는다**: 생성자가 private 이고 정적 팩터리로만 만들어져 `@JsonCreator` 도 없다 — Jackson 이 인스턴스를 만들 방법이 없다(research 2). 그 클래스는 백엔드가 **응답을 만들 때**의 것이고 프론트는 반대 방향이다
- [X] T020 [P] [US1] `.../front/client/BackendApiException.java` · `BackendUnavailableException.java` — 앞의 것은 `resCode` + `message` 를 **그대로** 들고 올라가고, 뒤의 것은 **코드가 없다**. 연결 실패에 `9000` 을 빌려 쓰지 않는다: "백엔드가 `9000` 을 줬다"와 "백엔드에 닿지 못했다"가 구분되지 않으면 사용자 안내가 갈리지 않는다(FR-606·607)
- [X] T021 [P] [US1] `.../front/client/BinaryPayload.java` — 바이트 · Content-Type · 파일명. 아이콘(003 의 2.10)과 엑셀 양식(004 의 3.11) 두 API 가 쓴다
- [X] T022 [US1] `.../front/client/BackendApiClient.java` 의 골격 — `RestClient` 빈 구성(T018 의 base-url·타임아웃)과 `get` · `post` · `patch` · `delete` · `getBinary`. **`put` 메서드를 두지 않는다**(FR-609): 규칙을 검증으로 세는 것보다 **부를 수 없게 하는 것**이 짧다. GET 은 Path 또는 Query 중 하나만 받는 시그니처로 나눈다(혼용 금지). 예외는 `StatisticsMonthlyGet` 하나이며 그 호출만 Path+Query 를 함께 받는 전용 메서드를 쓴다
- [X] T023 [US1] T022 에 **봉투 해석**을 넣는다(FR-603·604) — `resCode == 200` 이면 `data` 를 요청 타입으로 변환해 반환하고, 아니면 `BackendApiException` 을 **던진다**. 반환값으로 실패를 돌려주지 않는 이유는 **확인을 빠뜨린 코드도 컴파일되기 때문**이다. 봉투를 푸는 자리는 이 메서드 하나뿐이며 008~012 는 `resCode` 를 보지 않는다
- [X] T024 [US1] T022 에 **봉투 아닌 응답과 연결 실패**를 넣는다(FR-606) — `ResourceAccessException`(연결 거부·타임아웃)과 파싱 실패를 `BackendUnavailableException` 으로 묶는다. spec Edge Cases 의 "백엔드가 봉투가 아닌 것을 돌려줄 때 → 실패로 본다"가 이 갈래다
- [X] T025 [US1] T022 의 `getBinary` 를 완성한다(FR-605) — **Content-Type 을 보고 자동으로 갈라지지 않는다.** 호출부가 `getBinary` 를 부르는 것이 곧 "이건 바이너리다"라는 선언이다. 자동 판정은 두 방향으로 틀린다: 아이콘 요청에 **실패 봉투**가 오면 JSON 이라 봉투로 읽히는데 호출부는 바이트를 기다리고 있고, 반대 경우도 조용히 바이트가 된다. **바이너리 호출에 봉투가 오면 그것은 실패다** — `BackendApiException` 으로 올린다
- [X] T026 [US1] `.../front/web/Paging.java` — `page`(0-based)와 `limit` 을 받아 `offset = page × limit` 을 만든다. **`offset` 을 받는 생성자를 두지 않는다**(FR-610): `offset` 이 `limit` 의 배수가 아니면 목록이 통째로 `9001` 로 실패하는데, **만들 수 없게** 하는 편이 검증보다 짧다. `limit` 이 바뀌면 `page` 를 0 으로 되돌린다. 총 페이지 수 `ceil(totalCount / limit)` 도 여기서 낸다
- [X] T027 [US1] `.../front/web/FrontExceptionHandler.java`(`@ControllerAdvice`)와 `templates/error.html` — 두 예외를 받아 화면으로 보낸다(FR-612). `BackendApiException` 은 `resCode` + `message` 를, `BackendUnavailableException` 은 "서버에 닿지 못했습니다"를 넘긴다. **컨트롤러는 try-catch 를 쓰지 않는다.** 목록·상세를 **빈 값으로 그리지 않는다**(SC-608) — 데이터를 얻지 못했다는 사실이 글로 보여야 한다. `@RestControllerAdvice` 가 아니라 `@ControllerAdvice` 다(프론트는 뷰를 돌려준다). **`error.html` 의 레이아웃은 `auth` 다**(screen-shell.md §6) — main 을 쓰면 사이드바가 `role` 을 필요로 하는데, 세션이 없거나 깨진 상태에서도 떠야 하는 화면이다
- [X] T028 [US1] T022 에 **호출 로그와 마스킹**을 넣는다(원칙 IV·FR-622) — method · URI · 응답 `resCode` · 소요 시간 · 실패 시 message 를 남긴다. **AOP 를 쓰지 않는다**: 바깥으로 나가는 지점이 이 클래스 하나뿐이라 애스펙트를 놓을 이유가 없다(research 11, T006 에서 `starter-aspectj` 를 뺀 근거). `Authorization` 헤더 값·로그인/재발급의 토큰 필드·비밀번호는 **통째로 가린다**
- [X] T029 [US1] `.../front/web/IconProxyController.java` — `GET /expend-groups/icons/{filename}` 이 백엔드 `ExpendGroupIconGet` 을 대신 불러 **바이트 그대로** 내보낸다(FR-611). 브라우저가 `:8081` 을 직접 부르지 않기로 한 결정의 결과다. 파일명은 백엔드 규칙(`{id_key}_{expendGroupId}.{확장자}`)에 맞는지 확인해 **경로 이탈(`..`·`/`)을 막는다**. 백엔드가 실패 봉투를 주면 이미지를 내보내지 않고 **404** 로 끝낸다(모델·캐시에 담지 않는다 — data-model.md § 아이콘)

**Checkpoint**: 백엔드 API 를 부르고 성공·실패·바이너리·불통 넷을 구분할 수 있다. 화면은 아직 없다

---

## Phase 4: User Story 2 - 로그인 상태가 유지되고 권한이 지켜진다 (Priority: P1)

**Goal**: 로그인 상태가 서버 세션에 남아 이어지는 화면에서 유지되고, 로그인·권한이 없으면 해당 화면에 들어갈 수 없다

**Independent Test**: 세션에 토큰을 **직접 심은** 상태와 비운 상태로 보호 URL 에 들어가 결과가 갈리는지 본다. 로그인 **화면**(008) 없이 완결된다 — 005·006 이 없던 API 대신 데이터를 심어 시험한 것과 같은 방식이다

**⚠️ redirect 대상이 아직 없다**: `/auth/login`(008)·`/ledger`(010) 는 007 시점에 화면이 없다. **시험은 `Location` 헤더만 확인하고 렌더링 여부는 보지 않는다**

### Tests for User Story 2

- [X] T030 [P] [US2] `.../front/web/AuthInterceptorTest.java` — 네 갈래를 건다: ① 세션 빔 + 보호 URL → `Location: /auth/login`(SC-604) ② 세션 있음 + `validate` 200 → 통과 ③ `role=3` + `/admin/**` → `Location: /error/forbidden`(SC-605) ④ **세션 빔** + `/admin/**` → `Location: /auth/login`(**forbidden 이 아니다**, FR-620). ④가 이 시험의 핵심이다 — 순서가 뒤집히면 미로그인 사용자에게 그 URL 의 실재가 드러난다
- [X] T031 [P] [US2] `.../front/session/TokenRefreshTest.java` — 다섯 갈래: ① `1001` → 재발급 200 → 원래 요청 재시도 성공(SC-606) ② 재발급 응답의 **새 `refreshToken` 이 세션에 반영됨**(research 7) ③ 재시도 후 다시 `1001` → `/auth/refresh` 호출이 **1회뿐**(FR-617) ④ 처음부터 `1006` → 재발급 **시도 없이** 세션 무효화(FR-616) ⑤ 재발급이 `1005` → 세션 무효화. ③은 `MockRestServiceServer` 의 **호출 횟수**로 센다
- [X] T032 [P] [US2] `.../front/session/LoginSessionTest.java` — 속성 넷(`accessToken`·`refreshToken`·`memberId`·`role`)의 저장·조회·무효화. **JWT 를 파싱해 `memberId`·`role` 을 꺼내지 않는다**는 것도 확인한다(파싱하면 서명 검증 책임이 따라온다 — data-model.md)

### Implementation for User Story 2

- [X] T033 [P] [US2] `.../front/session/SessionUser.java` — `memberId`(로그인 아이디) · `role`(`1` 관리자 · `3` 일반). 백엔드 `tbl_user.id_key` 가 아니라 **사용자가 입력한 아이디**다
- [X] T034 [US2] `.../front/session/LoginSession.java` — 세션 속성을 읽고 쓰는 **유일한 자리**(FR-613). 컨트롤러·인터셉터·클라이언트가 `session.getAttribute("accessToken")` 을 직접 부르지 않는다: 속성 이름이 흩어지면 오타 하나가 "로그인이 안 풀리는" 증상으로 나타난다. 담는 것과 담지 않는 것의 목록은 data-model.md § 로그인 세션 — **만료 시각(`expiresIn`)을 담지 않는다**(프론트가 만료를 미리 판정하지 않는다)
- [X] T035 [US2] `.../front/session/TokenRefresher.java` — `MemberTokenRefresh`(`POST /auth/refresh`, Body `refreshToken`)를 부르고 성공 시 세션의 `accessToken` **과 `refreshToken` 을 둘 다** 덮어쓴다. **백엔드가 Rotation 이라 새 Refresh 를 함께 준다**: Access 만 갱신하면 세션에 폐기된 Refresh 가 남아 다음 재발급이 `1005` 로 실패하는데, 증상이 "한 번은 되고 두 번째부터 안 되는 로그인 연장"이라 원인을 찾기 어렵다
- [X] T036 [US2] T022 의 `BackendApiClient` 에 **인증 헤더와 재발급**을 잇는다(FR-614·615·617) — 매 호출에 세션의 `accessToken` 을 `Authorization: Bearer` 로 붙이고, `1001` 이면 T035 로 **한 번** 재발급한 뒤 원래 요청을 다시 보낸다. 재시도 여부는 **호출 지역 플래그**로 잠근다. 인터셉터가 아니라 여기 두는 이유는 진입 이후에 만료된 토큰을 받아낼 자리가 필요하기 때문이다(research 6). 비로그인 API(`MemberLogin`·`MemberSignup`·`MemberTokenRefresh`)는 헤더를 붙이지 않고 재발급 흐름도 타지 않는다
- [X] T037 [US2] T036 에 `1006` 갈래를 넣는다(FR-616) — **재발급을 시도하지 않고** 세션을 무효화한 뒤 `/auth/login` 으로 보낸다. 재발급 실패(`1005`·`1004`)도 결과는 같다. 사용자 문구는 `common-mod` 의 `ErrorCode` 기본값을 쓴다: `1006` "다른 곳에서 로그인되어…" · `1005` "다시 로그인해 주세요" · `1004` "비활성화된 계정입니다"
- [X] T038 [US2] `.../front/web/WebMvcConfig.java` — 인터셉터를 등록하고 **비로그인 허용 URL 화이트리스트 6개**를 둔다(FR-618): `/auth/login` · `/auth/signup` · `/auth/find-id` · `/auth/find-password` · `/auth/reset-password` · `/error/forbidden`. **화이트리스트여야 한다** — 블랙리스트면 008~012 가 화면을 더할 때 빠뜨린 URL 이 무방비가 된다. **인터셉터를 타지 않는 경로는 정적 자원(`/css/**`·`/js/**`·`/images/**`·`/favicon.ico`)과 아이콘 프록시(`/expend-groups/icons/**`) 둘이다.** 아이콘을 빼는 이유는 정적 자원을 뺀 이유와 같다 — 지출유형 20개인 화면이면 아이콘 요청 20건마다 `MemberTokenValidate` 가 붙어 왕복이 40건이 된다. **판정을 건너뛰는 것이 아니다**: T029 의 프록시가 세션 토큰을 붙여 백엔드를 부르므로 소유자 판정·토큰 검증을 백엔드가 그대로 하고, 세션이 비었으면 `1001` 을 받아 이미지를 내보내지 않는다
- [X] T039 [US2] `.../front/web/AuthInterceptor.java` 의 **① 로그인 판정** — 세션에 `accessToken` 이 없으면 `/auth/login` 으로 보낸다(FR-618·623). 세션 만료로 토큰을 잃은 경우도 같은 갈래다
- [X] T040 [US2] T039 에 **`MemberTokenValidate` 호출**을 더한다(FR-614) — 세션에 토큰이 **있을 때만** 부른다. 세션이 비었으면 토큰 없이 호출해 `1001` 을 받는 것이 확정이라 왕복이 낭비다(research 9). 이 호출이 **세션은 살아 있는데 백엔드 토큰이 죽은 경우**를 잡는 유일한 지점이다 — API 를 하나도 부르지 않는 화면에서도 죽은 세션이 열리지 않게 한다. **T038 의 제외 경로(정적 자원·아이콘 프록시)에는 이 호출이 나가지 않는다** — 시험으로 호출 0건을 건다(session-auth.md §7)
- [X] T041 [US2] T039 에 **② 권한 판정**을 더한다(FR-619·620) — 관리자 전용 URL(`/admin/**`)에서 `role != 1` 이면 `/error/forbidden` 으로 보낸다. **①이 ②보다 먼저인 것이 요구사항이다.** 인터셉터를 둘로 나누지 않는다: 순서가 `addInterceptors` 등록 순서에 달리면 다른 파일에서 한 줄 끼워 넣는 것만으로 조용히 뒤집힌다(research 8)
- [X] T042 [US2] `.../front/web/AuthSessionController.java` 에 로그아웃 경로를 만든다(FR-621) — `POST /auth/logout` 이 `MemberTokenRevoke` 를 **먼저** 부르고 그다음 `session.invalidate()` 한 뒤 `/auth/login` 으로 보낸다. **순서를 뒤집으면 백엔드 JWT 가 살아남는다**(세션을 먼저 비우면 `Revoke` 에 쓸 Access 토큰이 없다). `Revoke` 가 실패해도 세션 무효화는 진행한다 — 로그아웃을 눌렀는데 화면이 로그인 상태로 남는 쪽이 더 나쁘다. **008 은 상단 버튼(FR-708)만 만들고 이 주소로 보낸다** — 백엔드 `MemberTokenRevoke` 를 008 이 직접 부르지 않는다
- [X] T043 [US2] `.../front/support/TokenMasker.java` 와 그 적용(FR-622·SC-607) — 토큰이 로그·모델·HTML 어디에도 원문으로 나가지 않게 한다. `LoginSession` 은 토큰을 모델에 넣는 통로를 제공하지 않고, T028 의 호출 로그가 `Authorization` 과 토큰 필드를 가린다. 시험으로 **응답 HTML 에 JWT 문자열이 없음**을 건다

**Checkpoint**: 세션 하나로 로그인·권한이 판정되고, 만료된 Access 가 사용자 모르게 갱신된다

---

## Phase 5: User Story 3 - 모든 화면이 같은 껍데기를 쓴다 (Priority: P2)

**Goal**: 로그인 전/후 레이아웃이 정해지고, 사이드바·모달 셸·확인 다이얼로그가 화면마다 다시 만들어지지 않는다

**Independent Test**: 껍데기만 쓰는 화면(1.6)과 모달 셸을 붙인 **시험 전용 화면**을 열어 레이아웃·사이드바 활성 표시·모달 열고 닫기·딥링크가 동작하는지 본다

### Tests for User Story 3

- [X] T044 [P] [US3] `.../front/web/ScreenShellTest.java` — 다섯 갈래: ① main 레이아웃 화면에 사이드바가 있고 `activeMenu` 가 활성(FR-625) ② `role=3` 응답 HTML 에 `/admin/members` 문자열이 **없다**(FR-626) ③ `?m=` 이 아는 값이면 모델에 `openModal` 이 있다(SC-610) ④ `?m=nonsense` 면 `openModal` 이 **없고** HTTP 200 으로 부모가 뜬다(FR-630) ⑤ `/error/forbidden` 이 **미로그인으로 200**(FR-632)
- [X] T045 [P] [US3] T044 가 쓸 **시험 전용 화면**을 `src/test/` 에 둔다 — 1.6 에는 모달이 없어 ③·④를 걸 대상이 없다. 모달 셸과 `data-modal-map` 을 붙인 최소 화면을 시험 소스에 두고 그것으로 확인한다. **`src/main` 에 두지 않는다**(운영 화면이 아니다)

### Implementation for User Story 3

- [X] T046 [P] [US3] 화면기획 정적 자원 **3개를 이식한다**(FR-631) — `프로젝트설계/화면기획/css/tokens.css` → `resources/static/css/tokens.css`, `css/ui.css` → `static/css/ui.css`, `js/modal.js` → `static/js/modal.js`. **세 파일의 내용을 고치지 않는다.** `modal.js`(75행)에는 `data-modal-open`·`data-modal-close`·Esc·딤 클릭·`data-modal-map` 딥링크가 이미 다 들어 있다
- [X] T047 [US3] `resources/templates/layout/auth.html` — 비로그인 화면(1.1~1.6)의 껍데기. `proto/layout-auth-shell.html` 을 옮기고 상대경로(`../css/...`)를 `th:href="@{/css/...}"` 로 바꾼다. `<head>` 에서 T046 의 세 파일을 **한 번만** 건다 — 화면마다 다시 걸지 않는다
- [X] T048 [US3] `resources/templates/layout/main.html` — 로그인 후 화면의 껍데기(사이드바 + 상단바 + 본문). `proto/layout-main-shell.html` 을 옮긴다. **모든 화면이 auth·main 둘 중 하나를 쓴다**(FR-624) — 화면이 자기 `<html>` 골격을 따로 두지 않는다
- [X] T049 [US3] `resources/templates/fragments/sidebar.html :: sidebar` — 메뉴 12개(screen-shell.md §2 표)와 `activeMenu` 활성 표시(FR-625). 등록 메뉴 셋(지출·소득·고정지출)은 부모 페이지와 **같은 `activeMenu`** 를 쓴다. **회원 관리 메뉴는 `role == 1` 일 때만 HTML 에 그린다**(FR-626) — `display:none` 이면 소스에서 URL 이 읽힌다
- [X] T050 [P] [US3] `resources/templates/fragments/modal-shell.html :: modalShell` — 딤·카드·헤더(제목 + X)·푸터(취소/확인) 자리를 제공하고 본문은 각 화면 fragment 가 채운다. 네 가지 닫기 경로(취소·X·Esc·딤)가 전부 동작해야 한다(FR-628) — 동작 자체는 T046 의 `modal.js` 가 이미 갖고 있다
- [X] T051 [P] [US3] `resources/templates/fragments/confirm-dialog.html` — 삭제·회원 정지·할부 중도상환이 **공용으로** 쓴다(FR-629). 제목·본문·확인 버튼 문구를 파라미터로 받는다. 되돌릴 수 없는 삭제(고정지출은 월별 내역까지 사라진다)는 **그 사실을 본문에 적어** 넘긴다 — 다이얼로그가 문구를 만들지 않는다. 화면마다 `confirm()` 을 쓰지 않는다
- [X] T052 [US3] `.../front/web/ModalParam.java` — `?m=` 값을 **화이트리스트로 판정**한다(FR-627·630). 아는 값이면 모델에 `openModal` 을 넣고, 모르는 값이면 **키 자체를 넣지 않는다**. 오류 화면으로 보내지 않는다 — 북마크·오타로 들어온 사용자를 막을 이유가 없다. **서버가 판정하고 JS 는 연다**(research 14): JS 에만 두면 상세·수정 모달의 선행 조회(`id` 로 단건 GET) 시점을 정할 수 없다. **화면별 값 목록은 007 이 정하지 않는다** — 008~012 가 자기 `data-modal-map` 과 함께 낸다
- [X] T053 [US3] `.../front/web/ForbiddenController.java` 와 `resources/templates/error/forbidden.html` — 화면 1.6(FR-632). `GET /error/forbidden` · **auth 레이아웃**(T001 의 개정 결과) · 권한 없음(비로그인 허용) · **호출 API 없음**. `proto/error-forbidden.html` 을 옮긴다: 제목 「권한 없음」, 안내 문구, 버튼 둘(「가계부로」→`/ledger`, 「로그인」→`/auth/login`). 이 화면이 T041 권한 차단의 착지점이다

**Checkpoint**: 008~012 가 얹힐 껍데기가 서고, 007 범위의 화면 1개가 뜬다

---

## Phase 6: User Story 4 - 옛 주소로 들어와도 화면에 닿는다 (Priority: P3)

**Goal**: 루트 주소와 이전 화면 주소로 들어와도 지금의 화면으로 보내진다

**Independent Test**: 루트와 구 URL 목록에 차례로 들어가 `Location` 헤더가 정해진 곳을 가리키는지 본다. **대상 화면이 없어도(008~012 미완) 검증된다**

### Tests for User Story 4

- [X] T054 [P] [US4] `.../front/web/UrlMapTest.java` — url-map.md §5 의 여섯 갈래: ① 세션 있음 + `/` → `/ledger` ② 세션 없음 + `/` → `/auth/login` ③ `/mem/login` → `/auth/login` ④ `/payments/1/edit` → `/payments?m=edit&id=1` ⑤ `/expend-groups/icons/1_1.png` 가 상세 redirect 로 **새지 않는다** ⑥ 미로그인 + `/payments/new` → `/auth/login`(모달 URL 이 아니다 — redirect 도 인터셉터를 탄다)

### Implementation for User Story 4

- [X] T055 [US4] `.../front/web/RootController.java` — `GET /` 가 세션에 토큰이 있으면 `302 → /ledger`, 없으면 `302 → /auth/login`(FR-633). **토큰 유효성까지 확인하지 않는다** — 세션 존재만 보고 보내고 실제 검증은 착지한 화면의 인터셉터가 한다. `301` 이 아니라 `302` 인 이유는 목적지가 로그인 상태에 따라 바뀌기 때문이다(`301` 이면 브라우저가 캐시해 로그아웃 후에도 `/ledger` 로 간다)
- [X] T056 [US4] `.../front/web/LegacyUrlController.java` 에 **실재했던 레거시 URL** 을 넣는다(url-map.md §2) — `/mem/login`(GET) → `301 /auth/login`, `/mem/ind` → `301 /`. `/mem/login`(POST)와 `/api/ammounts/**` 는 **redirect 하지 않고 없앤다**: POST 는 본문 형식이 백엔드 계약과 다르고, `/api/**` 는 화면 URL 이 아니다. **프론트가 REST 를 노출하면 브라우저가 화면 모듈을 API 처럼 부르는 길이 생긴다**
- [X] T057 [US4] 같은 클래스에 **구 화면 URL 16건**을 넣는다(FR-634, url-map.md §3 표) — 수단 3 · 지출유형 3 · 지출 2 · 소득 2 · 고정지출 4 · 회원 2. 전부 `301` 로 부모 + `?m=`(+ `id`) 로 보낸다. `id` 가 없으면 부모 페이지로만 보낸다. 목록에 없는 주소는 redirect 하지 않고 Spring 기본 404 다
- [X] T058 [US4] **매핑 순서를 확인한다** — `/expend-groups/icons/{filename}`(T029 의 프록시)이 `/expend-groups/{id}`(T057 의 상세 redirect)보다 **먼저** 매칭되어야 한다. 순서를 잘못 두면 아이콘 요청이 상세 모달 redirect 로 새어 나가고, 증상은 "아이콘이 하나도 안 보인다"라 원인이 URL 매핑이라는 것을 짐작하기 어렵다

**Checkpoint**: 북마크·구 링크로 들어온 사용자가 지금의 화면 주소로 착지한다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 전 스토리에 걸리는 마무리와 완료 판정

- [ ] T059 **PostgreSQL 을 띄운 상태에서** `./gradlew test`(**전 모듈**)를 돌려 한 번에 통과하는지 확인한다 — SC-603. `data-mod` 의 스키마 검증 통합 시험이 실제 DB 를 쓰므로 US1 의 "DB 를 내린 채 기동" 검증과 **전제가 반대다**: 한 번의 실행으로 둘 다 확인하려 하면 어느 쪽이든 실패한다. DB 를 내린 확인은 US1 에서 따로 한다. `init` 커밋부터 깨져 있던 `money-app` 시험 3건이 T008 로 정리되어 **모듈을 나눠 돌리지 않아도 되는 상태**가 이 기능의 결과물이다
- [ ] T060 `CLAUDE.md` 의 「알아둘 함정」에서 **`./gradlew test`(전체)는 실패한다** 항목을 지운다(106행) — T059 로 사실이 아니게 된다. 같은 문서의 `money-app` 설명(「DB 직접 접근 없음, 백엔드 API만 호출」)이 이제 실제와 맞는지도 확인한다
- [ ] T061 [P] `README.md` 와 `.cursor/rules/project-modules.mdc` · `.cursor/rules/logging-aop.mdc` 에서 `money-app` 서술을 실제와 맞춘다 — 의존 6건 제거, 패키지 `...front`, 로깅이 `logback-spring.xml` 로 옮겨진 것
- [ ] T062 [P] `프로젝트설계/기능명세상세-프론트엔드/README.md` 의 1.6 상태를 **골격 → 완료**로 고친다 — "현재 전 화면 골격" 문장도 함께 손본다(1.6 만 완료다)
- [ ] T063 quickstart.md §3 의 US1~US4 검증을 순서대로 실행하고, §4 완료 판정 13개 항목을 하나씩 확인한다 — 특히 **`git diff sql/schema-moneylogdb.sql` 이 비어 있는지**(원칙 VI: 이 기능은 DB 를 안 건드린다)와 **`logs/` 에 파일 로그가 쌓이는지**(원칙 IV: 지금은 한 줄도 없다)
- [ ] T064 커밋 전 자가 점검(헌장 § 개발 워크플로) — 응답이 `{ resCode, data }` 규격인가(프론트는 **소비** 쪽이며 봉투를 만들지 않는다), Entity 가 노출되지 않는가(**Entity 자체가 0개**다), 명세 표의 설명 칸이 비어 있지 않은가(T002 의 `(작성 예정)` 0건), DB 구조가 바뀌었다면 덤프를 재생성했는가(**변경 없음**)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존 없음. 즉시 시작. **T001~T004 는 코드보다 먼저**(원칙 V)
- **Foundational (Phase 2)**: Setup 완료 후. **모든 User Story 를 막는다**
- **US1 (Phase 3)**: Foundational 완료 후. 다른 스토리에 의존하지 않는다
- **US2 (Phase 4)**: Foundational 완료 후. **T036·T037 이 US1 의 T022 를 고친다** — 클라이언트를 US1 이 세운 뒤 인증을 얹는다
- **US3 (Phase 5)**: Foundational 완료 후. US1·US2 없이도 껍데기는 뜨지만, `role` 기반 사이드바(T049)와 1.6 착지(T053)는 US2 와 함께여야 의미가 산다
- **US4 (Phase 6)**: Foundational 완료 후. **T058 이 US1 의 T029(아이콘 프록시)를 전제**한다
- **Polish (Phase 7)**: 원하는 스토리가 전부 끝난 뒤

### User Story Dependencies

- **US1 (P1)** — 독립. Foundational 후 바로 시작
- **US2 (P1)** — 독립하게 시험할 수 있지만(세션을 직접 심는다) **구현은 US1 의 클라이언트 위에 얹힌다**. US1 → US2 순서를 권한다
- **US3 (P2)** — 독립. 껍데기는 API 를 부르지 않는다. 다만 **US1·US2 없이는 그 안에 넣을 것이 없다**(spec 의 우선순위 근거)
- **US4 (P3)** — 독립. 대상 화면이 없어도 `Location` 으로 검증된다

### Within Each User Story

- 시험을 먼저 쓰고 **실패하는 것을 확인한 뒤** 구현한다
- 값 객체·예외 → 클라이언트/세션 → 컨트롤러 → 템플릿 순
- 스토리를 끝내고 다음 우선순위로 넘어간다

### Parallel Opportunities

- Phase 1: T001·T003·T004 가 서로 다른 파일이라 **동시에** 가능(T002 는 T001 과 같은 파일이라 뒤에)
- Phase 2: T011·T012·T013 이 각각 다른 파일 — T010 이후 동시에 가능
- US1: T015·T016·T017(시험 셋) 동시. 구현은 T018·T019·T020·T021(값 객체·예외) 동시 → T022 로 합류
- US2: T030·T031·T032(시험 셋) 동시. T033 만 [P]
- US3: T044·T045 동시, T046·T050·T051 동시
- 팀이 있으면 Foundational 후 **US1+US2 한 사람 · US3 한 사람 · US4 한 사람**으로 갈라진다

---

## Parallel Example: User Story 1

```bash
# 시험 셋을 함께 (구현 전에 실패를 확인한다)
Task: "MoneyAppApplicationTests — DB 없이 컨텍스트 (T015)"
Task: "BackendApiClientTest — 봉투 5갈래 (T016)"
Task: "PagingTest — offset 환산 (T017)"

# 값 객체·예외를 함께 (서로 다른 파일)
Task: "BackendApiProperties (T018)"
Task: "ApiEnvelope (T019)"
Task: "BackendApiException · BackendUnavailableException (T020)"
Task: "BinaryPayload (T021)"
```

---

## Implementation Strategy

### MVP First (US1 만)

1. Phase 1 Setup — 명세 선행 개정 4건
2. Phase 2 Foundational — **레거시를 걷어내고 DB 없이 뜨게 한다** (여기서 SC-601·602 가 성립한다)
3. Phase 3 US1 — 백엔드 API 통로
4. **멈추고 검증**: DB 를 내린 채 기동하고, 성공·실패·바이너리·불통 넷을 확인한다
5. 이 시점의 인도물: 화면은 없지만 **원칙 I 위반이 사라진 화면 모듈**

### Incremental Delivery

1. Setup + Foundational → 기반 (원칙 I 위반 해소)
2. US1 → 독립 검증 → **MVP**
3. US2 → 독립 검증 → 로그인·권한이 서고 008 이 화면을 얹을 수 있다
4. US3 → 독립 검증 → 009~012 가 화면을 얹을 수 있다
5. US4 → 독립 검증 → 구 링크가 살아난다

### 007 이 끝나면

008~012 는 **화면만** 만든다. 받아 가는 것은 quickstart.md §5 표에 있다 —
`BackendApiClient` · 세션·인터셉터 · 레이아웃 2종·사이드바·모달 셸·확인 다이얼로그 ·
`Paging` · `ModalParam`.

각 스펙이 정할 것: 자기 화면의 `?m=` 값 목록, 실패 코드별 문구, 폼 필드.

---

## Notes

- [P] = 다른 파일, 미완 작업에 의존하지 않음
- [Story] 라벨로 작업과 User Story 를 잇는다
- **이 기능은 삭제가 많다.** 지운 뒤 컴파일이 깨지는 것이 정상이며, 컴파일러가 남은 참조를 짚어 준다
- 시험이 실패하는 것을 확인한 뒤 구현한다
- 작업 단위나 논리적 묶음마다 커밋한다
- `/auth/login`·`/ledger`·`/payments` 등은 **007 시점에 화면이 없다.** 시험은 `Location` 만 본다
- 피할 것: 봉투를 클라이언트 밖에서 푸는 것, `offset` 을 직접 만드는 것, 인터셉터를 둘로 나누는 것, `PUT` 메서드를 클라이언트에 두는 것
