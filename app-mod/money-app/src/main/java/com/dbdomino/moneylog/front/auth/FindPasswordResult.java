package com.dbdomino.moneylog.front.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 백엔드 {@code MemberFindPassword} 성공 응답.
 *
 * <p>{@code matched} 는 언제나 참이다 — 맞지 않으면 이 응답이 아니라 실패 코드가 온다. 그래도
 * 받아 두는 이유는 백엔드 계약에 있는 값을 화면이 임의로 버리지 않기 위해서다.
 *
 * <p>확인된 아이디는 <b>화면 표시용</b>이며 가려져 올 수 있다. 그래서 저장 요청에 실을 값은
 * 이것이 아니라 사용자가 1.4 에 입력한 아이디다 — 가려진 값을 실어 보내면 백엔드가 대조에
 * 실패한다.
 *
 * @param matched 언제나 {@code true}
 * @param memberId 확인된 아이디. 표시용이며 가려질 수 있다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FindPasswordResult(boolean matched, String memberId) {
}
