package com.dbdomino.moneylog.backend;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 백엔드 API 통합 테스트 기반.
 *
 * <p>실제 PostgreSQL 과 실제 필터 체인을 지나는 요청을 검사한다. 여기서 확인하는 것은
 * 응답 {@code resCode}와 그 결과로 남은 <b>DB 상태</b>이며, 서비스 메서드의 반환값이
 * 아니다 — 인가·예외 변환·감사 컬럼이 전부 컨텍스트가 있어야 성립하기 때문이다.
 *
 * <h2>jwt.secret 을 여기서 싣는 이유</h2>
 *
 * <p>운영 {@code application.yml}은 {@code jwt.secret: ${JWT_SECRET}}으로 환경변수를
 * 요구하고 기본값이 없다. 그대로면 환경변수 없이 도는 실행(로컬·CI)에서
 * {@code JwtProperties}가 기동을 막아 이 클래스를 상속한 테스트가 전부 컨텍스트 로딩에서
 * 깨진다. {@code @SpringBootTest}는 {@code @Inherited}라 하위 테스트가 이 설정을
 * 그대로 물려받는다.
 *
 * <p><b>{@code src/test/resources/application.yml}을 만들지 않는다.</b> 테스트
 * 클래스패스의 같은 이름 파일은 메인 설정을 보완하는 것이 아니라 <b>대체</b>한다 —
 * {@code spring.profiles.active: postgresql}까지 사라져 datasource 가 통째로 없어진다.
 * {@code money-app}의 레거시 테스트 3건이 {@code init} 커밋부터 깨져 있는 원인이 그것이다.
 *
 * <h2>트랜잭션을 클래스에 걸지 않는다</h2>
 *
 * <p>{@code @Transactional} 테스트는 끝에 롤백되는데, MockMvc 요청은 별도 트랜잭션에서
 * 도는 서비스 코드를 부르므로 "테스트가 만든 것"과 "요청이 만든 것"의 가시성이 갈린다.
 * 대신 각 테스트가 만든 데이터를 {@link #cleanUpTestUsers()}가 지운다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/quickstart.md">quickstart.md §3</a>
 */
@SpringBootTest(properties = {
        // 테스트 전용 키. 운영 키와 절대 같은 값을 쓰지 않는다. HS256 이라 32바이트 이상이어야 한다.
        "jwt.secret=test-only-secret-not-for-any-real-environment-0123456789"
})
@AutoConfigureMockMvc
@ActiveProfiles("postgresql")
public abstract class AbstractApiIT {

    /**
     * 테스트가 만든 회원의 아이디 접두사. 정리할 때 이 접두사로 지운다.
     *
     * <p>밑줄을 쓰지 않는다 — SQL {@code LIKE}에서 {@code _}는 한 글자 와일드카드라
     * {@code 'it_%'}로 지우면 의도치 않은 아이디까지 걸린다. 001 에서 실제로 겪은 일이다.
     */
    protected static final String TEST_USER_PREFIX = "ittest";

    /** 테스트 회원의 평문 비밀번호. 규칙(8자 이상·3종류 이상)을 만족한다. */
    protected static final String TEST_PASSWORD = "Test1234!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TransactionTemplate tx;

    @Autowired
    protected JdbcTemplate jdbc;

    /** 일반 회원 1건을 만들어 저장한다. 비밀번호는 {@link #TEST_PASSWORD}다. */
    protected User createMember() {
        return createUser(User.ROLE_MEMBER, true);
    }

    /** 관리자 1건을 만들어 저장한다. 관리자 API 의 인가 검증에 쓴다. */
    protected User createAdmin() {
        return createUser(User.ROLE_ADMIN, true);
    }

    /** 비활성 회원 1건. 로그인이 {@code 1004}로 막히는지 확인할 때 쓴다. */
    protected User createInactiveMember() {
        return createUser(User.ROLE_MEMBER, false);
    }

    /**
     * 회원 1건을 만들어 저장한다.
     *
     * <p>비밀번호는 <b>반드시 인코더를 거친다</b>. 평문을 넣으면 로그인 테스트가
     * {@code matches}에서 실패해, 실제 원인과 무관한 자리에서 깨진다.
     *
     * <p>{@code created_by}/{@code updated_by}는 채우지 않는다 — {@code tbl_user}만 두
     * 컬럼이 nullable 이고, 본인 가입 경로가 바로 그 경우다.
     */
    protected User createUser(short role, boolean active) {
        User user = new User();
        user.setUserId(TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        user.setPw(passwordEncoder.encode(TEST_PASSWORD));
        user.setNickname("테스트회원");
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    /**
     * 테스트가 만든 회원과 그 자식 행을 지운다.
     *
     * <p><b>트랜잭션 안에서 지운다.</b> datasource 가 {@code auto-commit: false}라
     * 트랜잭션 밖 {@code JdbcTemplate} 갱신은 커밋되지 않고 조용히 사라진다 — 정리 코드가
     * 아무 일도 하지 않는 것처럼 보이면 이것을 의심한다(001 에서 겪은 일이다).
     *
     * <p>자식을 먼저 지운다. FK 가 있어 순서를 뒤집으면 위반으로 막힌다.
     */
    @AfterEach
    void cleanUpTestUsers() {
        tx.executeWithoutResult(status -> {
            String owner = "select id_key from moneylog.tbl_user where user_id like ?";
            String pattern = TEST_USER_PREFIX + "%";
            jdbc.update("delete from moneylog.tbl_user_login_history where id_key in (" + owner + ")", pattern);
            jdbc.update("delete from moneylog.tbl_user_session where id_key in (" + owner + ")", pattern);
            jdbc.update("delete from moneylog.tbl_user_expend_group where id_key in (" + owner + ")", pattern);
            jdbc.update("delete from moneylog.tbl_user where user_id like ?", pattern);
        });
    }
}
