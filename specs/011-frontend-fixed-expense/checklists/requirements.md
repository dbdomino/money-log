# Specification Quality Checklist: 프론트 고정지출 화면

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-07
**Updated**: 2026-09-21 — 구현 세부를 계약으로 옮겨 16/16 을 만들었다
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
이 저장소의 관례이며, 008~010 과 012 가 같은 정리를 마쳤다.

| 옮긴 것 | 스펙에서 | 어디로 |
|---|---|---|
| 화면 5개의 주소 | 「대상 화면」 표의 URL 열 | 각 계약의 메타 표 |
| 부르는 백엔드 기능 10건의 이름 | 「대상 화면」 아래 한 줄 | 각 계약의 메타 표 · 화면 상세 명세의 「호출 API」 |
| 실패 코드 7줄과 붙는 칸 | 「이 기능이 쓰는 에러코드」 표 | [fixed-expense-list.md](../contracts/fixed-expense-list.md) §5 · [monthly.md](../contracts/monthly.md) §6 |
| 요청 칸 이름 (`startYear` 등) | 핵심 개념 · FR-1002 | [fixed-expense-list.md](../contracts/fixed-expense-list.md) §2 |
| 사용 중 목록의 경로 (`/active/EXPENSE`) | FR-1003 · FR-1014 | [fixed-expense-list.md](../contracts/fixed-expense-list.md) §2 |
| 모달 딥링크 표기 (`?m=monthly`) | Clarifications · FR-1012 | [monthly.md](../contracts/monthly.md) 메타 표 |

**스펙 본문은 실패를 뜻으로 적는다.** 코드 표가 있던 자리에는 「실패를 다루는 원칙」이
들어갔다 — 어느 칸에 붙이고 어디에 보이는지를 **규칙으로** 적고, 코드와 칸의 대응은 계약이
들고 있다.

**`3401` 이 두 가지를 겸한다는 사실은 원칙으로 남겼다.** 「한 코드가 여러 칸을 겸하는
실패는 폼 상단에 둔다」로 적었고, 그것이 이 기능에서 가장 실수하기 쉬운 자리다.

### 옮기지 않은 것

- **화면번호(4.2~4.6)** — 이 저장소의 화면 상세 명세가 쓰는 식별자이며 구현 수단이 아니다.
- **「4.1 은 없다」** — 번호 체계의 사실이라 스펙에 남는 것이 맞다.

## Notes

### 이 스펙에서 특히 확인한 것

- **적용 기간이 연·월 정수 네 칸인 것**(FR-1002). 백엔드 설계 명세가 문자열 `YYYY-MM` 으로 잘못 적고 있어 이 스펙을 쓰기 전에 4.1~4.4 를 먼저 고쳤다.
- **변경 범위 안내가 요구사항인가**(FR-1009·SC-1002). 이 기능에서 가장 오해하기 쉬운 지점이라 US2 를 통째로 여기에 할애했다.
- **재작성의 안전장치**(FR-1020·1021). 체크박스 기본 꺼짐 + 확인 두 겹이 없으면 사용자가 직접 고친 값을 실수로 잃는다.
- **월별 내역을 열 때 특정 고정지출을 가리키지 않는 것**(FR-1012). 화면기획 01 과 프론트 README 가 갈려 있어 Clarifications 에 확정했다.

### 남은 판단

- 가계부(010)의 고정지출 행은 읽기만 한다. 만들고 고치는 곳이 이 기능이다.
