package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 4.2 고정지출 설정 목록.
 *
 * <p><b>페이징 필드가 {@code list} 와 같은 레벨이다</b>(FR-423). {@code data.list} 와
 * {@code data.totalCount} 가 형제이며 {@code list} 안이 아니다.
 *
 * <p><b>005 의 목록 넷 중 페이징이 있는 것은 여기뿐이다.</b> 4.5(월별 내역)·4.8(가계부)·
 * 4.9(재작성 결과)는 한 달치를 전부 돌려주고 합계·건수를 대신 싣는다 —
 * {@code _공통.md § 목록 응답 규칙} 의 적용 대상 표가 그렇게 확정했다.
 *
 * @param list       고정지출 설정 목록. 각 요소의 이름 두 개는 <b>현재 이름</b>이다
 * @param offset     이번 조회에서 건너뛴 건수. 요청값과 같다. 프론트가 현재 페이지 환산에 쓴다
 * @param limit      이번 조회에서 가져온 최대 건수. 요청값과 같다
 * @param totalCount 조건에 맞는 <b>전체 건수</b>. {@code list} 길이가 아니다 — 현재 페이지
 *                   건수를 돌려주면 화면이 마지막 페이지를 계산할 수 없다
 */
public record FixedExpenseListResponse(
        List<FixedExpenseResponse> list,
        int offset,
        int limit,
        long totalCount) {
}
