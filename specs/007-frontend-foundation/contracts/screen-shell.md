# Contract: 공통 껍데기 — 레이아웃 · 사이드바 · 모달

**Feature**: 007-frontend-foundation | **관련 FR**: 624~632

008~012 가 만드는 화면 30개는 **이 껍데기 안에** 들어간다. 007 은 껍데기와, 그 껍데기만
쓰는 화면 하나(1.6)를 만든다.

## 1. 레이아웃 둘

| 레이아웃 | 템플릿 | 쓰는 화면 | 구성 |
|---|---|---|---|
| **auth** | `templates/layout/auth.html` | 1.1~1.6 (비로그인 허용) | 가운데 카드. 사이드바·상단바 없음 |
| **main** | `templates/layout/main.html` | 1.7 및 Phase 2~6 전부 | 사이드바 + 상단바 + 본문 |

**모든 화면이 둘 중 하나를 쓴다**(FR-624). 화면이 자기 `<html>` 골격을 따로 두지 않는다.

프로토타입은 `화면기획/proto/layout-auth-shell.html` · `layout-main-shell.html` 이며
경로만 맞춰 이식한다(FR-631).

**1.6 은 auth 다.** 미로그인 사용자도 들어오는 화면이라 로그인 후 사이드바를 붙일 수 없다.
`1.6-ErrorForbidden.md` 메타가 `main` 으로 적혀 있어 선행 개정 1번으로 고친다.

### 정적 자원 (FR-631)

| 화면기획 | money-app |
|---|---|
| `화면기획/css/tokens.css` | `static/css/tokens.css` |
| `화면기획/css/ui.css` | `static/css/ui.css` |
| `화면기획/js/modal.js` | `static/js/modal.js` |

레이아웃 `<head>` 에서 한 번만 건다. 화면마다 다시 걸지 않는다.

```html
<link rel="stylesheet" th:href="@{/css/tokens.css}">
<link rel="stylesheet" th:href="@{/css/ui.css}">
<script defer th:src="@{/js/modal.js}"></script>
```

프로토타입은 상대경로(`../css/...`)를 쓰므로 `th:href` 로 바꾸는 것이 이식의 전부다.
**세 파일의 내용은 고치지 않는다.**

## 2. 사이드바 (main 레이아웃)

`templates/fragments/sidebar.html :: sidebar`. 메뉴 12개는 `_공통.md` § 사이드바 메뉴와 같다.

| 메뉴 | URL | `activeMenu` | 권한 |
|---|---|---|---|
| 월별 목록 | `/ledger` | `ledger` | 로그인 |
| 지출 등록 | `/ledger?m=expense-create` | `ledger` | 로그인 |
| 소득 등록 | `/ledger?m=income-create` | `ledger` | 로그인 |
| 엑셀 일괄 등록 | `/ledger/excel` | `excel` | 로그인 |
| 고정지출 설정 목록 | `/fixed-expenses` | `fixed-list` | 로그인 |
| 고정지출 등록 | `/fixed-expenses?m=create` | `fixed-list` | 로그인 |
| 지출·소득 수단 | `/payments` | `payments` | 로그인 |
| 지출유형 | `/expend-groups` | `expend-groups` | 로그인 |
| 목표금액 | `/targets/default` | `targets` | 로그인 |
| 월별 통계 | `/statistics` | `statistics` | 로그인 |
| 본인 정보 | `/member/profile` | `profile` | 로그인 |
| 회원 관리 | `/admin/members` | `admin-members` | **관리자** |

- 화면은 모델에 `activeMenu` 문자열 하나를 넣고, 사이드바가 그 값으로 활성 표시를 한다(FR-625).
  등록 메뉴 셋은 부모 페이지와 **같은 `activeMenu`** 를 쓴다 — 모달이 열려도 활성 메뉴는 부모다.
- **회원 관리 메뉴는 `role == 1` 일 때만 그린다**(FR-626). 숨기는 것이 아니라 **HTML 에 넣지
  않는다** — `display:none` 이면 소스에서 URL 이 읽힌다.
- 메뉴 노출과 접근 차단은 별개다. 메뉴를 감춰도 URL 직접 입력은
  [session-auth.md](./session-auth.md) 의 인터셉터가 막는다.

## 3. 모달

### 표시 규칙

| 구분 | 대상 | URL |
|---|---|---|
| 페이지 | 목록 · 허브 · auth · 본인정보 · 엑셀 · 목표 · 통계 | 대표 URL |
| **모달** | 등록 · 수정 · 상세 (및 고정지출 월별) | **부모 URL + `?m=`** (+ `id` 등) |

등록·수정·상세는 전용 페이지를 만들지 않는다(FR-627). 사이드바의 「지출 등록」 같은 항목도
부모로 이동한 뒤 모달을 연다.

### 딥링크 판정 (FR-627·630)

**서버가 판정하고 JS 는 연다**(research 14).

```
GET /payments?m=create
 └─ 컨트롤러: ModalParam 화이트리스트에 "create" 가 있는가?
      ├─ 있다  → 모델에 openModal = "modal-payment-create" 를 넣는다
      │           (상세·수정이면 id 로 단건 조회도 여기서 한다)
      └─ 없다  → 모델에 넣지 않는다. 부모 페이지만 렌더링한다
```

