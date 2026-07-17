# 헬스체크

> API: `HealthCheck` · Phase 1 · 구현순서 1  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 헬스체크 |
| 구현순서 | 1 |
| API 이름 | `HealthCheck` |
| Method | `GET` |
| URL | `/api/v1/ha` |
| 권한 | 없음 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| — | — | — | 사용하지 않음 |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| — | — | — | 사용하지 않음 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| — | — | 인증 불필요 |

### Body

없음 (GET 규칙)

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | string | 서버 상태. 예: `UP` |
| `timestamp` | string | ISO-8601 응답 시각 |
| `service` | string | 서비스 식별자. 예: `money-backend-app` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 9000 | 내부 서버 오류 |

## 예시

### 성공

**Request**

```http
GET /api/v1/ha HTTP/1.1
Host: localhost:8081
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "status": "UP",
    "timestamp": "2026-07-17T13:00:00+09:00",
    "service": "money-backend-app"
  }
}
```

### 실패

```json
{
  "resCode": 9000,
  "data": {
    "message": "내부 서버 오류가 발생했습니다"
  }
}
```

## 비고

- Phase 0에서 확립한 `{ resCode, data }` 응답 형식 검증용 API이다.
- 로드밸런서·모니터링 헬스체크에 사용할 수 있다.
- 인증·세션 없이 호출 가능하다.
