package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 2.1 지출·소득 수단 등록 요청.
 *
 * <p><b>{@code type}·{@code purpose} 를 Bean Validation 으로 막지 않는다.</b> 허용 값 밖은
 * {@code 3001} 인데 Bean Validation 실패는 전역 처리에서 {@code 9001} 로 나가기 때문이다 —
 * 값 검증은 서비스가 한다. 002 의 가입이 비밀번호 규칙({@code 2004})을 같은 이유로 서비스에
 * 둔 것과 같은 판단이다.
 *
 * <p>소유자를 받지 않는다. 수단의 주인은 <b>토큰이 지시하는 {@code id_key}</b> 이며 요청이
 * 지정할 수 없다(FR-201).
 *
 * @param name       수단 이름(예: 국민카드, 월급통장)
 * @param type       {@code CARD} 또는 {@code ACCOUNT}. 그 밖의 값은 {@code 3001}
 * @param purpose    {@code EXPENSE} 또는 {@code INCOME}. 한 수단은 한쪽만 갖는다
 * @param inUse      사용 여부. 필수다
 * @param cardExpiry 카드 유효기간 {@code YYYY-MM}(선택). {@code type=ACCOUNT} 면 저장 시 비운다
 */
public record PaymentMethodCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank String type,
        @NotBlank String purpose,
        @NotNull Boolean inUse,
        String cardExpiry) {
}
