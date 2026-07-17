# 기능명세 (API 상세)

> 기준: [2.1. 기능명세 - 백엔드.md](../2.1.%20기능명세%20-%20백엔드.md) · [2.기능명세.md](../2.기능명세.md)  
> **기능 1개 = 문서 1개**. 문서 제목은 **기능 이름**, 식별자는 **API 이름**을 사용한다.

## 공통

- Base URL: `http://localhost:8081/api/v1`
- 응답 래퍼·에러 코드·**JWT·Refresh Token**·**권한=로그인 시 토큰 검증 포함**·**비밀번호 bcrypt**·**목록 `list`(object[])**·**페이징 `offset`/`limit`/`totalCount`**·**지출유형 아이콘·프론트 수신**·**필드 설명(누락 금지)**: [_공통.md](./_공통.md)
- 문서 템플릿: [_TEMPLATE.md](./_TEMPLATE.md)  
  - 표 설명 칸은 자체 완결. `요청 에코`·`동일`·`공통 참고`만 적으면 **누락** ([_공통 § 필드 설명](./_공통.md#필드-설명-작성-규칙-누락))

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

#### 기타 회원 (1~2, 7~16)

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 1 | 헬스체크 | `HealthCheck` | [01-HealthCheck.md](./phase1-회원/01-HealthCheck.md) |
| 2 | 회원가입 | `MemberSignup` | [02-MemberSignup.md](./phase1-회원/02-MemberSignup.md) |
| 7 | 본인 정보 조회 | `MemberGetMe` | [07-MemberGetMe.md](./phase1-회원/07-MemberGetMe.md) |
| 8 | 본인 정보 수정 | `MemberUpdateMe` | [08-MemberUpdateMe.md](./phase1-회원/08-MemberUpdateMe.md) |
| 9 | id 찾기 | `MemberFindId` | [09-MemberFindId.md](./phase1-회원/09-MemberFindId.md) |
| 10 | 비밀번호 찾기 | `MemberFindPassword` | [10-MemberFindPassword.md](./phase1-회원/10-MemberFindPassword.md) |
| 11 | 비밀번호 변경 | `MemberResetPassword` | [11-MemberResetPassword.md](./phase1-회원/11-MemberResetPassword.md) |
| 12 | 회원 추가 | `AdminMemberCreate` | [12-AdminMemberCreate.md](./phase1-회원/12-AdminMemberCreate.md) |
| 13 | 회원 목록 | `AdminMemberList` | [13-AdminMemberList.md](./phase1-회원/13-AdminMemberList.md) |
| 14 | 회원 상세 조회 | `AdminMemberGet` | [14-AdminMemberGet.md](./phase1-회원/14-AdminMemberGet.md) |
| 15 | 다른 회원 수정 | `AdminMemberUpdate` | [15-AdminMemberUpdate.md](./phase1-회원/15-AdminMemberUpdate.md) |
| 16 | 관리자 회원 정지 | `AdminMemberDeactivate` | [16-AdminMemberDeactivate.md](./phase1-회원/16-AdminMemberDeactivate.md) |


### Phase 2 — 수단·지출유형

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 17 | 지출·소득 수단 등록 | `PaymentMethodCreate` | [17-PaymentMethodCreate.md](./phase2-수단-지출유형/17-PaymentMethodCreate.md) |
| 18 | 지출·소득 수단 전체 목록 | `PaymentMethodList` | [18-PaymentMethodList.md](./phase2-수단-지출유형/18-PaymentMethodList.md) |
| 19 | 지출·소득 수단 상세 조회 | `PaymentMethodGet` | [19-PaymentMethodGet.md](./phase2-수단-지출유형/19-PaymentMethodGet.md) |
| 20 | 지출·소득 수단 수정 | `PaymentMethodUpdate` | [20-PaymentMethodUpdate.md](./phase2-수단-지출유형/20-PaymentMethodUpdate.md) |
| 21 | 지출·소득 수단 삭제 | `PaymentMethodDelete` | [21-PaymentMethodDelete.md](./phase2-수단-지출유형/21-PaymentMethodDelete.md) |
| 22 | 사용 중인 지출·소득 수단 목록 | `PaymentMethodListActive` | [22-PaymentMethodListActive.md](./phase2-수단-지출유형/22-PaymentMethodListActive.md) |
| 23 | 지출유형 등록 | `ExpendGroupCreate` | [23-ExpendGroupCreate.md](./phase2-수단-지출유형/23-ExpendGroupCreate.md) |
| 24 | 지출유형 목록 | `ExpendGroupList` | [24-ExpendGroupList.md](./phase2-수단-지출유형/24-ExpendGroupList.md) |
| 25 | 지출유형 상세 조회 | `ExpendGroupGet` | [25-ExpendGroupGet.md](./phase2-수단-지출유형/25-ExpendGroupGet.md) |
| 26 | 지출유형 아이콘 조회 | `ExpendGroupIconGet` | [26-ExpendGroupIconGet.md](./phase2-수단-지출유형/26-ExpendGroupIconGet.md) |
| 27 | 지출유형 수정 | `ExpendGroupUpdate` | [27-ExpendGroupUpdate.md](./phase2-수단-지출유형/27-ExpendGroupUpdate.md) |
| 28 | 지출유형 삭제 | `ExpendGroupDelete` | [28-ExpendGroupDelete.md](./phase2-수단-지출유형/28-ExpendGroupDelete.md) |
| 29 | 사용 중인 지출유형 목록 | `ExpendGroupListActive` | [29-ExpendGroupListActive.md](./phase2-수단-지출유형/29-ExpendGroupListActive.md) |


### Phase 3 — 지출·소득

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 30 | 지출 등록 (일시불) | `ExpenseCreate` | [30-ExpenseCreate.md](./phase3-지출-소득/30-ExpenseCreate.md) |
| 31 | 지출 상세 조회 | `ExpenseGet` | [31-ExpenseGet.md](./phase3-지출-소득/31-ExpenseGet.md) |
| 32 | 지출 수정 | `ExpenseUpdate` | [32-ExpenseUpdate.md](./phase3-지출-소득/32-ExpenseUpdate.md) |
| 33 | 지출 삭제 | `ExpenseDelete` | [33-ExpenseDelete.md](./phase3-지출-소득/33-ExpenseDelete.md) |
| 34 | 지출 등록 (할부) | `ExpenseCreateInstallment` | [34-ExpenseCreateInstallment.md](./phase3-지출-소득/34-ExpenseCreateInstallment.md) |
| 35 | 할부 중도상환 | `ExpenseDeleteInstallmentRemainder` | [35-ExpenseDeleteInstallmentRemainder.md](./phase3-지출-소득/35-ExpenseDeleteInstallmentRemainder.md) |
| 36 | 소득 등록 | `IncomeCreate` | [36-IncomeCreate.md](./phase3-지출-소득/36-IncomeCreate.md) |
| 37 | 소득 상세 조회 | `IncomeGet` | [37-IncomeGet.md](./phase3-지출-소득/37-IncomeGet.md) |
| 38 | 소득 수정 | `IncomeUpdate` | [38-IncomeUpdate.md](./phase3-지출-소득/38-IncomeUpdate.md) |
| 39 | 소득 삭제 | `IncomeDelete` | [39-IncomeDelete.md](./phase3-지출-소득/39-IncomeDelete.md) |


### Phase 4 — 가계부·고정지출·엑셀

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 40 | 고정지출 등록 | `FixedExpenseCreate` | [40-FixedExpenseCreate.md](./phase4-가계부/40-FixedExpenseCreate.md) |
| 41 | 고정지출 설정 목록 | `FixedExpenseList` | [41-FixedExpenseList.md](./phase4-가계부/41-FixedExpenseList.md) |
| 42 | 고정지출 상세 조회 | `FixedExpenseGet` | [42-FixedExpenseGet.md](./phase4-가계부/42-FixedExpenseGet.md) |
| 43 | 고정지출 기본값 수정 | `FixedExpenseUpdate` | [43-FixedExpenseUpdate.md](./phase4-가계부/43-FixedExpenseUpdate.md) |
| 44 | 고정지출 월별 수정 조회 | `FixedExpenseMonthlyOverrideGet` | [44-FixedExpenseMonthlyOverrideGet.md](./phase4-가계부/44-FixedExpenseMonthlyOverrideGet.md) |
| 45 | 고정지출 월별 수정 저장 | `FixedExpenseMonthlyOverrideUpsert` | [45-FixedExpenseMonthlyOverrideUpsert.md](./phase4-가계부/45-FixedExpenseMonthlyOverrideUpsert.md) |
| 46 | 고정지출 삭제 | `FixedExpenseDelete` | [46-FixedExpenseDelete.md](./phase4-가계부/46-FixedExpenseDelete.md) |
| 47 | 월별 통합 목록 | `LedgerMonthlyList` | [47-LedgerMonthlyList.md](./phase4-가계부/47-LedgerMonthlyList.md) |
| 48 | 엑셀 양식 다운로드 | `LedgerExcelTemplateDownload` | [48-LedgerExcelTemplateDownload.md](./phase4-가계부/48-LedgerExcelTemplateDownload.md) |
| 49 | 엑셀 업로드·일괄 등록 | `LedgerExcelUpload` | [49-LedgerExcelUpload.md](./phase4-가계부/49-LedgerExcelUpload.md) |


### Phase 5 — 목표금액·통계

| 순서 | 기능 이름 | API 이름 | 문서 |
|------|-----------|----------|------|
| 50 | 기본 목표금액 목록 | `ExpendTargetDefaultList` | [50-ExpendTargetDefaultList.md](./phase5-목표-통계/50-ExpendTargetDefaultList.md) |
| 51 | 기본 목표금액 등록·수정 | `ExpendTargetDefaultUpsert` | [51-ExpendTargetDefaultUpsert.md](./phase5-목표-통계/51-ExpendTargetDefaultUpsert.md) |
| 52 | 월별 목표금액 등록·수정 | `ExpendTargetMonthlyUpsert` | [52-ExpendTargetMonthlyUpsert.md](./phase5-목표-통계/52-ExpendTargetMonthlyUpsert.md) |
| 53 | 월별 통계 조회 | `StatisticsMonthlyGet` | [53-StatisticsMonthlyGet.md](./phase5-목표-통계/53-StatisticsMonthlyGet.md) |
| 54 | 월별 통계 저장 | `StatisticsMonthlySave` | [54-StatisticsMonthlySave.md](./phase5-목표-통계/54-StatisticsMonthlySave.md) |


