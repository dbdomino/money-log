package com.dbdomino.moneylog.backend.dto.response;

import com.dbdomino.moneylog.backend.service.MemberSessionService.IssuedTokens;
import com.dbdomino.moneylog.data.entity.User;

/**
 * 1.3 로그인 응답.
 *
 * <p>{@link TokenResponse} 의 다섯 필드에 회원 식별 정보 셋을 더한 평면 구조다.
 * 계약이 중첩 없이 한 겹으로 정했다.
 *
 * <p><b>비밀번호는 필드로 두지 않는다.</b> 응답에 해시가 실리지 않는 것을 DTO 구조가
 * 보장한다(SC-107).
 *
 * @param memberId         로그인한 아이디
 * @param nickname         닉네임
 * @param role             {@code 1} 관리자, {@code 3} 일반
 * @param accessToken      JWT
 * @param tokenType        {@code Bearer} 고정
 * @param expiresIn        Access 만료까지 남은 초
 * @param refreshToken     불투명 랜덤 문자열
 * @param refreshExpiresIn Refresh 만료까지 남은 초
 */
public record LoginResponse(String memberId, String nickname, short role,
                            String accessToken, String tokenType, long expiresIn,
                            String refreshToken, long refreshExpiresIn) {

    public static LoginResponse of(User user, IssuedTokens tokens) {
        return new LoginResponse(
                user.getUserId(), user.getNickname(), user.getRole(),
                tokens.accessToken(), TokenResponse.BEARER, tokens.accessExpiresInSeconds(),
                tokens.refreshToken(), tokens.refreshExpiresInSeconds());
    }
}
