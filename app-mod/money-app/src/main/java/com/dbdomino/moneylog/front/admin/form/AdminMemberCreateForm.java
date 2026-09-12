package com.dbdomino.moneylog.front.admin.form;

import com.dbdomino.moneylog.front.support.PhoneNumbers;

/**
 * 1.9 회원 추가 폼. 칸 일곱이고 필수는 아이디·비밀번호·닉네임·권한 넷이다.
 *
 * <p><b>비밀번호 확인 칸이 없다.</b> 백엔드 생성 API 가 확인 값을 받지 않는다 — 관리자가 만든
 * 임시 비밀번호를 회원에게 알려 주는 흐름이라 회원가입(1.2)과 다르다. 1.2 는 본인이 정한 값을
 * 두 번 쳐서 오타를 거르지만, 여기서는 관리자가 값을 그대로 알고 있다.
 *
 * @param memberId 로그인 아이디 (필수)
 * @param password 임시 비밀번호 평문 (필수)
 * @param nickname 닉네임 (필수)
 * @param role 권한 (필수). 관리자 {@code 1} 또는 일반 {@code 3} 두 값뿐이다
 * @param email 이메일 (선택)
 * @param phone 연락처 (선택). 사용자가 친 그대로다
 * @param intro 자기소개 (선택)
 */
public record AdminMemberCreateForm(
        String memberId,
        String password,
        String nickname,
        Integer role,
        String email,
        String phone,
        String intro) {

    /**
     * 백엔드 {@code AdminMemberCreate} 요청 본문으로 옮긴다.
     *
     * <p>새로 만드는 자리라 선택 칸을 비웠으면 싣지 않는다. 수정과 달리 "비운다"와 "안 넣는다"의
     * 결과가 같다 — 아직 지울 값이 없다.
     */
    public Request toRequest() {
        return new Request(memberId, password, nickname, role,
                blankToNull(email), PhoneNumbers.digitsOnly(phone), blankToNull(intro));
    }

    private static String blankToNull(String value) {
        return PhoneNumbers.isBlank(value) ? null : value;
    }

    /** 백엔드 {@code AdminMemberCreate} 요청 본문. */
    public record Request(
            String memberId,
            String password,
            String nickname,
            Integer role,
            String email,
            String phone,
            String intro) {
    }
}
