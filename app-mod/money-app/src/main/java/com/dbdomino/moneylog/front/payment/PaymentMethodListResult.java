package com.dbdomino.moneylog.front.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 백엔드 수단 목록 응답.
 *
 * <p><b>쪽 정보가 없다.</b> 이 목록은 조회 구간을 받지 않고 본인 것을 전부 돌려주므로 담을
 * 것이 목록 하나뿐이다. 008 의 회원 목록과 다른 점이며, 010~012 의 가계부·고정지출 목록은
 * 다시 쪽 정보를 갖는다 — 목록이라고 다 같지 않다.
 *
 * <p><b>삭제 표시된 것까지 함께 온다.</b> 화면이 그것을 감추지 않고 상태 열로 구분해 보인다.
 *
 * @param list 본인 수단 전체
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentMethodListResult(List<PaymentMethodView> list) {

    /** 행이 없으면 빈 목록으로 다룬다. 템플릿이 null 을 가리는 분기를 갖지 않게 한다. */
    public List<PaymentMethodView> rows() {
        return list == null ? List.of() : list;
    }
}
