package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1.2 회원가입 요청.
 *
 * <p><b>여기에 붙은 검증은 전부 {@code 9001} 로 나간다.</b> 비밀번호 규칙 위반({@code 2004})과
 * 확인 불일치({@code 2005})는 코드가 달라야 하므로 애너테이션으로 막지 않고
 * {@code AuthService} 가 {@link MemberFieldRules} 로 검사한다.
 *
 * <p>{@code role} 을 받지 않는다. 가입은 항상 일반 회원({@code 3})이고 관리자는 가입으로
 * 만들 수 없다(FR-105) — 필드가 없으면 요청이 권한을 지정할 방법 자체가 없다.
 *
 * @param memberId        로그인 아이디. 4~20자 영문·숫자·{@code _}
 * @param password        비밀번호 평문. 규칙 검사 후 bcrypt 해시로만 저장한다
 * @param passwordConfirm 비밀번호 확인. {@code password} 와 같아야 한다
 * @param nickname        닉네임. 2~20자
 * @param email           이메일(선택). 값이 있을 때만 중복 검사한다
 * @param phone           연락처(선택). 하이픈 없이 숫자만
 * @param intro           자기소개(선택)
 */
public record SignupRequest(
        @NotBlank @Pattern(regexp = MemberFieldRules.MEMBER_ID_REGEX) String memberId,
        @NotBlank String password,
        @NotBlank String passwordConfirm,
        @NotBlank @Size(min = MemberFieldRules.NICKNAME_MIN, max = MemberFieldRules.NICKNAME_MAX)
        String nickname,
        @Email @Size(max = 100) String email,
        @Pattern(regexp = MemberFieldRules.PHONE_REGEX) String phone,
        @Size(max = 500) String intro) {
}
