# Contract: 루트 분기와 구 URL redirect

**Feature**: 007-frontend-foundation | **관련 FR**: 633 · 634

`_공통.md` 와 `화면기획/01-화면구성.md` 는 "구 경로는 구현 시 부모 + `?m=` 로 redirect"까지만
정하고 **목록을 남기지 않았다.** 여기가 확정본이다. 선행 개정 3번이 이 문서를 참조한다.

## 1. 루트 (FR-633)

| 상태 | 결과 |
|---|---|
| 세션에 토큰이 있다 | `302 → /ledger` (월별 가계부, 화면 5.1) |
| 세션이 비었다 | `302 → /auth/login` (화면 1.1) |

루트에서 **토큰 유효성까지 확인하지 않는다.** 세션 존재만 보고 보내고, 실제 검증은 착지한
화면의 인터셉터가 한다 — 여기서 또 검증하면 왕복이 두 번이 된다.

`/ledger` 는 010 이, `/auth/login` 은 008 이 만든다. **007 시점에는 두 주소 모두 화면이 없다.**
시험은 `Location` 헤더를 확인하고 렌더링 여부는 보지 않는다.

## 2. 실재했던 레거시 URL

지금 `money-app` 에 실제로 매핑돼 있는 주소다. 컨트롤러와 함께 사라진다.

| 구 URL | 무엇이었나 | 처리 |
|---|---|---|
| `/mem/login` (GET) | 레거시 로그인 페이지 (`signin.html`) | `301 → /auth/login` |
| `/mem/login` (POST) | 레거시 로그인 제출 | **없앤다.** redirect 하지 않는다 — POST 본문 형식이 다르고 백엔드 계약과도 어긋난다 |
| `/mem/ind` (GET) | 레거시 홈 (`home.html`) | `301 → /` (루트 규칙을 다시 탄다) |
| `/api/ammounts/**` (GET) | 구 REST 엔드포인트 (JSON) | **없앤다.** 화면 URL 이 아니라 redirect 대상이 아니다. 프론트가 `/api/**` 를 노출하지 않는다 |

`/api/ammounts/**` 를 살리지 않는 이유: 프론트가 REST 를 제공하면 브라우저가 화면 모듈을
API 처럼 부르는 길이 생긴다. 007 이 만드는 프론트 엔드포인트는 아이콘 프록시 하나뿐이고,
그것도 화면 자원이다.

## 3. 구 화면 URL → 부모 + `?m=` (FR-634)

`_공통.md` 와 `화면기획` 이 예로 든 "등록 전용 페이지" 관례 주소다. 실재한 적은 없지만
**사용자가 손으로 칠 법한 주소**이고, 두 문서가 redirect 하기로 이미 정했다.

| 구 URL | 보낼 곳 | 열릴 모달 |
|---|---|---|
| `/payments/new` | `/payments?m=create` | 2.2 수단 등록 |
| `/payments/{id}` | `/payments?m=detail&id={id}` | 2.3 수단 상세 |
| `/payments/{id}/edit` | `/payments?m=edit&id={id}` | 2.4 수단 수정 |
| `/expend-groups/new` | `/expend-groups?m=create` | 2.6 지출유형 등록 |
| `/expend-groups/{id}` | `/expend-groups?m=detail&id={id}` | 2.7 지출유형 상세 |
| `/expend-groups/{id}/edit` | `/expend-groups?m=edit&id={id}` | 2.8 지출유형 수정 |
| `/ledger/expenses/new` | `/ledger?m=expense-create` | 3.1 지출 등록 |
| `/ledger/expenses/{id}/edit` | `/ledger?m=expense-edit&id={id}` | 3.2 지출 수정 |
| `/ledger/incomes/new` | `/ledger?m=income-create` | 3.3 소득 등록 |
| `/ledger/incomes/{id}/edit` | `/ledger?m=income-edit&id={id}` | 3.4 소득 수정 |
| `/fixed-expenses/new` | `/fixed-expenses?m=create` | 4.3 고정지출 등록 |
| `/fixed-expenses/{id}` | `/fixed-expenses?m=detail&id={id}` | 4.4 고정지출 상세 |
| `/fixed-expenses/{id}/edit` | `/fixed-expenses?m=edit&id={id}` | 4.5 고정지출 수정 |
| `/fixed-expenses/monthly` | `/fixed-expenses?m=monthly` | 4.6 고정지출 월별 |
| `/admin/members/new` | `/admin/members?m=create` | 1.9 회원 추가 |
| `/admin/members/{id}/edit` | `/admin/members?m=edit&id={id}` | 1.10 회원 수정 |

**주의 — `/expend-groups/icons/{filename}` 은 redirect 대상이 아니다.** 아이콘 프록시
엔드포인트이며 `/expend-groups/{id}` 패턴보다 **먼저** 매칭되어야 한다. 매핑 순서를 잘못
두면 아이콘 요청이 상세 모달 redirect 로 새어 나간다.

`/statistics`·`/targets/default`·`/ledger/excel`·`/member/profile` 은 대표 URL 자체가
페이지라 구 URL 이 없다.

## 4. redirect 규칙

| 항목 | 값 |
|---|---|
| 상태 코드 | `301`(구 URL) · `302`(루트 분기) |
| 권한 | **redirect 도 인터셉터를 탄다.** 미로그인으로 `/payments/new` 에 오면 `/auth/login` 이 먼저다 |
| `id` 누락 | 부모 페이지로만 보낸다 (`?m=` 없이) |
| 목록에 없는 주소 | redirect 하지 않는다. Spring 기본 404 |

루트 분기를 `302` 로 두는 이유는 로그인 상태에 따라 목적지가 바뀌기 때문이다 —
`301` 이면 브라우저가 캐시해 로그아웃 후에도 `/ledger` 로 간다.

구 URL 은 목적지가 고정이라 `301` 이다.

## 5. 시험으로 고정할 것

| 항목 | 근거 |
|---|---|
| 세션 있음 + `/` → `Location: /ledger` | FR-633 |
| 세션 없음 + `/` → `Location: /auth/login` | FR-633 |
| `/mem/login` → `Location: /auth/login` | FR-634 |
| `/payments/1/edit` → `Location: /payments?m=edit&id=1` | FR-634 |
| `/expend-groups/icons/1_1.png` 가 상세 redirect 로 새지 않는다 | 매핑 순서 |
| 미로그인 + `/payments/new` → `Location: /auth/login` (모달 URL 이 아니다) | FR-618 |
