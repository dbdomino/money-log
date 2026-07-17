# 본인 정보 조회

> API: `MemberGetMe` · Phase 1 · 구현순서 4  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 본인 정보 조회 |
| 구현순서 | 4 |
| API 이름 | `MemberGetMe` |
| Method | `GET` |
| URL | `/api/v1/members/me` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| — | — | — | 고정 URL `/members/me` (Path variable 없음) |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | ✅ | `JSESSIONID=...` (로그인 세션) |

### Body

없음

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Member](../_공통.md#공유-타입--member)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 로그인 id |
| `nickname` | string | 닉네임 |
| `email` | string \| null | 이메일 |
| `phone` | string \| null | 연락처 |
| `intro` | string \| null | 자기소개 |
| `role` | number | `1` 관리자, `3` 일반 |

`active` 필드는 본인 조회 시 생략하거나 항상 `true`이다.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 세션 없음·만료 |
| 1004 | 비활성 계정 |
| 2001 | 회원 없음 (세션과 DB 불일치) |

## 예시

### 성공

**Request**

```http
GET /api/v1/members/me HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "user01",
    "nickname": "가계부초보",
    "email": "user01@example.com",
    "phone": null,
    "intro": "절약 중",
    "role": 3
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

- 로그인·GET 패턴 확립용 API.
- **본인 데이터만** 조회. 세션의 `memberId` 기준.
- 비밀번호는 응답에 포함하지 않는다.
