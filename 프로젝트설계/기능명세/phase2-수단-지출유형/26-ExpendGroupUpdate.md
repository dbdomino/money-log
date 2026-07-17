# 지출유형 수정

> API: `ExpendGroupUpdate` · Phase 2 · 구현순서 26  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 수정 |
| 구현순서 | 26 |
| API 이름 | `ExpendGroupUpdate` |
| Method | `PATCH` |
| URL | `/api/v1/expend-groups/{expendGroupId}` |
| 권한 | 로그인 |
| Content-Type | `multipart/form-data` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expendGroupId` | number | ✅ | 지출유형 PK |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `multipart/form-data` |

### Body (form fields)

PATCH omit: 전송한 필드만 갱신.

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `name` | string | ❌ | 유형 이름 |
| `inUse` | boolean | ❌ | 사용 여부 |
| `iconFile` | file | ❌ | 새 아이콘 (교체) |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

ExpendGroup 전체 필드 (갱신 후)

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3103 | 유형 없음 |
| 3105 | 기본 유형 이름 변경 불가 |
| 3102 | 아이콘 파일 오류 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/expend-groups/10 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data; boundary=----Boundary

------Boundary
Content-Disposition: form-data; name="inUse"

false
------Boundary--
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expendGroupId": 10,
    "name": "취미",
    "inUse": false,
    "iconUrl": "/api/v1/expend-groups/icons/user01_취미.png",
    "defaultGroup": false
  }
}
```

### 실패

```json
{
  "resCode": 3105,
  "data": {
    "message": "기본 지출 유형의 이름은 변경할 수 없습니다"
  }
}
```

## 비고

- `inUse=false`로 변경 시 **목표금액 API에서 거부** (`ExpendTarget*`).
- 아이콘 교체 시 파일명 규칙 동일. 이전 파일 유지 가능.
- 과거 지출의 `expendGroupName` 스냅샷은 변경되지 않음.
