package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.MemberDto;
import com.dbdomino.moneylog.entity.Member;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface MemberMapper {
    Member toEntity(MemberDto memberDto);

    MemberDto toDto(Member member);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Member partialUpdate(MemberDto memberDto, @MappingTarget Member member);
}