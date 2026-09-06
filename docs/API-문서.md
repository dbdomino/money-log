# API 문서 — 어디를 보고, 어떻게 여는가

백엔드 API 를 확인하는 방법이 **둘**이다. 역할이 다르니 목적에 맞는 쪽을 연다.

| | Swagger UI | `openapi.yaml` |
|---|---|---|
| 위치 | 실행 중인 앱 (`:8081`) | `app-mod/money-backend-app/openapi.yaml` |
| 만드는 주체 | **코드** — springdoc 이 컨트롤러·DTO 에서 읽어 만든다 | **사람** |
| 담는 것 | 지금 코드가 실제로 하는 것 | 의도·계약 — 실패 코드, 판정 순서, 그렇게 정한 이유 |
| 쓸 때 | 직접 호출해 보며 확인·디버깅 | 리뷰, 프론트 연동 규격 합의, 설계 근거 확인 |
| 앱 실행 | 필요하다 | 필요 없다 |

**둘 다 필요하다.** Swagger 는 "지금 무엇이 있는가"에 답하지만 "`3003` 과 `3401` 중 무엇이
언제 나오는가"에는 답하지 못한다 — 그건 코드에서 자동으로 읽히지 않는다. 반대로
`openapi.yaml` 은 사람이 쓰므로 방치하면 낡는다.

> 낡지 않게 하는 장치가 있다. `OpenApiDocumentIT.contractFileCoversEveryOperation` 이
> **두 문서의 (메서드, 경로) 집합을 대조**한다. API 를 추가하고 `openapi.yaml` 을 잊으면
> 테스트가 깨진다. 반대로 지운 API 가 문서에 남아 있어도 깨진다.

---

## Swagger UI 열기

### 1. 백엔드를 띄운다

```bash
./gradlew :app-mod:money-backend-app:bootRun
```

**환경변수 두 개가 필요하다.** 없으면 기동이 실패한다 — 기본값을 두지 않은 것이 의도다.

| 변수 | 뜻 | 비고 |
|---|---|---|
| `JWT_SECRET` | JWT 서명 키 | HS256 이라 **32바이트 이상**. 짧으면 기동 시점에 걸린다 |
| `ICON_STORAGE_DIR` | 지출유형 아이콘을 두는 디렉터리 | 클래스패스 안(`resources/`)을 가리키면 안 된다 — 배포하면 jar 내부가 되어 쓸 수 없다 |

```bash
# PowerShell
$env:JWT_SECRET = "로컬개발용_최소32바이트_시크릿_문자열_예시"
$env:ICON_STORAGE_DIR = "C:\source-project\a_project\money-log\.local\icons"
./gradlew :app-mod:money-backend-app:bootRun
```

PostgreSQL(`moneylogdb`, 스키마 `moneylog`)이 떠 있어야 한다.
**`ddl-auto` 가 `create` 라 기동할 때마다 전 테이블이 drop 후 재생성된다** — 남겨야 할
개발 데이터가 있으면 `data-mod/src/main/resources/application-postgresql.yml` 에서
`update` 로 돌린다.

### 2. 브라우저로 연다

| 무엇 | 주소 |
|---|---|
| **Swagger UI** | <http://localhost:8081/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8081/v3/api-docs> |
| OpenAPI YAML | <http://localhost:8081/v3/api-docs.yaml> |

문서 경로는 **인증 없이** 열린다. 토큰을 얻는 법을 보려고 문서를 여는 첫 사용자가
막히면 안 되기 때문이다 — 로그인 API 가 바로 그 문서 안에 있다.

### 3. 토큰을 넣는다

1. `POST /api/v1/auth/signup` 으로 가입한다. **기본 지출유형 10종이 함께 만들어진다.**
2. `POST /api/v1/auth/login` 으로 `accessToken` 을 받는다.
3. 오른쪽 위 **Authorize** 를 눌러 그 값을 붙여 넣는다. `Bearer ` 접두사는 UI 가 붙인다.
4. 이후 호출에 `Authorization` 헤더가 자동으로 붙는다.

**문서를 여는 것과 API 를 호출하는 것은 다르다.** Swagger UI 에서 쏘는 요청도 평소와 같은
보안 필터를 지난다 — UI 를 거친다고 인가가 느슨해지지 않는다.

---

## 읽을 때 반드시 알아야 하는 것

### 실패도 HTTP 200 이다

성공·실패가 모두 `{ "resCode": <숫자>, "data": { ... } }` 한 형태다(헌장 원칙 III).

