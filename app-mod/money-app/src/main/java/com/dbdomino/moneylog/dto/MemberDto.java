package com.dbdomino.moneylog.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dbdomino.moneylog.entity.Member}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class MemberDto implements Serializable {
    private final LocalDateTime dateCreate;
    private final LocalDateTime dateUpdate;
    private final String id;
    @NotEmpty
    private final String pw;
    private final String name;
    private final String email;
    private final String phone;
    private final String address;
    private final String type;
    private final String isDel;
}