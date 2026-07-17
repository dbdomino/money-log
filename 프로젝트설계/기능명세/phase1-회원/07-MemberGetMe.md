# 본인 정보 조회

> API: `MemberGetMe` · Phase 1 · 구현순서 7  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 본인 정보 조회 |
| 구현순서 | 7 |
| API 이름 | `MemberGetMe` |
| Method | `GET` |
| URL | `/api/v1/members/me` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| — | — | — | 고정 URL `/members/me` (Path variable 없음) |

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
공유 타입: [_공통.md § Member](../_공통.md#공유-타입--member)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 로그인 id |
| `nickname` | string | 닉네임 |
| `email` | string \| null | 이메일 |
| `phone` | string \| null | 연락처. **하이픈(-) 없이 숫자만** 저장값 |
| `intro` | string \| null | 자기소개 |
| `role` | number | `1` 관리자, `3` 일반 |

`active` 필드는 본인 조회 시 생략하거나 항상 `true`이다.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 토큰 없음·만료 (JWT·DB 만료 시각) |
| 1004 | 비활성 계정 |
| 1006 | 세션 무효 (다른 곳에서 로그인·로그아웃됨) |
| 2001 | 회원 없음 (토큰과 DB 불일치) |

## 예시

### 성공

**Request**

```http
GET /api/v1/members/me HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "memberId": "user01",
    "nickname": "가계부초보",
    "email": "user01@example.com",
    "phone": null,
    "intro": "절약 중",
    "role": 3
  }
}
```

### 실패

```json
{
  "resCode": 1001,
  "data": {
    "message": "로그인이 필요합니다"
  }
}
```

## 비고

- 로그인·GET 패턴 확립용 API.
- **본인 데이터만** 조회. JWT `sub`(memberId) 기준. 진입 전 [_공통 § 토큰 검증](../_공통.md#토큰-검증-공통--로그인-필요-api) 통과 필수.
- 비밀번호는 응답에 포함하지 않는다.
