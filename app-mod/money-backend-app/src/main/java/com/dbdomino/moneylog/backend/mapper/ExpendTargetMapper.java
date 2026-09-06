package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDefaultResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDetailResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetMonthlyResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetResponse;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 목표금액 Entity ↔ DTO.
 *
 * <h2>목표 행이 아니라 지출유형에서 출발한다</h2>
 *
 * <p>다른 매퍼와 달리 <b>변환의 출발점이 그 API 의 저장 단위가 아니다</b>. 목표금액
 * 응답 한 줄은 {@code tbl_expend_target_default} 나 {@code tbl_expend_target_monthly} 의
 * 한 행이 아니라 <b>지출유형 하나 + 두 층의 금액</b>이다.
 *
 * <p>그럴 수밖에 없는 이유가 둘이다.
 *
 * <ol>
 *   <li><b>목표 행이 없어도 줄이 나와야 한다.</b> 5.1 의 모집단은 사용 중 지출유형이지
 *       저장된 목표가 아니다 — 한 번도 목표를 정하지 않은 유형도
 *       {@code defaultTargetAmount=0}·{@code monthlyTargetAmount=null} 로 나온다.
 *       목표 행에서 출발하면 그 줄이 통째로 사라진다.</li>
 *   <li><b>한 줄이 두 테이블에서 온다.</b> 두 층이 독립 저장이라 어느 한쪽을 출발점으로
 *       삼으면 나머지가 부속이 된다.</li>
 * </ol>
 *
 * <h2>이름은 현재 이름이다</h2>
 *
 * <p>{@code group.name} 을 그대로 읽는다. {@code tbl_expend_target_*} 에 이름 컬럼이
 * <b>없어서</b>(덤프 확인) 여기서 읽을 스냅샷이 애초에 없다 — 005 의
 * {@link FixedExpenseMapper} 와 같은 성격이고 004 의 {@link ExpenseMapper}(스냅샷)와
 * 반대다. 목표금액은 <b>지금 유효한 설정</b>이지 과거 기록이 아니다(api-contract §9).
 *
 * <p>같은 006 안에서 통계 상세(5.5·5.6)는 반대로 스냅샷을 저장한다. 대상이 달라 규칙도
 * 다르며, 목표금액 API(5.1~5.4)는 현재 이름 · 통계 API(5.5·5.6)는 스냅샷이다.
 *
 * <p><b>비즈니스 판정을 담지 않는다</b>(헌장 원칙 II). 어느 유형이 목록에 드는가,
 * 월별 값이 있는가 없는가는 서비스가 정하고 여기는 받은 값을 옮기기만 한다.
 */
@Mapper(componentModel = "spring")
public interface ExpendTargetMapper {

    /**
     * 5.1 목록의 한 줄.
     *
     * @param monthlyTargetAmount 그 달 행이 없으면 {@code null} 을 그대로 넘긴다 —
     *                            여기서 0 으로 접지 않는다(FR-506)
     */
    @Mapping(target = "expendGroupId", source = "group.idx")
    @Mapping(target = "expendGroupName", source = "group.name")
    ExpendTargetResponse toResponse(UserExpendGroup group,
                                    long defaultTargetAmount,
                                    Long monthlyTargetAmount);

    /** 5.2 상세. 목록의 한 줄에 연·월이 붙은 것이다. */
    @Mapping(target = "expendGroupId", source = "group.idx")
    @Mapping(target = "expendGroupName", source = "group.name")
    ExpendTargetDetailResponse toDetailResponse(int year, int month,
                                                UserExpendGroup group,
                                                long defaultTargetAmount,
                                                Long monthlyTargetAmount);

    /** 5.3 저장 결과. 담당한 층만 싣는다. */
    @Mapping(target = "expendGroupId", source = "group.idx")
    @Mapping(target = "expendGroupName", source = "group.name")
    ExpendTargetDefaultResponse toDefaultResponse(UserExpendGroup group,
                                                  long defaultTargetAmount);

    /** 5.4 저장 결과. 담당한 층만 싣는다. */
    @Mapping(target = "expendGroupId", source = "group.idx")
    @Mapping(target = "expendGroupName", source = "group.name")
    ExpendTargetMonthlyResponse toMonthlyResponse(int year, int month,
                                                  UserExpendGroup group,
                                                  long monthlyTargetAmount);
}
