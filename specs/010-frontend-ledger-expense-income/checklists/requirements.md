# Specification Quality Checklist: 프론트 가계부·지출·소득 화면

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-07
**Updated**: 2026-09-20 — 구현 세부를 계약으로 옮겨 16/16 을 만들었다
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## 구현 세부를 어디로 옮겼나

Phase 1 설계가 끝나 **옮길 자리가 생겼다.** 007 의 스펙에 주소도 기능 이름도 코드도 한 건
없고 첫머리에 "어떤 수단으로 만드는지는 plan·contracts·quickstart 에 있다"고 못 박아 둔 것이
이 저장소의 관례이며, 008·009 가 같은 정리를 마쳤다.

| 옮긴 것 | 스펙에서 | 어디로 |
|---|---|---|
| 화면 6개의 주소 | 「대상 화면」 표의 URL 열 | 각 계약의 메타 표 |
| 부르는 백엔드 기능 15건의 이름 | 「대상 화면」 아래 한 줄 | 각 계약의 메타 표 · 화면 상세 명세의 「호출 API」 |
| 실패 코드 12줄과 붙는 칸 | 「이 기능이 쓰는 에러코드」 표 | [expense-income-modals.md](../contracts/expense-income-modals.md) §6 · [excel.md](../contracts/excel.md) §3 · [ledger-monthly.md](../contracts/ledger-monthly.md) §5 |
| 요청 칸 이름 (`amount`·`monthlyAmount` 등) | FR-912 | [expense-income-modals.md](../contracts/expense-income-modals.md) §2 |
| 응답의 행 종류 값 (`FIXED` 등) | 핵심 개념 · FR-905 | [data-model.md](../data-model.md) §1 |
| 모달 딥링크 표기 (`?m=`) | FR-908 · Edge Cases | [expense-income-modals.md](../contracts/expense-income-modals.md) 메타 표 |

**스펙 본문은 실패를 뜻으로 적는다.** 코드 표가 있던 자리에는 「실패를 다루는 원칙」이
들어갔다 — 어느 칸에 붙이고 어디에 보이는지를 **규칙으로** 적고, 코드와 칸의 대응은 계약이
들고 있다.

**계약에 없던 두 줄을 이때 채웠다.** 스펙이 가리키는 곳에 실제로 그 코드가 있어야 하므로
`3501`(연·월 오류)과 `3502`(행 오류)를 각각 ledger-monthly §5 와 excel §3 에 적었다.

### 옮기지 않은 것

- **`.xlsx`** — 사용자가 파일 선택 창에서 직접 마주하는 제약이라 화면의 요구사항이다.
  주소도 기능 이름도 코드도 아니다.
- **화면번호(5.1 · 3.1~3.5)** — 이 저장소의 화면 상세 명세가 쓰는 식별자이며 구현 수단이
  아니다.

## Notes

### 이 스펙에서 특히 확인한 것

- **화면 6개를 한 스펙에 둔 근거**가 적혀 있는가. 모달 넷의 부모가 5.1 이라 나누면 서로를 기다린다 — 본문에 「왜 묶었나」로 명시했다.
- **이름 규칙이 한 응답 안에서 갈리는 것**(FR-905). 고정지출 행만 현재 이름인데, 화면이 이를 모르면 이름을 다시 조회해 덮어쓰는 구현이 나온다.
- **합계가 필터와 무관한 것**(FR-904·SC-903). 화면이 목록을 다시 더해 맞추려 드는 것이 자연스러운 실수다.
- **지출 등록이 통로 두 개로 갈리는 것**(FR-912). 한 폼처럼 보이지만 제출 대상이 다르다.

### 남은 판단

- 고정지출 행은 여기서 읽기만 한다(FR-917). 만들고 고치는 것은 011 이다.
