# Feature Specification: 회원 가입·인증·토큰·관리자 회원 관리

**Feature Branch**: `002-backend-member-auth`

**Created**: 2026-09-02

**Status**: Draft

**Input**: `프로젝트설계/기능명세상세-백엔드/phase1-회원/` 의 API 16건(1.1~1.16)과 `_인증-토큰.md`

## 대상 API

| 기능번호 | API 이름 | Method | URL | 권한 |
|---|---|---|---|---|
| 1.1 | `HealthCheck` | GET | `/api/v1/ha` | 없음 |
| 1.2 | `MemberSignup` | POST | `/api/v1/auth/signup` | 없음 |
| 1.3 | `MemberLogin` | POST | `/api/v1/auth/login` | 없음 |
| 1.4 | `MemberTokenValidate` | GET | `/api/v1/auth/validate` | 로그인 |
| 1.5 | `MemberTokenRefresh` | POST | `/api/v1/auth/refresh` | 없음(Refresh Token 필요) |
| 1.6 | `MemberTokenRevoke` | POST | `/api/v1/auth/revoke` | 로그인 |
| 1.7 | `MemberGetMe` | GET | `/api/v1/members/me` | 로그인 |
| 1.8 | `MemberUpdateMe` | PATCH | `/api/v1/members/me` | 로그인 |
| 1.9 | `MemberFindId` | POST | `/api/v1/auth/find-id` | 없음 |
| 1.10 | `MemberFindPassword` | POST | `/api/v1/auth/find-password` | 없음 |
| 1.11 | `MemberResetPassword` | POST | `/api/v1/auth/reset-password` | 없음 |
| 1.12 | `AdminMemberCreate` | POST | `/api/v1/admin/members` | 관리자 |
| 1.13 | `AdminMemberList` | GET | `/api/v1/admin/members` | 관리자 |
| 1.14 | `AdminMemberGet` | GET | `/api/v1/admin/members/{memberId}` | 관리자 |
| 1.15 | `AdminMemberUpdate` | PATCH | `/api/v1/admin/members/{memberId}` | 관리자 |
| 1.16 | `AdminMemberDeactivate` | PATCH | `/api/v1/admin/members/{memberId}/deactivate` | 관리자 |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 인증 기반이 선다 (Priority: P1) 🎯 MVP

로그인하지 않아도 부를 수 있는 헬스체크(1.1)로 `{ resCode, data }` 응답 형식이 실제로 서는지 확인하고, 그 위에 **토큰 발급·검증·갱신·폐기**(1.3~1.6)를 올린다. 이 네 개가 서면 나머지 모든 API의 "권한 = 로그인"이 의미를 갖는다.

**Why this priority**: 이 프로젝트의 API 57건 중 53건이 로그인을 요구한다. 인증이 서지 않으면 그 53건은 하나도 검증할 수 없다. 반대로 이것만 서면 다른 Phase가 각자 진행할 수 있다.

**Independent Test**: 회원 1명을 DB에 직접 넣고 로그인해 토큰을 받은 뒤, 검증 API가 통과시키고, 갱신 API가 새 토큰 쌍을 주며, 폐기 후에는 같은 토큰이 거부되는지 확인한다. 다른 Phase의 API 없이 완결된다.

**Acceptance Scenarios**:

1. **Given** 활성 회원이 있을 때, **When** 올바른 아이디·비밀번호로 로그인하면, **Then** Access Token(1일)·Refresh Token(7일)이 발급되고 세션 1건이 저장된다
2. **Given** 이미 로그인한 세션이 있을 때, **When** 같은 회원이 다시 로그인하면, **Then** 기존 세션의 두 토큰 해시가 비워지고 `revoked`가 서며, 활성 세션은 새 것 1건뿐이다
3. **Given** 유효한 Access Token을 들고, **When** 검증 API를 부르면, **Then** `valid=true`와 남은 만료 시간이 온다
4. **Given** 다른 기기에서 재로그인해 세션이 폐기된 토큰으로, **When** 아무 API나 부르면, **Then** `1006`으로 거부된다
5. **Given** Access Token이 만료됐고 Refresh Token이 살아 있을 때, **When** 갱신 API를 부르면, **Then** **새 Access·Refresh 쌍**이 발급되고 같은 세션 행의 해시·만료 시각이 갱신된다(Rotation)
6. **Given** 로그아웃한 뒤, **When** 그 Refresh Token으로 갱신을 시도하면, **Then** `1005`로 거부된다
7. **Given** 비활성(`active=false`) 계정으로, **When** 로그인하면, **Then** `1004`로 거부된다

