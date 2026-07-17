# 기능명세 (API 상세)

> 기준: [2.1. 기능명세 - 백엔드.md](../2.1.%20기능명세%20-%20백엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> **기능 1개 = 문서 1개**. 문서 제목은 **기능 이름**, 식별자는 **API 이름**을 사용한다.

## 공통

- Base URL: `http://localhost:8081/api/v1`
- 응답 래퍼·에러 코드·**JWT·Refresh Token**·**비밀번호 bcrypt**: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)

## 색인 (구현순서)

### Phase 1 — 회원·인증

#### 인증·토큰 (3~6)

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| — | *(개요)* | — | [_인증-토큰.md](./phase1-회원/_인증-토큰.md) |
| 3 | 로그인 (토큰 발급) | `MemberLogin` | [03-MemberLogin.md](./phase1-회원/03-MemberLogin.md) |
| 4 | 토큰 검증 | `MemberTokenValidate` | [04-MemberTokenValidate.md](./phase1-회원/04-MemberTokenValidate.md) |
| 5 | 토큰 갱신 | `MemberTokenRefresh` | [05-MemberTokenRefresh.md](./phase1-회원/05-MemberTokenRefresh.md) |
| 6 | 로그아웃 (JWT Token 비활성화) | `MemberTokenRevoke` | [06-MemberTokenRevoke.md](./phase1-회원/06-MemberTokenRevoke.md) |

#### 기타 회원 (1~2, 7~15)

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 1 | 헬스체크 | `HealthCheck` | [01-HealthCheck.md](./phase1-회원/01-HealthCheck.md) |
| 2 | 회원가입 | `MemberSignup` | [02-MemberSignup.md](./phase1-회원/02-MemberSignup.md) |
| 7 | 본인 정보 조회 | `MemberGetMe` | [07-MemberGetMe.md](./phase1-회원/07-MemberGetMe.md) |
| 8 | 본인 정보 수정 | `MemberUpdateMe` | [08-MemberUpdateMe.md](./phase1-회원/08-MemberUpdateMe.md) |
| 9 | id 찾기 | `MemberFindId` | [09-MemberFindId.md](./phase1-회원/09-MemberFindId.md) |
| 10 | 비밀번호 찾기 | `MemberResetPassword` | [10-MemberResetPassword.md](./phase1-회원/10-MemberResetPassword.md) |
| 11 | 회원 추가 | `AdminMemberCreate` | [11-AdminMemberCreate.md](./phase1-회원/11-AdminMemberCreate.md) |
| 12 | 회원 목록 | `AdminMemberList` | [12-AdminMemberList.md](./phase1-회원/12-AdminMemberList.md) |
| 13 | 회원 상세 조회 | `AdminMemberGet` | [13-AdminMemberGet.md](./phase1-회원/13-AdminMemberGet.md) |
| 14 | 다른 회원 수정 | `AdminMemberUpdate` | [14-AdminMemberUpdate.md](./phase1-회원/14-AdminMemberUpdate.md) |
| 15 | 관리자 회원 정지 | `AdminMemberDeactivate` | [15-AdminMemberDeactivate.md](./phase1-회원/15-AdminMemberDeactivate.md) |

### Phase 2 — 수단·지출유형

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 16 | 지출·소득 수단 등록 | `PaymentMethodCreate` | [16-PaymentMethodCreate.md](./phase2-수단-지출유형/16-PaymentMethodCreate.md) |
| 17 | 지출·소득 수단 목록 (관리) | `PaymentMethodList` | [17-PaymentMethodList.md](./phase2-수단-지출유형/17-PaymentMethodList.md) |
| 18 | 지출·소득 수단 상세 조회 | `PaymentMethodGet` | [18-PaymentMethodGet.md](./phase2-수단-지출유형/18-PaymentMethodGet.md) |
| 19 | 지출·소득 수단 수정 | `PaymentMethodUpdate` | [19-PaymentMethodUpdate.md](./phase2-수단-지출유형/19-PaymentMethodUpdate.md) |
| 20 | 지출·소득 수단 삭제 | `PaymentMethodDelete` | [20-PaymentMethodDelete.md](./phase2-수단-지출유형/20-PaymentMethodDelete.md) |
| 21 | 지출·소득 수단 목록 (입력용) | `PaymentMethodListActive` | [21-PaymentMethodListActive.md](./phase2-수단-지출유형/21-PaymentMethodListActive.md) |
| 22 | 지출유형 등록 | `ExpendGroupCreate` | [22-ExpendGroupCreate.md](./phase2-수단-지출유형/22-ExpendGroupCreate.md) |
| 23 | 지출유형 목록 | `ExpendGroupList` | [23-ExpendGroupList.md](./phase2-수단-지출유형/23-ExpendGroupList.md) |
| 24 | 지출유형 상세 조회 | `ExpendGroupGet` | [24-ExpendGroupGet.md](./phase2-수단-지출유형/24-ExpendGroupGet.md) |
| 25 | 지출유형 아이콘 조회 | `ExpendGroupIconGet` | [25-ExpendGroupIconGet.md](./phase2-수단-지출유형/25-ExpendGroupIconGet.md) |
| 26 | 지출유형 수정 | `ExpendGroupUpdate` | [26-ExpendGroupUpdate.md](./phase2-수단-지출유형/26-ExpendGroupUpdate.md) |
| 27 | 지출유형 삭제 | `ExpendGroupDelete` | [27-ExpendGroupDelete.md](./phase2-수단-지출유형/27-ExpendGroupDelete.md) |
| 28 | 지출유형 목록 (입력용) | `ExpendGroupListActive` | [28-ExpendGroupListActive.md](./phase2-수단-지출유형/28-ExpendGroupListActive.md) |

