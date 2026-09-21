package com.dbdomino.moneylog.front.fixedexpense;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 월별 내역 모달의 연·월을 <b>주소에서 읽고 다시 주소로 만든다.</b>
 *
 * <p>양방향인 이유는 <b>모달 안의 연·월 이동과 단건 수정 착지가 모두 그 값을 실어
 * 나르기 때문</b>이다. 읽기만 하면 연·월을 바꾸는 링크를 만들 때마다 화면이 값을 손으로
 * 이어 붙이게 되고, 한 자리만 빠뜨려도 다른 달이 열린다.
 *
 * <h2>세션에 두지 않는다</h2>
 *
 * <p>두면 뒤로 가기와 새 탭에서 다른 달이 뜬다. 주소에 있으면 그 주소가 곧 그 화면이다.
 *
 * <h2>브라우저 시각을 믿지 않는다</h2>
 *
 * <p>주소에 연·월이 없으면 <b>이번 달</b>이고 그것을 <b>서버 시각으로</b> 정한다 — 브라우저
 * 시각을 믿으면 시차가 있는 사용자가 다른 달을 본다. 010·011 이 같은 판단을 쓴다.
 *
 * <h2>010 의 것보다 훨씬 작다</h2>
 *
 * <p>가계부에는 필터 여섯과 정렬 둘이 함께 실렸다. 여기에는 <b>연·월뿐</b>이라 같은 자리의
 * 훨씬 단순한 형태다.
 *
 * <p>이름이 {@code y}·{@code mm} 인 이유는 <b>부모 목록의 쪽 번호와 섞이지 않게</b> 하기
 * 위해서다 — 모달이 목록 위에 얹히므로 한 주소에 두 화면의 상태가 함께 실린다.
 */
public record MonthlyQuery(int year, int month) {

    /** 화면 주소에 실리는 이름. 부모 목록의 값과 섞이지 않게 짧게 둔다. */
    static final String PARAM_YEAR = "y";
    static final String PARAM_MONTH = "mm";

    /** 백엔드가 받는 Query 이름. 화면 주소의 이름과 다르다. */
    static final String BACKEND_YEAR = "year";
    static final String BACKEND_MONTH = "month";

    /**
     * 주소에 실려 온 값으로 만든다. <b>없거나 범위 밖이면 이번 달</b>이다.
     *
     * <p>범위 밖을 오류로 보지 않는 이유는 북마크를 잘못 저장했거나 주소를 오타로 친
     * 사용자를 막을 이유가 없기 때문이다 — 007 이 모달 딥링크를 그렇게 정했고 같은 판단이다.
     *
     * @param year 주소의 연도. 없으면 {@code null}
     * @param month 주소의 월. 없으면 {@code null}
     */
    public static MonthlyQuery of(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = isValidYear(year) ? year : today.getYear();
        int resolvedMonth = isValidMonth(month) ? month : today.getMonthValue();
        return new MonthlyQuery(resolvedYear, resolvedMonth);
    }

    private static boolean isValidYear(Integer year) {
        return year != null && year >= 1900 && year <= 9999;
    }

    private static boolean isValidMonth(Integer month) {
        return month != null && month >= 1 && month <= 12;
    }

    /** 백엔드 목록 요청에 실을 값. */
    public Map<String, Object> toBackendQuery() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put(BACKEND_YEAR, year);
        query.put(BACKEND_MONTH, month);
        return query;
    }

    /**
     * 화면 주소에 실을 값. 연·월 이동과 단건 수정 착지가 함께 쓴다.
     *
     * <p>백엔드로 나가는 값과 나눠 둔 이유는 <b>이름이 다르기 때문</b>이다 — 화면 주소에는
     * 부모 목록의 쪽 번호가 함께 실려 {@code year} 라는 이름을 그대로 쓰면 어느 화면의
     * 값인지 흐려진다.
     */
    public Map<String, Object> toUrlParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_YEAR, year);
        params.put(PARAM_MONTH, month);
        return params;
    }

    /** 이전 달. 연을 넘어가는 계산을 화면이 하지 않게 여기 둔다. */
    public MonthlyQuery previousMonth() {
        LocalDate moved = LocalDate.of(year, month, 1).minusMonths(1);
        return new MonthlyQuery(moved.getYear(), moved.getMonthValue());
    }

    /** 다음 달. */
    public MonthlyQuery nextMonth() {
        LocalDate moved = LocalDate.of(year, month, 1).plusMonths(1);
        return new MonthlyQuery(moved.getYear(), moved.getMonthValue());
    }

    /**
     * 그 날짜가 이 달 안인가. <b>단건 수정의 결제일이 그 달 안이어야 한다</b>는 규칙을
     * 화면이 먼저 알리는 데 쓴다.
     *
     * <p>서버도 판정하며 <b>화면이 막는 것과 서버가 막는 것은 서로를 대신하지 않는다</b> —
     * 주소를 직접 쳐서 올 수 있다.
     */
    public boolean contains(String paymentDate) {
        if (paymentDate == null || paymentDate.isBlank()) {
            return false;
        }
        try {
            LocalDate parsed = LocalDate.parse(paymentDate.trim());
            return parsed.getYear() == year && parsed.getMonthValue() == month;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** 그 달의 첫날·마지막날 {@code YYYY-MM-DD}. 편집 폼의 입력 범위에 건다. */
    public String firstDay() {
        return LocalDate.of(year, month, 1).toString();
    }

    public String lastDay() {
        LocalDate first = LocalDate.of(year, month, 1);
        return first.withDayOfMonth(first.lengthOfMonth()).toString();
    }
}
