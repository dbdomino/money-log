package com.dbdomino.moneylog.backend.support;

import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 006 의 연·월 검증 — <b>2000~2100 을 한 곳에 가둔다</b>(FR-525).
 *
 * <h2>왜 별도 자리인가</h2>
 *
 * <p>이 범위가 <b>다섯 곳</b>에 걸린다 — 5.1 목표금액 목록 · 5.2 단건 조회 ·
 * 5.4 월별 목표 upsert · 5.5 통계 조회 · 5.6 통계 저장(api-contract §5). 각 DTO 가
 * 숫자를 직접 적으면 한 곳만 고쳐도 나머지 넷이 갈리고, 그 갈림은 <b>경계값에서만</b>
 * 드러나 평소에는 보이지 않는다.
 *
 * <h2>연 범위가 005 와 다르다</h2>
 *
 * <p>{@link YearMonthValue} 는 005 가 만들었고 기본 범위가 <b>1900~9999</b> 다. 006 은
 * 그보다 좁으므로 {@code require(year, month, code, min, max)} 오버로드로 자기 범위를
 * 넘긴다 — <b>005 의 동작을 바꾸지 않는다</b>. 그쪽 호출부 넷(4.5·4.6·4.8·4.9)이 딸려
 * 움직이기 때문이다.
 *
 * <h2>실패 코드가 {@code 3603} 이다</h2>
 *
 * <p>같은 "연·월 범위 오류"인데 005 의 고정지출은 {@code 3403}, 가계부 목록은
 * {@code 3501} 이다. 자원별 코드 블록 배정({@code 34xx}/{@code 35xx}/{@code 36xx})의
 * 결과이며 <b>의도된 차이</b>다.
 *
 * <p>{@code 3603} 은 <b>{@code view} 값 오류에도 쓴다</b>(api-contract §6) — 두 쓰임이
 * 한 코드를 공유하는 것도 의도다.
 *
 * @see <a href="../../../../../../../../specs/006-backend-target-statistics/contracts/api-contract.md">api-contract.md §5</a>
 */
public final class StatisticsYearMonth {

    /**
     * 허용 연도의 하한.
     *
     * <p><b>DB 가 막아주지 않는다.</b> {@code ck_statistics_month}·
     * {@code ck_target_monthly_month} 는 <b>월만</b> 검사하므로(덤프 확인)
     * {@code year=0} 이나 {@code year=999999} 가 그대로 들어온다. 이 검사가 유일한 방어선이다.
     */
    public static final int MIN_YEAR = 2000;

    /** @see #MIN_YEAR */
    public static final int MAX_YEAR = 2100;

    private StatisticsYearMonth() {
    }

    /**
     * 006 의 연·월을 검증한다. 범위 밖이거나 누락이면 {@code 3603} 이다.
     *
     * <p>연·월을 받는 다섯 곳이 <b>전부 이 메서드를 부른다</b> — 한 곳이라도 직접
     * 검증하면 그 API 만 다른 범위를 갖게 된다.
     */
    public static YearMonthValue require(Integer year, Integer month) {
        return YearMonthValue.require(year, month, ErrorCode.STATISTICS_PARAM_INVALID,
                MIN_YEAR, MAX_YEAR);
    }
}
