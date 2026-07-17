# 기능명세 (API 상세)

> 기준: [2.1. 기능명세 - 백엔드.md](../2.1.%20기능명세%20-%20백엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> **기능 1개 = 문서 1개**. 문서 제목은 **기능 이름**, 식별자는 **API 이름**을 사용한다.

## 공통

- Base URL: `http://localhost:8081/api/v1`
- 응답 래퍼·에러 코드·**JWT·Refresh Token**·**비밀번호 bcrypt**: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)

## 색인 (구현순서)

### Phase 1 — 회원·인증

#### 인증·토큰 (3~5)

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| — | *(개요)* | — | [_인증-토큰.md](./phase1-회원/_인증-토큰.md) |
| 3 | 로그인 (토큰 발급) | `MemberLogin` | [03-MemberLogin.md](./phase1-회원/03-MemberLogin.md) |
| 4 | 토큰 갱신 | `MemberTokenRefresh` | [04-MemberTokenRefresh.md](./phase1-회원/04-MemberTokenRefresh.md) |
| 5 | 로그아웃 (JWT Token 비활성화) | `MemberTokenRevoke` | [05-MemberTokenRevoke.md](./phase1-회원/05-MemberTokenRevoke.md) |

#### 기타 회원 (1~2, 6~14)

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 1 | 헬스체크 | `HealthCheck` | [01-HealthCheck.md](./phase1-회원/01-HealthCheck.md) |
| 2 | 회원가입 | `MemberSignup` | [02-MemberSignup.md](./phase1-회원/02-MemberSignup.md) |
| 6 | 본인 정보 조회 | `MemberGetMe` | [06-MemberGetMe.md](./phase1-회원/06-MemberGetMe.md) |
| 7 | 본인 정보 수정 | `MemberUpdateMe` | [07-MemberUpdateMe.md](./phase1-회원/07-MemberUpdateMe.md) |
| 8 | id 찾기 | `MemberFindId` | [08-MemberFindId.md](./phase1-회원/08-MemberFindId.md) |
| 9 | 비밀번호 찾기 | `MemberResetPassword` | [09-MemberResetPassword.md](./phase1-회원/09-MemberResetPassword.md) |
| 10 | 회원 추가 | `AdminMemberCreate` | [10-AdminMemberCreate.md](./phase1-회원/10-AdminMemberCreate.md) |
| 11 | 회원 목록 | `AdminMemberList` | [11-AdminMemberList.md](./phase1-회원/11-AdminMemberList.md) |
| 12 | 회원 상세 조회 | `AdminMemberGet` | [12-AdminMemberGet.md](./phase1-회원/12-AdminMemberGet.md) |
| 13 | 다른 회원 수정 | `AdminMemberUpdate` | [13-AdminMemberUpdate.md](./phase1-회원/13-AdminMemberUpdate.md) |
| 14 | 관리자 회원 정지 | `AdminMemberDeactivate` | [14-AdminMemberDeactivate.md](./phase1-회원/14-AdminMemberDeactivate.md) |

### Phase 2 — 수단·지출유형

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 15 | 지출·소득 수단 등록 | `PaymentMethodCreate` | [15-PaymentMethodCreate.md](./phase2-수단-지출유형/15-PaymentMethodCreate.md) |
| 16 | 지출·소득 수단 목록 (관리) | `PaymentMethodList` | [16-PaymentMethodList.md](./phase2-수단-지출유형/16-PaymentMethodList.md) |
| 17 | 지출·소득 수단 상세 조회 | `PaymentMethodGet` | [17-PaymentMethodGet.md](./phase2-수단-지출유형/17-PaymentMethodGet.md) |
| 18 | 지출·소득 수단 수정 | `PaymentMethodUpdate` | [18-PaymentMethodUpdate.md](./phase2-수단-지출유형/18-PaymentMethodUpdate.md) |
| 19 | 지출·소득 수단 삭제 | `PaymentMethodDelete` | [19-PaymentMethodDelete.md](./phase2-수단-지출유형/19-PaymentMethodDelete.md) |
| 20 | 지출·소득 수단 목록 (입력용) | `PaymentMethodListActive` | [20-PaymentMethodListActive.md](./phase2-수단-지출유형/20-PaymentMethodListActive.md) |
| 21 | 지출유형 등록 | `ExpendGroupCreate` | [21-ExpendGroupCreate.md](./phase2-수단-지출유형/21-ExpendGroupCreate.md) |
| 22 | 지출유형 목록 | `ExpendGroupList` | [22-ExpendGroupList.md](./phase2-수단-지출유형/22-ExpendGroupList.md) |
| 23 | 지출유형 상세 조회 | `ExpendGroupGet` | [23-ExpendGroupGet.md](./phase2-수단-지출유형/23-ExpendGroupGet.md) |
| 24 | 지출유형 아이콘 조회 | `ExpendGroupIconGet` | [24-ExpendGroupIconGet.md](./phase2-수단-지출유형/24-ExpendGroupIconGet.md) |
| 25 | 지출유형 수정 | `ExpendGroupUpdate` | [25-ExpendGroupUpdate.md](./phase2-수단-지출유형/25-ExpendGroupUpdate.md) |
| 26 | 지출유형 삭제 | `ExpendGroupDelete` | [26-ExpendGroupDelete.md](./phase2-수단-지출유형/26-ExpendGroupDelete.md) |
| 27 | 지출유형 목록 (입력용) | `ExpendGroupListActive` | [27-ExpendGroupListActive.md](./phase2-수단-지출유형/27-ExpendGroupListActive.md) |

