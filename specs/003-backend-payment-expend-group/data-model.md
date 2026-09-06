# Data Model: 지출·소득 수단과 지출유형 관리

**Feature**: `003-backend-payment-expend-group` | **Date**: 2026-09-02 | **Phase**: 1

**이 기능은 스키마를 바꾸지 않는다.** 테이블·컬럼·CHECK·유니크·인덱스가 `001`에 전부 있다.
이 문서는 **003이 그 구조를 어떻게 쓰는지**를 적는다.

DB 구조의 단일 참조점은 `sql/schema-moneylogdb.sql`이다. 아래 표기는 그 파일에서 확인한 것이다.

---

## 쓰는 테이블

| 테이블 | 이 기능에서 | 쓰는 API |
|---|---|---|
| `tbl_user_payment_method` | 읽기·쓰기 | 2.1~2.6 |
| `tbl_user_expend_group` | 읽기·쓰기 | 2.7~2.13 |
| `tbl_expense` | **읽기만** (참조 검사) | 2.4·2.12 |
| `tbl_income` | **읽기만** (참조 검사) | 2.4 |
| `tbl_fixed_expense` | **읽기만** (참조 검사) | 2.4 |
| `tbl_fixed_expense_monthly` | **읽기만** (참조 검사) | 2.4 |
| 서버 로컬 디스크 | 읽기·쓰기 | 2.7·2.10·2.11 |

뒤의 넷은 `004`·`005`가 소유한다. 003은 **행이 있는지만** 본다(`exists`).

---

## 1. `tbl_user_payment_method` — 지출·소득 수단

Entity: `data-mod/.../entity/UserPaymentMethod.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 003에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | API의 `paymentMethodId` |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 소유자. **토큰이 정하며 요청이 지정할 수 없다**(FR-201) |
| `name` | VARCHAR(50) | NOT NULL | 수단 이름 (예: 국민카드) |
| `type` | VARCHAR(10) | NOT NULL, CHECK `ck_payment_method_type` (`CARD`/`ACCOUNT`) | 값 밖이면 `3001` |
| `purpose` | VARCHAR(10) | NOT NULL, CHECK `ck_payment_method_purpose` (`EXPENSE`/`INCOME`) | 값 밖이면 `3001`. 변경은 참조 0건일 때만 |
| `in_use` | BOOLEAN | NOT NULL, 기본 `true` | 사용 중 목록의 조건 |
| `deleted` | BOOLEAN | NOT NULL, 기본 `false` | 삭제 표시. 물리 삭제하지 않는다 |
| `card_expiry` | CHAR(7) | NULL | `YYYY-MM`. `type=ACCOUNT`면 비어야 한다 |

**INDEX**: `ix_user_payment_method_active (id_key, purpose, in_use, deleted)` —
사용 중 목록(2.6)의 네 조건과 컬럼 순서가 일치한다.

`card_expiry`가 `CHAR(7)`인 점에 주의한다. 고정 길이라 짧은 값을 넣으면 **공백으로 패딩**된다.
`YYYY-MM`이 정확히 7자라 정상 입력에서는 문제가 없지만, 형식 검증(`3002`)을 통과시킨 뒤에만
저장해야 한다.

### 애플리케이션이 지켜야 하는 것 (DB가 막아주지 않는다)

| 규칙 | 코드 | 왜 DB가 못 막나 |
|---|---|---|
| `type=ACCOUNT`이면 `card_expiry`가 비어야 한다(FR-204) | — | 두 컬럼을 함께 보는 CHECK이 없다 |
| `card_expiry`가 `YYYY-MM` 형식(FR-221) | `3002` | `CHAR(7)`은 길이만 강제한다 |
| `purpose` 변경은 참조 0건일 때만(FR-205) | `3005` | 삭제가 아니라 UPDATE라 FK가 발동하지 않는다 |
| 이미 삭제 표시된 것의 재삭제(FR-206 계열) | `3004` | `deleted`를 다시 `true`로 쓰는 것은 유효한 UPDATE다 |
| 본인 소유만 접근(FR-201) | `3003` | FK는 소유자를 강제하지만 **다른 사람의 행을 읽는 것**은 막지 않는다 |

### 상태 전이

```text
        등록(2.1)
            │
            ▼
   ┌─────────────────┐   수정(2.4) in_use=false   ┌─────────────────┐
   │   사용 중        │ ─────────────────────────▶ │   사용 안 함      │
   │ in_use=true     │ ◀───────────────────────── │ in_use=false    │
   │ deleted=false   │   수정(2.4) in_use=true    │ deleted=false   │
   └────────┬────────┘                            └────────┬────────┘
            │                                              │
            │           삭제(2.5) — 어느 쪽에서든            │
            └──────────────────┬───────────────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │     삭제 표시        │  행은 남는다
                    │   deleted=true      │  수정은 여전히 가능(이름 정리 등)
                    └─────────────────────┘  재삭제는 3004