---

### User Story 2 - 회원가입과 본인 정보 관리 (Priority: P2)

가입(1.2)하고, 본인 정보를 보고(1.7) 고친다(1.8).

**Why this priority**: 인증이 선 뒤 사람이 실제로 계정을 만들 수 있어야 서비스가 성립한다. 다만 US1의 토큰 흐름 없이는 1.7·1.8을 부를 수 없어 뒤에 둔다.

**Independent Test**: 가입 API로 계정을 만들고, 그 계정으로 로그인해 본인 정보를 조회·수정한 뒤 값이 바뀌었는지 다시 조회해 확인한다.

**Acceptance Scenarios**:

1. **Given** 쓰이지 않은 아이디로, **When** 가입하면, **Then** 권한 `3`(일반)으로 저장되고 비밀번호는 **bcrypt 해시로만** 남는다
2. **Given** 가입이 성공하면, **When** 그 회원의 지출유형을 조회하면, **Then** **기본 10종**(식비·교통·주거·통신·쇼핑·장보기·의료·교육·문화·기타)이 `defaultGroup=true`로 있고 각각 30×30 아이콘 파일명이 붙어 있다
3. **Given** 이미 쓰이는 아이디로, **When** 가입하면, **Then** `2002`로 거부된다
4. **Given** 이메일을 넣지 않고, **When** 두 명이 가입하면, **Then** 둘 다 성공한다(이메일은 선택 항목)
5. **Given** 비밀번호와 확인이 다를 때, **When** 가입하면, **Then** `2005`로 거부된다
6. **Given** 로그인한 회원이, **When** 본인 정보를 조회하면, **Then** 비밀번호는 응답에 **포함되지 않는다**
7. **Given** 본인 정보 수정에서 일부 필드를 omit하면, **When** 저장하면, **Then** omit한 필드는 그대로 유지된다(PATCH omit 규칙)

---

### User Story 3 - 아이디·비밀번호 찾기 (Priority: P3)

로그인하지 못하는 사람이 아이디를 찾고(1.9), 비밀번호를 재설정한다(1.10·1.11).

**Why this priority**: 없어도 서비스는 돌지만, 없으면 비밀번호를 잊은 사용자가 영영 들어오지 못한다. 인증·가입이 선 뒤에 붙인다.

**Independent Test**: 가입한 계정의 정보로 아이디를 찾고, 비밀번호 찾기를 거쳐 재설정한 뒤 새 비밀번호로 로그인되는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 가입할 때 이메일을 넣은 회원이, **When** 그 이메일로 아이디를 찾으면, **Then** 해당 아이디가 반환된다
2. **Given** 비밀번호를 재설정한 뒤, **When** 옛 비밀번호로 로그인하면, **Then** `1003`으로 거부된다
3. **Given** 비밀번호를 재설정하면, **When** 그 회원의 활성 세션을 확인하면, **Then** 기존 세션이 폐기되어 있다

---

### User Story 4 - 관리자 회원 관리 (Priority: P4)

관리자가 회원을 추가하고(1.12) 목록·상세를 보고(1.13·1.14) 수정하거나(1.15) 정지한다(1.16).

**Why this priority**: 운영자 기능이라 일반 사용자 흐름과 독립적이다. 권한 `1` 검사가 실제로 막는지가 이 스토리의 핵심이다.

