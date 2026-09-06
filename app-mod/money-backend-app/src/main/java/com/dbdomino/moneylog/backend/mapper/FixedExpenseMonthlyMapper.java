package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.FixedExpenseMonthlyResponse;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 월별 고정지출 내역 Entity → DTO.
 *
 * <h2>이름 셋이 전부 연관에서 온다</h2>
 *
 * <p>{@link FixedExpenseMapper} 와 같은 규칙이며 <b>004 의 매퍼들과는 정반대</b>다.
 * {@code tbl_fixed_expense_monthly} 에는 이름 컬럼이 하나도 없어 읽을 자리가 없다.
 *
 * <table border="1">
 *   <caption>어디서 읽는가</caption>
 *   <tr><th>필드</th><th>출처</th></tr>
 *   <tr><td>{@code fixedExpenseName}</td><td>{@code fixedExpense.name} — 관리 테이블의 현재 값</td></tr>
 *   <tr><td>{@code paymentMethodName}</td><td>{@code paymentMethod.name}</td></tr>
 *   <tr><td>{@code expendGroupName}</td><td>{@code expendGroup.name}</td></tr>
 * </table>
 *
 * <p><b>{@code fixedExpenseId} 는 부모의 {@code idx} 다.</b> 이 행 자신의 {@code idx} 가
 * 아니다 — 프론트가 4.6 을 부르려면 {@code /monthly/{year}/{month}/{fixedExpenseId}} 가
 * 필요한데 그 Path 에 들어가는 것이 부모 PK 이기 때문이다. 자기 {@code idx} 를 실으면
 * 1행을 특정할 수는 있어도 그 값으로 수정을 부를 수 없다.
 *
 * <p><b>연관이 LAZY 다.</b> 목록에서 N+1 이 실제로 발생한다 — 행마다 세 번씩이다.
 * 한 회원의 한 달 고정지출이 많아야 수십 건이라 현재는 감수한다. 문제가 되면 fetch join 을
 * <b>Repository 쪽에</b> 넣는다 — 매퍼는 변환만 하고 조회 전략을 갖지 않는다(원칙 II).
 */
@Mapper(componentModel = "spring")
public interface FixedExpenseMonthlyMapper {

    @Mapping(target = "fixedExpenseId", source = "fixedExpense.idx")
    @Mapping(target = "fixedExpenseName", source = "fixedExpense.name")
    @Mapping(target = "paymentMethodId", source = "paymentMethod.idx")
    @Mapping(target = "paymentMethodName", source = "paymentMethod.name")
    @Mapping(target = "expendGroupId", source = "expendGroup.idx")
    @Mapping(target = "expendGroupName", source = "expendGroup.name")
    FixedExpenseMonthlyResponse toResponse(UserFixedExpenseMonthly entity);
}
