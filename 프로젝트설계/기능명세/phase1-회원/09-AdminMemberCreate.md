# 회원 추가

> API: `AdminMemberCreate` · Phase 1 · 구현순서 9  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 회원 추가 |
| 구현순서 | 9 |
| API 이름 | `AdminMemberCreate` |
| Method | `POST` |
| URL | `/api/v1/admin/members` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | `application/json` |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | ✅ | `JSESSIONID=...` (관리자 세션) |
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 로그인 아이디 |
| `password` | string | ✅ | 비밀번호 평문. bcrypt 해시 저장 → [_공통 § 비밀번호](../_공통.md#비밀번호-저장--bcrypt) |
| `nickname` | string | ✅ | 닉네임 |
| `role` | number | ✅ | `1` 관리자, `3` 일반 |
| `email` | string | ❌ | 이메일 |
| `phone` | string | ❌ | 연락처 |
| `intro` | string | ❌ | 자기소개 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
공유 타입: [_공통.md § Member](../_공통.md#공유-타입--member)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 생성된 아이디 |
| `nickname` | string | 닉네임 |
| `email` | string \| null | |
| `phone` | string \| null | |
| `intro` | string \| null | |
| `role` | number | 지정한 권한 |
| `active` | boolean | `true` (신규 생성) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 2002 | 아이디 중복 |
| 2003 | 이메일 중복 |
| 2004 | 비밀번호 규칙 위반 |
| 9001 | role 값 오류 |

## 예시

### 성공

**Request**

```http
POST /api/v1/admin/members HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=ADMIN_SESSION
Content-Type: application/json

{
  "memberId": "staff01",
  "password": "AdminPass1!",
  "nickname": "운영자",
  "role": 1,
  "email": "admin@example.com"
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "staff01",
    "nickname": "운영자",
    "email": "admin@example.com",
    "phone": null,
    "intro": null,
    "role": 1,
    "active": true
  }
}
```

### 실패

```json
{
  "resCode": 1002,
  "data": {
    "message": "권한이 없습니다"
  }
}
```

## 비고

- **관리자(1) 권한** 검증 필수.
- 회원가입(`MemberSignup`)과 동일 검증 + **기본 지출유형 자동 등록**. 비밀번호는 **bcrypt** 해시 저장 ([_공통](../_공통.md#비밀번호-저장--bcrypt)).
- `role`은 `1` 또는 `3`만 허용.
