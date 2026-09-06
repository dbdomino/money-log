package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 1.10 비밀번호 찾기 요청.
 *
 * <p><b>비밀번호 필드가 없다.</b> 이 API 는 본인 계정의 존재만 확인하고, 실제 변경은
 * 1.11 이 한다. 본인 확인 수단은 {@code memberId}+{@code nickname} 대조 하나뿐이며
 * 이메일 인증·보안질문 같은 외부 채널은 이 기능의 범위 밖이다(FR-124).
 *
 * @param memberId 로그인 아이디
 * @param nickname 닉네임. 본인 확인용으로 아이디와 함께 대조한다
 */
public record FindPasswordRequest(@NotBlank String memberId, @NotBlank String nickname) {
}
