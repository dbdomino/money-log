package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.LoginHistoryDto;
import com.dbdomino.moneylog.entity.LoginHistory;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface LoginHistoryMapper {
    LoginHistory toEntity(LoginHistoryDto loginHistoryDto);

    LoginHistoryDto toDto(LoginHistory loginHistory);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    LoginHistory partialUpdate(LoginHistoryDto loginHistoryDto, @MappingTarget LoginHistory loginHistory);
}