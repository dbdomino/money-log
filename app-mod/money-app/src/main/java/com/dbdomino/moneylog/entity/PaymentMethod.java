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
@Table(name = "tbl_payment_Method")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class PaymentMethod extends CuDate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("결제 방법 ID")
    private Long paymentMethodId;

    @NotEmpty
    @Comment("회원 ID")
    private String memberId;

    @NotEmpty
    @Comment("결제 방법 이름")
    private String paymentMethodName;

    @NotEmpty
    @Comment("정렬 순서")
    @Builder.Default
    private String sequence= "0";

    protected PaymentMethod() {}
}
