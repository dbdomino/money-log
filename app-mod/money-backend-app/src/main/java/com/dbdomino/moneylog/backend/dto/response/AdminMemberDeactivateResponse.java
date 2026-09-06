package com.dbdomino.moneylog.backend.dto.response;

/**
 * 1.16 회원 정지 응답.
 *
 * <p>정지는 <b>표시만 바꾸는 것</b>이다. 회원 행도 그 회원의 가계부 데이터도 지우지
 * 않는다(FR-118) — 정지가 취소될 수 있고, 통계·이력이 그 데이터를 참조한다.
 *
 * @param memberId 정지된 아이디
 * @param active   항상 {@code false}
 * @param message  화면에 보여 줄 문구
 */
public record AdminMemberDeactivateResponse(String memberId, boolean active, String message) {
}
