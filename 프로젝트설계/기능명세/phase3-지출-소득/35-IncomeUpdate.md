# 소득 수정

> API: `IncomeUpdate` · Phase 3 · 구현순서 35  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 소득 수정 |
| 구현순서 | 35 |
| API 이름 | `IncomeUpdate` |
| Method | `PATCH` |
| URL | `/api/v1/incomes/{incomeId}` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `incomeId` | number | ✅ | 소득 PK |

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
| `paymentMethodId` | number | ❌ | 소득 수단 |
| `amount` | number | ❌ | 금액 |
| `paymentDate` | string | ❌ | `YYYY-MM-DD` |
| `content` | string \| null | ❌ | 내용. `null`이면 비움 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

갱신된 Income 전체

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3302 | 소득 없음 |
| 9001 | 잘못된 요청 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/incomes/501 HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
Content-Type: application/json

{
  "amount": 3600000,
  "content": "7월 급여(성과급 포함)"
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
    "amount": 3600000,
    "paymentDate": "2026-07-25",
    "content": "7월 급여(성과급 포함)"
  }
}
```

### 실패

```json
{
  "resCode": 3302,
  "data": {
    "message": "소득 내역을 찾을 수 없습니다"
  }
}
```

## 비고

- PATCH omit 규칙 적용.
- 수단 변경 시 `paymentMethodName` 스냅샷 갱신.
