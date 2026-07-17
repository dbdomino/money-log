# 지출·소득 수단 목록 (입력용)

> API: `PaymentMethodListActive` · Phase 2 · 구현순서 20  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출·소득 수단 목록 (입력용) |
| 구현순서 | 20 |
| API 이름 | `PaymentMethodListActive` |
| Method | `GET` |
| URL | `/api/v1/payment-methods/active/{purpose}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `purpose` | string | ✅ | `EXPENSE` (지출) \| `INCOME` (소득) |

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

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | 입력 폼용 활성 수단 |
| `items[].paymentMethodId` | number | PK |
| `items[].name` | string | 이름 |
| `items[].type` | string | `CARD` \| `ACCOUNT` |
| `items[].cardExpiry` | string \| null | 카드일 때 |

`inUse=true` 이고 `deleted=false` 인 항목만 포함.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3001 | purpose 값 오류 (`EXPENSE`/`INCOME` 외) |

## 예시

### 성공

**Request**

```http
GET /api/v1/payment-methods/active/EXPENSE HTTP/1.1
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
        "paymentMethodId": 1,
        "name": "국민카드",
        "type": "CARD",
        "cardExpiry": "2028-12"
      }
    ]
  }
}
```

### 실패

```json
{
  "resCode": 3001,
  "data": {
    "message": "지원하지 않는 purpose 값입니다"
  }
}
```

## 비고

- 지출 등록·소득 등록 폼의 **셀렉트 박스**용.
- `purpose`에 따라 내부 필터 (지출용/소득용 수단 구분).
- Path only (Query 혼용 없음).
