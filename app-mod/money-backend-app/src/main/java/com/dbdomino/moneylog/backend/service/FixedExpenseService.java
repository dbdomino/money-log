package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.FixedExpenseCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.FixedExpenseListQuery;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseListResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseResponse;
import com.dbdomino.moneylog.backend.mapper.FixedExpenseMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.FixedExpenseFieldRules.Period;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고정지출 설정 CRUD — 4.1 등록 · 4.2 목록 · 4.3 상세 · 4.4 수정 · 4.7 삭제.
 *
 * <h2>등록은 행 1건만 만든다 (FR-402)</h2>
 *
 * <p>적용 기간이 4개월이든 10년이든 <b>관리 행 하나</b>다. 월별 내역은 그 달을 처음
 * 조회할 때 생긴다(lazy 생성). 미리 만들면 10년짜리 설정 하나에 120행이 생기는데
 * 사용자가 실제로 여는 달은 몇 개뿐이고, 설정을 고칠 때마다 그 120행을 전부 손봐야 한다.
 *
 * <h2>수정은 다른 테이블도 바꾼다 (FR-412)</h2>
 *
 * <p>{@link #update} 는 응답에 드러나지 않는 부작용이 있다 — <b>미래 달이면서 사용자가
 * 직접 고치지 않은 월별 내역</b>을 새 설정값으로 갱신한다. 그 판정은
 * {@link FixedExpenseSyncService} 가 맡는다.
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/api-contract.md">api-contract.md §6</a>
 */
@Service
public class FixedExpenseService {

    /**
     * 4.4 가 받는 필드. 이 밖의 이름이 오면 {@link PatchFields} 가 {@code 9001} 로 막는다.
     *
     * <p><b>{@code fixedExpenseId} 가 없다.</b> Path 로 대상을 정하므로 Body 에 담길 이유가
     * 없고, 담을 수 있게 두면 "Path 와 다른 ID 를 보내면?"이라는 답할 필요 없는 질문이 생긴다.
     */
    private static final Set<String> UPDATABLE_FIELDS = Set.of(
            "name", "paymentMethodId", "expendGroupId", "amount", "paymentDayOfMonth",
            "content", "startYear", "startMonth", "endYear", "endMonth");

    private final UserFixedExpenseRepository fixedExpenseRepository;
    private final UserRepository userRepository;
    private final ReferenceResolver referenceResolver;
    private final FixedExpenseSyncService syncService;
    private final FixedExpenseMapper fixedExpenseMapper;

    public FixedExpenseService(UserFixedExpenseRepository fixedExpenseRepository,
                               UserRepository userRepository,
                               ReferenceResolver referenceResolver,
                               FixedExpenseSyncService syncService,
                               FixedExpenseMapper fixedExpenseMapper) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.userRepository = userRepository;
        this.referenceResolver = referenceResolver;
        this.syncService = syncService;
        this.fixedExpenseMapper = fixedExpenseMapper;
    }

    /**
     * 4.1 등록.
     *
     * <p><b>판정 순서가 결과 코드를 바꾼다</b>(api-contract §6).
     *
     * <pre>{@code
     * 1. 참조 검증
     *    ├ 수단이 없거나 타인 소유이거나 사용 불가  → 3003
     *    ├ 지출유형이 없거나 타인 소유이거나 사용 불가 → 3103
     *    └ 수단의 purpose 가 EXPENSE 가 아님        → 3401
     * 2. 값 검증                                   → 3401
     * 3. 관리 행 1건만 INSERT
     * }</pre>
     *
     * <p>1번의 마지막 갈래가 004 와 다른 지점이다. 004 는 용도 불일치까지 {@code 3003} 으로
     * 묶어 존재를 감췄지만, 여기 수단은 <b>사용자가 자기 목록에서 고른 것</b>이라 존재가
     * 이미 드러나 있고 취할 조치도 다르다("지출용 수단을 고른다").
     */
    @Transactional
    public FixedExpenseResponse create(AuthPrincipal principal,
                                       FixedExpenseCreateRequest request) {
        UserPaymentMethod paymentMethod = requireExpensePaymentMethod(
                principal, request.paymentMethodId());
        UserExpendGroup expendGroup =
                referenceResolver.requireUsableExpendGroup(principal, request.expendGroupId());

        Period period = FixedExpenseFieldRules.requirePeriod(
                request.startYear(), request.startMonth(), request.endYear(), request.endMonth());

        UserFixedExpense setting = new UserFixedExpense();
        setting.setUser(ownerOf(principal));
        setting.setName(FixedExpenseFieldRules.requireName(request.name()));
        setting.setContent(FixedExpenseFieldRules.requireContent(request.content()));
        setting.setAmount(FixedExpenseFieldRules.requireAmount(request.amount()));
        setting.setPaymentDayOfMonth(
                FixedExpenseFieldRules.requirePaymentDay(request.paymentDayOfMonth()));
        setting.setPaymentMethod(paymentMethod);
        setting.setExpendGroup(expendGroup);
        applyPeriod(setting, period);

        // 여기서 끝이다. 적용 기간 전체의 월별 내역을 만들지 않는다(FR-402).
        return fixedExpenseMapper.toResponse(fixedExpenseRepository.save(setting));
    }

    /**
     * 4.2 목록. {@code totalCount} 는 <b>조건에 맞는 전체 건수</b>다.
     *
     * <p>정렬을 {@code idx} 오름차순으로 고정한다. 정렬이 없으면 페이지마다 순서가 흔들려
     * 같은 행이 두 페이지에 나오거나 아예 빠질 수 있다.
     */
    @Transactional(readOnly = true)
    public FixedExpenseListResponse list(AuthPrincipal principal, FixedExpenseListQuery query) {
        Page<UserFixedExpense> page = fixedExpenseRepository.findByUserIdKey(
                principal.idKey(),
                PageRequest.of(query.pageNumber(), query.limit(), Sort.by(Sort.Order.asc("idx"))));

        List<FixedExpenseResponse> items = page.getContent().stream()
                .map(fixedExpenseMapper::toResponse)
                .toList();
        return new FixedExpenseListResponse(
                items, query.offset(), query.limit(), page.getTotalElements());
    }

    /** 4.3 상세. 없거나 남의 것이면 {@code 3402} 다. */
    @Transactional(readOnly = true)
    public FixedExpenseResponse get(AuthPrincipal principal, Long fixedExpenseId) {
        return fixedExpenseMapper.toResponse(findOwned(principal, fixedExpenseId));
    }

    /**
     * 4.4 수정 — <b>omit = 유지</b>.
     *
     * <p>기간 검증이 까다로운 지점이다. <b>보낸 값만 보고 판단하면 안 된다</b> — 시작만
     * 2027-05 로 밀고 종료를 omit 하면 기존 종료(2027-02)와 합쳐져 뒤집힌 상태가 된다.
     * 보낸 값만 보면 통과하고, 그러면 DB CHECK {@code ck_fixed_expense_period} 가 걸려
     * 사용자에게 {@code 3401} 이 아니라 <b>{@code 9000}</b>(서버 오류)이 나간다.
     * 그래서 <b>병합 후 최종 값</b>으로 검증한다.
     *
     * <p><b>참조를 안 보내면 검증하지 않는다.</b> 삭제 표시된 수단을 쓰던 설정의 금액만
     * 고치는 수정이 성공해야 하기 때문이다 — 004 의 FR-326 과 같은 규칙이며,
     * {@link ReferenceResolver#resolvePaymentMethodChange} 의 첫 갈래가 그것이다.
     *
     * <p>마지막에 {@link FixedExpenseSyncService#propagate} 를 부른다. 응답에는 나타나지
     * 않지만 <b>다른 테이블의 행이 바뀐다</b>(FR-412).
     */
    @Transactional
    public FixedExpenseResponse update(AuthPrincipal principal, Long fixedExpenseId,
                                       Map<String, Object> body) {
        UserFixedExpense setting = findOwned(principal, fixedExpenseId);
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);

        applyReferenceChanges(principal, setting, fields);

        if (fields.has("name")) {
            setting.setName(FixedExpenseFieldRules.requireName(fields.string("name")));
        }
        if (fields.has("content")) {
            setting.setContent(FixedExpenseFieldRules.requireContent(fields.string("content")));
        }
        if (fields.has("amount")) {
            setting.setAmount(FixedExpenseFieldRules.requireAmount(amountOf(fields)));
        }
        if (fields.has("paymentDayOfMonth")) {
            setting.setPaymentDayOfMonth(
                    FixedExpenseFieldRules.requirePaymentDay(intOf(fields, "paymentDayOfMonth")));
        }
        applyPeriodChange(setting, fields);

        UserFixedExpense saved = fixedExpenseRepository.saveAndFlush(setting);

        // 응답에 드러나지 않는 부작용이다 — 미래 달이면서 modified=false 인 월별 내역이
        // 새 설정값을 따라간다. 이번 달과 지난 달은 건드리지 않는다.
        syncService.propagate(saved);

        return fixedExpenseMapper.toResponse(saved);
    }

    /**
     * 4.7 삭제 — <b>물리 삭제</b>다(FR-416).
     *
     * <p>그 고정지출의 월별 내역이 <b>지난 달 것까지 전부</b> 함께 사라진다.
     * {@code fk_fixed_monthly_fixed_expense} 에 {@code ON DELETE CASCADE} 가 걸려 있어
     * DB 가 처리하므로 여기서 자식을 먼저 지우지 않는다.
     *
     * <p><b>막는 조건이 없다.</b> 003 의 지출유형 삭제는 그 유형을 쓴 지출이 있으면
     * {@code 3106} 으로 막았지만, 고정지출을 참조하는 것은 자기 월별 내역뿐이라 함께 지운다.
     */
    @Transactional
    public FixedExpenseDeleteResponse delete(AuthPrincipal principal, Long fixedExpenseId) {
        UserFixedExpense setting = findOwned(principal, fixedExpenseId);
        fixedExpenseRepository.delete(setting);
        return new FixedExpenseDeleteResponse(setting.getIdx(), "고정지출이 삭제되었습니다");
    }

    // ── 내부 ────────────────────────────────────────────────────────────────

    /**
     * 참조 검증 — 소유·사용 가능({@code 3003})과 용도({@code 3401})를 <b>나눠서</b> 본다.
     *
     * <p>한 메서드로 합치면 그 순서가 구현 안에 숨는다. 순서가 바뀌면 "남의 소득용 수단"이
     * {@code 3003} 이 아니라 {@code 3401} 로 나가 존재가 새어 나간다.
     */
    private UserPaymentMethod requireExpensePaymentMethod(AuthPrincipal principal, Long id) {
        UserPaymentMethod method = referenceResolver.requireOwnedUsablePaymentMethod(principal, id);
        return referenceResolver.requirePurpose(method, UserPaymentMethod.PURPOSE_EXPENSE,
                ErrorCode.FIXED_EXPENSE_FIELD_INVALID);
    }

    /** 수정에서 참조 둘을 바꿔야 하는지 판정하고 반영한다. 안 보냈으면 아무 일도 하지 않는다. */
    private void applyReferenceChanges(AuthPrincipal principal, UserFixedExpense setting,
                                       PatchFields fields) {
        Long requestedMethodId = referenceId(fields, "paymentMethodId");
        if (requestedMethodId != null
                && !requestedMethodId.equals(setting.getPaymentMethod().getIdx())) {
            setting.setPaymentMethod(requireExpensePaymentMethod(principal, requestedMethodId));
        }
        referenceResolver.resolveExpendGroupChange(principal,
                        referenceId(fields, "expendGroupId"),
                        setting.getExpendGroup().getIdx())
                .ifPresent(setting::setExpendGroup);
    }

    /**
     * 기간 수정 — <b>기존 값과 병합한 뒤</b> 검증한다.
     *
     * <p>넷 중 하나만 보내도 나머지 셋은 기존 값을 쓴다. 그 합쳐진 결과가 유효해야 한다.
     */
    private void applyPeriodChange(UserFixedExpense setting, PatchFields fields) {
        boolean touched = fields.has("startYear") || fields.has("startMonth")
                || fields.has("endYear") || fields.has("endMonth");
        if (!touched) {
            return;
        }
        Period merged = FixedExpenseFieldRules.requirePeriod(
                mergedInt(fields, "startYear", setting.getStartYear()),
                mergedInt(fields, "startMonth", setting.getStartMonth()),
                mergedInt(fields, "endYear", setting.getEndYear()),
                mergedInt(fields, "endMonth", setting.getEndMonth()));
        applyPeriod(setting, merged);
    }

    private static void applyPeriod(UserFixedExpense setting, Period period) {
        setting.setStartYear(period.start().year());
        setting.setStartMonth(period.start().month());
        setting.setEndYear(period.end().year());
        setting.setEndMonth(period.end().month());
    }

    private static Integer mergedInt(PatchFields fields, String name, Integer current) {
        return fields.has(name) ? intOf(fields, name) : current;
    }

    private UserFixedExpense findOwned(AuthPrincipal principal, Long fixedExpenseId) {
        return fixedExpenseRepository.findByIdxAndUserIdKey(fixedExpenseId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_NOT_FOUND));
    }

    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 참조 ID 를 꺼낸다. 보내지 않았으면 {@code null}(= 바꾸지 않는다)이다.
     *
     * <p>명시적 {@code null} 은 거절한다 — 두 컬럼 다 NOT NULL 이라 "비운다"는 조작이 없다.
     */
    private static Long referenceId(PatchFields fields, String name) {
        if (!fields.has(name)) {
            return null;
        }
        Long value = fields.longNumber(name);
        if (value == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " 은(는) 비울 수 없습니다.");
        }
        return value;
    }

    /**
     * 금액을 꺼낸다. <b>소수점이 오면 {@code 3401} 이다</b>({@code 9001} 이 아니다).
     *
     * <p>{@code 12000.5} 는 형식 오류가 아니라 값 오류다 — 원 단위 정수여야 한다는 규칙을
     * 어긴 것이고, 사용자가 받아야 할 안내도 "금액을 확인하세요"다.
     */
    private static Long amountOf(PatchFields fields) {
        if (fields.hasNonIntegerNumber("amount")) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "금액은 원 단위 정수여야 합니다.");
        }
        return fields.longNumber("amount");
    }

    /** 연·월·결제일처럼 작은 정수를 꺼낸다. 같은 이유로 소수점은 {@code 3401} 이다. */
    private static Integer intOf(PatchFields fields, String name) {
        if (fields.hasNonIntegerNumber(name)) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    name + " 은(는) 정수여야 합니다.");
        }
        Long value = fields.longNumber(name);
        if (value == null) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    name + " 은(는) 비울 수 없습니다.");
        }
        return Math.toIntExact(value);
    }
}
