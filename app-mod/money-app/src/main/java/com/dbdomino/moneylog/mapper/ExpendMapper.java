package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.ExpendDto;
import com.dbdomino.moneylog.entity.Expend;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {ExpendGroupMapper.class, PaymentMethodMapper.class})
public interface ExpendMapper {
    Expend toEntity(ExpendDto expendDto);

    ExpendDto toDto(Expend expend);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Expend partialUpdate(ExpendDto expendDto, @MappingTarget Expend expend);
}