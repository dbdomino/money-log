# Money Log — 가계부

## 개요

Spring Boot 기반 **멀티 모듈** 가계부 웹 애플리케이션이다.

- 로그인한 사용자 **본인**의 지출·소득·수단·지출유형·고정지출·통계만 조회·등록·수정·삭제
- **JWT + Refresh Token** 로그인 (Access 1일 / Refresh 7일)
- 프론트(`money-app`)는 Thymeleaf 화면만 담당하고, 데이터는 백엔드 API(`money-backend-app`)로 처리

| 항목 | 내용 |
|------|------|
| 루트 패키지 | `com.dbdomino.moneylog` |
| 프론트 | `app-mod/money-app` — http://localhost:8080 |
| 백엔드 API | `app-mod/money-backend-app` — http://localhost:8081 (`/api/v1/*`) |

## 아키텍처

```
브라우저 → money-app (Thymeleaf, :8080)
              ↓ HTTP (JWT Bearer)
         money-backend-app (REST, :8081, /api/v1/*)
              ↓
         core-mod / data-mod → PostgreSQL
```

| 모듈 | 역할 |
|------|------|
| `app-mod/money-app` | Thymeleaf UI. **DB 직접 접근 없음**, 백엔드 API만 호출 |
| `app-mod/money-backend-app` | REST API·비즈니스 진입점 (`com.dbdomino.moneylog.backend`) |
| `core-mod` | DB 사용 모델 설계(추상 모델). `data-mod` 쿼리 호출 구조 |
| `data-mod` | DB 접근·쿼리·DataSource/JPA 설정 |
| `common-mod` | 공통 유틸, 에러코드, 예외, AOP, 응답 규격 |

**호출 방향:** `Controller → Service → 모델 → Repository(data-mod)`  
프론트 앱은 `money-backend-app` API를 HTTP로 호출한다.

## 기술 스택

| 항목 | 버전 / 내용 |
|------|-------------|
| Spring Boot | **4.1.0** |
| Gradle | **9.6.1** (Wrapper) |
| Java | OpenJDK **17** |
| DB | **PostgreSQL 18** (로컬) |
| 기타 | Thymeleaf, Spring Data JPA, MyBatis, MapStruct, Lombok |

### Spring Boot 4 참고

- AOP starter: `spring-boot-starter-aop` → `spring-boot-starter-aspectj`
- JSON: Jackson 3 (`tools.jackson`)
- MyBatis Spring Boot Starter: **4.0.0**
- JDBC: `org.postgresql:postgresql` (BOM 관리)

## 주요 기능 (요구사항 요약)

상세: [프로젝트설계/1.요구사항 정리.md](./프로젝트설계/1.요구사항%20정리.md)

| # | 도메인 | 핵심 내용 |
|---|--------|-----------|
| ① | 회원 관리 | 가입·로그인(JWT)·id/pw 찾기·본인 정보 수정·비활성화. 관리자(1)만 회원 목록·추가·타 회원 수정 |
| ② | 지출·소득 수단 | 카드/계좌 타입, 사용여부, 논리 삭제. 입력 화면은 활성 수단만 |
| ③ | 지출유형 | 분류·아이콘·활성화. 지출 1건당 유형 1개 필수. 사용 중인 지출은 삭제 불가 |
| ④ | 가계부 | 지출(일시불·할부)·소득 CRUD, 월별 통합 목록, 고정지출(월별 override), 엑셀 일괄 등록(.xlsx, 최대 300행) |
| ⑤ | 목표금액 | 지출유형별 기본·월별 목표 (0원~1억 원, 활성 유형만) |
| ⑥ | 월별 통계 | 주별·유형별·수단별 합계, 목표 대비 사용률, 고정 vs 일반 비율. 저장 시 DB 스냅샷, 재저장 전까지 고정 |

**권한:** 관리자 `1` · 일반 `3` (가입 시 기본값 3)

## API·화면 명세

개요: [프로젝트설계/2.기능명세.md](./프로젝트설계/2.기능명세.md)

