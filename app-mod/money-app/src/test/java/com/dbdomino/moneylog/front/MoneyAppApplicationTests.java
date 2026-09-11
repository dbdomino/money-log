package com.dbdomino.moneylog.front;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * 화면 모듈이 <b>DB 없이</b> 뜬다는 것을 고정한다.
 *
 * <p>PostgreSQL 을 실제로 내리고 기동해 보는 대신 이 시험 하나로 대신한다 — 컨텍스트가
 * 떴는데 {@code DataSource} 빈이 하나도 없다면 이 모듈에는 DB 로 가는 길이 없다는 뜻이다.
 * 사람이 DB 를 껐다 켜며 확인할 필요가 없고, 나중에 누가 데이터 접근 의존을 되살리면
 * 이 시험이 먼저 깨진다.
 */
@SpringBootTest
class MoneyAppApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void 컨텍스트가_뜬다() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void 데이터소스_빈이_하나도_없다() {
        assertThat(applicationContext.getBeanNamesForType(DataSource.class))
                .as("화면 모듈에 DataSource 가 있으면 DB 로 가는 길이 열린 것이다")
                .isEmpty();
    }
}
