# 로그아웃 (JWT Token 비활성화)

> API: `MemberTokenRevoke` · Phase 1 · 구현순서 6  
> 인덱스: [README.md](../README.md) · [인증·토큰 공통](./_인증-토큰.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | **로그아웃 (JWT Token 비활성화)** |
| 구현순서 | 6 |
| API 이름 | `MemberTokenRevoke` |
| Method | `POST` |
| URL | `/api/v1/auth/revoke` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

> **관리자 회원 정지**(`AdminMemberDeactivate`)와 다르다. 본 API는 **로그인한 사용자가 스스로 JWT를 끊는** 로그아웃이다.

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `message` | string | 예: `로그아웃되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | Access Token 없음·만료 |
| 1006 | 이미 JWT Token 비활성화됨 (해시 NULL) |

## 처리 순서 (서버)

```
1. Access Token 검증 → session_id(sid), member_id(sub) 확보
2. tbl_member_session 해당 행:
   - access_token_hash  = NULL
   - refresh_token_hash = NULL
   - revoked = true
3. 성공 응답
```

→ Access·Refresh **둘 다** 이후 사용 불가.

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/revoke HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "message": "로그아웃되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 1006,
  "data": {
    "message": "이미 종료된 세션입니다"
  }
}
```

## 비고

- **Access Token만** 전달. Body 없음.
- DB 해시 NULL = JWT Token 비활성화. 상세: [_공통 § 로그아웃](../_공통.md#로그아웃-jwt-token-비활성화)
- 클라이언트는 `accessToken`·`refreshToken` 삭제.
- 관련: [04 토큰 검증](./04-MemberTokenValidate.md) · [05 토큰 갱신](./05-MemberTokenRefresh.md) · [_인증·토큰 공통](./_인증-토큰.md)
