# Implementation Plan: 프론트 공통 기반 — 화면 모듈 재구성

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-frontend-foundation/spec.md`

## Summary

`app-mod/money-app` 을 헌장 원칙 I 에 맞게 다시 세운다. 자체 Entity 12개·Repository·Mapper 와
`data-mod`·JPA·MyBatis 의존을 걷어내고, 데이터를 백엔드 API(`:8081/api/v1`)로만 얻는
화면 모듈로 바꾼다. 화면 31개가 밟을 **공통 기반**이며, 화면 자체는 **1.6 권한 없음 하나만** 만든다.

앞의 006까지와 성격이 셋 갈린다.

| 축 | 002~006 (백엔드) | 007 (프론트 기반) |
|---|---|---|
| 하는 일 | 없던 것을 **더한다** | 있던 것을 **걷어내고** 통로를 놓는다 |
| 저장 단위 | 테이블 6~15개 | **DB 저장 단위가 0개**. 상태는 세션 하나뿐 |
| 완료 판정 | 응답이 명세와 같은가 | **DB 로 가는 길이 없는가** (참조 0건·기동 성공) |

이 셋이 계획의 모양을 정한다.

**① 뺄셈이 먼저다.** 통로(API 클라이언트·세션 인증)를 먼저 만들고 레거시를 나중에 지우면,
지우는 동안 두 경로가 공존해 "화면이 어느 쪽으로 데이터를 얻는지" 알 수 없는 구간이 생긴다.
`data-mod` 의존을 **먼저** 끊으면 컴파일러가 레거시를 전부 짚어 준다 — 남은 참조를 사람이 세지 않아도 된다.

**② 봉투를 푸는 자리가 하나다.** 백엔드는 성공·실패 모두 `{ resCode, data }` 로 답하고
**실패도 HTTP 200** 이다(헌장 원칙 III). 화면마다 이걸 풀면 어느 화면 하나가 `resCode` 확인을
빠뜨렸을 때 실패가 성공으로 읽힌다. `BackendApiClient` 한 곳에서 풀고, 실패는 예외로 올린다.
**예외가 둘 있다** — 아이콘(003 의 2.10)과 엑셀 양식(004 의 3.11)은 본문이 바이너리다.

**③ 토큰은 서버 세션에만 산다.** 서버 렌더링이고 백엔드 호출도 서버가 하므로, 토큰이
브라우저에 있으면 `Authorization` 헤더를 붙일 수 없다. 브라우저로 내려가는 것은
`JSESSIONID` 뿐이다. 이 결정의 결과가 **아이콘 프록시**(FR-611)다 — 브라우저가 `:8081` 을
직접 부르지 않으므로 이미지도 화면 모듈이 중계해야 한다.

기술 결정 14건은 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Thymeleaf · Bean Validation · Lombok ·
`common-mod`(에러코드 Enum 참조용).
**이 기능은 의존성을 더하지 않고 뺀다** — `core-mod`·`data-mod`·`spring-boot-starter-data-jpa`·
`mybatis-spring-boot-starter`·`postgresql`·`spring-boot-starter-aspectj` 6건을 제거한다.
시험에만 `spring-boot-starter-webmvc-test` 를 더한다(Boot 4 에서 `spring-boot-starter-test` 가
`@AutoConfigureMockMvc` 를 더는 끌어오지 않는다).

HTTP 클라이언트는 **`RestClient`**(spring-web 내장, 추가 의존 없음). 근거는 research 1.

**Storage**: **없다.** 이 기능의 완료 조건이 "DB 로 가는 길이 없음"이다.
`sql/schema-moneylogdb.sql` 은 읽지도 바꾸지도 않는다. 화면 모듈이 들고 있는 상태는
`HttpSession` 속성 네 개뿐이다([data-model.md](./data-model.md)).

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test` ·
`MockRestServiceServer`(spring-test 내장 — 백엔드를 띄우지 않고 `RestClient` 응답을 세운다).
**깨져 있던 레거시 시험 2파일 3건을 지우는 것이 이 기능의 요구사항이다**(FR-608·SC-603).

