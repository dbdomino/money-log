package com.dbdomino.moneylog.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.dbdomino.moneylog.entity.Card}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class CardDto implements Serializable {
    private final LocalDateTime dateCreate;
    private final LocalDateTime dateUpdate;
    private final int id;
    private final String memberId;
    private final String cardBank;
    private final String cardName;
    private final String cardType;
    private final String cardMemo;
}