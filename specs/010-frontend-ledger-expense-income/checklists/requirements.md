# Specification Quality Checklist: 프론트 가계부·지출·소득 화면

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-07
**Feature**: [spec.md](../spec.md)

## Content Quality

- [ ] No implementation details (languages, frameworks, APIs) — **의도적 미충족.** 화면이 부르는 API 이름과 백엔드가 확정한 에러코드는 이 스펙의 계약이라 이름 없이는 요구사항을 검증할 수 없다. 001~007 과 같은 관례다
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
- [ ] No implementation details leak into specification — 위 첫 항목과 같은 사유

## Notes

### 이 스펙에서 특히 확인한 것

- **화면 6개를 한 스펙에 둔 근거**가 적혀 있는가. 모달 넷의 부모가 5.1 이라 나누면 서로를 기다린다 — 본문에 「왜 묶었나」로 명시했다.
- **이름 규칙이 한 응답 안에서 갈리는 것**(FR-905). `FIXED` 행만 현재 이름인데, 화면이 이를 모르면 이름을 다시 조회해 덮어쓰는 구현이 나온다.
- **합계가 필터와 무관한 것**(FR-904·SC-903). 화면이 목록을 다시 더해 맞추려 드는 것이 자연스러운 실수다.
- **지출 등록이 API 두 개로 갈리는 것**(FR-912). 한 폼처럼 보이지만 제출 대상이 다르다.

### 남은 판단

- 고정지출 행은 여기서 읽기만 한다(FR-917). 만들고 고치는 것은 011 이다.
