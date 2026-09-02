# Data Model: 회원 가입·인증·토큰·관리자 회원 관리

**Feature**: `002-backend-member-auth` | **Date**: 2026-09-02 | **Phase**: 1

**이 기능은 스키마를 바꾸지 않는다.** 테이블·컬럼·제약은 `001-backend-db-schema`가 만들었고
`success` 컬럼은 커밋 `1edf308`에서 반영·덤프됐다. 이 문서는 **002가 그 구조를 어떻게 쓰는지**를
적는다 — 어느 컬럼을 언제 쓰고, 어떤 상태 전이가 있고, 무엇을 애플리케이션이 지켜야 하는지다.

DB 구조의 단일 참조점은 `sql/schema-moneylogdb.sql`이다. 아래 컬럼 표기는 그 파일과
`data-mod`의 Entity에서 확인한 것이다.

---

## 쓰는 테이블

| # | 테이블 | 이 기능에서 | 쓰는 API |
|---|---|---|---|
| 1 | `tbl_user` | 읽기·쓰기 | 1.2·1.3·1.7·1.8·1.9~1.16 |
| 2 | `tbl_user_session` | 읽기·쓰기 | 1.3~1.7·1.11·1.16 |
| 3 | `tbl_user_login_history` | 쓰기만 | 1.3 |
| 4 | `tbl_user_expend_group` | **쓰기만**(FR-106) | 1.2·1.12 |

4번은 `003`이 소유한 테이블이다. 002는 가입·관리자 추가 시점에 기본 10종을 만들기만 하고
조회·수정·삭제는 하지 않는다. 그 이유는 [research.md §11](./research.md)에 있다.

---

## 1. `tbl_user` — 회원

Entity: `data-mod/.../entity/User.java` · 기본키 **`id_key`**(이 테이블만 `idx`가 아니다)

| 컬럼 | 타입 | 제약 | 002에서의 쓰임 |
|---|---|---|---|
| `id_key` | BIGINT | PK, IDENTITY | 토큰 `sub`의 환산 대상. 자식 테이블이 참조하는 유일한 소유자 키 |
| `user_id` | VARCHAR(20) | NOT NULL, UNIQUE, `updatable=false` | API가 주고받는 `memberId`. 가입 후 변경 불가 |
| `pw` | VARCHAR(100) | NOT NULL | bcrypt 해시. **어떤 응답에도 싣지 않는다**(SC-107) |
| `nickname` | VARCHAR(20) | NOT NULL | 2~20자. 비밀번호 찾기(1.10·1.11)의 본인 확인 수단 |
| `email` | VARCHAR(100) | NULL, 부분 유니크 `ux_user_email` | 아이디 찾기(1.9)의 본인 확인 수단. **값이 있을 때만** 유일 |
| `phone` | VARCHAR(20) | NULL | 하이픈 없이 숫자만 저장(FR-107) |
| `intro` | VARCHAR(500) | NULL | 자기소개 |
| `role` | SMALLINT | NOT NULL, CHECK `ck_user_role` (1 또는 3) | 가입은 `3` 고정. 관리자 API의 인가 기준 |
| `active` | BOOLEAN | NOT NULL, 기본 `true` | `false`면 로그인·검증·갱신 전부 `1004` |
| `created_by`·`updated_by` | BIGINT | **NULL 허용** | 본인 가입·본인 수정은 `null`, 관리자 조작은 관리자 `id_key` |

**`tbl_user`만 감사 컬럼 2개가 nullable이다.** 가입은 자기 자신을 만드는 행위라 INSERT 시점에
자기 `id_key`가 없다. 이 예외 덕분에 FR-121("본인 가입·본인 수정은 `null`")이 성립한다.

### 애플리케이션이 지켜야 하는 것 (DB가 막아주지 않는다)

| 규칙 | 근거 | 왜 DB가 못 막나 |
|---|---|---|
| `user_id` 4~20자 영문·숫자·`_` | FR-102 | 길이만 컬럼 제약이고 문자 범위는 없다 |
| `nickname` 2~20자 | FR-102 | 최소 길이 제약이 없다 |
| `pw` 8자 이상·3종류 이상 | FR-103 | 저장되는 값은 해시라 원문 규칙을 DB가 볼 수 없다 |
| `pw`는 bcrypt 해시만 | FR-104 | 컬럼은 문자열일 뿐이다 |
| 가입 시 `role=3` 고정 | FR-105 | CHECK은 1과 3을 둘 다 허용한다 |
| `phone`은 숫자만 | FR-107 | 형식 제약이 없다 |

