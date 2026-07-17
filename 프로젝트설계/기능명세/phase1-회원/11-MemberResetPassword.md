# 비밀번호 변경

> API: `MemberResetPassword` · Phase 1 · 구현순서 11  
> 인덱스: [README.md](../README.md)  
> 선행: [10 비밀번호 찾기](./10-MemberFindPassword.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 비밀번호 변경 |
| 구현순서 | 11 |
| API 이름 | `MemberResetPassword` |
| Method | `POST` |
| URL | `/api/v1/auth/reset-password` |
| 권한 | 없음 (찾기 성공 후·비로그인) |
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
| `memberId` | string | ✅ | 아이디 (찾기에서 확인한 값) |
| `nickname` | string | ✅ | 닉네임 (본인 재확인) |
| `newPassword` | string | ✅ | 새 비밀번호 평문. bcrypt 해시 저장 → [_공통 § 비밀번호](../_공통.md#비밀번호-저장--bcrypt) |
| `newPasswordConfirm` | string | ✅ | 새 비밀번호 확인 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 200`) — `data`

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
| 9001 | 필수 필드 누락 |

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
  "resCode": 200,
  "data": {
    "message": "비밀번호가 변경되었습니다"
  }
}
```

### 실패 (확인 불일치)

```json
{
  "resCode": 2005,
  "data": {
    "message": "비밀번호 확인이 일치하지 않습니다"
  }
}
```

## 비고

- **[10 비밀번호 찾기](./10-MemberFindPassword.md) 성공 후** 호출하는 재설정 API이다.
- 서버는 `memberId`·`nickname`을 **다시 검증**한 뒤 비밀번호를 변경한다. (찾기 API만 통과했다고 생략하지 않음)
- `newPassword`·`newPasswordConfirm`은 **필수**. 이 API의 목적이 재설정이므로 omit 불가.
- 변경 후 해당 회원 활성 세션에 **JWT Token 비활성화**(해시 NULL) 적용.
- 로그인 상태에서 프로필로 바꾸는 경우는 [08 본인 정보 수정](./08-MemberUpdateMe.md) (`password`는 선택).
