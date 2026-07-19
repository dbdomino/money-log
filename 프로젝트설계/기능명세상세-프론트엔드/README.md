# [프론트] 기능명세 (화면 상세)

> 기준: [2.2. 기능명세 - 프론트엔드.md](../2.2.%20기능명세%20-%20프론트엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> 화면 맵: [money-log-화면구성.drawio](./money-log-화면구성.drawio) · [화면구조.mermaid.md](./화면구조.mermaid.md)  
> **화면 1개 = 문서 1개**. 문서 제목은 **`[프론트] {화면 이름}`**, 식별자는 **화면 ID**를 사용한다.  
> **화면번호** = `{Phase}.{순번}` (예: `1.1`, `5.1`). Phase 안에서만 센다.  
> API 요청·응답 필드는 [기능명세상세-백엔드](../기능명세상세-백엔드/README.md)를 따른다.

## 공통

- Base URL (화면): `http://localhost:8080`
- 백엔드 API: `http://localhost:8081/api/v1`
- 인증·레이아웃·API 호출·페이징 UI·사이드바: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)  
  - 표 설명 칸은 자체 완결. `동일`·`공통 참고`만 적으면 **누락** ([_공통 § 필드 설명](./_공통.md#필드-설명-작성-규칙-누락))

## 작성 상태

| 상태 | 의미 |
|------|------|
| 골격 | 메타·URL·템플릿만. 상세(구성·동작) 미작성 |
| 작성중 | 상세 초안 |
| 완료 | 구현 가능 수준 |

현재 전 화면 **골격**.

---

## 색인 (화면번호)

> Phase 구분은 [money-log-화면구성.drawio](money-log-화면구조.drawio)와 동일.  
> **4.1은 없음** (고정지출은 `4.2`부터).

### Phase 1 — 회원·인증

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 1.1 | 로그인 | `AuthLogin` | `/auth/login` | [1.1-AuthLogin.md](./phase1-회원/1.1-AuthLogin.md) |
| 1.2 | 회원가입 | `AuthSignup` | `/auth/signup` | [1.2-AuthSignup.md](./phase1-회원/1.2-AuthSignup.md) |
| 1.3 | 아이디 찾기 | `AuthFindId` | `/auth/find-id` | [1.3-AuthFindId.md](./phase1-회원/1.3-AuthFindId.md) |
| 1.4 | 비밀번호 찾기 | `AuthFindPassword` | `/auth/find-password` | [1.4-AuthFindPassword.md](./phase1-회원/1.4-AuthFindPassword.md) |
| 1.5 | 비밀번호 변경 | `AuthResetPassword` | `/auth/reset-password` | [1.5-AuthResetPassword.md](./phase1-회원/1.5-AuthResetPassword.md) |
| 1.6 | 권한 없음 | `ErrorForbidden` | `/error/forbidden` | [1.6-ErrorForbidden.md](./phase1-회원/1.6-ErrorForbidden.md) |
| 1.7 | 본인 정보 | `MemberProfile` | `/member/profile` | [1.7-MemberProfile.md](./phase1-회원/1.7-MemberProfile.md) |
| 1.8 | 회원 목록 | `AdminMemberList` | `/admin/members` | [1.8-AdminMemberList.md](./phase1-회원/1.8-AdminMemberList.md) |
| 1.9 | 회원 추가 | `AdminMemberCreate` | `/admin/members/new` | [1.9-AdminMemberCreate.md](./phase1-회원/1.9-AdminMemberCreate.md) |
| 1.10 | 회원 수정 | `AdminMemberEdit` | `/admin/members/{id}/edit` | [1.10-AdminMemberEdit.md](./phase1-회원/1.10-AdminMemberEdit.md) |

토큰 검증·갱신·로그아웃은 **전용 화면 없음** → [_공통 § 인증·토큰](./_공통.md#인증토큰-화면-공통)

### Phase 2 — 수단·지출유형

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 2.1 | 수단 목록 | `PaymentList` | `/payments` | [2.1-PaymentList.md](./phase2-수단-지출유형/2.1-PaymentList.md) |
| 2.2 | 수단 등록 | `PaymentCreate` | `/payments/new` | [2.2-PaymentCreate.md](./phase2-수단-지출유형/2.2-PaymentCreate.md) |
| 2.3 | 수단 상세 | `PaymentDetail` | `/payments/{id}` | [2.3-PaymentDetail.md](./phase2-수단-지출유형/2.3-PaymentDetail.md) |
| 2.4 | 수단 수정 | `PaymentEdit` | `/payments/{id}/edit` | [2.4-PaymentEdit.md](./phase2-수단-지출유형/2.4-PaymentEdit.md) |
| 2.5 | 지출유형 목록 | `ExpendGroupList` | `/expend-groups` | [2.5-ExpendGroupList.md](./phase2-수단-지출유형/2.5-ExpendGroupList.md) |
| 2.6 | 지출유형 등록 | `ExpendGroupCreate` | `/expend-groups/new` | [2.6-ExpendGroupCreate.md](./phase2-수단-지출유형/2.6-ExpendGroupCreate.md) |
| 2.7 | 지출유형 상세 | `ExpendGroupDetail` | `/expend-groups/{id}` | [2.7-ExpendGroupDetail.md](./phase2-수단-지출유형/2.7-ExpendGroupDetail.md) |
| 2.8 | 지출유형 수정 | `ExpendGroupEdit` | `/expend-groups/{id}/edit` | [2.8-ExpendGroupEdit.md](./phase2-수단-지출유형/2.8-ExpendGroupEdit.md) |

### Phase 3 — 지출·소득

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 3.1 | 지출 등록 | `ExpenseCreate` | `/ledger/expenses/new` | [3.1-ExpenseCreate.md](./phase3-지출-소득/3.1-ExpenseCreate.md) |
| 3.2 | 지출 수정 | `ExpenseEdit` | `/ledger/expenses/{id}/edit` | [3.2-ExpenseEdit.md](./phase3-지출-소득/3.2-ExpenseEdit.md) |
| 3.3 | 소득 등록 | `IncomeCreate` | `/ledger/incomes/new` | [3.3-IncomeCreate.md](./phase3-지출-소득/3.3-IncomeCreate.md) |
| 3.4 | 소득 수정 | `IncomeEdit` | `/ledger/incomes/{id}/edit` | [3.4-IncomeEdit.md](./phase3-지출-소득/3.4-IncomeEdit.md) |
| 3.5 | 지출·소득 엑셀 등록 | `ExpenseIncomeExcel` | `/ledger/excel` | [3.5-ExpenseIncomeExcel.md](./phase3-지출-소득/3.5-ExpenseIncomeExcel.md) |

### Phase 4 — 고정지출

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 4.2 | 고정지출 설정 목록 | `FixedExpenseList` | `/fixed-expenses` | [4.2-FixedExpenseList.md](./phase4-고정지출/4.2-FixedExpenseList.md) |
| 4.3 | 고정지출 등록 | `FixedExpenseCreate` | `/fixed-expenses/new` | [4.3-FixedExpenseCreate.md](./phase4-고정지출/4.3-FixedExpenseCreate.md) |
| 4.4 | 고정지출 상세 | `FixedExpenseDetail` | `/fixed-expenses/{id}` | [4.4-FixedExpenseDetail.md](./phase4-고정지출/4.4-FixedExpenseDetail.md) |
| 4.5 | 고정지출 수정 | `FixedExpenseEdit` | `/fixed-expenses/{id}/edit` | [4.5-FixedExpenseEdit.md](./phase4-고정지출/4.5-FixedExpenseEdit.md) |
| 4.6 | 고정지출 월별 수정 | `FixedExpenseMonthly` | `/fixed-expenses/{id}/monthly` | [4.6-FixedExpenseMonthly.md](./phase4-고정지출/4.6-FixedExpenseMonthly.md) |

### Phase 5 — 가계부

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 5.1 | 월별 가계부 | `LedgerMonthly` | `/ledger` | [5.1-LedgerMonthly.md](./phase5-가계부/5.1-LedgerMonthly.md) |

### Phase 6 — 목표금액·통계

| 화면번호 | 화면 이름 | 화면 ID | URL | 문서 |
|----------|-----------|---------|-----|------|
| 6.1 | 기본 목표금액 | `TargetDefault` | `/targets/default` | [6.1-TargetDefault.md](./phase6-목표-통계/6.1-TargetDefault.md) |
| 6.2 | 월별 통계 | `StatisticsMonthly` | `/statistics` | [6.2-StatisticsMonthly.md](./phase6-목표-통계/6.2-StatisticsMonthly.md) |

---

## 화면 URL 빠른 조회

| URL | 화면번호 | 화면 이름 |
|-----|----------|-----------|
| `/` → `/ledger` (미로그인 → `/auth/login`) | — | 루트 redirect |
| `/auth/login` | 1.1 | 로그인 |
| `/auth/signup` | 1.2 | 회원가입 |
| `/auth/find-id` | 1.3 | 아이디 찾기 |
| `/auth/find-password` | 1.4 | 비밀번호 찾기 |
| `/auth/reset-password` | 1.5 | 비밀번호 변경 |
| `/error/forbidden` | 1.6 | 권한 없음 |
| `/member/profile` | 1.7 | 본인 정보 |
| `/admin/members` | 1.8 | 회원 목록 |
| `/admin/members/new` | 1.9 | 회원 추가 |
| `/admin/members/{id}/edit` | 1.10 | 회원 수정 |
| `/payments` | 2.1 | 수단 목록 |
| `/payments/new` | 2.2 | 수단 등록 |
| `/payments/{id}` | 2.3 | 수단 상세 |
| `/payments/{id}/edit` | 2.4 | 수단 수정 |
| `/expend-groups` | 2.5 | 지출유형 목록 |
| `/expend-groups/new` | 2.6 | 지출유형 등록 |
| `/expend-groups/{id}` | 2.7 | 지출유형 상세 |
| `/expend-groups/{id}/edit` | 2.8 | 지출유형 수정 |
| `/ledger/expenses/new` | 3.1 | 지출 등록 |
| `/ledger/expenses/{id}/edit` | 3.2 | 지출 수정 |
| `/ledger/incomes/new` | 3.3 | 소득 등록 |
| `/ledger/incomes/{id}/edit` | 3.4 | 소득 수정 |
| `/ledger/excel` | 3.5 | 지출·소득 엑셀 등록 |
| `/fixed-expenses` | 4.2 | 고정지출 설정 목록 |
| `/fixed-expenses/new` | 4.3 | 고정지출 등록 |
| `/fixed-expenses/{id}` | 4.4 | 고정지출 상세 |
| `/fixed-expenses/{id}/edit` | 4.5 | 고정지출 수정 |
| `/fixed-expenses/{id}/monthly` | 4.6 | 고정지출 월별 수정 |
| `/ledger` | 5.1 | 월별 가계부 |
| `/targets/default` | 6.1 | 기본 목표금액 |
| `/statistics` | 6.2 | 월별 통계 |

**합계:** 화면 문서 **31개** (4.1 없음) + 루트 redirect
