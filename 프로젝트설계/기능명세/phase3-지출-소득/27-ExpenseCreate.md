# 지출 등록 (일시불)

> API: `ExpenseCreate` · Phase 3 · 구현순서 27  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 지출 등록 (일시불) |
| 구현순서 | 27 |
| API 이름 | `ExpenseCreate` |
| Method | `POST` |
| URL | `/api/v1/expenses` |
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
| `paymentMethodId` | number | ✅ | 지출 수단 PK |
| `amount` | number | ✅ | 금액 (원, 양의 정수) |
| `paymentDate` | string | ✅ | `YYYY-MM-DD` |
| `place` | string | ✅ | 장소 |
| `content` | string | ✅ | 내용 |
| `expendGroupId` | number | ✅ | 지출유형 PK |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Expense](../_공통.md#공유-타입--expense)

### 성공 (`resCode: 0`) — `data`

Expense 전체. 일시불이므로 `installmentGroupId`, `installmentIndex`, `installmentTotal`은 `null`.

| 필드 | 타입 | 설명 |
|------|------|------|
| `expenseId` | number | 생성 PK |
| `paymentMethodId` | number | |
| `paymentMethodName` | string | 등록 시점 수단 이름 스냅샷 |
| `amount` | number | |
| `paymentDate` | string | |
| `place` | string | |
| `content` | string | |
| `expendGroupId` | number | |
| `expendGroupName` | string | 스냅샷 |
| `installmentGroupId` | null | |
| `installmentIndex` | null | |
| `installmentTotal` | null | |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3003 | 수단 없음·비활성 |
| 3103 | 지출유형 없음·비활성 |
| 3201 | 금액·날짜 형식 오류 |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/expenses HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
Content-Type: application/json

{
  "paymentMethodId": 1,
  "amount": 15000,
  "paymentDate": "2026-07-17",
  "place": "스타벅스",
  "content": "아메리카노",
  "expendGroupId": 1
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
    "amount": 15000,
    "paymentDate": "2026-07-17",
    "place": "스타벅스",
    "content": "아메리카노",
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
  "resCode": 3103,
  "data": {
    "message": "지출 유형을 찾을 수 없습니다"
  }
}
```

## 비고

- **일시불** 지출 등록. 할부는 `ExpenseCreateInstallment`.
- 수단·유형 이름을 **스냅샷**으로 저장 (이후 수단명 변경과 무관).
- 본인 데이터만 생성.
