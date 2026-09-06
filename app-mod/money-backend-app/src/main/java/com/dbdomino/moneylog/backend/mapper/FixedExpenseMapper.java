package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.FixedExpenseResponse;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 고정지출 설정 Entity ↔ DTO.
 *
 * <h2>004 의 {@code ExpenseMapper} 와 정반대로 매핑한다 — 함정이다</h2>
 *
 * <p>두 매퍼를 나란히 보면 같은 이름의 필드를 <b>다른 곳에서</b> 읽는다.
 *
 * <pre>{@code
 * // 004 ExpenseMapper — Entity 자신의 이름 컬럼에서 읽는다 (스냅샷)
 * //   expense.paymentMethodName  →  response.paymentMethodName
 *
 * // 005 여기 — 연관을 타고 원본에서 읽는다 (현재 이름)
 * //   fixedExpense.paymentMethod.name  →  response.paymentMethodName
 * }</pre>
 *
 * <p><b>{@code tbl_fixed_expense} 에는 이름 컬럼이 아예 없다.</b> 001 이 그 결정을 스키마에
 * 못박았고 테이블 주석도 "이름 스냅샷을 두지 않는다"다. 그래서 여기서 읽을 컬럼이 없어
 * 연관을 타는 것 외의 선택지가 없다.
 *
 * <p>이 대비를 적어 두는 이유는 <b>두 매퍼를 나란히 보는 사람이 한쪽을 다른 쪽에 맞추려
 * 들기 때문</b>이다. 005 를 004 에 맞추면 스키마에 이름 컬럼 2개를 추가하게 되고, 그건
 * 이 기능의 전제(스키마 무변경)를 깬다. 반대로 004 를 005 에 맞추면 과거 지출의 이름이
 * 원본을 따라 움직여 003 의 SC-205 가 깨진다.
 *
 * <p><b>연관은 LAZY 다.</b> 여기서 {@code getName()} 을 부르는 순간 쿼리가 나간다.
 * 목록(4.2)에서 N+1 이 실제로 발생하며, 한 회원의 고정지출이 많아야 수십 건이고
 * 페이징도 있어 현재는 감수한다. 문제가 되면 fetch join 을 <b>Repository 쪽에</b>
 * 넣는다 — 매퍼는 변환만 하고 조회 전략을 갖지 않는다(헌장 원칙 II).
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/research.md">research.md §6</a>
 */
@Mapper(componentModel = "spring")
public interface FixedExpenseMapper {

    @Mapping(target = "fixedExpenseId", source = "idx")
    @Mapping(target = "paymentMethodId", source = "paymentMethod.idx")
    @Mapping(target = "paymentMethodName", source = "paymentMethod.name")
    @Mapping(target = "expendGroupId", source = "expendGroup.idx")
    @Mapping(target = "expendGroupName", source = "expendGroup.name")
    FixedExpenseResponse toResponse(UserFixedExpense entity);
}
