# Research: 프론트 공통 기반 — 화면 모듈 재구성

**Feature**: 007-frontend-foundation | **Date**: 2026-09-08 | **Plan**: [plan.md](./plan.md)

spec 에 `NEEDS CLARIFICATION` 은 없다. 여기 적는 것은 **spec 이 "무엇을"만 정하고 "어떻게"를
열어 둔 자리**와, 지금 코드를 읽다가 드러난 함정이다. 결정 14건.

---

## 1. HTTP 클라이언트는 `RestClient`

**Decision**: `org.springframework.web.client.RestClient`. `RestClient.Builder` 를 빈으로 두고
`BackendApiClient` 가 주입받는다.

**Rationale**: spring-web 에 들어 있어 **의존성이 늘지 않는다**(007 은 의존을 빼는 기능이다).
동기 API 라 서버 렌더링 흐름에 그대로 맞고, `MockRestServiceServer` 로 백엔드 없이 시험할 수
있다 — 이것이 US1·US2 를 "화면 없이, 백엔드 없이" 검증하게 해 준다.

**Alternatives considered**:

- `RestTemplate` — 유지보수 모드다. 새로 까는 코드에 넣을 이유가 없다.
- `WebClient` — 리액터 의존이 따라온다. 서버 렌더링에서 매 호출을 `block()` 하게 되고,
  프론트 모듈에 안 쓰는 스택을 하나 더 얹는다.
- `HttpClient`(JDK) — 봉투 역직렬화·메시지 컨버터를 직접 짜야 한다. Jackson 연결을 손으로 잇는
  만큼 FR-603("한 곳에서만 해석")의 코드가 길어진다.

---

## 2. 봉투는 **역직렬화 전용 타입**으로 받는다

**Decision**: `common-mod` 의 `RestResponseDto` 를 응답 파싱에 **쓰지 않는다.**
`money-app` 안에 `ApiEnvelope<T>`(`resCode`, `data`)를 따로 둔다.

**Rationale**: `RestResponseDto` 는 **생성자가 private 이고 정적 팩터리(`ok`·`fail`·`failWith`)로만
만들어진다.** `@JsonCreator` 도 없다. Jackson 이 인스턴스를 만들 방법이 없어 그대로 쓰면
역직렬화가 실패한다. 그 클래스는 **백엔드가 응답을 만들 때**를 위한 것이고, 프론트는 반대
방향(읽기)이 필요하다.

억지로 맞추려면 `common-mod` 에 생성자·애너테이션을 더해야 하는데, 그건 **백엔드 응답 규격
클래스를 프론트 사정으로 고치는 일**이다. 원칙 I 의 의존 방향은 지켜지지만 변경 이유가
모듈을 넘나든다.

`ErrorCode` Enum 은 그대로 쓴다 — 값을 읽기만 하므로 문제가 없고, 007 이 코드를 새로 정의하지
않는다는 FR-607 을 컴파일 시점에 붙들어 준다.

**Alternatives considered**:

- `common-mod` 의 `RestResponseDto` 에 `@JsonCreator` 추가 — 백엔드 직렬화 결과가 바뀌지는
  않지만, 프론트를 위해 백엔드 규격 클래스를 여는 선례가 된다.
- `Map<String, Object>` 로 받고 꺼내 쓰기 — `resCode` 를 문자열로 읽거나 빠뜨려도 컴파일이
  통과한다. FR-604("실패를 성공으로 읽지 않는다")를 타입으로 막지 못한다.

---

## 3. 실패는 **예외로 올린다.** 코드는 그대로 옮긴다

**Decision**: `resCode != 200` 이면 `BackendApiException(resCode, message)` 를 던진다.
호출부(컨트롤러)는 잡지 않고, `@ControllerAdvice`(`FrontExceptionHandler`)가 받아 화면으로 보낸다.

**Rationale**: FR-612 는 "컨트롤러마다 try-catch 로 제각각 만들지 말라"이고, FR-604 는
"실패를 성공으로 읽지 말라"다. **반환값으로 돌려주면 호출부가 확인을 빠뜨릴 수 있다** —
빠뜨린 코드도 컴파일된다. 예외는 빠뜨릴 수 없다.

