# Figma 업로드용 산출물

프로토 HTML을 캡처한 **PNG / SVG / PDF** 입니다. Figma에 드래그하거나 PDF를 Import하면 화면을 바로 볼 수 있습니다.

## 빠른 방법 (권장)

### A. PDF 한 장으로 보기

1. Figma 파일 열기 (또는 New design file)
2. 메뉴 **File → Import** (또는 캔버스에 드롭)
3. `money-log-ui-for-figma.pdf` 선택  
4. 페이지마다 프레임이 생성됩니다.

### B. PNG를 프레임으로 배치

1. `png/` 폴더의 이미지를 캔버스로 드래그
2. 프레임 이름 = 파일명 (화면 번호 순)

| 파일 | 화면 |
|------|------|
| `01-auth-login.png` | 1.1 로그인 |
| `02-auth-signup.png` | 1.2 회원가입 |
| `03-error-forbidden.png` | 1.6 권한 없음 |
| `04-payment-list.png` | 2.1 수단 목록 |
| `04b-payment-create-modal.png` | 2.2 수단 등록 모달 |
| `05-expend-group-list.png` | 2.5 지출유형 |
| `06-ledger-monthly.png` | 5.1 가계부 |
| `06b-ledger-expense-modal.png` | 3.1 지출 등록 모달 |
| `07-ledger-excel.png` | 3.5 엑셀 |
| `08-fixed-expense-list.png` | 4.2 고정지출 |
| `09-member-admin-list.png` | 1.8 회원 관리 |
| `10-member-profile.png` | 1.7 본인 정보 |
| `11-target-default.png` | 6.1 목표금액 |
| `12-statistics-monthly.png` | 6.2 통계 |

### C. SVG 와이어 (편집용)

`svg/` 는 **벡터**라 Figma에서 색·텍스트를 직접 고칠 수 있습니다.

| 파일 | 용도 |
|------|------|
| `00-design-system.svg` | 컬러·뱃지·버튼·타이포 |
| `01-wire-main-ledger.svg` | main + 가계부 골격 |
| `02-wire-auth-login.svg` | auth 로그인 |
| `03-wire-modal.svg` | 모달 셸 |

드래그 → 우클릭 **Ungroup** / **Flatten** 없이 레이어로 분해해 편집.

## ZIP

배포용 묶음: `money-log-figma-export.zip`  
(위 PNG + SVG + PDF + 이 README)

## 재생성

```bash
cd 프로젝트설계/화면기획/figma
node capture.mjs
# PDF는 루트에서: python로 png → pdf (README 참고 스크립트는 capture 후)
```

원본 인터랙션(모달 열기 등)은 `../proto/*.html` 브라우저 확인.

## 참고

- Figma는 **HTML을 직접 올리지 않습니다**. PNG/SVG/PDF만 Import합니다.
- 실제 CSS는 `../css/tokens.css`, `../css/ui.css`.
- 편집 가능한 컴포넌트 라이브러리가 필요하면 SVG를 바탕으로 Auto Layout을 다시 잡으면 됩니다.
