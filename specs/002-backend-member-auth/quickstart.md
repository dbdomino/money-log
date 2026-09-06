# Quickstart: 회원 가입·인증·토큰·관리자 회원 관리

**Feature**: `002-backend-member-auth` | **Date**: 2026-09-02 | **Phase**: 1

이 기능이 실제로 동작하는지 확인하는 **검증 가이드**다. 구현 코드는 담지 않는다 —
작업 분해는 `/speckit-tasks`가, 구현은 `/speckit-implement`가 한다.

---

## 0. 착수 전 (원칙 V — 명세 우선)

**코드를 쓰기 전에** `프로젝트설계/`를 먼저 고친다. 셋 다 스펙에서 결론이 확정된 것이라
판단할 것은 없고 반영만 하면 된다.

| # | 파일 | 고칠 내용 |
|---|---|---|
| 1 | `_공통.md` § 구현 위치 가이드 | `common-mod/.../security/MemberSessionService.java`·`TokenAuthenticationFilter.java` → `money-backend-app` 경로로. `JwtTokenProvider`는 그대로 둔다 |
| 2 | `phase1-회원/1.2-MemberSignup.md` | 비고의 `tbl_member.pw` → `tbl_user.pw` |
| 3 | `phase1-회원/1.3-MemberLogin.md` | 비고의 `tbl_member_session` → `tbl_user_session` |

확인:

```bash
grep -rn 'tbl_member' 프로젝트설계/기능명세상세-백엔드/phase1-회원/
# → 0건이어야 한다
```

---

## 1. 전제

| 항목 | 값 |
|---|---|
| PostgreSQL 18 | `localhost:5432` · DB `moneylogdb` · 스키마 `moneylog` |
| 스키마 | 15개 테이블이 이미 있어야 한다. **이 기능은 스키마를 바꾸지 않는다** |
| 백엔드 | `:8081` · `/api/v1` |
| `jwt.secret` | 256비트 이상. 환경변수로 주입한다 |

**주의 — `ddl-auto`가 `create`다.** 백엔드를 기동할 때마다 전 테이블이 drop 후 재생성된다.
검증 중에 만든 회원·세션은 재기동하면 사라진다. 데이터를 남겨야 하면
`data-mod/src/main/resources/application-postgresql.yml`의 `ddl-auto`를 `update`로 되돌린다
(그 경우 테이블 주석이 붙지 않는다).

스키마 상태 확인:

```bash
psql -h localhost -U moneyloguser -d moneylogdb -c \
  "select count(*) from information_schema.tables where table_schema='moneylog';"
# → 15
psql -h localhost -U moneyloguser -d moneylogdb -c \
  "select column_name from information_schema.columns
    where table_schema='moneylog' and table_name='tbl_user_login_history' and column_name='success';"
# → success (없으면 커밋 1edf308 이 반영되지 않은 것이다)
```

---

## 2. 실행

```bash
./gradlew :app-mod:money-backend-app:bootRun     # 백엔드 (:8081)

./gradlew :data-mod:test                          # 스키마 IT (기존 77건이 계속 통과해야 한다)
./gradlew :app-mod:money-backend-app:test         # 이 기능의 테스트
```

**`./gradlew test`(전체)는 쓰지 않는다.** `money-app`의 레거시 테스트 3건이 `init` 커밋부터
깨져 있다(테스트용 `application.yml`이 메인 설정을 덮어쓰는데 datasource가 없다).
모듈별로 나눠 돌린다.

---

## 3. 검증 시나리오

각 시나리오는 spec.md의 User Story·SC와 1:1로 대응한다. 통합 테스트의 `@DisplayName`을
`#N`으로 맞춰 두면 빠진 번호를 바로 찾을 수 있다 — `001`에서 쓴 방식이다.

