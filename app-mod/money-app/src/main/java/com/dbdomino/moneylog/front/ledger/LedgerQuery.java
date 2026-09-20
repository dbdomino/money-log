package com.dbdomino.moneylog.front.ledger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 가계부 화면의 상태를 <b>주소에서 읽고 다시 주소로 만든다.</b>
 *
 * <p>양방향인 이유는 목록·모달·연월 이동이 <b>모두 그 값을 실어 나르기 때문</b>이다. 읽기만
 * 하면 연·월을 바꾸는 링크를 만들 때마다 화면이 나머지 값을 손으로 이어 붙이게 되고, 한
 * 자리만 빠뜨려도 조건이 조용히 사라진다.
 *
 * <p><b>주소로 다시 만드는 쪽이 핵심이다.</b> 연·월만 바꾼 주소를 만들면 나머지가 그대로
 * 실려 "연·월을 바꿔도 필터가 유지된다"(FR-921)가 저절로 따라온다.
 *
 * <h2>세션에 두지 않는다</h2>
 *
 * <p>두면 뒤로 가기와 새 탭에서 다른 목록이 뜬다. 주소에 있으면 그 주소가 곧 그 화면이다.
 *
 * <h2>브라우저 시각을 믿지 않는다</h2>
 *
 * <p>주소에 연·월이 없으면 <b>이번 달</b>이고 그것을 <b>서버 시각으로</b> 정한다. 브라우저
 * 시각을 믿으면 시차가 있는 사용자가 다른 달을 본다.
 *
 * <h2>비어 있는 조건은 싣지 않는다</h2>
 *
 * <p>빈 값을 보내면 백엔드가 <b>"그 값으로 걸러 달라"로 읽어 결과가 통째로 빈다.</b> 그래서
 * 요청을 만들 때 값이 있는 것만 담는다.
 *
 * @param year 조회 연도
 * @param month 조회 월
 * @param paymentMethodId 수단 필터. 없으면 {@code null}
 * @param expendGroupId 지출유형 필터. 없으면 {@code null}
 * @param dateFrom 결제일 시작. 그 달 안에서 더 좁힌다
 * @param dateTo 결제일 종료
 * @param keyword 검색어. <b>내용과 장소</b> 두 칸을 본다
 * @param types 행 종류 필터. <b>여러 값을 함께</b> 고를 수 있다
 * @param sort 정렬 기준. <b>결제일과 금액 둘뿐</b>이다
 * @param order 정렬 방향
 */