**Target Platform**: JVM 서버. 화면 `:8080`, 백엔드 `:8081`.

**Project Type**: Spring Boot 멀티모듈 — 이 기능은 프론트(Thymeleaf 서버 렌더링) 모듈 하나만 손댄다.

**Performance Goals**: 정하지 않는다. 다만 **성격은 기록한다** — 아이콘 프록시는 지출유형
개수만큼 요청이 나갈 수 있다(백엔드 `_공통.md` 가 "수십 개 이하면 허용"으로 이미 정했다).
화면 한 번 렌더링에 백엔드 호출이 여러 번 나갈 수 있고, 그중 하나가 `1001` 이면 재발급 1회가
그 호출 안에서 일어난다.

**Constraints**:
- 화면 모듈은 **에러코드를 새로 정의하지 않는다**(FR-607). 백엔드 코드를 그대로 옮긴다.
- 백엔드 호출은 메서드별 규칙을 지킨다(FR-609) — GET 은 Path **또는** Query 하나만, `PUT` 0건.
  예외는 `StatisticsMonthlyGet` 하나.
- `offset` 은 항상 `limit` 의 배수여야 한다. 어기면 목록이 통째로 `9001` 이다(FR-610).
- 재발급은 **한 번뿐**(FR-617). 재발급 직후 다시 `1001` 이어도 또 하지 않는다.
- 로그인 판정이 권한 판정보다 **먼저**다(FR-620). 순서가 뒤집히면 미로그인 사용자에게
  관리자 URL 의 실재가 드러난다.
- JWT 원문을 로그·HTML·화면에 남기지 않는다(FR-622).
- 비로그인 허용 화면 URL 은 **1.1~1.6 여섯 개뿐**이다(FR-618).

**Scale/Scope**: 화면 1개(1.6) · FR 34건(FR-601~634) · SC 10건 · User Story 4개(P1 2 · P2 1 · P3 1) ·
새 에러코드 0건 · DB 테이블 0개. 걷어내는 레거시 자바 파일 **53개**, 템플릿 6개.
(`templates/` 아래 화면별 디렉터리 골격 10개는 이미 비어 있는 채로 만들어져 있다 —
`auth`·`error`·`expend-group`·`fixed-expense`·`fragments`·`ledger`·`member`·`payment`·
`statistics`·`target`. 007 은 그중 `layout`·`fragments`·`error` 세 곳만 채운다.)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — **현재 VIOLATION 이고, 그것을 고치는 것이 이 기능이다.**
      `money-app` 이 `data-mod`·`core-mod`·JPA·MyBatis·PostgreSQL 을 의존하고
      `spring.profiles.active=postgresql` 로 DB 에 직접 붙는다. 작업 후 남는 의존은
      `common-mod` 하나다. **`core-mod` 도 함께 끊는다** — 원칙 I 의 허용 목록은
      `money-app → common-mod` 뿐이라, spec 의 FR-601 이 적지 않은 `core-mod` 도 위반이다.
- [x] **II. 레이어 흐름** — PASS. 화면 모듈에는 Repository 가 **없다**.
      흐름은 `PageController / *WebController → BackendApiClient → (HTTP)` 한 방향이다.
      Entity 를 노출하지 않는 정도가 아니라 **Entity 자체를 없앤다**(12개 삭제).
- [x] **III. 응답 규격** — PASS(소비 쪽). 화면 모듈은 `{ resCode, data }` 를 **만들지 않고 푼다**.
      푸는 자리를 한 곳으로 강제하는 것이 FR-603 이고, `PUT` 금지는 호출 쪽에도 그대로 건다.
      다만 **아이콘 프록시는 봉투가 아닌 바이트를 돌려주는 프론트 엔드포인트**다 →
      Complexity Tracking 에 기록한다.
