# 기본 목표금액 등록·수정

> API: `ExpendTargetDefaultUpsert` · Phase 5 · 구현순서 49  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 5 (목표금액·통계) |
| 기능 이름 | 기본 목표금액 등록·수정 |
| 구현순서 | 49 |
| API 이름 | `ExpendTargetDefaultUpsert` |
| Method | `PATCH` |
| URL | `/api/v1/expend-targets/default/{expendGroupId}` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `expendGroupId` | number | ✅ | 지출유형 PK |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `targetAmount` | number | ✅ | 기본 목표금액 0~100,000,000 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `application/json` |

## 응답

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `expendGroupId` | number | |
| `expendGroupName` | string | |
| `targetAmount` | number | 저장된 목표금액 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3103 | 유형 없음 |
| 3601 | **미사용(inUse=false) 유형** |
| 3602 | 목표금액 범위 초과 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/expend-targets/default/1 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "targetAmount": 450000
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "expendGroupId": 1,
    "expendGroupName": "식비",
    "targetAmount": 450000
  }
}
```

### 실패

```json
{
  "resCode": 3601,
  "data": {
    "message": "사용 중이지 않은 지출 유형에는 목표금액을 설정할 수 없습니다"
  }
}
```

## 비고

- 최초 설정·변경 모두 PATCH upsert.
- 월별 목표 미지정 시 통계·가계부에서 **기본값** 사용.
