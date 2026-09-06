package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.common.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 회원 유니크 제약 위반을 응답 코드로 옮긴다. 가입(1.2)과 관리자 추가(1.12)가 공유한다.
 *
 * <p><b>선검사만으로는 부족하다.</b> "이미 있는지" 확인과 INSERT 사이에 다른 요청이 먼저
 * 커밋하면 선검사는 통과하고 DB 제약이 막는다. 그 위반을 잡지 않으면 사용자에게
 * {@code 9000}(서버 오류)이 나가는데, 실제로는 "중복"이라는 평범한 결과다.
 *
 * <p>두 API 가 같은 판정을 쓰도록 여기 모은다 — 한쪽만 처리하면 같은 상황에서 응답이
 * 갈린다.
 */
final class MemberDuplicationRules {

    /** 부분 유니크 인덱스 이름. 이메일 쪽 위반인지 가리는 기준이다. */
    private static final String EMAIL_INDEX = "ux_user_email";

    private MemberDuplicationRules() {
    }

    /**
     * 유니크 위반이 아이디 쪽인지 이메일 쪽인지 가린다.
     *
     * <p>제약 이름은 드라이버 메시지에만 있어 문자열로 판정한다. 이름을 못 찾으면
     * 아이디 중복으로 본다 — {@code tbl_user} 의 유니크 제약은 둘뿐이고, 아이디가 필수라
     * 그쪽이 훨씬 흔하다.
     */
    static ErrorCode codeOf(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(EMAIL_INDEX)
                ? ErrorCode.EMAIL_DUPLICATED
                : ErrorCode.MEMBER_ID_DUPLICATED;
    }
}
