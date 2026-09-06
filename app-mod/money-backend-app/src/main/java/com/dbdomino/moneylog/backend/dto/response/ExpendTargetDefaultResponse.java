package com.dbdomino.moneylog.backend.dto.response;

/**
 * 5.3 기본 목표 등록·수정의 결과.
 *
 * <p><b>담당한 층만 돌려준다</b> — 월별 목표는 이 호출로 변하지 않으므로(FR-505) 여기
 * 실을 이유가 없다. 두 층을 다 보려면 5.2 나 5.1 을 부른다. 함께 실으면 "이 호출이 월별도
 * 건드렸나"라는 오해를 만든다.
 *
 * @param expendGroupId       지출유형 PK
 * @param expendGroupName     지출유형의 <b>현재</b> 이름
 * @param defaultTargetAmount 저장된 월 단위 기본 목표금액
 */
public record ExpendTargetDefaultResponse(
        long expendGroupId,
        String expendGroupName,
        long defaultTargetAmount) {
}