### Phase 3 — 지출·소득

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 29 | 지출 등록 (일시불) | `ExpenseCreate` | [29-ExpenseCreate.md](./phase3-지출-소득/29-ExpenseCreate.md) |
| 30 | 지출 상세 조회 | `ExpenseGet` | [30-ExpenseGet.md](./phase3-지출-소득/30-ExpenseGet.md) |
| 31 | 지출 수정 | `ExpenseUpdate` | [31-ExpenseUpdate.md](./phase3-지출-소득/31-ExpenseUpdate.md) |
| 32 | 지출 삭제 | `ExpenseDelete` | [32-ExpenseDelete.md](./phase3-지출-소득/32-ExpenseDelete.md) |
| 33 | 지출 등록 (할부) | `ExpenseCreateInstallment` | [33-ExpenseCreateInstallment.md](./phase3-지출-소득/33-ExpenseCreateInstallment.md) |
| 34 | 할부 중도상환 | `ExpenseDeleteInstallmentRemainder` | [34-ExpenseDeleteInstallmentRemainder.md](./phase3-지출-소득/34-ExpenseDeleteInstallmentRemainder.md) |
| 35 | 소득 등록 | `IncomeCreate` | [35-IncomeCreate.md](./phase3-지출-소득/35-IncomeCreate.md) |
| 36 | 소득 상세 조회 | `IncomeGet` | [36-IncomeGet.md](./phase3-지출-소득/36-IncomeGet.md) |
| 37 | 소득 수정 | `IncomeUpdate` | [37-IncomeUpdate.md](./phase3-지출-소득/37-IncomeUpdate.md) |
| 38 | 소득 삭제 | `IncomeDelete` | [38-IncomeDelete.md](./phase3-지출-소득/38-IncomeDelete.md) |

### Phase 4 — 가계부·고정지출·엑셀

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 39 | 고정지출 등록 | `FixedExpenseCreate` | [39-FixedExpenseCreate.md](./phase4-가계부/39-FixedExpenseCreate.md) |
| 40 | 고정지출 목록 (관리) | `FixedExpenseList` | [40-FixedExpenseList.md](./phase4-가계부/40-FixedExpenseList.md) |
| 41 | 고정지출 상세 조회 | `FixedExpenseGet` | [41-FixedExpenseGet.md](./phase4-가계부/41-FixedExpenseGet.md) |
| 42 | 고정지출 기본값 수정 | `FixedExpenseUpdate` | [42-FixedExpenseUpdate.md](./phase4-가계부/42-FixedExpenseUpdate.md) |
| 43 | 고정지출 월별 수정 조회 | `FixedExpenseMonthlyOverrideGet` | [43-FixedExpenseMonthlyOverrideGet.md](./phase4-가계부/43-FixedExpenseMonthlyOverrideGet.md) |
| 44 | 고정지출 월별 수정 저장 | `FixedExpenseMonthlyOverrideUpsert` | [44-FixedExpenseMonthlyOverrideUpsert.md](./phase4-가계부/44-FixedExpenseMonthlyOverrideUpsert.md) |
| 45 | 고정지출 삭제 | `FixedExpenseDelete` | [45-FixedExpenseDelete.md](./phase4-가계부/45-FixedExpenseDelete.md) |
| 46 | 월별 통합 목록 | `LedgerMonthlyList` | [46-LedgerMonthlyList.md](./phase4-가계부/46-LedgerMonthlyList.md) |
| 47 | 엑셀 양식 다운로드 | `LedgerExcelTemplateDownload` | [47-LedgerExcelTemplateDownload.md](./phase4-가계부/47-LedgerExcelTemplateDownload.md) |
| 48 | 엑셀 업로드·일괄 등록 | `LedgerExcelUpload` | [48-LedgerExcelUpload.md](./phase4-가계부/48-LedgerExcelUpload.md) |

### Phase 5 — 목표금액·통계

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 49 | 기본 목표금액 목록 | `ExpendTargetDefaultList` | [49-ExpendTargetDefaultList.md](./phase5-목표-통계/49-ExpendTargetDefaultList.md) |
| 50 | 기본 목표금액 등록·수정 | `ExpendTargetDefaultUpsert` | [50-ExpendTargetDefaultUpsert.md](./phase5-목표-통계/50-ExpendTargetDefaultUpsert.md) |
| 51 | 월별 목표금액 등록·수정 | `ExpendTargetMonthlyUpsert` | [51-ExpendTargetMonthlyUpsert.md](./phase5-목표-통계/51-ExpendTargetMonthlyUpsert.md) |
| 52 | 월별 통계 조회 | `StatisticsMonthlyGet` | [52-StatisticsMonthlyGet.md](./phase5-목표-통계/52-StatisticsMonthlyGet.md) |
| 53 | 월별 통계 저장 | `StatisticsMonthlySave` | [53-StatisticsMonthlySave.md](./phase5-목표-통계/53-StatisticsMonthlySave.md) |
