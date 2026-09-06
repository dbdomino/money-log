package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 1.9 아이디 찾기 요청.
 *
 * <p>형식이 어긋나면 {@code 9001}, 형식은 맞는데 그 이메일의 회원이 없으면 {@code 2001}
 * 이다(api-contract.md §5). 두 실패를 한 코드로 합치지 않는다 — 사용자가 "오타"와
 * "가입 안 함"을 구분해야 다음 행동이 갈린다.
 *
 * @param email 가입할 때 등록한 이메일
 */
public record FindIdRequest(@NotBlank @Email String email) {
}
