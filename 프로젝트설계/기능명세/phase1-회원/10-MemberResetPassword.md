# 비밀번호 찾기

> API: `MemberResetPassword` · Phase 1 · 구현순서 10  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 비밀번호 찾기 |
| 구현순서 | 10 |
| API 이름 | `MemberResetPassword` |
| Method | `POST` |
| URL | `/api/v1/auth/reset-password` |
| 권한 | 없음 |
| Content-Type | `application/json` |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 아이디 |
| `nickname` | string | ✅ | 닉네임 (본인 확인) |
| `newPassword` | string | ✅ | 새 비밀번호 평문. bcrypt 해시 저장 → [_공통 § 비밀번호](../_공통.md#비밀번호-저장--bcrypt) |
| `newPasswordConfirm` | string | ✅ | 새 비밀번호 확인 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `message` | string | 예: `비밀번호가 변경되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 2001 | 아이디·닉네임 불일치 |
| 2004 | 비밀번호 규칙 위반 |
| 2005 | 새 비밀번호 확인 불일치 |
| 1004 | 비활성 계정 |

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/reset-password HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "memberId": "user01",
  "nickname": "가계부초보",
  "newPassword": "NewPass1!",
  "newPasswordConfirm": "NewPass1!"
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "message": "비밀번호가 변경되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 2001,
  "data": {
    "message": "회원을 찾을 수 없습니다"
  }
}
```

## 비고

- **아이디 + 닉네임** 일치 시에만 비밀번호 재설정 허용 (본인 확인).
- 비밀번호 규칙: 8자 이상, 3종류 이상 문자 조합. 저장은 **bcrypt** 해시 ([_공통](../_공통.md#비밀번호-저장--bcrypt)).
- 변경 후 해당 회원 활성 세션에 **JWT Token 비활성화**(해시 NULL) 적용.
