# 기본 목표금액 목록

> API: `ExpendTargetDefaultList` · Phase 5 · 구현순서 47  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 5 (목표금액·통계) |
| 기능 이름 | 기본 목표금액 목록 |
| 구현순서 | 47 |
| API 이름 | `ExpendTargetDefaultList` |
| Method | `GET` |
| URL | `/api/v1/expend-targets/default` |
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
| `Cookie` | ✅ | `JSESSIONID=...` |

### Body

없음

## 응답

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `items` | array | 유형별 기본 목표 |
| `items[].expendGroupId` | number | 지출유형 PK |
| `items[].expendGroupName` | string | 유형 이름 |
| `items[].inUse` | boolean | 유형 사용 여부 |
| `items[].targetAmount` | number | 기본 목표금액 (0~100,000,000). 미설정 시 `0` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-targets/default HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "items": [
      {
        "expendGroupId": 1,
        "expendGroupName": "식비",
        "inUse": true,
        "targetAmount": 400000
      },
      {
        "expendGroupId": 10,
        "expendGroupName": "취미",
        "inUse": false,
        "targetAmount": 0
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

- 본인 **모든 지출유형** 목록 + 기본 목표금액.
- 목표금액 범위: **0 ~ 1억 원**.
- `inUse=false` 유형은 UI에서 입력 비활성화 (upsert API는 거부).
