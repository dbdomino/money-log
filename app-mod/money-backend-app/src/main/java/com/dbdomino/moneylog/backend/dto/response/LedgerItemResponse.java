package com.dbdomino.moneylog.backend.dto.response;

import java.time.LocalDate;

/**
 * 월별 가계부 목록의 행 하나 — 네 출처를 담는 <b>공통 형태</b>다.
 *
 * <h2>출처마다 없는 칸이 있다</h2>
 *
 * <table border="1">
 *   <caption>{@code null} 이 되는 자리</caption>
 *   <tr><th>{@code type}</th><th>{@code expendGroup*}</th><th>{@code place}</th>
 *       <th>{@code installment*}</th><th>{@code fixedExpenseName}</th></tr>
 *   <tr><td>{@code EXPENSE}</td><td>있다</td><td>있다</td><td>{@code null}</td><td>{@code null}</td></tr>
 *   <tr><td>{@code INSTALLMENT}</td><td>있다</td><td>있다</td><td>채워짐</td><td>{@code null}</td></tr>
 *   <tr><td>{@code INCOME}</td><td><b>{@code null}</b></td><td><b>{@code null}</b></td><td>{@code null}</td><td>{@code null}</td></tr>
 *   <tr><td>{@code FIXED}</td><td>있다</td><td><b>{@code null}</b></td><td>{@code null}</td><td>채워짐</td></tr>
 * </table>
 *
 * <p>{@code INCOME} 의 두 칸이 {@code null} 인 것은 값이 없어서가 아니라 <b>컬럼이 아예
 * 없기 때문</b>이다 — 소득에는 지출유형·장소라는 개념이 없다. 이 차이가 SQL
 * {@code UNION} 을 쓰지 않는 이유이기도 하다(그러면 둘이 구분되지 않는다).
 *
 * <h2>한 목록에 이름 규칙이 둘 섞인다 (FR-419·FR-425)</h2>
 *
 * <p><b>이 목록에서 가장 헷갈리는 지점이다.</b>
 *
 * <table border="1">
 *   <caption>같은 필드, 다른 규칙</caption>
 *   <tr><th>행 종류</th><th>{@code paymentMethodName}·{@code expendGroupName}</th></tr>
 *   <tr><td>{@code EXPENSE}·{@code INSTALLMENT}·{@code INCOME}</td>
 *       <td><b>등록 당시 스냅샷</b> — 과거 기록이므로 그때 이름으로 남는다</td></tr>
 *   <tr><td>{@code FIXED}</td>
 *       <td><b>조회 시점 현재 이름</b> — 지금 유효한 설정이므로 현재 이름이 맞다</td></tr>
 * </table>
 *
 * <p>수단 이름을 바꾸면 같은 달을 재조회했을 때 {@code FIXED} 행만 새 이름이 된다.
 * <b>행 종류로 구분한다</b> — 이 판정은 {@code LedgerItemFactory} 한 곳에만 있어야 한다.
 *
 * @param ledgerItemId       행 식별자. {@code EXPENSE}·{@code INSTALLMENT} 는
 *                           {@code expense:{id}}, {@code INCOME} 은 {@code income:{id}},
 *                           {@code FIXED} 는 {@code fixed:{id}:{year}:{month}} 다.
 *                           <b>{@code FIXED} 만 세 조각인 이유</b>는 {@code sourceId} 만으로
 *                           월별 내역 1행이 특정되지 않기 때문이다
 * @param type               {@code EXPENSE} · {@code INSTALLMENT} · {@code INCOME} · {@code FIXED}
 * @param sourceId           원본 PK. {@code FIXED} 는 <b>고정지출 설정</b>의 PK 이며
 *                           월별 내역의 PK 가 아니다 — 4.6 의 Path 에 들어가는 값이다
 * @param paymentDate        결제일. {@code dateFrom}·{@code dateTo} 필터와
 *                           {@code sort=paymentDate} 의 기준이다
 * @param amount             금액. <b>부호가 없다</b> — 지출인지 소득인지는 {@code type} 이 정한다
 * @param paymentMethodId    수단 참조
 * @param paymentMethodName  수단 이름. 규칙은 위 표를 본다
 * @param expendGroupId      지출유형 참조. {@code INCOME} 은 {@code null}
 * @param expendGroupName    지출유형 이름. {@code INCOME} 은 {@code null}
 * @param place              장소. {@code INCOME}·{@code FIXED} 는 {@code null}
 * @param content            내용. 네 종류 모두 갖는다
 * @param fixedExpenseName   고정지출 이름. {@code FIXED} 일 때만 채워지며 <b>현재</b> 값이다
 * @param installmentGroupId 할부 그룹. {@code INSTALLMENT} 일 때만 채워진다
 * @param installmentIndex   회차 1..N
 * @param installmentTotal   전체 회차 N
 */
public record LedgerItemResponse(
        String ledgerItemId,
        String type,
        Long sourceId,
        LocalDate paymentDate,
        Long amount,
        Long paymentMethodId,
        String paymentMethodName,
        Long expendGroupId,
        String expendGroupName,
        String place,
        String content,
        String fixedExpenseName,
        Long installmentGroupId,
        Integer installmentIndex,
        Integer installmentTotal) {

    /** 일반 지출. 할부가 아닌 {@code tbl_expense} 행이다. */
    public static final String TYPE_EXPENSE = "EXPENSE";

    /** 할부 지출. {@code installment_group_id} 가 채워진 {@code tbl_expense} 행이다. */
    public static final String TYPE_INSTALLMENT = "INSTALLMENT";

    /** 소득. */
    public static final String TYPE_INCOME = "INCOME";

    /** 고정지출. {@code tbl_fixed_expense_monthly} 행이다. */
    public static final String TYPE_FIXED = "FIXED";
}
