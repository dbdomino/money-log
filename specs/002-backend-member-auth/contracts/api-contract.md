# API 계약: 회원 가입·인증·토큰·관리자 회원 관리

**Feature**: `002-backend-member-auth` | **Date**: 2026-09-02 | **Phase**: 1

이 문서는 **구현이 지켜야 하는 계약**을 정한다. 각 API의 전체 필드 표는
`프로젝트설계/기능명세상세-백엔드/phase1-회원/` 의 개별 문서가 소유하며 여기서 복제하지 않는다 —
같은 표를 두 곳에 두면 반드시 갈라진다. 여기 적는 것은 **16건에 공통으로 걸리는 규칙**과
**설계 문서만 봐서는 갈리는 지점**이다.

---

## 1. 인가 경계

`SecurityFilterChain`에 이 표 그대로 선언한다. Controller 애너테이션으로 흩지 않는다.

| 경로 | 메서드 | 인가 | 해당 API |
|---|---|---|---|
| `/api/v1/ha` | GET | `permitAll` | 1.1 |
| `/api/v1/auth/signup` | POST | `permitAll` | 1.2 |
| `/api/v1/auth/login` | POST | `permitAll` | 1.3 |
| `/api/v1/auth/refresh` | POST | `permitAll` | 1.5 |
| `/api/v1/auth/find-id` | POST | `permitAll` | 1.9 |
| `/api/v1/auth/find-password` | POST | `permitAll` | 1.10 |
| `/api/v1/auth/reset-password` | POST | `permitAll` | 1.11 |
| `/api/v1/auth/validate` | GET | `authenticated` | 1.4 |
| `/api/v1/auth/revoke` | POST | `authenticated` | 1.6 |
| `/api/v1/members/me` | GET·PATCH | `authenticated` | 1.7·1.8 |
| `/api/v1/admin/**` | 전부 | `hasRole('ADMIN')` (= `role == 1`) | 1.12~1.16 |
| 그 밖의 전부 | — | `authenticated` | 003~006이 붙을 자리 |

마지막 줄이 중요하다. **기본값을 `authenticated`로 두어** 003~006의 API가 추가될 때
경로를 빠뜨려도 열리지 않게 한다. 반대(기본 `permitAll` + 보호 경로 열거)로 두면
빠뜨린 API가 조용히 공개된다.

`permitAll` 7건은 인증 필터를 **통과는 하되 토큰이 없어도 막지 않는다**. 토큰이 실려 오면
파싱해 `SecurityContext`를 채우되, 실패해도 익명으로 진행시킨다(가입·로그인 경로에서
낡은 토큰 때문에 막히면 안 된다).

---

## 2. 응답 규격 (16건 공통)

```json
{ "resCode": 200, "data": { } }
```

| 구분 | HTTP | `resCode` | `data` |
|---|---|---|---|
| 성공 | 200 | `200` | API별 객체 |
| 비즈니스·검증 실패 | **200** | 4자리 코드 | `{ "message": "..." }` |
| 서버 오류 | 500 | `9000` | `{ "message": "..." }` |

**비즈니스 실패가 HTTP 200인 것이 이 계약의 특이점이다**(SC-101). 예외를 던졌는데 200이
나가므로 `GlobalExceptionHandler`가 `BusinessException`을 200으로 매핑해야 한다.
Spring Security의 기본 401/403 응답도 가로채서 이 규격으로 바꾼다 →
`RestAuthEntryPoint`(인증 실패 → `1001`)와 `AccessDeniedHandler`(인가 실패 → `1002`).

이 두 개를 빠뜨리면 관리자 API에 일반 토큰으로 접근했을 때 `{ resCode: 1002 }`가 아니라
Spring 기본 403 JSON이 나가고, SC-106("전부 `1002`")이 깨진다.

### 목록 응답 (1.13만 해당)

```json
{ "resCode": 200,
  "data": { "list": [ {...} ], "offset": 0, "limit": 20, "totalCount": 137 } }
```

- `list`는 object 배열이다.
- `page`·`totalPages`는 **싣지 않는다**.
- `totalCount`는 현재 페이지 건수가 아니라 검색 조건 전체 건수다.

---

## 3. 토큰 응답 필드 (1.3·1.5 공통)

두 API가 같은 형태를 돌려준다. 필드명이 갈리면 프론트가 분기해야 하므로 **동일하게** 맞춘다.

| 필드 | 타입 | 값 |
|---|---|---|
| `accessToken` | string | JWT (HS256) |
| `tokenType` | string | `Bearer` 고정 |
| `expiresIn` | number | 초. 기본 `86400`(1일) |
| `refreshToken` | string | 불투명 랜덤 문자열 (JWT 아님) |
| `refreshExpiresIn` | number | 초. 기본 `604800`(7일) |

1.3은 여기에 `memberId`·`nickname`·`role`을 더한다. 1.5는 토큰 5개만 돌려준다.

`expiresIn`은 **남은 초**다. 설정값을 그대로 쓰는 것이 아니라 발급 시각 기준으로 계산한다 —
1.5의 Rotation에서 방금 발급한 토큰이므로 실질적으로 설정값과 같지만, 의미를 남은 시간으로
고정해 두면 나중에 만료를 연장하지 않는 갱신을 넣어도 계약이 흔들리지 않는다.

---

## 4. PATCH omit 규칙 (1.8·1.15)

전송한 필드만 갱신하고 omit한 필드는 유지한다. **`null`을 보낸 것과 omit한 것을 구분해야 한다.**

| 요청 | 동작 |
|---|---|
| 필드 자체가 Body에 없음 | 기존 값 유지 |
| `"email": null` | `null`로 갱신(값 지움) |
| `"email": "a@b.c"` | 그 값으로 갱신 |

