package com.dbdomino.moneylog.entity;

import com.dbdomino.moneylog.entity.superclass.CuDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
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
//@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
//@NoArgsConstructor //Builder와 충돌
@Table(name = "tbl_expend")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class Expend extends CuDate {
    @Id    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("지출 ID")
    private Long expendId;

    @Comment("회원Id")
    @Column(nullable = false)
    private String memberId;

    @Comment("지출 이름")
    private String expendName;

    @Comment("지출 금액")
    private Long expendCost;

    @NotEmpty
    @Comment("지출 날짜")
    private int expendDay;

    @NotEmpty
    @Comment("지출 월")
    private int expendMonth;

    @NotEmpty
    @Comment("지출 연")
    private int expendYear;

    @Comment("지출 설명")
    private String expendMemo;

    @Comment("상태, 0: 활성, 1: 결제취소, 2: 삭제")
    private int stat;

    @JoinColumn(name = "expendGroupId") // 외래 키 컬럼명 설정
    @OneToOne(fetch = FetchType.LAZY)
    @Comment("지출 그룹 Id")
    private ExpendGroup expendGroup;

    @JoinColumn(name = "paymentMethodId")
    @OneToOne(fetch = FetchType.LAZY)
    @Comment("결제 방법 ID")
    private PaymentMethod paymentMethod;

    protected Expend() {}
}
