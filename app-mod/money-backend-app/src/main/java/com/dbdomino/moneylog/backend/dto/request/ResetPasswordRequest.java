package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 1.11 비밀번호 재설정 요청.
 *
 * <p>1.10 을 거쳤는지 서버는 알지 못하고 알 필요도 없다. 재설정 토큰·인증코드가 존재하지
 * 않으므로 <b>같은 두 값을 여기서 다시 검증한다</b>(FR-124). 1.10 을 건너뛰고 이 API 만
 * 직접 불러도 판정은 같다.
 *
 * @param memberId           로그인 아이디
 * @param nickname           닉네임. 1.10 과 같은 대조를 다시 한다
 * @param newPassword        새 비밀번호 평문. 규칙 위반은 {@code 2004}
 * @param newPasswordConfirm 새 비밀번호 확인. 불일치는 {@code 2005}
 */
public record ResetPasswordRequest(
        @NotBlank String memberId,
        @NotBlank String nickname,
        @NotBlank String newPassword,
        @NotBlank String newPasswordConfirm) {
}
