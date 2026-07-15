package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.ExpendGroupDto;
import com.dbdomino.moneylog.entity.ExpendGroup;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpendGroupMapper {
    ExpendGroup toEntity(ExpendGroupDto expendGroupDto);

    ExpendGroupDto toDto(ExpendGroup expendGroup);

    // DTO에 값 중에 null이 아닌 값만 Entity로 Mapping시킴
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ExpendGroup partialUpdate(ExpendGroupDto expendGroupDto, @MappingTarget ExpendGroup expendGroup);
}