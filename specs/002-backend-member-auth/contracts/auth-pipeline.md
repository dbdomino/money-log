# 인증 파이프라인 계약: 발급·검증·갱신·폐기

**Feature**: `002-backend-member-auth` | **Date**: 2026-09-02 | **Phase**: 1

`_공통.md`가 정한 검증 순서를 구현 관점으로 옮긴 것이다. 순서는 **바꾸지 않는다** —
단계마다 다른 코드가 나가므로 순서가 바뀌면 응답 코드가 바뀐다.

책임 배치는 [research.md §5](../research.md)에서 정했다. 요약하면 **DB를 아는 것은
`money-backend-app`, 모르는 것은 `common-mod`** 다.

---

## 0. 토큰의 형태

| | Access Token | Refresh Token |
|---|---|---|
| 형식 | JWT (HS256) | 불투명 랜덤 문자열 |
| 생성 | `JwtTokenProvider` (`common-mod`) | `SecureRandom` 32바이트 → Base64URL (43자) |
| 클레임 | `sub`(memberId) · `role` · `sid`(sessionId UUID) · `exp` · `iat` | 없음 |
| 기본 수명 | 1일 (86400초) | 7일 (604800초) |
| DB 저장 | `access_token_hash` | `refresh_token_hash` |
| 저장 형태 | **SHA-256 hex 소문자 64자** | 동일 |

**원문은 어디에도 저장하지 않는다.** 해시가 `varchar(100)`에 들어가고, SHA-256이 결정적이라
Refresh를 해시로 조회할 수 있다([research.md §3](../research.md)).

`sub`는 **`memberId`(= `user_id`)** 이지 `id_key`가 아니다. 인증 필터가 `sub`를 `id_key`로
환산해 `SecurityContext`에 함께 싣는다(스펙 Assumptions).

---

## 1. 발급 — 로그인 (1.3)

```text
1. memberId 로 tbl_user 조회
   └ 없음 → 1003 (이력 남기지 않는다 — id_key 를 채울 수 없다)
2. bcrypt matches(요청 password, tbl_user.pw)
   └ 불일치 → 1003 + 이력 1건 (success=false)
3. tbl_user.active 확인
   └ false → 1004 + 이력 1건 (success=false)
4. 그 회원의 활성 세션(revoked=false)을 폐기
   └ 두 해시를 NULL, revoked=true. 행은 남긴다
5. 새 session_id(UUID) 생성
6. Access(JWT, sid 포함)·Refresh(랜덤) 발급
7. tbl_user_session INSERT — 해시 2개, 만료 시각 2개, revoked=false
   └ ux_user_session_active 위반 → 짧게 재시도(1회). 재시도도 실패하면 9000
8. 이력 1건 (success=true)
→ memberId·nickname·role + 토큰 5필드
```

**4번과 7번이 한 트랜잭션**이어야 한다. 폐기만 되고 삽입이 실패하면 회원이 로그아웃된 채로 남는다.

**7번의 재시도**는 동시 로그인 경합 때문이다([research.md §10](../research.md)).
두 요청이 같은 순간 "활성 세션 없음"을 보면 하나가 유니크 위반으로 실패하는데,
재시도하면 그때는 상대가 만든 세션이 보여 4번이 정상 동작한다.

**1번과 2·3번의 이력 차이가 FR-127이다.** 아이디 자체가 없으면 `id_key`가 없어 행을 만들 수
없다. 그 시도는 애플리케이션 로그로만 남기고 아이디는 마스킹한다.

---

## 2. 검증 — 로그인 필요 API 전부 (`TokenAuthenticationFilter`)

`_공통.md`의 10단계를 그대로 옮긴다.

```text
 1. Authorization: Bearer 추출          없음        → 1001
 2. JWT 서명·형식 검증                   실패        → 1001
 3. JWT exp 확인                        만료        → 1001
 4. sub(memberId) · sid(sessionId) 추출
 5. tbl_user_session: session_id=sid AND id_key=sub(환산)  없음 → 1006
 6. access_token_hash 가 NULL·빈값       → 1006
 7. sha256(요청 토큰) == access_token_hash  불일치  → 1006
 8. now < access_expires_at             지남       → 1001
 9. tbl_user.active                     false      → 1004
10. (선택) last_accessed_at 갱신
→ SecurityContext 에 AuthPrincipal(memberId, idKey, role, sessionId) 설정
```

### 이 순서에서 놓치기 쉬운 것

| # | 주의 |
|---|---|
| 3 vs 8 | **둘 다 한다.** JWT `exp`와 DB `access_expires_at`이 다를 수 있고, 어느 하나라도 지나면 `1001`이다(스펙 Edge Case) |
| 5 | `sub`는 `memberId`라 `id_key`로 환산해야 조회 조건이 성립한다 |
| 6 vs 7 | 순서를 지킨다. NULL인데 7번을 먼저 하면 NPE거나 항상 불일치가 되어 코드는 같아도 의도가 흐려진다 |
| 9 | 관리자가 방금 정지한 회원의 토큰이 아직 살아 있을 수 있다. 세션 폐기(FR-119)와 **중복 방어**다 |
| 10 | 선택이다. 매 요청 UPDATE가 부담이면 넣지 않는다 — 이 기능의 SC 어디에도 쓰이지 않는다 |

