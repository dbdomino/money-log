package com.dbdomino.moneylog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
*/
@Entity
@Table(name = "tbl_login_history")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class LoginHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("로그인 이력 기본키")
    private Long loginHistoryId;

    @Comment("로그인 Id")
    private String id;
    @Comment("로그인 시간")
    private LocalDateTime loginDatetime;
    @Comment("로그인 주소")
    private String loginIp;

    protected LoginHistory() {}

}
