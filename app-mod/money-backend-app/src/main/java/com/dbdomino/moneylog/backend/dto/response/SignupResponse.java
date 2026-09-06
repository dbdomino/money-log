package com.dbdomino.moneylog.backend.dto.response;

/**
 * 1.2 회원가입 응답.
 *
 * <p>가입 직후에는 아이디·닉네임·권한 셋만 돌려준다. 이메일·연락처는 방금 보낸 값이라
 * 되돌려 줄 이유가 없고, 프로필 전체가 필요하면 로그인 후 1.7 을 쓴다.
 *
 * @param memberId 가입된 아이디
 * @param nickname 닉네임
 * @param role     항상 {@code 3}(일반). 가입으로 관리자를 만들 수 없다
 */
public record SignupResponse(String memberId, String nickname, short role) {
}
