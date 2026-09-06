package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 목표금액 한 값의 규칙 — <b>5.3·5.4 두 곳이 같은 검사를 쓴다</b>.
 *
 * <p>한 곳에 모은 이유는 <b>DB 가 같은 범위를 CHECK 로 이미 걸고 있기</b> 때문이다
 * ({@code ck_target_default_amount}·{@code ck_target_monthly_amount}, 덤프 확인).
 * 애플리케이션 검사가 DB 와 어긋나면 통과한 값이 저장 단계에서 막혀 <b>{@code 9000} 으로
 * 새어 나간다</b> — 사용자에게는 원인을 알 수 없는 오류가 된다. 두 곳에 따로 적으면
 * 한쪽만 고쳐도 그 상황이 만들어진다.
 *
 * <p>005 의 {@code FixedExpenseFieldRules} 와 같은 자리의 클래스다.
 */
public final class ExpendTargetFieldRules {

    /** 목표금액 하한. <b>{@code 0} 은 유효한 값이다</b> — "쓰지 않겠다"는 뜻이다(FR-504). */
    public static final long MIN_AMOUNT = 0L;

    /** 목표금액 상한. 1억 원. */
    public static final long MAX_AMOUNT = 100_000_000L;

    private ExpendTargetFieldRules() {
    }

    /**
     * 목표금액이 {@code 0 ~ 100,000,000} 인가.
     *
     * <p><b>누락({@code null})도 같은 코드로 낸다.</b> 5.3·5.4 의 실패 표에
     * {@code 9001} 이 없고, 금액 하나뿐인 몸통에서 "빠뜨렸다"와 "값이 틀렸다"를 가르는
     * 것이 사용자에게 주는 정보가 없다. 005 의 금액 규칙과 같은 처방이다.
     *
     * <p><b>경계는 둘 다 포함이다</b> — 정확히 0 과 정확히 1억은 성공이다.
     *
     * @throws BusinessException {@code 3602} — 누락 또는 범위 밖
     */
    public static long requireAmount(Long amount) {
        if (amount == null || amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new BusinessException(ErrorCode.TARGET_AMOUNT_OUT_OF_RANGE);
        }
        return amount;
    }
}
