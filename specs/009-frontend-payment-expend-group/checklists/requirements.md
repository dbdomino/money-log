# Specification Quality Checklist: 프론트 수단·지출유형 화면

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

- **`purpose` 가 요구사항에 들어 있는가**(FR-801). 백엔드에서 필수인데 프론트 명세에 없던 항목이라, 이 스펙을 쓰기 전에 `1.2`·`2.2` 를 먼저 고쳤다.
- **삭제 표시 모델이 화면에 드러나는가**(FR-815·SC-802). 감추면 "지웠는데 왜 보이나"가 되고, 안 감추면 "지웠는데 왜 목록에 있나"가 된다 — 후자를 택하고 상태를 표시하기로 했다.
- **기본 지출유형의 보호**(FR-811). `3105`·`3107` 을 받고 나서 알리는 것보다 화면이 미리 잠그는 편이 낫다.

### 남은 판단

- 이 스펙이 목록+모달 패턴의 기준이 된다. 010~012 가 같은 모양을 따르므로 여기서 정한 것이 뒤에 영향을 준다.

### 구현 세부를 계약으로 옮긴 기록 (2026-09-13)

착수 시점에는 위 두 항목을 **의도적 미충족**으로 두었다. 008 에서 같은 항목을 정리하며
확인한 것이 있다 — **007 의 스펙에는 주소도 API 이름도 코드 값도 한 건 없고**, 첫머리에
"그것을 어떤 수단으로 만드는지는 plan·contracts·quickstart 에 있다"고 못 박아 넘겨 두었다.
009 도 같은 방식으로 맞췄다. 잃은 정보는 없다.

| 옮긴 것 | 어디로 |
|---|---|
| 실패 코드 13건과 붙는 칸·화면 표 | [contracts/list-modal.md](../contracts/list-modal.md) §4 |
| 부르는 API 11건과 010·011 이 부르는 2건 | [contracts/list-modal.md](../contracts/list-modal.md) §7 |
| 백엔드 값과 화면이 보이는 말의 대응 | [contracts/list-modal.md](../contracts/list-modal.md) §8 |
| 화면 8개의 주소 | [contracts/payment-methods.md](../contracts/payment-methods.md) · [expend-groups.md](../contracts/expend-groups.md) |

스펙 본문은 실패를 **뜻으로** 적는다 — "`3005` 로 거절된다"가 아니라 "사용 내역이 있어 바꿀
수 없다는 사유가 표시된다"이다. 요구사항은 그대로 검증할 수 있고, 백엔드가 코드를 재배치해도
스펙을 고칠 일이 없다.
