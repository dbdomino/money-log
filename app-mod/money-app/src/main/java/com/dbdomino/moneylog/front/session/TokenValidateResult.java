package com.dbdomino.moneylog.front.session;

/**
 * 토큰 검증 응답. 진입 판정이 이것 하나로 "세션이 아직 살아 있는가"를 확인한다.
 *
 * <p>{@code valid} 는 언제나 참이다 — 유효하지 않으면 이 응답이 아니라 실패 코드가 오기
 * 때문이다. 그래도 필드를 받아 두는 이유는 백엔드 계약에 있는 값을 화면 모듈이 임의로
 * 버리지 않기 위해서다.
 *
 * <p>만료까지 남은 초를 받지만 <b>세션에 담지 않는다.</b> 화면 모듈이 만료를 미리 판정하지
 * 않기 때문이다.
 *
 * @param valid 언제나 {@code true}
 * @param memberId 토큰이 가리키는 로그인 아이디
 * @param role 권한. {@code 1} 관리자 · {@code 3} 일반
 * @param expiresIn 접근 토큰 만료까지 남은 초
 */
public record TokenValidateResult(boolean valid, String memberId, int role, long expiresIn) {
}
