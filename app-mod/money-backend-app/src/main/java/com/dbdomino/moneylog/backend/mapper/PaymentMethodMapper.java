package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.PaymentMethodResponse;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code UserPaymentMethod} Entity → 응답 DTO 변환.
 *
 * <p>변환만 한다. 값 검증({@code 3001}·{@code 3002})과 참조 검사({@code 3005})는 전부
 * 서비스에 둔다(헌장 원칙 II).
 *
 * <p>Entity 의 {@code idx} 가 API 의 {@code paymentMethodId} 다. 저장 구조가 모든 대리키를
 * {@code idx} 로 부르기 때문에 이름이 갈리는데, 그 규칙은 API 밖으로 나가지 않는다.
 *
 * <p><b>소유자({@code user})를 매핑하지 않는다.</b> 응답 타입에 그 필드가 없어 매핑할 대상도
 * 없다 — {@code User} 를 실으면 회원의 비밀번호 해시까지 딸려 나간다.
 */
@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    @Mapping(target = "paymentMethodId", source = "idx")
    PaymentMethodResponse toResponse(UserPaymentMethod entity);

    /** 관리 목록(2.2)·사용 중 목록(2.6)이 함께 쓴다. 순서는 넘겨받은 그대로 유지한다. */
    List<PaymentMethodResponse> toResponses(List<UserPaymentMethod> entities);
}
