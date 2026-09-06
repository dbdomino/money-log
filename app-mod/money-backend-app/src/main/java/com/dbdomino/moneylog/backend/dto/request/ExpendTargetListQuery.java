package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.backend.support.StatisticsYearMonth;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 5.1 목표금액 목록의 조회 조건.
 *
 * <p><b>006 의 여섯 API 중 페이징이 있는 것은 5.1 뿐이다</b>(api-contract §3). 통계
 * 응답의 배열 3종은 한 통계 객체의 구성 요소라 {@code data.list} 규칙의 대상이 아니다.
 *
 * <h2>두 실패 코드가 다르다</h2>
 *
 * <table border="1">
 *   <caption>같은 요청의 두 검증</caption>
 *   <tr><th>어긋난 값</th><th>코드</th></tr>
 *   <tr><td>{@code year}·{@code month}</td><td><b>{@code 3603}</b></td></tr>
 *   <tr><td>{@code offset}·{@code limit}</td><td><b>{@code 9001}</b></td></tr>
 * </table>
 *
 * <p>연·월은 이 기능이 다루는 <b>자원의 좌표</b>라 자원별 코드({@code 3603})를 쓰고,
 * 페이징은 어느 목록에나 붙는 <b>요청 형식</b>이라 공통 코드({@code 9001})를 쓴다.
 * 002 의 관리자 목록(1.13)·005 의 4.2 와 같은 규칙이다.
 *
 * <p>{@code offset}·{@code limit} 은 <b>필수이고 기본값이 없다</b> — 그래서 Spring Data
 * 의 {@code Pageable} 자동 바인딩을 쓰지 않는다. 그쪽은 값이 빠지면 기본값을 채워 조용히
 * 통과시키는데, 우리 계약은 빠졌으면 {@code 9001} 로 거절해야 한다.
 *
 * @param yearMonth       조회 연·월. 월별 목표({@code monthlyTargetAmount})의 기준이다
 * @param offset          건너뛸 건수. 0 이상이고 {@code limit} 의 배수
 * @param limit           가져올 건수. 1 이상
 * @param expendGroupName 지출유형 이름 <b>부분 일치</b> 검색어. 선택이며 없으면 {@code null}
 */
public record ExpendTargetListQuery(YearMonthValue yearMonth, int offset, int limit,
                                    String expendGroupName) {

    /**
     * 값을 검증해 조회 조건을 만든다.
     *
     * <p><b>연·월을 먼저 본다.</b> 자원 좌표가 성립하지 않으면 페이징을 따질 이유가 없다.
     *
     * @throws BusinessException {@code 3603} — 연·월 누락·범위 오류
     * @throws BusinessException {@code 9001} — 페이징 누락·범위 오류·배수 아님
     */
    public static ExpendTargetListQuery of(Integer year, Integer month, Integer offset,
                                           Integer limit, String expendGroupName) {
        YearMonthValue yearMonth = StatisticsYearMonth.require(year, month);
        if (offset == null || limit == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 과 limit 은 필수입니다.");
        }
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 은 1 이상이어야 합니다.");
        }
        if (offset < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 은 0 이상이어야 합니다.");
        }
        if (offset % limit != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 은 limit 의 배수여야 합니다.");
        }
        return new ExpendTargetListQuery(yearMonth, offset, limit, blankToNull(expendGroupName));
    }

    /**
     * 검색어가 걸려 있는가.
     *
     * <p>빈 문자열을 {@code null} 로 접는 것은 {@code ?expendGroupName=} 처럼 값 없이
     * 온 요청이 "빈 문자열을 포함하는 이름"을 찾는 검색으로 해석되지 않게 하기 위해서다 —
     * 그 조건은 모든 이름에 걸려 검색을 건 것과 안 건 것이 같아 보인다.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 그 유형 이름이 검색 조건에 맞는가. 검색어가 없으면 전부 맞는다. */
    public boolean matches(String name) {
        return expendGroupName == null
                || (name != null && name.contains(expendGroupName));
    }
}
