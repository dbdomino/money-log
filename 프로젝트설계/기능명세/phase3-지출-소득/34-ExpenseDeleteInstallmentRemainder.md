# 할부 중도상환

> API: `ExpenseDeleteInstallmentRemainder` · Phase 3 · 구현순서 34  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 할부 중도상환 |
| 구현순서 | 34 |
| API 이름 | `ExpenseDeleteInstallmentRemainder` |
| Method | `DELETE` |
| URL | `/api/v1/expenses/installments/{installmentGroupId}/remainder` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `installmentGroupId` | number | ✅ | 할부 그룹 id |

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
| `installmentGroupId` | number | 그룹 id |
| `deletedCount` | number | 삭제된 남은 할부 건수 |
| `message` | string | 예: `남은 할부 9건이 삭제되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3206 | 할부 그룹 없음 |
| 3207 | 삭제할 남은 건 없음 |

## 예시

### 성공

**Request**

```http
DELETE /api/v1/expenses/installments/50/remainder HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "installmentGroupId": 50,
    "deletedCount": 9,
    "message": "남은 할부 9건이 삭제되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 3206,
  "data": {
    "message": "할부 그룹을 찾을 수 없습니다"
  }
}
```

## 비고

- **중도상환**: 오늘 이후(또는 미결제) **남은 할부 건 일괄 삭제**.
- 이미 지난 월의 할부 건은 유지.
- 본인 할부 그룹만 처리.
