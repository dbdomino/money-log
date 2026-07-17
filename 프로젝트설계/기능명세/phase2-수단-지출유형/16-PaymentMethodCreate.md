# 지출·소득 수단 등록

> API: `PaymentMethodCreate` · Phase 2 · 구현순서 16  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출·소득 수단 등록 |
| 구현순서 | 16 |
| API 이름 | `PaymentMethodCreate` |
| Method | `POST` |
| URL | `/api/v1/payment-methods` |
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
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `name` | string | ✅ | 수단 이름 (예: 국민카드, 월급통장) |
| `type` | string | ✅ | `CARD` \| `ACCOUNT` |
| `inUse` | boolean | ✅ | 사용 여부 |
| `cardExpiry` | string | ❌ | `YYYY-MM`. `type=CARD`일 때 권장 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § PaymentMethod](../_공통.md#공유-타입--paymentmethod)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `paymentMethodId` | number | 생성된 PK |
| `name` | string | 수단 이름 |
| `type` | string | `CARD` \| `ACCOUNT` |
| `inUse` | boolean | 사용 여부 |
| `cardExpiry` | string \| null | 카드 유효기간 |
| `deleted` | boolean | `false` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3001 | type 값 오류 |
| 3002 | cardExpiry 형식 오류 |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/payment-methods HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "국민카드",
  "type": "CARD",
  "inUse": true,
  "cardExpiry": "2028-12"
}
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
  "resCode": 3001,
  "data": {
    "message": "지원하지 않는 수단 유형입니다"
  }
}
```

## 비고

- 지출·소득 **공통 테이블**. `purpose`는 등록 시 내부 구분 또는 사용 맥락으로 결정.
- 본인(`memberId`) 소유로만 생성.
- `type=ACCOUNT`이면 `cardExpiry`는 `null`.
