# Data Model: 지출·소득 등록과 할부·엑셀 일괄 등록

**Feature**: `004-backend-expense-income` | **Date**: 2026-09-02 | **Phase**: 1

**이 기능은 스키마를 바꾸지 않는다.** 테이블·컬럼·CHECK·인덱스·시퀀스가 `001`에 전부 있다.
이 문서는 **004가 그 구조를 어떻게 쓰는지**를 적는다.

단일 참조점은 `sql/schema-moneylogdb.sql`이다. 아래 표기는 그 파일에서 확인한 것이다.

---

## 쓰는 테이블

| 대상 | 이 기능에서 | 쓰는 API |
|---|---|---|
| `tbl_expense` | 읽기·쓰기 | 3.1~3.6 · 3.12 |
| `tbl_income` | 읽기·쓰기 | 3.7~3.10 · 3.12 |
| `tbl_user_payment_method` | **읽기만** (검증 + 이름 스냅샷) | 3.1·3.3·3.7·3.9·3.11·3.12 |
| `tbl_user_expend_group` | **읽기만** (검증 + 이름 스냅샷) | 3.1·3.3·3.11·3.12 |
| `seq_installment_group` | `nextval` | 3.5 |

`tbl_fixed_expense`는 **건드리지 않는다**(FR-323). 고정지출은 `005`가 소유한다.

---

## 0. 003과 반대인 것 — 삭제와 이름

구현자가 가장 혼동하기 쉬운 지점이라 먼저 둔다.

| | `003` 수단·지출유형 | `004` 지출·소득 |
|---|---|---|
| 삭제 | **삭제 표시** (`deleted=true`, 행 보존) | **물리 삭제** (`DELETE`) |
| `deleted` 컬럼 | 있다 | **없다** |
| 이름 | 원본 하나. 스냅샷 없음 | **참조 + 스냅샷 둘 다** |
| 이름 변경의 영향 | 그 자원의 현재 이름이 바뀐다 | **과거 행은 바뀌지 않는다** |

이유는 성격이 다르기 때문이다. 수단·유형은 **과거 기록이 참조하는 대상**이라 지우면
참조가 끊긴다. 지출·소득은 **기록 자체**라, 지운다는 것은 "없었던 일로 한다"는 뜻이고
남겨 두면 합계가 틀린다.

---

## 1. `tbl_expense` — 월별 지출 내역

Entity: `data-mod/.../entity/UserExpense.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 004에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | API의 `expenseId` |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 소유자. **토큰이 정한다** |
| `payment_method_idx` | BIGINT | NOT NULL, FK | 수단 **참조** |
| `payment_method_name` | VARCHAR(50) | NOT NULL | 수단 이름 **스냅샷** |
| `expend_group_idx` | BIGINT | NOT NULL, FK | 지출유형 **참조** |
| `expend_group_name` | VARCHAR(30) | NOT NULL | 지출유형 이름 **스냅샷** |
| `amount` | BIGINT | NOT NULL, CHECK `ck_expense_amount` (> 0) | 원 단위 정수 |
| `payment_date` | DATE | NOT NULL | 결제일 |
| `place` | VARCHAR(100) | NOT NULL | 장소. **필수**다 |
| `content` | VARCHAR(255) | NOT NULL | 내용. **필수**다 |
| `installment_group_id` | BIGINT | NULL | 할부 그룹. 시퀀스가 발급 |
| `installment_index` | INT | NULL, CHECK (NULL 또는 ≥ 1) | 회차 |
| `installment_total` | INT | NULL, CHECK (NULL 또는 ≥ 2) | 총 개월 |

**INDEX**: `ix_expense_date (id_key, payment_date)` · `ix_expense_installment (installment_group_id, payment_date)`

두 번째 인덱스가 중도상환(3.6)의 조회 조건과 컬럼 순서까지 일치한다 —
001이 이 연산을 예상하고 만들었다.

### 참조와 스냅샷이 둘 다 있는 이유

| 컬럼 | 답하는 질문 |
|---|---|
| `payment_method_idx` | "지금 이 수단은 무엇인가" — 수정 시 원본을 찾고, 통계가 수단별로 묶는다 |
| `payment_method_name` | "**등록 당시** 이 수단은 뭐라고 불렸나" — 과거 화면을 그때 모습으로 복원한다 |

수단 이름을 바꿔도 과거 지출은 옛 이름으로 남아야 하므로(FR-302) 둘 다 필요하다.

### 스냅샷 갱신 조건 (FR-304) — 세 갈래

| 상황 | 스냅샷 |
|---|---|
| 등록 (3.1·3.5·3.12) | 그 시점 원본 이름을 읽어 저장 |
| 수정에서 참조를 **바꿈** | 새 참조의 **현재** 이름으로 갱신 |
| 수정에서 참조를 omit하거나 **같은 값** | **건드리지 않는다** |
| `003`에서 원본 이름만 바뀜 | 과거 행 그대로 |

세 번째가 놓치기 쉽다. "수정 요청이 왔으니 최신화한다"고 구현하면 같은 수단을 유지한 채
금액만 고쳤을 때 이름이 조용히 바뀐다. **참조가 실제로 바뀌었는가**를 비교해야 한다.

### 할부 3개 컬럼의 불변식

```text
일시불:  installment_group_id = NULL
        installment_index    = NULL
        installment_total    = NULL

