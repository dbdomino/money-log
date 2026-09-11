# Quickstart: 프론트 공통 기반 — 화면 모듈 재구성

**Feature**: 007-frontend-foundation | **Date**: 2026-09-08 | **Plan**: [plan.md](./plan.md)

이 문서는 **실행하고 검증하는 절차**다. 구현 순서는 `tasks.md` 가 낸다.

## 0. 착수 전

### 선행 기능

| 선행 | 왜 필요한가 |
|---|---|
| 002~006 (백엔드 API 56건) | 007 은 소비만 한다. `:8081` 이 떠 있어야 US1·US2 를 실제로 볼 수 있다 |
| `화면기획/` 산출물 | `tokens.css`·`ui.css`·`modal.js`·proto 17개가 이미 완성돼 있다. 007 은 경로만 맞춰 옮긴다 |

**008~012 는 선행이 아니다.** 007 은 껍데기와 통로만 만들고, 그 위 화면 30개는 그 스펙들이 맡는다.

### 명세 선행 개정 (원칙 V)

구현 전에 네 건을 고친다. 근거는 [plan.md](./plan.md) § 명세 선행 개정.

| # | 파일 | 고칠 것 |
|---|---|---|
| 1 | `프로젝트설계/기능명세상세-프론트엔드/phase1-회원/1.6-ErrorForbidden.md` | 메타 레이아웃 `main` → `auth` |
| 2 | 같은 파일 | 「화면 구성」·「동작」·「검증·안내」의 `(작성 예정)` 세 표를 채운다 |
| 3 | `프로젝트설계/기능명세상세-프론트엔드/_공통.md` § 표시방식 | 구 URL redirect 를 [contracts/url-map.md](./contracts/url-map.md) 기준으로 적는다 |
| 4 | 같은 파일 § 인증·토큰 「세션 만료」 행 | `server.servlet.session.timeout` = **30분** 을 적는다 |

확인:

```bash
grep -n "작성 예정" "프로젝트설계/기능명세상세-프론트엔드/phase1-회원/1.6-ErrorForbidden.md"
# → 0건이어야 한다

grep -n "레이아웃" "프로젝트설계/기능명세상세-프론트엔드/phase1-회원/1.6-ErrorForbidden.md"
# → auth
```

## 1. 전제 — 지금 상태 확인

착수 시점의 위반을 눈으로 본다. 이 값들이 완료 판정의 "전" 값이다.

```bash
# money-app 이 의존하는 것 (전: core-mod · data-mod · jpa · mybatis · postgresql · aspectj)
sed -n '/money-app/,/^}/p' build.gradle

# DB 참조 (전: 여러 건)
grep -rn "data-mod\|javax.persistence\|jakarta.persistence\|mybatis\|@Entity\|Repository" \
  app-mod/money-app/src/main/java | wc -l

# 프로필 (전: postgresql)
grep -n "profiles" app-mod/money-app/src/main/resources/application.yml

# 로깅 (전: logback.xml — springProfile 로 감싸여 appender 가 붙지 않는다)
ls app-mod/money-app/src/main/resources/logback*.xml
```

DB 스키마는 **읽기만** 한다. 이 기능은 `sql/schema-moneylogdb.sql` 을 바꾸지 않는다.

## 2. 실행

```bash
# 백엔드 (:8081) — 먼저 띄운다
./gradlew :app-mod:money-backend-app:bootRun

# 화면 (:8080)
./gradlew :app-mod:money-app:bootRun
```

시험:

```bash
./gradlew :app-mod:money-app:test          # 007 이 새로 만드는 시험 (DB 불필요)
./gradlew test                             # 전 모듈 (SC-603 — 한 번에 통과해야 한다)
```

**전 모듈 시험은 PostgreSQL 이 떠 있어야 한다** — `data-mod` 의 스키마 검증이 실제 DB 를 쓴다.
반대로 아래 §3 US1 의 "DB 없이 기동"은 DB 를 내린 상태에서 본다. **두 확인은 전제가 반대라
같은 실행에서 함께 할 수 없다.**

