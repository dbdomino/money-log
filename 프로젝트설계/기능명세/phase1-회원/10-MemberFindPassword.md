# 비밀번호 찾기

> API: `MemberFindPassword` · Phase 1 · 구현순서 10  
> 인덱스: [README.md](../README.md)  
> 이어지는 기능: [11 비밀번호 변경](./11-MemberResetPassword.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 비밀번호 찾기 |
| 구현순서 | 10 |
| API 이름 | `MemberFindPassword` |
| Method | `POST` |
| URL | `/api/v1/auth/find-password` |
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

> **비밀번호·비밀번호 확인 필드 없음.** 본인 계정 존재 여부만 확인한다.

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `matched` | boolean | 항상 `true` (불일치 시 실패 `resCode`) |
| `memberId` | string | 확인된 아이디 (화면 표시용. 마스킹 가능) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 2001 | 아이디·닉네임 불일치 (회원 없음) |
| 1004 | 비활성 계정 |
| 9001 | 필수 필드 누락 |

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/find-password HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "memberId": "user01",
  "nickname": "가계부초보"
}
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "matched": true,
    "memberId": "user01"
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

- **본인 확인만** 수행한다. 비밀번호를 바꾸지 않는다.
- 성공 후 프론트는 [11 비밀번호 변경](./11-MemberResetPassword.md) 화면으로 진행한다.
- 이메일 인증·보안질문 없음 (요구사항과 동일).
- 로그인 후 비밀번호 변경은 [08 본인 정보 수정](./08-MemberUpdateMe.md)의 `password` 필드.
