# Feature Specification: 지출·소득 수단과 지출유형 관리

**Feature Branch**: `003-backend-payment-expend-group`

**Created**: 2026-09-02

**Status**: Draft

**Input**: `프로젝트설계/기능명세상세-백엔드/phase2-수단-지출유형/` 의 API 13건(2.1~2.13)

## 대상 API

| 기능번호 | API 이름 | Method | URL |
|---|---|---|---|
| 2.1 | `PaymentMethodCreate` | POST | `/api/v1/payment-methods` |
| 2.2 | `PaymentMethodList` | GET | `/api/v1/payment-methods` |
| 2.3 | `PaymentMethodGet` | GET | `/api/v1/payment-methods/{paymentMethodId}` |
| 2.4 | `PaymentMethodUpdate` | PATCH | `/api/v1/payment-methods/{paymentMethodId}` |
| 2.5 | `PaymentMethodDelete` | DELETE | `/api/v1/payment-methods/{paymentMethodId}` |
| 2.6 | `PaymentMethodListActive` | GET | `/api/v1/payment-methods/active/{purpose}` |
| 2.7 | `ExpendGroupCreate` | POST | `/api/v1/expend-groups` |
| 2.8 | `ExpendGroupList` | GET | `/api/v1/expend-groups` |
| 2.9 | `ExpendGroupGet` | GET | `/api/v1/expend-groups/{expendGroupId}` |
| 2.10 | `ExpendGroupIconGet` | GET | `/api/v1/expend-groups/icons/{filename}` |
| 2.11 | `ExpendGroupUpdate` | PATCH | `/api/v1/expend-groups/{expendGroupId}` |
| 2.12 | `ExpendGroupDelete` | DELETE | `/api/v1/expend-groups/{expendGroupId}` |
| 2.13 | `ExpendGroupListActive` | GET | `/api/v1/expend-groups/active` |

전부 **로그인** 권한이다.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 지출·소득 수단을 관리한다 (Priority: P1) 🎯 MVP

카드·계좌를 등록(2.1)하고, 관리 목록에서 보고(2.2·2.3), 이름을 고치고(2.4), 더 쓰지 않는 것을 삭제 표시한다(2.5).

**Why this priority**: 지출·소득·고정지출이 모두 수단을 FK로 참조한다. 수단이 없으면 Phase 3·4의 어떤 등록도 할 수 없다.

**Independent Test**: 수단을 등록·수정·삭제 표시한 뒤, 관리 목록에는 삭제분까지 나오고 사용 중 목록에는 빠지는지 확인한다. 다른 Phase 없이 완결된다.

**Acceptance Scenarios**:

1. **Given** 로그인한 회원이, **When** `type=CARD`·`purpose=EXPENSE`로 수단을 등록하면, **Then** 본인 소유로 저장되고 `deleted=false`다
2. **Given** `type=ACCOUNT`로 등록하면, **When** 저장되면, **Then** `cardExpiry`는 `null`이다
3. **Given** `type`에 `CARD`·`ACCOUNT` 밖의 값을 주면, **When** 등록하면, **Then** `3001`로 거부된다
4. **Given** `purpose`에 `EXPENSE`·`INCOME` 밖의 값을 주면, **When** 등록하면, **Then** `3001`로 거부된다
5. **Given** 수단을 삭제하면, **When** 관리 목록(2.2)을 조회하면, **Then** **그 수단이 여전히 보인다**(삭제는 표시일 뿐 행이 남는다)
6. **Given** 수단을 삭제하면, **When** 사용 중 목록(2.6)을 조회하면, **Then** 그 수단은 빠진다
7. **Given** 남의 수단 ID로, **When** 상세를 조회하면, **Then** `3003`으로 거부된다
8. **Given** 수단 이름을 바꾸면, **When** 그 수단으로 이미 적은 지출을 조회하면, **Then** `paymentMethodName` 스냅샷은 **등록 당시 이름 그대로**다

---

### User Story 2 - 입력 화면이 고를 수 있는 것만 고른다 (Priority: P2)

지출·소득 입력 화면이 쓸 **사용 중 수단 목록**(2.6)과 **사용 중 지출유형 목록**(2.13)을 제공한다.

**Why this priority**: US1·US3의 관리 목록과 조회 축이 다르다. 이 필터가 틀리면 지출 입력 화면에 소득 수단이나 삭제된 유형이 섞여 나온다.

**Independent Test**: 용도·사용 여부·삭제 표시를 다르게 한 수단 4건을 만들고, 사용 중 목록이 정확히 1건만 돌려주는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 지출용·소득용·미사용·삭제된 수단이 각각 있을 때, **When** `purpose=EXPENSE`로 사용 중 목록을 조회하면, **Then** **용도가 맞고 사용 중이며 삭제되지 않은 것만** 나온다
2. **Given** `inUse=false`인 지출유형이 있을 때, **When** 사용 중 유형 목록(2.13)을 조회하면, **Then** 그 유형은 빠진다
3. **Given** `purpose`에 허용되지 않은 값을 주면, **When** 사용 중 목록을 조회하면, **Then** `3001`로 거부된다