**Independent Test**: 일반 회원 토큰으로 관리자 API를 불러 `1002`로 막히는지 확인하고, 관리자 토큰으로는 통과하는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 권한 `3`인 회원 토큰으로, **When** 관리자 API를 부르면, **Then** `1002`로 거부된다
2. **Given** 관리자가, **When** 회원 목록을 조회하면, **Then** `data.list`가 object 배열이고 `offset`·`limit`·`totalCount`가 함께 온다
3. **Given** 관리자가 회원을 정지하면, **When** 그 회원이 로그인을 시도하면, **Then** `1004`로 거부되고, **회원 행과 그 회원의 가계부 데이터는 남아 있다**
4. **Given** 관리자가 회원을 정지하면, **When** 그 회원의 기존 세션을 확인하면, **Then** 폐기되어 있다
5. **Given** 관리자가 회원을 추가하면, **When** 그 행의 감사 컬럼을 보면, **Then** `created_by`에 **관리자의 `id_key`** 가 들어 있다(본인 가입은 `null`)

### Edge Cases

- 로그인 요청이 **동시에 두 건** 들어오면? — 활성 세션 부분 유니크 인덱스가 DB에서 1건을 강제한다. 애플리케이션 검사만으로는 창을 닫을 수 없다
- 토큰의 서명은 유효한데 DB 세션이 없으면? — `1006`. JWT 서명·만료와 **DB 대조를 모두** 통과해야 한다
- Access Token 만료 시각이 JWT와 DB에서 다르면? — 둘 다 검사하고 어느 하나라도 지났으면 `1001`
- 아이디 찾기에서 **일치하는 회원이 없으면**? — 계정 존재 여부가 새지 않도록 응답을 통일할지 결정이 필요하다 [NEEDS CLARIFICATION: 존재하지 않는 아이디·이메일에 대해 성공 형태로 응답할지, 명시적 오류코드를 줄지]
- 비밀번호 재설정 링크·인증코드의 유효 시간은? [NEEDS CLARIFICATION: 1.10~1.11 사이의 본인 확인 수단과 만료 시간이 명세에 없다]

## Requirements *(mandatory)*

### Functional Requirements

- **FR-101**: 시스템은 인증 없이 부를 수 있는 헬스체크를 제공하고 `{ resCode: 200, data }` 형식으로 응답해야 한다
- **FR-102**: 시스템은 아이디·비밀번호·닉네임으로 회원가입을 받아야 하며, 아이디는 4~20자 영문·숫자·`_`, 닉네임은 2~20자여야 한다
- **FR-103**: 비밀번호는 **8자 이상**이고 영문 대문자·소문자·숫자·특수문자 중 **3종류 이상**을 포함해야 한다
- **FR-104**: 비밀번호는 **bcrypt 해시로만** 저장해야 하며 평문을 저장하거나 응답에 실어서는 안 된다
- **FR-105**: 가입 시 권한은 일반(`3`)으로 고정한다. 관리자(`1`)는 가입으로 만들 수 없다
- **FR-106**: 가입 성공 시 **기본 지출유형 10종**을 `defaultGroup=true`로 생성하고 회원별 아이콘 파일을 복사해야 한다
- **FR-107**: 이메일·전화번호·자기소개는 선택 항목이며, 전화번호는 하이픈 없이 숫자만 저장한다
- **FR-108**: 아이디는 전역 유일해야 한다. 이메일은 **값이 있을 때만** 유일하다
- **FR-109**: 로그인 성공 시 Access Token(기본 1일)과 Refresh Token(기본 7일)을 발급하고 세션 1건을 저장해야 한다
- **FR-110**: 회원당 폐기되지 않은 세션은 **동시에 1건**이어야 한다. 새 로그인은 기존 세션을 폐기한 뒤 만든다
- **FR-111**: 세션 폐기는 행 삭제가 아니라 **두 토큰 해시를 비우고 `revoked`를 세우는 것**이다. 폐기된 세션 행은 남는다
- **FR-112**: 로그인 필요 API는 진입 전에 JWT 서명·만료와 **DB 세션 해시·만료를 모두** 검증해야 한다. 이 검증은 공통 필터가 수행하며 Controller마다 중복 작성하지 않는다
- **FR-113**: 토큰 갱신 성공 시 Access·Refresh를 **모두 새로 발급**하고 같은 세션 행의 해시·만료 시각을 갱신해야 한다(Rotation)
- **FR-114**: 로그아웃은 세션의 두 해시를 비워 Access·Refresh 모두 사용 불가로 만들어야 한다
- **FR-115**: 비활성(`active=false`) 회원은 로그인·토큰 검증·갱신에서 모두 `1004`로 거부해야 한다
- **FR-116**: 회원은 **본인 데이터만** 조회·수정할 수 있어야 한다. 대상은 토큰의 회원으로 정해지며 요청이 지정할 수 없다
- **FR-117**: 관리자 전용 API는 권한 `1`을 확인해야 하며, 일반 회원은 `1002`로 거부한다
- **FR-118**: 관리자 회원 정지는 `active=false`로 표시하는 것이며 회원 행과 그 회원의 데이터를 지우지 않는다
- **FR-119**: 관리자 정지·비밀번호 변경은 그 회원의 활성 세션을 폐기해야 한다
- **FR-120**: 목록 응답은 `data.list`(object 배열)로 통일하고 페이징은 `offset`·`limit`·`totalCount`를 쓴다
- **FR-121**: 관리자가 다른 회원을 추가·수정하면 감사 컬럼 `created_by`/`updated_by`에 **관리자의 `id_key`** 를 남겨야 한다. 본인 가입·본인 수정은 `null`이다
- **FR-122**: 아이디 찾기·비밀번호 찾기·비밀번호 재설정은 비로그인 상태에서 호출할 수 있어야 한다

