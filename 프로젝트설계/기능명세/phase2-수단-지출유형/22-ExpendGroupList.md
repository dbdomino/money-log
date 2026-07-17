# 지출유형 목록

> API: `ExpendGroupList` · Phase 2 · 구현순서 22  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 목록 |
| 구현순서 | 22 |
| API 이름 | `ExpendGroupList` |
| Method | `GET` |
| URL | `/api/v1/expend-groups` |
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

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | 기본·사용자 유형 전체 |
| `items[].expendGroupId` | number | PK |
| `items[].name` | string | 이름 |
| `items[].inUse` | boolean | |
| `items[].iconUrl` | string \| null | |
| `items[].defaultGroup` | boolean | 시스템 기본 유형 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-groups HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
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
        "inUse": true,
        "iconUrl": "/api/v1/expend-groups/icons/user01_식비.png",
        "defaultGroup": true
      },
      {
        "expendGroupId": 10,
        "name": "취미",
        "inUse": true,
        "iconUrl": null,
        "defaultGroup": false
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

- 관리 화면용. **사용 중·미사용** 모두 포함.
- `defaultGroup=true` 유형은 회원가입 시 자동 생성.
