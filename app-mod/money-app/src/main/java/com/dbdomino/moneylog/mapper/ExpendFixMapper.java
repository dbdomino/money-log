package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.ExpendFixDto;
import com.dbdomino.moneylog.entity.ExpendFix;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpendFixMapper {
    ExpendFix toEntity(ExpendFixDto expendFixDto);

    ExpendFixDto toDto(ExpendFix expendFix);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ExpendFix partialUpdate(ExpendFixDto expendFixDto, @MappingTarget ExpendFix expendFix);
}