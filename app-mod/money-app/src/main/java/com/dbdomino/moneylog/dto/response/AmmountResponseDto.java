package com.dbdomino.moneylog.dto.response;

import lombok.Getter;
import lombok.Setter;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * Ammount Response DTO
 */
@Getter @Setter
public class AmmountResponseDto {
    Long id;
    String memberId;
    String amountName;
    int amount;
    String ammountType;
}
