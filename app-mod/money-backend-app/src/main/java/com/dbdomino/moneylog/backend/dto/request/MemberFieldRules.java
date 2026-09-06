package com.dbdomino.moneylog.backend.dto.request;

import java.util.regex.Pattern;

/**
 * 회원 필드의 형식 규칙. 가입(1.2)·본인 수정(1.8)·관리자 추가·수정(1.12·1.15)이 공유한다.
 *
 * <p><b>DB 가 막아주지 않는 규칙들이다</b>(data-model.md §1). 컬럼에는 길이 제한만 있고
 * 문자 범위·최소 길이·비밀번호 구성은 없다. 그래서 애플리케이션이 유일한 방어선이며,
 * 규칙이 여러 API 에 흩어지면 한 곳만 느슨해져도 데이터가 갈린다.
 *
 * <h2>실패 코드가 규칙마다 다르다</h2>
 *
 * <p>비밀번호 규칙 위반만 {@code 2004} 이고 나머지 형식 오류는 {@code 9001} 이다.
 * 그래서 비밀번호는 Bean Validation 이 아니라 서비스가 검사한다 — Bean Validation 실패는
 * 전역 처리에서 {@code 9001} 로 나가기 때문이다.
 */
public final class MemberFieldRules {

    /** 로그인 아이디 — 4~20자 영문·숫자·{@code _}(FR-102). */
    public static final String MEMBER_ID_REGEX = "^[A-Za-z0-9_]{4,20}$";

    /** 전화번호 — 하이픈 없이 숫자만(FR-107). 길이는 국내·국제를 모두 수용할 만큼만 둔다. */
    public static final String PHONE_REGEX = "^[0-9]{9,15}$";

    /** 닉네임 최소·최대 길이(FR-102). */
    public static final int NICKNAME_MIN = 2;
    public static final int NICKNAME_MAX = 20;

    /** 비밀번호 최소 길이(FR-103). */
    public static final int PASSWORD_MIN = 8;

    /** 비밀번호에 필요한 문자 종류 수(FR-103). */
    private static final int PASSWORD_REQUIRED_KINDS = 3;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    /** 특수문자 — 영문·숫자·공백이 아닌 모든 문자. 허용 목록을 좁히면 쓸 수 있는 암호가 준다. */
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    private MemberFieldRules() {
    }

    /**
     * 비밀번호 규칙 — 8자 이상이고 대문자·소문자·숫자·특수문자 중 <b>3종류 이상</b>.
     *
     * <p>위반은 {@code 2004} 다.
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            return false;
        }
        int kinds = 0;
        if (UPPER.matcher(password).find()) {
            kinds++;
        }
        if (LOWER.matcher(password).find()) {
            kinds++;
        }
        if (DIGIT.matcher(password).find()) {
            kinds++;
        }
        if (SPECIAL.matcher(password).find()) {
            kinds++;
        }
        return kinds >= PASSWORD_REQUIRED_KINDS;
    }

    /** 닉네임 길이 검사. 위반은 {@code 9001} 이다. */
    public static boolean isValidNickname(String nickname) {
        if (nickname == null) {
            return false;
        }
        int length = nickname.trim().length();
        return length >= NICKNAME_MIN && length <= NICKNAME_MAX;
    }

    /** 전화번호 검사. {@code null} 은 허용된다 — 선택 항목이다. */
    public static boolean isValidPhone(String phone) {
        return phone == null || phone.matches(PHONE_REGEX);
    }
}
