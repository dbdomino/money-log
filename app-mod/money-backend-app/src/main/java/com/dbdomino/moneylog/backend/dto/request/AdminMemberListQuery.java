package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 1.13 관리자 회원 목록의 조회 조건.
 *
 * <p><b>{@code offset}·{@code limit} 은 필수이고 기본값이 없다</b>(FR-120). 그래서
 * Spring Data 의 {@code Pageable} 자동 바인딩을 쓰지 않는다 — 그쪽은 {@code page}·
 * {@code size} 모델이고 값이 빠지면 기본값을 채워 조용히 통과시키는데, 우리 계약은
 * 빠졌으면 {@code 9001} 로 거절해야 한다(research.md §12).
 *
 * <p>{@code offset} 이 {@code limit} 의 배수여야 한다는 조건은 페이지 경계에 맞지 않는
 * 임의 offset 을 막아 목록이 겹쳐 보이는 것을 방지한다.
 *
 * @param offset   건너뛸 건수. 0 이상이고 {@code limit} 의 배수
 * @param limit    가져올 건수. 1 이상
 * @param memberId 아이디 부분 일치 검색어(선택)
 * @param nickname 닉네임 부분 일치 검색어(선택). {@code memberId} 와 함께 오면 AND 다
 */
public record AdminMemberListQuery(int offset, int limit, String memberId, String nickname) {

    /**
     * 값을 검증해 조회 조건을 만든다.
     *
     * <p>세 조건을 한 자리에 모은다 — 흩어 두면 어느 하나만 빠져도 그 조합에서 목록이
     * 어긋나는데, 그 사실이 화면에서 바로 드러나지 않는다.
     *
     * @throws BusinessException {@code 9001} — 누락·범위 오류·배수 아님
     */
    public static AdminMemberListQuery of(Integer offset, Integer limit,
                                          String memberId, String nickname) {
        if (offset == null || limit == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "offset 과 limit 은 필수입니다.");
        }
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 은 1 이상이어야 합니다.");
        }
        if (offset < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "offset 은 0 이상이어야 합니다.");
        }
        if (offset % limit != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "offset 은 limit 의 배수여야 합니다.");
        }
        return new AdminMemberListQuery(offset, limit, blankToNull(memberId), blankToNull(nickname));
    }

    /** 검색어의 빈 문자열은 "검색하지 않음"과 같게 다룬다. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** {@code offset}/{@code limit} 을 페이지 번호로 환산한다. 배수 검증을 통과했으므로 나누어떨어진다. */
    public int pageNumber() {
        return offset / limit;
    }
}
