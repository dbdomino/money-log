package com.dbdomino.moneylog.front.auth.form;

/**
 * 1.1 로그인 폼. 칸 둘이다.
 *
 * <p>화면마다 폼을 나누는 이유는 합치면 <b>그 화면이 보내지 않는 칸까지 검증 규칙이
 * 따라붙기 때문</b>이다. 로그인 폼에 가입 폼의 닉네임 규칙이 걸리면 로그인이 닉네임 때문에
 * 거절된다.
 *
 * <p>규칙 검사를 여기 걸지 않는다. 로그인은 <b>이미 저장된</b> 비밀번호를 확인하는 자리라,
 * 화면이 규칙을 들고 있으면 규칙이 바뀌기 전에 가입한 사용자가 로그인하지 못한다. 필수 여부는
 * 백엔드가 판정하고 화면은 그 응답을 보인다.
 *
 * @param memberId 로그인 아이디
 * @param password 비밀번호 평문. <b>모델에도 로그에도 담지 않는다</b>
 */
public record LoginForm(String memberId, String password) {
}
