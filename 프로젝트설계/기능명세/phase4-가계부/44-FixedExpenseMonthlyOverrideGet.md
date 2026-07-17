# 고정지출 월별 수정 조회

> API: `FixedExpenseMonthlyOverrideGet` · Phase 4 · 구현순서 44  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출) |
| 기능 이름 | 고정지출 월별 수정 조회 |
| 구현순서 | 44 |
| API 이름 | `FixedExpenseMonthlyOverrideGet` |
| Method | `GET` |
| URL | `/api/v1/fixed-expenses/{fixedExpenseId}/monthly-overrides/{year}/{month}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `fixedExpenseId` | number | ✅ | 고정지출 PK |
| `year` | number | ✅ | 연도 (예: 2026) |
| `month` | number | ✅ | 월 1~12 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

## 응답

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `fixedExpenseId` | number | |
| `year` | number | |
| `month` | number | |
| `override` | object \| null | 없으면 `null` |
| `override.amount` | number \| null | 해당 월 금액 |
| `override.paymentDayOfMonth` | number \| null | 해당 월 결제일 |
| `override.content` | string \| null | 해당 월 내용 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3402 | 고정지출 없음 |
| 3403 | year·month 범위 오류 |

## 예시

### 성공 (override 있음)

**Request**

```http
GET /api/v1/fixed-expenses/1/monthly-overrides/2026/7 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "fixedExpenseId": 1,
    "year": 2026,
    "month": 7,
    "override": {
      "amount": 550000,
      "paymentDayOfMonth": 10,
      "content": "7월만 관리비 포함"
    }
  }
}
```

### 성공 (override 없음)

```json
{
  "resCode": 200,
  "data": {
    "fixedExpenseId": 1,
    "year": 2026,
    "month": 8,
    "override": null
  }
}
```

## 비고

- 특정 연·월만 다른 금액·결제일·내용 적용.
- `override=null`이면 기본값(`FixedExpenseUpdate`) 사용.
