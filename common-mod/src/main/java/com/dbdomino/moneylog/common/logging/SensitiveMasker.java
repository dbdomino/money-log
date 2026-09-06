package com.dbdomino.moneylog.common.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 로그에 남기면 안 되는 값을 가린다(헌장 원칙 IV · 002 FR-128).
 *
 * <p>가리는 방식은 <b>값 전체를 {@code ***}로 대체</b>하는 것이다. 앞뒤 일부를 남기면
 * 토큰 길이나 비밀번호 첫 글자가 로그에 남는다. 아이디 찾기 응답의 마스킹
 * ({@code use***01})과는 목적이 다르므로 규칙을 공유하지 않는다 — 그쪽은 본인이
 * 알아볼 수 있어야 하고, 이쪽은 아무도 알아볼 수 없어야 한다.
 *
 * <p>대상은 <b>이름</b>으로 정한다. 값의 생김새로 토큰을 알아내려 하면 새 필드가
 * 늘 때마다 규칙을 고쳐야 한다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/api-contract.md">api-contract.md §8</a>
 */
public final class SensitiveMasker {

    /** 가려진 값의 표기. */
    public static final String MASK = "***";

    /**
     * 이름에 이 조각이 들어 있으면 가린다. <b>완전 일치가 아니라 부분 일치다.</b>
     *
     * <p>완전 일치로 두면 목록에 없는 변형이 그대로 새어 나간다 — 실제로
     * {@code newPasswordConfirm}(1.11 의 필드)이 목록에 없어 평문으로 로그에 찍혔다.
     * 필드가 늘 때마다 목록을 고치는 방식은 빠뜨리는 쪽이 기본값이 되므로, "비밀번호·토큰·
     * 비밀"이라는 <b>종류</b>로 막는다.
     */
    private static final Set<String> SENSITIVE_PARTS = Set.of(
            "password", "token", "secret", "credential", "authorization");

    /**
     * 부분 일치의 예외. 값이 비밀이 아니고 로그에서 쓸모가 있는 이름이다.
     *
     * <p>{@code tokenType}은 항상 {@code Bearer} 라 가릴 것이 없다. 이런 예외는
     * <b>가리지 않을 이유가 분명할 때만</b> 늘린다 — 예외가 늘수록 규칙이 무력해진다.
     */
    private static final Set<String> NOT_SENSITIVE = Set.of("tokentype");

    /**
     * 완전 일치로 가리는 이름. 위 조각으로는 걸리지 않는 짧은 이름들이다.
     *
     * <p>{@code pw}는 Entity 필드명이다. DTO 경계를 넘지 않으므로 로그에 나올 일이
     * 없지만, 나중에 누가 Entity 를 그대로 찍어도 걸리도록 둔다.
     */
    private static final Set<String> SENSITIVE_NAMES = Set.of("pw");

    /** 재귀 깊이 상한. 순환 참조와 거대한 객체 그래프를 로그가 따라가지 않게 한다. */
    private static final int MAX_DEPTH = 3;

    private SensitiveMasker() {
    }

    /** 이 이름이 가려야 할 것인가. */
    public static boolean isSensitive(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (NOT_SENSITIVE.contains(normalized)) {
            return false;
        }
        if (SENSITIVE_NAMES.contains(normalized)) {
            return true;
        }
        return SENSITIVE_PARTS.stream().anyMatch(normalized::contains);
    }

    /** 이름이 가릴 대상이면 {@code ***}, 아니면 값을 그대로 돌려준다. */
    public static String maskValue(String name, Object value) {
        return isSensitive(name) ? MASK : String.valueOf(value);
    }

    /**
     * 객체를 로그용 문자열로 만든다. 가릴 이름의 필드는 값 대신 {@code ***}가 들어간다.
     *
     * <p>{@code toString()}을 쓰지 않는 이유는 Lombok {@code @Data}가 만든
     * {@code toString()}이 비밀번호 필드를 그대로 찍기 때문이다. 리플렉션으로 필드를
     * 직접 훑어야 이름 기준 마스킹이 성립한다.
     */
    public static String describe(Object value) {
        return describe(value, 0);
    }

    private static String describe(Object value, int depth) {
        if (value == null) {
            return "null";
        }
        if (isScalar(value)) {
            return String.valueOf(value);
        }
        if (depth >= MAX_DEPTH) {
            return value.getClass().getSimpleName() + "{...}";
        }
        if (value instanceof Collection<?> collection) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            collection.stream().limit(10).forEach(e -> joiner.add(describe(e, depth + 1)));
            return collection.size() > 10 ? joiner + "(+" + (collection.size() - 10) + ")" : joiner.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(", ", "{", "}");
            map.forEach((k, v) -> {
                String name = String.valueOf(k);
                joiner.add(name + "=" + (isSensitive(name) ? MASK : describe(v, depth + 1)));
            });
            return joiner.toString();
        }
        return describeFields(value, depth);
    }

    private static String describeFields(Object value, int depth) {
        Class<?> type = value.getClass();
        StringJoiner joiner = new StringJoiner(", ", type.getSimpleName() + "{", "}");
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = field.getName();
                if (isSensitive(name)) {
                    joiner.add(name + "=" + MASK);
                    continue;
                }
                joiner.add(name + "=" + readField(field, value, depth));
            }
        }
        return joiner.toString();
    }

    private static String readField(Field field, Object owner, int depth) {
        try {
            field.setAccessible(true);
            return describe(field.get(owner), depth + 1);
        } catch (RuntimeException | ReflectiveOperationException e) {
            // 로깅이 요청을 깨뜨리면 안 된다. 읽지 못한 필드는 표시만 남기고 넘어간다.
            return "?";
        }
    }

    private static boolean isScalar(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof java.time.temporal.Temporal
                || value instanceof java.util.UUID;
    }
}
