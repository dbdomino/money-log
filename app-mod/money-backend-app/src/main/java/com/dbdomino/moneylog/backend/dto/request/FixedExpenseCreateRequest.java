package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 4.1 고정지출 등록 요청.
 *
 * <p><b>적용 기간을 연·월 네 필드로 받는다.</b> {@code "2026-11"} 같은 문자열 하나가
 * 아니라 정수 넷인 이유는 <b>저장 형태가 그렇기 때문</b>이다({@code start_year} ·
 * {@code start_month} · {@code end_year} · {@code end_month}, 헌장 DB 저장 구조 규칙).
 * 문자열로 받으면 파싱 실패가 {@code 9001} 인지 {@code 3401} 인지 흐려진다.
 *
 * <p><b>값 검증은 여기서 하지 않는다.</b> Bean Validation 은 "없거나 비었다"까지만 보고,
 * 결제일 1~31 · 금액 0 초과 · 기간 뒤집힘은 전부 {@link
 * com.dbdomino.moneylog.backend.service.FixedExpenseFieldRules} 가 {@code 3401} 로 낸다 —
 * 4.4 수정도 같은 규칙을 써야 하는데 그쪽은 DTO 가 아니라 {@code Map} 으로 들어오기
 * 때문이다. 두 경로가 같은 판정을 쓰지 않으면 등록은 막고 수정은 통과하는 구멍이 생긴다.
 *
 * @param name              고정지출 이름(예: 월세). 회원 안에서 중복을 막지 않는다
 * @param paymentMethodId   본인 소유의 사용 중 수단. <b>{@code purpose=EXPENSE}</b> 여야 한다
 * @param expendGroupId     본인 소유의 사용 중 지출유형
 * @param amount            기본 금액(원). 월별로 다른 금액은 월별 행이 따로 갖는다
 * @param paymentDayOfMonth 매달 결제일 1~31. <b>31 을 허용한다</b> — 말일 보정은 월별
 *                          내역을 만들 때 하며, 여기서 막으면 매월 말일에 나가는
 *                          고정지출을 표현할 수 없다
 * @param content           내용
 * @param startYear         적용 시작 연
 * @param startMonth        적용 시작 월(1~12)
 * @param endYear           적용 종료 연
 * @param endMonth          적용 종료 월(1~12). 종료가 시작보다 앞서면 {@code 3401}
 */
public record FixedExpenseCreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotNull(message = "수단은 필수입니다.")
        Long paymentMethodId,

        @NotNull(message = "지출유형은 필수입니다.")
        Long expendGroupId,

        @NotNull(message = "금액은 필수입니다.")
        Long amount,

        @NotNull(message = "결제일은 필수입니다.")
        Integer paymentDayOfMonth,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 255, message = "내용은 255자 이하여야 합니다.")
        String content,

        @NotNull(message = "시작 연은 필수입니다.")
        Integer startYear,

        @NotNull(message = "시작 월은 필수입니다.")
        Integer startMonth,

        @NotNull(message = "종료 연은 필수입니다.")
        Integer endYear,

        @NotNull(message = "종료 월은 필수입니다.")
        Integer endMonth) {
}
