package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.util.Map;
import java.util.Set;

/**
 * PATCH 요청 Body. <b>필드를 보내지 않은 것</b>과 <b>{@code null} 을 보낸 것</b>을 구분한다.
 *
 * <table border="1">
 *   <caption>PATCH omit 규칙(api-contract.md §4)</caption>
 *   <tr><th>요청</th><th>동작</th></tr>
 *   <tr><td>필드가 Body 에 없음</td><td>기존 값 유지</td></tr>
 *   <tr><td>{@code "email": null}</td><td>{@code null} 로 갱신(값 지움)</td></tr>
 *   <tr><td>{@code "email": "a@b.c"}</td><td>그 값으로 갱신</td></tr>
 * </table>
 *
 * <p><b>이 구분이 없으면 "이메일을 지우는" 조작이 불가능해진다.</b> 레코드나 클래스로 받으면
 * Jackson 이 둘을 똑같이 {@code null} 로 만들어 버려, 서버는 "지워 달라"와 "건드리지 마라"를
 * 구별할 수 없다.
 *
 * <p>그래서 {@code Map} 으로 받고 {@code containsKey} 로 판정한다. 대안이던
 * {@code JsonNullable} 은 별도 라이브러리가 필요하고 Jackson 3 대응도 확인해야 해서,
 * 표준 자료구조 하나로 끝나는 이 방식을 골랐다. <b>1.8 과 1.15 가 같은 방식을 쓴다</b> —
 * 두 API 가 다른 방식을 쓰면 프론트가 수정 화면마다 다르게 만들어야 한다.
 *
 * <p>Bean Validation 이 걸리지 않으므로 값 검증은 서비스가 {@link MemberFieldRules} 로
 * 직접 한다. 어차피 비밀번호({@code 2004})처럼 코드가 갈리는 규칙이 있어 서비스 검증이
 * 필요했다.
 */
public final class PatchFields {

    private final Map<String, Object> fields;

    private PatchFields(Map<String, Object> fields) {
        this.fields = fields == null ? Map.of() : fields;
    }

    /**
     * Body 를 감싼다.
     *
     * @param allowedNames 허용하는 필드 이름. 그 밖의 이름이 오면 {@code 9001} 이다 —
     *                     오타를 조용히 무시하면 "수정했는데 안 바뀐다"가 된다
     */
    public static PatchFields of(Map<String, Object> body, Set<String> allowedNames) {
        if (body != null) {
            for (String name : body.keySet()) {
                if (!allowedNames.contains(name)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "수정할 수 없는 필드입니다: " + name);
                }
            }
        }
        return new PatchFields(body);
    }

    /** 이 필드가 Body 에 있었는가. {@code null} 을 보낸 경우에도 참이다. */
    public boolean has(String name) {
        return fields.containsKey(name);
    }

    /** 바꿀 필드가 하나도 없는가. */
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /**
     * 문자열 값. 보내지 않았으면 {@code null} 이므로 {@link #has(String)} 로 먼저 가른다.
     *
     * @throws BusinessException {@code 9001} — 문자열이 아닌 값이 왔다
     */
    public String string(String name) {
        Object value = fields.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    name + " 은(는) 문자열이어야 합니다.");
        }
        return text.isBlank() ? text : text.trim();
    }

    /**
     * 정수 값. 보내지 않았으면 {@code null} 이므로 {@link #has(String)} 로 먼저 가른다.
     *
     * <p><b>{@code Integer} 와 {@code Long} 을 모두 받는다.</b> Jackson 은 JSON 숫자를 크기에
     * 따라 둘 중 하나로 만드는데, 그 차이가 호출부까지 새어 나가면 "작은 금액은 되고 큰
     * 금액은 안 되는" 판정이 생긴다.
     *
     * <p>소수점·문자는 여기서 {@code 9001} 이 아니라 <b>호출자가 정한 코드</b>로 거절해야
     * 하는 경우가 있어(지출 {@code 3201} · 소득 {@code 3301}) {@link #hasNonIntegerNumber}
     * 로 미리 가려낼 수 있게 했다.
     *
     * @throws BusinessException {@code 9001} — 숫자가 아닌 값이 왔다
     */
    public Long longNumber(String name) {
        Object value = fields.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer number) {
            return number.longValue();
        }
        if (value instanceof Long number) {
            return number;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, name + " 은(는) 정수여야 합니다.");
    }

    /**
     * 이 필드에 <b>정수가 아닌 숫자·문자</b>가 들어 있는가.
     *
     * <p>금액처럼 실패 코드가 API 마다 다른 필드는 {@link #longNumber} 가 던지는
     * {@code 9001} 대신 호출자의 코드({@code 3201}·{@code 3301})로 거절해야 한다. 그때
     * 먼저 이걸로 가른다.
     */
    public boolean hasNonIntegerNumber(String name) {
        Object value = fields.get(name);
        return value != null && !(value instanceof Integer) && !(value instanceof Long);
    }

    /**
     * 참/거짓 값. 보내지 않았으면 {@code null} 이므로 {@link #has(String)} 로 먼저 가른다.
     *
     * <p><b>{@code "true"} 같은 문자열을 받아 주지 않는다.</b> 관대하게 해석하면 오타
     * ({@code "ture"})가 조용히 {@code false} 가 되어 "껐는데 안 꺼진다"가 된다.
     *
     * @throws BusinessException {@code 9001} — 참/거짓이 아닌 값이 왔다.
     *                           {@code null} 은 여기서 걸리지 않으므로 nullable 이 아닌
     *                           필드는 호출자가 따로 막는다
     */
    public Boolean bool(String name) {
        Object value = fields.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean flag)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    name + " 은(는) true 또는 false 여야 합니다.");
        }
        return flag;
    }
}
