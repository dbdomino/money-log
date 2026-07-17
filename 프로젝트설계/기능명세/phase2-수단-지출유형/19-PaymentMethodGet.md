# 지출·소득 수단 상세 조회

> API: `PaymentMethodGet` · Phase 2 · 구현순서 19  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출·소득 수단 상세 조회 |
| 구현순서 | 19 |
| API 이름 | `PaymentMethodGet` |
| Method | `GET` |
| URL | `/api/v1/payment-methods/{paymentMethodId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `paymentMethodId` | number | ✅ | 수단 PK |

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
| `paymentMethodId` | number | PK |
| `name` | string | 이름 |
| `type` | string | `CARD` \| `ACCOUNT` |
| `inUse` | boolean | |
| `cardExpiry` | string \| null | |
| `deleted` | boolean | |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3003 | 수단 없음 또는 타인 소유 |

## 예시

### 성공

**Request**

```http
GET /api/v1/payment-methods/1 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "paymentMethodId": 1,
    "name": "국민카드",
    "type": "CARD",
    "inUse": true,
    "cardExpiry": "2028-12",
    "deleted": false
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

- **본인 소유** 수단만 조회 가능.
- 삭제된 수단도 상세 조회 가능 (관리 화면).
