# 월별 통계 저장

> API: `StatisticsMonthlySave` · Phase 5 · 구현순서 54  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 5 (목표금액·통계) |
| 기능 이름 | 월별 통계 저장 |
| 구현순서 | 54 |
| API 이름 | `StatisticsMonthlySave` |
| Method | `POST` |
| URL | `/api/v1/statistics/monthly/save` |
| 권한 | 로그인 |
| Content-Type | `application/json` |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |
| `Content-Type` | ✅ | `application/json` |

### Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `year` | number | ✅ | 저장 대상 연도 |
| `month` | number | ✅ | 저장 대상 월 1~12 |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `year` | number | |
| `month` | number | |
| `savedAt` | string | 저장 시각 ISO-8601 |
| `source` | string | `SAVED` |
| `message` | string | 예: `2026년 7월 통계가 저장되었습니다` |

저장 직후 `StatisticsMonthlyGet`과 동일한 통계 스냅샷이 DB에 반영된다.

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3603 | year·month 범위 오류 |
| 3604 | 미래 월 저장 시도 (정책에 따라) |

## 예시

### 성공

**Request**

```http
POST /api/v1/statistics/monthly/save HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "year": 2026,
  "month": 6
}
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "year": 2026,
    "month": 6,
    "savedAt": "2026-07-17T13:30:00+09:00",
    "source": "SAVED",
    "message": "2026년 6월 통계가 저장되었습니다"
  }
}
```

### 실패

```json
{
  "resCode": 3603,
  "data": {
    "message": "month는 1~12 사이여야 합니다"
  }
}
```

## 비고

- 당시 가계부·고정지출·목표금액을 반영해 **월별 통계 스냅샷** DB 저장 (갱신).
- 주 용도: **지난달 통계 생성·갱신** (배치 대체 수동 트리거).
- 가계부 데이터 변경과 무관하게 저장본은 유지 → 재저장 시 덮어씀.
- 저장 후 `StatisticsMonthlyGet`은 `source=SAVED` 반환.
