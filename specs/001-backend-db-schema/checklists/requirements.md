# Specification Quality Checklist: 백엔드 API를 지탱하는 DB 테이블 구성

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-31 (기능명세 개정 반영 재작성)
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
- [x] Success criteria are technology-agnostic (no implementation details)
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

- 이 기능은 **DB 저장 구조 확정**이 대상이므로, 저장 단위·항목·제약을 다루는 서술은 구현 세부가 아니라 이 기능의 산출물 자체로 본다. 물리 테이블·컬럼 이름, 타입, 인덱스는 spec에 넣지 않고 `/speckit-plan`에서 확정한다.
- 식별자·명명 규칙(FR-008·FR-009)은 2026-08-31 clarification 결과다 — 기본키는 `idx`, 회원만 `id_key` + `user_id`, 자식은 `id_key` 참조, 테이블 이름은 레거시와 겹치지 않게 신규 명명(회원 `tbl_user`, 세션 `tbl_user_session`). 재작성 직후 잠정 채택했던 "레거시 이름 계승" 방침은 폐기되었다.
- 병행 문서 개정 과제 3건이 Assumptions에 기록되어 있다 — ① `PaymentMethodCreate`/`Update` Body에 `purpose` 추가, ② `MemberSignup` 문서의 기본 지출유형 10종 목록 명시, ③ `_공통.md`의 `tbl_member`·`tbl_member_session` 표기를 `tbl_user`·`tbl_user_session`으로 갱신. 세 건 모두 저장 구조는 이 spec으로 확정되어 있어 planning을 막지 않는다.
- **주의**: 같은 디렉터리의 `plan.md`·`data-model.md`·`research.md`·`quickstart.md`·`contracts/`는 이전 판 spec을 기준으로 만들어진 산출물이라 FR 번호·FR-008 방침이 어긋난다. `/speckit-plan`을 다시 실행해 갱신해야 한다.