이 구분이 없으면 "이메일을 지우는" 조작이 불가능해진다. Jackson 기본 역직렬화는 둘 다
`null`이 되므로 `JsonNullable` 같은 래퍼나 `Map<String, Object>` 수신 후 `containsKey` 판정이
필요하다. 어느 쪽을 쓸지는 구현 단계에서 정하되, **한 방식으로 통일**한다.

`PUT`은 쓰지 않는다(헌장 원칙 III).

---

## 5. 실패 코드 매핑

전체 목록은 [spec.md § 이 기능이 쓰는 에러코드](../spec.md)에 있다. 여기서는
**구현이 헷갈리는 3쌍**만 못박는다.

### `1001` vs `1006` — 토큰이 안 먹힐 때

| 상황 | 코드 |
|---|---|
| Bearer 없음 · 서명 실패 · JWT `exp` 만료 · `access_expires_at` 지남 | `1001` |
| 세션 행 없음 · 해시가 NULL · 해시 불일치 | `1006` |

기준은 "**토큰 자체가 문제인가(1001), 세션이 무효인가(1006)**"다.
JWT는 멀쩡한데 다른 곳에서 로그인해 세션이 갈린 경우가 `1006`이다.

### `1005` vs `1006` — 갱신(1.5)이 실패할 때

`1.5-MemberTokenRefresh.md`의 실패 표에 둘 다 있어 갈리는 지점이다.

| 상황 | 코드 |
|---|---|
| Refresh 해시가 NULL·빈값 (로그아웃했다) | `1005` |
| 해시 불일치 · `refresh_expires_at` 지남 | `1005` |
| 다른 곳에서 로그인해 세션이 교체됐다 | `1006` |

`_공통.md`의 Refresh 검증 순서 3단계가 "해시가 NULL·빈값이면 `1005`"라 **로그아웃 후 갱신은
`1005`**다(US1 시나리오 6).

### `2001` vs `9001` — 찾기(1.9~1.11)가 실패할 때

| 상황 | 코드 |
|---|---|
| 이메일 형식이 어긋남 · 필수 필드 누락 | `9001` |
| 형식은 맞는데 일치하는 회원이 없음 | `2001` |
| `memberId`+`nickname` 불일치 | `2001` |
| 대상 회원이 비활성 | `1004` |

계정 존재 여부를 감추는 통일 응답은 **쓰지 않는다**(스펙 Edge Case에서 확정).

---

## 6. 아이디 찾기(1.9)의 마스킹

성공 응답은 원문 아이디를 그대로 주지 않는다.

| 필드 | 값 |
|---|---|
| `memberId` | 일부를 가린 값. 예: `user01` → `use***01` |
| `masked` | `true` |

SC-109가 "응답의 `memberId`가 **원문과 다르고** `masked=true`"를 요구하므로,
마스킹 규칙은 **원문과 반드시 달라져야** 한다. 아이디가 4자로 짧아도 가린 결과가 원문과
같아지지 않도록 규칙을 정한다(예: 앞 3자 + `***` + 뒤 2자, 길이가 모자라면 뒤쪽 우선).

---

## 7. 관리자 API의 추가 규칙 (1.12~1.16)

| 규칙 | 내용 | 근거 |
|---|---|---|
| 감사 컬럼 | 관리자가 만든·고친 행의 `created_by`/`updated_by`는 **관리자의 `id_key`** | FR-121·US4 시나리오 5 |
| 기본 지출유형 | 1.12도 1.2와 **똑같이** 10종을 만든다 | FR-106 |
| 정지(1.16) | `active=false`로 표시. 행·데이터를 지우지 않는다 | FR-118 |
| 정지 시 세션 | 그 회원의 활성 세션을 폐기한다 | FR-119 |
| 재정지 | 이미 `active=false`면 `9001` | Edge Case |
| 자기 정지 | 관리자가 자기 계정을 정지하면 거절 | Edge Case |
| 페이징 | `offset`·`limit` 필수. `limit<=0` · `offset<0` · `offset % limit != 0` → `9001` | FR-120 |
| 검색 | `memberId`·`nickname` 부분 일치. 둘 다 있으면 **AND** | FR-120 |

**자기 정지 거절의 코드가 설계 명세에 배정되어 있지 않다.** `1.16-AdminMemberDeactivate.md`의
실패 표는 `1001`·`1002`·`2001`·`9001` 넷뿐이고 자기 정지 항목이 없다. 스펙 Edge Case가
"거절한다"고만 적었으므로 **`9001`(잘못된 요청)로 처리**한다 — 이미 정지된 회원 재정지와 같은
코드다. 별도 코드를 새로 배정하지 않는 이유는 둘 다 "이 정지 요청은 성립하지 않는다"는 같은
성격이고, 프론트가 취할 조치도 같기 때문이다.

---

## 8. 로깅 마스킹 대상

AOP 로깅이 남기는 요청·응답에서 아래를 가린다(FR-128·SC-111).

| 위치 | 이름 |
|---|---|
| 요청 Body | `password`, `passwordConfirm`, `newPassword`, `refreshToken` |
| 요청 헤더 | `Authorization` |
| 응답 Body | `accessToken`, `refreshToken` |
| (방어용) | `pw` — Entity 필드명. DTO 경계를 넘지 않지만 목록에 둔다 |

가리는 방식은 **값 전체를 고정 문자열로 대체**한다(`***`). 앞뒤 일부를 남기면 토큰 길이나
비밀번호 첫 글자가 로그에 남는다. 아이디 찾기 응답의 `memberId` 마스킹(§6)과는 목적이 다르므로
같은 규칙을 쓰지 않는다.
