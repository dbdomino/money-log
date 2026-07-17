# 사용 중인 지출유형 목록

> API: `ExpendGroupListActive` · Phase 2 · 구현순서 29  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 사용 중인 지출유형 목록 |
| 구현순서 | 29 |
| API 이름 | `ExpendGroupListActive` |
| Method | `GET` |
| URL | `/api/v1/expend-groups/active` |
| 권한 | 로그인 |
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
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `list` | array (object[]) | `inUse=true` 유형만 |
| `list[].expendGroupId` | number | PK |
| `list[].name` | string | 이름 |
| `list[].iconUrl` | string \| null | |
| `list[].defaultGroup` | boolean | |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-groups/active HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "list": [
      {
        "expendGroupId": 1,
        "name": "식비",
        "iconUrl": "/api/v1/expend-groups/icons/user01_식비.png",
        "defaultGroup": true
      }
    ]
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

- 목록 응답 표준: `data.list` = **object 배열** ([_공통 § 목록 응답 규칙](../_공통.md#목록-응답-규칙)).
- 지출 등록·고정지출 등에서 고를 수 있는 **사용 중** 유형만.
- `inUse=false` 유형은 제외.
