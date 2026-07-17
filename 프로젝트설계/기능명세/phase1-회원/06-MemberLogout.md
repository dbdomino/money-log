# 로그아웃

> API: `MemberLogout` · Phase 1 · 구현순서 6  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 로그아웃 |
| 구현순서 | 6 |
| API 이름 | `MemberLogout` |
| Method | `POST` |
| URL | `/api/v1/auth/logout` |
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

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `message` | string | 예: `로그아웃되었습니다` |

세션 무효화 시 `Set-Cookie`로 쿠키 삭제할 수 있다.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 이미 로그아웃 상태·세션 없음 |

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/logout HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "message": "로그아웃되었습니다"
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

- 서버 세션을 **종료(무효화)** 한다.
- 이후 동일 `JSESSIONID`로 API 호출 시 `1001` 반환.
- Body 없이 POST로 호출한다.
