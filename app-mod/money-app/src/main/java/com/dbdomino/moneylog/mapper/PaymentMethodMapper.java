package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.PaymentMethodDto;
import com.dbdomino.moneylog.entity.PaymentMethod;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMethodMapper {
    PaymentMethod toEntity(PaymentMethodDto paymentMethodDto);

    PaymentMethodDto toDto(PaymentMethod paymentMethod);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PaymentMethod partialUpdate(PaymentMethodDto paymentMethodDto, @MappingTarget PaymentMethod paymentMethod);
}