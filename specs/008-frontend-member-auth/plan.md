# Implementation Plan: 프론트 회원·인증 화면

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-frontend-member-auth/spec.md`

## Summary

007 이 세운 공통 기반 위에 **화면 9개**를 얹는다. 1.1~1.5 는 로그인 전 껍데기, 1.7~1.10 은 로그인 후
껍데기를 쓰고, 백엔드 002 의 API 12건을 부른다.

007 과 성격이 반대다.

| 축 | 007 (공통 기반) | 008 (회원·인증 화면) |
|---|---|---|
| 하는 일 | 있던 것을 **걷어내고** 통로를 놓는다 | 그 통로 위에 **화면을 얹는다** |
| 새로 만드는 것 | 화면 1개, 공통 장치 여럿 | 화면 9개, 공통 장치 **하나**(폼 실패 착지) |
| 완료 판정 | DB 로 가는 길이 없는가 | 사용자가 가입부터 관리까지 **끝까지 가는가** |
| 건드리는 공통 코드 | 전부 | **없다** — 007 을 고치지 않고 얹는다 |

이 마지막 줄이 계획의 모양을 정한다.

**① 007 을 고치지 않는다.** 008 이 007 의 공통 코드를 고치기 시작하면 009~012 도 그렇게 하고,
공통 기반이 화면별 사정을 하나씩 알게 된다. 008 이 필요한 착지 변경은 **컨트롤러 안의 선언**으로
해결한다 — 컨트롤러 지역 선언이 공통 선언보다 먼저 잡히는 것이 프레임워크의 규칙이다(research 1).

**② 폼 실패는 폼으로 돌아온다.** 007 의 공통 실패 처리는 오류 화면으로 보내는데, 폼에서는 그것이
입력 일곱 칸을 날리는 동작이다. 착지를 폼으로 바꾸고 값을 되채우는 규약을 한 번 정해
[contracts/form-failure.md](./contracts/form-failure.md) 에 두고 화면 8개가 같은 방식을 쓴다.

**③ 비밀번호는 어느 방향으로도 남기지 않는다.** 실패로 폼을 다시 그릴 때도 비밀번호 칸은 비운다.
한 번만 허용해도 평문이 HTML 에 실리고 브라우저 캐시와 방문 기록에 남는다(SC-708).

기술 결정 14건은 [research.md](./research.md) 에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Thymeleaf · Bean Validation ·
`common-mod`(에러코드 Enum 참조용). **이 기능은 의존성을 더하지 않는다.** 007 이 남긴
`common-mod`·웹·타임리프·검증 넷으로 충분하다.

007 에서 받아 가는 것은 코드이지 의존이 아니다 — 백엔드 호출 통로, 로그인 세션, 진입 판정,
레이아웃 둘, 모달 껍데기, 확인 다이얼로그, 페이징 환산, 모달 딥링크 판정.

**Storage**: **없다.** 화면 모듈은 데이터베이스를 모른다(007 이 그렇게 만들었다).
`sql/schema-moneylogdb.sql` 은 읽지도 바꾸지도 않는다. 화면이 들고 있는 상태는 007 의 로그인 세션과
008 이 더하는 **비밀번호 재설정 표식** 하나뿐이다([data-model.md](./data-model.md)).

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test` ·
`MockRestServiceServer`. 백엔드를 띄우지 않고 응답을 세워 화면을 검증한다 — 007 이 그 방식을 이미
깔아 두었다(`BackendClientFixture`).

**Target Platform**: JVM 서버. 화면 `:8080`, 백엔드 `:8081`.

**Project Type**: Spring Boot 멀티모듈 — 이 기능은 프론트 모듈 하나만 손댄다.

**Performance Goals**: 정하지 않는다. 다만 **성격은 기록한다** — 회원 목록은 쪽당 행 수만큼만
받아 오고, 수정 모달은 열릴 때 단건 조회가 한 번 더 나간다. 화면 하나에 백엔드 호출이 최대 2회다.