### US1 — 인증 기반 (P1, MVP)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 1 | 활성 회원으로 로그인 | Access(1일)·Refresh(7일) 발급, 세션 1건 저장 | US1-1 |
| 2 | 같은 회원으로 재로그인 | 기존 세션 해시 2개가 NULL·`revoked=true`, 활성 세션은 새 것 1건 | US1-2·SC-103 |
| 3 | 유효한 Access로 `/auth/validate` | `valid=true` + 남은 만료 시간 | US1-3 |
| 4 | 재로그인으로 폐기된 토큰으로 아무 API | `1006` | US1-4·SC-104 |
| 5 | Refresh로 갱신 | 새 Access·Refresh 쌍, **같은 `session_id`** 행의 해시·만료 갱신 | US1-5 |
| 6 | 로그아웃 후 그 Refresh로 갱신 | `1005` | US1-6 |
| 7 | 비활성 계정으로 로그인 | `1004` | US1-7 |
| 8 | 로그인 → 검증 → 갱신 → 로그아웃 한 바퀴 | 전부 성공 | SC-102 |

5번은 **`session_id`가 바뀌지 않았는지**까지 확인한다. 새 행이 생기거나 UUID가 바뀌면
Rotation이 아니라 재로그인이 된 것이다.

### US2 — 가입과 본인 정보 (P2)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 9 | 새 아이디로 가입 | `role=3`, `pw`는 bcrypt 해시 | US2-1 |
| 10 | 가입 직후 그 회원의 지출유형 | **정확히 10건**, 전부 `defaultGroup=true`, 아이콘 파일명 있음 | US2-2·SC-105 |
| 11 | 중복 아이디로 가입 | `2002` | US2-3 |
| 12 | 이메일 없이 두 명 가입 | 둘 다 성공 (부분 유니크라 NULL은 중복이 아니다) | US2-4 |
| 13 | 비밀번호 확인 불일치 | `2005` | US2-5 |
| 14 | 본인 정보 조회 | 응답에 비밀번호가 **없다** | US2-6·SC-107 |
| 15 | 본인 정보 수정에서 일부 omit | omit한 필드는 그대로 | US2-7 |

10번은 아이콘 파일명이 `{id_key}_{expendGroupId}.png` 형태인지도 본다.
12번이 부분 유니크 인덱스 `ux_user_email`의 실제 동작을 확인하는 자리다.

### US3 — 아이디·비밀번호 찾기 (P3)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 16 | 가입 이메일로 아이디 찾기 | 아이디 반환, **원문과 다르고** `masked=true` | US3-1·SC-109 |
| 17 | 없는 이메일로 아이디 찾기 | `2001` | Edge Case |
| 18 | 비밀번호 재설정 후 옛 비밀번호로 로그인 | `1003` | US3-2 |
| 19 | 비밀번호 재설정 후 세션 확인 | 폐기되어 있다 | US3-3 |
| 20 | 1.10을 건너뛰고 1.11만 직접 호출 | 1.10을 거친 것과 **판정이 같다** | Edge Case |

20번이 "재설정 토큰이 없다"는 결정을 실제로 검증하는 자리다. 상태를 들고 다니지 않으므로
1.11 단독 호출이 성공해야 한다.

### US4 — 관리자 회원 관리 (P4)

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 21 | 일반 토큰으로 관리자 API 5건 | **전부 `1002`** | US4-1·SC-106 |
| 22 | 관리자가 회원 목록 조회 | `data.list` 배열 + `offset`·`limit`·`totalCount`, `page` 없음 | US4-2 |
| 23 | `offset`이 `limit`의 배수가 아님 | `9001` | FR-120 |
| 24 | `offset`·`limit` 누락 | `9001` (기본값으로 통과하지 않는다) | FR-120 |
| 25 | 관리자가 회원 정지 → 그 회원 로그인 | `1004`, **회원 행과 데이터는 남아 있다** | US4-3 |
| 26 | 정지 후 그 회원의 세션 | 폐기되어 있다 | US4-4 |
| 27 | 이미 정지된 회원 재정지 | `9001` | Edge Case |
| 28 | 관리자가 자기 계정 정지 | 거절 (`9001`) | Edge Case |
| 29 | 관리자가 회원 추가 | `created_by`에 **관리자의 `id_key`** | US4-5·FR-121 |
| 30 | 관리자 추가 직후 그 회원의 지출유형 | **정확히 10건** | SC-105 |

