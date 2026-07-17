# 지출유형 목록

> API: `ExpendGroupList` · Phase 2 · 구현순서 24  
> 인덱스: [README.md](../README.md)

## 메타

| 항목 | 내용 |
|------|------|
| Phase | 2 (수단·지출유형) |
| 기능 이름 | 지출유형 목록 |
| 구현순서 | 24 |
| API 이름 | `ExpendGroupList` |
| Method | `GET` |
| URL | `/api/v1/expend-groups` |
| 권한 | 로그인 |
| Content-Type | — (Body 없음) |

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

공통 래퍼: [_공통.md § 응답 래퍼](../_공통.md#응답-래퍼)  
목록 표준: [_공통.md § 목록 응답 규칙](../_공통.md#목록-응답-규칙) (`data.list` = object 배열)

### 성공 (`resCode: 0`) — `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| `list` | array (object[]) | 기본·사용자 유형 전체 |
| `list[].expendGroupId` | number | PK |
| `list[].name` | string | 이름 |
| `list[].inUse` | boolean | |
| `list[].iconUrl` | string \| null | 아이콘 조회 상대 경로. 수신 방법: [_공통 § 지출유형 아이콘 · 프론트 수신](../_공통.md#지출유형-아이콘--프론트-수신) |
| `list[].defaultGroup` | boolean | 시스템 기본 유형 |

### 실패 — 대표 `resCode`

| resCode | 조건 |
|---------|------|
| 1001 | 로그인 필요 |

## 예시

### 성공

**Request**

```http
GET /api/v1/expend-groups HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**

```json
{
  "resCode": 0,
  "data": {
    "list": [
      {
        "expendGroupId": 1,
        "name": "식비",
        "inUse": true,
        "iconUrl": "/api/v1/expend-groups/icons/user01_식비.png",
        "defaultGroup": true
      },
      {
        "expendGroupId": 10,
        "name": "취미",
        "inUse": true,
        "iconUrl": null,
        "defaultGroup": false
      }
    ]
  }
}
```

### 실패

```json
{
  "resCode": 1001,
  "data": {
    "message": "로그인이 필요합니다"
  }
}
```

## 비고

- 목록 응답 표준: `data.list` = **object 배열** ([_공통 § 목록 응답 규칙](../_공통.md#목록-응답-규칙)).
- 전체 목록. **사용 중·미사용** 모두 포함.
- `defaultGroup=true` 유형은 회원가입 시 자동 생성.
- `iconUrl`로 유형마다 아이콘 API를 호출할 수 있다. 프론트는 **프록시 또는 fetch+blob**으로 수신 ([_공통](../_공통.md#지출유형-아이콘--프론트-수신)). `<img src="{iconUrl}">`만으로 백엔드에 직접 붙이지 않는다(Bearer 미첨부 → 401).
