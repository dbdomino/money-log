# 비활성화

> API: `AdminMemberDeactivate` · Phase 1 · 구현순서 13  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | 비활성화 |
| 구현순서 | 13 |
| API 이름 | `AdminMemberDeactivate` |
| Method | `PATCH` |
| URL | `/api/v1/admin/members/{memberId}/deactivate` |
| 권한 | 관리자 (`role=1`) |
| Content-Type | — (Body 없음) |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `memberId` | string | ✅ | 비활성화 대상 회원 아이디 |

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | ✅ | `JSESSIONID=...` (관리자 세션) |

### Body

없음 (상태만 변경)

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 비활성화된 아이디 |
| `active` | boolean | `false` |
| `message` | string | 예: `회원이 비활성화되었습니다` |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 1002 | 관리자 권한 없음 |
| 2001 | 회원 없음 |
| 9001 | 이미 비활성 상태 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/admin/members/user01/deactivate HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=ADMIN_SESSION
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "user01",
    "active": false,
    "message": "회원이 비활성화되었습니다"
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

- `active`를 `false`로 설정 (물리 삭제 아님).
- **기존 세션은 유지**될 수 있으나, 이후 로그인 시 `1004` 거부.
- 재활성화 API는 현재 범위 외 (관리자 수동 DB 처리 또는 추후 추가).
- 본인 관리자 계정 비활성화 방지 로직 권장.
