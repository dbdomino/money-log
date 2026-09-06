package com.dbdomino.moneylog.backend.dto.response;

/**
 * 회원 정보. 본인 조회·수정(1.7·1.8)과 관리자 상세(1.14)가 함께 쓴다.
 *
 * <p><b>비밀번호 필드를 두지 않는다.</b> 응답에 해시가 실리지 않는 것을 이 타입의 모양이
 * 보장한다 — SC-107 은 그 사실을 확인하는 검사이지 방어 수단이 아니다.
 *
 * <p>{@code active} 도 두지 않는다. 본인 조회에서는 항상 {@code true} 라 의미가 없고,
 * 관리자 목록·상세가 필요로 하면 그쪽 응답 타입에서 다룬다.
 *
 * @param memberId 로그인 아이디
 * @param nickname 닉네임
 * @param email    이메일. 선택 항목이라 비어 있을 수 있다
 * @param phone    연락처. 하이픈 없이 숫자만 저장된 값이다
 * @param intro    자기소개. 선택 항목이라 비어 있을 수 있다
 * @param role     권한. {@code 1} 관리자, {@code 3} 일반
 */
public record MemberResponse(String memberId, String nickname, String email,
                             String phone, String intro, short role) {
}
