package com.dbdomino.moneylog.front.support;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiException;
import org.springframework.ui.Model;

/**
 * 폼이 실패했을 때 화면에 넘기는 값을 <b>한 곳에서</b> 정한다.
 *
 * <p>담는 것은 셋이다 — 응답 코드 · 백엔드 문구 · 실패한 칸. 화면 여덟 개가 각자 문자열을
 * 쓰면 오타 하나가 "그 화면에서만 안내가 안 뜬다"는 증상이 되는데, 증상만 보고는 오타라는
 * 것을 짐작하기 어렵다. 이름을 여기 묶어 두면 오타가 컴파일에서 걸린다.
 *
 * <h2>코드가 칸을 정한다</h2>
 *
 * <p>어느 칸에 붙일지는 백엔드 응답 코드로만 가른다. 화면이 문구를 읽어 짐작하지 않는다 —
 * 백엔드가 문구를 다듬는 순간 붙는 자리가 바뀐다.
 *
 * <p><b>모르는 코드는 상단이다.</b> 엉뚱한 칸에 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
 *
 * <h2>로그인 실패는 여기서도 상단이다</h2>
 *
 * <p>{@code 1003} 은 아이디·비밀번호 어느 쪽이 틀렸는지 백엔드가 일부러 묶은 코드다. 칸에
 * 붙이는 순간 그 아이디가 실재하는지 드러난다. 이 표에 넣지 않는 것이 곧 상단 표시다.
 *
 * <h2>입력값을 담지 않는다</h2>
 *
 * <p>여기 담기는 문구는 백엔드가 준 것 그대로다. 화면이 "비밀번호 abc 는 규칙에 맞지
 * 않습니다" 같은 문구를 만들면 사용자의 입력이 화면과 로그 양쪽에 남는다.
 */
public final class FormFailure {

    /** 실패한 응답 코드. 폼 상단 안내에 함께 적어 문의할 때 단서가 되게 한다. */
    public static final String RES_CODE = "failResCode";

    /** 백엔드가 준 안내 문구. 화면이 새로 만들지 않는다. */
    public static final String MESSAGE = "failMessage";

    /**
     * 안내를 붙일 칸 이름. 가릴 수 없으면 {@link #FIELD_FORM} 이다.
     *
     * <p>값은 폼 칸 이름과 같게 둔다 — 템플릿이 칸 옆에서 {@code failField == 'email'} 하나로
     * 판단할 수 있다.
     */
    public static final String FIELD = "failField";

    /** 폼 상단. 어느 칸인지 가릴 수 없는 실패가 여기로 온다. */
    public static final String FIELD_FORM = "form";

    public static final String FIELD_MEMBER_ID = "memberId";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_PASSWORD_CONFIRM = "passwordConfirm";

    private FormFailure() {
    }

    /**
     * 응답 코드가 가리키는 칸을 돌려준다. 가릴 수 없으면 {@link #FIELD_FORM}.
     *
     * <p>표에 없는 코드가 상단으로 가는 것이 기본값이다. 새 코드가 생겨도 화면은 안내를
     * 잃지 않고 자리만 상단이 된다.
     */
    public static String fieldOf(int resCode) {
        if (resCode == ErrorCode.MEMBER_ID_DUPLICATED.code()) {
            return FIELD_MEMBER_ID;
        }
        if (resCode == ErrorCode.EMAIL_DUPLICATED.code()) {
            return FIELD_EMAIL;
        }
        if (resCode == ErrorCode.PASSWORD_RULE_VIOLATION.code()) {
            return FIELD_PASSWORD;
        }
        if (resCode == ErrorCode.PASSWORD_CONFIRM_MISMATCH.code()) {
            return FIELD_PASSWORD_CONFIRM;
        }
        return FIELD_FORM;
    }

    /**
     * 실패 한 건을 모델에 담는다. 폼을 가진 컨트롤러의 실패 선언이 이것만 부른다.
     *
     * <p>폼 객체를 받지 않는다 — 받으면 비밀번호가 이 자리를 지나게 되고, 지나는 값은 언젠가
     * 로그에 찍힌다.
     */
    public static void applyTo(Model model, BackendApiException exception) {
        applyTo(model, exception.getResCode(), exception.getMessage());
    }

    /**
     * 코드와 문구를 직접 담는다. 백엔드 호출 없이 화면이 실패를 알릴 때 쓴다 — 이미 정지된
     * 회원이라 부를 필요가 없는 경우 같은 것이다.
     */
    public static void applyTo(Model model, int resCode, String message) {
        model.addAttribute(RES_CODE, resCode);
        model.addAttribute(MESSAGE, message);
        model.addAttribute(FIELD, fieldOf(resCode));
    }

    /**
     * 안내를 <b>상단에 고정</b>한다. 코드가 칸을 가리키더라도 상단에 둔다.
     *
     * <p>로그인 실패가 이쪽이다. {@code 1003} 은 표에 없어 어차피 상단이지만, 뒤에 코드가
     * 늘어 우연히 칸을 얻는 일이 없도록 부르는 쪽이 못 박는다.
     */
    public static void applyToFormTop(Model model, BackendApiException exception) {
        model.addAttribute(RES_CODE, exception.getResCode());
        model.addAttribute(MESSAGE, exception.getMessage());
        model.addAttribute(FIELD, FIELD_FORM);
    }
}
