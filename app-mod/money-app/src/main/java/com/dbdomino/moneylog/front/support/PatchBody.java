package com.dbdomino.moneylog.front.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 수정 요청 본문을 만든다. <b>빈 칸의 뜻이 칸마다 다르다</b>는 규칙이 여기 산다.
 *
 * <p>백엔드 수정 API 는 보내지 않은 항목을 그대로 유지하고, 값을 비우려면 {@code null} 을
 * 명시해야 한다. 그래서 화면은 "건드리지 않았다"와 "비웠다"를 <b>다르게 보내야</b> 한다.
 *
 * <table>
 *   <caption>칸마다 다른 빈 칸의 뜻</caption>
 *   <tr><th>칸</th><th>비어 있으면</th><th>부르는 메서드</th></tr>
 *   <tr><td>새 비밀번호</td><td>바꾸지 않는다</td><td>{@link #omitIfBlank}</td></tr>
 *   <tr><td>이메일 · 폰 · 소개</td><td>비운다</td><td>{@link #clearIfBlank}</td></tr>
 *   <tr><td>닉네임 · 권한</td><td>(비울 수 없다)</td><td>{@link #always}</td></tr>
 * </table>
 *
 * <p>새 비밀번호만 다른 이유는 <b>그 칸만 현재 값이 채워지지 않은 채 뜨기 때문</b>이다. 비어
 * 있는 것이 기본 상태이므로 "비웠다"가 아니라 "건드리지 않았다"로 읽는 것이 맞다. 나머지는
 * 현재 값이 채워진 채로 뜨므로 사용자가 지웠다면 지우려는 뜻이고, 이것을 "유지"로 읽으면
 * 사용자는 이메일을 지우고 저장했는데 그대로 남아 있는 화면을 보게 된다.
 *
 * <h2>왜 레코드가 아니라 지도인가</h2>
 *
 * <p>레코드로 만들면 항목을 <b>빼는</b> 방법이 없다. {@code null} 을 담으면 "비워 달라"로
 * 나가고, 직렬화에서 {@code null} 을 걸러내게 하면 이번에는 "비워 달라"를 보낼 방법이
 * 사라진다. 한 본문 안에 두 뜻이 함께 있어야 해서 키를 넣고 빼는 방식이어야 한다.
 *
 * <p>1.7 본인 정보와 1.10 관리자 회원 수정이 같은 규칙을 쓴다. 두 화면이 각자 판단하면
 * 한쪽만 고쳤을 때 "관리자가 고치면 지워지는데 본인이 고치면 안 지워지는" 화면이 된다.
 */
public final class PatchBody {

    private final Map<String, Object> values = new LinkedHashMap<>();

    /** 언제나 싣는다. 필수라 비울 수 없는 칸이 이쪽이다. */
    public PatchBody always(String name, Object value) {
        values.put(name, value);
        return this;
    }

    /**
     * 비어 있으면 <b>비우라는 값으로</b> 싣는다. 이메일·폰·소개가 이쪽이다.
     *
     * <p>현재 값이 채워진 채로 뜨는 칸이라, 사용자가 지웠다면 지우려는 뜻이다.
     */
    public PatchBody clearIfBlank(String name, String value) {
        values.put(name, PhoneNumbers.isBlank(value) ? null : value);
        return this;
    }

    /**
     * 비어 있으면 <b>아예 싣지 않는다.</b> 새 비밀번호가 이쪽이다.
     *
     * <p>현재 값이 채워지지 않은 채 뜨는 칸이라, 비어 있는 것이 기본 상태다.
     */
    public PatchBody omitIfBlank(String name, String value) {
        if (!PhoneNumbers.isBlank(value)) {
            values.put(name, value);
        }
        return this;
    }

    /**
     * 폰을 싣는다. <b>숫자만 남기고</b>, 남은 것이 없으면 비우라는 값으로 싣는다.
     *
     * <p>숫자만 남기는 일과 빈 칸을 다루는 일이 한 칸에 겹쳐 있어 따로 둔다. 두 규칙을 화면이
     * 각자 이어 붙이면 어느 화면 하나가 순서를 바꿔 하이픈만 친 입력을 빈 값이 아니라 오류로
     * 보내게 된다.
     */
    public PatchBody phoneClearIfBlank(String name, String rawPhone) {
        values.put(name, PhoneNumbers.digitsOnly(rawPhone));
        return this;
    }

    /** 백엔드로 나갈 본문. 키가 없는 항목은 백엔드가 그대로 유지한다. */
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
