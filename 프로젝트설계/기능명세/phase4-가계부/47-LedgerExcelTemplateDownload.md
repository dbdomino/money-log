# 엑셀 양식 다운로드

> API: `LedgerExcelTemplateDownload` · Phase 4 · 구현순서 47  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 엑셀 양식 다운로드 |
| 구현순서 | 47 |
| API 이름 | `LedgerExcelTemplateDownload` |
| Method | `GET` |
| URL | `/api/v1/ledger/excel/template` |
| 권한 | 로그인 |
| Content-Type | — (xlsx 바이너리 응답) |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Authorization` | ✅ | `Bearer <accessToken>` |

### Body

없음

## 응답

> **JSON 래퍼 없음.** Excel 파일을 직접 반환한다.

### 성공

| 항목 | 내용 |
|------|------|
| HTTP Status | `200 OK` |
| `Content-Type` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `Content-Disposition` | `attachment; filename="ledger_template.xlsx"` |
| Body | `.xlsx` 바이너리 |

#### 양식 시트 구조 (참고)

| 열 | 설명 |
|----|------|
| A | 구분 (`EXPENSE` / `INCOME`) |
| B | 결제일 `YYYY-MM-DD` |
| C | 금액 |
| D | 수단 (드롭다운: 본인 활성 수단) |
| E | 지출유형 (지출 시, 드롭다운) |
| F | 장소 (지출) |
| G | 내용 |

### 실패

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 9000 | 파일 생성 오류 |

## 예시

### 성공

**Request**

```http
GET /api/v1/ledger/excel/template HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```http
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="ledger_template.xlsx"

(binary xlsx data)
```

### 실패 (JSON 래퍼 사용 시)

```json
{
  "resCode": 1001,
  "data": {
    "message": "로그인이 필요합니다"
  }
}
```

## 비고

- 본인 **활성 수단·지출유형**을 셀 데이터 유효성(드롭다운)에 포함.
- `{ resCode, data }` JSON 래퍼 **미사용** (파일 다운로드).
- 업로드 양식과 컬럼 정의가 일치해야 한다.
