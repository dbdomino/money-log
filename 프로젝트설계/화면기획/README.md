# 화면기획

Thymeleaf(`money-app`) 이식 전제의 **그린 UI** 기획·정적 프로토타입.

> 상세 매트릭스: [01-화면구성.md](./01-화면구성.md)  
> 이식 대응: [02-Thymeleaf이식가이드.md](./02-Thymeleaf이식가이드.md)  
> 디자인: [00-디자인시스템.md](./00-디자인시스템.md)  
> 기능 명세: [기능명세상세-프론트엔드](../기능명세상세-프론트엔드/README.md)

## 규칙

- **목록·허브 = 풀페이지**, **등록·수정·상세 = 모달** (부모 목록/허브에 fragment).
- 지출·소득 모달 부모는 **5.1 월별 가계부** (`/ledger?m=expense-create` 등).
- CSS/JS는 `money-app/.../static/` 로 복사 가능한 경로·파일명.

## 디렉터리

```
화면기획/
├── README.md
├── 00-디자인시스템.md
├── 01-화면구성.md
├── 02-Thymeleaf이식가이드.md
├── css/          # → static/css/
├── js/           # → static/js/
└── proto/        # 브라우저로 열어 보는 샘플 HTML (가짜 내용 포함)
```

## 프로토타입 보는 법

1. `proto/auth-login.html` 또는 `proto/ledger-monthly.html` 을 브라우저로 연다.
2. 모달: 툴바 버튼 또는 URL `?m=create` / `?m=expense-create` 등.
3. 서버 연결 없음. 화면에 **가짜로 넣어 둔 숫자·이름**이 있음 → [예시데이터.md](./예시데이터.md)

### Figma로 보기

업로드용 파일: [`figma/`](./figma/README.md)

- **한 번에:** `figma/money-log-ui-for-figma.pdf` → Figma **File → Import**
- **묶음:** `figma/money-log-figma-export.zip` (PNG + SVG + PDF)
- **벡터 편집:** `figma/svg/*.svg` 드래그

| 프로토 | 대응 화면 |
|--------|-----------|
| `auth-login.html` | 1.1 |
| `auth-signup.html` | 1.2 |
| `error-forbidden.html` | 1.6 |
| `member-profile.html` | 1.7 |
| `member-admin-list.html` | 1.8 + 1.9·1.10 모달 |
| `payment-list.html` | 2.1 + 2.2~2.4 모달 |
| `expend-group-list.html` | 2.5 + 2.6~2.8 모달 |
| `ledger-excel.html` | 3.5 |
| `fixed-expense-list.html` | 4.2 + 4.3~4.6 모달 |
| `ledger-monthly.html` | 5.1 + 3.1~3.4 모달 |
| `target-default.html` | 6.1 |
| `statistics-monthly.html` | 6.2 |
| `layout-*-shell.html` | 레이아웃 골격만 |

## 다음 단계 (이 폴더 밖)

- `PageController` · `templates/` · `BackendApiClient` 연동
- 구 URL(` /payments/new` 등) → 부모 + `?m=` redirect
