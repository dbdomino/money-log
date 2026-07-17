# 엑셀 업로드·일괄 등록

> API: `LedgerExcelUpload` · Phase 4 · 구현순서 46  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 4 (가계부·고정지출·엑셀) |
| 기능 이름 | 엑셀 업로드·일괄 등록 |
| 구현순서 | 46 |
| API 이름 | `LedgerExcelUpload` |
| Method | `POST` |
| URL | `/api/v1/ledger/excel/upload` |
| 권한 | 로그인 |
| Content-Type | `multipart/form-data` |

## 요청

### Path Variables

없음

### Query Parameters

없음

### Headers

| 이름 | 필수 | 설명 |
|------|:----:|------|
| `Cookie` | ✅ | `JSESSIONID=...` |
| `Content-Type` | ✅ | `multipart/form-data` |

### Body (form fields)

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `file` | file | ✅ | `.xlsx` only, **최대 300행** (헤더 제외) |

## 응답

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `importedCount` | number | 등록된 건수 |
| `expenseCount` | number | 지출 건수 |
| `incomeCount` | number | 소득 건수 |
| `message` | string | 예: `150건이 등록되었습니다` |

### 실패 — 검증 오류 (`resCode: 3502`)

| 필드 | 타입 | 설명 |
|------|------|------|
| `message` | string | 요약 메시지 |
| `errors` | array | 행별 오류 목록 |
| `errors[].row` | number | 엑셀 행 번호 (1-based, 헤더=1) |
| `errors[].column` | string | 열 이름 또는 문자 (예: `D`, `금액`) |
| `errors[].message` | string | 오류 상세 |

**1행이라도 오류 시 전체 실패** (트랜잭션 롤백).

### 기타 실패 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |
| 3503 | `.xlsx` 아님 |
| 3504 | 300행 초과 |
| 3505 | 빈 파일 |

## 예시

### 성공

**Request**

```http
POST /api/v1/ledger/excel/upload HTTP/1.1
Host: localhost:8081
Cookie: JSESSIONID=A1B2C3D4E5F6
Content-Type: multipart/form-data; boundary=----Boundary

------Boundary
Content-Disposition: form-data; name="file"; filename="july.xlsx"
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

(binary)
------Boundary--
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "importedCount": 25,
    "expenseCount": 20,
    "incomeCount": 5,
    "message": "25건이 등록되었습니다"
  }
}
```

### 실패 (검증)

```json
{
  "resCode": 3502,
  "data": {
    "message": "엑셀 검증에 실패했습니다",
    "errors": [
      {
        "row": 5,
        "column": "C",
        "message": "금액은 0보다 커야 합니다"
      },
      {
        "row": 8,
        "column": "D",
        "message": "존재하지 않는 수단입니다"
      }
    ]
  }
}
```

## 비고

- `.xlsx`만 허용. **최대 300행**.
- **중복 행 허용** (동일 내용 여러 번 등록 가능).
- 검증 실패 시 **전체 롤백**, `row`·`column`·`message`로 위치 안내.
- 지출·소득은 각각 `ExpenseCreate`·`IncomeCreate`와 동일 규칙 적용.
