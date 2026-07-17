# id 찾기

> API: `MemberFindId` · Phase 1 · 구현순서 9  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 1 (회원·인증) |
| 기능 이름 | id 찾기 |
| 구현순서 | 9 |
| API 이름 | `MemberFindId` |
| Method | `POST` |
| URL | `/api/v1/auth/find-id` |
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
| `email` | string | ✅ | 가입 시 등록한 이메일 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | string | 조회된 아이디 (일부 마스킹 가능) |
| `masked` | boolean | 마스킹 적용 여부 |

마스킹 예: `use***01`

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 2001 | 해당 이메일로 가입된 회원 없음 |
| 9001 | 이메일 형식 오류 |

## 예시

### 성공

**Request**

```http
POST /api/v1/auth/find-id HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "email": "user01@example.com"
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "memberId": "use***01",
    "masked": true
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

- **가입 이메일**로 아이디를 조회한다. 이메일 미등록 회원은 조회 불가.
- 보안상 아이디 일부 마스킹을 권장한다.
- 비활성 계정도 조회 대상일 수 있으나, 정책에 따라 제외 가능.
