package com.dbdomino.moneylog.backend;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
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
        "jwt.secret=test-only-secret-not-for-any-real-environment-0123456789",
        // 아이콘 복사본을 둘 임시 디렉터리. 운영은 ICON_STORAGE_DIR 로 주입한다.
        "icon.storage.dir=${java.io.tmpdir}/moneylog-it-icons"
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

    /** 로그인해 토큰 한 벌을 받는다. 성공을 전제하며, 실패하면 그 자리에서 터진다. */
    protected Tokens login(User user) throws Exception {
        JsonNode data = postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD))
                .get("data");
        return new Tokens(data.get("accessToken").asString(), data.get("refreshToken").asString());
    }

    /** JSON 본문을 POST 하고 응답을 파싱한다. HTTP 상태는 확인하지 않는다 — 규격상 대부분 200 이다. */
    protected JsonNode postJson(String url, String body) throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** Bearer 토큰을 실어 GET 한다. 토큰이 {@code null} 이면 헤더를 붙이지 않는다. */
    protected JsonNode getJson(String url, String accessToken) throws Exception {
        var request = MockMvcRequestBuilders.get(url);
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** Bearer 토큰을 실어 POST 한다(Body 없음). */
    protected JsonNode postJson(String url, String accessToken, String body) throws Exception {
        var request = MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        if (body != null) {
            request = request.content(body);
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** Bearer 토큰을 실어 PATCH 한다. {@code body} 가 {@code null} 이면 본문을 붙이지 않는다. */
    protected JsonNode patchJson(String url, String accessToken, String body) throws Exception {
        var request = MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        if (body != null) {
            request = request.content(body);
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /**
     * Bearer 토큰을 실어 DELETE 한다.
     *
     * <p>이 프로젝트의 DELETE 는 <b>본문이 없고 삭제 표시만 한다</b>(2.5·2.12). 실패도
     * HTTP 200 에 실려 오므로 상태 코드가 아니라 {@code resCode} 로 갈린다.
     */
    protected JsonNode deleteJson(String url, String accessToken) throws Exception {
        var request = MockMvcRequestBuilders.delete(url);
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** 응답의 {@code resCode}. 모든 검사가 이 값으로 갈린다. */
    protected int resCode(JsonNode response) {
        return response.get("resCode").asInt();
    }

    /** 회원의 활성 세션 수. 부분 유니크 인덱스가 1건을 강제하는지 확인할 때 쓴다. */
    protected int countActiveSessions(User user) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_session where id_key = ? and revoked = false",
                Integer.class, user.getIdKey());
        return count == null ? 0 : count;
    }

    /** 회원의 로그인 이력 수. */
    protected int countLoginHistory(User user) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_login_history where id_key = ?",
                Integer.class, user.getIdKey());
        return count == null ? 0 : count;
    }

    /** 발급받은 토큰 한 벌. */
    protected record Tokens(String accessToken, String refreshToken) {
    }

    // ── 가입으로 만든 회원과 그 소유 자원 ──────────────────────────────────
    //
    // 아래 헬퍼는 003(지출유형·아이콘)과 004(지출·소득·할부·엑셀)가 함께 쓴다.
    // 원래 003 의 AbstractExpendGroupIT 에 있었으나, 004 의 네 테스트 패키지가 전부
    // 필요로 해 여기로 올렸다 — 복제하면 가입 절차가 두 곳에서 갈린다.

    /**
     * 가입으로 만든 회원. <b>아이디를 함께 들고 다닌다</b> — 그 회원의 행만 골라 보거나
     * 고치려면 토큰만으로는 부족하다.
     */
    protected record Member(String memberId, String token) {
    }

    /**
     * 가입(1.2)하고 로그인(1.1)한다. <b>기본 지출유형 10종과 아이콘이 함께 생긴다.</b>
     *
     * <p>{@link #createMember()} 와 다르다 — 그쪽은 Repository 로 회원 행만 만들어
     * 기본 지출유형이 생기지 않는다. 지출을 등록하려면 참조할 유형이 있어야 하므로
     * 004 의 시험은 대부분 이쪽을 쓴다.
     */
    protected Member signupAndLogin() throws Exception {
        String memberId = TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        JsonNode signup = postJson("/api/v1/auth/signup", """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"테스트회원"}
                """.formatted(memberId, TEST_PASSWORD, TEST_PASSWORD));
        if (resCode(signup) != 200) {
            throw new IllegalStateException("가입 실패: " + signup);
        }

        JsonNode login = postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(memberId, TEST_PASSWORD));
        if (resCode(login) != 200) {
            throw new IllegalStateException("로그인 실패: " + login);
        }
        return new Member(memberId, login.get("data").get("accessToken").asString());
    }

    /** 그 회원의 대리키. 참조 행을 JDBC 로 만들 때 소유자·감사 컬럼에 쓴다. */
    protected Long idKeyOf(Member member) {
        return jdbc.queryForObject(
                "select id_key from moneylog.tbl_user where user_id = ?",
                Long.class, member.memberId());
    }

    /** 가입이 만들어 준 기본 지출유형 하나(이름으로 고른다)의 PK. */
    protected long defaultGroupId(Member member, String name) {
        return jdbc.queryForObject("""
                select g.idx from moneylog.tbl_user_expend_group g
                  join moneylog.tbl_user u on u.id_key = g.id_key
                 where u.user_id = ? and g.name = ?
                """, Long.class, member.memberId(), name);
    }

    /**
     * 003 의 2.1 로 수단 1건을 만들고 PK 를 돌려준다.
     *
     * <p><b>API 로 만든다.</b> JDBC 로 넣으면 감사 컬럼을 손으로 채워야 하고, 004 가
     * 의존하는 "사용 중"(FR-325) 상태가 003 의 등록 규칙과 어긋날 수 있다.
     *
     * @param purpose {@code EXPENSE}(지출용) 또는 {@code INCOME}(소득용). 소득은 소득용
     *                수단을 써야 한다
     */
    protected long createPaymentMethod(String token, String name, String purpose) throws Exception {
        JsonNode response = postJson("/api/v1/payment-methods", token, """
                {"name":"%s","type":"CARD","purpose":"%s","inUse":true}
                """.formatted(name, purpose));
        if (resCode(response) != 200) {
            throw new IllegalStateException("수단 등록 실패: " + response);
        }
        return response.get("data").get("paymentMethodId").asLong();
    }

    /** 지출용 수단. 3.1·3.5 가 쓴다. */
    protected long createExpensePaymentMethod(String token, String name) throws Exception {
        return createPaymentMethod(token, name, "EXPENSE");
    }

    /** 소득용 수단. 3.7 이 쓴다. */
    protected long createIncomePaymentMethod(String token, String name) throws Exception {
        return createPaymentMethod(token, name, "INCOME");
    }

    /**
     * 003 의 2.7 로 지출유형 1건을 만들고 PK 를 돌려준다.
     *
     * <p><b>기본 유형은 이름을 바꿀 수 없다</b>({@code 3105}). 이름 변경이 필요한 시험은
     * {@link #defaultGroupId} 대신 이것으로 새 유형을 만들어 쓴다.
     *
     * <p>{@code multipart/form-data} 다 — 2.7 이 아이콘을 함께 받기 때문이다. 아이콘은
     * 선택이라 여기서는 폼 필드만 보낸다.
     *
     * <p><b>{@code inUse} 는 생략할 수 없다.</b> {@code ExpendGroupCreateRequest} 에서
     * {@code @NotNull} 이라 빠뜨리면 {@code 9001} 이다 — 2.1 수단 등록의 {@code inUse} 가
     * 기본값을 갖는 것과 다르다.
     */
    protected long createExpendGroup(String token, String name) throws Exception {
        var request = MockMvcRequestBuilders.multipart("/api/v1/expend-groups");
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.part(new MockPart("name", name.getBytes(StandardCharsets.UTF_8)));
        request.part(new MockPart("inUse", "true".getBytes(StandardCharsets.UTF_8)));
        JsonNode response = objectMapper.readTree(mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        if (resCode(response) != 200) {
            throw new IllegalStateException("지출유형 등록 실패: " + response);
        }
        return response.get("data").get("expendGroupId").asLong();
    }

    /** 003 의 2.11 로 지출유형 이름을 바꾼다. {@code multipart} + PATCH 다. */
    protected JsonNode renameExpendGroup(String token, long expendGroupId, String name)
            throws Exception {
        var request = MockMvcRequestBuilders.multipart("/api/v1/expend-groups/" + expendGroupId);
        request.with(servletRequest -> {
            servletRequest.setMethod("PATCH");
            return servletRequest;
        });
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.part(new MockPart("name", name.getBytes(StandardCharsets.UTF_8)));
        return objectMapper.readTree(mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    // ── 005(고정지출·가계부)가 쓰는 헬퍼 ─────────────────────────────────────

    /**
     * 4.1 로 고정지출 설정 1건을 만들고 PK 를 돌려준다.
     *
     * <p><b>API 로 만든다.</b> JDBC 로 넣으면 감사 컬럼을 손으로 채워야 하고, 004 에서
     * 겪었듯 등록 규칙과 시험이 갈릴 수 있다.
     *
     * <p><b>이 호출은 관리 행 1건만 만든다.</b> 월별 내역은 그 달을 처음 조회할 때
     * 생긴다(FR-402). 시험이 월별 행을 원하면 4.5·4.8 을 부르거나 4.9 로 재작성한다.
     *
     * @param yearMonthRange {@code "2026-11"} ~ {@code "2027-02"} 를 각각 시작·종료로 쓴다
     */
    protected long createFixedExpense(String token, String name, long paymentMethodId,
                                      long expendGroupId, long amount, int paymentDayOfMonth,
                                      String startYearMonth, String endYearMonth) throws Exception {
        JsonNode response = postJson("/api/v1/fixed-expenses", token, """
                {"name":"%s","paymentMethodId":%d,"expendGroupId":%d,"amount":%d,
                 "paymentDayOfMonth":%d,"content":"%s",
                 "startYear":%s,"startMonth":%s,"endYear":%s,"endMonth":%s}
                """.formatted(name, paymentMethodId, expendGroupId, amount, paymentDayOfMonth, name,
                yearOf(startYearMonth), monthOf(startYearMonth),
                yearOf(endYearMonth), monthOf(endYearMonth)));
        if (resCode(response) != 200) {
            throw new IllegalStateException("고정지출 등록 실패: " + response);
        }
        return response.get("data").get("fixedExpenseId").asLong();
    }

    /** {@code "2026-11"} 의 연. */
    protected static int yearOf(String yearMonth) {
        return Integer.parseInt(yearMonth.substring(0, 4));
    }

    /** {@code "2026-11"} 의 월. */
    protected static int monthOf(String yearMonth) {
        return Integer.parseInt(yearMonth.substring(5, 7));
    }

    /**
     * 그 회원의 그 연·월 월별 내역 건수.
     *
     * <p>lazy 생성이 실제로 몇 행을 만들었는지는 <b>DB 를 봐야</b> 안다. 응답의 목록
     * 길이는 필터가 걸리면 달라지므로(FR-406) 생성 여부의 근거가 되지 못한다.
     */
    protected int countMonthly(Member member, int year, int month) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_fixed_expense_monthly m
                  join moneylog.tbl_user u on u.id_key = m.id_key
                 where u.user_id = ? and m.year = ? and m.month = ?
                """, Integer.class, member.memberId(), year, month);
        return count == null ? 0 : count;
    }

    /** 그 회원의 월별 내역 전체 건수(연·월 무관). 삭제 CASCADE 검증(SC-407)이 쓴다. */
    protected int countMonthlyAll(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_fixed_expense_monthly m
                  join moneylog.tbl_user u on u.id_key = m.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }

    /**
     * 한 고정지출의 그 연·월 행을 통째로 읽는다.
     *
     * <p>{@code payment_date}(말일 보정 결과)와 {@code modified} 를 <b>저장된 값</b>으로
     * 확인하는 데 쓴다. 응답만 보면 조회 때마다 다시 계산하는 구현도 통과해 버린다.
     *
     * @return 컬럼 이름 → 값. 행이 없으면 예외가 난다 — "아직 안 만들어졌다"를 확인할
     *         때는 {@link #countMonthly} 를 쓴다
     */
    protected Map<String, Object> monthlyRowOf(Member member, long fixedExpenseId,
                                               int year, int month) {
        return jdbc.queryForMap("""
                select m.idx, m.amount, m.payment_date, m.content, m.modified,
                       m.payment_method_idx, m.expend_group_idx, m.year, m.month
                  from moneylog.tbl_fixed_expense_monthly m
                  join moneylog.tbl_user u on u.id_key = m.id_key
                 where u.user_id = ? and m.fixed_expense_idx = ? and m.year = ? and m.month = ?
                """, member.memberId(), fixedExpenseId, year, month);
    }

    /**
     * 월별 내역 1행을 JDBC 로 직접 넣는다.
     *
     * <p><b>4.5 가 아직 없는 US1 단계에서 쓴다.</b> 삭제 CASCADE(SC-407)를 확인하려면
     * 지울 자식 행이 있어야 하는데, 정상 경로인 lazy 생성은 US2 가 만든다. US2 이후로는
     * 그 달을 <b>열어서</b> 만드는 편이 낫다 — 그쪽이 실제 경로이고 값도 규칙대로 채워진다.
     *
     * <p><b>감사 컬럼을 손으로 채운다.</b> {@code AuditingEntityListener} 는 Entity 를 거칠
     * 때만 동작하는데 이 경로는 Entity 를 만들지 않는다. 네 컬럼이 NOT NULL 이라 빠뜨리면
     * INSERT 가 통째로 실패한다.
     *
     * <p><b>트랜잭션 안에서 넣는다.</b> datasource 가 {@code auto-commit: false} 라
     * 트랜잭션 밖 갱신은 커밋되지 않고 조용히 사라진다.
     */
    protected void insertMonthlyRow(Member member, long fixedExpenseId, int year, int month,
                                    long amount) {
        Long idKey = idKeyOf(member);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_fixed_expense_monthly
                    (id_key, fixed_expense_idx, year, month, amount, payment_date, content,
                     payment_method_idx, expend_group_idx, modified,
                     created_at, updated_at, created_by, updated_by)
                select ?, f.idx, ?, ?, ?, make_date(?, ?, 1), f.content,
                       f.payment_method_idx, f.expend_group_idx, false,
                       now(), now(), ?, ?
                  from moneylog.tbl_fixed_expense f
                 where f.idx = ?
                """, idKey, year, month, amount, year, month, idKey, idKey, fixedExpenseId));
    }

    /**
     * 그 달 행의 {@code modified} 를 JDBC 로 세운다.
     *
     * <p>4.6 이 아직 없는 단계(US2)에서 "직접 수정한 달"을 만들 때 쓴다. US3 이후로는
     * 4.6 을 부르는 편이 낫다 — 그쪽이 실제 경로다.
     *
     * <p><b>트랜잭션 안에서 갱신한다.</b> datasource 가 {@code auto-commit: false} 라
     * 트랜잭션 밖 갱신은 커밋되지 않고 조용히 사라진다.
     */
    protected void markMonthlyModified(Member member, long fixedExpenseId, int year, int month) {
        tx.executeWithoutResult(status -> jdbc.update("""
                update moneylog.tbl_fixed_expense_monthly
                   set modified = true
                 where fixed_expense_idx = ? and year = ? and month = ?
                   and id_key = (select id_key from moneylog.tbl_user where user_id = ?)
                """, fixedExpenseId, year, month, member.memberId()));
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
            for (String table : TABLES_IN_DELETE_ORDER) {
                jdbc.update("delete from moneylog." + table + " where id_key in (" + owner + ")",
                        pattern);
            }
            jdbc.update("delete from moneylog.tbl_user where user_id like ?", pattern);
        });
    }

    /**
     * 정리 순서. <b>자식 → 부모</b>이며 바꾸면 FK 위반으로 정리가 통째로 실패한다.
     *
     * <p>맨 앞 여섯은 `tbl_user_payment_method(idx)`·`tbl_user_expend_group(idx)` 를 참조한다.
     * 003 의 참조 검사 시험(수단의 {@code purpose} 변경·유형 삭제 차단)이 그 행들을
     * Repository 로 직접 만들기 때문에, 부모부터 지우면 남은 자식이 FK 로 버틴다.
     *
     * <p>정리가 실패해도 예외는 다음 테스트의 엉뚱한 자리에서 터지므로 원인을 찾기 어렵다.
     * 새 테이블에 행을 만드는 시험을 추가하면 <b>이 목록도 함께 늘린다</b>.
     */
    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "tbl_expense",
            "tbl_income",
            "tbl_fixed_expense_monthly",
            "tbl_fixed_expense",
            "tbl_expend_target_monthly",
            "tbl_expend_target_default",
            "tbl_user_payment_method",
            "tbl_user_expend_group",
            "tbl_user_session",
            "tbl_user_login_history");
}
