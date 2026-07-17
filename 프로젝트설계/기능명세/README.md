# 기능명세 (API 상세)

> 기준: [2.1. 기능명세 - 백엔드.md](../2.1.%20기능명세%20-%20백엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> **기능 1개 = 문서 1개**. 문서 제목은 **기능 이름**, 식별자는 **API 이름**을 사용한다.

## 공통

- Base URL: `http://localhost:8081/api/v1`
- 응답 래퍼·에러 코드·**비밀번호 bcrypt**: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)

## 색인 (구현순서)

### Phase 1 — 회원·인증

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 1 | 헬스체크 | `HealthCheck` | [01-HealthCheck.md](./phase1-회원/01-HealthCheck.md) |
| 2 | 회원가입 | `MemberSignup` | [02-MemberSignup.md](./phase1-회원/02-MemberSignup.md) |
| 3 | 로그인 | `MemberLogin` | [03-MemberLogin.md](./phase1-회원/03-MemberLogin.md) |
| 4 | 본인 정보 조회 | `MemberGetMe` | [04-MemberGetMe.md](./phase1-회원/04-MemberGetMe.md) |
| 5 | 본인 정보 수정 | `MemberUpdateMe` | [05-MemberUpdateMe.md](./phase1-회원/05-MemberUpdateMe.md) |
| 6 | 로그아웃 | `MemberLogout` | [06-MemberLogout.md](./phase1-회원/06-MemberLogout.md) |
| 7 | id 찾기 | `MemberFindId` | [07-MemberFindId.md](./phase1-회원/07-MemberFindId.md) |
| 8 | 비밀번호 찾기 | `MemberResetPassword` | [08-MemberResetPassword.md](./phase1-회원/08-MemberResetPassword.md) |
| 9 | 회원 추가 | `AdminMemberCreate` | [09-AdminMemberCreate.md](./phase1-회원/09-AdminMemberCreate.md) |
| 10 | 회원 목록 | `AdminMemberList` | [10-AdminMemberList.md](./phase1-회원/10-AdminMemberList.md) |
| 11 | 회원 상세 조회 | `AdminMemberGet` | [11-AdminMemberGet.md](./phase1-회원/11-AdminMemberGet.md) |
| 12 | 다른 회원 수정 | `AdminMemberUpdate` | [12-AdminMemberUpdate.md](./phase1-회원/12-AdminMemberUpdate.md) |
| 13 | 비활성화 | `AdminMemberDeactivate` | [13-AdminMemberDeactivate.md](./phase1-회원/13-AdminMemberDeactivate.md) |

### Phase 2 — 수단·지출유형

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 14 | 지출·소득 수단 등록 | `PaymentMethodCreate` | [14-PaymentMethodCreate.md](./phase2-수단-지출유형/14-PaymentMethodCreate.md) |
| 15 | 지출·소득 수단 목록 (관리) | `PaymentMethodList` | [15-PaymentMethodList.md](./phase2-수단-지출유형/15-PaymentMethodList.md) |
| 16 | 지출·소득 수단 상세 조회 | `PaymentMethodGet` | [16-PaymentMethodGet.md](./phase2-수단-지출유형/16-PaymentMethodGet.md) |
| 17 | 지출·소득 수단 수정 | `PaymentMethodUpdate` | [17-PaymentMethodUpdate.md](./phase2-수단-지출유형/17-PaymentMethodUpdate.md) |
| 18 | 지출·소득 수단 삭제 | `PaymentMethodDelete` | [18-PaymentMethodDelete.md](./phase2-수단-지출유형/18-PaymentMethodDelete.md) |
| 19 | 지출·소득 수단 목록 (입력용) | `PaymentMethodListActive` | [19-PaymentMethodListActive.md](./phase2-수단-지출유형/19-PaymentMethodListActive.md) |
| 20 | 지출유형 등록 | `ExpendGroupCreate` | [20-ExpendGroupCreate.md](./phase2-수단-지출유형/20-ExpendGroupCreate.md) |
| 21 | 지출유형 목록 | `ExpendGroupList` | [21-ExpendGroupList.md](./phase2-수단-지출유형/21-ExpendGroupList.md) |
| 22 | 지출유형 상세 조회 | `ExpendGroupGet` | [22-ExpendGroupGet.md](./phase2-수단-지출유형/22-ExpendGroupGet.md) |
| 23 | 지출유형 아이콘 조회 | `ExpendGroupIconGet` | [23-ExpendGroupIconGet.md](./phase2-수단-지출유형/23-ExpendGroupIconGet.md) |
| 24 | 지출유형 수정 | `ExpendGroupUpdate` | [24-ExpendGroupUpdate.md](./phase2-수단-지출유형/24-ExpendGroupUpdate.md) |
| 25 | 지출유형 삭제 | `ExpendGroupDelete` | [25-ExpendGroupDelete.md](./phase2-수단-지출유형/25-ExpendGroupDelete.md) |
| 26 | 지출유형 목록 (입력용) | `ExpendGroupListActive` | [26-ExpendGroupListActive.md](./phase2-수단-지출유형/26-ExpendGroupListActive.md) |