**`./gradlew test`(전체)가 통과하는 것이 이 기능의 요구사항이다**(FR-608·SC-603).
지금은 `money-app` 의 레거시 시험 3건이 `Failed to determine a suitable driver class` 로
깨져 있어 모듈을 나눠 돌려야 한다.

## 3. 검증 시나리오

### US1 — 화면이 DB 대신 백엔드 API 를 본다 (P1, MVP)

**DB 없이 기동** (SC-601)

```bash
# PostgreSQL 을 내린 상태에서
./gradlew :app-mod:money-app:bootRun
# → 정상 기동. datasource 관련 로그가 한 줄도 없어야 한다.
```

DB 를 실제로 내리기 어려우면 같은 것을 시험으로 본다 — `MoneyAppApplicationTests` 가
datasource 없이 컨텍스트를 띄운다. **`@SpringBootTest` 가 뜨면 datasource 가 없다는 뜻**이다.

**DB 참조 0건** (SC-602)

```bash
grep -rn "data-mod\|jakarta.persistence\|javax.persistence\|mybatis\|org.postgresql" \
  app-mod/money-app/src/main/ build.gradle
# → build.gradle 의 money-app 블록과 money-app 소스에서 0건

grep -rn "@Entity\|JpaRepository\|@Mapper" app-mod/money-app/src/main/java
# → 0건
```

**봉투 해석** (Acceptance 2·3)

`BackendApiClientTest` 가 `MockRestServiceServer` 로 세운다.

| 세운 응답 | 기대 |
|---|---|
| `{"resCode":200,"data":{"paymentMethodId":1}}` | `data` 가 타입으로 변환되어 반환 |
| `{"resCode":3003,"data":{"message":"..."}}` | `BackendApiException(3003, "...")` 발생 |
| `{"resCode":3003,...}` | 예외의 코드가 **3003 그대로**다. 화면 모듈이 바꾸지 않는다 (SC-609) |
| 연결 거부 | `BackendUnavailableException` (코드 없음) |
| `<html>...` (봉투 아님) | `BackendUnavailableException` |

**바이너리 2건** (Acceptance 5)

| 호출 | 세운 응답 | 기대 |
|---|---|---|
| `getBinary(아이콘)` | `image/png` + 바이트 | `BinaryPayload` — 봉투 해석 없음 |
| `getBinary(아이콘)` | `{"resCode":3103,...}` | `BackendApiException(3103)` — 바이트로 착각하지 않는다 |

**백엔드가 꺼져 있을 때** (Acceptance 4 · SC-608)

백엔드를 내린 채 `/error/forbidden` 이 아닌 아무 보호 URL 을 연다 →
"서버에 닿지 못했습니다"가 화면에 **글로** 보이고, 빈 목록이 정상처럼 뜨지 않는다.

### US2 — 로그인 상태 유지와 권한 (P1)

로그인 화면(008)이 없으므로 **세션에 값을 직접 심어** 시험한다. 005·006 이 없던 API 대신
데이터를 심어 시험한 것과 같은 방식이다.

| # | 상황 | 기대 | 근거 |
|---|---|---|---|
| 1 | 세션 빔 + 보호 URL | `302 → /auth/login` | FR-618 · SC-604 |
| 2 | 세션 있음 + 보호 URL (`validate` 200) | 화면이 열린다 | FR-614 |
| 3 | `role=3` + `/admin/members` | `302 → /error/forbidden` | FR-619 · SC-605 |
| 4 | **세션 빔** + `/admin/members` | `302 → /auth/login` (**forbidden 이 아니다**) | FR-620 |
| 5 | 호출이 `1001` → 재발급 200 | 원래 요청이 다시 나가 성공 | FR-615 · SC-606 |
| 6 | 재발급 응답의 새 `refreshToken` | 세션에 반영됨 | research 7 |
| 7 | 재시도 후 다시 `1001` | 두 번째 재발급이 **없다**. 세션 무효화 | FR-617 |
| 8 | 호출이 `1006` | 재발급 시도 **없이** 세션 무효화 | FR-616 |
| 9 | 로그아웃 | `Revoke` **먼저**, 그다음 `invalidate()` | FR-621 |
| 10 | 아무 응답 HTML | JWT 원문 문자열이 **없다** | FR-622 · SC-607 |
| 11 | 아이콘 프록시 요청 | `MemberTokenValidate` 가 **나가지 않는다**(호출 0건) | session-auth.md §2 |

