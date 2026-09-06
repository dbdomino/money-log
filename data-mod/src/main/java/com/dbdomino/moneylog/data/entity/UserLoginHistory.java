package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 이력 — {@code tbl_user_login_history}.
 *
 * <p>로그인 시도마다 한 줄씩 쌓인다. 성공과 실패를 모두 남기며 {@code success}로 구분한다.
 * 세션과 달리 갱신되지 않는 기록이다.
 *
 * <p>{@code id_key}가 NOT NULL이라 <b>회원이 특정되는 실패만</b> 행이 된다(비밀번호 불일치·비활성
 * 계정). 존재하지 않는 아이디로 온 시도는 채울 회원이 없어 행을 만들지 않는다.
 *
 * <p><b>무기한 보존한다</b>(FR-019a). 보존 기간·건수 상한을 두지 않고 정리 배치도
 * 두지 않는다. 사용자 수가 많지 않아 누적량이 문제되지 않는다는 판단이다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §3</a>
 */
@Entity
@Table(
        name = "tbl_user_login_history",
        comment = "로그인 이력. 성공·실패를 함께 쌓고 success로 구분한다. 정리 배치 없이 무기한 누적되므로 조회는 반드시 페이징으로 끊는다",
        indexes = @Index(name = "ix_user_login_history_at", columnList = "id_key, login_at")
)
@Getter
@Setter
@NoArgsConstructor
public class UserLoginHistory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 로그인한 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_login_history_user")
    )
    private User user;

    /** 로그인 시도 시각. */
    @Column(name = "login_at", nullable = false)
    private OffsetDateTime loginAt;

    /** 접속 IP. IPv6 표기(최대 45자)까지 수용한다. 확보하지 못하면 비어 있을 수 있다. */
    @Column(name = "login_ip", length = 45)
    private String loginIp;

    /**
     * 로그인 성공 여부.
     *
     * <p>성공과 실패가 한 테이블에 섞이므로 "마지막 로그인"을 뽑을 때는 이 값이 {@code true}인
     * 행만 골라야 한다. 원시 타입이라 NOT NULL이 자연스럽게 강제된다.
     */
    @Column(name = "success", nullable = false)
    private boolean success;
}
