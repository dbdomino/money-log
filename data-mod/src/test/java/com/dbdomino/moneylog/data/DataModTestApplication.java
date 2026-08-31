package com.dbdomino.moneylog.data;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code data-mod} 테스트 전용 부트 클래스.
 *
 * <p>{@code data-mod}는 라이브러리 모듈이라 {@code @SpringBootConfiguration}을 가진
 * 클래스가 없다. 그대로 두면 이 모듈의 {@code @SpringBootTest}가 설정 클래스를 찾지
 * 못해 컨텍스트 로딩 자체가 실패한다. 그래서 테스트 소스셋에만 이 클래스를 둔다.
 *
 * <p>이 패키지({@code com.dbdomino.moneylog.data})가 루트라 Entity·Repository·설정이
 * 모두 기본 스캔 범위에 들어온다 — 운영 앱({@code money-backend-app})이
 * {@code @EntityScan}·{@code @EnableJpaRepositories}로 명시해야 하는 것과 달리
 * 여기서는 추가 지정이 필요 없다.
 *
 * <p>datasource·JPA 설정은 {@code data-mod/src/main/resources/application-postgresql.yml}
 * 에서 온다. 테스트는 {@code @ActiveProfiles("postgresql")}로 그 프로필을 켠다.
 */
@SpringBootApplication
public class DataModTestApplication {
}