| 문서 | 내용 |
|------|------|
| [2.1. 기능명세 - 백엔드.md](./프로젝트설계/2.1.%20기능명세%20-%20백엔드.md) | REST API 54개, Method·URL·구현 Phase |
| [2.2. 기능명세 - 프론트엔드.md](./프로젝트설계/2.2.%20기능명세%20-%20프론트엔드.md) | 화면 URL·템플릿·호출 API |

### API 공통 규칙

- Base URL: `http://localhost:8081/api/v1`
- 응답: `{ "resCode": 0, "data": { ... } }` (실패 시 4자리 `resCode`)
- HTTP: **GET** 조회 · **POST** 생성·로그인 · **PATCH** 수정(omit=유지) · **DELETE** 삭제 — **PUT 미사용**
- 인증: JWT `Authorization: Bearer`. 본인 데이터만 접근, 관리자 API는 권한 `1` 필요

### 백엔드 구현 Phase (요약)

| Phase | 범위 |
|-------|------|
| 0 | 공통 (`RestResponseDto`, `ErrorCode`, AOP, Entity 이전) |
| 1 | 회원·인증 (API 1~14) |
| 2 | 수단·지출유형 (14~26) |
| 3 | 지출·소득 (27~36) |
| 4 | 고정지출·가계부·엑셀 (37~46) |
| 5 | 목표금액·통계 (48~52) |
| 6 | `money-app` ↔ API 연동 |

## 데이터베이스 (PostgreSQL 18)

로컬 PostgreSQL 사용.

| 항목 | 값 |
|------|-----|
| Host | `localhost:5432` |
| Database | `moneylogdb` |
| User | `moneyloguser` |
| Password | `1q2w3e4r` |

설정: `data-mod/src/main/resources/application-postgresql.yml`  
활성 프로필: `money-app`의 `application.yml` → `spring.profiles.active=postgresql`

```sql
CREATE USER moneyloguser WITH PASSWORD '1q2w3e4r';
CREATE DATABASE moneylogdb OWNER moneyloguser;
```

## 빌드 / 실행

JDK 17 필요. PATH에 다른 Java가 있으면 `JAVA_HOME` 지정.

```bash
export JAVA_HOME="/path/to/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew clean build
./gradlew :app-mod:money-app:bootRun
./gradlew :app-mod:money-backend-app:bootRun
```

| 앱 | URL |
|----|-----|
| money-app | http://localhost:8080 |
| money-backend-app | http://localhost:8081 (`GET /api/v1/ha`, API `/api/v1/*`) |

## 설계 문서

| # | 문서 | 상태 |
|---|------|------|
| 1 | [요구사항 정리](./프로젝트설계/1.요구사항%20정리.md) | **최신** |
| 1.1 | [요구사항 - 백엔드](./프로젝트설계/1.1.%20요구사항%20-%20백엔드.md) | |
| 1.2 | [요구사항 - 프론트엔드](./프로젝트설계/1.2.%20요구사항%20-%20프론트엔드.md) | |
| 2 | [기능명세](./프로젝트설계/2.기능명세.md) | **최신** |
| 2.1 | [기능명세 - 백엔드](./프로젝트설계/2.1.%20기능명세%20-%20백엔드.md) | |
| 2.2 | [기능명세 - 프론트엔드](./프로젝트설계/2.2.%20기능명세%20-%20프론트엔드.md) | |
| 3~ | [기능(서비스)정의](./프로젝트설계/3.기능(서비스)정의.md), [ERD](./프로젝트설계/5.%20ERD,%20Entity%20다이어그램.md) 등 | 이전 버전 기준 |

## 변경 이력

### 2026-07-15 — 스택·모듈

- Spring Boot **3.3.1 → 4.1.0**, Gradle **8.8 → 9.6.1**
- DB PostgreSQL 18 (`moneylogdb`), 설정 `data-mod` 배치
- 루트 패키지 `com.vanilla` → `com.dbdomino.moneylog`
- `money-backend-app` 모듈 추가 (REST API, port 8081)

### 2026-07-16 — 설계 문서 1·2 갱신

- [1.요구사항 정리](./프로젝트설계/1.요구사항%20정리.md): 도메인·권한·할부·고정지출·통계 요구사항 정리
- [2.기능명세](./프로젝트설계/2.기능명세.md): 프론트/백엔드 분리 아키텍처, API 54개, 구현 Phase 정의
