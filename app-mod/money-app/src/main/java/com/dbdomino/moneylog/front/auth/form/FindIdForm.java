package com.dbdomino.moneylog.front.auth.form;

/**
 * 1.3 아이디 찾기 폼. 이메일 한 칸이다.
 *
 * <p>형식 검사를 화면이 하지 않는다. 두 곳에서 검사하면 규칙이 갈리고, 갈린 순간 화면은
 * 통과시킨 값을 백엔드가 거절하거나 그 반대가 된다.
 *
 * @param email 가입할 때 등록한 이메일
 */
public record FindIdForm(String email) {
}
