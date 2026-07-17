# 고정지출 삭제

> API: `FixedExpenseDelete` · Phase 4 · 구현순서 46  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출) |
| 기능 이름 | 고정지출 삭제 |
| 구현순서 | 46 |
| API 이름 | `FixedExpenseDelete` |
| Method | `DELETE` |
| URL | `/api/v1/fixed-expenses/{fixedExpenseId}` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `fixedExpenseId` | number | ✅ | 삭제 대상 PK |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

## 응답

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `fixedExpenseId` | number | |
| `message` | string | 예: `고정지출이 삭제되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3402 | 고정지출 없음 |

## 예시

### 성공

**Request**

```http
DELETE /api/v1/fixed-expenses/1 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "fixedExpenseId": 1,
    "message": "고정지출이 삭제되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 3402,
  "data": {
    "message": "고정지출을 찾을 수 없습니다"
  }
}
```

## 비고

- 고정지출 설정과 **월별 override 데이터 함께 삭제**.
- 이후 `LedgerMonthlyList`에서 FIXED 항목 제외.
