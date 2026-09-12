package com.dbdomino.moneylog.front.member.form;

import com.dbdomino.moneylog.front.support.PatchBody;
import java.util.Map;

/**
 * 1.7 본인 정보 수정 폼. 칸 다섯이다.
 *
 * <p><b>권한 칸을 두지 않는다.</b> 본인은 자기 권한을 바꿀 수 없고, 백엔드 본인 수정 API 에
 * 그 칸 자체가 없다. 화면에 두면 사용자가 고를 수 있는 값이 서버에서 조용히 무시된다.
 *
 * <p>아이디 칸도 없다. 읽기 전용으로 보이기만 하고 요청에 실리지 않는다.
 *
 * @param nickname 닉네임 (필수라 비울 수 없다)
 * @param email 이메일. 비우면 지운다
 * @param phone 연락처. 사용자가 친 그대로이며 보낼 때 숫자만 남긴다. 비우면 지운다
 * @param intro 자기소개. 비우면 지운다
 * @param newPassword 새 비밀번호. <b>비우면 바꾸지 않는다</b> — 다른 칸과 뜻이 반대다
 */
public record MemberProfileForm(
        String nickname,
        String email,
        String phone,
        String intro,
        String newPassword) {

    /**
     * 백엔드 {@code MemberUpdateMe} 요청 본문으로 옮긴다.
     *
     * <p>빈 칸을 칸마다 다르게 보내는 것이 이 화면의 핵심 규칙이다. 새 비밀번호만 싣지 않고
     * 나머지는 비우라는 값으로 싣는다.
     */
    public Map<String, Object> toRequest() {
        return new PatchBody()
                .always("nickname", nickname)
                .clearIfBlank("email", email)
                .phoneClearIfBlank("phone", phone)
                .clearIfBlank("intro", intro)
                .omitIfBlank("password", newPassword)
                .toMap();
    }
}
