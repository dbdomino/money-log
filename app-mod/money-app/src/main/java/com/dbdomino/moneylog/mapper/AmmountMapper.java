package com.dbdomino.moneylog.mapper;

import com.dbdomino.moneylog.dto.AmmountDto;
import com.dbdomino.moneylog.dto.response.AmmountResponseDto;
import com.dbdomino.moneylog.dto.service.AmmountServiceDto;
import com.dbdomino.moneylog.entity.Ammount;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * Ammount DTO Mapper
 */
@Mapper(componentModel = "spring")
public interface AmmountMapper {
//    AmmountMapper INSTANCE = Mappers.getMapper(AmmountMapper.class);

    // 매핑 메서드 예시
    AmmountServiceDto toServiceDto(Ammount dto);
    AmmountResponseDto toResponseDto(AmmountServiceDto dto);
    AmmountDto toDto(Ammount entity);
    Ammount toEntity(AmmountServiceDto dto);
    List<AmmountServiceDto> toServiceDtoList(List<Ammount> ammountList);
    List<AmmountResponseDto> toResponseDtoList(List<AmmountServiceDto> serviceDtoList);
    // dto와 dto간의 매핑도 가능,

    // DTO에 값 중에 null이 아닌 값만 Entity로 Mapping시킴
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Ammount partialUpdate(AmmountDto dto, @MappingTarget Ammount entity);
}
