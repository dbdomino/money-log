package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 4.2 고정지출 설정 목록의 조회 조건.
 *
 * <p><b>005 의 목록 넷 중 페이징이 있는 것은 4.2 뿐이다.</b> 4.5(월별 내역)·4.8(가계부)·
 * 4.9(재작성 결과)는 한 달치를 전부 돌려주고 합계·건수를 대신 싣는다 —
 * {@code _공통.md § 목록 응답 규칙}의 적용 대상 표가 그렇게 확정했다.
 *
 * <p>규칙은 002 의 {@code AdminMemberListQuery}(1.13)와 같다. {@code offset}·{@code limit}
 * 은 <b>필수이고 기본값이 없다</b> — 그래서 Spring Data 의 {@code Pageable} 자동 바인딩을
 * 쓰지 않는다. 그쪽은 {@code page}·{@code size} 모델이고 값이 빠지면 기본값을 채워 조용히
 * 통과시키는데, 우리 계약은 빠졌으면 {@code 9001} 로 거절해야 한다.
 *
 * <p>{@code offset} 이 {@code limit} 의 배수여야 한다는 조건은 페이지 경계에 맞지 않는
 * 임의 offset 을 막아 목록이 겹쳐 보이는 것을 방지한다.
 *
 * @param offset 건너뛸 건수. 0 이상이고 {@code limit} 의 배수
 * @param limit  가져올 건수. 1 이상
 */
public record FixedExpenseListQuery(int offset, int limit) {

    /**
     * 값을 검증해 조회 조건을 만든다.
     *
     * @throws BusinessException {@code 9001} — 누락·범위 오류·배수 아님
     */
    public static FixedExpenseListQuery of(Integer offset, Integer limit) {
        if (offset == null || limit == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 과 limit 은 필수입니다.");
        }
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 은 1 이상이어야 합니다.");
        }
        if (offset < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 은 0 이상이어야 합니다.");
        }
        if (offset % limit != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 은 limit 의 배수여야 합니다.");
        }
        return new FixedExpenseListQuery(offset, limit);
    }

    /** {@code offset}/{@code limit} 을 페이지 번호로 환산한다. 배수 검증을 통과했으므로 나누어떨어진다. */
    public int pageNumber() {
        return offset / limit;
    }
}
