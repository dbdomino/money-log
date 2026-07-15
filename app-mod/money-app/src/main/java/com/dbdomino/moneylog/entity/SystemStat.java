package com.dbdomino.moneylog.entity;

import com.dbdomino.moneylog.entity.superclass.CuDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 */
@Entity
@Table(name = "tbl_system_stat")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class SystemStat extends CuDate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("사용자별 시스템 설정 Id")
    private Long id;

    @NotBlank
    @Comment("회원 Id")
    private String memberId;

    @NotBlank
    @Comment("기본 결제수단")
    private String systemPayment;

    @Comment("기본 가계부 정렬")
    private String systemSequence;

    protected SystemStat() {}

}
