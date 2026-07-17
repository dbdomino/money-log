# 지출 수정

> API: `ExpenseUpdate` · Phase 3 · 구현순서 30  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 지출 수정 |
| 구현순서 | 30 |
| API 이름 | `ExpenseUpdate` |
| Method | `PATCH` |
| URL | `/api/v1/expenses/{expenseId}` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expenseId` | number | ✅ | 지출 PK |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `paymentMethodId` | number | ❌ | 수단 변경 시 스냅샷 갱신 |
| `amount` | number | ❌ | 금액 |
| `paymentDate` | string | ❌ | `YYYY-MM-DD` |
| `place` | string | ❌ | 장소 |
| `content` | string | ❌ | 내용 |
| `expendGroupId` | number | ❌ | 유형 변경 시 스냅샷 갱신 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

갱신된 Expense 전체

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3202 | 지출 없음 |
| 3203 | 할부 **개월·시작월** 변경 시도 (불가) |
| 9001 | 잘못된 요청 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/expenses/101 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "amount": 18000,
  "content": "라떼 포함"
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expenseId": 101,
    "paymentMethodId": 1,
    "paymentMethodName": "국민카드",
    "amount": 18000,
    "paymentDate": "2026-07-17",
    "place": "스타벅스",
    "content": "라떼 포함",
    "expendGroupId": 1,
    "expendGroupName": "식비",
    "installmentGroupId": null,
    "installmentIndex": null,
    "installmentTotal": null
  }
}
```

### 실패

```json
{
  "resCode": 3203,
  "data": {
    "message": "할부 개월 수와 시작 연월은 수정할 수 없습니다. 삭제 후 다시 등록하세요"
  }
}
```

## 비고

- **건별 수정**. 할부 건도 해당 월 1건만 수정.
- 할부 **개월·시작연월 변경 불가** — 삭제 후 재등록 안내.
- `paymentMethodId` 변경 시 `paymentMethodName` 스냅샷 갱신.
