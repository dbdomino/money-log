---
name: tc-example-generator
description: specs/ 디렉토리의 기능 번호를 입력받아 Speckit 산출물을 취합하고, 시험항목·시험목적·시험절차·시험내역(테스트 예시)을 자체시험 결과서 xlsx로 생성한다.
---

# TC 예시 생성 스킬

## 목적

이 스킬은 저장소 루트 **`specs/`** 디렉토리의 특정 기능 번호를 기준으로 Speckit 산출물을 취합한 뒤, 테스트 가능한 요소를 추출하여 자체시험 결과서 xlsx 파일을 생성한다.

최종 산출물은 반드시 엑셀 파일(`.xlsx`)이어야 하며, 스킬에 포함된 자체시험 결과서 양식을 기준으로 작성한다.

## 사용해야 하는 상황

사용자가 다음과 같이 요청하면 이 스킬을 사용한다.

- `specs` 번호(또는 `/specs/NNN-*`) 기준으로 TC 예시를 만들어달라고 요청하는 경우 (복수 번호·「만들거나 수정한 API」 범위 지정 포함)
- 특정 기능 번호의 산출물에서 테스트 항목을 뽑아달라고 요청하는 경우
- 자체시험 결과서 양식으로 xlsx 파일을 만들어달라고 요청하는 경우
- 기능 정의서, 요구사항, API 명세, DB 변경사항, 작업 목록을 기준으로 시험항목을 작성해야 하는 경우

예시 요청:

```text
/specs 009 기준으로 TC 예시 만들어줘
```

```text
spec 번호 006 산출물 취합해서 자체시험 결과서 xlsx로 만들어줘
```

```text
specs/001-nnn 기능으로 시험항목, 시험목적, 시험절차 작성해줘
```

```text
001, 002, 006 기준으로 만들거나 수정한 API만 TC xlsx로 만들어줘
```

## 사용하지 말아야 하는 상황

다음 요청에는 이 스킬을 사용하지 않는다.

- 단순 테스트 케이스 작성 방법 설명
- xlsx가 아닌 일반 표 형태 응답만 원하는 경우
- `specs/` 산출물과 무관한 테스트 전략 수립
- 이미 완성된 자체시험 결과서의 단순 맞춤법 수정

## 입력 기준

사용자는 보통 **기능 번호**만 제공한다. Agent는 해당 번호를 기준으로 `specs/` 안의 관련 디렉터리·파일을 찾아야 한다.

### 디렉터리 명명 규칙 (저장소 실측)

기능 폴더는 다음 패턴을 따른다.

```text
specs/{번호}-{기능-slug}/
```

예시:

| 번호 입력 | 매칭 디렉터리 |
|-----------|---------------|
| `6`, `006` | `specs/006-sns-disconnect-receive/` |
| `9`, `009` | `specs/009-sns-profile-aud-verify/` |
| `10`, `010` | `specs/010-google-risc-receive/` |

### 경로 탐색 우선순위

1. `specs/{번호}-*` — zero-padding·비패딩 모두 시도 (`6` → `006-*`)
2. `specs/*{번호}*` — slug에 번호가 포함된 경우
3. 사용자가 명시한 전체 경로 (예: `specs/008-google-login-signup`)

번호만으로 **여러 디렉터리가 매칭**되면 가장 구체적으로 일치하는 하나를 선택하고, 나머지는 「추가 확인 필요」에 보고한다.

