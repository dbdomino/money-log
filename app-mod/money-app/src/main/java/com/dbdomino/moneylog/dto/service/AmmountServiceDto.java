package com.dbdomino.moneylog.dto.service;

import lombok.Getter;
import lombok.Setter;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * Ammount Service DTO
 */
@Getter @Setter
public class AmmountServiceDto {
    Long id;
    String memberId;
    String amountName;
    int amount;
    String ammountType;
}