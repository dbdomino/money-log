package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserLoginHistory;
import com.dbdomino.moneylog.data.repository.UserLoginHistoryRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시도 이력. 성공·실패를 <b>모두</b> 남긴다(FR-125).
 *
 * <h2>회원이 특정될 때만 행을 만든다</h2>
 *
 * <p>존재하지 않는 아이디로 온 시도는 행을 만들지 않는다(FR-127). {@code id_key}·
 * {@code created_by}·{@code updated_by} 가 전부 NOT NULL 인데 채울 값이 없기 때문이다 —
 * 감사 컬럼을 nullable 로 두는 것은 {@code tbl_user} 에만 허용된 예외이고, 이력 테이블에
 * 예외를 하나 더 만들지 않는다. 그 시도는 애플리케이션 로그로만 남기고
 * <b>아이디는 마스킹</b>한다(FR-128).
 *
 * <p>비밀번호와 토큰은 이력에도 로그에도 남기지 않는다(SC-111).
 */
@Service
public class LoginHistoryService {

    private static final Logger log = LoggerFactory.getLogger(LoginHistoryService.class);

    private final UserLoginHistoryRepository historyRepository;

    public LoginHistoryService(UserLoginHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * 이력 1건을 남긴다.
     *
     * <p>감사 컬럼을 직접 채운다. 로그인은 인증 <b>이전</b>이라 {@code SecurityContext} 가
     * 비어 있어 {@code AuditorAware} 가 값을 주지 못하는데, 이 테이블의 감사 컬럼은
     * NOT NULL 이다. 넣을 값은 시도한 그 회원의 {@code id_key} 다.
     *
     * @param user    시도한 회원. 아이디가 실재할 때만 넘어온다
     * @param loginIp 요청 IP. 확보하지 못하면 {@code null}
     * @param success 성공 여부
     */
    @Transactional
    public void record(User user, String loginIp, boolean success) {
        UserLoginHistory history = new UserLoginHistory();
        history.setUser(user);
        history.setLoginAt(OffsetDateTime.now());
        history.setLoginIp(loginIp);
        history.setSuccess(success);
        history.setCreatedBy(user.getIdKey());
        history.setUpdatedBy(user.getIdKey());
        historyRepository.save(history);
    }

    /**
     * 회원을 특정할 수 없는 시도. <b>행을 만들지 않고</b> 로그만 남긴다.
     *
     * @param attemptedMemberId 시도한 아이디. 마스킹해서 남긴다
     * @param loginIp           요청 IP
     */
    public void recordUnknownMember(String attemptedMemberId, String loginIp) {
        log.warn("login attempt for unknown member id={} ip={}",
                maskMemberId(attemptedMemberId), loginIp);
    }

    /**
     * 로그에 남길 아이디를 가린다.
     *
     * <p>아이디 찾기(1.9)의 마스킹과 목적이 다르다 — 그쪽은 본인이 자기 아이디를 알아볼
     * 수 있어야 하고, 이쪽은 운영자가 같은 아이디의 반복 시도를 셀 수 있을 만큼만 남기면
     * 된다. 그래서 규칙을 공유하지 않는다.
     */
    private static String maskMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return "***";
        }
        return memberId.length() <= 2
                ? "***"
                : memberId.charAt(0) + "***" + memberId.charAt(memberId.length() - 1);
    }
}
