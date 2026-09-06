package com.dbdomino.moneylog.backend;

import org.junit.jupiter.api.Test;

/**
 * 컨텍스트가 뜨는지만 본다.
 *
 * <p>{@link AbstractApiIT}를 상속해 {@code jwt.secret}과 {@code postgresql} 프로필을
 * 물려받는다. 운영 {@code application.yml}이 {@code jwt.secret}에 기본값을 두지 않아,
 * 이 설정 없이는 {@code JwtProperties}가 기동을 막는다.
 */
class MoneyBackendApplicationTests extends AbstractApiIT {

	@Test
	void contextLoads() {
	}
}
