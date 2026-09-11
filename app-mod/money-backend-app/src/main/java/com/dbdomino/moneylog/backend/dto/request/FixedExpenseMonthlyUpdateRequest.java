package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.util.Map;
import java.util.Set;

/**
 * 4.6 월별 내역 단건 수정의 요청 필드.
 *
 * <h2>바꿀 수 있는 것은 넷뿐이다 (FR-410)</h2>
 *
 * <p>{@code amount} · {@code paymentDate} · {@code content} · {@code paymentMethodId}.
 * <b>이름과 지출유형은 대상이 아니다</b> — 그것들은 설정(4.4)에서 바꾸고 월별 내역이
 * 따라간다. 여기서 바꾸게 두면 같은 고정지출의 달마다 유형이 달라져 통계가 무너진다.
 *
 * <h2>대상 밖 필드는 {@code 9001} 로 거절한다</h2>
 *
 * <p>{@link PatchFields} 가 허용 목록 밖의 이름을 막는다. 조용히 버리면 오타
 * ({@code amout})가 "아무것도 안 바꿈"으로 흘러가 <b>사용자는 고쳤다고 믿는다.</b>
 * 이 저장소의 모든 PATCH 가 같은 규칙이다(1.10·1.14·2.4·3.3·3.9·4.4).
 *
 * <h2>빈 Body 는 {@code 3401} 이다 — omit 규칙의 예외</h2>
 *
 * <p>PATCH 의 omit = 유지 규칙대로면 {@code {}} 는 "아무것도 안 바꾼다"로 읽혀 성공해야
 * 한다. 그러나 설계 명세가 이를 거절한다 — 성공으로 흘러가면 <b>{@code modified=true} 만
 * 세워지고</b>, 그 표시가 이후 설정 수정(4.4)의 자동 반영에서 이 달을 영구히 제외한다.
 * 아무것도 바꾸지 않은 달이 "사용자가 직접 손댄 달"로 굳는 것이다.
 */
public final class FixedExpenseMonthlyUpdateRequest {

    /** 이 경로가 받는 이름. 밖의 것은 {@code 9001} 이다. */
    private static final Set<String> UPDATABLE_FIELDS =
            Set.of("amount", "paymentDate", "content", "paymentMethodId");

    private FixedExpenseMonthlyUpdateRequest() {
    }

    /**
     * 요청 Body 를 검증된 {@link PatchFields} 로 바꾼다.
     *
     * @throws BusinessException {@code 9001} 대상 밖 필드 · {@code 3401} 빈 Body
     */
    public static PatchFields of(Map<String, Object> body) {
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);
        if (fields.isEmpty()) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "바꿀 항목을 하나 이상 보내야 합니다.");
        }
        return fields;
    }
}
