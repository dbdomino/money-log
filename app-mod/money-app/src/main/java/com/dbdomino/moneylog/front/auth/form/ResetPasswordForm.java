package com.dbdomino.moneylog.front.auth.form;

/**
 * 1.5 비밀번호 변경 폼. 새 비밀번호와 확인 두 칸이다.
 *
 * <p><b>아이디·닉네임 칸을 두지 않는다.</b> 그 값은 1.4 가 담아 둔 재설정 표식에서 오고,
 * 사용자는 입력하지 않는다(FR-712). 폼에 칸을 두면 사용자에게 다시 묻는 화면이 된다.
 *
 * <p>저장 요청에는 네 값이 함께 실린다. 표식의 두 값을 화면이 숨은 값으로도 싣지만, 사용자가
 * 그것을 바꿔도 <b>백엔드가 저장 시점에 다시 대조하므로</b> 남의 비밀번호를 바꿀 수 없다.
 *
 * @param newPassword 새 비밀번호 평문
 * @param newPasswordConfirm 새 비밀번호 확인. 백엔드가 일치를 판정한다
 */
public record ResetPasswordForm(String newPassword, String newPasswordConfirm) {

    /**
     * 표식의 아이디·닉네임과 합쳐 백엔드 요청으로 만든다.
     *
     * <p>합치는 자리를 폼에 두는 이유는 네 값이 <b>함께</b> 나가야 한다는 것을 한 곳에서
     * 보이게 하기 위해서다. 컨트롤러가 손으로 조립하면 어느 값을 빠뜨렸는지 읽어서 세야 한다.
     */
    public Request toRequest(String memberId, String nickname) {
        return new Request(memberId, nickname, newPassword, newPasswordConfirm);
    }

    /** 백엔드 {@code MemberResetPassword} 요청 본문. */
    public record Request(
            String memberId,
            String nickname,
            String newPassword,
            String newPasswordConfirm) {
    }
}