```

**삭제 표시 상태에서도 수정은 가능하다**(스펙 Edge Case). 되돌리는 전이(`deleted=false`)는
API가 없다 — 설계 명세에 복구 API가 없기 때문이다.

### 목록 두 종류의 차이 (FR-207)

| API | 조건 | 삭제분 |
|---|---|---|
| 2.2 관리 목록 | `id_key`만 | **포함**. `deleted` 필드로 구분 |
| 2.6 사용 중 목록 | `id_key` + `purpose` 일치 + `in_use=true` + `deleted=false` | 제외 |

SC-202가 "용도·사용 여부·삭제 표시를 달리한 수단 4건 중 사용 중 목록이 돌려주는 것은
**정확히 1건**"을 요구한다 — 네 조건이 전부 걸리는지 확인하는 시나리오다.

---

## 2. `tbl_user_expend_group` — 지출유형

Entity: `data-mod/.../entity/UserExpendGroup.java` · 기본키 `idx`

| 컬럼 | 타입 | 제약 | 003에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | API의 `expendGroupId`. **아이콘 파일명에도 들어간다** |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 소유자 |
| `name` | VARCHAR(30) | NOT NULL, **UNIQUE `(id_key, name)`** | 회원 안에서 유일. 부분 조건 없음 |
| `in_use` | BOOLEAN | NOT NULL, 기본 `true` | 사용 중 목록의 조건 |
| `default_group` | BOOLEAN | NOT NULL, 기본 `false` | 가입이 만든 10종. 이름 변경·삭제 불가 |
| `deleted` | BOOLEAN | NOT NULL, 기본 `false` | 삭제 표시 |
| `icon_filename` | VARCHAR(255) | NULL | 파일명만. 경로·Base URL은 저장하지 않는다 |

**UNIQUE**: `ux_user_expend_group_name (id_key, name)` — **`WHERE` 조건이 없다.**
삭제 표시된 행의 이름도 계속 점유한다(FR-209). 이것이 US3 시나리오 3("삭제한 이름을 다시
쓰면 `3101`")의 DB 근거다.

**INDEX**: `ix_user_expend_group_active (id_key, in_use, deleted)` —
`purpose`가 없다. 지출유형에 용도 구분이 없다는 사실이 DB에 드러나 있다.

### 애플리케이션이 지켜야 하는 것

| 규칙 | 코드 | 왜 DB가 못 막나 |
|---|---|---|
| 그 유형을 쓴 지출이 있으면 삭제 불가(FR-210) | `3106` | 삭제가 UPDATE라 FK RESTRICT가 발동하지 않는다 |
| 기본 유형은 삭제 불가(FR-210) | `3107` | `default_group`을 보는 CHECK이 없다 |
| 기본 유형은 이름 변경 불가(FR-220) | `3105` | 위와 같음 |
| 재삭제(FR-222) | `3108` | `deleted`를 다시 `true`로 쓰는 것은 유효한 UPDATE다 |
| 본인 소유만 접근(FR-201) | `3103` | 위 수단과 같음 |
| 이름 중복(FR-209) | `3101` | **DB가 막는다.** 선검사 + 위반 처리 양쪽을 둔다 |

마지막 줄만 DB가 막아준다. 나머지 다섯은 전부 서비스 코드의 책임이다.

### 상태 전이

수단과 같은 모양이되 **삭제 진입에 두 개의 문턱**이 있다.

```text
   ┌─────────────────┐
   │   사용 중        │  ◀──▶  사용 안 함 (in_use 토글, 2.11)
   │ in_use=true     │
   │ deleted=false   │
   └────────┬────────┘
            │  삭제(2.12)
            │    ├─ default_group=true      → 3107 (막힌다)
            │    ├─ 그 유형을 쓴 지출 있음    → 3106 (막힌다)
            │    └─ 이미 deleted=true       → 3108 (막힌다)
            ▼
   ┌─────────────────┐
   │     삭제 표시    │  행·목표금액·통계 참조 유지(FR-211)
   │  deleted=true   │  아이콘 파일도 디스크에 남는다(FR-215)
   └─────────────────┘  이름은 계속 점유한다(FR-209)
