# 월별 통합 목록

> API: `LedgerMonthlyList` · Phase 4 · 구현순서 47  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 월별 통합 목록 |
| 구현순서 | 47 |
| API 이름 | `LedgerMonthlyList` |
| Method | `GET` |
| URL | `/api/v1/ledger/monthly` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

없음 (Query only)

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `year` | number | ✅ | 조회 연도 |
| `month` | number | ✅ | 조회 월 1~12 |
| `paymentMethodId` | number | ❌ | 수단 필터 |
| `expendGroupId` | number | ❌ | 지출유형 필터 |
| `dateFrom` | string | ❌ | `YYYY-MM-DD` 시작일 |
| `dateTo` | string | ❌ | `YYYY-MM-DD` 종료일 |
| `keyword` | string | ❌ | 장소·내용 검색 |
| `sort` | string | ❌ | `paymentDate` \| `amount` (기본 `paymentDate`) |
| `order` | string | ❌ | `asc` \| `desc` (기본 `desc`) |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `year` | number | 조회 연도 |
| `month` | number | 조회 월 |
| `expenseTotal` | number | 해당 월 지출 합계 (일반+할부+고정) |
| `incomeTotal` | number | 해당 월 소득 합계 |
| `list` | array (object[]) | 통합 목록 (정렬·필터 적용 후) |

#### `list[]` 요소

| 필드 | 타입 | 설명 |
|------|------|------|
| `ledgerItemId` | string | 목록 행 식별자 (예: `expense:101`, `fixed:1`, `income:501`) |
| `type` | string | `EXPENSE` \| `INCOME` \| `FIXED` \| `INSTALLMENT` |
| `sourceId` | number | 원본 PK (`expenseId`, `incomeId`, `fixedExpenseId` 등) |
| `paymentDate` | string | `YYYY-MM-DD` |
| `amount` | number | 금액 (지출·소득 부호는 type으로 구분) |
| `paymentMethodId` | number \| null | 소득·지출 수단 |
| `paymentMethodName` | string | 스냅샷 이름 |
| `expendGroupId` | number \| null | 지출·고정지출만 |
| `expendGroupName` | string \| null | |
| `place` | string \| null | 지출·할부만 |
| `content` | string \| null | |
| `fixedExpenseName` | string \| null | `FIXED`일 때 고정지출 이름 |
| `installmentGroupId` | number \| null | 할부 그룹 |
| `installmentIndex` | number \| null | N번째 |
| `installmentTotal` | number \| null | 총 개월 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3501 | year·month 누락·범위 오류 |

## 예시

### 성공

**Request**

```http
GET /api/v1/ledger/monthly?year=2026&month=7&sort=paymentDate&order=desc HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "year": 2026,
    "month": 7,
    "expenseTotal": 650000,
    "incomeTotal": 3500000,
    "list": [
      {
        "ledgerItemId": "income:501",
        "type": "INCOME",
        "sourceId": 501,
        "paymentDate": "2026-07-25",
        "amount": 3500000,
        "paymentMethodId": 3,
        "paymentMethodName": "월급통장",
        "expendGroupId": null,
        "expendGroupName": null,
        "place": null,
        "content": "7월 급여",
        "fixedExpenseName": null,
        "installmentGroupId": null,
        "installmentIndex": null,
        "installmentTotal": null
      },
      {
        "ledgerItemId": "fixed:1",
        "type": "FIXED",
        "sourceId": 1,
        "paymentDate": "2026-07-05",
        "amount": 500000,
        "paymentMethodId": 1,
        "paymentMethodName": "국민카드",
        "expendGroupId": 2,
        "expendGroupName": "주거",
        "place": null,
        "content": "원룸 월세",
        "fixedExpenseName": "월세",
        "installmentGroupId": null,
        "installmentIndex": null,
        "installmentTotal": null
      },
      {
        "ledgerItemId": "expense:205",
        "type": "INSTALLMENT",
        "sourceId": 205,
        "paymentDate": "2026-07-01",
        "amount": 100000,
        "paymentMethodId": 1,
        "paymentMethodName": "국민카드",
        "expendGroupId": 5,
        "expendGroupName": "쇼핑",
        "place": "가전매장",
        "content": "노트북 (3/12)",
        "fixedExpenseName": null,
        "installmentGroupId": 50,
        "installmentIndex": 3,
        "installmentTotal": 12
      }
    ]
  }
}
```

### 실패

```json
{
  "resCode": 3501,
  "data": {
    "message": "year와 month는 필수입니다"
  }
}
```

## 비고

- 목록 응답 표준: `data.list` = **object 배열** ([_공통 § 목록 응답 규칙](../_공통.md#목록-응답-규칙)).
- **일반 지출·할부·소득·고정지출**을 한 테이블로 통합.
- 고정지출은 DB 월별 행 없이 **조회 시 전개** (override 반영).
- `type=INSTALLMENT`는 할부 지출 건 (`installmentGroupId` 있음).
- Path·Query **혼용 없음** (Query only).
