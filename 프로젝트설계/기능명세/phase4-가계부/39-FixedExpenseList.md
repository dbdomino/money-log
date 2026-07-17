# 고정지출 목록 (관리)

> API: `FixedExpenseList` · Phase 4 · 구현순서 39  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 고정지출 목록 (관리) |
| 구현순서 | 39 |
| API 이름 | `FixedExpenseList` |
| Method | `GET` |
| URL | `/api/v1/fixed-expenses` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

없음

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `page` | number | ✅ | 페이지 (1-based) |
| `pageSize` | number | ✅ | 페이지 크기 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

공통 래퍼 + [_공통.md § 페이징](../_공통.md#공유-타입--페이징)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | FixedExpense 목록 |
| `page` | number | |
| `pageSize` | number | |
| `totalCount` | number | |
| `totalPages` | number | |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 9001 | page·pageSize 누락 |

## 예시

### 성공

**Request**

```http
GET /api/v1/fixed-expenses?page=1&pageSize=10 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "items": [
      {
        "fixedExpenseId": 1,
        "name": "월세",
        "paymentMethodId": 1,
        "amount": 500000,
        "paymentDayOfMonth": 5,
        "content": "원룸 월세",
        "expendGroupId": 2,
        "startYearMonth": "2026-01",
        "endYearMonth": "2026-12"
      }
    ],
    "page": 1,
    "pageSize": 10,
    "totalCount": 1,
    "totalPages": 1
  }
}
```

### 실패

```json
{
  "resCode": 9001,
  "data": {
    "message": "page와 pageSize는 필수입니다"
  }
}
```

## 비고

- **관리 화면**용. 가계부 월 목록과 별도.
- 본인 고정지출만 조회.
