# 지출 등록 (할부)

> API: `ExpenseCreateInstallment` · Phase 3 · 구현순서 33  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 지출 등록 (할부) |
| 구현순서 | 33 |
| API 이름 | `ExpenseCreateInstallment` |
| Method | `POST` |
| URL | `/api/v1/expenses/installments` |
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
| `paymentMethodId` | number | ✅ | 지출 수단 |
| `monthlyAmount` | number | ✅ | 월 납부액 (원) |
| `installmentMonths` | number | ✅ | 할부 개월 수 (2 이상) |
| `startYearMonth` | string | ✅ | 시작 연월 `YYYY-MM` |
| `place` | string | ✅ | 장소 |
| `content` | string | ✅ | 내용 (할부 표시용) |
| `expendGroupId` | number | ✅ | 지출유형 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `installmentGroupId` | number | 할부 그룹 id |
| `installmentMonths` | number | 총 개월 |
| `monthlyAmount` | number | 월 납부액 |
| `startYearMonth` | string | 시작 연월 |
| `expenses` | array | 생성된 Expense N건 |
| `expenses[]` | Expense | `installmentIndex` 1~N |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3204 | 개월 수·금액 검증 실패 |
| 3205 | 생성 중 오류 (전체 롤백) |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/expenses/installments HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "paymentMethodId": 1,
  "monthlyAmount": 100000,
  "installmentMonths": 12,
  "startYearMonth": "2026-07",
  "place": "가전매장",
  "content": "노트북",
  "expendGroupId": 5
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "installmentGroupId": 50,
    "installmentMonths": 12,
    "monthlyAmount": 100000,
    "startYearMonth": "2026-07",
    "expenses": [
      {
        "expenseId": 201,
        "paymentMethodId": 1,
        "paymentMethodName": "국민카드",
        "amount": 100000,
        "paymentDate": "2026-07-01",
        "place": "가전매장",
        "content": "노트북 (1/12)",
        "expendGroupId": 5,
        "expendGroupName": "쇼핑",
        "installmentGroupId": 50,
        "installmentIndex": 1,
        "installmentTotal": 12
      }
    ]
  }
}
```

> 응답의 `expenses`는 예시로 1건만 표기. 실제로는 12건 전부 생성.

### 실패

```json
{
  "resCode": 3204,
  "data": {
    "message": "할부 개월 수는 2 이상이어야 합니다"
  }
}
```

## 비고

- **N건 즉시 생성**. 트랜잭션: 1건이라도 실패 시 **전부 롤백**.
- 각 월 `paymentDate`는 `startYearMonth` 기준 매월 1일 (또는 정책일).
- `content`에 `(n/N)` 접미사 자동 부여 가능.