```json
{ "resCode": 200,  "data": { "expenseId": 12 } }
{ "resCode": 3003, "data": { "message": "결제수단을 찾을 수 없습니다." } }
```

**Swagger UI 의 "200 OK" 를 성공으로 읽으면 안 된다.** 본문의 `resCode` 를 봐야 한다.
그래서 각 API 의 `200` 응답 설명에 나올 수 있는 실패 코드를 함께 적어 두었다.

### 래퍼를 쓰지 않는 API 가 둘 있다

| API | 어떻게 다른가 |
|---|---|
| `GET /api/v1/expend-groups/icons/{filename}` | **성공도 실패도** 래퍼가 아니다. `<img src>` 로 받는 이미지라 실패는 본문 없는 HTTP 상태코드다 |
| `GET /api/v1/expense-incomes/excel/template` | **성공만** 예외(`.xlsx` 바이너리). 실패는 래퍼 + HTTP 200 이다 — 사용자가 다운로드 버튼을 누르는 흐름이라 사유를 화면에 띄워야 한다 |

둘의 차이가 헷갈리기 쉬운 지점이다. 사용 흐름이 달라서 갈렸다.

### PATCH 는 omit = 유지다

보내지 않은 필드는 그대로 두고 보낸 필드만 바꾼다. **`PUT` 은 쓰지 않는다.**

지출·소득(3.3·3.9)에는 규칙이 하나 더 있다 — **이름 스냅샷은 참조가 실제로 바뀔 때만
갱신한다.** 같은 수단을 유지한 채 금액만 고쳤는데 이름이 조용히 바뀌면 안 되기 때문이다.

### "없음"과 "타인 소유"가 같은 코드다

`3003`(수단) · `3103`(지출유형) · `3202`(지출) · `3302`(소득)가 그렇다. 나누면 ID 를 훑는
것만으로 남의 데이터가 존재한다는 사실이 새어 나간다.

---

## 에러코드 대역

| 대역 | 자원 | 정의 위치 |
|---|---|---|
| `1001`~`1006` | 인증·인가·세션 | `common-mod/.../error/ErrorCode.java` |
| `2001`~`2005` | 회원 | 〃 |
| `30xx` | 지출 수단 | 〃 |
| `31xx` | 지출유형·아이콘 | 〃 |
| `32xx` | 지출·할부 | 〃 |
| `33xx` | 소득 | 〃 |
| `34xx` | 고정지출 | 〃 (005 에서 사용) |
| `35xx` | 월별 가계부·엑셀 | 〃 |
| `36xx` | 목표금액·통계 | 〃 (006 에서 사용) |
| `9000`·`9001` | 서버 오류·잘못된 요청 | 〃 |

**같은 뜻인데 코드가 다른 자리가 있다.** 연·월 범위 오류가 4.5·4.6·4.9 는 `3403`,
4.8 은 `3501`, 006 의 통계는 `3603` 이다. 실수가 아니라 **자원별 대역 배정의 결과**이며
통일하면 규칙이 깨진다.

`3404` 는 **결번**이다. 폐기된 구 `FixedExpenseMonthlyOverrideUpsert` 의 코드이므로
되살려 쓰지 않는다.

---

## 지금 문서에 있는 것과 없는 것

| 기능 | 상태 | API |
|---|---|---|
| 002 회원·인증 | ✅ 구현됨 | 16건 |
| 003 수단·지출유형 | ✅ 구현됨 | 13건 |
| 004 지출·소득·할부·엑셀 | ✅ 구현됨 | 12건 |
| **005 고정지출·가계부** | ✅ 구현됨 | 9건 (4.1~4.9) |
| 006 목표금액·통계 | ⏳ 미착수 | — |

합계 **50개 오퍼레이션 / 34개 경로**(헬스체크 포함).

006 이 붙으면 `openapi.yaml` 과 `OpenApiDocumentIT.REPRESENTATIVE_PATHS` 를 함께 늘린다. `openapi.yaml` 을 잊으면 `OpenApiDocumentIT` 가 먼저 깨진다.

---

## 운영에서는 끈다

Swagger 는 스키마 전체를 그대로 드러낸다. 배포 프로필에서 아래를 덮어쓴다.

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

**경로를 `SecurityConfig` 에서 지우는 방식이 아니다.** 위 설정이면 경로 자체가 404 가 되므로
`permitAll` 이 남아 있어도 노출되지 않는다. 보안 설정과 노출 스위치를 한 곳에 묶어 두면
한쪽만 고쳤을 때 열린 채로 남는다.