할부:    installment_group_id = 시퀀스 값 (그룹 내 N개 행이 공유)
        installment_index    = 1 .. N
        installment_total    = N  (N >= 2)
```

**셋 다 비거나 셋 다 채워져야 한다.** DB는 이걸 막지 않는다 —
CHECK 2건이 각 컬럼의 **범위만** 보고 세 컬럼의 동시성은 검사하지 않는다.

`installment_index`만 채운 행이 생기면 "일시불인가 할부인가"를 판정할 수 없고,
조회·집계·중도상환이 전부 그 판정에 의존하므로 조용히 틀린 답이 나온다.
저장 진입점 셋(3.1 · 3.5 · 3.12)에서 애플리케이션이 강제한다.

### 애플리케이션이 지켜야 하는 것 (DB가 막아주지 않는다)

| 규칙 | 코드 | 왜 DB가 못 막나 |
|---|---|---|
| 할부 3컬럼 동시성 | `3201`·`3204` | CHECK이 컬럼별 범위만 본다 |
| `place` 100자·`content` 255자 초과 | `3201` | 초과하면 DB 오류(`9000`)로 새어 나간다. 먼저 잡는다 |
| 금액이 소수점·문자·범위 초과 | `3201` | `BIGINT` 파싱 실패는 업무 코드가 아니다 |
| 본인 소유만 접근 | `3202` | FK는 소유자를 강제하지만 **남의 행 읽기**를 막지 않는다 |
| 새 참조는 사용 중이어야 | `3003`·`3103` | FK는 존재만 본다. `in_use`·`deleted`를 모른다 |
| 할부 개월·시작 연월 수정 불가 | `3203` | 컬럼 UPDATE는 유효하다 |

---

## 2. `tbl_income` — 월별 수입 내역

Entity: `data-mod/.../entity/UserIncome.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 004에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | API의 `incomeId` |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 소유자 |
| `payment_method_idx` | BIGINT | NOT NULL, FK | 수단 참조 |
| `payment_method_name` | VARCHAR(50) | NOT NULL | 수단 이름 스냅샷 |
| `amount` | BIGINT | NOT NULL, CHECK `ck_income_amount` (> 0) | 원 단위 정수 |
| `payment_date` | DATE | NOT NULL | **입금일**. 응답 필드도 `paymentDate`다 |
| `content` | VARCHAR(255) | **NULL 허용** | 내용. 비어 있을 수 있다(FR-307) |

**INDEX**: `ix_income_date (id_key, payment_date)`

### 지출과 다른 점

| 항목 | 지출 | 소득 |
|---|---|---|
| 지출유형 | 참조 + 스냅샷 | **없다** |
| 장소 `place` | NOT NULL | **없다** |
| 할부 3컬럼 | 있다 | **없다** |
| `content` | NOT NULL | **NULL 허용** |

**컬럼이 아예 없는 것**이지 비워 두는 것이 아니다(FR-306). 소득 등록 Body에
`place`·`expendGroupId`·할부 필드를 실어 보내도 저장되지 않고 응답에도 나타나지 않는다.

지출과 구조가 달라 별도 자원으로 다룬다 — API도 3.1~3.6(지출)과 3.7~3.10(소득)으로 갈린다.

---

## 3. `seq_installment_group` — 할부 그룹 식별자

```sql
CREATE SEQUENCE moneylog.seq_installment_group
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
```

**그룹당 한 번** 뽑아 N개 행이 공유한다(FR-312). 행마다 새로 발급되면 그룹이 흩어진다.

