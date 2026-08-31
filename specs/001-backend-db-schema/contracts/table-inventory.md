# Contract: 테이블 인벤토리

**재작성**: 2026-08-31

이 기능이 `moneylog` 스키마에 만드는 물리 객체의 확정 목록이다. 구현·검수·`sql/schema-moneylogdb.sql` 대조의 기준이 된다. 컬럼 상세는 [data-model.md](../data-model.md).

## 테이블 15개

| # | 테이블 | PK | 소유자 | 논리 저장 단위 | Spec Entity |
|---|--------|-----|--------|----------------|:-----------:|
| 1 | `tbl_user` | `id_key` | — | 회원 | #1 |
| 2 | `tbl_user_session` | `idx` | `id_key` | 회원 세션 | #2 |
| 3 | `tbl_user_login_history` | `idx` | `id_key` | 로그인 이력 | #3 |
| 4 | `tbl_user_payment_method` | `idx` | `id_key` | 지출·소득 수단 | #4 |
| 5 | `tbl_user_expend_group` | `idx` | `id_key` | 지출유형 | #5 |
| 6 | `tbl_user_expense` | `idx` | `id_key` | 월별 지출 내역 | #6 |
| 7 | `tbl_user_income` | `idx` | `id_key` | 월별 수입 내역 | #7 |
| 8 | `tbl_user_fixed_expense` | `idx` | `id_key` | 고정지출 관리 | #8 |
| 9 | `tbl_user_fixed_expense_monthly` | `idx` | `id_key` | 월별 고정지출 내역 | #9 |
| 10 | `tbl_user_expend_target_default` | `idx` | `id_key` | 기본 목표금액 | #10 |
| 11 | `tbl_user_expend_target_monthly` | `idx` | `id_key` | 월별 목표금액 | #11 |
| 12 | `tbl_user_statistics` | `idx` | `id_key` | 월별 통계 스냅샷 | #12 |
| 13 | `tbl_user_statistics_weekly` | `idx` | `id_key` | 통계 주별 지출 | #13 |
| 14 | `tbl_user_statistics_expend_group` | `idx` | `id_key` | 통계 지출유형별 요약 | #14 |
| 15 | `tbl_user_statistics_payment_method` | `idx` | `id_key` | 통계 수단별 요약 | #15 |

## 시퀀스 1개

| 시퀀스 | 용도 |
|--------|------|
| `seq_installment_group` | `tbl_user_expense.installment_group_id` 발급. 한 할부의 N개 행이 같은 값을 공유 |

각 테이블의 IDENTITY PK가 만드는 시퀀스는 Hibernate/PostgreSQL이 자동 생성하므로 이 목록에 세지 않는다.

## 레거시 비겹침 확인

레거시 `money-app` 엔티티가 쓰는 테이블 이름 9개와 위 15개는 하나도 겹치지 않는다. PostgreSQL은 따옴표 없는 식별자를 소문자로 접으므로 대소문자 차이는 충돌 회피 근거가 되지 않는다 — 아래는 접힌 뒤의 이름으로 비교한 것이다.

| 레거시 이름 (접힘 후) | 이 기능의 대응 테이블 | 충돌 |
|-----------------------|----------------------|:----:|
| `tbl_member` | `tbl_user` | 없음 |
| `tbl_login_history` | `tbl_user_login_history` | 없음 |
| `tbl_payment_method` (원문 `tbl_payment_Method`) | `tbl_user_payment_method` | 없음 |
| `tbl_card` | (대응 없음) | 없음 |
| `tbl_expend` | `tbl_user_expense` | 없음 |
| `tbl_expend_group` | `tbl_user_expend_group` | 없음 |
| `tbl_expend_fix` | `tbl_user_fixed_expense` | 없음 |
| `tbl_ammount` (원문 `tbl_Ammount`) | `tbl_user_expend_target_default` | 없음 |
| `tbl_system_stat` | `tbl_user_statistics` | 없음 |
| `expend_statistics` / `expend_statistics_detail` (`@Table` 없이 기본 명명) | `tbl_user_statistics*` | 없음 |

**검수 방법** — 스키마 반영 후:

```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'moneylog' AND tablename LIKE 'tbl_user%'
ORDER BY tablename;
-- 15행이 나와야 한다
```

## 이 기능이 만들지 않는 것

- 레거시 테이블(위 9개) — 손대지 않는다. `money-app`은 Phase 6에서 API 호출로 전환된다
- 월별 가계부 목록 테이블 — 조회 시 합치는 결과일 뿐
- 할부 그룹 테이블 — 시퀀스 값 공유로 표현
- 고정지출 월별 예외 테이블 — 폐기됨
- 엑셀 업로드 이력 테이블
- 시드 데이터 — 기본 지출유형 10종은 회원마다 생기는 데이터라 덤프에 넣지 않는다
