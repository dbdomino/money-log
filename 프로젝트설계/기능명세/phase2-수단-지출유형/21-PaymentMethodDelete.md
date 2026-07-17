# 지출·소득 수단 삭제

> API: `PaymentMethodDelete` · Phase 2 · 구현순서 21  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출·소득 수단 삭제 |
| 구현순서 | 21 |
| API 이름 | `PaymentMethodDelete` |
| Method | `DELETE` |
| URL | `/api/v1/payment-methods/{paymentMethodId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `paymentMethodId` | number | ✅ | 삭제 대상 PK |

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

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `paymentMethodId` | number | 삭제(soft)된 PK |
| `deleted` | boolean | `true` |
| `message` | string | 예: `결제 수단이 삭제되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3003 | 수단 없음 |
| 3004 | 이미 삭제됨 |

## 예시

### 성공

**Request**

```http
DELETE /api/v1/payment-methods/2 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "paymentMethodId": 2,
    "deleted": true,
    "message": "결제 수단이 삭제되었습니다"
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

- **Soft delete**: `deleted=true` 설정. 물리 삭제 아님.
- 과거 가계부 내역의 **수단 이름 스냅샷은 유지**.
- `PaymentMethodListActive`에서는 제외.
