# 다른 회원 수정

> API: `AdminMemberUpdate` · Phase 1 · 구현순서 12  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 다른 회원 수정 |
| 구현순서 | 12 |
| API 이름 | `AdminMemberUpdate` |
| Method | `PATCH` |
| URL | `/api/v1/admin/members/{memberId}` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | `application/json` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 수정 대상 회원 아이디 |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | ✅ | `JSESSIONID=...` (관리자 세션) |
| `Content-Type` | ✅ | `application/json` |

### Body

PATCH omit 규칙: [_공통.md § PATCH Body 규칙](../_공통.md#patch-body-규칙)

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `password` | string | ❌ | 새 비밀번호 평문. bcrypt 해시 저장 → [_공통 § 비밀번호](../_공통.md#비밀번호-저장--bcrypt) |
| `nickname` | string | ❌ | 닉네임 |
| `role` | number | ❌ | `1` \| `3` |
| `email` | string \| null | ❌ | 이메일 |
| `phone` | string \| null | ❌ | 연락처 |
| `intro` | string \| null | ❌ | 자기소개 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 아이디 |
| `nickname` | string | |
| `email` | string \| null | |
| `phone` | string \| null | |
| `intro` | string \| null | |
| `role` | number | |
| `active` | boolean | (비활성화는 별도 API) |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 2001 | 회원 없음 |
| 2003 | 이메일 중복 |
| 2004 | 비밀번호 규칙 위반 |
| 9001 | role 값 오류 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/admin/members/user01 HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=ADMIN_SESSION
Content-Type: application/json

{
  "nickname": "수정된닉네임",
  "role": 3
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "user01",
    "nickname": "수정된닉네임",
    "email": "user01@example.com",
    "phone": null,
    "intro": null,
    "role": 3,
    "active": true
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

- 관리자가 **다른 회원** 정보·비밀번호·권한 변경. 비밀번호 변경 시 **bcrypt** 해시 ([_공통](../_공통.md#비밀번호-저장--bcrypt)).
- `active` 변경은 `AdminMemberDeactivate` API 사용.
- 보낸 필드만 PATCH 갱신.
