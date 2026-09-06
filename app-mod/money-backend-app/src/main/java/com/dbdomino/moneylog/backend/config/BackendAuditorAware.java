package com.dbdomino.moneylog.backend.config;

import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 감사 컬럼({@code created_by}·{@code updated_by})에 넣을 회원 {@code id_key} 공급자.
 *
 * <p>{@code data-mod}에 있던 임시 구현(빈 {@code Optional} 반환)을 대체한다. 그 임시
 * 구현 때문에 {@code BaseAuditEntity}에 공개 세터가 열려 있었고 테스트가 감사 값을 손으로
 * 채우고 있었다 — 둘 다 이 구현이 서면 걷어낸다.
 *
 * <p><b>비로그인 경로는 빈 값이다.</b> 회원가입은 자기 자신을 만드는 행위라 INSERT 시점에
 * 자기 {@code id_key}가 없다. 그래서 {@code tbl_user}만 두 감사 컬럼이 nullable 이다.
 *
 * <p>감사 컬럼이 NOT NULL 인 테이블에 비로그인 경로가 쓰는 곳은 <b>가입이 만드는
 * 지출유형 10종 하나뿐</b>이고, 그 서비스는 방금 만든 회원의 {@code id_key}를 알고 있으므로
 * 값을 명시적으로 채운다.
 *
 * <p>실재하지 않는 시스템 계정 id(0 등)를 돌려주지 않는다. 감사 컬럼에는 FK 가 없어
 * (001 의 결정) DB 가 걸러 주지 않으므로, 없는 회원 번호가 그대로 기록에 남는다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/research.md">research.md §6</a>
 */
@Component
public class BackendAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthPrincipal principal
                ? Optional.ofNullable(principal.idKey())
                : Optional.empty();
    }
}