### Key Entities

- **회원(`tbl_user`)**: 로그인 계정과 프로필. 기본키 `id_key`, 로그인 아이디 `user_id`, bcrypt 해시 `pw`, 권한 `role`(1 관리자 / 3 일반), 활성 여부 `active`
- **회원 세션(`tbl_user_session`)**: 로그인 1회의 토큰 상태. 세션 UUID, Access·Refresh 해시와 만료 시각, 폐기 여부. 회원당 폐기되지 않은 것은 1건
- **로그인 이력(`tbl_user_login_history`)**: 로그인 시도 기록. 무기한 누적되므로 조회는 페이징으로 끊는다

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-101**: 16개 API가 모두 `{ resCode, data }` 형식으로 응답하며, 실패 시 `resCode`가 정수 4자리다
- **SC-102**: 로그인 → 검증 → 갱신 → 로그아웃 순서를 한 번 도는 통합 테스트가 통과한다
- **SC-103**: 같은 회원으로 두 번 로그인한 뒤 활성 세션을 세면 **정확히 1건**이다
- **SC-104**: 폐기된 토큰으로 로그인 필요 API를 부르면 **100% `1006`** 으로 거부된다
- **SC-105**: 가입 직후 그 회원의 지출유형이 **정확히 10건**이고 전부 `defaultGroup=true`다
- **SC-106**: 일반 회원 토큰으로 관리자 API 5건을 부르면 **전부 `1002`** 로 거부된다
- **SC-107**: 어떤 응답에도 비밀번호 해시가 포함되지 않는다

## Assumptions

- 저장 구조는 `001-backend-db-schema`에서 이미 만들어졌다. 이 기능은 그 위에 API를 얹는다
- API가 주고받는 `memberId`는 `tbl_user.user_id` 값이다. 인증 필터가 토큰의 `sub`를 `id_key`로 환산해 `SecurityContext`에 함께 싣는 것을 전제한다
- JWT 알고리즘은 HS256, 클레임은 `sub`(memberId)·`role`·`sid`(sessionId)·`exp`·`iat`다
- Refresh Token은 JWT가 아닌 불투명 랜덤 문자열이다
- 비밀번호 해싱은 Spring Security `BCryptPasswordEncoder`(또는 동등 라이브러리)를 쓴다
- 이메일 발송·SMS 인증 같은 외부 채널 연동은 이 기능의 범위 밖이다. 아이디·비밀번호 찾기의 본인 확인 수단은 clarification이 필요하다
- 로그인 이력은 무기한 보존하며 정리 배치를 두지 않는다