**Constraints**:
- **007 의 공통 코드를 고치지 않는다.** 고쳐야 할 것이 나오면 그것은 007 의 결함이므로 007 쪽을
  고치고 근거를 남긴다(research 14 가 그런 경우다).
- 브라우저는 **GET 과 POST 만** 쓴다. 백엔드가 요구하는 메서드로 옮기는 일은 화면 모듈이 한다.
- 화면 모듈은 **에러코드를 새로 정의하지 않는다.** 백엔드 코드를 그대로 옮긴다.
- 로그인 실패는 **아이디의 존재 여부를 드러내지 않는다**(FR-706 · SC-702).
- 비밀번호 평문을 화면·로그·모델 어디에도 남기지 않는다(SC-708).
- 폰은 **화면 모듈이** 숫자만 남겨 보낸다. 브라우저 스크립트에 맡기지 않는다(FR-703 · SC-704).
- 회원 목록 검색은 **아이디와 닉네임 두 칸**이다. 백엔드가 두 조건을 AND 로 묶는다(research 5).
- 관리자 화면 3개의 권한 판정은 007 이 한다. 008 이 다시 판정하지 않는다.

**Scale/Scope**: 화면 9개 · FR 24건(FR-701~724) · SC 8건 · User Story 4개(P1 1 · P2 2 · P3 1) ·
부르는 API 12건 · 새 에러코드 0건 · DB 테이블 0개 · 새 의존 0건.
공통 장치는 **폼 실패 착지 하나**이며, 그것도 007 을 고치지 않고 컨트롤러 쪽에 둔다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. 화면 모듈은 007 이 이미 DB 에서 떼어 놓았고 008 은 의존을 더하지
      않는다. 데이터는 전부 백엔드 API 호출로 얻는다.
- [x] **II. 레이어 흐름** — PASS. 화면 모듈에 Repository 가 없다. 흐름은
      `컨트롤러 → 백엔드 호출 통로 → (HTTP)` 한 방향이며 Entity 가 0개다.
- [x] **III. 응답 규격** — PASS(소비 쪽). 화면 모듈은 봉투를 만들지 않고 푼다. 푸는 자리는 007 의
      호출 통로 한 곳이다. 백엔드 호출에 `PUT` 이 없다 — 수정은 전부 PATCH 다.
      **브라우저 폼이 POST 만 쓰는 것은 위반이 아니다** — 그 구간은 API 가 아니라 화면 주소다
      (research 8).
- [x] **IV. 로깅** — PASS. 007 이 콘솔+파일 로깅을 되살렸고 호출 로그가 본문을 남기지 않는다.
      008 은 **폼 객체를 통째로 찍지 않는 것**만 지키면 된다(research 13).