- [x] **IV. 로깅** — **현재 조용히 깨져 있다.** `money-app` 의 `logback.xml` 은 전체를
      `<springProfile name="default">` 로 감쌌는데, ① 파일명이 `logback-spring.xml` 이 아니라
      Spring 확장 태그가 해석되지 않고 ② 앱이 `postgresql` 프로필로 떠서 조건도 맞지 않는다.
      **오류 없이 appender 가 하나도 붙지 않는다.** 파일명을 바꿔 콘솔+파일을 되살린다.
      AOP 는 원칙 IV 상 **백엔드 의무**이고, 프론트는 바깥으로 나가는 지점이
      `BackendApiClient` 하나라 AOP 없이 그 한 곳에서 남긴다(research 11).
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `프로젝트설계/기능명세상세-프론트엔드/` 와
      `화면기획/` 이 있다. **착수 전 개정 4건**이 남아 있다 → 아래 표.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql` 을 읽어 확인했고 **이 기능은
      DB 를 건드리지 않는다**. 화면 모듈이 DB 를 떠나는 것이라 스키마 영향도가 구조적으로 0 이다.
      덤프 재생성 대상이 아니다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 `money-app` 의 컴포넌트 스캔을 `com.dbdomino.moneylog.front` 로 좁혔다.
      지금은 `com.dbdomino.moneylog` 전체를 스캔해 `common-mod` 의 `com.dbdomino.moneylog.common`
      까지 들어오는데, 레거시 `money-app` 이 **같은 패키지 이름**(`...moneylog.common.aop` 등)을
      쓰고 있어 split package 이기도 하다. 패키지를 `...front` 로 옮겨 둘을 갈랐다.
- [x] **II** — Repository 0개. `BackendApiClient` 가 유일한 바깥 통로다.
- [x] **III** — 호출 규칙(FR-609)을 [contracts/api-client.md](./contracts/api-client.md) 표로
      고정했다. `PUT` 0건. 아이콘 프록시 1건만 봉투 밖이며 근거를 Complexity Tracking 에 적었다.
- [x] **IV** — `logback-spring.xml` 로 옮기고, 토큰 마스킹을 호출 로그와 모델 전달 두 곳에 건다.
- [x] **V** — 선행 개정 4건을 [quickstart.md](./quickstart.md) 의 「0. 착수 전」 절차로 넣었다.
- [x] **VI** — DB 무변경. 완료 판정에 `git diff sql/schema-moneylogdb.sql` 이 비어 있는지를 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `기능명세상세-프론트엔드/phase1-회원/1.6-ErrorForbidden.md` 메타 | 레이아웃 `main` → **`auth`** | `화면기획/01-화면구성.md` 표와 `proto/error-forbidden.html`(`layout-auth`) 이 둘 다 auth 다. 1.6 은 **미로그인도 들어오는 화면**이라 로그인 후 사이드바를 붙일 수 없다 |
| 2 | `1.6-ErrorForbidden.md` 본문 | 「화면 구성」·「동작」·「검증·안내」의 **(작성 예정)** 세 표를 채운다 | 007 범위의 화면이 1.6 하나다(spec §범위). 골격인 채로 구현하면 원칙 V 의 "설명 칸 자체 완결"을 어긴다 |
| 3 | `기능명세상세-프론트엔드/_공통.md` § 표시방식 | 구 URL redirect 예시가 **실재한 적 없는 주소**(`/payments/new`·`/ledger/expenses/new`)뿐이다. 실재 레거시 URL(`/mem/login`·`/mem/ind`·`/api/ammounts/**`) 처리를 함께 적는다 | FR-634 의 대상이 무엇인지 문서만 보고 알 수 없다. 확정본은 [contracts/url-map.md](./contracts/url-map.md) |
| 4 | `_공통.md` § 인증·토큰 의 「세션 만료」 행 | `server.servlet.session.timeout` **값을 적는다(30분)** | 현재 설정이 **60초**다. 명세가 값을 비워 둬 아무도 이상하다고 보지 않았다. Access 토큰이 1일인데 화면 세션이 1분이면 FR-623 이 "1분마다 재로그인"으로 관측된다 |

3번과 4번이 특히 필요하다. 3번을 두면 구현자가 redirect 표를 **추측으로** 만들고,
4번을 두면 60초 설정을 "의도된 값"으로 오해해 그대로 둔다.

## Project Structure

### Documentation (this feature)

```text
specs/007-frontend-foundation/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 14건
├── data-model.md        # Phase 1 — 세션 하나 (DB 저장 단위 0개)
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-client.md    # 봉투 해석·바이너리 예외·메서드 규칙·페이징·실패 전달
│   ├── session-auth.md  # 세션 속성·판정 순서·재발급 1회·로그아웃
│   ├── screen-shell.md  # 레이아웃 2종·사이드바·모달 딥링크·확인 다이얼로그·1.6
│   └── url-map.md       # 루트 분기와 구 URL redirect 표
├── checklists/
│   └── requirements.md  # /speckit-checklist 산출물
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물
```

### Source Code (repository root)

`+`는 신규, `~`는 수정, `-`는 **삭제**다. 이 기능은 삭제가 가장 많다.

```text
build.gradle                                      ~ money-app 의존 6건 제거,
                                                    webmvc-test 1건 추가 (모듈별 build.gradle 없음)

app-mod/money-app/src/main/java/com/dbdomino/moneylog/
├── VanillaApplication.java                       - → front/MoneyAppApplication.java
├── common/{aop,constants,exception,util}/**      - 레거시 5개 (common-mod 와 패키지 이름 충돌)
├── controller/{Ammount,MemberLogin}Controller    - 구 REST·로그인 컨트롤러
├── domain/{UserEx,UserExController,UserExRepository} -
├── dto/**                                        - 12개 (Ammount·Card·Expend·Member 등)
├── entity/**                                     - Entity 12개 + superclass/CuDate
├── mapper/**                                     - MyBatis Mapper 9개
├── repository/**                                 - Repository 5개
└── service/{Ammount,Member}Service.java          -

app-mod/money-app/src/main/java/com/dbdomino/moneylog/front/   + 새 패키지 (스캔 범위도 여기로)
├── MoneyAppApplication.java                      + scanBasePackages = "...moneylog.front"
├── client/
│   ├── BackendApiClient.java                     + 봉투를 푸는 유일한 자리 (FR-603·604)
│   ├── ApiEnvelope.java                          + { resCode, data } 역직렬화 전용 타입
│   │                                               common-mod 의 RestResponseDto 는 못 쓴다 (research 2)
│   ├── BinaryPayload.java                        + 바이트 + Content-Type + 파일명 (FR-605)
│   ├── BackendApiException.java                  + resCode + message — 코드를 그대로 들고 올라간다
│   ├── BackendUnavailableException.java          + 연결 거부·타임아웃 (FR-606) — 코드가 아니다
│   └── BackendApiProperties.java                 + base-url · 타임아웃
├── session/
│   ├── LoginSession.java                         + 세션 속성 읽기·쓰기의 유일한 자리 (FR-613)
│   ├── SessionUser.java                          + memberId · role (JWT 원문 아님)
│   └── TokenRefresher.java                       + 1001 → 재발급 1회 (FR-615·617)
├── web/
│   ├── WebMvcConfig.java                         + 인터셉터 등록 + 비로그인 허용 URL 목록
│   ├── AuthInterceptor.java                      + 로그인 판정 → 권한 판정 순서 (FR-618~620)
│   ├── FrontExceptionHandler.java                + @ControllerAdvice — 실패 표시 공통화 (FR-612)
│   ├── AuthSessionController.java                + POST /auth/logout — Revoke 후 세션 무효화 (FR-621)
│   ├── ModalParam.java                           + ?m= 화이트리스트 판정 (FR-627·630)
│   ├── Paging.java                               + offset = page × limit 환산 (FR-610)
│   ├── RootController.java                       + / 분기 (FR-633)
│   ├── LegacyUrlController.java                  + 구 URL → 부모 + ?m= (FR-634)
│   ├── IconProxyController.java                  + 아이콘 중계 (FR-611)
│   └── ForbiddenController.java                  + 1.6 권한 없음 (FR-632)
└── support/
    └── TokenMasker.java                          + 로그·모델에서 JWT 를 가린다 (FR-622)

app-mod/money-app/src/main/resources/
├── application.yml                               ~ profiles.active 제거, JPA·MyBatis 설정 제거,
│                                                   backend base-url 추가, 세션 30분
├── application-h2.yml · application-mysql.yml    - 쓰지 않는 DB 프로필
├── logback.xml                                   - → logback-spring.xml (원칙 IV, research 11)
├── logback-spring.xml                            + 콘솔+파일. money-backend-app 것과 같은 골격
├── static/css/{tokens.css,ui.css}                + 화면기획에서 이식 (FR-631)
├── static/js/modal.js                            + 화면기획에서 이식
├── static/css/bootstrap*.css · static/js/bootstrap.js - 레거시 샘플이 쓰던 것
└── templates/
    ├── {home,index,signin}.html                  - 레거시 샘플
    ├── page/signupsample.html · target/default.html -
    ├── layout/base.html                          - → layout/{auth,main}.html
    ├── layout/{auth,main}.html                   + 껍데기 2종 (FR-624)
    ├── fragments/sidebar.html                    + activeMenu·관리자 메뉴 숨김 (FR-625·626)
    ├── fragments/modal-shell.html                + 모달 껍데기 (FR-627·628)
    ├── fragments/confirm-dialog.html             + 공통 확인 다이얼로그 (FR-629)
    ├── error/forbidden.html                      + 1.6 — auth 레이아웃 (FR-632)
    └── error.html                                ~ 백엔드 불통·미정의 오류의 착지 (FR-606)

app-mod/money-app/src/test/
├── resources/application.yml                     - **메인 설정을 덮어써 3건을 깨뜨린 파일**
├── java/.../VanillaApplicationTests.java         -
├── java/.../service/MemberServiceTest.java       -
└── java/com/dbdomino/moneylog/front/             +
    ├── MoneyAppApplicationTests.java             + DB 없이 컨텍스트가 뜨는지 (SC-601)
    ├── client/BackendApiClientTest.java          + 봉투·바이너리·연결 실패 (US1)
    ├── session/TokenRefreshTest.java             + 재발급 1회·Rotation·1006 (US2)
    ├── web/AuthInterceptorTest.java              + 판정 순서·허용 URL (US2)
    ├── web/ScreenShellTest.java                  + 레이아웃·사이드바·모달 딥링크 (US3)
    ├── resources/templates/__test/modal-probe.html + 딥링크 검증용 시험 전용 화면 (1.6 에는 모달이 없다)
    └── web/UrlMapTest.java                       + 루트 분기·구 URL redirect (US4)

프로젝트설계/기능명세상세-프론트엔드/
├── _공통.md                                       ~ 선행 개정 3·4
└── phase1-회원/1.6-ErrorForbidden.md              ~ 선행 개정 1·2
```

**Structure Decision**: 모듈 구성은 그대로다. 새 모듈을 만들지 않고 `money-app` **안에서**
패키지를 갈아엎는다.

007 이 새로 만드는 구조는 셋이다.

- **`com.dbdomino.moneylog.front` 로의 패키지 이동** — 지금 `money-app` 은 `com.dbdomino.moneylog`
  루트에 클래스를 두고 `com.dbdomino.moneylog.common.{aop,exception,util}` 까지 쓴다.
  **`common-mod` 의 패키지와 이름이 겹친다.** 게다가 `@SpringBootApplication(scanBasePackages =
  "com.dbdomino.moneylog")` 이라 `common-mod` 의 빈까지 스캔 범위에 든다. 지금은
  `GlobalExceptionHandler` 와 `ApiLoggingAspect` 가 `moneylog.common.web.enabled=true` 조건으로
  막혀 있어 사고가 나지 않을 뿐이고, **조건 하나에 기대는 상태**다. `...front` 로 옮기고 스캔
  범위를 거기로 좁히면 두 모듈이 이름으로 부딪힐 일이 없어진다.

- **`client` 하나가 유일한 바깥 통로** — 봉투 해석(FR-603)·실패 전달(FR-604·607)·바이너리
  예외(FR-605)·연결 실패(FR-606)·호출 규칙(FR-609)·재발급(FR-615)·호출 로그(원칙 IV)가
  **전부 여기 모인다**. 008~012 는 이 클래스만 부르고 봉투를 보지 않는다.
  "화면마다 각자 풀지 않는다"를 구조로 강제하는 방법은 풀 수 있는 자리를 하나만 두는 것이다.

- **`session` 과 `web` 의 분리** — 세션 속성을 읽고 쓰는 자리(`LoginSession`)와, 그것을 보고
  통과·차단을 정하는 자리(`AuthInterceptor`)를 나눈다. 재발급(`TokenRefresher`)은 세션 쪽에
  둔다 — 인터셉터가 아니라 **API 호출 도중**에 일어나는 일이기 때문이다(FR-615). 인터셉터에
  두면 화면 진입 시점에만 갱신되어, 한 화면이 API 를 여러 번 부를 때 두 번째 호출의 `1001` 을
  받아낼 자리가 없다.

`Paging`·`ModalParam` 은 `web` 에 값 객체로 둔다. 008~012 가 각자 계산하면 `offset` 배수 규칙
(어기면 목록이 통째로 `9001`)과 `?m=` 화이트리스트가 화면마다 갈린다.

`core-mod`·`data-mod` 는 이 기능 이후 `money-app` 에서 **참조 0건**이 된다.

## Complexity Tracking

> 헌장 원칙 위반은 없다. 다만 **원칙 III 의 문면과 어긋나 보이는 결정 1건**과,
> spec 이 적지 않았으나 원칙 I 이 요구하는 **범위 확대 1건**을 기록한다.

| 항목 | 통상적인 기대 | 이 기능이 하는 것 | 이유 |
|---|---|---|---|
| 아이콘 프록시 응답 | 원칙 III — 모든 응답이 `{ resCode, data }` | `GET /expend-groups/icons/{filename}` 이 **이미지 바이트**를 돌려준다 | FR-611. 원칙 III 는 **백엔드 REST API 계약**이고, 이것은 `<img src>` 가 가리키는 화면 자원이다. 봉투에 담으면 `<img>` 가 그릴 수 없다 |
| `core-mod` 의존 제거 | spec FR-601 은 DB·`data-mod`·JPA·MyBatis 만 적었다 | **`core-mod` 도 함께 끊는다** | 원칙 I 의 허용 목록은 `money-app → common-mod` 뿐이다. FR-601 의 열거가 좁은 것이지 `core-mod` 참조가 허용된 것이 아니다 |

**왜 아이콘만 예외인가**: 백엔드의 `ExpendGroupIconGet` 자체가 이미 봉투 밖이다
(백엔드 `_공통.md` § 지출유형 아이콘 — "JSON 에 base64 를 넣지 않는다"). 007 은 그 바이트를
**중계**할 뿐 새 규격을 만들지 않는다. 엑셀 양식 다운로드(3.11)도 같은 성격이지만 그쪽
엔드포인트는 010 이 만든다 — 007 은 바이트를 실어 올 수 있는 통로(`BinaryPayload`)까지만 낸다.

**대가**: 이 경로는 실패해도 봉투로 알릴 수 없다. 백엔드가 실패 봉투를 주면 프록시는
**이미지를 내보내지 않고** 404 로 끝내고, 화면은 `<img>` 의 대체 표시로 넘어간다.
`iconUrl` 이 `null` 인 경우와 화면상 같은 결과가 되며, 이는 백엔드 `_공통.md` 가 이미
"없으면 플레이스홀더"로 정한 것과 어긋나지 않는다.

기각한 대안 2가지.

- **브라우저가 `:8081` 을 직접 부르고 JS 가 Bearer 를 붙인다** — 토큰이 JS 에 읽혀야 하므로
  "토큰은 서버 세션에만" 결정이 무너진다. 백엔드에 CORS 설정도 따라붙는다.
- **목록 JSON 에 base64 로 아이콘을 실어 보낸다** — 백엔드 계약을 바꾸는 일이고
  (007 의 가정은 "백엔드 코드는 바뀌지 않는다"), 응답이 비대해진다.
