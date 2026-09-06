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
}
