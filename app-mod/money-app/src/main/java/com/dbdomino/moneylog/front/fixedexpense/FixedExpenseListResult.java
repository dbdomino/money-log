package com.dbdomino.moneylog.front.fixedexpense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 백엔드 고정지출 설정 목록 응답. <b>쪽 정보가 함께 온다.</b>
 *
 * <h2>009·010 의 목록과 다른 점이다</h2>
 *
 * <p>수단·지출유형(009)과 가계부(010)는 조회 구간을 받지 않고 전부 돌려주었다. 고정지출
 * 목록은 <b>시작점과 개수를 필수로 받으며</b> 시작점이 개수의 배수가 아니면 목록이 통째로
 * 실패한다.
 *
 * <p>그래서 007 의 페이징 환산기를 쓴다 — 008 의 관리자 회원 목록에 이어 두 번째다.
 * <b>"목록이라고 다 같지 않다"</b>가 009 에서 이미 규칙이 됐고 011 이 그 반대쪽 예다.
 *
 * @param list 본인 고정지출 설정 (현재 쪽). 수단·유형 이름이 <b>현재 값</b>으로 함께 온다
 * @param offset 이번 조회에서 건너뛴 건수. 현재 쪽 환산에 쓴다
 * @param limit 이번 조회에서 가져온 최대 건수. 전체 쪽 수 환산에 쓴다
 * @param totalCount 조건에 맞는 <b>전체 건수</b>. 현재 쪽의 건수가 아니다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixedExpenseListResult(
        List<FixedExpenseView> list,
        Integer offset,
        Integer limit,
        Long totalCount) {

    /** 응답이 통째로 비어 왔을 때 쓰는 값. 빈 목록과 건수 0 이다. */
    public static FixedExpenseListResult empty(int offset, int limit) {
        return new FixedExpenseListResult(List.of(), offset, limit, 0L);
    }

    /** 행이 없으면 빈 목록으로 다룬다. 템플릿이 null 을 가리는 분기를 갖지 않게 한다. */
    public List<FixedExpenseView> rows() {
        return list == null ? List.of() : list;
    }

    /** 전체 건수. 없으면 0 으로 본다 — 쪽 수 환산이 터지지 않게 한다. */
    public long total() {
        return totalCount == null ? 0L : totalCount;
    }

    /** 한 쪽에 담긴 개수. 없으면 환산기의 기본값으로 본다. */
    public int pageSize() {
        return limit == null || limit <= 0
                ? com.dbdomino.moneylog.front.web.Paging.DEFAULT_LIMIT
                : limit;
    }

    /** 건너뛴 건수. 없으면 첫 쪽으로 본다. */
    public int skipped() {
        return offset == null || offset < 0 ? 0 : offset;
    }
}
