# Implementation Plan: 프론트 수단·지출유형 화면

**Branch**: `develop` (기능 브랜치를 따로 두지 않는다) | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-frontend-payment-expend-group/spec.md`

## Summary

007 의 공통 기반과 008 의 폼 규약 위에 **화면 8개**를 얹는다. **목록 하나에 모달 셋**이 두
벌이며(수단 · 지출유형), 백엔드 003 의 API 11건을 부른다.

008 과 성격이 이어지되 두 가지가 새롭다.

| 축 | 008 (회원·인증) | 009 (수단·지출유형) |
|---|---|---|
| 화면 모양 | 폼 화면 다섯 + 목록 하나 + 모달 둘 | **목록+모달 셋이 두 벌** — 010~012 가 따라갈 기준 |
| 다루는 것 | 글자 | 글자와 **파일**(아이콘) |
| 삭제 | 정지(상태 변경) | **삭제 표시** — 행이 남는다 |
| 007 고치기 | 없었다 | **한 자리 있다** — 호출 통로에 수정+파일 조합이 빠져 있다 |

이 네 줄이 계획의 모양을 정한다.

**① 목록+모달 패턴을 여기서 굳힌다.** 008 이 관리자 회원 화면에서 한 번 만들었지만 그것은
회원이라는 한 자원이었다. 009 는 **같은 모양을 두 벌** 만들며, 두 벌이 서로 다르게 생기면
010~012 도 제각각이 된다. 공통 규약을
[contracts/list-modal.md](./contracts/list-modal.md) 에 한 번 적고 두 화면이 그것을 따른다.

**② 삭제는 행을 남긴다.** 사용자가 이 모델을 이해하는 통로는 목록 화면뿐이다. 사용 여부와
삭제 여부를 **다른 열**로 보이고, 삭제된 행에는 되돌릴 수 없는 동작을 열지 않는다. 프로토타입에
그 열도 그 버튼도 없어 둘 다 더한다.

**③ 파일은 브라우저가 백엔드를 부르지 않고 오간다.** 올릴 때는 화면 모듈을 거치고, 보일 때는
007 의 중계를 거친다. 아이콘 주소를 이미지에 그대로 걸면 인증이 붙지 않아 전부 실패한다.

**④ 007 의 구멍 하나를 고친다.** 지출유형 수정이 **수정+파일**인데 007 의 호출 통로에는
등록용만 있다. 009 가 자기 통로를 만들면 봉투를 푸는 자리가 둘이 되므로 007 쪽에 더한다.

기술 결정 13건은 [research.md](./research.md) 에 있다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 4.1.0 · Spring Web MVC · Thymeleaf · Bean Validation ·
`common-mod`(에러코드 Enum 참조용). **이 기능은 의존성을 더하지 않는다.** 007 이 남긴 넷과
008 이 쓴 것으로 충분하며, 파일 업로드는 Spring Web MVC 가 이미 다룬다.

007·008 에서 받아 가는 것은 코드이지 의존이 아니다 — 백엔드 호출 통로, 로그인 세션, 진입
판정, 레이아웃 둘, 모달 껍데기, 확인 다이얼로그, 아이콘 중계, 모달 딥링크 판정, 폼 실패 착지
규약, 빈 칸을 "유지"와 "비움"으로 가르는 장치.

**Storage**: **없다.** 화면 모듈은 데이터베이스를 모른다. `sql/schema-moneylogdb.sql` 은
읽지도 바꾸지도 않는다. **008 과 달리 세션에 더하는 상태도 없다** — 화면 여덟이 전부 한 요청
안에서 끝난다([data-model.md](./data-model.md)).

**Testing**: JUnit 5 · `spring-boot-starter-test` · `spring-boot-starter-webmvc-test` ·
`MockRestServiceServer` · `MockMultipartFile`. 백엔드를 띄우지 않고 응답을 세워 화면을
검증한다. 파일을 다루는 시험은 `MockMultipartFile` 로 올린 값이 요청에 어떻게 실리는지 본다.

**Target Platform**: JVM 서버. 화면 `:8080`, 백엔드 `:8081`.

**Project Type**: Spring Boot 멀티모듈 — 이 기능은 프론트 모듈 하나만 손댄다.

**Performance Goals**: 정하지 않는다. 다만 **성격은 기록한다** — 두 목록 모두 페이징이 없어
본인 것을 전부 받는다(한 사람 기준 수십 개). 지출유형 목록은 아이콘 수만큼 이미지 요청이
따로 나가며, 그 요청은 진입 판정을 타지 않아 백엔드 왕복이 두 배가 되지 않는다(007 이 그렇게
만들었다).

**Constraints**:
- **007 의 공통 코드를 고치지 않는다.** 예외는 **호출 통로 한 자리**이며 그것은 007 의
  구멍이다(research 1). 그 밖의 파일은 손대지 않고 완료 판정에서 diff 로 확인한다.
- 브라우저는 **GET 과 POST 만** 쓴다. 백엔드가 요구하는 메서드로 옮기는 일은 화면 모듈이 한다.
- 화면 모듈은 **에러코드를 새로 정의하지 않는다.**
- **아이콘 주소를 이미지에 직접 걸지 않는다**(FR-814 · SC-807).
- **삭제 표시된 항목을 목록에서 감추지 않는다**(FR-815 · SC-802).
- 기본 지출유형은 **화면이 미리 막는다**(SC-804). 용도 변경은 **미리 막지 못한다**(research 9).
- 폼 실패 착지는 **008 의 규약을 그대로** 쓴다. 새로 정하지 않는다.

**Scale/Scope**: 화면 8개 · FR 18건(FR-801~818) · SC 8건 · User Story 3개(P1 2 · P2 1) ·
부르는 API 11건 · 새 에러코드 0건 · DB 테이블 0개 · 새 의존 0건 · 007 을 고치는 파일 1개.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution v1.1.0 기준 게이트.

**Phase 0 이전 (초기 평가)**

- [x] **I. 모듈 경계** — PASS. 화면 모듈은 007 이 DB 에서 떼어 놓았고 009 는 의존을 더하지
      않는다. 데이터는 전부 백엔드 API 호출로 얻는다. **아이콘도 예외가 아니다** — 파일조차
      백엔드를 거쳐 온다.
- [x] **II. 레이어 흐름** — PASS. 화면 모듈에 Repository 가 없고 Entity 가 0개다. 흐름은
      `컨트롤러 → 백엔드 호출 통로 → (HTTP)` 한 방향이다.
- [x] **III. 응답 규격** — PASS(소비 쪽). 화면 모듈은 봉투를 만들지 않고 푼다. 푸는 자리는
      007 의 통로 한 곳이며, **009 가 그 자리를 늘리지 않는 것**이 research 1 의 결론이다.
      백엔드 호출에 `PUT` 이 없다. 브라우저 폼이 POST 만 쓰는 것은 위반이 아니다 — 그 구간은
      API 가 아니라 화면 주소다.
- [x] **IV. 로깅** — PASS. 007 이 콘솔+파일 로깅을 되살렸고 호출 로그가 본문을 남기지 않는다.
      009 는 **파일을 로그에 넘기지 않는 것**만 더 지키면 된다 — 아이콘 바이트가 로그로 가면
      로그 파일이 부풀고 내용이 그대로 남는다.
- [ ] **V. 명세 우선** — **착수 전 개정이 남아 있다.** 화면 8건 문서가 전부 골격이고,
      프로토타입과 확정 계약이 어긋나는 곳이 2건 있다 → 아래 표.
- [x] **VI. 스키마 덤프** — PASS. `sql/schema-moneylogdb.sql` 을 읽어 확인했고 **이 기능은
      DB 를 건드리지 않는다.** 백엔드 코드도 바뀌지 않는다. 덤프 재생성 대상이 아니다.

**Phase 1 이후 (재평가)**

- [x] **I** — 설계 결과 009 가 만드는 것은 컨트롤러와 템플릿뿐이다. 백엔드로 나가는 통로는
      007 의 것 하나이며, 009 가 더하는 것도 **그 통로 안의 메서드 하나**이지 새 통로가 아니다.
- [x] **II** — Repository 0개. 화면이 다루는 값은 **화면 용도의 값**이고, 응답에 있어도 화면이
      쓰지 않는 항목은 타입에 두지 않는다([data-model.md](./data-model.md) §1). 아이콘은
      주소 전체가 아니라 **파일명만** 들고 있다 — 전체를 들고 있으면 그것이 그대로 이미지에
      걸릴 길이 생긴다.
- [x] **III** — 백엔드 호출은 007 의 통로가 강제한다. 더하는 메서드도 `PATCH` 이며 `PUT` 을
      부를 방법은 여전히 없다. 삭제는 백엔드에서 `DELETE` 이고 007 의 통로에 이미 있다.
- [x] **IV** — 파일을 로그에 넘기지 않는다. 실패 안내는 백엔드 문구를 그대로 쓰고 입력값을
      되비추지 않는다.
- [x] **V** — 착수 전 개정 4건을 [quickstart.md](./quickstart.md) 의 「0. 착수 전」 절차로
      넣었다. 화면 문서 8건의 골격 채우기가 그중 하나다. **스펙 자체의 구현 세부도 함께
      정리한다** — 아래 「스펙 품질 게이트」 참고.
- [x] **VI** — DB 무변경. 완료 판정에 `git diff sql/schema-moneylogdb.sql` 이 비어 있는지를
      넣었다.

### 명세 선행 개정 (착수 전, 원칙 V)

| # | 대상 | 고칠 내용 | 근거 |
|---|---|---|---|
| 1 | `기능명세상세-프론트엔드/phase2-수단-지출유형/` 화면 8건 | 「화면 구성」·「동작」·「검증·안내」의 **(작성 예정)** 표를 채운다 | spec 의 Clarifications 가 "이 스펙의 Phase 1 에서 phase2 8건만 채운다"로 정했다. 골격인 채로 구현하면 원칙 V 의 "설명 칸 자체 완결"을 어긴다 |
| 2 | `2.1-PaymentList.md` · `2.5-ExpendGroupList.md` 화면 구성 | **상태 열과 삭제 버튼을 더한다** | 프로토타입에 둘 다 없다. 삭제 표시 모델을 사용자가 이해하는 통로가 목록뿐인데, 열이 없으면 지운 것이 그냥 사라진 것처럼 보인다(research 5) |
| 3 | `2.6-ExpendGroupCreate.md` · `2.8-ExpendGroupEdit.md` 검증·안내 | **아이콘 제약**(형식 셋·1MB)과 **파일 칸을 비우면 기존 아이콘이 남는다**를 적는다 | 표만 보고는 파일 칸의 빈 값이 "지우라"인지 "그대로"인지 알 수 없다. 잘못 읽으면 이름만 고쳤는데 형식 오류가 나는 화면이 된다(research 3) |
| 4 | `2.4-PaymentEdit.md` · `2.8-ExpendGroupEdit.md` 검증·안내 | **못 바꾸는 칸의 이유**를 적는다 — 용도는 참조가 있을 때, 기본 유형의 이름은 언제나 | 두 제약의 성격이 다르다. 하나는 화면이 미리 막을 수 있고 하나는 못 한다(research 8·9). 적지 않으면 구현자가 둘을 같게 다룬다 |

2번이 특히 필요하다. 프로토타입을 그대로 옮기면 **지울 방법이 없는 목록**이 된다.

### 스펙 품질 게이트 (checklists/requirements.md)

착수 시점 상태는 **14/16**이며 미충족 2건이 전부 "구현 세부가 스펙에 있다"이다. 008 에서
같은 항목을 정리하며 확인한 것이 있다 — **007 의 스펙에는 주소도 API 이름도 코드 값도 한 건
없고**, 첫머리에 "그것을 어떤 수단으로 만드는지는 plan·contracts·quickstart 에 있다"고 못 박아
넘겨 두었다.

009 도 같은 방식으로 맞춘다. **Phase 1 설계가 끝난 지금 옮길 자리가 생겼다.**

| 옮길 것 | 어디로 |
|---|---|
| 실패 코드 12건과 붙는 칸 | [contracts/list-modal.md](./contracts/list-modal.md) §4 |
| 화면 8개의 주소 | [contracts/payment-methods.md](./contracts/payment-methods.md) · [expend-groups.md](./contracts/expend-groups.md) |
| 부르는 API 11건 | 위 두 계약의 메타 표 |

스펙 본문은 실패를 **뜻으로** 적는다. 요구사항은 그대로 검증할 수 있고, 백엔드가 코드를
재배치해도 스펙을 고칠 일이 없다. 이 정리는 `/speckit-tasks` 가 낼 Phase 1 작업에 포함한다.

## Project Structure

### Documentation (this feature)

```text
specs/009-frontend-payment-expend-group/
├── plan.md              # 이 파일
├── research.md          # Phase 0 — 기술 결정 13건
├── data-model.md        # Phase 1 — 화면이 들고 있는 값
├── quickstart.md        # Phase 1 — 실행과 검증 절차
├── contracts/           # Phase 1
│   ├── list-modal.md        # 목록 하나 + 모달 셋 (010~012 가 받아 간다)
│   ├── payment-methods.md   # 2.1~2.4 수단
│   ├── expend-groups.md     # 2.5~2.8 지출유형
│   └── icons.md             # 아이콘을 올리고 보이는 방법
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 — /speckit-tasks 가 만든다
```

### Source Code (repository root)

007·008 이 만든 것은 **그대로 두고** 화면 묶음 둘을 더한다. 아래에서 `(007)`·`(008)` 은
손대지 않는 것이다.

```text
app-mod/money-app/src/main/java/com/dbdomino/moneylog/front/
├── client/                           (007) 백엔드 호출 통로
│   └── BackendApiClient.java         ← 009: patchMultipart 하나를 더한다 (research 1)
├── session/ · web/                   (007) 세션 · 진입 판정 · 모달 판정 · 아이콘 중계
├── support/                          (007)(008) 폼 실패 · 빈 칸 가르기 · 폰
│   └── IconUrls.java                 ← 009: 백엔드 주소에서 파일명만 꺼낸다
├── auth/ · member/ · admin/          (008) 회원·인증 화면
├── payment/                          ← 009: 2.1~2.4
│   ├── PaymentMethodController.java      목록 · 등록 · 수정 · 삭제
│   ├── PaymentMethodView.java
│   └── form/
└── expendgroup/                      ← 009: 2.5~2.8
    ├── ExpendGroupController.java        목록 · 등록 · 수정 · 삭제
    ├── ExpendGroupView.java
    └── form/

