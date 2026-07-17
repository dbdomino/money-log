# 지출 상세 조회

> API: `ExpenseGet` · Phase 3 · 구현순서 31  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 3 (지출·소득) |
| 기능 이름 | 지출 상세 조회 |
| 구현순서 | 31 |
| API 이름 | `ExpenseGet` |
| Method | `GET` |
| URL | `/api/v1/expenses/{expenseId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

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

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Expense](../_공통.md#공유-타입--expense)

### 성공 (`resCode: 0`) — `data`

Expense 전체. 할부 건이면 `installmentGroupId`, `installmentIndex`, `installmentTotal` 채움.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3202 | 지출 없음 또는 타인 소유 |

## 예시

### 성공 (할부 건)

**Request**

```http
GET /api/v1/expenses/205 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expenseId": 205,
    "paymentMethodId": 1,
    "paymentMethodName": "국민카드",
    "amount": 100000,
    "paymentDate": "2026-09-01",
    "place": "가전매장",
    "content": "노트북 할부 3/12",
    "expendGroupId": 5,
    "expendGroupName": "쇼핑",
    "installmentGroupId": 50,
    "installmentIndex": 3,
    "installmentTotal": 12
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

- 할부 그룹의 **개별 월 건**도 동일 API로 조회.
- 수정 화면 진입 시 사용.
