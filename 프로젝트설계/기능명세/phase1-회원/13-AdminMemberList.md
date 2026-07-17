# 회원 목록

> API: `AdminMemberList` · Phase 1 · 구현순서 13  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 회원 목록 |
| 구현순서 | 13 |
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
| `offset` | number | ✅ | 건너뛸 건수. 0 이상, `limit`의 배수 |
| `limit` | number | ✅ | 가져올 건수. 1 이상 (예: 10, 20) |
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
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)  
페이징: [_공통.md § 페이징](../_공통.md#공유-타입--페이징)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `list` | array (object[]) | 회원 목록. 요소는 Member + `active` |
| `list[].memberId` | string | 아이디 |
| `list[].nickname` | string | 닉네임 |
| `list[].email` | string \| null | |
| `list[].phone` | string \| null | 연락처. **하이픈(-) 없이 숫자만** 저장값 |
| `list[].intro` | string \| null | |
| `list[].role` | number | `1` \| `3` |
| `list[].active` | boolean | 활성 여부 |
| `offset` | number | 이번 조회에서 건너뛴 건수. 요청 `offset`과 동일(보정 시 보정값). 현재 페이지 환산에 사용 |
| `limit` | number | 이번 조회에서 가져온 최대 건수. 요청 `limit`과 동일(보정 시 보정값). 페이지 크기·전체 페이지 수 환산에 사용 |
| `totalCount` | number | 검색 조건에 맞는 **전체 건수** (`list` 길이·현재 페이지 건수가 아님) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 9001 | offset·limit 누락·범위 오류 (`limit<=0`, `offset`이 `limit` 배수 아님 등) |

## 예시

### 성공

**Request**

```http
GET /api/v1/admin/members?offset=0&limit=10&nickname=가계 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (관리자)
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "list": [
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
    "offset": 0,
    "limit": 10,
    "totalCount": 1
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

- 목록 응답 표준: `data.list` = **object 배열** ([_공통 § 목록 응답 규칙](../_공통.md#목록-응답-규칙)).
- 페이징: 요청·응답 `offset`/`limit`, 응답 `totalCount`. 현재·전체 페이지는 프론트 환산 ([_공통 § 페이징](../_공통.md#공유-타입--페이징)).
- Query **페이징·검색** 패턴 확립 API.
- `memberId`·`nickname`은 AND 조건으로 필터 가능.
- 비밀번호는 목록에 포함하지 않는다.
