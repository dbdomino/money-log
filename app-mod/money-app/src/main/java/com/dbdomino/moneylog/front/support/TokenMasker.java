package com.dbdomino.moneylog.front.support;

/**
 * 토큰과 비밀번호를 로그에 남기기 전에 가린다.
 *
 * <p><b>값을 자르지 않고 통째로 가린다.</b> 앞뒤 몇 글자를 남기는 방식은 같은 토큰인지
 * 대조할 수 있게 해 주지만, 그 대조가 가능하다는 것은 로그를 본 사람이 토큰을 구분할 수
 * 있다는 뜻이다. 서명 알고리즘과 발급자가 드러나는 앞부분은 특히 남길 이유가 없다.
 *
 * <p>화면 모듈이 토큰을 로그로 넘기는 자리는 원래 없다 — 백엔드 호출 로그는 경로와 응답
 * 코드만 남기고 헤더·본문을 건드리지 않는다. 이 클래스는 그 규칙을 어기게 되는 자리가
 * 생겼을 때 쓰는 마지막 안전장치다.
 */
public final class TokenMasker {

    /** 가린 자리에 남기는 표시. 길이로 원래 값을 짐작할 수 없게 고정 문자열을 쓴다. */
    public static final String MASK = "****";

    private TokenMasker() {
    }

    /** 값이 있으면 가리고, 없으면 없다는 것만 남긴다. */
    public static String mask(String secret) {
        return secret == null || secret.isBlank() ? "(없음)" : MASK;
    }

    /** {@code Bearer abc...} 같은 인증 헤더 값을 통째로 가린다. */
    public static String maskAuthorization(String headerValue) {
        return headerValue == null || headerValue.isBlank() ? "(없음)" : MASK;
    }
}
