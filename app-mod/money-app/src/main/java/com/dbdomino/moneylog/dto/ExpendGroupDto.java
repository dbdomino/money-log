package com.dbdomino.moneylog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dbdomino.moneylog.entity.ExpendGroup}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class ExpendGroupDto implements Serializable {
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