5·7 은 `MockRestServiceServer` 의 **호출 횟수**로 확인한다 — `/auth/refresh` 가 1번인지 2번인지.

**보호 URL 전부를 세는 방법**(SC-604·605): 화이트리스트(비로그인 허용 6개)만 두고, 시험은
그 목록에 없는 URL 을 골라 확인한다. 007 시점에는 화면이 1.6 뿐이므로 **인터셉터가 매칭하는
패턴**을 대상으로 시험한다. 화면이 늘 때마다 목록을 갱신할 필요가 없는 구조인지가 핵심이다.

### US3 — 모든 화면이 같은 껍데기를 쓴다 (P2)

| # | 확인 | 근거 |
|---|---|---|
| 1 | main 레이아웃 화면에 사이드바가 있고 `activeMenu` 가 활성 | FR-625 |
| 2 | `role=3` 응답 HTML 에 `/admin/members` 가 **없다** | FR-626 |
| 3 | `?m=` 이 아는 값이면 모델에 `openModal` 이 있다 | FR-627 · SC-610 |
| 4 | 취소·X·Esc·딤 넷 다 닫힌다 | FR-628 |
| 5 | `?m=nonsense` → `openModal` **없음** + HTTP 200 부모 페이지 | FR-630 · SC-610 |
| 6 | `static/css/tokens.css`·`ui.css`·`js/modal.js` 가 있고 레이아웃이 건다 | FR-631 |
| 7 | `/error/forbidden` 이 미로그인으로 200 | FR-632 |

4번은 브라우저에서 눈으로 본다 — `modal.js` 는 이식만 하고 고치지 않으므로 시험 대상은
"이식이 됐는가"다.

3·5 를 시험할 대상 화면은 1.6 이 아니다(모달이 없다). **모달 셸을 붙인 시험 전용 화면**을
시험 소스에 두고 그것으로 확인한다.

### US4 — 옛 주소로 들어와도 화면에 닿는다 (P3)

| # | 요청 | 기대 `Location` |
|---|---|---|
| 1 | `/` (세션 있음) | `/ledger` |
| 2 | `/` (세션 없음) | `/auth/login` |
| 3 | `/mem/login` | `/auth/login` |
| 4 | `/mem/ind` | `/` |
| 5 | `/payments/new` (세션 있음) | `/payments?m=create` |
| 6 | `/payments/1/edit` (세션 있음) | `/payments?m=edit&id=1` |
| 7 | `/payments/new` (세션 없음) | `/auth/login` — redirect 도 인터셉터를 탄다 |
| 8 | `/expend-groups/icons/1_1.png` | redirect 아님. 프록시가 받는다 |

`/ledger`·`/auth/login`·`/payments` 는 008~010 이 만들 화면이라 **007 시점에는 없다.**
시험은 `Location` 헤더만 본다.

## 4. 완료 판정

확인 결과는 2026-09-11 기준이다.

- [x] **PostgreSQL 을 띄운 채** `./gradlew test` (전 모듈)가 **한 번에** 통과한다 — SC-603
  → 834건 통과(`data-mod` 80 · `money-app` 62 · `money-backend-app` 692), 실패 0
- [x] DB 를 내린 채 `money-app` 이 기동한다 — SC-601
  → 1.4초에 기동하고 datasource 관련 로그가 0줄. 시험도 `DataSource` 빈이 0개임을 건다
