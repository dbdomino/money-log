# {기능이름}

> API: `{ApiName}` · Phase {phase} · 구현순서 {order}  
> 인덱스: [README.md](./README.md)

> **필드 설명:** Path/Query/Body/응답 표의 설명은 **그 칸만 보고 의미가 읽혀야** 한다.  
> `요청 에코`·`동일`·`공통 참고`만 적는 것은 **누락**. [_공통.md § 필드 설명 작성 규칙](./_공통.md#필드-설명-작성-규칙-누락)

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

> **권한 = 로그인 또는 관리자**이면 Access Token **만료·유효성 검증이 API에 포함**된다.  
> [_공통.md § 권한과 토큰 검증](./_공통.md#권한과-토큰-검증-필수-규칙)

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
| `Authorization` | 조건부 | `Bearer <accessToken>` (로그인 필요 시) |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](./_공통.md#응답-래퍼)  
목록 API면 배열 필드명 **`list`**, 요소는 **JSON object**: [_공통.md § 목록 응답 규칙](./_공통.md#목록-응답-규칙)

### 성공 (`resCode: 200`) — `data`

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
  "resCode": 200,
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