- [ ] **V. 명세 우선** — **착수 전 개정이 남아 있다.** 화면 9건 문서가 전부 골격이고, 프로토타입과
      백엔드 계약이 어긋나는 곳이 2건 있다 → 아래 표.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql` 을 읽어 확인했고 **이 기능은 DB 를
      건드리지 않는다.** 백엔드 코드도 바뀌지 않는다. 덤프 재생성 대상이 아니다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 008 이 만드는 것은 컨트롤러와 템플릿뿐이다. 백엔드로 나가는 통로는 007 의
      것 하나이며 008 이 별도 통로를 만들지 않는다.
- [x] **II** — Repository 0개. 화면이 다루는 값은 백엔드 응답을 받은 **화면 용도의 값**이고,
      응답에 있어도 화면이 쓰지 않는 항목은 타입에 두지 않는다([data-model.md](./data-model.md) §3).
- [x] **III** — 백엔드 호출은 007 의 통로가 강제한다. `PUT` 은 부를 방법 자체가 없다.
      회원 정지가 백엔드에서 **본문 없는 PATCH** 인 것을 확인했고 007 의 통로가 그대로 보낼 수 있다.
- [x] **IV** — 폼 객체를 로그에 넘기지 않는다. 실패 안내는 백엔드 문구를 그대로 쓰고 입력값을
      되비추지 않는다.
- [x] **V** — 착수 전 개정 5건을 [quickstart.md](./quickstart.md) 의 「0. 착수 전」 절차로 넣었다.
      화면 문서 9건의 골격 채우기가 그중 하나다.
- [x] **VI** — DB 무변경. 완료 판정에 `git diff sql/schema-moneylogdb.sql` 이 비어 있는지를 넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `기능명세상세-프론트엔드/phase1-회원/` 화면 9건 | 「화면 구성」·「동작」·「검증·안내」의 **(작성 예정)** 표를 채운다 | spec 의 Clarifications 가 "이 스펙의 Phase 1 에서 phase1-회원 9건만 채운다"로 정했다. 골격인 채로 구현하면 원칙 V 의 "설명 칸 자체 완결"을 어긴다 |
| 2 | `1.8-AdminMemberList.md` 화면 구성 | 검색을 **아이디·닉네임 두 칸**으로 적고 둘이 AND 로 묶인다는 것을 밝힌다 | 프로토타입은 칸 하나인데 백엔드가 두 조건을 AND 로 묶는다. 한 값을 둘에 실으면 "둘 다 맞는" 회원만 나와 사용자 기대와 정반대다(research 5) |
| 3 | `1.8-AdminMemberList.md` 화면 구성 | **「가입일」 열과 「해제」 버튼을 뺀다.** 정지는 화면에서 되돌릴 수 없다고 적는다 | 목록 응답에 가입일이 없고, 정지 해제 API 자체가 없다(research 11·12). 되돌릴 수 없다는 사실은 **누르기 전에** 보여야 한다 |
| 4 | `1.7-MemberProfile.md` · `1.10-AdminMemberEdit.md` 검증·안내 | **빈 칸의 뜻**을 적는다 — 새 비밀번호는 유지, 이메일·폰·소개는 비움 | 표만 보고는 두 칸의 성격이 다르다는 것을 알 수 없다. 잘못 읽으면 "이메일을 지웠는데 그대로 있는" 화면이 된다(research 7) |
| 5 | `1.5-AuthResetPassword.md` 진입 | 아이디·닉네임을 **1.4 에서 넘겨받는다**는 것과, 없이 들어오면 1.4 로 안내한다는 것을 적는다 | FR-712·714 가 요구하는데 화면 문서에는 그 연결이 없다. 적지 않으면 구현자가 사용자에게 다시 묻는 화면을 만든다 |

2번과 3번이 특히 필요하다. 프로토타입을 그대로 옮기면 **동작하지 않는 검색**과 **누를 수 없는
버튼**이 화면에 남는다.

## Project Structure

### Documentation (this feature)

```text
specs/008-frontend-member-auth/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 14건
├── data-model.md        # Phase 1 — 화면이 들고 있는 값
├── quickstart.md        # Phase 1 — 실행과 검증 절차
├── contracts/           # Phase 1
│   ├── form-failure.md      # 폼 실패를 화면에 되돌리는 공통 규약
│   ├── auth-screens.md      # 1.1~1.5 비로그인 화면 다섯
│   ├── member-profile.md    # 1.7 본인 정보
│   └── admin-members.md     # 1.8~1.10 관리자 회원 관리
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 — /speckit-tasks 가 만든다
```

### Source Code (repository root)

007 이 만든 것은 **그대로 두고** 화면 묶음만 더한다. 아래에서 `(007)` 은 손대지 않는 것이다.

```text
app-mod/money-app/src/main/java/com/dbdomino/moneylog/front/
├── MoneyAppApplication.java          (007)
├── client/                           (007) 백엔드 호출 통로 · 봉투 해석 · 파일 응답
├── session/                          (007) 로그인 세션 · 재발급 · 세션 만료
│   └── PasswordResetMark.java        ← 008: 비밀번호 재설정 표식 (1.4 → 1.5)
├── support/                          (007) 토큰 가리기
│   └── PhoneNumbers.java             ← 008: 폰에서 숫자만 남기기
├── web/                              (007) 진입 판정 · 공통 실패 착지 · 페이징 · 모달 판정 · 주소 연결
├── auth/                             ← 008: 1.1~1.5
│   ├── LoginController.java
│   ├── SignupController.java
│   ├── FindIdController.java
│   ├── FindPasswordController.java
│   ├── ResetPasswordController.java
│   └── form/                             화면별 폼
├── member/                           ← 008: 1.7
│   ├── MemberProfileController.java
│   └── form/
└── admin/                            ← 008: 1.8~1.10
    ├── AdminMemberController.java        목록 · 추가 · 수정 · 정지
    └── form/

