package com.dbdomino.moneylog.backend.config;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 인증 <b>이전</b>에 도는 요청의 감사자를 지정한다.
 *
 * <p>감사 컬럼은 {@code tbl_user} 를 뺀 전 테이블에서 NOT NULL 인데, 값의 정상 출처인
 * {@code SecurityContext} 가 비어 있는 경로가 실제로 있다.
 *
 * <ul>
 *   <li>로그인(1.3) — 세션·로그인 이력을 만든다. 인증을 <b>만드는</b> 요청이라 아직 주체가 없다
 *   <li>회원가입(1.2) — 기본 지출유형 10종을 만든다
 *   <li>토큰 갱신(1.5) — 세션 행을 고친다. Refresh 로만 도는 {@code permitAll} 경로다
 * </ul>
 *
 * <p>이 경로들은 "누가 만들었나"를 알고 있다 — 그 요청이 다루는 회원 본인이다. 그 값을
 * 여기에 실어 두면 {@link BackendAuditorAware} 가 꺼내 쓰고, <b>감사 값은 여전히
 * {@code AuditingEntityListener} 만 채운다</b>. 엔티티 세터로 직접 채우면 그 세터가 열려
 * 있는 한 누구든 {@code created_by} 를 위조할 수 있다.
 *
 * <p>{@code SecurityContext} 가 우선한다. 관리자가 남의 세션을 폐기한 기록이 회원 본인으로
 * 둔갑하면 안 되기 때문이다.
 *
 * <p><b>반드시 {@code runAs} 로만 쓴다.</b> 값을 직접 넣고 지우는 방식은 예외가 났을 때
 * 스레드에 값이 남고, 그 스레드가 처리하는 다음 요청의 감사 컬럼이 엉뚱한 회원으로 찍힌다.
 */
public final class SelfAuditorContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private SelfAuditorContext() {
    }

    /** 지정한 회원을 감사자로 두고 작업을 실행한다. */
    public static <T> T runAs(Long idKey, Supplier<T> work) {
        Long previous = CURRENT.get();
        CURRENT.set(idKey);
        try {
            return work.get();
        } finally {
            // 중첩 호출(로그인이 세션 폐기를 부르는 등)을 위해 이전 값으로 되돌린다.
            // 바깥이 없으면 지운다 — 남겨 두면 이 스레드의 다음 요청에 새어 나간다.
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** 반환값이 없는 작업용. */
    public static void runAs(Long idKey, Runnable work) {
        runAs(idKey, () -> {
            work.run();
            return null;
        });
    }

    /** 현재 스레드에 지정된 감사자. {@link BackendAuditorAware} 만 쓴다. */
    static Optional<Long> current() {
        return Optional.ofNullable(CURRENT.get());
    }
}
