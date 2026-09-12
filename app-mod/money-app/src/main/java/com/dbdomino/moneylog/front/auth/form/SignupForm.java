package com.dbdomino.moneylog.front.auth.form;

import com.dbdomino.moneylog.front.support.PhoneNumbers;

/**
 * 1.2 회원가입 폼. 칸 일곱이고 필수는 앞의 넷이다.
 *
 * <p>폰은 사용자가 하이픈을 넣어도 <b>보낼 때 숫자만 남긴다</b>. 걷어내는 자리를 화면 모듈
 * 한 곳으로 정해 두면 브라우저 스크립트가 막힌 환경에서도 저장값이 같다.
 *
 * @param memberId 로그인 아이디 (필수)
 * @param password 비밀번호 평문 (필수)
 * @param passwordConfirm 비밀번호 확인 (필수). 백엔드가 일치를 판정한다
 * @param nickname 닉네임 (필수)
 * @param email 이메일 (선택)
 * @param phone 연락처 (선택). 사용자가 친 그대로다 — 숫자만 남기는 것은 {@link #toRequest()}
 * @param intro 자기소개 (선택)
 */
public record SignupForm(
        String memberId,
        String password,
        String passwordConfirm,
        String nickname,
        String email,
        String phone,
        String intro) {

    /**
     * 백엔드 가입 요청으로 옮긴다.
     *
     * <p>선택 칸을 비워 보냈으면 <b>싣지 않는다</b>(값이 {@code null} 이 된다). 가입은 새로
     * 만드는 자리라 "비운다"와 "안 넣는다"가 결과가 같고, 빈 문자열을 보내면 백엔드가 형식
     * 오류로 볼 수 있다.
     */
    public Request toRequest() {
        return new Request(
                memberId,
                password,
                passwordConfirm,
                nickname,
                blankToNull(email),
                PhoneNumbers.digitsOnly(phone),
                blankToNull(intro));
    }

    private static String blankToNull(String value) {
        return PhoneNumbers.isBlank(value) ? null : value;
    }

    /** 백엔드 {@code MemberSignup} 요청 본문. 폼과 따로 두어 칸 이름이 계약을 따라가게 한다. */
    public record Request(
            String memberId,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            String phone,
            String intro) {
    }
}
