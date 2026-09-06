package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.ExpenseResponse;
import com.dbdomino.moneylog.data.entity.UserExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code UserExpense} Entity → 응답 DTO 변환.
 *
 * <p>변환만 한다. 값 검증({@code 3201})·참조 검증({@code 3003}·{@code 3103})·스냅샷 갱신
 * 판정은 전부 서비스와 {@code ReferenceResolver} 에 둔다(헌장 원칙 II).
 *
 * <p>이름이 갈리는 자리가 셋이다 — Entity 의 {@code idx} 가 API 의 {@code expenseId} 이고,
 * 연관 {@code paymentMethod}·{@code expendGroup} 의 {@code idx} 가 각각
 * {@code paymentMethodId}·{@code expendGroupId} 다. 저장 구조가 모든 대리키를 {@code idx} 로
 * 부르기 때문인데, 그 규칙은 API 밖으로 나가지 않는다.
 *
 * <p><b>이름 스냅샷은 연관이 아니라 Entity 자신의 컬럼에서 온다.</b>
 * {@code paymentMethodName} 은 {@code paymentMethod.name} 이 아니라 {@code tbl_expense} 의
 * {@code payment_method_name} 이다 — 연관에서 읽으면 원본 이름이 바뀔 때마다 과거 지출이
 * 따라가 버려 FR-302 가 깨진다. 필드 이름이 같아 MapStruct 가 자동으로 맞춰 주지만,
 * <b>여기서 {@code source = "paymentMethod.name"} 을 쓰면 안 된다</b>.
 *
 * <p><b>소유자({@code user})를 매핑하지 않는다.</b> 대상 타입에 그 필드가 없어 매핑할
 * 자리도 없다 — {@code User} 를 실으면 회원의 비밀번호 해시까지 딸려 나간다.
 */
@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "expenseId", source = "idx")
    @Mapping(target = "paymentMethodId", source = "paymentMethod.idx")
    @Mapping(target = "expendGroupId", source = "expendGroup.idx")
    ExpenseResponse toResponse(UserExpense entity);
}
