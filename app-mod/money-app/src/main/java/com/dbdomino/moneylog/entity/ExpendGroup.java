package com.dbdomino.moneylog.entity;

import com.dbdomino.moneylog.entity.superclass.CuDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 */
@NamedQuery( // name ="StaticQuery"   이런식으로 그냥 써도 되지만, 엔티티명.쿼리명 으로 쓰는게 관례 라고 함.
        name = "ExpendClassification.findByUsername",
        query = "select m from ExpendGroup m where m.memberId = :memberId" // 쿼리 이상할 경우컴파일에러로 스프링 실행안됨. (이게 장점이라고 함)
)
@Entity
@Table(name = "tbl_expend_group")
@Builder(toBuilder = true)
@AllArgsConstructor
@Getter @Setter
public class ExpendGroup extends CuDate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("지출 그룹 Id")
    private Long expendGroupId;

    @NotBlank
    @Comment("회원 Id")
    private String memberId;

    @NotBlank
    @Comment("지출 그룹 이름")
    private String expendGroupName;

    @Comment("지출 그룹 설명")
    private String expendGroupMemo;

    @NotNull
    @Comment("월간 한도 금액")
    private Long limitCost;

    @NotBlank
    @Comment("출력 순서")
    @Builder.Default
    private String sequence = "0";

    @NotBlank
    @Comment("지출 그룹 상태 0: 활성, 1: 비활성")
    private String stat;

    protected ExpendGroup() {}
}