`permitAll` 경로에서도 이 필터는 돈다. 다만 **1번에서 토큰이 없으면 익명으로 통과**시키고,
2~9번의 실패도 막지 않는다. 낡은 토큰을 들고 로그인하러 온 사용자가 막히면 안 된다.

---

## 3. 갱신 — Rotation (1.5)

```text
1. Body refreshToken 필수                  없음   → 9001
2. sha256(요청 refreshToken) 으로 세션 조회   없음   → 1005
3. refresh_token_hash 가 NULL·빈값          → 1005   (로그아웃했다)
4. 해시 일치 확인                            불일치 → 1005
5. now < refresh_expires_at                 지남   → 1005
6. tbl_user.active                          false  → 1004
7. Access·Refresh 를 둘 다 새로 발급하고
   같은 세션 행의 해시 2개·만료 시각 2개를 갱신
→ 토큰 5필드
```

**`session_id`는 바뀌지 않는다.** 같은 행을 갱신하므로 이미 발급된 JWT의 `sid`가 계속 유효하다.
새 행을 만들면 부분 유니크 인덱스에 걸리고, 폐기 후 삽입하면 `session_id`가 바뀌어
"같은 로그인 세션"이라는 의미가 끊긴다.

**Access·Refresh를 둘 다 새로 낸다**(FR-113). Refresh만 남기고 Access만 갱신하면
Refresh가 7일 내내 고정돼 탈취 시 창이 길어진다.

2번이 **해시로 조회**하는 단계라 SHA-256(결정적 해시)이 필수다. bcrypt였다면 이 조회가 성립하지
않는다.

`1005`와 `1006`의 구분은 [api-contract.md §5](./api-contract.md)에 있다.

---

## 4. 폐기 — 로그아웃 (1.6)과 그 밖의 경로

폐기는 **한 가지 동작**이다. 부르는 곳이 넷일 뿐이다.

```text
폐기(세션 행) =
    access_token_hash  ← NULL
    refresh_token_hash ← NULL
    revoked            ← true
  (행은 삭제하지 않는다)
```

| 부르는 곳 | 대상 | 근거 |
|---|---|---|
| 로그아웃 (1.6) | 현재 세션 | FR-114 |
| 재로그인 (1.3) | 그 회원의 활성 세션 | FR-110 |
| 비밀번호 재설정 (1.11) | 그 회원의 활성 세션 | US3 시나리오 3 |
| 관리자 정지 (1.16) | 그 회원의 활성 세션 | FR-119 |

**해시 비우기와 `revoked` 세우기를 둘 다 해야 한다.** 해시만 비우면 부분 유니크 인덱스가
그 행을 여전히 활성으로 보아 다음 로그인이 유니크 위반으로 막힌다. `revoked`만 세우면
해시가 남아 검증 7단계를 통과해 버린다.

1.6의 실패 코드는 `1001`(토큰 없음·만료)과 `1006`(이미 폐기됨)이다.
이미 폐기된 세션의 재로그아웃을 성공으로 흘리지 않는다.

---

## 5. 책임 배치

| 클래스 | 모듈 | 하는 일 | DB |
|---|---|---|---|
| `JwtTokenProvider` | `common-mod` | JWT 서명·파싱, 클레임 추출 | 모른다 |
| `TokenAuthenticationFilter` | `money-backend-app` | 위 10단계 | 5~9단계 |
| `MemberSessionService` | `money-backend-app` | 세션 생성·조회·갱신·폐기, 해시 계산 | 전부 |
| `AuthPrincipal` | `money-backend-app` | `SecurityContext`에 실리는 값 | — |
| `BackendAuditorAware` | `money-backend-app` | `AuthPrincipal.idKey` → 감사 컬럼 | — |

**해시 계산은 `MemberSessionService` 한 곳에 가둔다.** 알고리즘을 바꿔야 할 때
고칠 자리가 하나여야 한다.

설계 명세 `_공통.md`는 이 넷을 전부 `common-mod`에 두라고 적고 있는데, 그러면
`common-mod → data-mod` 역방향 의존이 생긴다. 명세를 고치는 것이 이 계획의 결론이다
([plan.md § 명세 선행 개정](../plan.md), [research.md §5](../research.md)).

---

## 6. 설정 값

`application.yml`에 두고 `JwtProperties`로 바인딩한다.

| 키 | 기본값 | 비고 |
|---|---|---|
| `jwt.secret` | — | HS256 서명키. **커밋하지 않는다** — 환경변수로 주입 |
| `jwt.access-token-validity-seconds` | `86400` | 1일 |
| `jwt.refresh-token-validity-seconds` | `604800` | 7일 |

`jwt.secret`은 HS256이므로 **256비트 이상**이어야 한다. 짧은 키를 쓰면 jjwt가 기동 시점에
거부하는데, 그 실패를 런타임까지 미루지 말고 `JwtProperties`에서 길이를 검증해
기동에서 막는다.
