# 회원 목록

> API: `AdminMemberList` · Phase 1 · 구현순서 12  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 회원 목록 |
| 구현순서 | 12 |
| API 이름 | `AdminMemberList` |
| Method | `GET` |
| URL | `/api/v1/admin/members` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

없음

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `page` | number | ✅ | 페이지 번호 (1-based) |
| `pageSize` | number | ✅ | 페이지 크기 (예: 10, 20) |
| `memberId` | string | ❌ | 아이디 부분 일치 검색 |
| `nickname` | string | ❌ | 닉네임 부분 일치 검색 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` (관리자) |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
페이징: [_공통.md § 페이징](../_공통.md#공유-타입--페이징)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | 회원 목록. 요소는 Member + `active` |
| `items[].memberId` | string | 아이디 |
| `items[].nickname` | string | 닉네임 |
| `items[].email` | string \| null | |
| `items[].phone` | string \| null | |
| `items[].intro` | string \| null | |
| `items[].role` | number | `1` \| `3` |
| `items[].active` | boolean | 활성 여부 |
| `page` | number | 현재 페이지 |
| `pageSize` | number | 페이지 크기 |
| `totalCount` | number | 전체 건수 |
| `totalPages` | number | 전체 페이지 수 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 9001 | page·pageSize 누락·범위 오류 |

## 예시

### 성공

**Request**

```http
GET /api/v1/admin/members?page=1&pageSize=10&nickname=가계 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (관리자)
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "items": [
      {
        "memberId": "user01",
        "nickname": "가계부초보",
        "email": "user01@example.com",
        "phone": null,
        "intro": null,
        "role": 3,
        "active": true
      }
    ],
    "page": 1,
    "pageSize": 10,
    "totalCount": 1,
    "totalPages": 1
  }
}
```

### 실패

```json
{
  "resCode": 1002,
  "data": {
    "message": "권한이 없습니다"
  }
}
```

## 비고

- Query **페이징·검색** 패턴 확립 API.
- `memberId`·`nickname`은 AND 조건으로 필터 가능.
- 비밀번호는 목록에 포함하지 않는다.
