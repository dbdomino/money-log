package com.dbdomino.moneylog.front.session;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 접근 토큰이 만료됐을 때 <b>한 번</b> 재발급한다.
 *
 * <p>재발급 호출 자체는 인증을 요구하지 않고 재시도 흐름도 타지 않는다. 그래서 인증 헤더를
 * 붙이지 않는 통로로 보낸다 — 만료된 토큰을 실어 보내면 재발급마저 만료로 거절된다.
 */
@Component
public class TokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    /** 백엔드 토큰 갱신 경로. */
    private static final String REFRESH_PATH = "/auth/refresh";

    private final BackendApiClient backendApiClient;
    private final LoginSession loginSession;

    public TokenRefresher(BackendApiClient backendApiClient, LoginSession loginSession) {
        this.backendApiClient = backendApiClient;
        this.loginSession = loginSession;
    }

    /**
     * 세션의 재발급 토큰으로 새 토큰 한 벌을 받아 세션에 덮어쓴다.
     *
     * <p>실패하면 세션을 버리고 {@link SessionExpiredException} 을 올린다. 이때 코드는
     * 백엔드가 준 것을 그대로 쓴다 — 재발급 토큰 만료와 다른 곳 로그인과 계정 정지는 처리가
     * 같아도 사용자에게 할 말이 다르다.
     *
     * @return 재발급에 성공했으면 참
     */
    public boolean refresh() {
        String refreshToken = loginSession.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            loginSession.invalidate();
            throw new SessionExpiredException(0, "다시 로그인해 주세요.");
        }

        TokenPair issued;
        try {
            issued = backendApiClient.postWithoutAuth(REFRESH_PATH,
                    Map.of("refreshToken", refreshToken), TokenPair.class);
        } catch (BackendApiException e) {
            // 재발급 토큰 만료·다른 곳 로그인·계정 정지가 여기 온다. 셋 다 세션을 버린다.
            log.info("토큰 재발급 실패 resCode={}", e.getResCode());
            loginSession.invalidate();
            throw new SessionExpiredException(e.getResCode(), e.getMessage());
        }

        if (issued == null || issued.accessToken() == null || issued.refreshToken() == null) {
            loginSession.invalidate();
            throw new SessionExpiredException(0, "다시 로그인해 주세요.");
        }

        // 둘 다 덮어쓴다. 백엔드가 Rotation 이라 옛 재발급 토큰은 이 순간 폐기된다.
        loginSession.replaceTokens(issued.accessToken(), issued.refreshToken());
        return true;
    }

    /**
     * 재발급 응답에서 화면 모듈이 쓰는 두 값.
     *
     * <p>만료까지 남은 초와 토큰 종류도 함께 오지만 받지 않는다 — 세션에 담지 않을 값을
     * 굳이 타입에 두면 "이건 어디 쓰나"를 읽는 사람이 매번 되묻게 된다.
     */
    public record TokenPair(String accessToken, String refreshToken) {
    }
}
