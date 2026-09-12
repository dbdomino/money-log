package com.dbdomino.moneylog.front.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 백엔드 {@code MemberSignup} 성공 응답.
 *
 * <p>화면이 쓰는 것은 <b>아이디 하나</b>다 — 로그인 화면으로 넘겨 아이디 칸을 채운다. 닉네임과
 * 권한은 계약에 있어 받아 두지만 가입 직후 화면에 쓰이지 않는다.
 *
 * <p>비밀번호는 응답에 없다. 백엔드가 내려주지 않고 화면도 담을 자리를 두지 않는다.
 *
 * @param memberId 가입된 아이디
 * @param nickname 닉네임
 * @param role 권한. 가입은 언제나 {@code 3} 일반이다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SignupResult(String memberId, String nickname, int role) {
}
