# Output Row Guidelines

## D·E·F 계층 예시 (001 — 회원가입 이메일 인증)

| 시험항목(대분류) | 시험항목(중분류) | 시험목적 | 시험절차 | 시험내역 |
|---|---|---|---|---|
| 회원가입 이메일 인증 | 인증 이메일 발송 | 유효한 ID·이메일로 인증 이메일 발송 API 호출 시 발송이 성공(resultcode 200)하는지 확인한다. | 1. 미가입 ID·유효 이메일·flow_token을 준비한다.\n2. 인증 이메일 발송 API(POST …/email/send 등 contracts 기준)를 호출한다.\n3. resultcode 200을 확인한다.\n4. tbl_email_auth_send에 발송 행이 생성되었는지 확인한다. | [요청 예시]\n(contracts/openapi.yaml·quickstart.md 기준 path·body)\n\n[기대 응답 예시]\nresultcode: 200\n\n[DB 확인 예시]\ntbl_email_auth_send: 신규 행, delivery_status 등 |
| 회원가입 이메일 인증 | 인증 이메일 발송 | 이미 사용 중인 ID로 인증 이메일 발송 API 호출 시 resultcode 701로 실패를 확인한다. | 1. 기가입 ID를 준비한다.\n2. 인증 이메일 발송 API를 호출한다.\n3. resultcode 701을 확인한다. | [요청 예시]\n…\n\n[기대 응답 예시]\nresultcode: 701 |
| 회원가입 이메일 인증 | 이메일 인증요청 | 최신 이메일·유효 payload로 이메일 인증요청 API 호출 시 인증 성공(resultcode 200) 및 콜백 status=OK를 확인한다. | 1. 인증 이메일 발송 후 유효 payload를 준비한다.\n2. 이메일 인증요청 API를 호출한다.\n3. resultcode 200·콜백 1회를 확인한다. | … |

→ **E는 그룹명만 반복**, **F에 시나리오·코드**. **FR-/NC 등 Speckit 식별자만 금지** (6.22 등 연동 API 번호는 허용). 상세 표는 `category-grouping.md` 참고.

## D·E·F 계층 예시 (006 — SNS 연동 해제)

| 시험항목(대분류) | 시험항목(중분류) | 시험목적 | 시험절차 | 시험내역 |
|---|---|---|---|---|
| SNS 연동 해제 | 네이버 연동 해제 알림 수신 | 위조 signature로 수신 시 HTTP 401 및 tbl_sns_info 미변경을 확인한다. | 1. sns_type=3 연동 행이 있는 계정을 준비한다.\n2. POST /v1/account/sns/naver/disconnect-receive 를 잘못된 signature로 호출한다.\n3. HTTP 401을 확인한다.\n4. tbl_sns_info 행이 유지되는지 확인한다. | [요청 예시]\nPOST /v1/account/sns/naver/disconnect-receive\n…\n\n[기대 응답 예시]\nHTTP 401\n\n[DB 확인 예시]\ntbl_sns_info: 변경 없음 |
| SNS 연동 해제 | 네이버 연동 해제 알림 수신 | 유효 HMAC으로 수신 시 연동 해제(행 삭제)가 성공하는지 확인한다. | … | … |

실제 작성 시에는 산출물에 존재하는 API path, 응답코드, 테이블명만 사용한다. 시험내역에는 요청·응답·DB 샘플을, 시험절차에는 수행 순서만 적는다. **FR-/NC/Txxx 등 Speckit 전용 식별자는 넣지 않는다.** (6.22·12.3 등 연동 API 번호는 허용)