```

`in_use=false`로 바꾸면 **`006`의 목표금액 API가 `3601`로 거절**한다(FR-212).
003은 그 조건을 만들기만 하고 코드는 006이 소유한다.

---

## 3. 아이콘 파일 (DB 아님)

DB에는 `icon_filename` 문자열만 있고 실체는 서버 로컬 디스크에 있다.

| 항목 | 값 |
|---|---|
| 파일명 | `{id_key}_{expendGroupId}.{확장자}` (FR-224) |
| 확장자 | `png` · `jpg` · `gif` — **업로드 파일의 실제 형식**에서 정한다 |
| 최대 크기 | 1MB (FR-219) |
| 저장 위치 | `icon.storage.dir` 설정값. 클래스패스 밖 |
| 시드 원본 | `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/` (10개, 읽기 전용) |

**파일명에 유형 이름이 들어가지 않는다.** 파일명이 조회 URL 경로에 그대로 실리므로
이름에 `/`·`..`이 있으면 경로를 벗어나고 공백·유니코드는 인코딩이 어긋난다(FR-224).
ID 기반이라 이름을 바꿔도 파일명이 그대로 유효하다(FR-214를 구조로 보장).

**생성 순서가 강제된다** — `expendGroupId`가 파일명에 들어가므로:

```text
① 행 INSERT (PK 획득)  →  ② 커밋  →  ③ 파일 저장  →  ④ icon_filename UPDATE
```

②를 ③보다 앞에 두는 이유는 [research.md §2](./research.md)에 있다. 요약하면
파일 먼저 쓰면 롤백 시 **회수 불가능한 쓰레기 파일**이 남고, DB 먼저 쓰면 실패 모드가
"아이콘 없는 유형"이라 **정상 상태로 흡수된다**.

**삭제해도 파일은 남긴다**(FR-215). 삭제 표시가 행을 보존하므로 파일을 지우면
"행은 있는데 파일이 없는" 상태가 되고, 2.10이 `3104`를 내게 된다.

---

## 4. 이 기능이 만들지 않는 저장 단위

| 개념 | 왜 만들지 않나 |
|---|---|
| 아이콘 메타 테이블 | 파일명 하나면 충분하다. 크기·형식은 파일 자체가 갖고 있다 |
| 수단 종류 코드 테이블 | `type`·`purpose` 값이 각각 2개뿐이고 CHECK이 강제한다 |
| 삭제 이력 | 삭제 표시가 곧 이력이다. 행이 남는다 |
| 기본 지출유형 템플릿 테이블 | 10종은 `002`의 가입 흐름이 코드로 만든다. 회원마다 생기는 데이터라 시드에 넣지 않는다(001의 결정) |
| 유형 순서 컬럼 | 설계 명세에 정렬 요구가 없다. 필요해지면 별도로 다룬다 |
