package com.dbdomino.moneylog.backend.service;

/**
 * 아이디 찾기(1.9·1.10) 응답에 실을 아이디를 가린다.
 *
 * <p>이메일만 알면 남의 아이디를 알아낼 수 있으면 안 되지만, 본인은 자기 아이디를
 * 알아볼 수 있어야 한다. 그래서 <b>앞뒤를 남기고 가운데를 가린다</b>.
 *
 * <p>{@code SensitiveMasker}(로그 마스킹)와 규칙을 공유하지 않는다. 그쪽은 값 전체를
 * {@code ***} 로 지우는 것이 목적이라 아무도 알아볼 수 없어야 하고, 이쪽은 본인만
 * 알아볼 수 있어야 한다 — 목적이 반대다.
 *
 * <p><b>결과가 원문과 반드시 달라야 한다</b>(SC-109). 아이디는 최소 4자(FR-102)지만
 * 짧은 값에서도 그 성질이 깨지지 않도록 남길 글자 수를 길이에 맞춰 줄인다.
 */
public final class MemberIdMasker {

    /** 가림 표시. 길이를 노출하지 않도록 원문 길이와 무관하게 고정한다. */
    private static final String MASK = "***";

    /** 앞에 남길 글자 수(기본). */
    private static final int HEAD = 3;

    /** 뒤에 남길 글자 수(기본). */
    private static final int TAIL = 2;

    private MemberIdMasker() {
    }

    /**
     * 아이디를 가린다. 예: {@code user01} → {@code use***01}
     *
     * <p>남길 앞뒤 글자 수의 합이 원문 길이 이상이면 그만큼 줄인다. 줄여도 남길 자리가
     * 없으면 전부 가린다 — 그 편이 "가렸는데 원문과 같다"보다 낫다.
     */
    public static String mask(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return MASK;
        }
        int length = memberId.length();
        int head = HEAD;
        int tail = TAIL;
        // 앞뒤로 남긴 것이 원문 전체가 되면 가린 의미가 없다. 한 글자 이상은 반드시 가린다.
        while (head + tail >= length && (head > 0 || tail > 0)) {
            if (tail > 0) {
                tail--;
            } else {
                head--;
            }
        }
        if (head == 0 && tail == 0) {
            return MASK;
        }
        return memberId.substring(0, head) + MASK + memberId.substring(length - tail);
    }
}