24번이 `Pageable` 자동 바인딩을 쓰지 않기로 한 결정을 검증한다
([research.md §12](./research.md)). 자동 바인딩이면 기본값으로 채워져 200이 나간다.

29번은 본인 가입(`created_by`가 `null`)과 대비해 확인한다.

### 이력·로깅

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 31 | 로그인 성공 1회 + 비밀번호 오류 1회 | 이력 행 각 1건, `success`가 `true`·`false` | SC-108 |
| 32 | **존재하지 않는 아이디**로 로그인 | 이력 행이 **늘지 않는다** | SC-110·FR-127 |
| 33 | 이력 테이블 전 행 검사 | 비밀번호·토큰 값이 없다 | SC-111 |
| 34 | 로그인·갱신 요청의 로그 파일 | `password`·`accessToken`·`refreshToken`·`Authorization`이 `***` | FR-128 |

32번이 FR-127의 핵심이다. 회원을 특정할 수 없으면 `id_key`를 채울 수 없어 행을 만들지 않는다.

### 응답 규격

| # | 시나리오 | 기대 | 대응 |
|---|---|---|---|
| 35 | 16건 전부 호출 | 성공 HTTP 200 + `resCode 200` | SC-101 |
| 36 | 비즈니스 실패(예: `2002`) | **HTTP 200** + 4자리 `resCode` | SC-101 |
| 37 | 인증 없이 보호 API | `{ resCode: 1001 }` (Spring 기본 401 JSON이 아니다) | api-contract §2 |
| 38 | 일반 토큰으로 관리자 API | `{ resCode: 1002 }` (Spring 기본 403 JSON이 아니다) | api-contract §2 |

37·38이 `RestAuthEntryPoint`·`AccessDeniedHandler`를 실제로 검증하는 자리다.
이 둘을 빠뜨리면 Spring Security의 기본 응답이 규격을 벗어난 채로 나간다.

### 회귀 — 임시 조치 제거

| # | 시나리오 | 기대 |
|---|---|---|
| 39 | `data-mod` 스키마 IT 77건 | 전부 통과 |
| 40 | `BaseAuditEntity`에 `@Setter`가 없다 | `AuditorAware` 실 구현으로 대체됨 |
| 41 | `AbstractSchemaIT.stampAudit()`이 없다 | 위와 같음 |

39~41은 `JpaAuditingConfig`의 TODO가 지목한 정리 작업이다. 40·41을 걷어낸 뒤에도
39가 통과해야 실 구현이 제 역할을 한다는 뜻이다.

---

## 4. 완료 판정

| 항목 | 확인 수단 |
|---|---|
| 명세 선행 개정 3건 | `grep -rn 'tbl_member' 프로젝트설계/.../phase1-회원/` → 0건 |
| API 16건 동작 | 시나리오 1~38 |
| SC-101~111 (11건) | 위 표의 "대응" 열에 전부 나온다 |
| 임시 조치 제거 | 시나리오 39~41 |
| 스키마 무변경 | `git diff sql/schema-moneylogdb.sql` → 변경 없음 |
| `:data-mod:test` | 77건 통과 |
| `:app-mod:money-backend-app:test` | 신규 테스트 통과 |
| 헌장 게이트 6개 | [plan.md § Constitution Check](./plan.md#constitution-check) |

**스키마 무변경 확인이 원칙 VI의 이 기능판이다.** 덤프가 바뀌었다면 의도치 않게 Entity를
건드린 것이므로 원인을 찾는다.
