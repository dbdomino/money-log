package com.dbdomino.moneylog.front.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 진입 판정을 어디에 걸지 정한다.
 *
 * <p>판정은 <b>모든 주소에 기본으로 걸고</b> 빼는 것만 여기 적는다. 반대로 하면 008~012 가
 * 화면을 더할 때마다 등록을 잊을 수 있고, 잊은 화면은 무방비로 열린다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 정적 자원. 판정을 걸 이유가 없고, 걸면 로그인 화면의 스타일까지 막힌다.
                        "/css/**", "/js/**", "/images/**", "/favicon.ico",
                        // 오류 착지. 여기에 판정을 걸면 오류 화면이 로그인으로 밀려나
                        // 무엇이 잘못됐는지 사용자에게 영영 보이지 않는다.
                        "/error",
                        // 화면이 아니라 보낼 곳만 정하는 주소 셋. 본문을 그리지도 백엔드를
                        // 부르지도 않고 Location 하나만 내보낸다.
                        //
                        // 루트를 빼는 것은 요구사항이다 — 로그인 상태에 따라 가계부와 로그인
                        // 화면으로 갈라 보내는 판단을 루트가 직접 해야 하는데, 판정이 먼저
                        // 가로채면 그 분기가 실행되지 않는다.
                        //
                        // 옛 홈은 루트로 보내 같은 분기를 다시 타게 하고, 옛 로그인 페이지는
                        // 지금의 로그인 화면으로 보낸다. 둘 다 로그인하지 않은 사용자가
                        // 북마크로 들어오는 자리라 막으면 갈 곳을 잃는다.
                        "/", "/mem/ind", "/mem/login",
                        // 아이콘 프록시. 정적 자원을 뺀 이유와 같다 — 지출유형이 20개인 화면을
                        // 열면 아이콘 요청이 20건 나가는데 그때마다 토큰 검증이 따라붙으면
                        // 화면 한 번에 백엔드 왕복이 40건이 된다.
                        //
                        // 판정을 건너뛰는 것이 아니다: 프록시가 세션의 토큰을 실어 백엔드를
                        // 부르므로 소유자 판정과 토큰 검증은 백엔드가 그대로 한다. 세션이
                        // 비어 있으면 토큰 없이 나가 인증 실패를 받고 이미지를 내보내지 않는다.
                        "/expend-groups/icons/**");
    }
}
