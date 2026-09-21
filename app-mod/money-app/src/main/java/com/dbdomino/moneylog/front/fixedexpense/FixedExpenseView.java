package com.dbdomino.moneylog.front.fixedexpense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 화면이 보여 주는 고정지출 <b>설정</b>. 목록(4.2)·상세(4.4)·수정(4.5)이 같은 모양을 쓴다.
 *
 * <h2>설정은 그 달 내역이 아니다</h2>
 *
 * <p>여기 담긴 금액은 <b>"매달 이만큼 나간다"는 기준값</b>이고, {@link MonthlyRow} 의 금액은
 * <b>그 달에 실제로 잡힌 값</b>이다. 화면이 둘을 같은 말로 부르면 사용자는 하나로 읽고,
 * <b>"설정을 고쳤는데 지난달이 안 바뀐다"를 버그로 신고한다.</b>
 *
 * <p>그래서 이 값의 이름을 {@code amount} 가 아니라 <b>기본 금액</b>으로 적는다 — 열 제목도
 * 「기본 금액」이다.
 *
 * <h2>이름을 저장하지 않는다 — 010 과 정반대다</h2>
 *
 * <p>수단·지출유형 이름은 <b>조회 시점 현재 값</b>이다. 고정지출에는 이름 컬럼이 없어
 * 조회할 때마다 수단·지출유형 행에서 읽는다 — 수단 이름을 바꾸면 이 설정도 새 이름으로
 * 보인다.
 *
 * <table>
 *   <caption>이름 규칙이 기능마다 갈린다</caption>
 *   <tr><th></th><th>지출·소득 (010)</th><th>고정지출 (011)</th></tr>
 *   <tr><td>출처</td><td>등록 당시 스냅샷</td><td><b>조회 시점 현재 값</b></td></tr>
 *   <tr><td>왜</td><td>이미 일어난 일의 기록</td><td><b>지금 유효한 설정</b></td></tr>
 *   <tr><td>화면이 할 일</td><td>받은 값 그대로</td><td><b>받은 값 그대로</b></td></tr>
 * </table>
 *
 * <p><b>화면이 할 일은 양쪽 다 같다.</b> 저장된 값으로 덮어쓰지도, 식별자로 원본을 다시
 * 조회하지도 않는다 — 010 의 가계부 목록에서 고정지출 행만 현재 이름이었던 것이 이 차이다.
 *
 * <h2>적용 기간은 정수 네 칸이다</h2>
 *
 * <p>010 의 할부가 문자열 한 칸({@code "2026-01"})인 것과 다르다. 고정지출은 <b>시작·종료
 * 두 쌍</b>이라 네 칸이며, <b>한쪽 규칙을 다른 쪽에 옮기면 등록이 형식 오류로 막힌다.</b>
 *
 * <p><b>이어 붙이는 자리를 여기 둔다</b>({@link #periodLabel()}) — 화면마다 이으면 같은
 * 기간이 자리마다 다른 모양이 된다.
 *
 * @param fixedExpenseId 식별자. 주소와 요청 경로에만 쓰고 화면에 보이지 않는다
 * @param name 고정지출 이름. 목록에서 이 값으로 구분한다
 * @param paymentMethodId 수단 식별자. 수정 모달의 선택 칸이 쓴다
 * @param paymentMethodName 수단 이름. <b>조회 시점 현재 값</b>이다
 * @param amount 기본 금액. 원 단위 정수이며 <b>그 달 금액이 아니다</b>
 * @param paymentDayOfMonth 매달 결제일 1~31
 * @param content 내용
 * @param expendGroupId 지출유형 식별자
 * @param expendGroupName 지출유형 이름. 위와 같은 규칙이다
 * @param startYear 적용 시작 연도
 * @param startMonth 적용 시작 월 1~12
 * @param endYear 적용 종료 연도
 * @param endMonth 적용 종료 월 1~12
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixedExpenseView(
        Long fixedExpenseId,
        String name,
        Long paymentMethodId,
        String paymentMethodName,
        Long amount,
        Integer paymentDayOfMonth,
        String content,
        Long expendGroupId,
        String expendGroupName,
        Integer startYear,
        Integer startMonth,
        Integer endYear,
        Integer endMonth) {

    /**
     * 적용 기간을 「2026-01 ~ 2026-12」로 잇는다.
     *
     * <p>저장은 정수 네 칸이지만 <b>읽는 자리에서는 이어 붙인다.</b> 목록·상세가 같은
     * 모양을 쓰도록 여기 한 곳에 둔다.
     *
     * <p>값이 비면 그 자리를 비운다 — 「없음」 같은 말을 채우지 않는다.
     */
    public String periodLabel() {
        String start = yearMonthLabel(startYear, startMonth);
        String end = yearMonthLabel(endYear, endMonth);
        if (start.isEmpty() && end.isEmpty()) {
            return "";
        }
        return start + " ~ " + end;
    }

    private static String yearMonthLabel(Integer year, Integer month) {
        if (year == null || month == null) {
            return "";
        }
        return String.format("%04d-%02d", year, month);
    }

    /** 결제일을 화면에 보일 말로 바꾼다. 「매달 5일」처럼 보인다. */
    public String paymentDayLabel() {
        return paymentDayOfMonth == null ? "" : "매달 " + paymentDayOfMonth + "일";
    }
}
