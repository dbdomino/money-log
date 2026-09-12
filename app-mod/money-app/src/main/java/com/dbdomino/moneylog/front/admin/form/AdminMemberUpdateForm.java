package com.dbdomino.moneylog.front.admin.form;

import com.dbdomino.moneylog.front.support.PatchBody;
import java.util.Map;

/**
 * 1.10 회원 수정 폼. 칸 여섯이다.
 *
 * <p><b>빈 칸의 뜻은 본인 정보(1.7)와 같다</b> — 새 비밀번호는 비우면 유지, 이메일·폰·소개는
 * 비우면 비움이다. 두 화면이 같은 {@link PatchBody} 를 쓰는 이유는, 각자 판단하면 한쪽만
 * 고쳤을 때 "관리자가 고치면 지워지는데 본인이 고치면 안 지워지는" 화면이 되기 때문이다.
 *
 * <p>1.7 과 다른 점은 <b>권한 칸이 있다</b>는 것이다. 본인은 자기 권한을 바꿀 수 없지만
 * 관리자는 남의 권한을 바꾼다.
 *
 * <p>아이디 칸은 없다. 주소의 식별자로만 쓰이고 요청 본문에 실리지 않는다.
 *
 * @param nickname 닉네임 (필수라 비울 수 없다)
 * @param role 권한. 관리자 {@code 1} 또는 일반 {@code 3} 두 값뿐이다
 * @param email 이메일. 비우면 지운다
 * @param phone 연락처. 비우면 지우고, 값이 있으면 숫자만 남긴다
 * @param intro 자기소개. 비우면 지운다
 * @param newPassword 새 비밀번호. <b>비우면 바꾸지 않는다</b>
 */
public record AdminMemberUpdateForm(
        String nickname,
        Integer role,
        String email,
        String phone,
        String intro,
        String newPassword) {

    /** 백엔드 {@code AdminMemberUpdate} 요청 본문으로 옮긴다. */
    public Map<String, Object> toRequest() {
        return new PatchBody()
                .always("nickname", nickname)
                .always("role", role)
                .clearIfBlank("email", email)
                .phoneClearIfBlank("phone", phone)
                .clearIfBlank("intro", intro)
                .omitIfBlank("password", newPassword)
                .toMap();
    }
}
