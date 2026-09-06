package com.dbdomino.moneylog.data;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

/**
 * 테스트에서 감사 컬럼({@code created_by}·{@code updated_by})을 공급하는 감사자.
 *
 * <p>운영에서는 {@code money-backend-app}의 {@code BackendAuditorAware}가
 * {@code SecurityContext}의 {@code id_key}를 돌려준다. {@code data-mod}에는 웹 계층이
 * 없어 그 경로가 없으므로 테스트 컨텍스트가 이 구현으로 대신한다.
 *
 * <p><b>값을 바꿀 수 있어야 한다.</b> 운영에는 감사자가 없는 경로가 실제로 존재하기
 * 때문이다 — 회원가입은 로그인 없이 도는 요청이라 {@code SecurityContext}가 비어 있고,
 * 그래서 {@code tbl_user}만 두 감사 컬럼이 nullable 이다. 고정값만 돌려주면 그 상황을
 * 재현할 수 없어 "가입한 회원의 {@code created_by}가 NULL 인가"를 검증할 방법이 사라진다.
 *
 * <p>테스트끼리 상태가 새지 않도록 {@code AbstractSchemaIT}가 각 테스트 뒤에
 * {@link #reset()}을 부른다.
 */
public class TestAuditorAware implements AuditorAware<Long> {

    /**
     * 기본 감사자 {@code id_key}. 실재하지 않아도 된다 — 감사 컬럼에는 FK 가 없고
     * (001 의 결정), 이 값의 목적은 NOT NULL 컬럼을 채우는 것뿐이다. 소유자
     * {@code id_key}와 다른 값이라, 둘을 혼동한 코드가 있으면 테스트에서 드러난다.
     */
    public static final Long DEFAULT_AUDITOR_ID_KEY = -1L;

    private Long currentAuditor = DEFAULT_AUDITOR_ID_KEY;

    @Override
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(currentAuditor);
    }

    /** 감사자를 지운다. 로그인하지 않은 요청(회원가입 등)을 재현할 때 쓴다. */
    public void clear() {
        this.currentAuditor = null;
    }

    /** 감사자를 지정한 값으로 바꾼다. */
    public void set(Long idKey) {
        this.currentAuditor = idKey;
    }

    /** 기본값으로 되돌린다. */
    public void reset() {
        this.currentAuditor = DEFAULT_AUDITOR_ID_KEY;
    }
}
