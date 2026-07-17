# 지출 삭제

> API: `ExpenseDelete` · Phase 3 · 구현순서 31  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 지출 삭제 |
| 구현순서 | 31 |
| API 이름 | `ExpenseDelete` |
| Method | `DELETE` |
| URL | `/api/v1/expenses/{expenseId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expenseId` | number | ✅ | 삭제 대상 PK |

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
| `expenseId` | number | 삭제된 PK |
| `message` | string | 예: `지출이 삭제되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3202 | 지출 없음 |

## 예시

### 성공

**Request**

```http
DELETE /api/v1/expenses/101 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expenseId": 101,
    "message": "지출이 삭제되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 3202,
  "data": {
    "message": "지출 내역을 찾을 수 없습니다"
  }
}
```

## 비고

- **1건 물리 삭제**. 할부 건도 해당 월 1건만 삭제.
- 할부 전체 삭제는 건별 삭제 또는 `ExpenseDeleteInstallmentRemainder` 사용.
