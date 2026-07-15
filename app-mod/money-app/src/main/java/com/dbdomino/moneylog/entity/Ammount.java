package com.dbdomino.moneylog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * 재산 정보
 */
@Entity
@Table(name = "tbl_Ammount")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class Ammount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("재산 Id")
    private Long id;

    @Comment("회원Id")
    @Column(nullable = false)
    private String memberId;

    @Comment("재산 이름")
    private String amountName;

    @Comment("금액")
    private int amount;

    @Comment("재산 종류 0: 은행 잔고  1: 예금,  2: 적금   3: 기타")
    private String ammountType;

    protected Ammount() {}
}