- [x] `money-app` 소스·`build.gradle` 에 `data-mod`·`core-mod`·JPA·MyBatis·PostgreSQL 참조가 0건 — SC-602
  → 자바 import 0건 · 의존 선언 0건 · **runtimeClasspath 에도 0건**
- [x] 화이트리스트 밖 URL 패턴을 미로그인으로 열면 **예외 없이** `/auth/login` — SC-604 (007 시점 기준. 화면이 다 선 뒤의 전수 확인은 012)
- [x] `/admin/**` 패턴을 `role=3` 으로 열면 **예외 없이** `/error/forbidden` — SC-605 (전수 확인은 012)
- [x] `1001` 후 재로그인 없이 원래 요청이 성공한다 — SC-606
  → 재발급 호출이 **1회뿐**이라는 것도 가짜 서버의 기대 건수로 함께 고정했다
- [x] 어떤 응답에도 JWT 원문이 없다 — SC-607
- [x] 백엔드가 꺼진 상태가 화면에 **글로** 보인다 — SC-608
  → **시험으로 확인했다.** 007 에는 백엔드를 부르는 화면이 하나도 없어(1.6 은 호출 없음) 뜬 앱에서 눈으로 볼 대상이 없다. 실제 화면에서의 확인은 008 의 첫 화면이 서는 시점이다
- [x] 백엔드 실패의 `resCode` 가 바뀌지 않고 화면까지 온다 — SC-609
- [x] `?m=` 딥링크가 열리고, 모르는 값이면 부모만 뜬다 — SC-610
  → 1.6 에는 모달이 없어 **시험 전용 화면**(`src/test/resources/templates/shelltest/`)으로 확인한다
- [x] `logs/` 에 파일 로그가 쌓인다 (원칙 IV — 지금은 한 줄도 없다)
  → `app-mod/money-app/logs/money-app.log` 가 생긴다. 기동 디렉터리 기준 경로다
- [x] `git diff sql/schema-moneylogdb.sql` 이 **비어 있다** (원칙 VI — 이 기능은 DB 를 안 건드린다)
- [x] 선행 개정 4건이 반영돼 있고 `1.6-ErrorForbidden.md` 에 `(작성 예정)` 이 0건 (원칙 V)

### 개발 기동에서만 보이는 것

`bootRun` 으로 띄우면 404 같은 오류의 **JSON 응답에 예외 스택이 실린다.** `application.yml` 은
`server.error.include-stacktrace: never` 로 두었지만 `spring-boot-devtools` 가 개발 편의를 위해
그 값을 덮는다. devtools 는 `developmentOnly` 의존이라 배포 산출물에는 들어가지 않으므로 운영
응답에는 스택이 실리지 않는다.

브라우저처럼 `Accept: text/html` 로 요청하면 `templates/error.html` 이 그려진다. 스택이 실리는
것은 HTML 을 받지 않는 클라이언트의 JSON 응답 쪽이다.

## 5. 다음

007 이 끝나면 008~012 는 **화면만** 만든다. 그 스펙들이 007 에서 받아 가는 것:

| 받아 가는 것 | 어디에 |
|---|---|
| `BackendApiClient` — 봉투·바이너리·재발급·실패 | [contracts/api-client.md](./contracts/api-client.md) |
| 세션·인터셉터 — 로그인·권한 판정 | [contracts/session-auth.md](./contracts/session-auth.md) |
| `POST /auth/logout` — Revoke 후 세션 무효화 (008 은 버튼만 만든다) | session-auth.md §5 |
| 레이아웃 2종·사이드바·모달 셸·확인 다이얼로그 | [contracts/screen-shell.md](./contracts/screen-shell.md) |
| `Paging` — `offset` 환산 | api-client.md § 5 |
| `ModalParam` — `?m=` 판정 방법 | screen-shell.md § 3 |

각 스펙이 정할 것은 자기 화면의 `?m=` 값 목록, 실패 코드별 문구, 폼 필드다.