app-mod/money-app/src/main/resources/templates/
├── layout/ · fragments/              (007)(008)
├── auth/ · member/ · admin/          (008)
├── payments/                         ← 009: list (+ 등록·상세·수정 모달)
└── expend-groups/                    ← 009: list (+ 등록·상세·수정 모달)

app-mod/money-app/src/test/java/com/dbdomino/moneylog/front/
├── client/ · session/ · web/ · support/   (007)(008)
├── auth/ · member/ · admin/               (008)
├── payment/                          ← 009
└── expendgroup/                       ← 009
```

**Structure Decision**: 008 이 정한 **화면 묶음별 패키지**를 이어 간다(`payment` ·
`expendgroup`). 007 의 `web` 은 화면을 가리지 않는 공통 장치의 자리이고, 화면은 자기 묶음에
둔다. 010~012 가 `ledger` · `fixedexpense` · `statistics` 를 같은 자리에 더한다.

**컨트롤러를 자원당 하나로 둔다.** 008 의 관리자 회원 화면이 목록·추가·수정·정지를 한
컨트롤러에 담아 잘 동작했고, 모달이 부모 목록 위에 뜨는 구조라 **실패 착지가 목록을 다시
그려야** 하기 때문이다. 나누면 그 목록을 그리는 코드가 네 곳에 복사된다.

`IconUrls` 를 `support` 에 두는 이유는 그것이 **화면 묶음에 속하지 않는 변환**이기 때문이다.
012 의 통계 화면도 같은 아이콘을 보인다.

## Complexity Tracking

> Constitution Check 에 정당화가 필요한 위반이 있을 때만 채운다.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| **007 의 호출 통로를 고친다** (008 이 세운 "007 을 고치지 않는다"의 예외) | 지출유형 수정이 **수정+파일**인데 007 에 그 조합이 없다. 007 이 `postMultipart` 를 만들며 "아이콘 업로드(009)가 쓴다"고 적었지만 등록만 보고 수정을 보지 않은 **구멍**이다 | **009 가 자체 호출 코드를 만든다** — 봉투를 푸는 자리가 둘이 되고, 007 이 그 자리를 하나로 묶은 이유(어느 화면 하나가 코드 확인을 빠뜨리면 실패가 성공으로 읽힌다)가 그대로 되살아난다. **백엔드를 `POST` 로 바꾼다** — 009 의 전제가 "백엔드 무변경"이고 헌장 III 이 수정을 `PATCH` 로 못 박았다. **아이콘 전용 업로드 주소를 만든다** — 같은 이유로 배제하며, 이름과 아이콘을 한 번에 고치는 흐름이 두 번의 저장으로 갈린다 |
| 컨트롤러마다 실패 착지를 **선언**한다 (007 의 공통 착지를 화면별로 덮는다) | 008 이 이미 정한 규약을 따르는 것이다. 폼·모달 실패가 오류 화면으로 가면 사용자가 채운 입력이 사라진다 | **008 의 규약을 공통 모듈로 끌어올린다** — 아직 두 기능뿐이라 이르다. 010~012 가 같은 것을 쓰는 것이 확인되면 그때 옮긴다. 지금 옮기면 아직 모르는 화면의 사정까지 공통 코드가 알게 된다 |
