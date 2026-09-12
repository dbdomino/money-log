# Specification Quality Checklist: 프론트 회원·인증 화면

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-07
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

## Notes

### 이 스펙에서 특히 확인한 것

- **1.4 → 1.5 의 값 전달**이 요구사항으로 명시돼 있는가. 백엔드의 비밀번호 변경이 아이디·닉네임을 다시 요구하는데 화면 명세에는 그 흐름이 적혀 있지 않았다 — FR-712·714 로 못박았다.
- **로그인 실패에서 아이디 존재가 드러나지 않는가**(FR-706·SC-702). 백엔드가 두 경우를 한 코드로 묶은 의도를 화면이 풀어 버리기 쉬운 자리다.
- **정지와 로그아웃의 구분**(FR-723). 두 행위가 섞이면 관리자가 잘못된 기대를 갖는다.

### 남은 판단

- 1.6 권한 없음은 007 이 만든다. 이 스펙은 그 화면을 쓰기만 한다.

### 구현 세부를 계약으로 옮긴 기록 (2026-09-12)

착수 시점에는 위 두 항목을 **의도적 미충족**으로 두고 "API 이름과 에러코드가 이 스펙의
계약이라 뺄 수 없다"고 적었다. 확인해 보니 그 근거가 사실과 달랐다 — **007 의 스펙에는 주소도
API 이름도 에러코드도 한 건도 없다.** 007 은 첫머리에 "그것을 어떤 수단으로 만드는지는
plan·contracts·quickstart 에 있다"고 못 박고 그쪽으로 넘겼다.

그래서 008 도 같은 방식으로 맞췄다. 잃은 정보는 없다.

| 옮긴 것 | 어디로 |
|---|---|
| 실패 코드 8건과 붙는 칸·화면 표 | [contracts/form-failure.md](../contracts/form-failure.md) §8 |
| 부르는 API 12건과 007 이 부르는 3건 | [contracts/form-failure.md](../contracts/form-failure.md) §9 |
| 화면 9개의 주소 | [contracts/auth-screens.md](../contracts/auth-screens.md) · [member-profile.md](../contracts/member-profile.md) · [admin-members.md](../contracts/admin-members.md) |

스펙 본문은 실패를 **뜻으로** 적는다 — "`2002` 를 받는다"가 아니라 "아이디가 이미 쓰이고
있다는 사유가 표시된다"이다. 요구사항은 그대로 검증할 수 있고, 백엔드가 코드를 재배치해도
스펙을 고칠 일이 없다.

**009 도 같은 상태다**(미완 2건). 같은 방식으로 정리한다.
