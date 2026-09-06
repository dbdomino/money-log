package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.IncomeCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.IncomeCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.IncomeDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.IncomeResponse;
import com.dbdomino.moneylog.backend.mapper.IncomeMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserIncome;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소득 — 등록(3.7)·상세(3.8)·수정(3.9)·삭제(3.10).
 *
 * <h2>지출과 구조가 다르다</h2>
 *
 * <table border="1">
 *   <caption>api-contract.md §7</caption>
 *   <tr><th>항목</th><th>지출</th><th>소득</th></tr>
 *   <tr><td>지출유형</td><td>필수</td><td><b>없다</b></td></tr>
 *   <tr><td>장소</td><td>필수</td><td><b>없다</b></td></tr>
 *   <tr><td>할부</td><td>있다</td><td><b>없다</b></td></tr>
 *   <tr><td>{@code content}</td><td>필수</td><td><b>비어 있을 수 있다</b></td></tr>
 *   <tr><td>값 오류</td><td>{@code 3201}</td><td>{@code 3301}</td></tr>
 *   <tr><td>없음·타인 소유</td><td>{@code 3202}</td><td>{@code 3302}</td></tr>
 * </table>
 *
 * <p><b>{@code ExpenseService} 를 상속하거나 코드를 공유하지 않는다.</b> 구조가 닮았지만
 * 위 여섯 줄이 전부 다르고, 특히 실패 코드를 물려받으면 소득이 지출의 대역을 내보낸다.
 *
 * <p>공유하는 것은 <b>규칙</b>이지 구현이 아니다 — 값 규칙은 {@link LedgerFieldRules},
 * 참조 검증과 스냅샷 판정은 {@link ReferenceResolver} 한 곳에서 나온다.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/api-contract.md">api-contract.md §7</a>
 */
@Service
public class IncomeService {

    /**
     * 3.9 가 수정할 수 있는 필드. 그 밖의 이름이 오면 {@code 9001} 이다.
     *
     * <p>지출의 목록과 달리 {@code place}·{@code expendGroupId} 가 없다 — 컬럼이 없으므로
     * 그 이름으로 수정하려는 요청은 오타이거나 지출 API 와 혼동한 것이다.
     */
    private static final Set<String> UPDATABLE_FIELDS =
            Set.of("paymentMethodId", "amount", "paymentDate", "content");

    private final UserIncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final ReferenceResolver referenceResolver;
    private final IncomeMapper incomeMapper;

    public IncomeService(UserIncomeRepository incomeRepository,
                         UserRepository userRepository,
                         ReferenceResolver referenceResolver,
                         IncomeMapper incomeMapper) {
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
        this.referenceResolver = referenceResolver;
        this.incomeMapper = incomeMapper;
    }

    /**
     * 3.7 소득 등록.
     *
     * <pre>{@code
     * 1. 필수 필드 누락                → 9001
     * 2. 참조 검증 (소득용 수단인가)     → 3003
     * 3. 값 검증 (금액·날짜·길이)       → 3301
     * 4. 스냅샷 획득 + INSERT
     * }</pre>
     *
     * <p><b>지출유형을 부르지 않는다</b> — 소득에는 그 개념이 없다(FR-306).
     *
     * <p>수단은 {@code purpose=INCOME} 이어야 한다. 지출용 카드로 소득을 적을 수 있으면
     * 003 이 {@code purpose} 변경을 참조 0건일 때만 허용하는({@code 3005}) 이유가 무너진다.
     */
    @Transactional
    public IncomeCreateResponse create(AuthPrincipal principal, IncomeCreateRequest request) {
        var paymentMethod = referenceResolver.requireUsablePaymentMethod(
                principal, request.paymentMethodId(), UserPaymentMethod.PURPOSE_INCOME);

        UserIncome income = new UserIncome();
        income.setUser(ownerOf(principal));
        income.setAmount(
                LedgerFieldRules.requireAmount(request.amount(), ErrorCode.INCOME_FIELD_INVALID));
        income.setPaymentDate(
                LedgerFieldRules.requireDate(request.paymentDate(), ErrorCode.INCOME_FIELD_INVALID));
        income.setContent(LedgerFieldRules.normalizeOptionalContent(
                request.content(), ErrorCode.INCOME_FIELD_INVALID));

        income.setPaymentMethod(paymentMethod);
        income.setPaymentMethodName(paymentMethod.getName());

        return new IncomeCreateResponse(incomeRepository.save(income).getIdx());
    }

    /** 3.8 상세 조회. 참조하던 수단이 나중에 죽어도 정상 동작한다(FR-326). */
    @Transactional(readOnly = true)
    public IncomeResponse get(AuthPrincipal principal, Long incomeId) {
        return incomeMapper.toResponse(findOwned(principal, incomeId));
    }

