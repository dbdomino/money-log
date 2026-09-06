package com.dbdomino.moneylog.backend.dto.response;

/**
 * 2.12 지출유형 삭제 응답.
 *
 * <p>삭제는 <b>표시</b>다. 행도 남고 <b>아이콘 파일도 남는다</b>(FR-215) — 목표금액·통계가
 * 그 유형을 계속 참조하므로 화면이 옛 기록을 그릴 때 아이콘이 필요하다.
 *
 * <p><b>{@code deleted} 필드가 없다.</b> 수단 삭제(2.5)의 응답에는 있는데 여기에는 없는 것이
 * 명세가 정한 형태다(2.12) — 삭제 API 의 성공 응답에서 그 값은 언제나 {@code true} 라
 * 읽을 것이 없다는 판단이다. 두 API 의 형태가 갈리는 것은 <b>알고 있는 차이</b>이며,
 * 맞추려면 명세를 먼저 고쳐야 한다(헌장 원칙 V).
 *
 * @param expendGroupId 삭제 표시된 유형의 대리키
 * @param message       화면에 보여 줄 문구
 */
public record ExpendGroupDeleteResponse(Long expendGroupId, String message) {
}
