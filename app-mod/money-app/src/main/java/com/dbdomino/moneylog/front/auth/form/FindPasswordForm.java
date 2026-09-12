package com.dbdomino.moneylog.front.auth.form;

/**
 * 1.4 비밀번호 찾기 폼. 아이디·닉네임 두 칸이다.
 *
 * <p><b>비밀번호 칸이 없다.</b> 이 화면은 본인을 확인만 하고, 새 비밀번호는 1.5 가 받는다.
 * 두 일을 한 화면에서 하면 확인에 실패했을 때 사용자가 고른 비밀번호가 함께 날아간다.
 *
 * @param memberId 로그인 아이디
 * @param nickname 닉네임. 아이디와 함께 본인임을 확인하는 값이다
 */
public record FindPasswordForm(String memberId, String nickname) {
}
