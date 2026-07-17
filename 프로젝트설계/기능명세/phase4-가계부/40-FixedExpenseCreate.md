# 고정지출 등록

> API: `FixedExpenseCreate` · Phase 4 · 구현순서 40  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 고정지출 등록 |
| 구현순서 | 40 |
| API 이름 | `FixedExpenseCreate` |
| Method | `POST` |
| URL | `/api/v1/fixed-expenses` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

없음

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
| `name` | string | ✅ | 고정지출 이름 (예: 월세) |
| `paymentMethodId` | number | ✅ | 지출 수단 |
| `amount` | number | ✅ | 기본 금액 (원) |
| `paymentDayOfMonth` | number | ✅ | 매달 결제일 1~31 |
| `content` | string | ✅ | 내용 |
| `expendGroupId` | number | ✅ | 지출유형 |
| `startYearMonth` | string | ✅ | 적용 시작 `YYYY-MM` |
| `endYearMonth` | string | ✅ | 적용 종료 `YYYY-MM` |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § FixedExpense](../_공통.md#공유-타입--fixedexpense)

### 성공 (`resCode: 200`) — `data`

FixedExpense 전체 + `paymentMethodName`, `expendGroupName` 스냅샷

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3401 | 기간·결제일 검증 실패 |
| 3103 | 지출유형 없음 |
| 3003 | 수단 없음 |

## 예시

### 성공

**Request**

```http
POST /api/v1/fixed-expenses HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "월세",
  "paymentMethodId": 1,
  "amount": 500000,
  "paymentDayOfMonth": 5,
  "content": "원룸 월세",
  "expendGroupId": 2,
  "startYearMonth": "2026-01",
  "endYearMonth": "2026-12"
}
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "fixedExpenseId": 1,
    "name": "월세",
    "paymentMethodId": 1,
    "paymentMethodName": "국민카드",
    "amount": 500000,
    "paymentDayOfMonth": 5,
    "content": "원룸 월세",
    "expendGroupId": 2,
    "expendGroupName": "주거",
    "startYearMonth": "2026-01",
    "endYearMonth": "2026-12"
  }
}
```

### 실패

```json
{
  "resCode": 3401,
  "data": {
    "message": "종료 연월은 시작 연월보다 이후여야 합니다"
  }
}
```

## 비고

- 매달 DB에 행을 넣지 않고 **조회 시 전개**한다.
- `LedgerMonthlyList`에 `type=FIXED`로 포함.
