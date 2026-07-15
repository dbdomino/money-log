package com.dbdomino.moneylog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dbdomino.moneylog.entity.Expend}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class ExpendDto implements Serializable {
    private final LocalDateTime dateCreate;
    private final LocalDateTime dateUpdate;
    private final Long expendId;
    private final String memberId;
    private final String expendName;
    private final Long expendCost;
    private final int expendDay;
    private final int expendMonth;
    private final int expendYear;
    private final String expendMemo;
    private final int stat;
    private final NestExpendGroupDto expendGroup;
    private final NestPaymentMethodDto paymentMethod;

    /**
     * DTO for {@link com.dbdomino.moneylog.entity.ExpendGroup}
     */
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class NestExpendGroupDto implements Serializable {
        private final LocalDateTime dateCreate;
        private final LocalDateTime dateUpdate;
        private final Long expendGroupId;
        @NotBlank
        private final String memberId;
        @NotBlank
        private final String expendGroupName;
        private final String expendGroupMemo;
        @NotNull
        private final Long limitCost;
        @NotBlank
        private final String sequence;
        @NotBlank
        private final String stat;
    }

    /**
     * DTO for {@link com.dbdomino.moneylog.entity.PaymentMethod}
     */
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class NestPaymentMethodDto implements Serializable {
        private final LocalDateTime dateCreate;
        private final LocalDateTime dateUpdate;
        private final Long paymentMethodId;
        @NotEmpty
        private final String memberId;
        @NotEmpty
        private final String paymentMethodName;
        @NotEmpty
        private final String sequence;
    }
}