사용자가 **복수 spec 번호**(예: `001, 002, 006`)를 주면 각 번호 디렉터리를 모두 읽고, 요청에 「만들거나 수정한 API」가 있으면 `contracts/`·`tasks.md` 범위에 해당하는 API만 TC로 추출한다. 산출물은 단일 xlsx에 합칠 수 있다(파일명은 아래 [결과 파일명](#결과-파일명) 참고).

## `specs/` 산출물 카탈로그

아래는 저장소 `specs/` 하위 **실제 존재하는** 산출물 종류다. Speckit 워크플로·`CLAUDE.md` 읽기 순서와 정합한다.

### 기능 루트 문서

| 파일 | 역할 | TC 추출 시 활용 |
|------|------|----------------|
| **`spec.md`** | 기능 명세 SSOT — User Story, Acceptance Scenarios, FR-*, SC-*, Edge Cases, Schema reference, Out of scope | **1순위**. 시나리오·기대 결과·검증 가능 요구사항의 주 근거 |
| **`plan.md`** | 구현 계획 — Summary, 기술 맥락, 헌법 점검, 검증·스모크 시나리오, 소스 구조 | API·도메인 경계, 검증 표, plan §검증 시나리오 |
| **`research.md`** | Phase 0 결정 기록 — R1, R2… 표, HTTP 상태·경로·알고리즘 확정 | 실패 코드·엔드포인트·제공자별 동작 차이 |
| **`data-model.md`** | 엔티티·컬럼·상태 전이·DDL 힌트 | DB 검증 TC — 테이블·컬럼·상태값 |
| **`tasks.md`** | 구현 태스크(T001…), Phase·User Story, FR↔Task 매핑 | 누락 TC 보완, Phase별 시험 구간 힌트(구현 완료 범위) |

### `contracts/` — HTTP·API 계약

| 파일 패턴 | 역할 | TC 추출 시 활용 |
|-----------|------|----------------|
| **`contracts/http-*.md`** | 기능별 HTTP SSOT (예: `http-sns-aud-verify.md`, `http-risc-receive.md`, `http-sns-google.md`) | **API TC 핵심** — Method, path, 요청 필드, resultcode/HTTP, 단계별 성공·실패 표 |
| **`contracts/http-outline.md`** | 다수 기능 공통 HTTP 개요 | 엔드포인트·상태코드 요약 |
| **`contracts/openapi.yaml`** | OpenAPI 부분 계약 (현재 `001-m365-email-send` 등) | path·operationId·응답 스키마 |

계약 문서가 **선행 기능**을 참조하면(예: 009 → 008 `contracts/http-sns-google.md`) 해당 파일도 읽되, **이번 기능 범위**에 해당하는 절만 TC로 변환한다.

### `checklists/` — 품질 체크리스트

| 파일 | 역할 | TC 추출 시 활용 |
|------|------|----------------|
| **`checklists/requirements.md`** | spec 품질·완전성 체크리스트 | 직접 TC 소스는 아님. Notes에 있는 HTTP·FR 보완·범위 경계 참고 |

### `references/` — 보조·스모크 자료

| 파일 패턴 | 역할 | TC 추출 시 활용 |
|-----------|------|----------------|
| **`references/smoke-*.md`** | 배포 후 수동 스모크 표 (예: `smoke-phase7.md`) | **기성 TC 후보** — 시나리오 번호·resultcode·수동 확인 항목을 시험절차로 변환 |
| **`references/schema-notes.md`** 등 | 스키마·연동 보조 메모 | DB·필드 해석 보조 |

### `quickstart.md` — 로컬 검증 가이드

| 파일 | 역할 | TC 추출 시 활용 |
|------|------|----------------|
| **`quickstart.md`** | 로컬 실행·환경변수·API 호출 순서 (현재 `001-m365-email-send` 등) | **통합 시험절차** — 호출 순서·샘플 요청·기대 resultcode |

### 기능별 산출물 보유 현황 (참고)

모든 기능이 전체 세트를 갖추지는 않는다. 없는 파일은 건너뛰고, 있는 파일만 근거로 사용한다.

| 기능 디렉터리 | spec | plan | research | data-model | tasks | contracts | quickstart | checklists | references |
|---------------|:----:|:----:|:--------:|:----------:|:-----:|:---------:|:----------:|:----------:|:----------:|
| 001-m365-email-send | ✓ | ✓ | ✓ | ✓ | ✓ | ✓+openapi | ✓ | ✓ | ✓ |
| 002-remove-mailercheck | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| 003-google-sns-connect | ✓ | | | | | | | ✓ | |
| 004-sns-kakao-naver-apple | ✓ | ✓ | | | ✓ | | | | |
| 006-sns-disconnect-receive | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| 007-oversea-login-block | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | ✓ |
| 008-google-login-signup | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| 009-sns-profile-aud-verify | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| 010-google-risc-receive | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |

### DB 스키마 권위 (산출물 외)

테이블·컬럼명은 산출물과 함께 **`.specify/memory/dump-sksiap-sksdb.sql`** 을 참고한다. `spec.md` Schema reference·`data-model.md`와 **불일치 시** spec·data-model·contracts를 우선하고, dump는 보조 확인용으로만 쓴다.

## 읽기 순서 (TC 생성용)

1. **`spec.md`** — User Story·Acceptance Scenarios·FR·Edge Cases
2. **`contracts/`** — HTTP path·method·resultcode·요청/응답 (있으면)
3. **`data-model.md`** — DB 검증 대상
4. **`plan.md`** — 검증 시나리오·스모크 표
5. **`research.md`** — 실패 경로·제공자별 예외
6. **`quickstart.md`**·**`references/smoke-*.md`** — 통합·수동 절차
7. **`tasks.md`** — Phase·FR 매핑으로 누락 보완
8. **`checklists/requirements.md`** — Notes만 선택 참고

찾은 산출물의 내용만 근거로 사용한다. 문서에 없는 API 경로, 파라미터, 응답코드, DB 테이블명은 임의로 만들지 않는다.

## 산출물별 TC 변환 힌트

| 산출물 섹션 | 변환 대상 |
|-------------|-----------|
| `spec.md` — **Acceptance Scenarios** (Given/When/Then) | 정상·실패 TC 1행씩 |
| `spec.md` — **FR-*** | 요구사항 단위 검증 목적 |
| `spec.md` — **Edge Cases** | 경계·예외 TC |
| `spec.md` — **Independent Test** | 시험절차 개요 |
| `contracts/*.md` — 단계·조건·성공·실패 표 | API 호출 순서·기대 resultcode/HTTP |
| `data-model.md` — 상태 전이 | DB 전·후 상태 확인 절차 |
| `plan.md` — 검증·스모크 시나리오 | 통합 TC |
| `references/smoke-*.md` — 시나리오 표 | 수동 확인 TC (구현 대응 열 참고) |
| `quickstart.md` — API 흐름 | E2E 시험절차 |

**ResCode·HTTP**: Internal API는 `resultcode`(예: 744, 803), External·웹훅은 plain HTTP(204, 401, 200 등) — 산출물에 명시된 쪽만 사용한다.

## 테스트 요소 추출 기준

다음 항목은 테스트 가능한 요소로 취급한다.

- 신규 API 추가 (`ExternalAPI` `/v1/account/...`, `InternalAPI` `/v1/api/account/...`)
- 기존 API 요청/응답 규격 변경
- 필수값, 선택값, 형식, 길이 제한 변경
- 정상 처리 시나리오
- 실패 처리 시나리오
- 권한/인증/토큰·aud·HMAC·JWT 검증
- 외부 솔루션 또는 외부 API·웹훅 연동
- DB insert/update/delete/select 결과 확인
- 상태값 변경·행 삭제
- 중복 요청 처리
- 예외 응답 코드 및 메시지 (`ResCode`/`ResMsg`, HTTP status)
- redirect, callback, webhook 수신
- Redis `flowToken`·OTP 등 부가 저장소
- 로그/이력 저장 (`@StatLog`, `tbl_user_action_log` 등 — 산출물에 명시된 경우만)

테스트 항목은 가능한 경우 정상/실패/경계/DB검증 케이스로 분리한다.

## 제외 기준

다음 내용은 별도 근거가 없으면 시험항목으로 만들지 않는다.

- 단순 코드 리팩터링·패키지 이동
- `tasks.md` 구현 체크리스트만 있고 검증 기대가 없는 항목
- `checklists/requirements.md`의 메타 품질 항목(「No implementation details」 등)
- `plan.md` 헌법 점검·빌드 도구 버전
- 테스트로 확인할 수 없는 설명성 문장
- 산출물에서 확인되지 않는 추정 시나리오
- 운영자가 수동으로만 판단 가능한 모호한 항목

## 엑셀 양식 기준

기본 템플릿 파일:

```text
.agents/skills/tc-example-generator/templates/self-test-template.xlsx
```

(동일 경로: `.claude/skills/tc-example-generator/templates/self-test-template.xlsx`)

기본 작성 시트:

```text
Account API
```

1행은 헤더로 유지한다.

컬럼 구조는 다음 기준을 따른다.

| 컬럼 | 헤더 | 작성 여부 |
|---|---|---|
| A | 시험ID | 빈값 |
| B | 시험유형 | 빈값 |
| C | 시험구간 | 빈값 |
| D | 시험항목(대분류) | 작성 |
| E | 시험항목(중분류) | 작성 |
| F | 시험목적 | 작성 |
| G | 시험절차 | 작성 |
| H | 시험내역 | 작성 (테스트 예시) |
| I | 시험결과내역(판단근거) | 빈값 |
| J | 시험일자 | 빈값 |
| K | 시험자 | 빈값 |
| L | 시험결과 | 빈값 |
| M | 비고 | 빈값 |

중요 규칙:

- 생성 행에서는 **D~H** 컬럼을 채운다 (시험내역 = 테스트 예시).
- **A~C, I~M** 컬럼은 반드시 빈값으로 둔다.
- 기존 템플릿의 헤더, 컬럼 순서, 시트명, 기본 서식은 유지한다.
- 템플릿에 기존 샘플 데이터가 있으면 2행부터 데이터 영역을 비운 뒤 새 TC를 작성한다.
- 새 행의 서식은 템플릿의 기존 데이터 행 서식을 최대한 유지한다.
- 줄바꿈이 필요한 시험절차·시험내역은 셀 내부 줄바꿈으로 작성한다.

xlsx 작성은 Python `openpyxl` 등으로 템플릿을 복사·채우거나, 동등한 방식으로 수행한다.

### 시험절차 vs 시험내역

| 컬럼 | 역할 |
|------|------|
| **시험절차 (G)** | 수행 **순서** — 무엇을 준비·호출·확인할지 단계별 안내 |
| **시험내역 (H)** | 수행 시 참고할 **구체적 예시** — 요청·응답·DB 샘플 |

시험절차에 예시 JSON·curl을 길게 넣지 말고, 시험내역에 분리한다.

## 컬럼 작성 규칙

### D·E·F 역할 (대분류 / 중분류 / 시험목적)

| 컬럼 | 역할 | 한 줄 요약 |
|------|------|------------|
| **D 대분류** | 업무·도메인 상위 그룹 | 시험 구간을 사람이 훑어볼 때의 큰 묶음 |
| **E 중분류** | **API·연동규격 하위 그룹명** | 같은 API는 **항상 동일**한 중분류 문자열 |
| **F 시험목적** | **한 행의 검증 시나리오** | 정상/실패/변경·기대 resultcode·DB 상태 |

**중분류에 시나리오(정상·실패·코드·DB 결과)를 넣지 않는다.** 시나리오는 시험목적·시험절차에 쓴다.

### 시험항목(대분류)

기능 도메인 또는 업무 흐름의 **상위** 분류를 작성한다. API 직접 관련이 없는 변경(설정 제거·필드 삭제 등)도 여기서 도메인별로 구분한다.

작성 예시:

- 이메일 인증
- 회원가입
- 회원정보 변경
- SNS 간편로그인
- SNS 연동 해제 알림
- 회원 탈퇴
- 해외 로그인 차단
- 구글 RISC 수신

`spec.md` Feature 제목·User Story 제목을 우선하되, 외부 웹훅·내부 API를 같은 대분류에 섞지 않는다(예: 8.17~8.19는 「SNS 연동 해제 알림」, 12.3은 「SNS 간편로그인」).

### 시험항목(중분류)

**대분류 아래에서 구분 가능한 API·기능 그룹 이름**을 쓴다. `contracts/`·연동규격 표제·`openapi.yaml` summary에서 **짧게 인용**한다.

**명명 우선순위**

1. 연동규격 번호 + 공식 API 제목 — 예: `6.22 이메일 인증 발송`, `12.3 SNS 연동 설정`, `8.20 구글 RISC 연동 해제 알림`
2. 연동규격 번호 없음 — `contracts/http-outline.md` 절 제목·기능명 — 예: `ID 중복 확인`, `회원정보 변경`, `mailercheck 검증 API (삭제)`
3. 신규 Internal API만 있는 경우 — contract 절 제목 — 예: `구글 프로필 조회`

**규칙**

- **동일 API·동일 엔드포인트**에 대한 여러 TC 행(정상·실패·경계)은 **중분류 문자열을 동일**하게 유지한다.
- 여러 spec에서 같은 API를 다루면(예: 008·009의 12.3) 중분류는 **하나로 통일**한다.
- path·HTTP 메서드·resultcode 숫자는 중분류에 넣지 않는다(시험내역·시험목적으로).

좋은 예시 (중분류만):

```text
6.22 이메일 인증 발송
```

```text
8.17 네이버 연동 해제 알림
```

```text
12.3 SNS 연동 설정
```

```text
6.25 해외로그인 차단 OTP 알림톡 발송
```

나쁜 예시 (시나리오를 중분류에 넣은 경우):

```text
6.22 ID 중복 시 701
```

```text
네이버 HMAC 검증 실패 시 401
```

```text
정상 확인
```

### 시험목적

**해당 행 하나**가 검증하는 기대 결과를 작성한다. 중분류와 **같은 문장을 반복하지 않는다**.

**접두어 (권장)**

| 접두어 | 용도 |
|--------|------|
| `[정상]` | 성공·정상 처리·기대 필드 존재 |
| `[실패]` | 오류 코드·HTTP 실패·DB 미변경 |
| `[변경]` | 기존 API 응답·동작 변경(필드 제거·단계 생략 등) |

예:

```text
[실패] 이미 사용 중인 ID로 호출 시 resultcode 701이 나오고 메일이 나가지 않는지 확인한다.
```

작성 원칙:

- 무엇을 호출하거나 수행하는지, 어떤 결과를 확인하는지 **한두 문장**으로 쓴다.
- 가능하면 **와이어 resultcode 숫자**(200, 701, 803) 또는 **HTTP status**(204, 401)를 쓴다. `INTAPI_200` 같은 **코드 상수명**은 시험자용 문서에 쓰지 않는다(상수값이 200이면 `resultcode: 200`).
- 산출물에 없는 값은 임의로 쓰지 않는다.
- 구현 클래스명·Feign·mock 등 **개발 용어**는 쓰지 않는다. 필요 시 「SNS 제공자 연동 해제」「회원가입 Redis」 등 일상어로 쓴다.

### 시험절차

실제로 수행 가능한 단계로 작성한다.

작성 원칙:

- 번호 목록으로 작성한다.
- API 호출, 입력값 준비, 응답 확인, DB 확인 순서로 쓴다.
- `contracts/`·`quickstart.md`에 path·파라미터·응답코드가 있으면 포함한다. Internal·External 모두 **context-path `/v1`** 을 contracts 기준으로 통일한다(예: `/v1/api/account/...`, `/v1/account/sns/...`).
- 절차 안 경로는 **줄임(`…/otp/alarm`) 없이 전체 path**를 쓴다.
- DB 확인이 필요한 경우 `data-model.md`·`spec.md` Schema reference의 테이블·컬럼을 포함한다.
- 산출물에 값이 부족하면 「산출물 기준 확인 필요」라고 표시하고 임의로 보완하지 않는다.

기본 형식:

```text
1. 테스트 데이터를 준비한다.
2. {API명 또는 기능명}을 호출한다.
3. API Response에서 {기대 응답}을 확인한다.
4. 필요한 경우 DB에서 {테이블/컬럼} 값을 확인한다.
```

### 시험내역

시험 수행자가 바로 참고할 **테스트 예시**를 작성한다. 산출물에 있는 값만 사용한다.

**포함 가능 항목** (해당 TC에 맞는 것만, 산출물에 있을 때만):

- HTTP Method·URL (context-path `/v1` 포함 여부는 contracts 기준)
- 요청 헤더·쿼리·form·JSON body 예시 (`quickstart.md`·`contracts/`·`openapi.yaml` 인용)
- 기대 응답 예시 — `resultcode`·`message`·HTTP status·응답 body 필드
- DB 확인 예시 — 전·후 상태 (테이블·컬럼·값; `data-model.md`·`spec.md` Schema reference)
- Given/When/Then의 구체 조건 (spec Acceptance Scenarios에서 인용)

**작성 형식** (필요한 블록만, 셀 내부 줄바꿈):

```text
[요청 예시]
POST /v1/api/account/snsinfo
Content-Type: application/json
{ ... 산출물에 있는 필드·값 ... }

[기대 응답 예시]
resultcode: 803
message: (산출물에 명시된 경우)

[DB 확인 예시]
tbl_sns_info: id_key=..., sns_type=6 / (시험 전) 행 존재 / (시험 후) 행 삭제
```

**민감·비밀 값**: 실제 토큰·비밀번호·시크릿은 넣지 않는다. 산출물에 실값 예시가 없으면 `{valid_id_token}`, `{refresh_token}`, `{flow_token}` 등 **플레이스홀더**를 쓰고 필드명은 contracts·DTO 기준으로 적는다.

**예시가 없을 때**: 요청·응답 구조만 산출물에서 확인 가능하면 필드명·타입 수준으로 적고, 값은 「산출물 기준 확인 필요」로 표시한다. 임의 resultcode·endpoint·테이블값을 채우지 않는다.

좋은 예시:

```text
[요청 예시]
POST /v1/account/sns/naver/disconnect-receive
Content-Type: application/x-www-form-urlencoded
clientId={naver_client_id}&encryptUniqueId={encrypted_sns_id}&timestamp={epoch_ms}&signature={hmac_signature}

[기대 응답 예시]
HTTP 401 (HMAC 불일치 시)

[DB 확인 예시]
tbl_sns_info: sns_type=3, sns_id={복호화된 이용자 ID} / 변경 없음
```

나쁜 예시:

```text
정상 동작 확인
```

```text
API 호출 후 결과 확인
```

## 생성 절차

1. 사용자가 제공한 spec 번호를 정규화한다 (`6` → `006`). 복수 번호면 각각 찾는다.
2. `specs/`에서 번호와 일치하는 기능 디렉터리를 찾는다.
3. [읽기 순서](#읽기-순서-tc-생성용)에 따라 관련 문서를 읽고, 선행 기능 `contracts/` 참조가 있으면 해당 절만 추가로 읽는다.
4. **신규·변경 API 목록**을 `contracts/`에서 먼저 뽑고, API마다 **중분류 이름(연동규격 제목)** 을 하나 정한다.
5. API별로 정상/실패/경계/DB검증 행을 나눈다(한 행 = 하나의 검증 목적).
6. 중복 시나리오는 병합하되, **중분류는 API 단위로 유지**한다.
7. 각 행을 다음 5개 필드로 변환한다.
   - **대분류** — 도메인
   - **중분류** — API 그룹명(동일 API 동일 문자열)
   - **시험목적** — `[정상]`/`[실패]`/`[변경]` + 검증 내용
   - **시험절차** — 단계별 수행 순서
   - **시험내역** — 요청·응답·DB 예시
8. 템플릿 xlsx를 복사한다 (`.agents/skills/tc-example-generator/templates/self-test-template.xlsx`).
9. 2행부터 기존 데이터 값을 제거한다.
10. **D~H** 컬럼에 TC를 작성한다. 가능하면 **대분류 → 중분류** 순으로 행을 모은다.
11. **A~C, I~M** 컬럼은 빈값으로 둔다.
12. 결과 xlsx를 저장한다 (단일 spec: `specs/{기능}/` 또는 루트; 복수 spec: `TC_{번호범위}_api_{YYYYMMDD}.xlsx` 등).
13. 생성 후 **중분류가 API별로 묶였는지**, 시나리오가 시험목적에만 있는지 self-check 한다.
14. 사용자에게 생성 파일 링크·작성 건수·참고 산출물·추가 확인 필요 항목을 보고한다.

참고: 복수 spec 일괄 생성 스크립트 예시 — `.agents/skills/tc-example-generator/scripts/generate_tc_001_010.py` (데이터·규칙 갱신 시 스크립트와 스킬 본문을 함께 맞춘다).

## 품질 기준

- 한 행은 **하나의 검증 목적**만 가진다.
- **중분류 = API 그룹명**, **시험목적 = 시나리오** — 서로 역할을 바꾸지 않는다.
- 중분류와 시험목적이 동일한 문장으로 반복되지 않게 작성한다.
- 같은 API의 중분류 문자열이 행마다 달라지지 않게 한다.
- 시험절차는 비개발자도 따라 할 수 있는 **짧은 번호 목록**으로 쓴다.
- 시험내역은 시험절차와 중복되지 않게 **요청·응답·DB 샘플** 위주로 작성한다.
- 정상만 만들지 말고 산출물 근거가 있으면 실패·변경 케이스도 포함한다.
- resultcode·HTTP status·메시지·테이블명·컬럼명은 산출물에 있는 값만 사용한다.
- 문장은 **읽기 쉬운 일상어**로 통일한다(접속 토큰, 연동 해제, 폼 형식 등). 개발 은어·영어 혼용은 필드명·경로 등 계약에 필요한 경우만 허용한다.
- 모르는 값은 추정하지 않는다. 빈칸으로 두라는 컬럼은 채우지 않는다.

## 결과 파일명

기본 파일명 형식:

```text
TC_{번호}_{기능slug}_{YYYYMMDD}.xlsx
```

예: `TC_009_sns-profile-aud-verify_20260610.xlsx`

기능명을 알 수 없으면:

```text
TC_{번호}_{YYYYMMDD}.xlsx
```

복수 spec·API 통합 파일:

```text
TC_001-010_api_{YYYYMMDD}.xlsx
```

## 응답 형식

작업 완료 후 사용자에게 다음만 간략히 전달한다.

```text
완료했어.
- 생성 파일: {xlsx 링크}
- 작성 건수: {n}건
- 기준 spec: {specs/경로}
- 참고 산출물: {파일명 목록}
- 추가 확인 필요: {있으면 요약, 없으면 없음}
```

## 추가 확인 필요로 분리할 상황

다음 상황은 결과 파일 생성은 진행하되, 사용자에게 별도로 알린다.

- spec 번호와 일치하는 디렉터리가 여러 개 있는 경우
- `spec.md`만 있고 `contracts/`·`data-model.md`가 없어 API·DB 값이 부족한 경우 (예: `003-google-sns-connect`)
- API 경로는 있으나 요청/응답 예시가 없는 경우
- 요구사항은 있으나 기대 결과가 명확하지 않은 경우
- DB 확인이 필요해 보이지만 테이블/컬럼명이 없는 경우
- 선행 기능 contract 참조 범위가 모호한 경우
- 대분류를 판단할 근거가 부족한 경우
- 템플릿의 헤더가 예상과 다른 경우

## 금지 사항

- **중분류**에 정상/실패·resultcode·HTTP status·「~시 확인」 같은 **시나리오 문장**을 넣지 않는다.
- 시험ID, 시험유형, 시험구간, 시험결과내역, 시험일자, 시험자, 시험결과, 비고를 임의로 채우지 않는다.
- 시험내역에 실제 운영 토큰·비밀번호·시크릿을 넣지 않는다.
- 산출물에 없는 resultcode, message, endpoint, table, column을 만들어내지 않는다.
- 단순히 문장만 나열하지 말고 실제 테스트 절차로 변환한다.
- xlsx 대신 markdown 표만 제공하고 끝내지 않는다.
- 기존 템플릿의 헤더 구조를 임의로 바꾸지 않는다.
- 구현 코드(`src/`)만 보고 산출물에 없는 동작을 TC에 추가하지 않는다.