`BackendApiException` 은 백엔드가 준 `resCode` 와 `data.message` 를 **그대로** 들고 올라간다.
화면 모듈은 코드를 만들지도 바꾸지도 않는다(FR-607). 화면별 문구(예: `3401` 을 "지출용 수단을
고르세요"로 바꾸는 일)는 **그 화면의 스펙**(009~012)이 하고, 007 은 코드와 원문 메시지를 나른다.

**Alternatives considered**:

- `Result<T>` / `Either` 반환 — 확인 누락을 언어가 막아 주지 않는다.
- 컨트롤러마다 try-catch — FR-612 가 금지한다.

---

## 4. 연결 실패는 **에러코드가 아니다**

**Decision**: 연결 거부·타임아웃·봉투가 아닌 응답은 `BackendUnavailableException`(코드 없음)으로
구분한다. 화면에는 "서버에 닿지 못했습니다"로 표시하고, **빈 목록으로 렌더링하지 않는다**.

**Rationale**: FR-606 과 FR-607 이 함께 걸린다. 사용자에게는 알려야 하는데(606) 화면 모듈은
새 코드를 정의할 수 없다(607). 그래서 **코드를 붙이지 않고** 상태로만 다룬다. `9000`(서버 오류)
을 빌려 쓰면 "백엔드가 9000 을 줬다"와 "백엔드에 닿지 못했다"가 구분되지 않는다 — 둘은 원인도
사용자 안내도 다르다.

봉투가 아닌 응답(HTML 오류 페이지, 빈 본문 등)도 여기로 묶는다. spec Edge Cases 의
"백엔드가 봉투가 아닌 것을 돌려줄 때 → 실패로 본다"가 이 갈래다.

**Alternatives considered**:

- `9000` 재사용 — 위 이유로 기각.
- 화면 모듈 전용 코드 신설(예: `9500`) — FR-607 정면 위반.

---

## 5. 바이너리 2건은 **호출 지점이 스스로 밝힌다**

**Decision**: `BackendApiClient` 에 `getBinary(...)` 를 따로 둔다. Content-Type 을 보고 자동으로
갈라지지 않는다.

**Rationale**: 자동 판정은 두 방향으로 틀린다. ① 백엔드가 아이콘 요청에 **실패 봉투**를 주면
Content-Type 이 `application/json` 이라 봉투로 읽히는데, 호출부는 바이트를 기다리고 있다.
② 반대로 정상 JSON 응답이 어떤 이유로 `application/octet-stream` 으로 오면 조용히 바이트가 된다.

**어느 API 가 바이너리인지는 명세에 이미 적혀 있다** — 003 의 2.10(아이콘)과 004 의 3.11(엑셀
양식) 둘뿐이다. 호출부가 `getBinary` 를 부르는 것이 곧 그 선언이고, 그 밖의 모든 호출은
봉투를 강제한다(FR-605 의 "2건"이 코드에서도 2곳으로 센다).

바이너리 호출에서 봉투(JSON)가 오면 **그것은 실패다** — `BackendApiException` 으로 올린다.

**Alternatives considered**:

- Content-Type 자동 분기 — 위 두 오작동.
- URL 패턴 화이트리스트 — 007 이 008~012 의 URL 을 미리 알아야 한다. 경계가 반대로 뒤집힌다.

---

## 6. 재발급은 **호출 단위**에 둔다. 인터셉터가 아니다

**Decision**: `BackendApiClient.call()` 안에서 `1001` 을 받으면 `TokenRefresher` 로 한 번
재발급하고 **그 요청만** 다시 보낸다. 재시도 여부는 호출 지역 변수(플래그)로 잠근다.

**Rationale**: FR-615·617 과 spec Edge Cases 의 "한 화면이 API 를 여러 번 부르는데 그중 하나가
`1001` 이면?"이 이 자리를 정한다. 인터셉터(화면 진입 시점)에 두면 **진입 이후에 만료된 토큰**을
받아낼 곳이 없다 — 화면이 첫 호출을 성공하고 두 번째에서 `1001` 을 만나면 그대로 실패한다.

"한 번뿐"은 **호출 하나당 한 번**이다. 화면이 API 를 셋 부르면 최악의 경우 재발급 시도가 세 번
일어날 수 있지만, 첫 번째가 성공하면 새 토큰이 세션에 반영되어 나머지 둘은 `1001` 을 만나지
않는다(Edge Cases 의 명시된 기대). 첫 번째가 실패하면 그 자리에서 세션이 무효화되고 로그인으로
가므로 두 번째 호출은 일어나지 않는다.

**Alternatives considered**:

- 인터셉터에서 진입 시 1회 — 위 이유로 기각.
- 요청 스코프 빈으로 "이 요청에서 이미 재발급함"을 기억 — 화면 전체에서 한 번으로 묶이지만,
  첫 호출이 재발급에 성공한 뒤 **다른 이유로** 두 번째가 `1001` 을 받는 경우(예: 그 사이 다른
  기기 로그인)를 영영 못 살린다. 그 경우도 어차피 `1006` 이라 세션 무효화로 끝나므로 이득이
  없고, 요청 스코프 빈을 더 놓는 만큼만 복잡해진다.

---

## 7. Refresh Token 도 **함께 덮어쓴다** (Rotation) · `1005` 의 자리

**Decision**: 재발급 성공 시 세션의 `accessToken` **과 `refreshToken` 을 모두** 새 값으로
덮어쓴다. 재발급 실패 코드는 `1005`·`1006`·`1004` 중 무엇이든 **결과가 같다** — 세션 무효화 후
로그인.

**Rationale**: 백엔드 `1.5-MemberTokenRefresh` 응답은 `accessToken` 과 함께 **새 `refreshToken`
을 준다(Rotation)**. Access 만 덮어쓰면 세션에는 이미 폐기된 Refresh 가 남고, 다음 재발급이
`1005` 로 실패한다 — 증상은 "한 번은 되고 두 번째부터 안 되는 로그인 연장"이라 원인을 찾기 어렵다.

spec 의 에러코드 표에는 `1001`·`1006`·"그 밖의 4자리"만 있고 **`1005` 가 없다.** 표를 고칠
필요는 없다 — FR-616 이 "재발급이 **실패하거나** `1006` 을 받으면"으로 이미 덮는다. `1005` 는
재발급 실패의 대표 코드이고, 화면 처리는 `1006` 과 같다. 다만 사용자 문구는 갈린다:
`1006` 은 "다른 곳에서 로그인되어 로그아웃되었습니다", `1005` 는 "다시 로그인해 주세요"다
(`ErrorCode` 의 기본 문구가 이미 그렇게 나뉘어 있다).

**Alternatives considered**:

- Access 만 갱신 — 위 증상.
- `1005` 를 spec 표에 추가 — 표는 "화면 모듈의 반응"을 적는 표이고 `1005` 의 반응이
  `1006` 과 같아 행이 늘 뿐이다. 대신 이 결정을 [contracts/session-auth.md](./contracts/session-auth.md)
  에 적는다.

---

## 8. 판정 순서를 **인터셉터 하나 안**에 가둔다

**Decision**: `AuthInterceptor` 하나가 ① 로그인 판정 ② 권한 판정을 순서대로 한다.
인터셉터를 둘로 나누지 않는다.

**Rationale**: FR-620("로그인 판정이 권한 판정보다 먼저")은 **순서가 곧 요구사항**이다.
인터셉터를 둘로 나누면 순서가 `addInterceptors` 등록 순서에 달리는데, 그건 다른 파일에 있고
누가 나중에 한 줄 끼워 넣으면 조용히 뒤집힌다. 뒤집히면 미로그인 사용자가 관리자 URL 에서
`/error/forbidden` 을 보게 되고, **그 URL 이 실재한다는 사실이 드러난다**(spec Edge Cases).

한 클래스 안이면 순서가 코드 줄 순서이고, 시험 하나로 고정할 수 있다.

**Alternatives considered**:

- 인터셉터 2개 + `order` — 순서가 등록부에 흩어진다.
- Spring Security — 이 프로젝트는 JWT 검증을 **백엔드**가 하고 프론트는 세션만 본다.
  Security 를 들이면 필터 체인·인증 객체 개념이 하나 더 늘고, 의존을 빼는 이 기능과 어긋난다.

---

## 9. `MemberTokenValidate` 는 **보호 화면 진입 때만** 부른다

**Decision**: 인터셉터가 세션에 토큰이 **있을 때만** `MemberTokenValidate` 를 부른다.
세션이 비었으면 부르지 않고 바로 `/auth/login` 이다. 정적 자원(`/css/**`·`/js/**`)·**아이콘
프록시(`/expend-groups/icons/**`)**·비로그인 허용 URL 은 인터셉터를 타지 않는다.

**Rationale**: FR-614 가 "보호 화면 진입 시 확인한다"이다. 세션이 빈 상태에서 부르면 토큰 없이
호출해 `1001` 을 받는 것이 확정이라 **의미 없는 왕복**이다(`_공통.md` 도 "세션이 비었으면 확인
없이 `/auth/login`"으로 이미 적어 두었다).

이 호출은 **세션은 살아 있는데 백엔드 토큰이 죽은 경우**를 잡는 유일한 지점이다(spec Edge
Cases). 화면이 API 를 하나도 부르지 않는 경우(예: 1.6, 정적 안내 화면)에도 세션 존재만으로
통과시키지 않겠다는 뜻이다.

정적 자원을 제외하는 이유는 명백하다 — 페이지 한 장에 CSS·JS 요청이 따라붙는데 그때마다
백엔드를 부르면 화면 한 번에 검증이 여러 번 나간다.

**같은 이유로 아이콘 프록시(`/expend-groups/icons/**`)도 제외한다.** 동적 엔드포인트지만
브라우저가 `<img>` 로 부르는 화면 자원이라 성격이 정적 자원과 같다. 지출유형 20개인 화면이면
아이콘 요청 20건마다 검증이 붙어 왕복이 40건이 된다. **권한이 느슨해지지 않는다** — 프록시는
세션의 `accessToken` 을 붙여 백엔드를 부르므로 소유자 판정과 토큰 검증을 백엔드가 그대로 하고,
세션이 비었으면 `1001` 을 받아 이미지를 내보내지 않는다.

**Alternatives considered**:

- 매 요청 검증(정적 포함) — 위 이유.
- 검증을 아예 생략하고 `1001` 이 올 때만 대응 — API 를 부르지 않는 화면에서 죽은 세션이
  살아 있는 것처럼 열린다. FR-614 위반.

---

## 10. 페이징 환산은 **`page` 를 입력으로 받는다**

**Decision**: `Paging` 값 객체가 `page`(0-based)와 `limit` 을 받아 `offset = page × limit` 을
만든다. **`offset` 을 직접 받는 생성자를 두지 않는다.** `limit` 이 바뀌면 `page` 를 0 으로 되돌린다.

**Rationale**: FR-610 이 "`offset` 은 항상 `limit` 의 배수"인데, 이를 **검증**으로 두면 어기는
값이 만들어질 수 있고 그 결과는 목록이 통째로 `9001` 로 실패하는 것이다. **만들 수 없게** 하는
편이 짧다 — 생성 경로를 `page × limit` 하나만 두면 배수가 아닌 `offset` 이 나올 방법이 없다.

`limit` 변경 시 첫 페이지로 되돌리는 것도 같은 이유다. `page=3`·`limit=10`(offset 30)에서
`limit` 만 20 으로 바꾸면 offset 30 은 20 의 배수가 아니다.

응답의 `totalCount` 로 총 페이지 수(`ceil(totalCount / limit)`)를 만드는 것도 여기 둔다.
008~012 가 각자 나눗셈을 하면 마지막 페이지 계산이 화면마다 갈린다.

**Alternatives considered**:

- `offset` 을 받고 배수 검증 — 위 이유.
- 화면마다 계산 — FR-610 이 "화면마다 따로 두지 않는다"로 금지.

---

## 11. 로깅 — 파일명을 고치고, AOP 를 쓰지 않는다

**Decision**: `logback.xml` → **`logback-spring.xml`** 로 옮기고 `<springProfile>` 로 전체를
감싸지 않는다. 요청~응답 AOP 는 두지 않고, **`BackendApiClient` 한 곳**에서 호출 로그를 남긴다.

**Rationale**: 지금 `money-app` 의 로깅은 **조용히 죽어 있다.** 두 겹으로 어긋났다.

1. 파일명이 `logback.xml` 이다. `<springProfile>`·`<springProperty>` 는 Spring 확장 태그라
   `logback-spring.xml` 에서만 해석된다. `logback.xml` 은 Spring 이 개입하기 전에 로드된다.
2. 설정 전체가 `<springProfile name="default">` 로 감싸여 있는데 앱은 `postgresql` 프로필로 뜬다.

**어느 쪽이든 appender 가 하나도 붙지 않고, 오류도 나지 않는다.** `money-backend-app` 의
`logback-spring.xml` 머리말이 이 함정을 이미 적어 두었다 — 007 이 그 골격을 그대로 가져온다.

AOP 를 두지 않는 이유: 원칙 IV 의 AOP 의무는 **`money-backend-app`** 에 걸린 것이고, 프론트에서
관측해야 할 것은 컨트롤러 진입이 아니라 **백엔드 호출**(method·URI·`resCode`·소요 시간)이다.
그 지점은 `BackendApiClient` 하나뿐이라 AOP 로 가로챌 이유가 없다 — 한 메서드 안에서 남기면
된다. 이 결정 덕에 `spring-boot-starter-aspectj` 의존도 함께 뺀다.

**마스킹은 여기서 걸린다**(FR-622): `Authorization` 헤더와 로그인·재발급 응답의 토큰 필드는
로그에 원문으로 나가지 않는다.

**Alternatives considered**:

- `<springProfile name="default">` 를 `postgresql` 로 바꾸기 — 프로필 자체를 없애는 기능에서
  프로필 이름을 고치는 건 방향이 반대다.
- 프론트에도 AOP — 가로챌 지점이 하나인데 애스펙트를 놓는다.

---

## 12. 세션 타임아웃은 **30분**

**Decision**: `server.servlet.session.timeout: 30m`. (현재 값은 `60` — 단위 없는 숫자라 **60초**다.)

**Rationale**: 화면 세션이 죽으면 토큰을 잃은 것이라 미로그인이 된다(FR-623). 지금 값이면
**1분 방치할 때마다 다시 로그인**해야 한다. Access 토큰이 1일인데 화면이 1분이라 재발급 흐름
(FR-615)이 실제로 관측될 일도 거의 없다 — 세션이 먼저 죽기 때문이다.

30분으로 한 근거는 두 쪽에서 온다. ① 백엔드가 회원당 활성 세션 1건이라 **브라우저를 켜 둔 채
방치한 세션이 오래 살면** 그만큼 토큰이 서버 세션에 남는다. ② 그렇다고 Access 토큰(1일)까지
늘리면 FR-623 이 사실상 관측되지 않는다. 둘 사이에서 관례값을 고른다.

`_공통.md` 에 값을 적는 것을 선행 개정 4번으로 넣었다 — 명세가 값을 비워 둔 탓에 60초가
"의도된 값"으로 읽혔다.

**Alternatives considered**:

- 1일(Access 토큰과 같게) — 세션 만료 경로가 사실상 사라진다.
- 60초 유지 — 사용 불가.

---

## 13. 패키지를 `...front` 로 옮기고 스캔 범위를 좁힌다

**Decision**: `com.dbdomino.moneylog.front` 로 모으고
`@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog.front")` 로 좁힌다.
`moneylog.common.web.enabled` 는 프론트에서 **켜지 않는다**.

**Rationale**: 지금 `money-app` 은 `com.dbdomino.moneylog.common.{aop,constants,exception,util}`
을 쓴다. **`common-mod` 와 패키지 이름이 겹친다**(split package). 게다가 스캔 범위가
`com.dbdomino.moneylog` 전체라 `common-mod` 의 `GlobalExceptionHandler`(`@RestControllerAdvice`)
와 `ApiLoggingAspect` 가 프론트에도 후보로 들어온다. 지금은 두 빈이
`@ConditionalOnProperty(moneylog.common.web.enabled=true)` 로 막혀 있어 사고가 없을 뿐이다.

`@RestControllerAdvice` 가 프론트에 붙으면 **화면 오류가 JSON 으로 나간다.** 프론트는 뷰를
돌려주는 앱이라 오류도 화면이어야 한다(FR-612). 조건 프로퍼티 하나에 이 성질이 걸려 있는
상태를 007 이 정리한다 — 이름이 안 겹치고 스캔 범위가 안 겹치면 조건이 없어도 안전하다.

`common-mod` 의 `ErrorCode` 는 계속 쓴다(빈이 아니라 Enum 이라 스캔과 무관하다).

**Alternatives considered**:

- 패키지 유지 + 조건 프로퍼티에 계속 기대기 — 백엔드 `application.yml` 을 손대는 사람이
  프론트를 깨뜨릴 수 있다. 두 앱이 같은 프로퍼티 이름을 공유한다.
- `common-mod` 의 웹 빈들을 아예 다른 패키지로 옮기기 — 백엔드를 건드리는 일이고,
  007 의 가정("백엔드 코드는 바뀌지 않는다")과 어긋난다.

---

## 14. 모달 딥링크는 **서버가 판정하고**, JS 는 열기만 한다

**Decision**: 컨트롤러가 `?m=` 값을 화이트리스트(`ModalParam`)로 확인해 **아는 값일 때만**
모델에 `openModal` 을 넣는다. 모르는 값이면 모델에 넣지 않고 부모 페이지만 렌더링한다.
`modal.js` 는 `data-modal-map` 에 있는 id 만 연다.

**Rationale**: FR-630 은 "정의되지 않은 `?m=` 값은 모달을 열지 않고 부모만. 오류 화면으로
보내지 않는다"이다. **두 겹으로 막는 것이 아니라, 판정을 서버에 두고 JS 는 결과만 따르게**
한다. JS 에만 두면 서버 렌더링 시점에는 아무 것도 모르는 상태라 모달 fragment 를 항상 그려야
하고, 상세·수정 모달이 필요로 하는 **선행 조회**(예: `id` 로 단건 GET)를 언제 할지 정할 수 없다.

`modal.js` 는 화면기획 산출물을 그대로 이식한다(FR-631) — `data-modal-open`·`data-modal-close`·
Esc·딤 클릭·`data-modal-map` 딥링크가 이미 다 들어 있다(75줄). **007 은 이 파일을 고치지 않는다.**

`ModalParam` 이 아는 값의 목록은 007 이 정하지 않는다 — 화면별 목록은 008~012 가 자기 화면의
`data-modal-map` 과 함께 낸다. 007 은 **판정하는 방법과 모르는 값의 처리**만 고정한다.

**Alternatives considered**:

- JS 단독 판정 — 위 이유(선행 조회 시점을 정할 수 없다).
- 모르는 `?m=` 을 400/오류 화면으로 — FR-630 이 명시적으로 금지한다. 북마크·오타로 들어온
  사용자를 막을 이유가 없다.

---

## 미해결로 남긴 것

| 항목 | 왜 남기나 | 누가 정하나 |
|---|---|---|
| 화면별 `?m=` 값 목록 | 007 은 판정 방법만 정한다. 값은 그 화면의 것이다 | 008~012 |
| 실패 메시지의 화면별 문구 (`3401` → "지출용 수단을 고르세요" 등) | 같은 코드라도 화면마다 안내가 다르다(`_공통.md` § 자주 만나는 실패 코드) | 009~012 |
| 아이콘 캐시 헤더 | 백엔드 `_공통.md` 가 "Cache-Control 로 재요청을 줄인다"까지만 정했다. 값이 없다 | 009 (아이콘을 실제로 쓰는 첫 화면) |
| 플래시 메시지 표시 위치 | 화면기획 이식 가이드가 "성공 시 부모 URL redirect + flash"로만 적었다 | 008 (첫 폼 화면) |