`installment_group_id`가 `tbl_expense`의 **PK가 아니라 일반 컬럼**이라 JPA의
`@GeneratedValue`로 쓸 수 없다. 001이 이 시퀀스를 `AdditionalMappingContributor`로
따로 만든 이유가 그것이다 — "`@SequenceGenerator`는 어느 Entity의 식별자 생성기로 쓰일 때만
DDL에 나오는데 이 시퀀스는 어느 PK의 생성기도 아니다".

서비스가 명시적으로 `nextval`을 조회한다.

---

## 4. 할부의 생명주기

```text
등록(3.5) — startYearMonth=2026-07, months=12, 한 트랜잭션
    │
    │  seq_installment_group.nextval → 예: 5
    ▼
┌──────────────────────────────────────────────────────┐
│ 12개 행. installment_group_id = 5 공유                │
│ index 1..12, total 12                                 │
│ payment_date = 2026-07-01, 2026-08-01, … 2027-06-01  │  ← 매월 1일 (FR-324)
└──────────────────────────────────────────────────────┘
    │
    ├─ 단건 수정(3.3) ── 그 달 1건만. 개월·시작연월 변경은 3203
    ├─ 단건 삭제(3.4) ── 그 달 1건만 물리 삭제
    ├─ 단건 조회(3.2) ── 일시불과 같은 API
    │
    └─ 중도상환(3.6) ─ PATCH
           payment_date >  today  → 물리 삭제
           payment_date <= today  → 남긴다
           대상 0건               → 3207
```

**결제일이 매월 1일**인 이유: 요청이 `startYearMonth`(`YYYY-MM`)만 받고 일(day)을 받지 않으며,
`tbl_user_payment_method`에 결제일 컬럼이 없다(덤프 확인). 1일로 고정하면 말일 보정이
필요 없다 — 31일 시작 할부의 2월 회차 문제가 생기지 않는다.

**중도상환 경계가 `>`인 이유**: `>=`로 잡으면 오늘 결제된 회차까지 사라져 **이번 달 합계가
소급해 줄어든다.** 사용자가 이미 본 숫자가 바뀐다.
"미결제"를 기준으로 쓰지 않는 것은 `tbl_expense`에 결제 완료 여부 컬럼이 없기 때문이다.

**전체 롤백**(FR-310): 12개 중 1건이라도 실패하면 한 행도 남기지 않는다(`3205`).
부분 생성되면 사용자가 재등록할 때 앞부분이 중복된다 — 004는 중복을 허용하므로(FR-309)
DB가 막아주지 않는다.

---

## 5. 참조 검증의 비대칭 (FR-325 ↔ FR-326)

| 경로 | 참조 대상이 `in_use=false`·`deleted=true`이면 |
|---|---|
| 새로 참조를 건다 (등록, 또는 수정에서 참조 변경) | **거절.** 수단 `3003`, 지출유형 `3103` |
| 이미 저장된 행을 조회·수정·삭제 | **정상 동작.** 참조가 죽어도 막지 않는다 |

이 비대칭이 없으면 003의 삭제 표시가 004를 망가뜨린다. 수단을 삭제 표시하는 순간
그 수단으로 적은 과거 지출 전부가 조회 불가가 되고, 003이 물리 삭제 대신 삭제 표시를
고른 이유("과거 기록 보존")가 무너진다.

**함정은 수정 경로다.** 금액만 고치는 수정에서도 참조 검증을 돌리면 죽은 수단을 쓰던
과거 지출을 못 고친다. **참조를 실제로 바꿀 때만** 검증한다 — §1의 스냅샷 갱신 조건과
같은 판정이므로 `ReferenceResolver` 한 곳에 둔다.

---

## 6. 이 기능이 만들지 않는 저장 단위

| 개념 | 왜 만들지 않나 |
|---|---|
| 고정지출 행 | `005`가 소유한다(FR-323). 004는 건드리지 않는다 |
| 엑셀 업로드 이력 | FR-321. 업로드는 지출·소득 행만 만든다 |
| 삭제 이력·휴지통 | 물리 삭제다(FR-308). 001이 스키마로 확정했다 |
| 할부 그룹 테이블 | 그룹은 `installment_group_id` 값으로만 존재한다. 그룹 자체의 속성이 없다 |
| 결제 완료 여부 컬럼 | 중도상환을 날짜로 판정한다(§4). 컬럼을 추가하면 스키마 변경이다 |
| 업무 유일 제약 | FR-309 — 같은 날짜·금액·수단의 지출을 여러 건 등록할 수 있어야 한다 |
