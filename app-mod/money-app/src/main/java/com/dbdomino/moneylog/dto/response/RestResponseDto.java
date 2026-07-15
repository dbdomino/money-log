package com.dbdomino.moneylog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.dbdomino.moneylog.common.constants.CodeCommon;
import com.dbdomino.moneylog.common.exception.ServiceException;
import lombok.*;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * Rest 방식으로 응답을 위한 DTO 제공, 팩토리 메서드 패턴으로 DTO 생성 가능
 */
@Getter
@Setter
@ToString
@Builder
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestResponseDto<T> {
    private Integer code;
    private String message;

    private T data;

    public static <T> RestResponseDtoBuilder<T> of(CodeCommon status) {
        return RestResponseDto.<T>builder()
                .code(status.getCode())
                .message(status.getMessage());
    }
    /*   팩토리 메서드 패턴 사용예제
    ServiceResponse status = new ServiceResponse(200, "Success");
    RestResponseDto<String> response = RestResponseDto.of(status)
    .data("Hello, World!")
    .build();

    결과
    {
      "code": 200,
      "message": "Success",
      "data": "Hello, World!"
    }

     */
    public static <T> RestResponseDtoBuilder<T> ofError(ServiceException status) {
        return RestResponseDto.<T>builder()
                .code(status.getCode())
                .message(status.getMessage());
    }
    /*  Rest 서비스에서 예외 생성 예제1
    throw new ServiceException(400, "Invalid Input");

     Rest 서비스에서 예외 생성 예제2
    try {
    // Some code that might throw an exception
        throw new ServiceException(400, "Invalid Input");
    } catch (ServiceException e) {
        RestResponseDto<String> errorResponse = RestResponseDto.ofError(e).build();
        System.out.println(errorResponse);
    }

    {
      "code": 400,
      "message": "Invalid Input"
    }


     */


    public boolean eq(CodeCommon serviceResponse) {
        return this.code.equals(serviceResponse.getCode());
    }
}