### Phase 3 — 지출·소득

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 27 | 지출 등록 (일시불) | `ExpenseCreate` | [27-ExpenseCreate.md](./phase3-지출-소득/27-ExpenseCreate.md) |
| 28 | 지출 상세 조회 | `ExpenseGet` | [28-ExpenseGet.md](./phase3-지출-소득/28-ExpenseGet.md) |
| 29 | 지출 수정 | `ExpenseUpdate` | [29-ExpenseUpdate.md](./phase3-지출-소득/29-ExpenseUpdate.md) |
| 30 | 지출 삭제 | `ExpenseDelete` | [30-ExpenseDelete.md](./phase3-지출-소득/30-ExpenseDelete.md) |
| 31 | 지출 등록 (할부) | `ExpenseCreateInstallment` | [31-ExpenseCreateInstallment.md](./phase3-지출-소득/31-ExpenseCreateInstallment.md) |
| 32 | 할부 중도상환 | `ExpenseDeleteInstallmentRemainder` | [32-ExpenseDeleteInstallmentRemainder.md](./phase3-지출-소득/32-ExpenseDeleteInstallmentRemainder.md) |
| 33 | 소득 등록 | `IncomeCreate` | [33-IncomeCreate.md](./phase3-지출-소득/33-IncomeCreate.md) |
| 34 | 소득 상세 조회 | `IncomeGet` | [34-IncomeGet.md](./phase3-지출-소득/34-IncomeGet.md) |
| 35 | 소득 수정 | `IncomeUpdate` | [35-IncomeUpdate.md](./phase3-지출-소득/35-IncomeUpdate.md) |
| 36 | 소득 삭제 | `IncomeDelete` | [36-IncomeDelete.md](./phase3-지출-소득/36-IncomeDelete.md) |

### Phase 4 — 가계부·고정지출·엑셀

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 37 | 고정지출 등록 | `FixedExpenseCreate` | [37-FixedExpenseCreate.md](./phase4-가계부/37-FixedExpenseCreate.md) |
| 38 | 고정지출 목록 (관리) | `FixedExpenseList` | [38-FixedExpenseList.md](./phase4-가계부/38-FixedExpenseList.md) |
| 39 | 고정지출 상세 조회 | `FixedExpenseGet` | [39-FixedExpenseGet.md](./phase4-가계부/39-FixedExpenseGet.md) |
| 40 | 고정지출 기본값 수정 | `FixedExpenseUpdate` | [40-FixedExpenseUpdate.md](./phase4-가계부/40-FixedExpenseUpdate.md) |
| 41 | 고정지출 월별 수정 조회 | `FixedExpenseMonthlyOverrideGet` | [41-FixedExpenseMonthlyOverrideGet.md](./phase4-가계부/41-FixedExpenseMonthlyOverrideGet.md) |
| 42 | 고정지출 월별 수정 저장 | `FixedExpenseMonthlyOverrideUpsert` | [42-FixedExpenseMonthlyOverrideUpsert.md](./phase4-가계부/42-FixedExpenseMonthlyOverrideUpsert.md) |
| 43 | 고정지출 삭제 | `FixedExpenseDelete` | [43-FixedExpenseDelete.md](./phase4-가계부/43-FixedExpenseDelete.md) |
| 44 | 월별 통합 목록 | `LedgerMonthlyList` | [44-LedgerMonthlyList.md](./phase4-가계부/44-LedgerMonthlyList.md) |
| 45 | 엑셀 양식 다운로드 | `LedgerExcelTemplateDownload` | [45-LedgerExcelTemplateDownload.md](./phase4-가계부/45-LedgerExcelTemplateDownload.md) |
| 46 | 엑셀 업로드·일괄 등록 | `LedgerExcelUpload` | [46-LedgerExcelUpload.md](./phase4-가계부/46-LedgerExcelUpload.md) |

### Phase 5 — 목표금액·통계

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 47 | 기본 목표금액 목록 | `ExpendTargetDefaultList` | [47-ExpendTargetDefaultList.md](./phase5-목표-통계/47-ExpendTargetDefaultList.md) |
| 48 | 기본 목표금액 등록·수정 | `ExpendTargetDefaultUpsert` | [48-ExpendTargetDefaultUpsert.md](./phase5-목표-통계/48-ExpendTargetDefaultUpsert.md) |
| 49 | 월별 목표금액 등록·수정 | `ExpendTargetMonthlyUpsert` | [49-ExpendTargetMonthlyUpsert.md](./phase5-목표-통계/49-ExpendTargetMonthlyUpsert.md) |
| 50 | 월별 통계 조회 | `StatisticsMonthlyGet` | [50-StatisticsMonthlyGet.md](./phase5-목표-통계/50-StatisticsMonthlyGet.md) |
| 51 | 월별 통계 저장 | `StatisticsMonthlySave` | [51-StatisticsMonthlySave.md](./phase5-목표-통계/51-StatisticsMonthlySave.md) |