`user_id` 유일성과 `email` 부분 유일성만 DB가 강제한다. 즉 `2002`(아이디 중복)·`2003`(이메일 중복)은
애플리케이션 선검사 + 유니크 위반 처리 **양쪽**을 둔다 — 선검사만으로는 동시 가입 창을 닫지 못한다.

---

## 2. `tbl_user_session` — 회원 세션

Entity: `data-mod/.../entity/UserSession.java` · 기본키 **`idx`**

| 컬럼 | 타입 | 제약 | 002에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | 대리키. 외부에 노출하지 않는다 |
| `session_id` | UUID | NOT NULL, UNIQUE `ux_user_session_id` | JWT 클레임 `sid`. **PK가 아니다** |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 세션 주인 |
| `access_token_hash` | VARCHAR(100) | NULL 허용 | SHA-256 hex 64자. **폐기 시 `NULL`** |
| `refresh_token_hash` | VARCHAR(100) | NULL 허용 | 위와 같음 |
| `access_expires_at` | TIMESTAMPTZ | NOT NULL | JWT `exp`와 **별개로** DB에서도 검사한다 |
| `refresh_expires_at` | TIMESTAMPTZ | NOT NULL | 갱신 가능 기한 |
| `revoked` | BOOLEAN | NOT NULL, 기본 `false` | 부분 유니크 인덱스의 조건 컬럼 |
| `last_accessed_at` | TIMESTAMPTZ | NULL | 선택. 검증 10단계 |

**부분 유니크 인덱스 `ux_user_session_active`** — `(id_key) WHERE NOT revoked`.
회원당 폐기되지 않은 세션이 1건임을 DB가 강제한다. 애플리케이션 검사는 이 인덱스를
대신하지 못한다([research.md §10](./research.md)).

### 상태 전이

```text
           로그인(1.3)                     갱신(1.5, Rotation)
                │                                  │
                ▼                                  │
        ┌───────────────┐                          │
        │    ACTIVE     │◀─────────────────────────┘
        │ revoked=false │   같은 행의 해시·만료 시각만 교체
        │ 해시 2개 있음  │   session_id 는 그대로
        └───────┬───────┘
                │
                │  ① 로그아웃(1.6)
                │  ② 같은 회원 재로그인(1.3) — 새 행을 만들기 전에
                │  ③ 비밀번호 재설정(1.11)
                │  ④ 관리자 정지(1.16)
                ▼
        ┌───────────────┐
        │    REVOKED    │  행은 남는다. 무기한 보존(FR-019a)
        │ revoked=true  │  이 상태에서 되살아나는 전이는 없다
        │ 해시 2개 NULL  │
        └───────────────┘
```

**폐기는 행 삭제가 아니다**(FR-111). 두 해시를 `NULL`로 만들고 `revoked`를 세운다.
해시를 비우는 것이 실질적 무효화이고, `revoked`는 부분 유니크 인덱스가 다음 세션을
허용하게 하는 스위치다. **둘 다 해야 한다** — 해시만 비우면 인덱스가 여전히 그 행을
활성으로 보아 새 로그인이 유니크 위반으로 막힌다.

### 검증 단계와 컬럼의 대응

`_공통.md`가 정한 Access Token 10단계 중 DB를 보는 5~9단계다.

| 단계 | 보는 것 | 실패 코드 |
|---|---|---|
| 5 | `session_id = sid AND id_key = sub` 인 행이 있는가 | `1006` |
| 6 | `access_token_hash`가 NULL·빈값인가 | `1006` |
| 7 | `sha256(요청 토큰) == access_token_hash` | `1006` |
| 8 | `now < access_expires_at` | `1001` |
| 9 | `tbl_user.active` | `1004` |

8단계가 JWT `exp` 검사(3단계)와 **중복돼 보이지만 둘 다 한다.** 스펙 Edge Case가
"만료 시각이 JWT와 DB에서 다르면 둘 다 검사하고 어느 하나라도 지났으면 `1001`"로 정했다.

