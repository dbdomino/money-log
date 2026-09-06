package com.dbdomino.moneylog.backend.dto.response;

/**
 * 관리자가 보는 회원 정보. 추가(1.12)·목록 항목(1.13)·상세(1.14)·수정(1.15)이 함께 쓴다.
 *
 * <p>{@link MemberResponse} 와 다른 점은 <b>{@code active} 가 있다</b>는 것뿐이다.
 * 본인 조회에서는 항상 참이라 의미가 없지만, 관리자에게는 정지 여부가 핵심 정보다.
 *
 * <p>비밀번호는 여기에도 없다. 관리자라도 남의 해시를 볼 이유가 없다.
 *
 * @param memberId 로그인 아이디
 * @param nickname 닉네임
 * @param email    이메일. 선택 항목이라 비어 있을 수 있다
 * @param phone    연락처. 하이픈 없이 숫자만 저장된 값이다
 * @param intro    자기소개. 선택 항목이라 비어 있을 수 있다
 * @param role     권한. {@code 1} 관리자, {@code 3} 일반
 * @param active   활성 여부. {@code false} 면 정지된 회원이다
 */
public record AdminMemberResponse(String memberId, String nickname, String email,
                                  String phone, String intro, short role, boolean active) {
}
