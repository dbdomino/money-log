package com.dbdomino.moneylog.common.error;

/**
 * 실패 응답의 {@code data} 에 <b>메시지 말고 더 실을 것이 있는</b> 비즈니스 실패.
 *
 * <p>보통의 {@link BusinessException} 은 {@code { resCode, data: { message } }} 로 나간다.
 * 그것으로 충분하지 않은 경우가 하나 있다 — 004 의 엑셀 업로드({@code 3502})는
 * <b>행별 오류 목록</b>을 함께 줘야 프론트가 "표의 N행 M열을 고치세요"를 안내할 수 있다.
 *
 * <h2>왜 Controller 가 응답을 조립하지 않는가</h2>
 *
 * <p>헌장 원칙 III 이 "비즈니스·검증 실패는 커스텀 예외로 던지고 전역 예외 처리에 위임한다"
 * 고 정했다. Controller 가 실패 응답을 직접 만들면 그 규칙이 API 마다 갈리고, 성공·실패의
 * 형태를 한 곳에서 보장할 수 없게 된다.
 *
 * <h2>왜 {@code Object} 로 받는가</h2>
 *
 * <p>상세의 실제 타입({@code ExcelImportResponse} 등)은 <b>앱 모듈이 소유한다</b>.
 * {@code common-mod} 는 프론트 {@code money-app} 도 의존하는 최하위 모듈이라 004 의 타입을
 * 알면 원칙 I 의 단방향 의존이 깨진다. 그래서 여기서는 <b>실어 나르기만</b> 하고 직렬화는
 * Jackson 이 실제 타입을 보고 한다.
 *
 * <p>{@link GlobalExceptionHandler} 가 이 예외를 만나면 {@code data} 자리에 {@link #details}
 * 를 그대로 넣는다 — {@code message} 도 그 안에 담아 보내야 한다.
 */
public class DetailedBusinessException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final transient Object details;

    /**
     * @param errorCode 실패 코드
     * @param details   응답의 {@code data} 가 될 객체. <b>{@code message} 를 포함해</b>
     *                  프론트가 필요한 것을 전부 담는다 — 이 값이 {@code data} 를 통째로
     *                  대신하므로, 담지 않은 것은 응답에 나타나지 않는다
     */
    public DetailedBusinessException(ErrorCode errorCode, Object details) {
        super(errorCode, errorCode.message());
        this.details = details;
    }

    /** 응답 {@code data} 에 실릴 객체. */
    public Object getDetails() {
        return details;
    }
}
