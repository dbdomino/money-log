package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.ExpendTargetListQuery;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDefaultResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDetailResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetListResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetMonthlyResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetResponse;
import com.dbdomino.moneylog.backend.mapper.ExpendTargetMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpendTargetDefault;
import com.dbdomino.moneylog.data.entity.UserExpendTargetMonthly;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserExpendTargetDefaultRepository;
import com.dbdomino.moneylog.data.repository.UserExpendTargetMonthlyRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목표금액 — 5.1 목록 · 5.2 상세 · 5.3 기본 저장 · 5.4 월별 저장.
 *
 * <h2>두 층은 독립이다 (FR-505)</h2>
 *
 * <p>기본을 바꿔도 저장된 월별 값은 변하지 않고, 기본을 정할 때 월별로 <b>복사하지
 * 않는다</b>. 복사해 두면 "이 달은 기본을 따른다"와 "이 달은 마침 기본과 같은 값으로
 * 정했다"가 구분되지 않는다 — 그리고 나중에 기본을 고쳤을 때 어느 달까지 따라가야 하는지가
 * 매번 판단거리가 된다.
 *
 * <p>그래서 이 클래스의 저장 메서드 둘은 <b>서로의 테이블을 건드리지 않는다</b>.
 *
 * <h2>판정 순서가 결과를 바꾼다</h2>
 *
 * <pre>{@code
 * 5.2  3603(연·월) → 3103(소유) → 3601(사용 중)
 * 5.3            3103(소유) → 3601(사용 중) → 3602(금액)
 * 5.4  3603(연·월) → 3103(소유) → 3601(사용 중) → 3602(금액)
 * }</pre>
 *
 * <p><b>소유({@code 3103})가 사용 여부({@code 3601})보다 먼저다.</b> 남의 유형 ID 로
 * 접근했는데 그게 마침 {@code in_use=false} 라면 {@code 3601} 을 내는 순간 그 ID 가
 * 실재한다는 사실이 코드 차이로 새어 나간다(quickstart #14).
 *
 * <p><b>금액({@code 3602})이 가장 나중이다.</b> 순서가 뒤집히면 남의 유형에 잘못된 금액을
 * 보냈을 때 {@code 3103} 대신 {@code 3602} 가 나가 같은 것이 드러난다.
 *
 * <p>연·월({@code 3603})은 {@code ExpendTargetListQuery} 와 컨트롤러의 Path 변환에서 이미
 * 끝나 있다 — 자원 좌표가 성립하지 않으면 유형을 찾을 이유가 없기 때문이다.
 *
 * @see <a href="../../../../../../../../specs/006-backend-target-statistics/contracts/target-amount.md">target-amount.md</a>
 */
@Service
public class ExpendTargetService {

    private final UserExpendGroupRepository expendGroupRepository;
    private final UserExpendTargetDefaultRepository defaultRepository;
    private final UserExpendTargetMonthlyRepository monthlyRepository;
    private final ReferenceResolver referenceResolver;
    private final ExpendTargetMapper mapper;

    public ExpendTargetService(UserExpendGroupRepository expendGroupRepository,
                               UserExpendTargetDefaultRepository defaultRepository,
                               UserExpendTargetMonthlyRepository monthlyRepository,
                               ReferenceResolver referenceResolver,
                               ExpendTargetMapper mapper) {
        this.expendGroupRepository = expendGroupRepository;
        this.defaultRepository = defaultRepository;
        this.monthlyRepository = monthlyRepository;
        this.referenceResolver = referenceResolver;
        this.mapper = mapper;
    }

    /**
     * 5.1 목록.
     *
     * <p><b>모집단이 목표 행이 아니라 사용 중 지출유형이다</b>(FR-509). 목표를 한 번도
     * 정하지 않은 유형도 {@code 0}·{@code null} 로 한 줄 나온다 — 목표금액 화면이
     * "아직 안 정한 유형"을 보여주고 거기에 값을 넣게 해야 하기 때문이다.
     *
     * <p><b>{@code totalCount} 도 같은 모집단에서 센다</b>(FR-526). 목록만 필터하고 건수를
     * 필터하지 않으면 페이지 수가 맞지 않아 화면이 빈 마지막 페이지를 그린다.
     *
     * <p><b>두 층을 각각 한 번씩만 읽는다.</b> 유형마다 단건 조회를 걸면 페이지 하나에
     * 쿼리가 {@code 2 × limit} 번 나간다(N+1).
     *
     * <p><b>페이징을 DB 가 아니라 메모리에서 한다.</b> 이름 부분 일치가 걸리면 남는 건수를
     * 세어야 하는데 모집단이 한 회원의 지출유형(많아야 수십 건)이라 전부 읽어도 부담이 없다.
     */
    @Transactional(readOnly = true)
    public ExpendTargetListResponse list(AuthPrincipal principal, ExpendTargetListQuery query) {
        YearMonthValue yearMonth = query.yearMonth();

        List<UserExpendGroup> groups =
                expendGroupRepository.findByUserIdKeyAndInUseTrueOrderByIdxAsc(principal.idKey())
                        .stream()
                        .filter(group -> query.matches(group.getName()))
                        .toList();

        Map<Long, Long> defaults = defaultAmountsOf(principal);
        Map<Long, Long> monthlies = monthlyAmountsOf(principal, yearMonth);

        List<ExpendTargetResponse> page = new ArrayList<>();
        for (int i = query.offset(); i < groups.size() && page.size() < query.limit(); i++) {
            UserExpendGroup group = groups.get(i);
            page.add(mapper.toResponse(group,
                    defaultAmountOf(defaults, group.getIdx()),
                    monthlies.get(group.getIdx())));
        }

        return new ExpendTargetListResponse(yearMonth.year(), yearMonth.month(), page,
                query.offset(), query.limit(), groups.size());
    }

    /**
     * 5.2 상세.
     *
     * <p>목록과 달리 <b>사용하지 않는 유형을 {@code 3601} 로 거절한다</b>(FR-510). 목록은
     * 조용히 빼는데 단건은 거절하는 것이 어긋나 보이지만, 단건은 사용자가 그 유형을
     * <b>지목한 것</b>이라 왜 볼 수 없는지 알려줘야 하고 목록은 지목한 것이 아니라 알릴
     * 대상이 없다.
     */
    @Transactional(readOnly = true)
    public ExpendTargetDetailResponse get(AuthPrincipal principal, YearMonthValue yearMonth,
                                          Long expendGroupId) {
        UserExpendGroup group = requireUsableGroup(principal, expendGroupId);
        return mapper.toDetailResponse(yearMonth.year(), yearMonth.month(), group,
                defaultAmountOf(principal, group),
                monthlyAmountOf(principal, yearMonth, group));
    }

    /**
     * 5.3 기본 목표 저장 — <b>있으면 갱신하고 없으면 만든다</b>(FR-512).
     *
     * <p>사용자에게 "최초 설정"과 "변경"은 같은 행위이므로 API 를 나누지 않는다. 나누면
     * 프론트가 "이미 있는가"를 먼저 조회해 분기해야 한다.
     *
     * <p><b>월별 목표를 건드리지 않는다</b>(FR-505).
     */
    @Transactional
    public ExpendTargetDefaultResponse upsertDefault(AuthPrincipal principal, Long expendGroupId,
                                                     Long targetAmount) {
        UserExpendGroup group = requireUsableGroup(principal, expendGroupId);
        long amount = ExpendTargetFieldRules.requireAmount(targetAmount);

        defaultRepository.upsert(principal.idKey(), group.getIdx(), amount, principal.idKey());

        return mapper.toDefaultResponse(group, amount);
    }

    /**
     * 5.4 월별 목표 저장.
     *
     * <p>규칙은 5.3 과 같고 좌표에 연·월이 붙는다. <b>기본 목표를 건드리지 않는다</b>
     * (FR-505).
     *
     * <p><b>{@code 0} 을 그대로 저장한다.</b> "그 달엔 쓰지 않겠다"라 행이 없는 상태와
     * 다르다(FR-506) — 0 을 "지우기"로 해석해 DELETE 로 바꾸면 그 구분이 사라진다.
     */
    @Transactional
    public ExpendTargetMonthlyResponse upsertMonthly(AuthPrincipal principal,
                                                     YearMonthValue yearMonth,
                                                     Long expendGroupId, Long targetAmount) {
        UserExpendGroup group = requireUsableGroup(principal, expendGroupId);
        long amount = ExpendTargetFieldRules.requireAmount(targetAmount);

        monthlyRepository.upsert(principal.idKey(), yearMonth.year(), yearMonth.month(),
                group.getIdx(), amount, principal.idKey());

        return mapper.toMonthlyResponse(yearMonth.year(), yearMonth.month(), group, amount);
    }

    // ── 공통 판정 ────────────────────────────────────────────────────────────

    /**
     * 소유({@code 3103}) → 사용 중({@code 3601}) 순으로 본다.
     *
     * <p>5.2·5.3·5.4 셋이 같은 순서를 요구한다. 한 곳에 두지 않으면 셋 중 하나만 뒤집혀도
     * 그 경로에서만 유형의 실재가 드러난다.
     */
    private UserExpendGroup requireUsableGroup(AuthPrincipal principal, Long expendGroupId) {
        UserExpendGroup group =
                referenceResolver.requireOwnedExpendGroup(principal, expendGroupId);
        return referenceResolver.requireInUse(group, ErrorCode.TARGET_GROUP_NOT_IN_USE);
    }

    // ── 두 층 읽기 ───────────────────────────────────────────────────────────

    private Map<Long, Long> defaultAmountsOf(AuthPrincipal principal) {
        Map<Long, Long> amounts = new HashMap<>();
        for (UserExpendTargetDefault row
                : defaultRepository.findByUserIdKeyOrderByIdxAsc(principal.idKey())) {
            amounts.put(row.getExpendGroup().getIdx(), row.getTargetAmount());
        }
        return amounts;
    }

    private Map<Long, Long> monthlyAmountsOf(AuthPrincipal principal, YearMonthValue yearMonth) {
        Map<Long, Long> amounts = new HashMap<>();
        for (UserExpendTargetMonthly row : monthlyRepository.findByUserIdKeyAndYearAndMonth(
                principal.idKey(), yearMonth.year(), yearMonth.month())) {
            amounts.put(row.getExpendGroup().getIdx(), row.getTargetAmount());
        }
        return amounts;
    }

    /**
     * 기본 목표는 <b>행이 없으면 {@code 0}</b> 이다(FR-507).
     *
     * <p>여기서 {@code null} 을 돌려주면 적용 금액이 {@code null} 이 될 수 있고, 그러면
     * 통계 저장이 NOT NULL 인 {@code tbl_statistics_expend_group.target_amount} 에서
     * 막힌다. 월별과 대칭으로 만들면 안 되는 이유다.
     */
    private static long defaultAmountOf(Map<Long, Long> amounts, Long expendGroupIdx) {
        Long amount = amounts.get(expendGroupIdx);
        return amount == null ? 0L : amount;
    }

    private long defaultAmountOf(AuthPrincipal principal, UserExpendGroup group) {
        return defaultRepository
                .findByUserIdKeyAndExpendGroupIdx(principal.idKey(), group.getIdx())
                .map(UserExpendTargetDefault::getTargetAmount)
                .orElse(0L);
    }

    /**
     * 월별 목표는 <b>행이 없으면 {@code null}</b> 이다(FR-506).
     *
     * <p>0 으로 접으면 "그 달은 따로 정하지 않았다"와 "그 달은 0원으로 정했다"가 같아지고,
     * 기본값을 무시하겠다는 의사 표시를 표현할 수 없게 된다.
     */
    private Long monthlyAmountOf(AuthPrincipal principal, YearMonthValue yearMonth,
                                 UserExpendGroup group) {
        return monthlyRepository
                .findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
                        principal.idKey(), yearMonth.year(), yearMonth.month(), group.getIdx())
                .map(UserExpendTargetMonthly::getTargetAmount)
                .orElse(null);
    }
}
