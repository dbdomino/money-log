# 프로젝트 Vanilla - 가계부

## 개요
스프링 부트 기반의 웹 프로젝트로 가계부를 구현하는 것이 목적이다.

- Spring Boot + Thymeleaf + JPA + MyBatis + PostgreSQL
- 멀티 모듈: `common-mod`, `core-mod`, `data-mod`, `app-mod/money-app`, `app-mod/money-backend-app`
- 루트 패키지: `com.dbdomino.moneylog`

## 기술 스택

| 항목 | 버전 / 내용 |
|------|-------------|
| Spring Boot | **4.1.0** |
| Gradle | **9.6.1** (Wrapper) |
| Java | OpenJDK **17** |
| DB | **PostgreSQL 18** (로컬) |
| 기타 | Thymeleaf, Spring Data JPA, MyBatis, MapStruct, Lombok |

## 모듈 구성

| 모듈 | 역할 |
|------|------|
| `app-mod/money-app` | Thymeleaf 웹 애플리케이션 (화면 + 서버) |
| `app-mod/money-backend-app` | REST API 백엔드 (`com.dbdomino.moneylog.backend`, port **8081**) |
| `data-mod` | 데이터 접근 계층 · **DB(DataSource/JPA) 설정** |
| `common-mod` | 공통 Spring Boot 설정·유틸 |
| `core-mod` | 코어 유틸 (Jackson, commons-lang3 등) |

## 데이터베이스 (PostgreSQL 18)

로컬에 설치된 PostgreSQL을 사용한다.

| 항목 | 값 |
|------|-----|
| Host | `localhost:5432` |
| Database | `moneylogdb` |
| User | `moneyloguser` |
| Password | `1q2w3e4r` |

설정 파일: `data-mod/src/main/resources/application-postgresql.yml`  
활성 프로필: `money-app`의 `application.yml`에서 `spring.profiles.active=postgresql`

### DB / 유저 생성 예시

```sql
CREATE USER moneyloguser WITH PASSWORD '1q2w3e4r';
CREATE DATABASE moneylogdb OWNER moneyloguser;
```

## 빌드 / 실행

JDK 17이 필요합니다. PATH에 다른 Java가 앞에 있으면 `JAVA_HOME`을 지정하세요.

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
| money-backend-app | http://localhost:8081 (`GET /api/health`) |

## Spring Boot 4 관련 참고

- AOP starter: `spring-boot-starter-aop` → `spring-boot-starter-aspectj`
- JSON: Jackson 3 (`tools.jackson`) 사용
- MyBatis Spring Boot Starter: **4.0.0**
- JDBC: `org.postgresql:postgresql` (BOM 관리)

---

## 2024-07-10
### 프로젝트 제작, 작업 순서 정리
- 의존성 설정
- 프로퍼티 설정
- git 설정
- 프로젝트 설계
  1. 요구사항 정리 ㅇ
  2. 기능 상세정리
  3. 도메인 모델 그래프
  4. 객체 그래프
  5. 어플리케이션 아키텍처
  6. 엔티티 설계
  7. 테이블 설계
  8.
- 개발 웹 준비하기

## 2026-07-15
### 스택·DB 설정 업데이트
- Spring Boot **3.3.1 → 4.1.0**, Gradle **8.8 → 9.6.1**
- DB를 PostgreSQL 18 (`moneylogdb`)로 통일, 설정을 `data-mod`에 배치
- MySQL/H2 기본 연결 설정 제거, `postgresql` 프로필 사용
- 루트 패키지 `com.vanilla` → `com.dbdomino.moneylog`
- `app-mod/money-backend-app` 모듈 추가 (REST API, port 8081)
