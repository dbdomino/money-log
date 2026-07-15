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
@Table(name = "tbl_expend_fix")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class ExpendFix extends CuDate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("고정지출 Id")
    private Long expendFixId;

    @NotBlank
    @Comment("회원 Id")
    private String memberId;

    @NotBlank
    @Comment("고정지출 이름")
    private String expendFixName;

    @Comment("출금일")
    @Builder.Default
    private int outDay = 1;

    @Comment("고정지출 설명")
    private String expendFixDetail;

    @Comment("고정지출 타입  0: 하얀색, 1: 주황색, 2: 옅은 회색, 그외 검은색 배경으로")
    private String expendFixType;

    @Comment("고정지출 상태  0: 비활성, 1: 활성")
    @Builder.Default
    private int expendFixStat=1;

    protected ExpendFix() {}

}