---

### User Story 3 - 지출유형을 관리한다 (Priority: P2)

지출유형을 등록(2.7)하고, 목록·상세를 보고(2.8·2.9), 이름·아이콘·사용 여부를 고치고(2.11), 삭제 표시한다(2.12).

**Why this priority**: 지출·고정지출·목표금액·통계가 모두 지출유형을 참조한다. 수단과 대등하게 중요하지만, 가입 시 기본 10종이 이미 생기므로 최소 동작은 US1보다 나중이어도 된다.

**Independent Test**: 유형을 등록·수정·삭제 표시한 뒤 이름 유일성과 삭제 차단 조건이 실제로 막는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 같은 회원이 같은 이름의 유형을 두 번 만들면, **When** 두 번째를 등록하면, **Then** `3101`로 거부된다
2. **Given** 회원이 다르면, **When** 같은 이름의 유형을 각자 만들면, **Then** 둘 다 성공한다
3. **Given** 유형을 삭제 표시한 뒤, **When** **같은 이름**으로 다시 만들면, **Then** 거부된다(삭제된 이름도 점유 상태다)
4. **Given** 그 유형을 쓴 지출이 한 건이라도 있으면, **When** 삭제하면, **Then** `3106`으로 거부된다
5. **Given** `defaultGroup=true`인 기본 유형이면, **When** 삭제하면, **Then** `3107`로 거부된다
6. **Given** 유형을 삭제 표시하면, **When** 그 유형을 참조하던 목표금액을 조회하면, **Then** 목표금액 행과 참조가 **유지된다**
7. **Given** 유형을 `inUse=false`로 바꾸면, **When** 목표금액 단건·변경 API를 부르면, **Then** `3601`로 거부되고, 목록 API는 그 유형을 **제외**하고 돌려준다

---

### User Story 4 - 지출유형 아이콘을 주고받는다 (Priority: P3)

유형마다 30×30 아이콘을 올리고 조회한다(2.10).

**Why this priority**: 화면 표현용이라 기능 동작에는 영향이 없다. 다만 응답 규격의 **유일한 예외**라 따로 다룬다.

**Independent Test**: 아이콘을 올린 유형의 `iconUrl`로 이미지를 받아 바이너리가 오는지, 토큰 없이 부르면 막히는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 아이콘이 있는 유형이면, **When** 목록·상세를 조회하면, **Then** `iconUrl`이 조회 경로 문자열이고 바이너리가 아니다
2. **Given** `iconUrl`을 **Bearer 없이** 부르면, **When** 응답을 받으면, **Then** `401`이다
3. **Given** 아이콘 조회 API가 성공하면, **When** 응답을 보면, **Then** `{ resCode, data }` **래퍼가 아니라** 이미지 바이너리다
4. **Given** 유형을 삭제 표시해도, **When** 그 아이콘 파일을 조회하면, **Then** **파일은 남아 있다**
5. **Given** 유형 이름을 바꿔도, **When** 아이콘을 조회하면, **Then** 파일명은 **생성 시점 이름 그대로**라 기존 파일을 계속 찾는다

### Edge Cases

- 등록 후 수단의 `purpose`를 바꾸려 하면? — 그 수단을 참조하는 지출·소득·고정지출이 **0건일 때만** 허용하고, 1건이라도 있으면 `3005`. 이미 그 카드로 적힌 지출이 "소득 수단으로 낸 지출"이 되어 월별 집계·통계 수단별 요약이 어긋난다
- `purpose`를 omit하면? — 기존 값이 유지되고 참조 검사도 하지 않는다(PATCH omit 규칙)
- 이미 삭제 표시된 수단을 다시 삭제하면? — `3004`
- 삭제 표시된 수단·유형을 수정할 수 있는가? — **가능하다**(이름 정리 등)
- 아이콘 파일명이 `{user_id}_{유형이름}.png`인데 유형 이름에 파일명으로 못 쓰는 문자가 있으면? [NEEDS CLARIFICATION: 유형 이름의 허용 문자 범위와 파일명 변환 규칙이 명세에 없다]
- 아이콘 업로드의 최대 크기·허용 확장자는? [NEEDS CLARIFICATION: 30×30 PNG 외 규격 제한이 명세에 없다]

## Requirements *(mandatory)*

### Functional Requirements

