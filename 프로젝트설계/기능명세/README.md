# 기능명세 (API 상세)

> 기준: [2.1. 기능명세 - 백엔드.md](../2.1.%20기능명세%20-%20백엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> **기능 1개 = 문서 1개**. 문서 제목은 **기능 이름**, 식별자는 **API 이름**을 사용한다.  
> **기능번호** = `{Phase}.{순번}` (예: `1.1`, `3.12`). Phase 안에서만 1부터 센다.

## 공통

- Base URL: `http://localhost:8081/api/v1`
- 응답 래퍼·에러 코드·**JWT·Refresh Token**·**권한=로그인 시 토큰 검증 포함**·**비밀번호 bcrypt**·**목록 `list`(object[])**·**페이징 `offset`/`limit`/`totalCount`**·**지출유형 아이콘·프론트 수신**·**필드 설명(누락 금지)**: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)  
  - 표 설명 칸은 자체 완결. `요청 에코`·`동일`·`공통 참고`만 적으면 **누락** ([_공통 § 필드 설명](./_공통.md#필드-설명-작성-규칙-누락))

## 색인 (기능번호)

### Phase 1 — 회원·인증

#### 인증·토큰 (1.3~1.6)

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| — | *(개요)* | — | [_인증-토큰.md](./phase1-회원/_인증-토큰.md) |
| 1.3 | 로그인 (토큰 발급) | `MemberLogin` | [1.3-MemberLogin.md](./phase1-회원/1.3-MemberLogin.md) |
| 1.4 | 토큰 검증 | `MemberTokenValidate` | [1.4-MemberTokenValidate.md](./phase1-회원/1.4-MemberTokenValidate.md) |
| 1.5 | 토큰 갱신 | `MemberTokenRefresh` | [1.5-MemberTokenRefresh.md](./phase1-회원/1.5-MemberTokenRefresh.md) |
| 1.6 | 로그아웃 (JWT Token 비활성화) | `MemberTokenRevoke` | [1.6-MemberTokenRevoke.md](./phase1-회원/1.6-MemberTokenRevoke.md) |

#### 기타 회원 (1.1~1.2, 1.7~1.16)

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| 1.1 | 헬스체크 | `HealthCheck` | [1.1-HealthCheck.md](./phase1-회원/1.1-HealthCheck.md) |
| 1.2 | 회원가입 | `MemberSignup` | [1.2-MemberSignup.md](./phase1-회원/1.2-MemberSignup.md) |
| 1.7 | 본인 정보 조회 | `MemberGetMe` | [1.7-MemberGetMe.md](./phase1-회원/1.7-MemberGetMe.md) |
| 1.8 | 본인 정보 수정 | `MemberUpdateMe` | [1.8-MemberUpdateMe.md](./phase1-회원/1.8-MemberUpdateMe.md) |
| 1.9 | id 찾기 | `MemberFindId` | [1.9-MemberFindId.md](./phase1-회원/1.9-MemberFindId.md) |
| 1.10 | 비밀번호 찾기 | `MemberFindPassword` | [1.10-MemberFindPassword.md](./phase1-회원/1.10-MemberFindPassword.md) |
| 1.11 | 비밀번호 변경 | `MemberResetPassword` | [1.11-MemberResetPassword.md](./phase1-회원/1.11-MemberResetPassword.md) |
| 1.12 | 회원 추가 | `AdminMemberCreate` | [1.12-AdminMemberCreate.md](./phase1-회원/1.12-AdminMemberCreate.md) |
| 1.13 | 회원 목록 | `AdminMemberList` | [1.13-AdminMemberList.md](./phase1-회원/1.13-AdminMemberList.md) |
| 1.14 | 회원 상세 조회 | `AdminMemberGet` | [1.14-AdminMemberGet.md](./phase1-회원/1.14-AdminMemberGet.md) |
| 1.15 | 다른 회원 수정 | `AdminMemberUpdate` | [1.15-AdminMemberUpdate.md](./phase1-회원/1.15-AdminMemberUpdate.md) |
| 1.16 | 관리자 회원 정지 | `AdminMemberDeactivate` | [1.16-AdminMemberDeactivate.md](./phase1-회원/1.16-AdminMemberDeactivate.md) |


### Phase 2 — 수단·지출유형

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| 2.1 | 지출·소득 수단 등록 | `PaymentMethodCreate` | [2.1-PaymentMethodCreate.md](./phase2-수단-지출유형/2.1-PaymentMethodCreate.md) |
| 2.2 | 지출·소득 수단 전체 목록 | `PaymentMethodList` | [2.2-PaymentMethodList.md](./phase2-수단-지출유형/2.2-PaymentMethodList.md) |
| 2.3 | 지출·소득 수단 상세 조회 | `PaymentMethodGet` | [2.3-PaymentMethodGet.md](./phase2-수단-지출유형/2.3-PaymentMethodGet.md) |
| 2.4 | 지출·소득 수단 수정 | `PaymentMethodUpdate` | [2.4-PaymentMethodUpdate.md](./phase2-수단-지출유형/2.4-PaymentMethodUpdate.md) |
| 2.5 | 지출·소득 수단 삭제 | `PaymentMethodDelete` | [2.5-PaymentMethodDelete.md](./phase2-수단-지출유형/2.5-PaymentMethodDelete.md) |
| 2.6 | 사용 중인 지출·소득 수단 목록 | `PaymentMethodListActive` | [2.6-PaymentMethodListActive.md](./phase2-수단-지출유형/2.6-PaymentMethodListActive.md) |
| 2.7 | 지출유형 등록 | `ExpendGroupCreate` | [2.7-ExpendGroupCreate.md](./phase2-수단-지출유형/2.7-ExpendGroupCreate.md) |
| 2.8 | 지출유형 목록 | `ExpendGroupList` | [2.8-ExpendGroupList.md](./phase2-수단-지출유형/2.8-ExpendGroupList.md) |
| 2.9 | 지출유형 상세 조회 | `ExpendGroupGet` | [2.9-ExpendGroupGet.md](./phase2-수단-지출유형/2.9-ExpendGroupGet.md) |
| 2.10 | 지출유형 아이콘 조회 | `ExpendGroupIconGet` | [2.10-ExpendGroupIconGet.md](./phase2-수단-지출유형/2.10-ExpendGroupIconGet.md) |
| 2.11 | 지출유형 수정 | `ExpendGroupUpdate` | [2.11-ExpendGroupUpdate.md](./phase2-수단-지출유형/2.11-ExpendGroupUpdate.md) |
| 2.12 | 지출유형 삭제 | `ExpendGroupDelete` | [2.12-ExpendGroupDelete.md](./phase2-수단-지출유형/2.12-ExpendGroupDelete.md) |
| 2.13 | 사용 중인 지출유형 목록 | `ExpendGroupListActive` | [2.13-ExpendGroupListActive.md](./phase2-수단-지출유형/2.13-ExpendGroupListActive.md) |


### Phase 3 — 지출·소득

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| 3.1 | 지출 등록 (일시불) | `ExpenseCreate` | [3.1-ExpenseCreate.md](./phase3-지출-소득/3.1-ExpenseCreate.md) |
| 3.2 | 지출 상세 조회 | `ExpenseGet` | [3.2-ExpenseGet.md](./phase3-지출-소득/3.2-ExpenseGet.md) |
| 3.3 | 지출 수정 | `ExpenseUpdate` | [3.3-ExpenseUpdate.md](./phase3-지출-소득/3.3-ExpenseUpdate.md) |
| 3.4 | 지출 삭제 | `ExpenseDelete` | [3.4-ExpenseDelete.md](./phase3-지출-소득/3.4-ExpenseDelete.md) |
| 3.5 | 지출 등록 (할부) | `ExpenseCreateInstallment` | [3.5-ExpenseCreateInstallment.md](./phase3-지출-소득/3.5-ExpenseCreateInstallment.md) |
| 3.6 | 할부 중도상환 | `ExpenseSettleInstallmentRemainder` | [3.6-ExpenseSettleInstallmentRemainder.md](./phase3-지출-소득/3.6-ExpenseSettleInstallmentRemainder.md) |
| 3.7 | 소득 등록 | `IncomeCreate` | [3.7-IncomeCreate.md](./phase3-지출-소득/3.7-IncomeCreate.md) |
| 3.8 | 소득 상세 조회 | `IncomeGet` | [3.8-IncomeGet.md](./phase3-지출-소득/3.8-IncomeGet.md) |
| 3.9 | 소득 수정 | `IncomeUpdate` | [3.9-IncomeUpdate.md](./phase3-지출-소득/3.9-IncomeUpdate.md) |
| 3.10 | 소득 삭제 | `IncomeDelete` | [3.10-IncomeDelete.md](./phase3-지출-소득/3.10-IncomeDelete.md) |
| 3.11 | 지출·소득 엑셀 양식 다운로드 | `ExpenseIncomeExcelTemplateDownload` | [3.11-ExpenseIncomeExcelTemplateDownload.md](./phase3-지출-소득/3.11-ExpenseIncomeExcelTemplateDownload.md) |
| 3.12 | 지출·소득 엑셀 일괄 등록 | `ExpenseIncomeExcelUpload` | [3.12-ExpenseIncomeExcelUpload.md](./phase3-지출-소득/3.12-ExpenseIncomeExcelUpload.md) |


### Phase 4 — 가계부·고정지출

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| 4.1 | 고정지출 등록 | `FixedExpenseCreate` | [4.1-FixedExpenseCreate.md](./phase4-가계부/4.1-FixedExpenseCreate.md) |
| 4.2 | 고정지출 설정 목록 | `FixedExpenseList` | [4.2-FixedExpenseList.md](./phase4-가계부/4.2-FixedExpenseList.md) |
| 4.3 | 고정지출 상세 조회 | `FixedExpenseGet` | [4.3-FixedExpenseGet.md](./phase4-가계부/4.3-FixedExpenseGet.md) |
| 4.4 | 고정지출 기본값 수정 | `FixedExpenseUpdate` | [4.4-FixedExpenseUpdate.md](./phase4-가계부/4.4-FixedExpenseUpdate.md) |
| 4.5 | 고정지출 월별 수정 조회 | `FixedExpenseMonthlyOverrideGet` | [4.5-FixedExpenseMonthlyOverrideGet.md](./phase4-가계부/4.5-FixedExpenseMonthlyOverrideGet.md) |
| 4.6 | 고정지출 월별 수정 저장 | `FixedExpenseMonthlyOverrideUpsert` | [4.6-FixedExpenseMonthlyOverrideUpsert.md](./phase4-가계부/4.6-FixedExpenseMonthlyOverrideUpsert.md) |
| 4.7 | 고정지출 삭제 | `FixedExpenseDelete` | [4.7-FixedExpenseDelete.md](./phase4-가계부/4.7-FixedExpenseDelete.md) |
| 4.8 | 월별 가계부 목록 조회 | `LedgerMonthlyList` | [4.8-LedgerMonthlyList.md](./phase4-가계부/4.8-LedgerMonthlyList.md) |


### Phase 5 — 목표금액·통계

| 기능번호 | 기능 이름 | API 이름 | 문서 |
|----------|-----------|----------|------|
| 5.1 | 목표금액 목록 | `ExpendTargetList` | [5.1-ExpendTargetList.md](./phase5-목표-통계/5.1-ExpendTargetList.md) |
| 5.2 | 목표금액 상세 조회 | `ExpendTargetGet` | [5.2-ExpendTargetGet.md](./phase5-목표-통계/5.2-ExpendTargetGet.md) |
| 5.3 | 기본 목표금액 등록·수정 | `ExpendTargetDefaultUpsert` | [5.3-ExpendTargetDefaultUpsert.md](./phase5-목표-통계/5.3-ExpendTargetDefaultUpsert.md) |
| 5.4 | 월별 목표금액 등록·수정 | `ExpendTargetMonthlyUpsert` | [5.4-ExpendTargetMonthlyUpsert.md](./phase5-목표-통계/5.4-ExpendTargetMonthlyUpsert.md) |
| 5.5 | 월별 통계 조회 | `StatisticsMonthlyGet` | [5.5-StatisticsMonthlyGet.md](./phase5-목표-통계/5.5-StatisticsMonthlyGet.md) |
| 5.6 | 월별 통계 저장 | `StatisticsMonthlySave` | [5.6-StatisticsMonthlySave.md](./phase5-목표-통계/5.6-StatisticsMonthlySave.md) |