---

## 3. `tbl_user_login_history` — 로그인 이력

Entity: `data-mod/.../entity/UserLoginHistory.java` · 기본키 `idx` · **이 기능은 INSERT만 한다**

| 컬럼 | 타입 | 제약 | 002에서의 쓰임 |
|---|---|---|---|
| `idx` | BIGINT | PK, IDENTITY | |
| `id_key` | BIGINT | NOT NULL, FK → `tbl_user` | 시도한 회원. **이 NOT NULL이 FR-127을 만든다** |
| `login_at` | TIMESTAMPTZ | NOT NULL | 시도 시각 |
| `login_ip` | VARCHAR(45) | NULL | IPv6까지 수용. 확보 못 하면 비운다 |
| `success` | BOOLEAN | NOT NULL | 성공 여부. 커밋 `1edf308`에서 추가됨 |

**INDEX**: `ix_user_login_history_at (id_key, login_at)`

### 기록 규칙 (FR-125·127)

| 상황 | 행을 만드나 | `success` |
|---|---|---|
| 로그인 성공 | 만든다 | `true` |
| 아이디는 맞고 비밀번호 불일치(`1003`) | 만든다 | `false` |
| 비활성 계정(`1004`) | 만든다 | `false` |
| **존재하지 않는 아이디** | **만들지 않는다** | — |

마지막 줄이 핵심이다. `id_key`·`created_by`·`updated_by`가 전부 NOT NULL인데 회원을 특정할 수
없으니 채울 값이 없다. 감사 컬럼을 nullable로 두는 것은 `tbl_user`에만 허용된 예외이므로
이 테이블에 예외를 하나 더 만들지 않는다. 그 시도는 애플리케이션 로그로만 남기고,
로그에 찍는 아이디는 마스킹한다(FR-128).

**"마지막 로그인"을 뽑을 때는 `success = true` 조건이 필요하다.** 성공과 실패가 한 테이블에
섞이므로, 조건 없이 최신 행을 집으면 실패 시도의 시각이 나온다.

---

## 4. `tbl_user_expend_group` — 지출유형 (쓰기만)

`003`이 소유한다. 002는 FR-106의 기본 10종 생성에서만 INSERT한다.

| 컬럼 | 가입·관리자 추가 시 넣는 값 |
|---|---|
| `id_key` | 방금 만든 회원의 `id_key` |
| `name` | 식비·교통·주거·통신·쇼핑·장보기·의료·교육·문화·기타 (확정 10종, "등"으로 열지 않는다) |
| `in_use` | `true` |
| `default_group` | `true` |
| `deleted` | `false` |
| `icon_filename` | `{id_key}_{expendGroupId}.png` |
| `created_by`·`updated_by` | 방금 만든 **그 회원의 `id_key`** (관리자 추가여도 회원 본인) |

**저장 순서가 강제된다.** `icon_filename`에 `expendGroupId`(= `idx`)가 들어가므로
① 행을 저장해 PK를 받고 → ② 시드 아이콘을 `{id_key}_{idx}.png`로 복사하고 → ③ 파일명을 UPDATE한다.
순서를 뒤집으면 파일명을 정할 수 없다.

시드 원본은 `app-mod/money-backend-app/src/main/resources/seed/expend-group-icons/`에
10개가 커밋되어 있다(실재 확인).

---

## 이 기능이 만들지 않는 저장 단위

| 개념 | 왜 만들지 않나 |
|---|---|
| 비밀번호 재설정 토큰 | 1.10·1.11 사이에 상태를 들고 다니지 않는다. 본인 확인은 `memberId`+`nickname` 대조뿐이고 1.11이 다시 검증한다 |
| 이메일 인증 코드 | 외부 채널 연동은 범위 밖이다 |
| 로그인 실패 횟수·잠금 | 잠금·속도 제한은 범위 밖이다(스펙 Assumptions). 실패 이력은 남기지만 그걸로 막지 않는다 |
| 권한·역할 테이블 | 권한은 `tbl_user.role` 값 2개(1·3)뿐이다. 테이블로 뺄 만한 다형성이 없다 |
| Refresh Token 전용 테이블 | 세션 1건이 Access·Refresh를 함께 관리한다(001의 결정) |