app-mod/money-app/src/main/resources/templates/
├── layout/                           (007) auth.html · main.html
├── fragments/                        (007) sidebar · modal-shell · confirm-dialog
├── error.html · error/forbidden.html (007)
├── auth/                             ← 008: login · signup · find-id · find-password · reset-password
├── member/                           ← 008: profile
└── admin/                            ← 008: members (+ 추가·수정 모달 조각)

app-mod/money-app/src/test/java/com/dbdomino/moneylog/front/
├── client/ · session/ · web/         (007)
├── auth/                             ← 008
├── member/                           ← 008
└── admin/                            ← 008
```

**Structure Decision**: 컨트롤러를 **화면 묶음별 패키지**로 나눈다(`auth` · `member` · `admin`).
007 의 `web` 은 진입 판정·주소 연결처럼 화면을 가리지 않는 공통 장치의 자리이고, 화면은 자기 묶음에
둔다. 009~012 가 `payment`·`ledger`·`fixed-expense`·`statistics` 를 같은 자리에 더한다.

폼 객체를 화면 묶음 안의 `form` 에 두는 이유는 **화면마다 받는 칸이 다르기 때문**이다. 하나로
합치면 그 화면이 보내지 않는 칸까지 검증 규칙이 따라붙는다([data-model.md](./data-model.md) §3.2).

`PasswordResetMark` 를 007 의 `session` 옆에 두는 이유는 **그것도 세션 속성이기 때문**이다. 세션을
만지는 코드가 두 곳에 흩어지면 무엇이 세션에 사는지 한눈에 세기 어렵다.

## Complexity Tracking

> Constitution Check 에 정당화가 필요한 위반이 있을 때만 채운다.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| 컨트롤러마다 실패 착지를 **선언**한다 (007 의 공통 착지를 화면별로 덮는다) | 폼 실패가 오류 화면으로 가면 사용자가 채운 입력이 전부 사라진다. spec 의 Acceptance(US1-3)가 "입력이 남아 있다"를 요구한다 | **공통 착지 하나로 두기** — 폼인지 목록인지 가릴 기준이 없다. 경로로 가르면 화면이 늘 때마다 007 의 공통 코드를 고쳐야 하고, 그것은 008~012 가 007 을 계속 건드린다는 뜻이다. 헌장 III 이 금지한 것은 **try-catch 로 응답을 제각각 만드는 것**이고, 여기서는 응답 형태도 코드도 바뀌지 않는다 |
| 비밀번호 재설정 값을 **세션에 잠시 둔다** | 1.5 의 저장 API 가 아이디·닉네임을 다시 요구하는데 사용자에게 두 번 묻지 않아야 한다(FR-712) | **주소에 싣기** — 아이디가 주소창·방문 기록·Referer 에 남는다. **flash 로 넘기기** — 한 번의 이동만 살아남아 사용자가 1.5 에서 새로고침하면 처음으로 되돌아간다. 비밀번호를 고르는 중에 일어나기 쉬운 일이다 |
