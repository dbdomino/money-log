package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.IncomeResponse;
import com.dbdomino.moneylog.data.entity.UserIncome;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code UserIncome} Entity → 응답 DTO 변환.
 *
 * <p><b>{@code ExpenseMapper} 와 공통 상위를 두지 않는다.</b> 필드 구성이 달라 —
 * 소득에는 장소·지출유형·할부가 <b>없다</b>(FR-306) — 상위 인터페이스를 만들면 없는
 * 필드를 매핑하려다 막힌다. 두 매퍼가 닮아 보이는 것은 우연이 아니라 두 자원이 같은
 * 규칙(참조 + 이름 스냅샷)을 따르기 때문이지, 같은 구조라는 뜻이 아니다.
 *
 * <p><b>이름 스냅샷은 연관이 아니라 Entity 자신의 컬럼에서 온다.</b>
 * {@code paymentMethodName} 을 {@code paymentMethod.name} 에서 읽으면 원본 이름이 바뀔
 * 때마다 과거 소득이 따라가 FR-303 이 깨진다. 필드 이름이 같아 MapStruct 가 자동으로
 * 맞춰 주지만, 여기서 {@code source = "paymentMethod.name"} 을 쓰면 안 된다.
 */
@Mapper(componentModel = "spring")
public interface IncomeMapper {

    @Mapping(target = "incomeId", source = "idx")
    @Mapping(target = "paymentMethodId", source = "paymentMethod.idx")
    IncomeResponse toResponse(UserIncome entity);
}
