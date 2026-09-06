package com.dbdomino.moneylog.backend.dto.response;

import java.time.OffsetDateTime;

/**
 * 5.6 통계 저장의 결과.
 *
 * <h2>다섯 필드뿐이다 — 통계 본문을 싣지 않는다</h2>
 *
 * <p>사용자는 5.5 로 그 달을 <b>보다가</b> 저장을 누른다. 화면에 숫자가 이미 있으므로
 * 필요한 것은 "저장됨 · 그 시각" 배지를 바꾸는 것뿐이다. 본문까지 실으면 같은 값이 두
 * 응답에 흩어져 갈릴 여지만 생긴다.
 *
 * <p><b>그래서 재조회가 필요 없다</b>(api-contract §8). 저장 → 재조회의 왕복 두 번이
 * 한 번으로 줄어든다 — {@code savedAt} 과 {@code source} 가 없으면 화면이 배지를 채우려고
 * 5.5 를 한 번 더 불러야 한다.
 *
 * @param year    저장한 연도
 * @param month   저장한 월
 * @param savedAt 저장 시각. 재저장이면 <b>새 시각</b>이다
 * @param source  항상 {@code SAVED}. 이 API 는 저장만 하므로 다른 값이 나올 수 없지만,
 *                5.5 응답과 <b>같은 이름·같은 값 집합</b>이라야 화면이 두 응답을 같은
 *                코드로 다룰 수 있다
 * @param message 사람이 읽는 확인 문구
 */
public record StatisticsSaveResponse(
        int year,
        int month,
        OffsetDateTime savedAt,
        String source,
        String message) {
}
