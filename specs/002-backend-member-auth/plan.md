# Implementation Plan: 회원 가입·인증·토큰·관리자 회원 관리

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-backend-member-auth/spec.md`

## Summary

Phase 1 회원·인증 API 16건(1.1~1.16)을 `money-backend-app`에 올린다. 저장 구조는
`001-backend-db-schema`가 이미 만들었고(`tbl_user`·`tbl_user_session`·`tbl_user_login_history`),
이 기능은 그 위에 **인증 골격**과 **회원 유스케이스**를 얹는다.

작업의 무게 중심은 API 16건이 아니라 **아직 없는 공통 기반 5종**이다.

| 기반 | 현재 상태 | 이 기능에서 만드는 것 |
|---|---|---|
| 에러코드 Enum·커스텀 예외·전역 예외 처리 | 없음 | `common-mod`에 `ErrorCode`·`BusinessException`·`GlobalExceptionHandler` |
| 인증 필터·JWT 발급·검증 | 없음 (Spring Security 의존성 자체가 없다) | Spring Security 도입 + 토큰 파이프라인 |
| 요청·응답 AOP 로깅과 마스킹 | 없음 | `common-mod`에 로깅 Aspect |
| 파일 로깅 | `money-app`에만 `logback.xml` | 백엔드용 `logback-spring.xml` |
| `AuditorAware` 실 구현 | 빈 `Optional`을 돌려주는 임시 구현 | `SecurityContext`의 `id_key`를 공급하도록 교체 |

마지막 항목이 특히 중요하다. `data-mod`의 `JpaAuditingConfig.auditorAware()`가
`Optional::empty`를 돌려주는 임시 상태이고, 그 대가로 `BaseAuditEntity`에 `@Setter`와
테스트의 `stampAudit()`이 붙어 있다. 코드 주석이 "백엔드 Phase 1 구현 시 교체하고 임시
조치를 함께 걷어낸다"고 지목한 시점이 지금이다.

기술적 접근은 [research.md](./research.md)에서 결정했다. 요지는 Spring Security를 도입해
`SecurityFilterChain`으로 인가 경계를 선언하고, 토큰 검증은 `_공통.md`가 정한 10단계 순서를
그대로 옮긴 필터 하나가 수행하며, **세션을 읽는 코드는 `common-mod`가 아니라
`money-backend-app`에 둔다**는 것이다(마지막 항목은 설계 명세의 구현 위치 가이드와 다르다 —
[Complexity Tracking](#complexity-tracking) 참고).

## Technical Context

**Language/Version**: Java 17 (`sourceCompatibility`/`targetCompatibility` 모두 17)

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Spring Data JPA · Spring AOP(aspectj) ·
Bean Validation · Lombok · MapStruct 1.6.3 · PostgreSQL JDBC.
**이 기능에서 추가**: `spring-boot-starter-security`(BCrypt·`SecurityContext`·필터 체인),
JWT 라이브러리 `io.jsonwebtoken:jjwt` 0.12.x 3종(api/impl/jackson).
추가는 전부 루트 `build.gradle`의 `project(':...')` 블록에서 한다 — 모듈별 `build.gradle`이 없다.

**Storage**: PostgreSQL 18, DB `moneylogdb`, 스키마 `moneylog`.
이 기능이 쓰는 테이블은 **3개**다 — `tbl_user`, `tbl_user_session`, `tbl_user_login_history`.
`tbl_user_expend_group`은 FR-106(기본 지출유형 10종)에서 **쓰기만** 한다.
스키마 변경은 없다 — `success` 컬럼은 직전 커밋에서 이미 들어갔다.

**Testing**: JUnit 5. `money-backend-app`은 `spring-boot-starter-test` +
`spring-boot-starter-webmvc-test`(Boot 4에서 `@AutoConfigureMockMvc`가 여기로 옮겨졌다).
통합 테스트는 실 PostgreSQL을 쓴다 — `data-mod`의 스키마 IT 77건이 이미 그 방식이다.

**Target Platform**: Linux/Windows JVM 서버. 백엔드 `:8081`, `/api/v1/*`.

**Project Type**: Spring Boot 멀티모듈 웹 서비스(백엔드 API). 프론트(`money-app`)는 이 기능의 범위 밖이다.

**Performance Goals**: 명세에 수치 목표가 없다. 이 기능의 성능 성격은
"요청마다 DB 세션 조회 1회가 추가된다"는 것뿐이며, 인덱스는 `ux_user_session_id`가 이미 있다.
목표 수치는 정하지 않는다 — 근거 없는 숫자를 적으면 검증도 근거 없이 한다.

**Constraints**:
- 응답은 예외 없이 `{ resCode, data }`. 성공 HTTP 200 + `resCode 200`, **비즈니스·검증 실패도 HTTP 200** +
  4자리 코드, 서버 오류만 HTTP 500 + `9000` (SC-101).
- `PUT` 금지. 수정은 `PATCH`이고 omit한 필드는 유지.
- 비밀번호는 bcrypt 해시만 저장하고 어떤 응답에도 싣지 않는다(SC-107).
- 회원당 활성 세션 1건. DB 부분 유니크 인덱스 `ux_user_session_active`가 최종 방어선이다.
- 토큰 해시 컬럼은 `varchar(100)` — 해시 표현이 100자를 넘으면 안 된다.
- 로그에 비밀번호·토큰을 남기지 않는다(SC-111).

**Scale/Scope**: API 16건 · FR 28건(FR-101~128) · SC 11건 · 사용 테이블 3개.
User Story 4개를 P1→P4 순으로 독립 인도 가능한 단위로 자른다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS(조건부). `money-app`은 이 기능에서 건드리지 않는다. 다만 설계 명세
      `_공통.md`의 구현 위치 가이드가 `common-mod`에 `MemberSessionService`를 두라고 적고 있는데,
      `common-mod`는 `data-mod`를 의존하지 않으므로 그대로 두면 **역방향 의존이 된다.**
      Phase 0에서 배치를 다시 정하고 명세 개정 대상으로 올렸다 → Complexity Tracking 참고.
- [x] **II. 레이어 흐름** — PASS. `Controller → Service → Repository`로 두고 Controller가
      Repository를 직접 부르지 않는다. Entity는 경계를 넘지 않으며 Request/Response DTO로 분리한다.
      `pw`가 응답에 실리지 않는 것은 DTO 분리로 구조가 보장한다(SC-107).
- [x] **III. 응답 규격** — PASS. 16건 전부 `{ resCode, data }`. 에러코드는 Enum,
      실패는 커스텀 예외 → `@RestControllerAdvice` 한 곳에서 응답으로 변환한다. `PUT`은 쓰지 않는다.
- [x] **IV. 로깅** — PASS. 백엔드용 `logback-spring.xml`로 콘솔+파일을 동시에 쓰고,
      요청~응답 로깅은 AOP 하나가 담당한다. 비밀번호·토큰은 마스킹한다.
- [x] **V. 명세 우선** — PASS(선행 조건 있음). `프로젝트설계/phase1-회원/` 16건이 이미 있고
      2026-09-02 커밋에서 개정됐다. **다만 착수 전에 개정해야 할 항목 3건**이 남아 있다 →
      아래 "명세 선행 개정" 참고.
- [x] **VI. 스키마 덤프** — PASS. DB 영향도를 `sql/schema-moneylogdb.sql`로 확인했고
      **이 기능에서 스키마는 바뀌지 않는다.** `success` 컬럼은 커밋 `1edf308`에서 이미 반영·덤프됐다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 `common-mod`에는 DB를 모르는 것만 남았다(`ErrorCode`, 예외, 응답 래퍼,
      로깅 Aspect, `JwtTokenProvider`). 세션을 읽는 코드는 전부 `money-backend-app`에 있다.
      역방향 의존 0건.
- [x] **II** — [contracts/](./contracts/)의 16건 모두 Controller가 Service만 부른다.
- [x] **III** — 16건의 성공·실패 응답을 계약 문서에 코드까지 적었다. `PUT` 0건.
- [x] **IV** — 마스킹 대상(`password`·`newPassword`·`passwordConfirm`·`accessToken`·`refreshToken`·
      `Authorization` 헤더)을 계약에 명시했다.
- [x] **V** — 선행 개정 3건을 `tasks.md`의 첫 작업으로 넣도록 [quickstart.md](./quickstart.md)에 적었다.
- [x] **VI** — 스키마 무변경. 덤프 재생성 작업 없음.

### 명세 선행 개정 (착수 전, 원칙 V)

구현 전에 `프로젝트설계/`를 먼저 고쳐야 하는 3건이다. 스펙이 이미 결론을 적어 두었으므로
개정 내용은 확정돼 있고, 남은 것은 반영뿐이다.

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `_공통.md` § 구현 위치 가이드 | `common-mod/.../security/MemberSessionService.java` → `money-backend-app` 쪽 경로 | 원칙 I. `common-mod`는 `data-mod`를 의존하지 않는다 |
| 2 | `phase1-회원/1.2-MemberSignup.md` | 비밀번호 저장 대상 `tbl_member.pw` → `tbl_user.pw` | 레거시 테이블 이름. 001에서 `tbl_user`로 확정됐다 |
| 3 | `phase1-회원/1.3-MemberLogin.md` | 세션 테이블 `tbl_member_session` → `tbl_user_session` | 위와 같음 |

2·3은 이번 리뷰·clarify에서 다루지 않은 잔존 오기다. 같은 절에 있는 `_공통.md`의 표기는
`Phase 8 Polish` 커밋에서 이미 고쳐졌는데 개별 API 문서에는 남아 있다.

## Project Structure

### Documentation (this feature)

```text
specs/002-backend-member-auth/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 12건
├── data-model.md        # Phase 1 — 쓰는 테이블 3개와 상태 전이
├── quickstart.md        # Phase 1 — 검증 시나리오
├── contracts/
│   ├── api-contract.md      # API 16건의 요청·응답·실패 코드
│   └── auth-pipeline.md     # 토큰 발급·검증·갱신·폐기 순서와 경계
├── spec.md              # 입력
└── tasks.md             # /speckit-tasks 산출물 (이 명령이 만들지 않는다)
```

### Source Code (repository root)

이 기능이 **새로 만들거나 고치는** 경로만 적는다. `+`는 신규, `~`는 수정이다.

```text
common-mod/src/main/java/com/dbdomino/moneylog/common/
├── api/
│   └── RestResponseDto.java                    ~ 실패 응답 팩토리를 ErrorCode 기반으로 정리
├── error/
│   ├── ErrorCode.java                          + 4자리 코드 Enum (1001~9001)
│   ├── BusinessException.java                  + ErrorCode 를 들고 다니는 런타임 예외
│   └── GlobalExceptionHandler.java             + @RestControllerAdvice
├── logging/
│   ├── ApiLoggingAspect.java                   + 요청~응답 AOP 로깅
│   └── SensitiveMasker.java                    + 비밀번호·토큰 마스킹
└── security/
    └── JwtTokenProvider.java                   + JWT 발급·파싱 (DB 를 모른다)

app-mod/money-backend-app/src/main/
├── java/com/dbdomino/moneylog/backend/
│   ├── config/
│   │   ├── SecurityConfig.java                 + SecurityFilterChain·PasswordEncoder
│   │   ├── JwtProperties.java                  + 서명키·만료(1일/7일) 바인딩
│   │   └── BackendAuditorAware.java            + SecurityContext → id_key 공급
│   ├── security/
│   │   ├── TokenAuthenticationFilter.java      + Access Token 10단계 검증
│   │   ├── AuthPrincipal.java                  + memberId·idKey·role·sessionId
│   │   └── RestAuthEntryPoint.java             + 인증 실패도 { resCode, data } 로
│   ├── controller/
│   │   ├── HealthController.java               ~ 1.1 (이미 있음, 그대로 둔다)
│   │   ├── AuthController.java                 + 1.2~1.6·1.9~1.11
│   │   ├── MemberController.java               + 1.7·1.8
│   │   └── AdminMemberController.java          + 1.12~1.16
│   ├── service/
│   │   ├── AuthService.java                    + 가입·로그인·찾기·재설정
│   │   ├── MemberSessionService.java           + 세션 생성·검증·갱신·폐기
│   │   ├── MemberService.java                  + 본인 조회·수정
│   │   ├── AdminMemberService.java             + 관리자 회원 관리
│   │   ├── LoginHistoryService.java            + 성공·실패 이력 기록
│   │   └── DefaultExpendGroupService.java      + 기본 지출유형 10종·아이콘 복사
│   ├── dto/
│   │   ├── request/                            + API 별 Request DTO
│   │   └── response/                           + API 별 Response DTO
│   └── mapper/
│       └── MemberMapper.java                   + Entity ↔ DTO (MapStruct)
└── resources/
    ├── application.yml                         ~ jwt.* 프로퍼티 추가
    ├── logback-spring.xml                      + 콘솔+파일 동시 기록
    └── seed/expend-group-icons/                  (이미 10개 커밋되어 있다)

data-mod/src/main/java/com/dbdomino/moneylog/data/
├── config/JpaAuditingConfig.java               ~ auditorAware() 임시 구현 제거
├── entity/BaseAuditEntity.java                 ~ 임시 @Setter 제거
└── repository/
    ├── UserRepository.java                     ~ 조회 메서드 추가
    ├── UserSessionRepository.java              ~ 조회·폐기 메서드 추가
    └── UserLoginHistoryRepository.java         ~ 저장만 쓴다

app-mod/money-backend-app/src/test/java/com/dbdomino/moneylog/backend/
├── auth/       + 토큰 파이프라인 통합 테스트 (US1)
├── member/     + 가입·본인 정보 (US2)
├── recovery/   + 아이디·비밀번호 찾기 (US3)
└── admin/      + 관리자 회원 관리 (US4)

data-mod/src/test/java/com/dbdomino/moneylog/data/schema/
└── AbstractSchemaIT.java                       ~ stampAudit() 임시 조치 제거
```

**Structure Decision**: 기존 멀티모듈 구조를 그대로 쓴다. 새 모듈을 만들지 않는다.

배치 기준은 **"DB를 아는가"** 하나다.

- `common-mod` — DB를 모르는 것만. 에러코드·예외·전역 처리·로깅 Aspect·`JwtTokenProvider`.
  `JwtTokenProvider`는 문자열을 서명하고 파싱할 뿐 세션을 조회하지 않으므로 여기 둔다.
- `money-backend-app` — 세션·회원을 읽고 쓰는 전부. 인증 필터, 세션 서비스, Controller·Service·DTO.
- `data-mod` — Entity·Repository. 이 기능에서는 조회 메서드 추가와 **임시 조치 제거**만 한다.
- `core-mod` — 건드리지 않는다. 지금은 `package-info.java` 하나뿐인 빈 모듈이고, 이 기능에
  "DB 사용 모델의 추상 설계"를 넣을 만한 대상이 없다. 억지로 채우면 통과 지점만 하나 늘어난다.

## Complexity Tracking

> 헌장 원칙을 어기는 것은 아니지만, **상위 설계 명세와 다르게 가는 결정 1건**을 기록한다.
> 원칙을 지키기 위해 명세를 고치는 방향이므로 위반이 아니라 개정 요구다.

| 항목 | 명세가 적은 것 | 이 계획이 정한 것 | 이유 |
|---|---|---|---|
| `MemberSessionService` 위치 | `common-mod/.../security/MemberSessionService.java` (`_공통.md` 구현 위치 가이드) | `app-mod/money-backend-app/.../service/MemberSessionService.java` | 이 서비스는 `tbl_user_session`을 읽고 쓴다. `common-mod`에 두면 `common-mod → data-mod` 의존이 생겨 **헌장 원칙 I의 의존 방향이 역행**한다(`data-mod → common-mod`가 정방향이다). 명세 쪽을 고친다 |
| `TokenAuthenticationFilter` 위치 | `common-mod/.../security/` | `money-backend-app/.../security/` | 위와 같다. 필터가 DB 세션을 대조해야 하므로(`_공통.md` 검증 순서 5~9단계) `data-mod`가 필요하다 |
| `JwtTokenProvider` 위치 | `common-mod/.../security/` | **명세대로 `common-mod`** | 서명·파싱만 하고 DB를 모른다. 옮길 이유가 없다 |

기각한 대안 2가지.

- **`common-mod`가 `data-mod`를 의존하게 한다** — 원칙 I을 정면으로 어긴다. `common-mod`는 전 모듈이
  공유하는 최하위 모듈이라 여기에 JPA가 들어오면 `money-app`(프론트)까지 DB 계층을 끌고 간다.
  프론트가 DB에 직접 붙지 못하게 하는 것이 원칙 I의 목적인데 그 방어선이 무너진다.
- **세션 조회를 인터페이스로 `common-mod`에 두고 구현만 백엔드에 둔다** — 의존 방향은 지켜지지만,
  이 기능에서 그 인터페이스를 쓰는 모듈은 `money-backend-app` 하나뿐이다. 구현체가 하나뿐인
  추상을 최하위 모듈에 두면 통과 지점만 늘고 얻는 것이 없다. `core-mod`가 채워지고 여러 앱이
  같은 세션 규칙을 공유하게 되는 시점에 다시 검토한다.
