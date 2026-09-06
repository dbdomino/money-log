package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1.12 관리자 회원 추가 요청.
 *
 * <p>가입(1.2)과 다른 점은 <b>{@code role} 을 지정한다</b>는 것 하나다. 관리자는 관리자를
 * 만들 수 있다 — 가입으로는 만들 수 없는 계정을 만드는 것이 이 API 의 존재 이유다.
 * {@code 1}·{@code 3} 이 아닌 값은 {@code 9001} 이며, DB 의 {@code ck_user_role} 이
 * 최종 방어선이다.
 *
 * <p>비밀번호 확인 필드가 없다. 관리자가 발급하는 계정이라 오타 확인 절차를 요구하지
 * 않는다 — 대신 규칙 검증({@code 2004})은 가입과 똑같이 한다.
 *
 * @param memberId 로그인 아이디. 4~20자 영문·숫자·{@code _}
 * @param password 비밀번호 평문. bcrypt 해시로만 저장한다
 * @param nickname 닉네임. 2~20자
 * @param role     권한. {@code 1} 관리자, {@code 3} 일반
 * @param email    이메일(선택)
 * @param phone    연락처(선택). 하이픈 없이 숫자만
 * @param intro    자기소개(선택)
 */
public record AdminMemberCreateRequest(
        @NotBlank @Pattern(regexp = MemberFieldRules.MEMBER_ID_REGEX) String memberId,
        @NotBlank String password,
        @NotBlank @Size(min = MemberFieldRules.NICKNAME_MIN, max = MemberFieldRules.NICKNAME_MAX)
        String nickname,
        @NotNull Short role,
        @Email @Size(max = 100) String email,
        @Pattern(regexp = MemberFieldRules.PHONE_REGEX) String phone,
        @Size(max = 500) String intro) {
}
