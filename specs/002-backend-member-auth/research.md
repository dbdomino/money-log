# Research: 회원 가입·인증·토큰·관리자 회원 관리

**Feature**: `002-backend-member-auth` | **Date**: 2026-09-02 | **Phase**: 0

Technical Context의 미결 항목과, 기존 코드를 읽어야 답이 나오는 항목을 정리한다.
각 항목은 **결정 / 근거 / 기각한 대안** 형식이다.

조사 기준점은 세 가지다 — 저장소의 현재 코드(`build.gradle`, `data-mod`),
`프로젝트설계/기능명세상세-백엔드/_공통.md`, 그리고 헌장 `.specify/memory/constitution.md`.

---

## 1. Spring Security를 도입할 것인가

**결정**: 도입한다. `spring-boot-starter-security`를 `money-backend-app`에 추가한다.

**근거**: 스펙이 두 가지를 전제로 적고 있다 — Assumptions의
"비밀번호 해싱은 Spring Security `BCryptPasswordEncoder`를 쓴다"와
"인증 필터가 토큰의 `sub`를 `id_key`로 환산해 `SecurityContext`에 함께 싣는다".
`_공통.md`의 검증 순서도 마지막 줄이 "→ SecurityContext에 memberId, role, sessionId 설정"이다.
`SecurityContextHolder`는 `AuditorAware` 구현이 현재 요청의 회원을 알아내는 표준 경로이기도 해서,
`data-mod`의 임시 구현을 걷어내는 데 그대로 쓰인다.

인가 경계를 `SecurityFilterChain` 한 곳에 선언할 수 있는 것도 크다. 이 기능의 API 16건은
**권한 없음 7건 / 로그인 5건 / 관리자 5건** 세 부류로 갈리는데, 이걸 Controller 애너테이션이나
직접 만든 필터의 `if`문으로 흩으면 새 API가 늘 때마다 빠뜨릴 자리가 생긴다.

**기각한 대안**:
- **직접 만든 `OncePerRequestFilter`만 쓰고 Security를 안 넣는다** — 필터 자체는 어렵지 않지만
  `PasswordEncoder`를 따로 구해야 하고(bcrypt 단독 라이브러리 추가), `SecurityContext`가 없으니
  요청 범위 회원 정보를 담을 그릇을 직접 만들어야 하며, `AuditorAware`도 그 자체 그릇을 봐야 한다.
  결국 Security가 주는 것을 하나씩 다시 만드는 셈이다.
- **Spring Security OAuth2 Resource Server(`spring-boot-starter-oauth2-resource-server`)** — JWT 검증을
  선언적으로 처리해 주지만, 이 프로젝트는 **JWT 검증만으로 끝나지 않는다.** `_공통.md`가 정한
  10단계 중 5~9단계가 DB 대조다. 표준 리소스 서버 흐름에 DB 대조를 끼워 넣으려면 결국 커스텀
  `AuthenticationProvider`를 붙여야 해서, 얻는 것보다 우회가 많다.

---

## 2. JWT 라이브러리

