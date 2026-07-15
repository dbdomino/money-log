package com.dbdomino.moneylog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dbdomino.moneylog.entity.SystemStat}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class SystemStatDto implements Serializable {
    private final LocalDateTime dateCreate;
    private final LocalDateTime dateUpdate;
    private final Long id;
    @NotBlank
    private final String memberId;
    @NotBlank
    private final String systemPayment;
    private final String systemSequence;
}