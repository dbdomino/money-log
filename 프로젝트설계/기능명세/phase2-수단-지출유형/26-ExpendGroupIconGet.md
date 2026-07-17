# 지출유형 아이콘 조회

> API: `ExpendGroupIconGet` · Phase 2 · 구현순서 26  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 아이콘 조회 |
| 구현순서 | 26 |
| API 이름 | `ExpendGroupIconGet` |
| Method | `GET` |
| URL | `/api/v1/expend-groups/icons/{filename}` |
| 권한 | 로그인 |
| Content-Type | — (이미지 바이너리 응답) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `filename` | string | ✅ | 저장된 아이콘 파일명 (예: `user01_식비.png`) |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

> **JSON 래퍼 없음.** 이미지 파일을 직접 반환한다.

### 성공

| 항목 | 내용 |
|------|------|
| HTTP Status | `200 OK` |
| `Content-Type` | `image/png`, `image/jpeg`, `image/gif` 등 |
| Body | 이미지 바이너리 |

### 실패

파일 없음 시 HTTP `404` 또는 JSON 래퍼 `{ resCode, data }` (구현 정책에 따름).

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3104 | 파일 없음 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-groups/icons/user01_식비.png HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```http
HTTP/1.1 200 OK
Content-Type: image/png
Content-Length: 4096

(binary image data)
```

### 실패 (JSON 래퍼 사용 시)

```json
{
  "resCode": 3104,
  "data": {
    "message": "아이콘 파일을 찾을 수 없습니다"
  }
}
```

## 비고

- `{ resCode, data }` **JSON 래퍼를 사용하지 않는** 예외 API.
- 유형 삭제 후에도 **파일은 디스크에 유지** (과거 참조·복구용).
- 본인 아이콘 또는 접근 권한 있는 파일만 제공.
