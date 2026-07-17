# 로그인

> API: `MemberLogin` · Phase 1 · 구현순서 3  
> 인덱스: [README.md](../README.md) · [인증·토큰 공통](./_인증-토큰.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 로그인 (토큰 발급) |
| 구현순서 | 3 |
| API 이름 | `MemberLogin` |
| Method | `POST` |
| URL | `/api/v1/auth/login` |
| 권한 | 없음 |
| Content-Type | `application/json` |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 로그인 아이디 |
| `password` | string | ✅ | 비밀번호 평문 (HTTPS). DB 저장값과 [_공통 § 비밀번호](../_공통.md#비밀번호-저장--bcrypt) 절차로 검증 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
인증: [_공통.md § 인증](../_공통.md#인증) · [_공통.md § 토큰 검증](../_공통.md#토큰-검증-공통--로그인-필요-api)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 로그인한 아이디 |
| `nickname` | string | 닉네임 |
| `role` | number | `1` 관리자, `3` 일반 |
| `accessToken` | string | JWT Access Token |
| `tokenType` | string | `Bearer` |
| `expiresIn` | number | Access Token 만료까지 남은 시간(초). 기본 86400(1일) |
| `refreshToken` | string | Refresh Token (재발급용) |
| `refreshExpiresIn` | number | Refresh Token 만료까지 남은 시간(초). 기본 604800(7일) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1003 | 아이디·비밀번호 불일치 |
| 1004 | 비활성화된 계정 |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/login HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "memberId": "user01",
  "password": "Passw0rd!"
}
```

**Response**

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "resCode": 0,
  "data": {
    "memberId": "user01",
    "nickname": "가계부초보",
    "role": 3,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "refreshToken": "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...",
    "refreshExpiresIn": 604800
  }
}
```

### 실패

```json
{
  "resCode": 1003,
  "data": {
    "message": "아이디 또는 비밀번호가 올바르지 않습니다"
  }
}
```

## 비고

- 로그인 성공 시 **새 세션**(`tbl_member_session`) 생성 + **JWT Access Token**·**Refresh Token** 발급. 토큰 해시·만료 시각을 DB에 저장한다.
- **중복 로그인 방지**: 기존 활성 세션 **JWT Token 비활성화**(해시 NULL) 후 새 세션 생성.
- **비밀번호 검증**: `BCryptPasswordEncoder.matches` 로 요청 `password`와 DB 해시 비교. 상세는 [_공통.md § 비밀번호 저장](../_공통.md#비밀번호-저장--bcrypt).
- 로그인 이력(`LoginHistory`)을 기록할 수 있다.
- 비활성(`active=false`) 계정은 로그인 거부 (`1004`).
- 이후 API 호출 시 `Authorization: Bearer <accessToken>`. 서버는 [_공통 § 토큰 검증](../_공통.md#토큰-검증-공통--로그인-필요-api)으로 DB 대조·만료 확인.
- 앱 기동·화면 진입 시 [04 토큰 검증](./04-MemberTokenValidate.md). Access 만료 시 [05 토큰 갱신](./05-MemberTokenRefresh.md). 로그아웃 시 [06 로그아웃 (JWT Token 비활성화)](./06-MemberTokenRevoke.md).
- 관련: [_인증·토큰 공통](./_인증-토큰.md)
