package com.dbdomino.moneylog.front.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 백엔드 {@code MemberFindId} 성공 응답.
 *
 * <p>가림 여부를 함께 받는 것이 요점이다. 가려진 값이면 화면이 <b>그 사실을 적어야</b> 하고,
 * 적지 않으면 사용자는 가려진 값이 진짜 아이디라고 믿고 그대로 로그인을 시도한다.
 *
 * <p><b>가려진 아이디를 화면이 풀지 않는다.</b> 무엇을 얼마나 가릴지는 백엔드가 판단한다.
 *
 * @param memberId 조회된 아이디. 일부가 가려져 올 수 있다
 * @param masked 가림이 적용됐는가
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FindIdResult(String memberId, boolean masked) {
}
