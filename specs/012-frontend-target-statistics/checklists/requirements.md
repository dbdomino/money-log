# Specification Quality Checklist: 프론트 목표금액·통계 화면

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

- **화면 둘을 묶은 근거**. 월별 목표 입력이 통계 화면에 있고 두 화면이 같은 목록 API 를 쓴다.
- **`null` 과 `0` 의 비대칭**(FR-1102·1117·1118·SC-1202). 화면이 빈 칸과 0 을 같게 다루면 "그 달엔 쓰지 않겠다"는 의사 표시가 사라진다. 기본은 0, 월별은 빈 칸이라 방향도 반대다.
- **적용 금액을 화면이 낸다는 것**(FR-1119). 서버가 두 값만 주기로 확정돼 있어 화면이 합쳐야 한다.
- **저장본 불변과 `view=live`**(FR-1123·SC-1207). 조회가 저장을 겸하지 않는다는 계약을 화면이 어기기 쉬운 자리다.
- **연 범위가 005 와 다른 것**(FR-1107). 2000~2100 으로 끊지 않으면 사용자가 고른 값이 서버에서 거절된다.

### 남은 판단

- 차트는 표로 대체할 수 있다고 Assumptions 에 적었다. 값이 읽히면 요구사항을 만족한다.
