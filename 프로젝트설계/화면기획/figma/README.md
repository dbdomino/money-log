# Figma에 넣는 방법

**PNG만 넣으세요.** PDF·SVG는 Figma에서 실패합니다.

## 하는 방법

1. 탐색기에서 이 폴더를 연다  
   `c:\a_project\money-log\프로젝트설계\화면기획\figma\png`
2. 안의 `.png`만 전부 선택 (`Ctrl + A`)
3. Figma 캔버스에 **끌어다 놓기**

또는 캔버스 클릭 → `Ctrl + Shift + K` → **png만** 고르기

## 넣지 말 것

| 파일 | 이유 |
|------|------|
| `*.pdf` | Figma가 PDF 가져오기 미지원 |
| `svg/*` | 변환 실패할 수 있음 (깨진 글자 등) |
| `proto/` 폴더 | 디자인 파일이 아님 |

## png ↔ 화면

| 파일 | 화면 |
|------|------|
| `01-auth-login.png` | 로그인 |
| `02-auth-signup.png` | 회원가입 |
| `02b-auth-find-id.png` | 아이디 찾기 |
| `02c-auth-find-password.png` | 비밀번호 찾기 |
| `02d-auth-reset-password.png` | 비밀번호 변경 |
| `03-error-forbidden.png` | 권한 없음 |
| `04-payment-list.png` | 수단 목록 |
| `04b-payment-create-modal.png` | 수단 등록 모달 |
| `05-expend-group-list.png` | 지출유형 |
| `06-ledger-monthly.png` | 가계부 |
| `06b-ledger-expense-modal.png` | 지출 등록 모달 |
| `07-ledger-excel.png` | 엑셀 |
| `08-fixed-expense-list.png` | 고정지출 |
| `09-member-admin-list.png` | 회원 관리 |
| `10-member-profile.png` | 본인 정보 |
| `11-target-default.png` | 목표금액 |
| `12-statistics-monthly.png` | 통계 |
