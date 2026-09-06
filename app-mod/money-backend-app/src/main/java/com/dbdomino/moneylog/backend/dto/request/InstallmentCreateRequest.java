package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 3.5 할부 등록 요청. 한 번의 요청이 <b>{@code installmentMonths} 개의 지출 행</b>을 만든다.
 *
 * <p><b>일(day)을 받지 않는다.</b> 회차 결제일은 시작 연월부터 <b>매월 1일</b>로 고정한다
 * (FR-324) — 요청에 일이 없고 {@code tbl_user_payment_method} 에도 결제일 컬럼이 없어
 * 카드별 결제일을 쓸 수 없다(덤프 확인). 1일 고정이면 <b>말일 보정이 필요 없다</b>:
 * 31일에 시작한 할부의 2월 회차를 며칠로 할지 정할 필요가 애초에 생기지 않는다.
 *
 * <p>{@code installmentMonths} 의 하한(2)을 Bean Validation 으로 막지 않는다. 위반은
 * <b>{@code 3204}</b> 인데 Bean Validation 실패는 전역 처리에서 {@code 9001} 로 나가기
 * 때문이다 — 값 판정은 서비스가 한다.
 *
 * <p>금액이 <b>월 납부액</b>({@code monthlyAmount})이지 총액이 아니다. 각 회차 행의
 * {@code amount} 에 이 값이 그대로 들어간다 — 총액을 받아 나누면 나머지를 어느 회차에
 * 붙일지 정해야 하고, 그 규칙이 명세에 없다.
 *
 * @param paymentMethodId   지출 수단. {@code purpose=EXPENSE} 이고 사용 중이어야 한다
 * @param expendGroupId     지출유형. 사용 중이어야 한다 — 아니면 {@code 3103}
 * @param monthlyAmount     <b>월 납부액</b>(원). 0보다 커야 하며 아니면 {@code 3204}
 * @param installmentMonths 할부 개월 수. <b>2 이상</b>이어야 한다(FR-311) — 1개월은
 *                          일시불이므로 3.1 을 쓴다
 * @param startYearMonth    할부 시작 연월 {@code YYYY-MM}. 회차 n 의 결제일은 여기에
 *                          n-1개월을 더한 달의 1일이다
 * @param place             장소. 전 회차가 같은 값을 갖는다
 * @param content           내용(할부 표시용). 전 회차가 같은 값을 갖는다
 */
public record InstallmentCreateRequest(
        @NotNull Long paymentMethodId,
        @NotNull Long expendGroupId,
        @NotNull Long monthlyAmount,
        @NotNull Integer installmentMonths,
        @NotBlank String startYearMonth,
        @NotBlank String place,
        @NotBlank String content) {
}
