package com.dbdomino.moneylog.common.constants;

import org.springframework.http.HttpStatus;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 *
 * 에러코드 정의
 */
public enum CodeCommon {

    SUCCESS(HttpStatus.OK.value(), "성공"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "내부 서버 오류"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "잘못된 요청"),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), "찾을수 없음"),
    FORBIDDEN(HttpStatus.FORBIDDEN.value(), "금지됨"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "승인되지 않음"),

    // Auth
    INVALID_TOKEN(1001, "옳지 않은 토큰 입니다."),
    EXPIRED_TOKEN(1002, "만료된 토큰 입니다."),
    UNSUPPORTED_TOKEN(1003, "지원 되지 않는 토큰 입니다."),
    EMPTY_PAYLOAD(1004, "옳지 않은 토큰의 Payload 입니다."),
    STOPPED_USER(1005, "사용이 중지된 사용자 입니다."),

    // USER
    INVALID_ID(2001, "잘못된 아이디입니다."),
    INVALID_PW(2002, "잘못된 패스워드 입니다."),
    TEMPORARY_PW(2003, "임시 비밀번호입니다. 비밀번호 재설정이 필요합니다."),

    // User
    NOT_FOUND_ID(2004, "이름과 이메일로 아이디를 찾지 못했습니다."),
    DUPLICATED_ID(2005, "동일한 ID의 사용자가 존재합니다."),
    DUPLICATED_EMAIL(2006, "동일한 Email의 사용자가 존재합니다."),
    INVALID_PARTNER(2007, "존재하지 않는 제휴사 정보입니다."),
    NOT_FOUND_USER(2008, "아이디와 이메일로 사용자를 찾지 못했습니다."),
    INVALID_USER(2009, "존재하지 않는 사용자 정보입니다."),
    INVALID_TEMP_PW(2010, "발급한 임시비밀번호가 만료되거나 유효하지 않습니다."),
    DUPLICATED_PHONENO(2011, "동일한 휴대폰 번호의 사용자가 존재합니다."),
    BAD_REQUEST_PARTNER_IDX(HttpStatus.BAD_REQUEST.value(), "옳지 않은 파라미터 : partnerIdx"),

    // INF-SAP
    NOT_FOUND_CONTRACT(6201, "등록되지 않은 계약코드입니다."),

    //INF_SQS
    EMPTY_MESSAGE(6601, "메시지가 없습니다."),
    FAIL_RECEIVE_MESSAGE(6602, "메시지 수신에 실패했습니다."),
    FAIL_DELETE_MESSAGE(6603, "메시지 삭제에 실패했습니다."),

    //INF-SES
    FAIL_SEND_EMAIL(6401, "이메일 발송에 실패했습니다.")
    ;
    private final int code;
    private final String message;

    CodeCommon(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static CodeCommon getServiceResponse(int code) {
        for (CodeCommon response : CodeCommon.values()) {
            if (response.getCode() == code) {
                return response;
            }
        }
        return CodeCommon.INTERNAL_SERVER_ERROR;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
