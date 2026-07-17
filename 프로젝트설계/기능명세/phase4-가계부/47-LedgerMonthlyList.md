# 월별 가계부 목록 조회

> API: `LedgerMonthlyList` · Phase 4 · 구현순서 47  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출) |
| 기능 이름 | 월별 가계부 목록 조회 |
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
| `year` | number | ✅ | 조회할 가계부 연도 |
| `month` | number | ✅ | 조회할 가계부 월 (1~12) |
| `paymentMethodId` | number | ❌ | 지출·소득·고정지출 **수단**으로 필터 |
| `expendGroupId` | number | ❌ | **지출유형**으로 필터 (지출·고정·할부 행) |
| `dateFrom` | string | ❌ | **결제일(`paymentDate`) 필터 시작일**. 형식 `YYYY-MM-DD`. 이 날짜 **이상**(포함). `year`·`month`로 잡은 달 안에서 추가 좁히기용 |
| `dateTo` | string | ❌ | **결제일(`paymentDate`) 필터 종료일**. 형식 `YYYY-MM-DD`. 이 날짜 **이하**(포함). `dateFrom`과 함께 쓰면 그 사이 결제일만 |
| `keyword` | string | ❌ | 장소·내용 **부분 일치** 검색 |
| `sort` | string | ❌ | 정렬 기준. `paymentDate`(결제일) \| `amount`(금액). 기본 `paymentDate` |
| `order` | string | ❌ | 정렬 방향. `asc` \| `desc`. 기본 `desc` |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)

### 성공 (`resCode: 200`) — `data`

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
| `paymentDate` | string | 해당 행의 **결제일**(또는 고정지출의 그 달 결제일). 형식 `YYYY-MM-DD`. `dateFrom`/`dateTo`·`sort=paymentDate`의 기준 필드 |
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
  "resCode": 200,
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
- `dateFrom`/`dateTo`는 행의 **결제일(`paymentDate`)** 범위다. `year`·`month`가 가계부 달을 정하고, 그 안에서 결제일로 더 좁힐 때 쓴다.