- **정의되지 않은 `?m=` 값은 오류가 아니다.** 모달을 열지 않고 부모 페이지가 정상으로 뜬다.
  400·오류 화면으로 보내지 않는다(FR-630).
- `id` 가 필요한 모달(`detail`·`edit`)에 `id` 가 없거나 조회가 실패하면 **모달을 열지 않고**
  부모 페이지에 실패를 표시한다.
- 화면별 `?m=` 값 목록은 007 이 정하지 않는다. 008~012 가 자기 화면의 값과
  `data-modal-map` 을 함께 낸다. 007 은 **판정하는 방법과 모르는 값의 처리**만 고정한다.

### 열고 닫기 (FR-628)

`modal.js` 의 계약을 그대로 쓴다. **이 파일은 고치지 않는다.**

| 동작 | 방법 |
|---|---|
| 열기 | `data-modal-open="{id}"` 또는 `MoneyLogModal.open(id)` |
| 닫기 | `data-modal-close` · **Esc** · **딤 클릭** |
| 딥링크 | `<body data-modal-map='{"create":"modal-payment-create", ...}'>` 로 로드 시 자동 오픈 |

네 가지 닫기 경로(취소 버튼·X·Esc·딤)가 전부 동작해야 한다.

### 모달 껍데기

`templates/fragments/modal-shell.html :: modalShell`. 딤·카드·헤더(제목 + X)·푸터(취소/확인)
자리를 제공하고, 본문은 각 화면의 fragment 가 채운다.

## 4. 확인 다이얼로그 (FR-629)

`templates/fragments/confirm-dialog.html`. **하나를 공용으로 쓴다.**

| 쓰는 곳 | 화면 |
|---|---|
| 삭제 | 수단(2.x) · 지출유형(2.x) · 지출·소득(3.x) · 고정지출(4.x) |
| 회원 정지 | 1.8 관리자 회원 목록 |
| 할부 중도상환 | 5.1 월별 가계부 |

제목·본문·확인 버튼 문구를 파라미터로 받는다. 되돌릴 수 없는 삭제(고정지출은 월별 내역까지
사라진다)는 **그 사실을 본문에 적어** 넘긴다 — 다이얼로그가 문구를 만들지 않는다.

화면마다 `confirm()` 을 쓰거나 자체 다이얼로그를 만들지 않는다. 같은 성격의 확인이 화면마다
다르게 보이면 사용자가 무엇이 위험한 동작인지 배우지 못한다.

## 5. 화면 1.6 — 권한 없음 (FR-632)

| 항목 | 값 |
|---|---|
| URL | `GET /error/forbidden` |
| 템플릿 | `templates/error/forbidden.html` |
| 레이아웃 | **auth** (선행 개정 1번) |
| 권한 | 없음 (비로그인 허용) |
| `activeMenu` | — |
| 호출 API | **없다** |

이 화면만 007 이 만드는 이유는 둘이다. ① 권한 차단(FR-619)이 착지할 곳이 없으면 인터셉터를
완성할 수 없다. ② **API 를 부르지 않아** 다른 화면과 성격이 다르다 — 008~012 의 화면은 전부
백엔드를 부른다.

화면 구성은 `proto/error-forbidden.html` 을 옮긴다: 제목 「권한 없음」, 안내 문구,
버튼 둘(「가계부로」 → `/ledger`, 「로그인」 → `/auth/login`).

**미로그인 사용자도 이 화면을 볼 수 있다.** 다만 인터셉터가 로그인 판정을 먼저 하므로
관리자 URL 을 통해 여기 도달하지는 않는다 — 직접 주소를 친 경우에만이다.

## 6. 백엔드 불통·미정의 오류의 착지 (FR-606)

`templates/error.html`. 백엔드에 닿지 못했거나 처리 중 예외가 난 경우가 여기 온다.

**레이아웃은 auth 다.** FR-624 가 "모든 화면이 둘 중 하나"를 요구하는데, main 을 쓰면 사이드바가
`role` 을 필요로 한다 — 세션이 없거나 깨진 상태에서도 떠야 하는 화면이라 **로그인 여부와 무관하게
그려지는 쪽**이어야 한다. 1.6 이 auth 인 것과 같은 이유다.

**빈 목록·빈 값을 정상처럼 그리지 않는다**(SC-608). 데이터를 얻지 못했다는 사실이 화면에
글로 보여야 한다.

`whitelabel` 은 계속 꺼 둔다(현재 설정 유지).

## 7. 시험으로 고정할 것

| 항목 | 근거 |
|---|---|
| main 레이아웃 화면에 사이드바가 있고 `activeMenu` 가 활성으로 표시된다 | FR-625 |
| `role == 3` 응답 HTML 에 `/admin/members` 문자열이 **없다** | FR-626 |
| `?m=` 이 화이트리스트 값이면 모델에 `openModal` 이 있다 | FR-627 · SC-610 |
| `?m=` 이 모르는 값이면 `openModal` 이 **없고** HTTP 200 으로 부모가 뜬다 | FR-630 · SC-610 |
| `/error/forbidden` 이 미로그인으로 200 이다 | FR-632 |
| 세 정적 파일이 `static/` 에 있고 레이아웃이 건다 | FR-631 |
