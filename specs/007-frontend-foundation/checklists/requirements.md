# Specification Quality Checklist: 프론트 공통 기반 — 화면 모듈 재구성

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-07
**Feature**: [spec.md](../spec.md)

## Content Quality

- [ ] No implementation details (languages, frameworks, APIs) — **의도적 미충족. 아래 참고**
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [ ] Success criteria are technology-agnostic — **부분 미충족. 아래 참고**
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

### 두 항목을 의도적으로 비워 뒀다

이 스펙은 **기존 모듈을 헌장에 맞게 다시 세우는 기능**이라, 무엇을 걷어내고 무엇으로 대체하는지가 곧 요구사항이다. 그 이름을 지우면 요구사항이 검증 불가능해진다.

| 스펙에 남은 것 | 왜 지우지 않았나 |
|---|---|
| `data-mod` · JPA · MyBatis | **제거 대상 그 자체다.** "DB 접근을 없앤다"로만 적으면 FR-601·SC-602 를 확인할 방법이 없다 |
| `HttpSession` · `JSESSIONID` | 사용자가 clarify 에서 **직접 고른 계약**이다. 저장 위치가 바뀌면 인증 흐름 전체와 CORS 필요 여부가 함께 바뀐다 |
| `{ resCode, data }` · `1001` · `1006` | 백엔드가 이미 확정한 **응답 계약**(헌장 원칙 III)이다. 코드마다 화면 모듈의 반응이 달라 이름 없이는 FR-615·616 을 적을 수 없다 |
| `?m=` 딥링크 | `_공통.md` 가 확정한 **화면 주소 규칙**이다 |
| `tokens.css` · `ui.css` · `modal.js` | 화면기획이 이미 만들어 둔 **이식 대상 파일**이다 |

001~006 의 스펙도 같은 이유로 테이블 이름·에러코드·HTTP 메서드를 그대로 적었다. 이 저장소의 관례이며 헌장 원칙 V("설명 칸은 그 칸만 보고 의미가 읽혀야 한다")와도 맞는 방향이다.

### 검증하며 실제로 고친 것

- **SC-603** 에서 빌드 명령(`./gradlew test`)을 뺐다. 성공 기준은 "전 모듈 테스트가 한 번에 통과한다"는 **결과**이지 그것을 확인하는 수단이 아니다.

### 검토했으나 고치지 않은 것

- **US2 의 Independent Test 가 008 을 앞질러 보인다.** 로그인 화면 없이 세션에 토큰을 심어 시험한다고 적었는데, 005·006 이 아직 없는 API 대신 DB 행을 직접 심어 US 를 독립 검증한 것과 같은 처방이라 그대로 뒀다.
- **화면 1.6 을 007 이 만드는 것**이 Phase 구분(1.6 은 Phase 1)과 어긋나 보인다. 권한 차단이 착지할 곳이 없으면 FR-619 를 완성할 수 없고 이 화면만 API 를 부르지 않는다 — 범위 표와 본문에 근거를 적어 두었다.

### 다음 단계

`/speckit-plan` 으로 진행할 수 있다. `/speckit-clarify` 는 건너뛴다 — 열려 있던 결정 셋(토큰 저장 위치 · 명세 채우는 시점 · 스펙 분할)을 착수 전에 사용자가 확정했고 spec.md 의 Clarifications 에 적었다.