    /**
     * 3.9 소득 수정.
     *
     * <pre>{@code
     * 1. 대상 조회 (본인 소유?)              없음·타인 → 3302
     * 2. 참조 필드를 보냈고 값이 달라졌는가?   → 3003
     * 3. 값 검증                            → 3301
     * 4. UPDATE (+ 참조가 바뀌었으면 스냅샷 갱신)
     * }</pre>
     *
     * <p>지출과 달리 <b>{@code 3203}(할부) 갈래가 없다</b> — 소득에는 할부가 없다.
     *
     * <p><b>{@code content} 에서 omit 과 {@code null} 이 갈린다.</b> 컬럼이 NULL 을
     * 허용하므로 {@code "content": null} 은 <b>비우라</b>는 뜻이고, 보내지 않은 것은
     * 그대로 두라는 뜻이다. 지출의 {@code content} 는 NOT NULL 이라 이 구분이 없다 —
     * {@link PatchFields} 가 왜 {@code Map} 으로 받는지가 여기서 실제로 쓰인다.
     */
    @Transactional
    public IncomeResponse update(AuthPrincipal principal, Long incomeId,
                                 Map<String, Object> body) {
        UserIncome income = findOwned(principal, incomeId);
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);

        referenceResolver.resolvePaymentMethodChange(principal,
                        referenceId(fields, "paymentMethodId"),
                        income.getPaymentMethod().getIdx(), UserPaymentMethod.PURPOSE_INCOME)
                .ifPresent(method -> {
                    income.setPaymentMethod(method);
                    income.setPaymentMethodName(method.getName());
                });

        if (fields.has("amount")) {
            income.setAmount(LedgerFieldRules.requireAmount(
                    amount(fields), ErrorCode.INCOME_FIELD_INVALID));
        }
        if (fields.has("paymentDate")) {
            income.setPaymentDate(LedgerFieldRules.requireDate(
                    fields.string("paymentDate"), ErrorCode.INCOME_FIELD_INVALID));
        }
        if (fields.has("content")) {
            // null 을 보냈으면 비운다. normalizeOptionalContent 가 null·빈 값을 모두 null 로 만든다.
            income.setContent(LedgerFieldRules.normalizeOptionalContent(
                    fields.string("content"), ErrorCode.INCOME_FIELD_INVALID));
        }

        return incomeMapper.toResponse(incomeRepository.saveAndFlush(income));
    }

    /**
     * 3.10 소득 삭제 — <b>물리 삭제</b>다(FR-308).
     *
     * <p>"이미 삭제됨" 코드가 없다 — 행이 사라졌으므로 두 번째 요청은 그냥 {@code 3302} 다.
     */
    @Transactional
    public IncomeDeleteResponse delete(AuthPrincipal principal, Long incomeId) {
        UserIncome income = findOwned(principal, incomeId);
        incomeRepository.delete(income);
        return new IncomeDeleteResponse(income.getIdx(), "소득이 삭제되었습니다");
    }

    /**
     * 본인 소유 소득 1건. 없거나 남의 것이면 {@code 3302} 다.
     *
     * <p>지출의 {@code 3202} 와 <b>코드가 갈린다</b>. 두 테이블이 각자 시퀀스를 쓰므로
     * ID 가 겹칠 수 있는데, 조회를 자원별로 나눠 두면 지출 ID 로 소득을 부르는 요청이
     * 여기서 걸린다.
     */
    private UserIncome findOwned(AuthPrincipal principal, Long incomeId) {
        return incomeRepository.findByIdxAndUserIdKey(incomeId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.INCOME_NOT_FOUND));
    }

    /** 감사 컬럼과 FK 에 쓸 소유 회원. */
    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * PATCH 로 온 수단 ID. 보내지 않았으면 {@code null} 이며 그것이 곧 omit 이다.
     *
     * <p>{@code "paymentMethodId": null} 을 명시적으로 보낸 요청은 {@code 9001} 로 막는다 —
     * 컬럼이 NOT NULL 이라 "참조를 비운다"는 조작이 없는데, 그대로 두면
     * {@link ReferenceResolver} 가 omit 으로 오해해 조용히 무시한다.
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

    /** PATCH 로 온 금액. 소수점·문자는 {@code 9001} 이 아니라 <b>{@code 3301}</b> 이다. */
    private static Long amount(PatchFields fields) {
        if (fields.hasNonIntegerNumber("amount")) {
            throw new BusinessException(ErrorCode.INCOME_FIELD_INVALID,
                    "금액은 0보다 큰 정수여야 합니다.");
        }
        return fields.longNumber("amount");
    }
}
