# 고정지출 설정 목록

> API: `FixedExpenseList` · Phase 4 · 구현순서 41  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 고정지출 설정 목록 |
| 구현순서 | 41 |
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
| `offset` | number | ✅ | 건너뛸 건수. 0 이상, `limit`의 배수 |
| `limit` | number | ✅ | 가져올 건수. 1 이상 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

공통 래퍼 + [_공통.md § 페이징](../_공통.md#공유-타입--페이징)  
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `list` | array (object[]) | FixedExpense 목록 |
| `offset` | number | 이번 조회에서 건너뛴 건수. 요청 `offset`과 동일(보정 시 보정값). 현재 페이지 환산에 사용 |
| `limit` | number | 이번 조회에서 가져온 최대 건수. 요청 `limit`과 동일(보정 시 보정값). 페이지 크기·전체 페이지 수 환산에 사용 |
| `totalCount` | number | 조건에 맞는 **전체 건수** (`list` 길이·현재 페이지 건수가 아님) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 9001 | offset·limit 누락·범위 오류 |

## 예시

### 성공

**Request**

```http
GET /api/v1/fixed-expenses?offset=0&limit=10 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "list": [
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
    "offset": 0,
    "limit": 10,
    "totalCount": 1
  }
}
```

### 실패

```json
{
  "resCode": 9001,
  "data": {
    "message": "offset과 limit은 필수입니다"
  }
}
```

## 비고

- 목록 응답 표준: `data.list` = **object 배열** ([_공통 § 목록 응답 규칙](../_공통.md#목록-응답-규칙)).
- 페이징: 요청·응답 `offset`/`limit`, 응답 `totalCount`. 현재·전체 페이지는 프론트 환산 ([_공통 § 페이징](../_공통.md#공유-타입--페이징)).
- **설정 화면**용. 가계부 월 목록과 별도.
- 본인 고정지출만 조회.