**결정**: `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.12.x 3종. 버전을 명시한다
(Spring Boot BOM이 관리하지 않는다).

**근거**: 필요한 것은 HS256 서명·파싱·클레임 추출뿐이다. 스펙의 클레임은
`sub`(memberId)·`role`·`sid`(sessionId)·`exp`·`iat` 다섯 개로 고정돼 있다. jjwt는 이 범위에서
API가 가장 좁고, `impl`·`jackson`을 `runtimeOnly`로 분리해 컴파일 표면을 `api`로 한정할 수 있다.
프로젝트가 이미 Jackson을 쓰고 있어 `jjwt-jackson`이 직렬화기를 새로 끌고 오지 않는다.

**기각한 대안**:
- **nimbus-jose-jwt** — Spring Security가 내부적으로 쓰는 라이브러리라 전이 의존으로 들어올 수도
  있지만, 우리가 쓰는 것은 대칭키 HS256 하나다. JOSE 전반을 다루는 API 표면이 필요 이상으로 넓다.
- **Spring Security의 `JwtEncoder`/`JwtDecoder`** — `spring-security-oauth2-jose`가 추가로 필요하고,
  결정 1에서 리소스 서버 방식을 기각한 것과 같은 이유로 이 추상을 쓸 자리가 없다.
- **직접 구현(HMAC + Base64URL)** — 서명 검증을 직접 짜는 것은 보안 코드를 자체 제작하는 것이다.
  상수 시간 비교, 알고리즘 혼동(`alg: none`) 방어 같은 것을 빠뜨리기 쉽다.

---

## 3. 토큰 해시 알고리즘 (`access_token_hash`·`refresh_token_hash`)

**결정**: **SHA-256 hex 소문자 64자**. bcrypt를 쓰지 않는다.

**근거**: 두 가지가 결정한다.

1. **`_공통.md`의 Refresh 검증 2단계가 "DB: `refresh_token_hash` 일치 세션 조회"다.**
   해시값으로 행을 찾으려면 같은 입력이 항상 같은 해시를 내야 한다. bcrypt는 salt가 들어가
   매번 다른 값이 나오므로 **조회 자체가 불가능**하다. 전 세션을 훑으며 `matches()`를 돌리는
   방법밖에 없는데 그건 세션이 늘수록 선형으로 느려진다.
2. **컬럼이 `varchar(100)`이다.** SHA-256 hex는 64자로 여유 있게 들어간다.

토큰은 이미 128비트 이상의 엔트로피를 가진 난수(또는 서명된 JWT)라, 비밀번호처럼 사전 공격
대상이 아니다. 비밀번호에 bcrypt를 쓰는 이유(느리게 만들어 무차별 대입을 막는다)가 토큰에는
적용되지 않는다. 저장 해시의 목적은 **DB가 유출돼도 토큰 원문이 바로 드러나지 않게** 하는 것이고
SHA-256이 그 목적을 충족한다.

**기각한 대안**:
- **bcrypt** — 위 1번 때문에 Refresh 조회 경로가 성립하지 않는다.
- **원문 저장** — DB 유출이 곧 전 사용자 세션 탈취가 된다.
- **HMAC-SHA256(서버 비밀키)** — SHA-256보다 한 겹 낫지만 키 관리가 하나 더 늘고,
  토큰 자체가 이미 고엔트로피라 실익이 작다. 필요해지면 알고리즘만 교체하면 되도록
  해시 함수를 한 곳(`MemberSessionService`)에 가둔다.

---

## 4. Refresh Token의 형식

**결정**: JWT가 아닌 **불투명 랜덤 문자열**. `SecureRandom` 32바이트를 Base64URL로 인코딩한다(43자).

**근거**: 스펙 Assumptions가 "Refresh Token은 JWT가 아닌 불투명 랜덤 문자열이다"로 못박았다.
근거도 분명하다 — Refresh는 **항상 DB를 거쳐야만** 유효하다(`_공통.md` 검증 2~5단계가 전부 DB다).
JWT로 만들면 "서명만 맞으면 유효해 보이는" 자기 완결 토큰이 되는데, 실제 판정은 DB가 하므로
클레임이 무의미하고 오히려 오해를 부른다.

**기각한 대안**: JWT Refresh — 위 이유로 이점이 없다. `UUID` — 122비트로 부족하지 않지만
난수 목적이 드러나지 않고 형식이 고정돼 길이 조정 여지가 없다.

---

## 5. 인증 코드를 어느 모듈에 둘 것인가 (가장 중요한 결정)

**결정**: **DB를 아는가**로 가른다.

| 클래스 | 모듈 | 이유 |
|---|---|---|
| `JwtTokenProvider` | `common-mod` | 문자열 서명·파싱만 한다. DB를 모른다 |
| `ErrorCode`·`BusinessException`·`GlobalExceptionHandler` | `common-mod` | 헌장이 지정한 위치다 |
| `MemberSessionService` | `money-backend-app` | `tbl_user_session`을 읽고 쓴다 |
| `TokenAuthenticationFilter` | `money-backend-app` | 검증 5~9단계가 DB 대조다 |

**근거**: `_공통.md`의 구현 위치 가이드는 넷을 전부 `common-mod`에 두라고 적었는데,
그대로 하면 `common-mod → data-mod` 의존이 생긴다. 헌장 원칙 I의 정방향은
`data-mod → common-mod`이므로 **역행**이다. 게다가 `common-mod`는 프론트 `money-app`도
의존하는 최하위 모듈이라, 여기에 JPA가 들어오면 "프론트는 DB에 직접 붙지 않는다"는
원칙 I의 목적 자체가 무너진다.

실제로 루트 `build.gradle`의 `project(':common-mod')` 블록에는 `data-mod`가 없고
`spring-boot-starter-data-jpa`도 없다. 명세대로 두려면 이 블록을 고쳐야 하는데,
그 수정이 곧 원칙 위반이다.

**기각한 대안**: [plan.md의 Complexity Tracking](./plan.md#complexity-tracking)에 적었다 —
`common-mod`가 `data-mod`를 의존하게 하는 안과, 인터페이스만 `common-mod`에 두는 안 둘 다 기각했다.

**후속**: `_공통.md`의 구현 위치 가이드를 개정 대상으로 올렸다(plan.md § 명세 선행 개정 #1).

---

## 6. `AuditorAware` 실 구현과 임시 조치 제거

**결정**: `money-backend-app`에 `BackendAuditorAware`를 두고 `SecurityContext`의 `id_key`를 돌려준다.
`data-mod`의 `JpaAuditingConfig.auditorAware()` 임시 빈은 없앤다. 함께 걷어낼 임시 조치는
`BaseAuditEntity`의 `@Setter`와 `AbstractSchemaIT.stampAudit()`이다.

**근거**: `JpaAuditingConfig`의 주석이 직접 지목한다 — "TODO 백엔드 Phase 1(회원·인증) 구현 시:
인증 필터가 `SecurityContext`에 실어 둔 `id_key`를 꺼내 돌려주도록 교체한다. …
`BaseAuditEntity`의 `@Setter`와 `AbstractSchemaIT.stampAudit()`이 같은 임시 조치이므로 함께 걷어낸다."

**비로그인 경로의 처리**가 이 결정의 핵심이다. 감사 컬럼은 `tbl_user`를 뺀 전 테이블에서
NOT NULL인데, 가입(1.2)은 로그인 없이 도는 경로다. 규칙은 이렇다.

| 경로 | `created_by`/`updated_by` |
|---|---|
| 본인 가입(1.2)이 만드는 `tbl_user` 행 | `null` — `tbl_user`만 두 컬럼이 nullable이다 |
| 가입이 만드는 `tbl_user_expend_group` 10건 | **방금 만든 그 회원의 `id_key`** |
| 관리자 회원 추가(1.12)가 만드는 `tbl_user` 행 | **관리자의 `id_key`** (FR-121·US4 시나리오 5) |
| 로그인한 회원의 모든 쓰기 | 그 회원의 `id_key` |
| 로그인 실패 이력(FR-127) | 그 회원의 `id_key` — 회원이 특정될 때만 행을 만든다 |

즉 `AuditorAware`가 빈 값을 돌려주는 상황에서 NOT NULL 컬럼에 쓰는 경로는
**가입이 만드는 지출유형 10건 하나뿐**이고, 그건 방금 만든 회원의 `id_key`를 알고 있으므로
서비스가 명시적으로 채운다.

**기각한 대안**: `AuditorAware`가 0이나 시스템 계정 id를 돌려준다 — 실재하지 않는 회원 id가
감사 컬럼에 들어간다. 감사 컬럼에 FK가 없어서(001의 결정) DB가 막아주지도 않는다.

---

## 7. 에러코드 Enum과 전역 예외 처리

**결정**: `common-mod`에 `ErrorCode` Enum(코드 + 기본 메시지)과 `BusinessException`(ErrorCode 보유),
`GlobalExceptionHandler`(`@RestControllerAdvice`)를 만든다.

**근거**: 헌장 원칙 III이 "에러코드는 Enum으로 정의하고 비즈니스·검증 실패는 커스텀 예외로 던진다.
`RuntimeException`에 문자열만 담아 던지지 않는다"고 명시한다. `common-mod`에 두는 이유는
프론트도 같은 코드 목록을 참조할 수 있어야 하기 때문이다(`common-mod`는 `money-app`도 의존한다).

이 기능이 정의하는 코드는 [spec.md § 이 기능이 쓰는 에러코드](./spec.md)의 13개다.
다만 Enum은 **이 기능 것만 담지 않는다** — 003~006이 쓰는 `30xx`~`36xx`도 자리를 잡아 두면
후속 기능이 Enum을 매번 손대지 않는다. 003~006 스펙에 에러코드 표가 이미 다 있으므로
한 번에 옮길 수 있다.

**HTTP 상태 매핑**이 까다로운 지점이다. SC-101이 "비즈니스·검증 실패도 HTTP 200"을 요구하므로
`GlobalExceptionHandler`는 `BusinessException`을 **HTTP 200**으로 내려야 한다. 예외를 던졌는데
200이 나가는 것이 낯설지만, 명세가 정한 계약이다. 서버 오류(`Exception`)만 500 + `9000`이다.

**기각한 대안**: 코드별 예외 클래스를 따로 만든다 — 13개(전체로는 40여 개) 클래스가 생기는데
셋 다 하는 일이 같다. `ErrorCode`를 들고 다니는 예외 하나로 충분하다.

---

## 8. AOP 요청·응답 로깅과 마스킹

**결정**: `common-mod`에 `ApiLoggingAspect`를 두고 `@RestController` 대상 포인트컷 하나로
진입·종료·소요 시간을 남긴다. 마스킹은 `SensitiveMasker`가 담당한다.

**근거**: 헌장 원칙 IV — "백엔드의 요청~응답 로깅은 AOP로 한다. Controller마다 진입/종료 로그를
수동 작성하지 않는다"와 "비밀번호·토큰은 마스킹한다".

마스킹 대상은 이 기능에서 구체적으로 정해진다.

| 종류 | 대상 |
|---|---|
| 요청 필드 | `password`, `newPassword`, `passwordConfirm`, `refreshToken` |
| 요청 헤더 | `Authorization` |
| 응답 필드 | `accessToken`, `refreshToken` |

`pw`(Entity 필드명)는 DTO 경계를 넘지 않으므로 로그에 나올 일이 없지만, 이름을 목록에 함께
넣어 두면 나중에 누가 Entity를 로그에 찍어도 걸린다.

**기각한 대안**: 서블릿 필터에서 body를 읽어 로깅한다 — `HttpServletRequest`의 body는 한 번만
읽히므로 `ContentCachingRequestWrapper`가 필요하고, 그러면 전 요청의 body를 메모리에 복사한다.
AOP는 이미 역직렬화된 DTO를 받으므로 그 비용이 없다.

---

## 9. 파일 로깅

**결정**: `money-backend-app/src/main/resources/logback-spring.xml`을 만들어 콘솔+파일을 동시에 쓴다.

**근거**: 헌장 원칙 IV가 "콘솔+파일 동시 기록"을 요구하는데 백엔드에는 logback 설정이 아예 없다.
`money-app`에는 `logback.xml`이 있으므로 그 설정을 기준으로 맞춘다.

파일명을 `logback-spring.xml`로 하는 이유는 Spring 확장(`<springProfile>`, `<springProperty>`)을
쓸 수 있어서다. `logback.xml`은 Spring이 개입하기 전에 로드돼 프로필 분기를 못 쓴다.

**기각한 대안**: `application.yml`의 `logging.file.name`만 쓴다 — 파일 출력은 되지만
롤링 정책·패턴을 세밀하게 정할 수 없다.

---

## 10. 동시 로그인 경합

**결정**: 애플리케이션이 "기존 세션 폐기 → 새 세션 삽입"을 한 트랜잭션으로 처리하되,
**부분 유니크 인덱스 위반을 최종 방어선으로 삼아** 잡아서 재시도한다.

**근거**: 스펙 Edge Cases가 "활성 세션 부분 유니크 인덱스가 DB에서 1건을 강제한다.
애플리케이션 검사만으로는 창을 닫을 수 없다"고 적었다. 실제로 인덱스
`ux_user_session_active`가 `WHERE NOT revoked` 조건으로 DB에 있다(001에서 만든 부분 유니크 2건 중 하나).

두 요청이 같은 순간 "활성 세션 없음"을 보고 둘 다 삽입하면 하나가 유니크 위반으로 실패한다.
그 실패를 500(`9000`)으로 흘리면 사용자에게는 "로그인 실패"로 보인다. 실패한 쪽이 짧게 재시도하면
그때는 상대가 만든 세션이 보이므로 폐기 후 삽입이 정상적으로 끝난다.

**기각한 대안**: `SELECT ... FOR UPDATE`로 회원 행을 잠근다 — 확실하지만 로그인마다 회원 행에
쓰기 잠금이 걸린다. 경합이 드문 상황(같은 계정 동시 로그인)에 상시 비용을 치른다.

---

## 11. 기본 지출유형 10종 생성 (FR-106) — 기능 경계 문제

**결정**: `002`가 `DefaultExpendGroupService`를 만들어 `tbl_user_expend_group`에 직접 쓴다.
`003`의 API(2.7)를 부르지 않는다.

**근거**: 이건 002가 003의 자원을 만드는 구조라 경계가 어색해 보이지만, 대안이 더 나쁘다.
가입 시점에 003의 REST API를 호출하면 백엔드가 자기 자신에게 HTTP 요청을 보내는 꼴이고,
가입 트랜잭션과 유형 생성 트랜잭션이 갈라져 "회원은 생겼는데 유형이 없는" 상태가 가능해진다.
설계 명세 `1.2-MemberSignup.md`도 "기본 지출유형 10종을 **내부 로직으로** 자동 등록한다
(Phase 2 API 전 선행 처리)"로 이 방향을 이미 정해 두었다.

**아이콘 복사**는 `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/`의
10개 PNG(실재 확인함 — 교육·교통·기타·문화·쇼핑·식비·의료·장보기·주거·통신)를 회원별 복사본으로
만든다. 파일명은 `{id_key}_{expendGroupId}.png`다 —
`003`이 clarify에서 확정한 규칙이며, `1.2-MemberSignup.md`의 옛 표기는 개정됐다(커밋 `84ad88c`).

파일명에 `expendGroupId`가 들어가므로 **행을 먼저 저장해 PK를 받은 뒤 파일을 복사**해야 한다.
순서가 뒤집히면 파일명을 정할 수 없다.

**기각한 대안**: 시드 SQL로 넣는다 — 회원마다 생기는 데이터라 스키마 덤프에 넣지 않기로
001에서 이미 정했다(`1.2-MemberSignup.md` 비고).

---

## 12. 관리자 목록 페이징 검증 (FR-120)

**결정**: `offset`·`limit`을 **필수**로 받고, `limit <= 0` · `offset < 0` · `offset % limit != 0`
셋 중 하나라도 걸리면 `9001`로 거절한다. Spring Data의 `Pageable` 자동 바인딩을 쓰지 않는다.

**근거**: `Pageable`은 `page`(0부터)와 `size`를 받는 모델이고 기본값을 자동으로 채운다.
우리 계약은 `offset`·`limit`이며 **기본값이 없다**(둘 다 필수, 없으면 `9001`).
자동 바인딩을 쓰면 값이 빠졌을 때 기본값으로 조용히 통과해 `9001`이 나가지 않는다.

`offset`이 `limit`의 배수여야 한다는 조건은 설계 명세 `1.13-AdminMemberList.md`가 정한 것으로,
페이지 경계에 맞지 않는 임의 offset을 막아 목록이 겹쳐 보이는 것을 방지한다.

응답에서 `page`·`totalPages`를 빼는 것도 같은 이유다 — `offset`/`limit` 모델과 `page` 모델을
한 응답에 섞으면 프론트가 어느 쪽을 신뢰할지 갈린다.

**기각한 대안**: `Pageable`을 쓰고 `offset`을 `page`로 환산한다 — 환산이 성립하려면
`offset % limit == 0`이 보장돼야 하는데, 그 검증이 바로 우리가 직접 해야 하는 일이다.
환산 후에도 `9001` 판정은 직접 해야 하므로 얻는 것이 없다.

---

## 미해결로 남긴 것

없다. spec.md의 `[NEEDS CLARIFICATION]`은 clarify 세션에서 전부 닫혔고(커밋 `84ad88c`),
이 문서의 12개 결정으로 Technical Context의 빈칸이 모두 채워졌다.

**Performance Goals만 의도적으로 비워 두었다.** 명세에 수치 목표가 없고, 없는 목표를 지어내면
검증도 지어낸 기준으로 하게 된다. 이 기능이 만드는 성능 성격("요청마다 DB 세션 조회 1회 추가")은
plan.md의 Technical Context에 적어 두었으므로, 실측이 필요해지는 시점에 근거를 갖고 정한다.
