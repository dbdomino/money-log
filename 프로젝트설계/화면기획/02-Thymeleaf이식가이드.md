# [화면기획] Thymeleaf 이식 가이드

> 기획 산출물 → `app-mod/money-app` 대응표.  
> CSS/JS는 경로만 맞추면 복사 이식 가능하다.

## static

| 기획 | money-app |
|------|-----------|
| `화면기획/css/tokens.css` | `src/main/resources/static/css/tokens.css` |
| `화면기획/css/ui.css` | `src/main/resources/static/css/ui.css` |
| `화면기획/js/modal.js` | `src/main/resources/static/js/modal.js` |

레이아웃 head 예:

```html
<link rel="stylesheet" th:href="@{/css/tokens.css}">
<link rel="stylesheet" th:href="@{/css/ui.css}">
<script defer th:src="@{/js/modal.js}"></script>
```

프로토타입은 상대경로 `../css/...`, `../js/...` 사용.

## templates 매핑

| 역할 | 경로 |
|------|------|
| auth 레이아웃 | `templates/layout/auth.html` |
| main 레이아웃 | `templates/layout/main.html` |
| 사이드바 | `templates/fragments/sidebar.html` :: `sidebar` |
| 모달 껍데기 | `templates/fragments/modal-shell.html` :: `modalShell` |
| 확인 다이얼로그 | `templates/fragments/confirm-dialog.html` |

페이지·모달 fragment는 [01-화면구성.md](./01-화면구성.md) 표와 동일.

### layout/main 골격 (개념)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layout/main :: layout(~{::section})}">
<body>
<section>
  <!-- 페이지 본문 -->
  <div th:replace="~{payment/fragments/payment-modal :: modal}"></div>
</section>
</body>
</html>
```

(실제 `th:replace` 시그니처는 프로젝트 레이아웃 관례에 맞출 것.)

## JS 모달 계약

`modal.js` API:

| 동작 | 방법 |
|------|------|
| 열기 | `data-modal-open="modal-payment-create"` 또는 `MoneyLogModal.open(id)` |
| 닫기 | `data-modal-close` / Esc / 딤 클릭 |
| 딥링크 | URL `?m=create` → 페이지 로드 시 `data-modal-deeplink` 맵으로 open |

목록 페이지에:

```html
<body data-modal-map='{"create":"modal-payment-create","edit":"modal-payment-edit","detail":"modal-payment-detail"}'>
```

서버(Thymeleaf)는 `openModal` 모델로 넘겨도 되고, 쿼리만 JS가 읽어도 된다.

## 폼 → API

| UI | 백엔드 |
|----|--------|
| form `name` | Request Body / Query 필드명과 동일 |
| POST/PATCH 제출 | `*WebController` → `BackendApiClient` |
| 성공 | 부모 URL redirect (+ flash 메시지) |
| 실패 | `resCode`·message를 모달/페이지 alert |

## Controller 역할

| 클래스 | 역할 |
|--------|------|
| `PageController` | GET 페이지 + (선택) `?m=` → model |
| `*WebController` | 모달 폼 POST/PATCH/DELETE |
| `BackendApiClient` | `:8081/api/v1` |

## 이식 체크리스트 (화면 1개)

1. [01](./01-화면구성.md)에서 페이지/모달·템플릿 경로 확인  
2. proto HTML → `templates/` 로 옮기고 `th:` 바인딩  
3. 더미 행 → API `list` / `data`  
4. 모달 제출 → WebController  
5. 사이드바 `activeMenu` 일치  
6. 권한(1.1~1.6 제외 로그인, 1.8~1.10 관리자) 인터셉터/필터
