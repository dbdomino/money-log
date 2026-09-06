package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 5.5 의 {@code view} — 조회 모드 스위치.
 *
 * <pre>{@code
 * 생략 · saved  →  저장본이 있으면 저장본(SAVED), 없으면 즉석 계산(CALCULATED)
 * live          →  저장본을 읽지도 쓰지도 않고 즉석 계산. source 는 항상 CALCULATED
 * 그 밖의 값     →  3603
 * }</pre>
 *
 * <h2>생략과 {@code saved} 가 같은 동작이다</h2>
 *
 * <p>{@code saved} 가 <b>명시적 값으로도 존재한다</b>는 사실을 놓치면 구현자가
 * {@code ?view=saved} 를 알 수 없는 값으로 보고 거절한다 — 프론트가 기본값을 명시해
 * 보내는 것이 흔한 습관이라 실제로 걸린다.
 *
 * <h2>모르는 값을 조용히 무시하지 않는다</h2>
 *
 * <p>{@code view=lives} 같은 오타를 기본 동작으로 넘기면 사용자는 <b>최신값을 본다고
 * 믿는데 저장본을 보고 있다.</b> 화면에는 아무 이상이 없어 알아챌 방법이 없다.
 *
 * <h2>Path 와 Query 를 함께 쓰는 유일한 API 다</h2>
 *
 * <p>연·월은 Path 이고 {@code view} 는 Query 다. {@code view} 가 <b>자원 식별자가 아니라
 * 모드 스위치</b>라 허용한 예외다(api-contract §6).
 */
public enum StatisticsViewQuery {

    /** 저장본 우선. 생략과 같다. */
    SAVED,

    /** 저장본을 무시하고 즉석 계산한다. <b>DB 를 바꾸지 않는다.</b> */
    LIVE;

    private static final String SAVED_VALUE = "saved";
    private static final String LIVE_VALUE = "live";

    /**
     * 쿼리 값을 모드로 바꾼다.
     *
     * <p><b>{@code null} 과 빈 문자열은 {@link #SAVED} 다.</b> {@code ?view=} 처럼 값 없이
     * 온 요청을 오류로 보면 생략과 결과가 갈리는데, 둘 다 "지정하지 않았다"이므로 같은
     * 동작이어야 한다.
     *
     * @throws BusinessException {@code 3603} — 셋 중 어느 것도 아닌 값
     */
    public static StatisticsViewQuery of(String view) {
        if (view == null || view.isBlank()) {
            return SAVED;
        }
        String normalized = view.trim().toLowerCase();
        if (SAVED_VALUE.equals(normalized)) {
            return SAVED;
        }
        if (LIVE_VALUE.equals(normalized)) {
            return LIVE;
        }
        throw new BusinessException(ErrorCode.STATISTICS_PARAM_INVALID,
                "view 는 saved 또는 live 여야 합니다.");
    }

    /** 저장본을 읽어도 되는 모드인가. {@link #LIVE} 는 읽지 않는다. */
    public boolean prefersSaved() {
        return this == SAVED;
    }
}
