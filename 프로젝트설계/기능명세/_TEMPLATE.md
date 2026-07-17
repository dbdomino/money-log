# {기능이름}

> API: `{ApiName}` · Phase {phase} · 구현순서 {order}  
> 인덱스: [README.md](./README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | {phase} |
| 기능 이름 | {기능이름} |
| 구현순서 | {order} |
| API 이름 | `{ApiName}` |
| Method | `{METHOD}` |
| URL | `/api/v1/...` |
| 권한 | 없음 / 로그인 / 관리자 |
| Content-Type | `application/json` / `multipart/form-data` / — |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | 조건부 | `JSESSIONID=...` (로그인 필요 시) |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](./_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|

## 예시

### 성공

**Request**

```http
```

**Response**

```json
{
  "resCode": 0,
  "data": {}
}
```

### 실패

```json
{
  "resCode": 1001,
  "data": { "message": "..." }
}
```

## 비고

- 
