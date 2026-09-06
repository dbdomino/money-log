package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 3.1 지출 등록 요청.
 *
 * <p><b>값 범위·길이를 Bean Validation 으로 막지 않는다.</b> 허용 밖은 {@code 3201} 인데
 * Bean Validation 실패는 전역 처리에서 전부 {@code 9001} 로 나가기 때문이다. 여기 붙은
 * {@code @NotNull}·{@code @NotBlank} 는 <b>누락({@code 9001})</b> 만 잡고, 금액이 0 이하인지
 * 장소가 100자를 넘는지 같은 <b>값 판정({@code 3201})은 서비스가</b> 한다.
 *
 * <p>002 의 {@code SignupRequest} 가 비밀번호 규칙({@code 2004})을, 003 의
 * {@code PaymentMethodCreateRequest} 가 {@code type}·{@code purpose}({@code 3001})를 같은
 * 이유로 서비스에 둔 것과 같은 판단이다.
 *
 * <p><b>할부 3필드를 받지 않는다.</b> 일시불 등록은 세 컬럼이 전부 NULL 이며(api-contract.md
 * §8), 할부는 3.5 전용이다. 필드가 없으면 실어 보내도 저장될 자리가 없다.
 *
 * <p>소유자도 받지 않는다. 지출의 주인은 <b>토큰이 지시하는 {@code id_key}</b> 이며 요청이
 * 지정할 수 없다(FR-301).
 *
 * @param paymentMethodId 지출 수단. 사용 중이어야 한다 — 아니면 {@code 3003}(FR-325)
 * @param expendGroupId   지출유형. 같은 규칙이며 실패 코드만 {@code 3103} 이다
 * @param amount          금액(원). 0보다 커야 하며 아니면 {@code 3201}
 * @param paymentDate     결제일 {@code YYYY-MM-DD}. 형식이 어긋나면 {@code 3201}
 * @param place           장소. 100자를 넘으면 {@code 3201} — 소득에는 없는 항목이다
 * @param content         내용. 255자를 넘으면 {@code 3201}. <b>지출에서는 필수</b>이며
 *                        소득(3.7)에서 선택인 것과 다르다
 */
public record ExpenseCreateRequest(
        @NotNull Long paymentMethodId,
        @NotNull Long expendGroupId,
        @NotNull Long amount,
        @NotNull String paymentDate,
        @NotBlank String place,
        @NotBlank String content) {
}
