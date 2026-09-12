package com.dbdomino.moneylog.front.member;

import com.dbdomino.moneylog.front.session.SessionUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 화면이 보여 주는 회원 값. 1.7 본인 정보와 1.8~1.10 관리자 화면이 함께 쓴다.
 *
 * <h2>비밀번호 자리가 없다</h2>
 *
 * <p>백엔드가 내려주지 않고 화면도 담지 않는다는 것을 <b>타입이 보장한다.</b> 자리를 두고
 * "채우지 말자"고 약속하는 것보다, 채울 자리가 없는 편이 낫다 — 약속은 화면이 늘수록 새고
 * 새는 자리를 사람이 세야 한다.
 *
 * <p>새 비밀번호는 저장 요청에만 실린다. 그 값은 폼에서 요청으로 곧장 가고 이 타입을 거치지
 * 않는다.
 *
 * <h2>회원이 언제 만들어졌는지도 없다</h2>
 *
 * <p>백엔드 목록 응답에 그 값이 없다. 응답에 없는 값을 타입에 두면 언제나 비어 있게 되고,
 * 화면은 그것을 "모르는 회원"으로 그린다.
 *
 * <h2>모르는 항목이 와도 깨지지 않는다</h2>
 *
 * <p>백엔드가 뒤에 항목을 더해도 화면은 자기가 쓰는 것만 읽는다. 화면이 쓰지 않는 항목까지
 * 타입에 두면 "이건 어디 쓰나"를 읽는 사람이 매번 되묻게 된다.
 *
 * @param memberId 로그인 아이디. 어느 화면에서도 고칠 수 없다
 * @param nickname 닉네임
 * @param email 이메일. 등록하지 않았으면 {@code null}
 * @param phone 연락처. 숫자만인 저장값이다. 등록하지 않았으면 {@code null}
 * @param intro 자기소개. 입력하지 않았으면 {@code null}
 * @param role 권한. {@code 1} 관리자 · {@code 3} 일반
 * @param active 활성 여부. <b>관리자 목록에만 있다</b> — 본인 조회 응답에는 없거나 항상 참이라
 *        {@code null} 로 올 수 있고, 그때는 화면이 상태 열을 그리지 않는다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberView(
        String memberId,
        String nickname,
        String email,
        String phone,
        String intro,
        Integer role,
        Boolean active) {

    public boolean isAdmin() {
        return role != null && role == SessionUser.ROLE_ADMIN;
    }

    /** 권한을 화면에 보일 말로 바꾼다. 목록에 숫자를 그대로 내보내지 않는다. */
    public String roleLabel() {
        return isAdmin() ? "관리자" : "일반";
    }

    /** 상태를 화면에 보일 말로 바꾼다. 값이 없으면 활성으로 본다(본인 조회 응답이 그렇다). */
    public String statusLabel() {
        return active == null || active ? "활성" : "정지";
    }

    /** 정지된 회원인가. 목록이 정지 버튼을 그릴지 정할 때 본다. */
    public boolean isDeactivated() {
        return active != null && !active;
    }
}
