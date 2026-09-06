package com.dbdomino.moneylog.backend.dto.response;

/**
 * 수단 1건. 등록(2.1)·관리 목록(2.2)·상세(2.3)·수정(2.4)이 함께 쓴다.
 *
 * <p><b>{@code deleted} 를 싣는다.</b> 관리 목록(2.2)이 삭제 표시된 수단까지 돌려주므로
 * (FR-207), 이 필드가 없으면 화면이 살아 있는 수단과 삭제된 수단을 구분할 수 없다.
 * 사용 중 목록(2.6)은 {@code deleted=false} 만 돌려주지만 같은 타입을 써도 값이 항상
 * {@code false} 라 해가 없다.
 *
 * <p>Entity 를 그대로 내보내지 않는다(헌장 원칙 II) — {@code tbl_user_payment_method} 의
 * 감사 컬럼과 소유자 연관이 API 로 새어 나가지 않는다.
 *
 * @param paymentMethodId 수단 대리키({@code idx})
 * @param name            수단 이름
 * @param type            {@code CARD} 또는 {@code ACCOUNT}
 * @param purpose         {@code EXPENSE} 또는 {@code INCOME}
 * @param inUse           사용 여부. {@code false} 면 사용 중 목록(2.6)에서 빠진다
 * @param cardExpiry      카드 유효기간 {@code YYYY-MM}. 계좌면 {@code null}
 * @param deleted         삭제 표시 여부. {@code true} 여도 관리 목록에는 남는다
 */
public record PaymentMethodResponse(Long paymentMethodId, String name, String type,
                                    String purpose, boolean inUse, String cardExpiry,
                                    boolean deleted) {
}