- **FR-201**: 수단은 회원 소유로만 만들어지고 조회·수정·삭제도 **본인 것만** 가능해야 한다
- **FR-202**: 수단의 `type`은 `CARD`·`ACCOUNT` 둘 중 하나여야 한다
- **FR-203**: 수단의 `purpose`는 `EXPENSE`·`INCOME` 둘 중 하나이며, 한 수단은 **한쪽만** 갖는다. 서버가 맥락으로 추론하지 않고 요청 Body가 정한다
- **FR-204**: `type=ACCOUNT`이면 `cardExpiry`는 비어 있어야 한다
- **FR-205**: 등록 후 `purpose` 변경은 그 수단을 참조하는 지출·소득·고정지출(관리·월별)이 **0건일 때만** 허용하고, 그렇지 않으면 `3005`로 거절해야 한다. 이 검사는 애플리케이션이 수행한다
- **FR-206**: 수단 삭제는 **삭제 표시**(행 보존)여야 한다. 물리 삭제하지 않는다
- **FR-207**: 수단 관리 목록은 삭제 표시된 것까지 전부 돌려주고, 사용 중 목록은 **용도 일치·사용 중·미삭제** 세 조건을 모두 걸어야 한다
- **FR-208**: 수단 이름을 바꿔도 과거 지출·소득의 이름 스냅샷은 바뀌지 않아야 한다
- **FR-209**: 지출유형 이름은 같은 회원 안에서 유일해야 하며, **삭제 표시된 행도 포함**해 유일성을 판정한다
- **FR-210**: 지출유형 삭제는 **삭제 표시**여야 하며, 그 유형을 쓴 지출이 있으면 `3106`, 기본 유형(`defaultGroup=true`)이면 `3107`로 거절해야 한다. 이 두 판정은 애플리케이션이 수행한다
- **FR-211**: 지출유형이 삭제 표시돼도 그 유형을 참조하는 목표금액·통계의 참조는 유지되어야 한다
- **FR-212**: 지출유형을 `inUse=false`로 바꾸면 목표금액 단건 조회·변경 API는 `3601`로 거절하고, 목표금액 목록은 그 유형을 제외해야 한다
- **FR-213**: 아이콘은 파일명만 저장하고 조회 경로는 응답을 만들 때 붙여야 한다. 경로를 저장하면 Base URL 변경 시 전 행을 고쳐야 한다
- **FR-214**: 아이콘 파일명은 생성 시점에 정해지고 **유형 이름 변경을 따라가지 않아야** 한다
- **FR-215**: 지출유형을 삭제해도 아이콘 파일은 디스크에 남겨야 한다
- **FR-216**: 아이콘 조회 API는 `{ resCode, data }` 래퍼를 쓰지 않는 **예외**이며 이미지 바이너리를 직접 돌려준다. 인증은 다른 API와 같이 요구한다
- **FR-217**: 목록 응답은 `data.list`(object 배열)로 통일한다. 수단·지출유형 목록은 본인 보유 수가 제한적이라 페이징을 두지 않는다

### Key Entities

- **지출·소득 수단(`tbl_user_payment_method`)**: 카드·계좌. `type`(CARD/ACCOUNT), `purpose`(EXPENSE/INCOME), 사용 여부 `in_use`, 삭제 표시 `deleted`, 카드 유효기간
- **지출유형(`tbl_user_expend_group`)**: 회원별 지출 분류. 이름(회원 안에서 유일), 사용 여부, 기본 유형 여부 `default_group`, 아이콘 파일명, 삭제 표시
- **아이콘 파일**: 회원별 복사본. 이름 규칙 `{user_id}_{유형이름}.{확장자}`. DB에는 파일명만 저장

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-201**: 13개 API가 모두 `{ resCode, data }` 형식으로 응답한다. 단 아이콘 조회(2.10)는 바이너리를 돌려주는 명시된 예외다
- **SC-202**: 용도·사용 여부·삭제 표시를 달리한 수단 4건 중 사용 중 목록이 돌려주는 것은 **정확히 1건**이다
- **SC-203**: 같은 이름의 지출유형을 같은 회원이 두 번 만들면 **100% 거부**되고, 다른 회원이면 **100% 성공**한다
- **SC-204**: 삭제 표시된 수단·유형이 관리 목록에는 **남아 있고** 사용 중 목록에는 **없다**
- **SC-205**: 수단·유형 이름을 바꾼 뒤 과거 지출을 조회하면 스냅샷 이름이 **바뀌지 않는다**
- **SC-206**: 참조가 1건 이상인 수단의 `purpose` 변경 시도가 **100% `3005`** 로 거부된다
- **SC-207**: 남의 자원 ID로 상세·수정·삭제를 시도하면 **100% 거부**된다

## Assumptions

- 저장 구조는 `001-backend-db-schema`에서 만들어졌다. 삭제 표시·유일 제약·인덱스가 이미 DB에 있다
- 인증은 `002-backend-member-auth`가 제공한다. 이 기능의 모든 API는 로그인 필터를 통과한 뒤 실행된다
- 기본 지출유형 10종은 **회원가입(1.2)이 생성**한다. 이 기능은 그 뒤의 관리 API만 다룬다
- 삭제 차단 조건(`3106`·`3107`)은 삭제가 UPDATE라 FK RESTRICT가 대신 막아줄 수 없어 애플리케이션이 판정한다
- 아이콘은 서버 로컬 디스크에 저장한다. 오브젝트 스토리지 전환은 범위 밖이다
- 프론트는 `iconUrl`을 `<img src>`로 직접 붙이지 않고 money-app 프록시 또는 fetch+blob으로 받는다(Bearer 미첨부 시 401)
