# 지출·소득 수단 목록 (관리)

> API: `PaymentMethodList` · Phase 2 · 구현순서 17  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출·소득 수단 목록 (관리) |
| 구현순서 | 17 |
| API 이름 | `PaymentMethodList` |
| Method | `GET` |
| URL | `/api/v1/payment-methods` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

없음

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
공유 타입: [_공통.md § PaymentMethod](../_공통.md#공유-타입--paymentmethod)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | 본인 수단 전체 목록 |
| `items[].paymentMethodId` | number | PK |
| `items[].name` | string | 이름 |
| `items[].type` | string | `CARD` \| `ACCOUNT` |
| `items[].inUse` | boolean | 사용 여부 |
| `items[].cardExpiry` | string \| null | |
| `items[].deleted` | boolean | soft delete 여부 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |

## 예시

### 성공

**Request**

```http
GET /api/v1/payment-methods HTTP/1.1
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
        "inUse": true,
        "cardExpiry": "2028-12",
        "deleted": false
      },
      {
        "paymentMethodId": 2,
        "name": "옛 카드",
        "type": "CARD",
        "inUse": false,
        "cardExpiry": "2020-01",
        "deleted": true
      }
    ]
  }
}
```

### 실패

```json
{
  "resCode": 1001,
  "data": {
    "message": "로그인이 필요합니다"
  }
}
```

## 비고

- **관리 화면용** 전체 목록. 삭제(`deleted=true`) 건도 포함.
- 페이징 없음 (본인 수단 수 제한적).
- 입력용 목록은 `PaymentMethodListActive` 사용.
