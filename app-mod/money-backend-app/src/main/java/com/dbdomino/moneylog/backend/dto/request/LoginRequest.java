package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 1.3 로그인 요청.
 *
 * <p>누락은 {@code 9001} 이다 — Bean Validation 실패를 {@code GlobalExceptionHandler} 가
 * 그 코드로 바꾼다. 아이디·비밀번호가 <b>틀린</b> 것({@code 1003})과는 다른 실패다.
 *
 * @param memberId 로그인 아이디({@code tbl_user.user_id})
 * @param password 비밀번호 평문. bcrypt 해시와 대조만 하고 어디에도 남기지 않는다
 */
public record LoginRequest(
        @NotBlank String memberId,
        @NotBlank String password) {
}
