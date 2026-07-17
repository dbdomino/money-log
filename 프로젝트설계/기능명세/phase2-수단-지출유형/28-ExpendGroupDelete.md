# 지출유형 삭제

> API: `ExpendGroupDelete` · Phase 2 · 구현순서 28  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 삭제 |
| 구현순서 | 28 |
| API 이름 | `ExpendGroupDelete` |
| Method | `DELETE` |
| URL | `/api/v1/expend-groups/{expendGroupId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expendGroupId` | number | ✅ | 삭제 대상 PK |

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
| `expendGroupId` | number | 삭제된 PK |
| `message` | string | 예: `지출 유형이 삭제되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3103 | 유형 없음 |
| 3106 | **지출 사용 이력 있음** (삭제 불가) |
| 3107 | 기본 유형 삭제 불가 |

## 예시

### 성공

**Request**

```http
DELETE /api/v1/expend-groups/10 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "expendGroupId": 10,
    "message": "지출 유형이 삭제되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 3106,
  "data": {
    "message": "지출 내역에 사용 중인 유형은 삭제할 수 없습니다"
  }
}
```

## 비고

- **지출(Expense) 사용 이력이 있으면 오류** — 데이터 무결성.
- `defaultGroup=true` 시스템 기본 유형은 삭제 불가.
- 아이콘 **파일은 삭제하지 않고 유지**.
