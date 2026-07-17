# 토큰 검증

> API: `MemberTokenValidate` · Phase 1 · 구현순서 4  
> 인덱스: [README.md](../README.md) · [인증·토큰 공통](./_인증-토큰.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 토큰 검증 |
| 구현순서 | 4 |
| API 이름 | `MemberTokenValidate` |
| Method | `GET` |
| URL | `/api/v1/auth/validate` |
| 권한 | 로그인 (유효한 Access Token + DB 세션 필요) |
| Content-Type | — (Body 없음) |

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
검증 절차: [_공통.md § 토큰 검증](../_공통.md#토큰-검증-공통--로그인-필요-api) 과 **동일** (JWT + DB 해시·만료 대조)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `valid` | boolean | 항상 `true` (실패 시 아래 `resCode`로 응답) |
| `memberId` | string | JWT `sub` · 로그인 id |
| `role` | number | `1` 관리자, `3` 일반 |
| `expiresIn` | number | Access Token 만료까지 남은 시간(초) |

> 본인 상세 프로필(닉네임·이메일 등)은 [07 본인 정보 조회](./07-MemberGetMe.md)를 사용한다. 본 API는 **토큰·세션 유효성**만 확인한다.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | Access Token 없음·형식 오류·JWT/DB 만료 |
| 1004 | 비활성 계정 |
| 1006 | 세션 무효 (로그아웃·중복 로그인·해시 불일치) |

## 예시

### 성공

**Request**

```http
GET /api/v1/auth/validate HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "valid": true,
    "memberId": "user01",
    "role": 3,
    "expiresIn": 85120
  }
}
```

### 실패 (만료)

```json
{
  "resCode": 1001,
  "data": {
    "message": "로그인이 필요합니다"
  }
}
```

### 실패 (다른 곳에서 로그인)

```json
{
  "resCode": 1006,
  "data": {
    "message": "다른 곳에서 로그인되어 세션이 종료되었습니다"
  }
}
```

## 비고

- **공통 필터**의 Access Token 검증과 같은 규칙이다. 프론트가 앱 기동·화면 진입 시 **명시적으로** 유효성을 확인할 때 호출한다.
- Body·Query 없음. Path variable 없음. ([HTTP Method 규칙 — GET](../../2.1.%20기능명세%20-%20백엔드.md#get--조회))
- 관련: [03 로그인](./03-MemberLogin.md) · [05 토큰 갱신](./05-MemberTokenRefresh.md) · [06 로그아웃](./06-MemberTokenRevoke.md) · [_인증·토큰 공통](./_인증-토큰.md)
