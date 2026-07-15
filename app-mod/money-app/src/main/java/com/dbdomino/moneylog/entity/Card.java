package com.dbdomino.moneylog.entity;

import com.dbdomino.moneylog.entity.superclass.CuDate;
import jakarta.persistence.*;
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
@Table(name = "tbl_card")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class Card extends CuDate {

    @Id @GeneratedValue
    @Comment("카드 Id")
    private int id;

    @Comment("회원Id")
    @Column(nullable = false)
    private String memberId;

    @Comment("카드 담당은행")
    private String cardBank;

    @Comment("카드 이름")
    private String cardName;

    @Comment("카드 종류")
    private String cardType;

    @Comment("카드 설명")
    private String cardMemo;

    protected Card() {}

}
