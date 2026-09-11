# Contract: 백엔드 API 클라이언트

**Feature**: 007-frontend-foundation | **관련 FR**: 601~612 · 622

`BackendApiClient` 는 화면 모듈이 백엔드로 나가는 **유일한 통로**다. 008~012 는 이 계약만
보고 호출하며, `{ resCode, data }` 봉투를 직접 다루지 않는다.

## 1. 봉투를 푸는 자리는 여기 하나다

백엔드는 성공·실패 모두 아래 형태로 답한다(헌장 원칙 III). **실패도 HTTP 200 이다.**

```json
{ "resCode": 200, "data": { } }
```

| 받은 것 | 클라이언트가 하는 일 |
|---|---|
| `resCode == 200` | `data` 를 요청한 타입으로 변환해 **반환한다** |
| `resCode != 200` (4자리) | `BackendApiException(resCode, data.message)` 를 **던진다** |
| 봉투가 아닌 본문 (HTML·빈 응답 등) | `BackendUnavailableException` 을 던진다 |
| 연결 거부·타임아웃 | `BackendUnavailableException` 을 던진다 |

**호출부는 `resCode` 를 보지 않는다.** 성공이면 값이 오고, 아니면 예외가 온다.
반환값으로 실패를 돌려주지 않는 이유는 확인을 빠뜨린 코드도 컴파일되기 때문이다(FR-604).

`data.message` 는 백엔드 문구 그대로 싣는다. 화면별 문구로 바꾸는 것은 그 화면의 스펙이 한다.
화면 모듈은 **코드를 새로 정의하지 않는다**(FR-607).

## 2. 봉투를 건너뛰는 호출이 둘 있다

| API | 기능번호 | 본문 |
|---|---|---|
| `ExpendGroupIconGet` | 003 의 2.10 | 이미지 바이트 |
| `ExpenseIncomeExcelTemplateDownload` | 004 의 3.11 | 엑셀 바이트 |

이 둘은 `getBinary(...)` 로 부르고 `BinaryPayload`(바이트 · Content-Type · 파일명)를 받는다.
**Content-Type 을 보고 자동으로 갈라지지 않는다** — 호출부가 `getBinary` 를 부르는 것이 곧
"이건 바이너리다"라는 선언이다(research 5).

바이너리 호출에 **봉투(JSON)가 오면 그것은 실패다.** 백엔드가 실패 봉투를 준 경우이므로
`BackendApiException` 으로 올린다.

007 이 만드는 프론트 엔드포인트는 아이콘 프록시 하나뿐이고, 엑셀 다운로드 엔드포인트는
010 이 만든다. 007 은 **바이트를 실어 올 수 있는 통로까지** 낸다.

## 3. 메서드별 호출 규칙 (FR-609)

| Method | Path | Query | Body | 규칙 |
|---|:---:|:---:|:---:|---|
| GET | ✅ 또는 | ✅ 또는 | ❌ | Path **또는** Query 중 **하나만**. 혼용 금지 |
| POST | ❌ | ❌ | ✅ | Body 만 (JSON 또는 multipart) |
| PATCH | ✅ (키) | ❌ | ✅ (변경분) | omit 한 필드는 유지된다 |
| DELETE | ✅ | ❌ | ❌ | Path 만 |
| PUT | — | — | — | **쓰지 않는다.** 메서드 자체를 클라이언트가 제공하지 않는다 |

**유일한 예외**: `StatisticsMonthlyGet` — Path 연·월 + Query `view`(`saved`\|`live`) 를 함께 쓴다.

`PUT` 을 막는 방법은 검증이 아니라 **부재**다. 클라이언트에 `put()` 메서드를 두지 않으면
호출할 수 없다. 규칙을 지키는지 시험으로 세는 것보다 짧다.

multipart 는 POST 에만 쓴다 — 아이콘 업로드(009)와 엑셀 업로드(010)가 대상이다.

## 4. 인증 헤더와 재발급