public record LedgerQuery(
        int year,
        int month,
        Long paymentMethodId,
        Long expendGroupId,
        String dateFrom,
        String dateTo,
        String keyword,
        List<String> types,
        String sort,
        String order) {

    /** 백엔드가 받는 Query 이름. 화면 주소의 이름과 같게 둔다. */
    static final String PARAM_YEAR = "year";
    static final String PARAM_MONTH = "month";
    static final String PARAM_PAYMENT_METHOD = "paymentMethodId";
    static final String PARAM_EXPEND_GROUP = "expendGroupId";
    static final String PARAM_DATE_FROM = "dateFrom";
    static final String PARAM_DATE_TO = "dateTo";
    static final String PARAM_KEYWORD = "keyword";
    static final String PARAM_TYPE = "type";
    static final String PARAM_SORT = "sort";
    static final String PARAM_ORDER = "order";

    /**
     * 백엔드가 받는 정렬 기준. <b>이 둘뿐이다.</b>
     *
     * <p>스펙이 처음에 수단·지출유형 정렬도 적었는데 백엔드가 그 둘을 받지 않는다 — 부를
     * 방법이 없어 명세를 계약에 맞췄다. <b>화면이 받은 목록을 스스로 다시 정렬하지 않는다</b>:
     * 지금은 한 달치를 전부 받아 괜찮아 보이지만 그 가정이 화면 코드에 숨고, 쪽 넘기기가
     * 생기는 순간 깨진다.
     */
    public static final String SORT_PAYMENT_DATE = "paymentDate";
    public static final String SORT_AMOUNT = "amount";
    private static final Set<String> SORTS = Set.of(SORT_PAYMENT_DATE, SORT_AMOUNT);

    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";
    private static final Set<String> ORDERS = Set.of(ORDER_ASC, ORDER_DESC);

    /** 고를 수 있는 행 종류. 화면 밖의 값이라 모르는 값은 버린다. */
    private static final Set<String> TYPES = Set.of(LedgerRow.TYPE_EXPENSE,
            LedgerRow.TYPE_INSTALLMENT, LedgerRow.TYPE_INCOME, LedgerRow.TYPE_FIXED);

    /**
     * 주소에 실려 온 값으로 만든다. <b>연·월이 없거나 범위 밖이면 이번 달</b>이다.
     *
     * <p>범위 밖을 오류로 보지 않는 이유는 북마크를 잘못 저장했거나 주소를 오타로 친
     * 사용자를 막을 이유가 없기 때문이다 — 007 이 모달 딥링크를 그렇게 정했고 같은 판단이다.
     */
    public static LedgerQuery of(Integer year, Integer month) {
        return of(year, month, null, null, null, null, null, null, null, null);
    }

    public static LedgerQuery of(Integer year, Integer month, Long paymentMethodId,
            Long expendGroupId, String dateFrom, String dateTo, String keyword,
            List<String> types, String sort, String order) {

        LocalDate today = LocalDate.now();
        return new LedgerQuery(
                isValidYear(year) ? year : today.getYear(),
                isValidMonth(month) ? month : today.getMonthValue(),
                paymentMethodId,
                expendGroupId,
                trimToNull(dateFrom),
                trimToNull(dateTo),
                trimToNull(keyword),
                normalizeTypes(types),
                allowed(SORTS, sort),
                allowed(ORDERS, order));
    }

    /**
     * 아는 값이면 그대로, 아니면 {@code null}.
     *
     * <p>{@code Set.of(...)} 로 만든 집합은 <b>{@code null} 을 물어보는 것만으로 터진다.</b>
     * 조건이 비어 있는 것이 이 화면의 정상 상태라 널을 먼저 가려낸다 — 가려내지 않으면
     * 필터를 걸지 않은 첫 화면부터 오류가 난다.
     *
     * <p>모르는 값을 오류로 보지 않는 이유는 주소를 오타로 친 사용자를 막을 이유가 없기
     * 때문이다. 그 조건만 빠지고 목록은 정상으로 뜬다.
     */
    private static String allowed(Set<String> candidates, String value) {
        String trimmed = trimToNull(value);
        return trimmed != null && candidates.contains(trimmed) ? trimmed : null;
    }

    private static boolean isValidYear(Integer year) {
        return year != null && year >= 1900 && year <= 9999;
    }

    private static boolean isValidMonth(Integer month) {
        return month != null && month >= 1 && month <= 12;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 아는 종류만 남기고 중복을 없앤다. 순서는 사용자가 고른 대로 둔다.
     *
     * <p>모르는 값을 오류로 보지 않는 이유는 주소를 오타로 친 사용자를 막을 이유가 없기
     * 때문이다. 그 값만 빠지고 목록은 정상으로 뜬다.
     */
    private static List<String> normalizeTypes(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Set<String> kept = new LinkedHashSet<>();
        for (String value : requested) {
            String trimmed = allowed(TYPES, value);
            if (trimmed != null) {
                kept.add(trimmed);
            }
        }
        return List.copyOf(kept);
    }

    // ── 백엔드로 ────────────────────────────────────────────────────────

    /**
     * 백엔드 목록 요청에 실을 값.
     *
     * <p><b>조회 구간을 싣지 않는다</b> — 한 달치를 전부 받고 합계가 함께 온다.
     *
     * <p><b>비어 있는 조건은 담지 않는다.</b> 빈 값을 보내면 백엔드가 "그 값으로 걸러
     * 달라"로 읽어 결과가 통째로 빈다.
     */
    public Map<String, Object> toBackendQuery() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put(PARAM_YEAR, year);
        query.put(PARAM_MONTH, month);
        putIfPresent(query, PARAM_PAYMENT_METHOD, paymentMethodId);
        putIfPresent(query, PARAM_EXPEND_GROUP, expendGroupId);
        putIfPresent(query, PARAM_DATE_FROM, dateFrom);
        putIfPresent(query, PARAM_DATE_TO, dateTo);
        putIfPresent(query, PARAM_KEYWORD, keyword);
        // 백엔드가 콤마로 여러 값을 받는다. 한 값만 보내면 "지출과 할부만" 같은 조합을
        // 만들 수 없다.
        if (hasTypes()) {
            query.put(PARAM_TYPE, String.join(",", types));
        }
        putIfPresent(query, PARAM_SORT, sort);
        putIfPresent(query, PARAM_ORDER, order);
        return query;
    }

    private static void putIfPresent(Map<String, Object> query, String name, Object value) {
        if (value != null) {
            query.put(name, value);
        }
    }

    // ── 주소로 ──────────────────────────────────────────────────────────

    /**
     * 화면 주소에 실을 값. 목록으로 돌아가는 링크와 연·월 이동이 함께 쓴다.
     *
     * <p>백엔드로 나가는 값과 나눠 둔 이유는 <b>둘이 같아야 할 이유가 없기 때문</b>이다.
     * 화면 주소에는 열 모달처럼 백엔드가 모르는 값이 함께 실린다.
     *
     * <p>종류는 값마다 한 칸씩 싣는다 — 주소에서 읽을 때 여러 값으로 다시 읽히려면 그래야
     * 하고, 콤마로 잇는 것은 백엔드로 나갈 때의 모양이다.
     */
    public Map<String, Object> toUrlParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_YEAR, year);
        params.put(PARAM_MONTH, month);
        putIfPresent(params, PARAM_PAYMENT_METHOD, paymentMethodId);
        putIfPresent(params, PARAM_EXPEND_GROUP, expendGroupId);
        putIfPresent(params, PARAM_DATE_FROM, dateFrom);
        putIfPresent(params, PARAM_DATE_TO, dateTo);
        putIfPresent(params, PARAM_KEYWORD, keyword);
        if (hasTypes()) {
            params.put(PARAM_TYPE, new ArrayList<>(types));
        }
        putIfPresent(params, PARAM_SORT, sort);
        putIfPresent(params, PARAM_ORDER, order);
        return params;
    }

    /**
     * 주소에 붙일 질의 문자열. 연·월 이동 링크가 이것을 쓴다.
     *
     * <p>지도를 그대로 넘기지 않는 이유는 <b>템플릿의 링크 식이 지도를 펼쳐 주지 않기
     * 때문</b>이다. 펼쳐지는 줄 알고 넘기면 링크가 조용히 조건 없는 주소가 되고, 증상은
     * "달을 옮기면 필터가 풀린다"로만 나타난다.
     *
     * <p>값은 <b>여기서 인코딩한다.</b> 검색어에 한글과 {@code &} 가 들어갈 수 있어 그대로
     * 이으면 주소가 끊긴다.
     */
    public String toQueryString() {
        StringBuilder text = new StringBuilder();
        toUrlParams().forEach((name, value) -> {
            if (value instanceof List<?> many) {
                many.forEach(one -> append(text, name, String.valueOf(one)));
            } else {
                append(text, name, String.valueOf(value));
            }
        });
        return text.toString();
    }

    private static void append(StringBuilder text, String name, String value) {
        if (text.length() > 0) {
            text.append('&');
        }
        text.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /**
     * 연·월만 바꾼 값. <b>나머지는 그대로 따라간다.</b>
     *
     * <p>여기가 FR-921 이 사는 자리다 — 연·월 이동 링크가 이 값으로 만들어지므로 걸어 둔
     * 조건이 저절로 실린다.
     */
    public LedgerQuery withYearMonth(int newYear, int newMonth) {
        return new LedgerQuery(newYear, newMonth, paymentMethodId, expendGroupId, dateFrom,
                dateTo, keyword, types, sort, order);
    }

    /** 이전 달. 연을 넘어가는 계산을 화면이 하지 않게 여기 둔다. */
    public LedgerQuery previousMonth() {
        LocalDate moved = LocalDate.of(year, month, 1).minusMonths(1);
        return withYearMonth(moved.getYear(), moved.getMonthValue());
    }

    /** 다음 달. */
    public LedgerQuery nextMonth() {
        LocalDate moved = LocalDate.of(year, month, 1).plusMonths(1);
        return withYearMonth(moved.getYear(), moved.getMonthValue());
    }

    // ── 템플릿이 쓰는 판단 ──────────────────────────────────────────────

    public boolean hasTypes() {
        return types != null && !types.isEmpty();
    }

    /** 그 종류가 골라져 있는가. 도구줄의 다중 선택이 이것으로 표시를 남긴다. */
    public boolean hasType(String type) {
        return hasTypes() && types.contains(type);
    }

    /**
     * 조건을 하나라도 걸었는가. <b>빈 목록 안내의 문구를 가르는 근거</b>다.
     *
     * <p>걸어 둔 조건 때문에 0건인 것과 그 달에 거래가 없는 것은 사용자가 할 일이 다르다 —
     * 앞은 조건을 풀면 되고 뒤는 등록해야 한다. <b>다만 화면이 조건을 풀어 주지는 않는다.</b>
     * 사용자가 건 조건이고, 자동으로 풀면 자기가 무엇을 걸었는지 잃는다.
     */
    public boolean hasFilters() {
        return paymentMethodId != null || expendGroupId != null || dateFrom != null
                || dateTo != null || keyword != null || hasTypes();
    }

    /** 정렬 기준. 고르지 않았으면 백엔드의 기본값과 같은 결제일이다. */
    public String sortOrDefault() {
        return sort == null ? SORT_PAYMENT_DATE : sort;
    }

    /** 정렬 방향. 고르지 않았으면 백엔드의 기본값과 같은 내림차순이다. */
    public String orderOrDefault() {
        return order == null ? ORDER_DESC : order;
    }
}