### Phase 3 — 지출·소득

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 28 | 지출 등록 (일시불) | `ExpenseCreate` | [28-ExpenseCreate.md](./phase3-지출-소득/28-ExpenseCreate.md) |
| 29 | 지출 상세 조회 | `ExpenseGet` | [29-ExpenseGet.md](./phase3-지출-소득/29-ExpenseGet.md) |
| 30 | 지출 수정 | `ExpenseUpdate` | [30-ExpenseUpdate.md](./phase3-지출-소득/30-ExpenseUpdate.md) |
| 31 | 지출 삭제 | `ExpenseDelete` | [31-ExpenseDelete.md](./phase3-지출-소득/31-ExpenseDelete.md) |
| 32 | 지출 등록 (할부) | `ExpenseCreateInstallment` | [32-ExpenseCreateInstallment.md](./phase3-지출-소득/32-ExpenseCreateInstallment.md) |
| 33 | 할부 중도상환 | `ExpenseDeleteInstallmentRemainder` | [33-ExpenseDeleteInstallmentRemainder.md](./phase3-지출-소득/33-ExpenseDeleteInstallmentRemainder.md) |
| 34 | 소득 등록 | `IncomeCreate` | [34-IncomeCreate.md](./phase3-지출-소득/34-IncomeCreate.md) |
| 35 | 소득 상세 조회 | `IncomeGet` | [35-IncomeGet.md](./phase3-지출-소득/35-IncomeGet.md) |
| 36 | 소득 수정 | `IncomeUpdate` | [36-IncomeUpdate.md](./phase3-지출-소득/36-IncomeUpdate.md) |
| 37 | 소득 삭제 | `IncomeDelete` | [37-IncomeDelete.md](./phase3-지출-소득/37-IncomeDelete.md) |

### Phase 4 — 가계부·고정지출·엑셀

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 38 | 고정지출 등록 | `FixedExpenseCreate` | [38-FixedExpenseCreate.md](./phase4-가계부/38-FixedExpenseCreate.md) |
| 39 | 고정지출 목록 (관리) | `FixedExpenseList` | [39-FixedExpenseList.md](./phase4-가계부/39-FixedExpenseList.md) |
| 40 | 고정지출 상세 조회 | `FixedExpenseGet` | [40-FixedExpenseGet.md](./phase4-가계부/40-FixedExpenseGet.md) |
| 41 | 고정지출 기본값 수정 | `FixedExpenseUpdate` | [41-FixedExpenseUpdate.md](./phase4-가계부/41-FixedExpenseUpdate.md) |
| 42 | 고정지출 월별 수정 조회 | `FixedExpenseMonthlyOverrideGet` | [42-FixedExpenseMonthlyOverrideGet.md](./phase4-가계부/42-FixedExpenseMonthlyOverrideGet.md) |
| 43 | 고정지출 월별 수정 저장 | `FixedExpenseMonthlyOverrideUpsert` | [43-FixedExpenseMonthlyOverrideUpsert.md](./phase4-가계부/43-FixedExpenseMonthlyOverrideUpsert.md) |
| 44 | 고정지출 삭제 | `FixedExpenseDelete` | [44-FixedExpenseDelete.md](./phase4-가계부/44-FixedExpenseDelete.md) |
| 45 | 월별 통합 목록 | `LedgerMonthlyList` | [45-LedgerMonthlyList.md](./phase4-가계부/45-LedgerMonthlyList.md) |
| 46 | 엑셀 양식 다운로드 | `LedgerExcelTemplateDownload` | [46-LedgerExcelTemplateDownload.md](./phase4-가계부/46-LedgerExcelTemplateDownload.md) |
| 47 | 엑셀 업로드·일괄 등록 | `LedgerExcelUpload` | [47-LedgerExcelUpload.md](./phase4-가계부/47-LedgerExcelUpload.md) |

### Phase 5 — 목표금액·통계

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 48 | 기본 목표금액 목록 | `ExpendTargetDefaultList` | [48-ExpendTargetDefaultList.md](./phase5-목표-통계/48-ExpendTargetDefaultList.md) |
| 49 | 기본 목표금액 등록·수정 | `ExpendTargetDefaultUpsert` | [49-ExpendTargetDefaultUpsert.md](./phase5-목표-통계/49-ExpendTargetDefaultUpsert.md) |
| 50 | 월별 목표금액 등록·수정 | `ExpendTargetMonthlyUpsert` | [50-ExpendTargetMonthlyUpsert.md](./phase5-목표-통계/50-ExpendTargetMonthlyUpsert.md) |
| 51 | 월별 통계 조회 | `StatisticsMonthlyGet` | [51-StatisticsMonthlyGet.md](./phase5-목표-통계/51-StatisticsMonthlyGet.md) |
| 52 | 월별 통계 저장 | `StatisticsMonthlySave` | [52-StatisticsMonthlySave.md](./phase5-목표-통계/52-StatisticsMonthlySave.md) |
