package com.dbdomino.moneylog.common.exception;

import com.dbdomino.moneylog.common.constants.CodeCommon;
import lombok.ToString;

/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 *
 * Service에서 사용할 Exception
 */

@ToString
public class ServiceException extends RuntimeException {

    private final Integer code;
    private final String message;

    public ServiceException(CodeCommon response) {
        super(String.format("%s", response.getMessage()));
        this.code = response.getCode();
        this.message = response.getMessage();
    }

    public ServiceException(Integer code, String message) {
        super(String.format("%s", message));
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.message;
    }
}
