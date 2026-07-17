# 지출유형 상세 조회

> API: `ExpendGroupGet` · Phase 2 · 구현순서 24  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 상세 조회 |
| 구현순서 | 24 |
| API 이름 | `ExpendGroupGet` |
| Method | `GET` |
| URL | `/api/v1/expend-groups/{expendGroupId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expendGroupId` | number | ✅ | 지출유형 PK |

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

ExpendGroup 전체 필드 ( [_공통.md § ExpendGroup](../_공통.md#공유-타입--expendgroup) )

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3103 | 유형 없음 또는 타인 소유 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-groups/1 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expendGroupId": 1,
    "name": "식비",
    "inUse": true,
    "iconUrl": "/api/v1/expend-groups/icons/user01_식비.png",
    "defaultGroup": true
  }
}
```

### 실패

```json
{
  "resCode": 3103,
  "data": {
    "message": "지출 유형을 찾을 수 없습니다"
  }
}
```

## 비고

- 본인 소유(또는 본인에게 할당된 기본 유형)만 조회.
