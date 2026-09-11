# Specification Quality Checklist: 프론트 공통 기반 — 화면 모듈 재구성

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

### 2026-09-11 — 기술 이름을 스펙에서 걷어냈다

처음에는 세 항목(구현 세부 2건 · 성공 기준의 기술 중립성 1건)을 **의도적 미충족**으로 두었다. 근거는
"제거 대상과 확정된 계약의 이름을 지우면 요구사항을 검증할 수 없다"였다. 착수 직전에 이 판단을
뒤집었다 — 설계 산출물이 갖춰진 지금은 **그 이름들이 놓일 더 맞는 자리가 이미 있기 때문**이다.

| 스펙에서 뺀 것 | 옮겨 간 곳 |
|---|---|
| 데이터 접근 라이브러리·드라이버 이름 | [plan.md](../plan.md) Technical Context · [quickstart.md](../quickstart.md) §4 |
| 서버 세션·세션 쿠키의 구현 이름 | [contracts/session-auth.md](../contracts/session-auth.md) |
| 응답 봉투의 필드 이름과 에러코드 값 | [contracts/api-client.md](../contracts/api-client.md) §1·§4 |
| 호출 메서드별 규칙표 | [contracts/api-client.md](../contracts/api-client.md) §3 |
| 페이징 파라미터 이름과 실패 코드 | [contracts/api-client.md](../contracts/api-client.md) §5 |
| 모달 딥링크의 질의 문자열 표기 | [contracts/screen-shell.md](../contracts/screen-shell.md) · [contracts/url-map.md](../contracts/url-map.md) |
| 화면 주소와 관리자 주소 규칙 | [contracts/session-auth.md](../contracts/session-auth.md) §2 · [contracts/url-map.md](../contracts/url-map.md) |
| 정적 자원 파일 이름 | [contracts/screen-shell.md](../contracts/screen-shell.md) §2 |

스펙 본문은 같은 것을 **관찰 가능한 행동**으로 적는다. 예를 들어 "인증 만료를 뜻하는 응답을 받으면
재발급을 한 번 시도한다"는 코드 값 없이도 시험할 수 있고, 코드가 바뀌어도 요구사항은 그대로다.

### 검증하며 실제로 고친 것

- **SC-602** 를 "소스에 특정 라이브러리 참조가 0건"에서 **"데이터베이스로 가는 경로가 하나도 남아 있지
  않다 — 백엔드를 내리면 어떤 화면도 데이터를 얻지 못한다"** 로 바꿨다. 참조를 세는 것은 확인 수단이고
  성공 기준은 결과여야 한다. 세는 방법은 quickstart.md 가 그대로 들고 있다.
- **SC-603** 에서 빌드 명령을 뺐다. 성공 기준은 "전 모듈 시험이 한 번에 통과한다"는 결과다.
- 문서 머리에 **기술 세부가 어디 있는지 가리키는 한 줄**을 넣었다. 읽는 사람이 스펙에서 이름을 찾다가
  헤매지 않게 한다.

### 검토했으나 고치지 않은 것

- **US2 의 Independent Test 가 008 을 앞질러 보인다.** 로그인 화면 없이 세션에 토큰을 심어 시험한다고
  적었는데, 005·006 이 아직 없는 API 대신 데이터를 직접 심어 US 를 독립 검증한 것과 같은 처방이라
  그대로 뒀다.
- **화면 1.6 을 007 이 만드는 것**이 Phase 구분과 어긋나 보인다. 권한 차단이 착지할 곳이 없으면
  FR-619 를 완성할 수 없고 이 화면만 API 를 부르지 않는다 — 범위 표와 본문에 근거를 적어 두었다.
- **기능 번호(003 의 2.10 · 004 의 3.11)와 화면 번호(1.1~1.10)는 남겼다.** 기술 이름이 아니라
  이 프로젝트의 기획 문서가 쓰는 식별자다.

### 다음 단계

`/speckit-implement` 로 진행할 수 있다.
