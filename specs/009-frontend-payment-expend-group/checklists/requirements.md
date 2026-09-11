# Specification Quality Checklist: 프론트 수단·지출유형 화면

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

- **`purpose` 가 요구사항에 들어 있는가**(FR-801). 백엔드에서 필수인데 프론트 명세에 없던 항목이라, 이 스펙을 쓰기 전에 `1.2`·`2.2` 를 먼저 고쳤다.
- **삭제 표시 모델이 화면에 드러나는가**(FR-815·SC-802). 감추면 "지웠는데 왜 보이나"가 되고, 안 감추면 "지웠는데 왜 목록에 있나"가 된다 — 후자를 택하고 상태를 표시하기로 했다.
- **기본 지출유형의 보호**(FR-811). `3105`·`3107` 을 받고 나서 알리는 것보다 화면이 미리 잠그는 편이 낫다.

### 남은 판단

- 이 스펙이 목록+모달 패턴의 기준이 된다. 010~012 가 같은 모양을 따르므로 여기서 정한 것이 뒤에 영향을 준다.
