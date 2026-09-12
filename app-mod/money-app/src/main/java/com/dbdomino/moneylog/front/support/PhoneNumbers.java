package com.dbdomino.moneylog.front.support;

/**
 * 폰에서 <b>숫자만 남긴다</b>. 가입·본인 수정·관리자 회원 폼 셋이 함께 쓴다.
 *
 * <h2>브라우저에 맡기지 않는다</h2>
 *
 * <p>입력 중의 편의(숫자 자판 띄우기, 하이픈 자동 넣기)는 브라우저가 맡되 <b>보장은 서버가
 * 한다.</b> 브라우저에서만 걷어내면 스크립트가 막힌 환경에서 하이픈이 그대로 나가고, 백엔드는
 * 형식 오류로 거절한다. 그 실패는 "어떤 사용자에게만 가입이 안 된다"로 나타나 재현이 어렵다.
 *
 * <p>두 곳에서 하는 것이 아니라 <b>보장하는 자리를 한 곳으로 정하는 것</b>이다.
 *
 * <h2>비어 있는 것과 지우는 것을 가르지 않는다</h2>
 *
 * <p>여기서는 값의 모양만 다듬는다. 빈 값을 "유지"로 볼지 "비움"으로 볼지는 화면이 정하며,
 * 그 판단은 칸마다 다르다.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * 숫자가 아닌 문자를 전부 버린다.
     *
     * <p>하이픈뿐 아니라 공백·괄호·점도 함께 버린다. 사용자가 어떻게 치든 저장되는 값은
     * 하나여야 하고, 허용할 구분자를 나열하면 빠뜨린 것이 그대로 백엔드로 나간다.
     *
     * @param raw 사용자가 입력한 값. {@code null} 이어도 된다
     * @return 숫자만 남은 값. 입력이 {@code null} 이거나 숫자가 하나도 없으면 {@code null}
     */
    public static String digitsOnly(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        return digits.isEmpty() ? null : digits.toString();
    }

    /**
     * 사용자가 칸을 <b>비웠는지</b> 본다. 공백만 친 것도 비운 것으로 본다.
     *
     * <p>비운 칸을 어떻게 보낼지는 화면이 정한다 — 폰·이메일·소개는 비우라는 값으로 싣고,
     * 새 비밀번호는 아예 싣지 않는다.
     */
    public static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }
}
