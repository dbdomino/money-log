# 지출유형 목록 (입력용)

> API: `ExpendGroupListActive` · Phase 2 · 구현순서 26  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 목록 (입력용) |
| 구현순서 | 26 |
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
| `Cookie` | ✅ | `JSESSIONID=...` |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | `inUse=true` 유형만 |
| `items[].expendGroupId` | number | PK |
| `items[].name` | string | 이름 |
| `items[].iconUrl` | string \| null | |
| `items[].defaultGroup` | boolean | |

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
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "items": [
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

- 지출 등록·고정지출 등 **입력 폼**용 활성 유형 목록.
- `inUse=false` 유형은 제외.
