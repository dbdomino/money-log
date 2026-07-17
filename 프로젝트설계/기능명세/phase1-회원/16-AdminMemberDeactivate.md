# 관리자 회원 정지

> API: `AdminMemberDeactivate` · Phase 1 · 구현순서 16  
> 인덱스: [README.md](../README.md)

> **본 문서는 관리자 전용 기능이다.**  
> 사용자 **로그아웃 (JWT Token 비활성화)** 는 [06-MemberTokenRevoke.md](./06-MemberTokenRevoke.md) 를 본다.

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | **관리자 회원 정지** |
| 구현순서 | 16 |
| API 이름 | `AdminMemberDeactivate` |
| Method | `PATCH` |
| URL | `/api/v1/admin/members/{memberId}/deactivate` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 정지 대상 회원 아이디 |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` (관리자) |

### Body

없음 (상태만 변경)

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 200`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 정지된 아이디 |
| `active` | boolean | `false` |
| `message` | string | 예: `회원이 정지되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 2001 | 회원 없음 |
| 9001 | 이미 정지 상태 (`active=false`) |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/admin/members/user01/deactivate HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (관리자)
```

**Response**

```json
{
  "resCode": 200,
  "data": {
    "memberId": "user01",
    "active": false,
    "message": "회원이 정지되었습니다"
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

- `active`를 `false`로 설정 (물리 삭제 아님). 요구사항상 「비활성화」에 해당하는 **계정** 조치이다.
- 정지 시 해당 회원 활성 세션에 **JWT Token 비활성화** 절차 적용 (해시 NULL). [_공통 § 로그아웃](../_공통.md#로그아웃-jwt-token-비활성화)
- 이후 해당 회원 로그인 시 `1004`. 기존 JWT로 API 호출 시 `1006`/`1005`.
- **로그아웃 API(`MemberTokenRevoke`)와 다름** — 본 API는 관리자가 **타인 계정**을 막는 것.
- 재활성화 API는 현재 범위 외.
- 본인 관리자 계정 정지 방지 로직 권장.
