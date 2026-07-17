# 회원 상세 조회

> API: `AdminMemberGet` · Phase 1 · 구현순서 13  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 회원 상세 조회 |
| 구현순서 | 13 |
| API 이름 | `AdminMemberGet` |
| Method | `GET` |
| URL | `/api/v1/admin/members/{memberId}` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 조회 대상 회원 아이디 |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` (관리자) |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Member](../_공통.md#공유-타입--member)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 아이디 |
| `nickname` | string | 닉네임 |
| `email` | string \| null | |
| `phone` | string \| null | |
| `intro` | string \| null | |
| `role` | number | `1` \| `3` |
| `active` | boolean | 활성 여부 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 2001 | 회원 없음 |

## 예시

### 성공

**Request**

```http
GET /api/v1/admin/members/user01 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (관리자)
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "user01",
    "nickname": "가계부초보",
    "email": "user01@example.com",
    "phone": null,
    "intro": "절약 중",
    "role": 3,
    "active": true
  }
}
```

### 실패

```json
{
  "resCode": 2001,
  "data": {
    "message": "회원을 찾을 수 없습니다"
  }
}
```

## 비고

- 관리자가 **임의 회원 1건** 상세 조회.
- Path only (Query 혼용 없음).
