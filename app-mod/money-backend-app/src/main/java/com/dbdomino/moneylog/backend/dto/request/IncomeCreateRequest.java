package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 3.7 소득 등록 요청.
 *
 * <p><b>{@code place}·{@code expendGroupId}·할부 필드를 아예 두지 않는다.</b>
 * {@code tbl_income} 에 대응 컬럼이 <b>없기</b> 때문이다(FR-306) — 비워 두는 것이 아니라
 * 존재하지 않는 것이며, 그 사실이 타입으로 드러난다. 요청 Body 에 실어 보내도 바인딩될
 * 자리가 없어 조용히 무시된다.
 *
 * <p>지출과 값 규칙은 같지만 <b>실패 코드가 {@code 3301}</b> 이다(지출은 {@code 3201}).
 * 여기 붙은 {@code @NotNull} 은 누락({@code 9001})만 잡고 값 판정은 서비스가 한다 —
 * {@code ExpenseCreateRequest} 와 같은 판단이다.
 *
 * @param paymentMethodId 소득 수단. {@code purpose=INCOME} 이고 사용 중이어야 한다 —
 *                        아니면 {@code 3003}
 * @param amount          금액(원). 0보다 커야 하며 아니면 {@code 3301}
 * @param paymentDate     입금일 {@code YYYY-MM-DD}. 형식이 어긋나면 {@code 3301}
 * @param content         내용. <b>선택이다</b>(FR-307) — 지출에서 필수인 것과 다르며,
 *                        비우면 {@code null} 로 저장한다
 */
public record IncomeCreateRequest(
        @NotNull Long paymentMethodId,
        @NotNull Long amount,
        @NotNull String paymentDate,
        String content) {
}
