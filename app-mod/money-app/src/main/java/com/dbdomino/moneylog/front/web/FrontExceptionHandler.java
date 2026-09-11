package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.client.BackendUnavailableException;
import com.dbdomino.moneylog.front.session.SessionExpiredException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 백엔드 호출 실패를 화면으로 옮기는 <b>한 자리</b>.
 *
 * <p>컨트롤러는 try-catch 를 쓰지 않는다. 화면 31개가 각자 실패를 붙잡아 안내하면 같은
 * 실패가 화면마다 다르게 보이고, 사용자는 무엇이 잘못됐는지 배우지 못한다.
 *
 * <p><b>{@code @RestControllerAdvice} 가 아니라 {@code @ControllerAdvice} 다.</b> 화면
 * 모듈은 뷰를 돌려준다 — 앞의 것을 쓰면 사용자가 오류 화면 대신 JSON 을 본다.
 *
 * <p><b>빈 값으로 그리지 않는다.</b> 데이터를 얻지 못했으면 목록·상세를 빈 채로 띄우는 대신
 * 그 사실을 글로 보여 준다. 빈 목록과 "가져오지 못한 목록"이 똑같이 보이면 사용자는 자기
 * 데이터가 없어졌다고 읽는다.
 *
 * <p>모달 안에서 난 실패를 부모 페이지로 튕기지 않고 모달에 남겨 표시하는 것은 그 모달을
 * 가진 화면의 몫이다(008~012). 007 은 모달을 하나도 만들지 않으므로 여기서는 공통 착지만
 * 맡는다.
 */
@ControllerAdvice
public class FrontExceptionHandler {

    /** 백엔드가 못 닿는 상태일 때 보여 줄 문구. 코드가 없으므로 숫자를 적지 않는다. */
    private static final String UNAVAILABLE_MESSAGE = "서버에 닿지 못했습니다. 잠시 후 다시 시도해 주세요.";

    /**
     * 백엔드가 실패 봉투로 답했다. <b>코드를 바꾸지 않고</b> 그대로 화면에 넘긴다(SC-609).
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleBackendApi(BackendApiException exception, Model model) {
        model.addAttribute("resCode", exception.getResCode());
        model.addAttribute("message", exception.getMessage());
        return "error";
    }

    /**
     * 백엔드에 닿지 못했다. 코드를 붙이지 않는다 — 붙이면 "백엔드가 서버 오류를 줬다"와
     * "백엔드가 아예 응답하지 않았다"가 화면에서 구분되지 않는다.
     */
    @ExceptionHandler(BackendUnavailableException.class)
    public String handleBackendUnavailable(BackendUnavailableException exception, Model model) {
        model.addAttribute("message", UNAVAILABLE_MESSAGE);
        return "error";
    }

    /**
     * 로그인 상태가 끝났다. 오류 화면이 아니라 <b>로그인 화면</b>으로 보낸다.
     *
     * <p>세션은 이 예외가 올라오기 전에 이미 버려졌다. 사용자가 다음에 할 일이 로그인 하나뿐인
     * 상황에서 오류 화면을 한 번 거치게 하면 클릭만 늘어난다.
     *
     * <p>안내 문구는 백엔드가 준 것을 그대로 실어 보낸다. 다른 곳에서 로그인해 밀려난 경우와
     * 재발급 토큰이 만료된 경우와 계정이 정지된 경우는 다음에 할 일이 달라서, 백엔드가 이미
     * 셋으로 나눠 둔 문구를 화면이 하나로 뭉개지 않는다.
     */
    @ExceptionHandler(SessionExpiredException.class)
    public String handleSessionExpired(SessionExpiredException exception,
            RedirectAttributes redirectAttributes) {
        String message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            redirectAttributes.addFlashAttribute("message", message);
        }
        return "redirect:" + AuthInterceptor.LOGIN_URL;
    }
}
