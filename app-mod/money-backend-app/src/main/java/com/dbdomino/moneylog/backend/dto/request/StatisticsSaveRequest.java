package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.backend.support.StatisticsYearMonth;
import com.dbdomino.moneylog.backend.support.YearMonthValue;

/**
 * 5.6 통계 저장의 Body — 연·월.
 *
 * <h2>연·월을 Body 로만 받는다 (FR-524)</h2>
 *
 * <p>5.5 는 Path 로 받는데 5.6 은 Body 다. 나뉘는 이유는 <b>메서드가 다르기</b> 때문이다 —
 * GET 은 자원을 가리키므로 좌표가 Path 에 있고, POST 는 행위를 보내므로 대상이 Body 에 있다.
 *
 * <p><b>Query 를 조용히 읽어 주지 않는다.</b> 읽어 주면 입력 경로가 둘이 되고, 둘이 다른
 * 값을 담았을 때 어느 쪽을 따르는지가 구현에 숨는다. Query·Path 로만 보낸 요청은 Body 가
 * 비어 {@code 3603} 이다.
 *
 * <h2>{@code Integer} 로 받는다</h2>
 *
 * <p>원시 타입이면 Jackson 이 {@code 0} 을 채워 <b>"보내지 않았다"를 가릴 수 없다</b> —
 * {@code year=0} 은 범위 밖이라 어차피 {@code 3603} 이지만, {@code month} 를 빠뜨리면
 * {@code 0} 이 되어 "월 범위 오류"로 읽히고 진짜 원인(필드 누락)이 가려진다.
 *
 * @param year  저장 대상 연도. {@code 2000 ~ 2100}
 * @param month 저장 대상 월. {@code 1 ~ 12}
 */
public record StatisticsSaveRequest(Integer year, Integer month) {

    /**
     * 값을 검증해 연·월을 만든다.
     *
     * <p><b>{@code request} 자체가 {@code null} 일 수 있다</b> — 컨트롤러가 Body 를
     * 선택으로 받기 때문이다. 몸통을 아예 안 보낸 요청과 {@code {}} 를 보낸 요청이 같은
     * {@code 3603} 이어야 "Body 로만 받는다"가 한 가지 결과로 드러난다.
     *
     * @throws com.dbdomino.moneylog.common.error.BusinessException {@code 3603}
     */
    public static YearMonthValue yearMonthOf(StatisticsSaveRequest request) {
        if (request == null) {
            return StatisticsYearMonth.require(null, null);
        }
        return StatisticsYearMonth.require(request.year(), request.month());
    }
}
