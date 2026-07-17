# 소득 등록

> API: `IncomeCreate` · Phase 3 · 구현순서 33  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 소득 등록 |
| 구현순서 | 33 |
| API 이름 | `IncomeCreate` |
| Method | `POST` |
| URL | `/api/v1/incomes` |
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
| `Cookie` | ✅ | `JSESSIONID=...` |
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `paymentMethodId` | number | ✅ | 소득 수단 PK |
| `amount` | number | ✅ | 금액 (원, 양의 정수) |
| `paymentDate` | string | ✅ | `YYYY-MM-DD` |
| `content` | string | ❌ | 내용 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Income](../_공통.md#공유-타입--income)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `incomeId` | number | PK |
| `paymentMethodId` | number | |
| `paymentMethodName` | string | 스냅샷 |
| `amount` | number | |
| `paymentDate` | string | |
| `content` | string \| null | |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3003 | 소득 수단 없음·비활성 |
| 3301 | 금액·날짜 오류 |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/incomes HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
Content-Type: application/json

{
  "paymentMethodId": 3,
  "amount": 3500000,
  "paymentDate": "2026-07-25",
  "content": "7월 급여"
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "incomeId": 501,
    "paymentMethodId": 3,
    "paymentMethodName": "월급통장",
    "amount": 3500000,
    "paymentDate": "2026-07-25",
    "content": "7월 급여"
  }
}
```

### 실패

```json
{
  "resCode": 3003,
  "data": {
    "message": "결제 수단을 찾을 수 없습니다"
  }
}
```

## 비고

- 소득 수단은 `PaymentMethodListActive`의 `purpose=INCOME` 목록에서 선택.
- `paymentMethodName` 스냅샷 저장.
