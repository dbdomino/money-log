# 월별 목표금액 등록·수정

> API: `ExpendTargetMonthlyUpsert` · Phase 5 · 구현순서 52  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 5 (목표금액·통계) |
| 기능 이름 | 월별 목표금액 등록·수정 |
| 구현순서 | 52 |
| API 이름 | `ExpendTargetMonthlyUpsert` |
| Method | `PATCH` |
| URL | `/api/v1/expend-targets/monthly/{year}/{month}/{expendGroupId}` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

| 이름 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `year` | number | ✅ | 연도 |
| `month` | number | ✅ | 월 1~12 |
| `expendGroupId` | number | ✅ | 지출유형 PK |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `targetAmount` | number | ✅ | 해당 월·유형 목표금액 0~100,000,000 |

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `application/json` |

## 응답

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `year` | number | |
| `month` | number | |
| `expendGroupId` | number | |
| `expendGroupName` | string | |
| `targetAmount` | number | 월별 목표 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3601 | 미사용 유형 |
| 3602 | 목표금액 범위 초과 |
| 3603 | year·month 범위 오류 |

## 예시

### 성공

**Request**

```http
PATCH /api/v1/expend-targets/monthly/2026/7/1 HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "targetAmount": 500000
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "year": 2026,
    "month": 7,
    "expendGroupId": 1,
    "expendGroupName": "식비",
    "targetAmount": 500000
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

- 통계 화면에서 **특정 달·유형** 목표 설정.
- 월별 미지정 시 `ExpendTargetDefaultList`의 기본값 사용.
- `targetAmount=0` 허용 (목표 없음).