| 시점 | 동작 |
|---|---|
| 매 호출 | 세션의 `accessToken` 을 `Authorization: Bearer` 로 붙인다 (FR-614) |
| `1001` 수신 | 세션의 `refreshToken` 으로 **한 번** 재발급하고 원래 요청을 다시 보낸다 (FR-615) |
| 재발급 성공 | 세션의 `accessToken`·`refreshToken` 을 **둘 다** 덮어쓴다 (Rotation) |
| 재발급 실패 (`1005`·`1004` 등) | 세션 무효화 → `/auth/login` (FR-616) |
| `1006` 수신 | **재발급 없이** 세션 무효화 → `/auth/login` (FR-616) |
| 재발급 직후 다시 `1001` | **또 재발급하지 않는다.** 세션 무효화 → `/auth/login` (FR-617) |

세부는 [session-auth.md](./session-auth.md).

토큰이 없는 호출도 있다 — `MemberLogin`·`MemberSignup`·`MemberTokenRefresh` 등 비로그인
API 다. 이들은 헤더를 붙이지 않고 부르며, `1001` 재발급 흐름도 타지 않는다.

## 5. 페이징 (FR-610)

목록 API 는 `offset`·`limit` 을 **둘 다 필수**로 받고 기본값이 없다. 빠뜨리면 `9001` 이다.

| 규칙 | 값 |
|---|---|
| `offset` 생성 | `page × limit` (`page` 는 0-based) — **다른 경로로 만들지 않는다** |
| `limit` 변경 | `page` 를 0 으로 되돌린다 |
| 총 페이지 수 | `ceil(totalCount / limit)` |
| 현재 페이지 | `offset / limit` |

`offset` 이 `limit` 의 배수가 아니면 **목록이 통째로 `9001` 로 실패한다.** 그래서 `Paging` 은
`offset` 을 받는 생성자를 두지 않는다 — 배수가 아닌 값이 만들어질 경로 자체가 없다(research 10).

사용자가 임의 `offset` 을 넣을 수 있는 입력을 화면에 두지 않는다.

## 6. 실패를 화면에 알리는 방법 (FR-606·612)

컨트롤러는 **try-catch 를 쓰지 않는다.** `@ControllerAdvice`(`FrontExceptionHandler`)가
두 예외를 받아 화면으로 보낸다.

| 예외 | 화면에 전달되는 것 | 사용자에게 보이는 것 |
|---|---|---|
| `BackendApiException` | `resCode` + `message` | 백엔드가 준 문구. 화면별로 다르게 안내할 코드는 그 화면 스펙이 정한다 |
| `BackendUnavailableException` | **코드 없음** | "서버에 닿지 못했습니다". 목록·상세를 **빈 값으로 그리지 않는다** |

`BackendUnavailableException` 에 코드를 붙이지 않는 이유는 FR-607 이다. `9000`(서버 오류)을
빌려 쓰면 "백엔드가 `9000` 을 줬다"와 "백엔드에 닿지 못했다"가 구분되지 않는다.

**모달에서 난 실패는 부모 페이지로 튕기지 않는다** — 모달을 연 채로 폼 상단에 표시한다.
어느 쪽인지는 요청이 모달 제출인지로 가른다.

## 7. 로그 (원칙 IV · FR-622)

호출 한 건마다 남긴다. AOP 를 쓰지 않는다 — 나가는 지점이 이 클래스 하나뿐이다(research 11).

| 남기는 것 | 예 |
|---|---|
| method · URI | `GET /api/v1/payment-methods` |
| 응답 `resCode` | `200` · `3003` |
| 소요 시간 | `142ms` |
| 실패 시 `message` | 백엔드 문구 |

**남기지 않는 것**: `Authorization` 헤더 값, 로그인·재발급 요청·응답의 토큰 필드,
비밀번호 필드. 마스킹은 값을 자르지 않고 **통째로 가린다**.

## 8. 설정

| 프로퍼티 | 값 | 설명 |
|---|---|---|
| `moneylog.backend.base-url` | `http://localhost:8081/api/v1` | 백엔드 API 기준 주소 |
| `moneylog.backend.connect-timeout` | `3s` | 연결 타임아웃. 초과 시 `BackendUnavailableException` |
| `moneylog.backend.read-timeout` | `10s` | 읽기 타임아웃. 엑셀 업로드(010)가 가장 길다 |

주소를 코드에 적지 않는다 — 배포 환경이 바뀌면 프로퍼티만 고친다.
