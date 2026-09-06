package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.ExpenseCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.ExpenseCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpenseDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpenseResponse;
import com.dbdomino.moneylog.backend.mapper.ExpenseMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일시불 지출 — 등록(3.1)·상세(3.2)·수정(3.3)·삭제(3.4).
 *
 * <p>할부 등록(3.5)과 중도상환(3.6)은 {@code InstallmentService} 가 맡는다. 다만
 * <b>할부 회차의 조회·수정·삭제는 여기서 한다</b> — 명세가 일시불과 같은 API 를 쓰도록
 * 정했고(US3 시나리오 5·6), 대상이 언제나 "그 달 1건"이라 다를 이유가 없다(FR-313).
 *
 * <h2>소유자는 토큰이 정한다</h2>
 *
 * <p>조회를 {@code idx + id_key} 로 걸어 없는 지출과 남의 지출이 같은 {@code 3202} 가 된다
 * (FR-301). 코드를 갈라 두면 ID 를 훑는 것만으로 남의 지출이 존재한다는 사실이 새어 나간다.
 *
 * <h2>삭제는 물리 삭제다</h2>
 *
 * <p>003 과 정반대다(FR-308). {@code tbl_expense} 에 {@code deleted} 컬럼이 <b>없고</b>,
 * 지출은 "과거 기록이 참조하는 대상"이 아니라 <b>기록 자체</b>라 지운다는 것이 "없었던
 * 일로 한다"는 뜻이다 — 남겨 두면 합계가 틀린다.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/api-contract.md">api-contract.md §4·§6</a>
 */
@Service
public class ExpenseService {

    /** 3.3 이 수정할 수 있는 필드. 그 밖의 이름이 오면 {@code 9001} 이다. */
    private static final Set<String> UPDATABLE_FIELDS = Set.of(
            "paymentMethodId", "expendGroupId", "amount", "paymentDate", "place", "content");

    /**
     * 할부 구조를 바꾸려는 시도로 보는 필드({@code 3203}).
     *
     * <p>{@code UPDATABLE_FIELDS} 에 없으므로 그냥 두면 {@code 9001} 이 나가는데, 명세는
     * 이 경우를 <b>{@code 3203}</b>(할부 개월·시작 연월 변경 불가)으로 정했다. "바꾸려면
     * 삭제 후 재등록한다"는 안내를 화면이 띄워야 하기 때문이다(FR-314).
     */
    private static final Set<String> INSTALLMENT_FIELDS = Set.of(
            "installmentGroupId", "installmentIndex", "installmentTotal",
            "installmentMonths", "startYearMonth");

    private final UserExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ReferenceResolver referenceResolver;
    private final ExpenseMapper expenseMapper;

    public ExpenseService(UserExpenseRepository expenseRepository,
                          UserRepository userRepository,
                          ReferenceResolver referenceResolver,
                          ExpenseMapper expenseMapper) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.referenceResolver = referenceResolver;
        this.expenseMapper = expenseMapper;
    }

    /**
     * 3.1 지출 등록. 판정 순서를 api-contract.md §6 그대로 따른다.
     *
     * <pre>{@code
     * 1. 필수 필드 누락                    → 9001  (Bean Validation 이 Controller 에서 잡는다)
     * 2. 참조 검증 (수단·유형이 사용 중인가) → 3003 / 3103
     * 3. 값 검증 (금액·날짜·길이)           → 3201
     * 4. 스냅샷 획득 + INSERT (할부 3컬럼은 전부 NULL)
     * }</pre>
     *
     * <p>3번의 <b>길이 검사를 빠뜨리면</b> DB 오류가 {@code 9000}(서버 오류)으로 새어 나간다.
     * 사용자 입력 문제인데 서버 장애처럼 보인다.
     *
     * <p>응답은 <b>생성 PK 한 칸</b>이다 — 상세는 3.2 로 읽는다(설계 명세 3.1).
     */
    @Transactional
    public ExpenseCreateResponse create(AuthPrincipal principal, ExpenseCreateRequest request) {
        var paymentMethod =
                referenceResolver.requireUsablePaymentMethod(
                        principal, request.paymentMethodId(), UserPaymentMethod.PURPOSE_EXPENSE);
        var expendGroup =
                referenceResolver.requireUsableExpendGroup(principal, request.expendGroupId());

        UserExpense expense = new UserExpense();
        expense.setUser(ownerOf(principal));
        expense.setAmount(LedgerFieldRules.requireAmount(request.amount(), ErrorCode.EXPENSE_FIELD_INVALID));
        expense.setPaymentDate(
                LedgerFieldRules.requireDate(request.paymentDate(), ErrorCode.EXPENSE_FIELD_INVALID));
        expense.setPlace(LedgerFieldRules.requirePlace(request.place(), ErrorCode.EXPENSE_FIELD_INVALID));
        expense.setContent(
                LedgerFieldRules.requireContent(request.content(), ErrorCode.EXPENSE_FIELD_INVALID));

        // 참조와 스냅샷을 함께 건다. 이름은 그 시점 원본에서 읽는다(FR-302).
        expense.setPaymentMethod(paymentMethod);
        expense.setPaymentMethodName(paymentMethod.getName());
        expense.setExpendGroup(expendGroup);
        expense.setExpendGroupName(expendGroup.getName());

        // 일시불이므로 할부 3컬럼을 전부 비운다(api-contract.md §8).
        InstallmentColumns.markAsLumpSum(expense);

        return new ExpenseCreateResponse(expenseRepository.save(expense).getIdx());
    }

    /**
     * 3.2 상세 조회. <b>할부 회차도 같은 API 로 읽힌다</b>(US3 시나리오 5).
     *
     * <p>참조하던 수단·유형이 나중에 죽어도 정상 동작한다(FR-326) — 조회는 참조를 새로
     * 걸지 않으므로 검증할 것이 없다.
     */
    @Transactional(readOnly = true)
    public ExpenseResponse get(AuthPrincipal principal, Long expenseId) {
        return expenseMapper.toResponse(findOwned(principal, expenseId));
    }

    /**
     * 3.3 지출 수정. 판정 순서를 api-contract.md §6 그대로 따른다.
     *
     * <pre>{@code
     * 1. 대상 조회 (본인 소유?)                  없음·타인 → 3202
     * 2. 할부 개월 수·시작 연월을 바꾸려 하는가?   → 3203
     * 3. 참조 필드를 보냈고 값이 달라졌는가?
     *    └ 새 수단·유형이 사용 중이 아님          → 3003 / 3103
     * 4. 값 검증 (금액·날짜·길이)                 → 3201
     * 5. UPDATE (+ 참조가 바뀌었으면 스냅샷 갱신)
     * }</pre>
     *
     * <p><b>2번이 3·4번보다 먼저다.</b> 할부 구조를 바꾸려는 요청은 다른 필드가 아무리
     * 정상이어도 거절이며, <b>할부 건이든 일시불이든</b> 그 필드를 보냈으면 {@code 3203} 이다
     * (FR-314).
     *
     * <p><b>스냅샷 갱신은 {@link ReferenceResolver} 에 맡긴다.</b> 여기서 다시 판정하지
     * 않는다 — 세 갈래(omit · 같은 값 · 다른 값)를 두 곳에서 구현하면 반드시 갈린다.
     */
    @Transactional
    public ExpenseResponse update(AuthPrincipal principal, Long expenseId,
                                  Map<String, Object> body) {
        UserExpense expense = findOwned(principal, expenseId);
        rejectInstallmentChange(body);

        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);

        // 참조 변경 — 바뀔 때만 검증하고 스냅샷을 함께 갱신한다(FR-304).
        referenceResolver.resolvePaymentMethodChange(principal,
                        referenceId(fields, "paymentMethodId"),
                        expense.getPaymentMethod().getIdx(), UserPaymentMethod.PURPOSE_EXPENSE)
                .ifPresent(method -> {
                    expense.setPaymentMethod(method);
                    expense.setPaymentMethodName(method.getName());
                });
        referenceResolver.resolveExpendGroupChange(principal,
                        referenceId(fields, "expendGroupId"),
                        expense.getExpendGroup().getIdx())
                .ifPresent(group -> {
                    expense.setExpendGroup(group);
                    expense.setExpendGroupName(group.getName());
                });

        if (fields.has("amount")) {
            expense.setAmount(LedgerFieldRules.requireAmount(
                    amount(fields), ErrorCode.EXPENSE_FIELD_INVALID));
        }
        if (fields.has("paymentDate")) {
            expense.setPaymentDate(LedgerFieldRules.requireDate(
                    fields.string("paymentDate"), ErrorCode.EXPENSE_FIELD_INVALID));
        }
        if (fields.has("place")) {
            expense.setPlace(LedgerFieldRules.requirePlace(
                    fields.string("place"), ErrorCode.EXPENSE_FIELD_INVALID));
        }
        if (fields.has("content")) {
            expense.setContent(LedgerFieldRules.requireContent(
                    fields.string("content"), ErrorCode.EXPENSE_FIELD_INVALID));
        }

        return expenseMapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    /**
     * 3.4 지출 삭제 — <b>물리 삭제</b>다(FR-308).
     *
     * <p>할부 건이어도 <b>그 달 1건만</b> 지운다(FR-313). 남은 회차를 한 번에 정리하는 것은
     * 중도상환(3.6)의 몫이다.
     *
     * <p>"이미 삭제됨" 코드가 없다 — 행이 사라졌으므로 두 번째 요청은 그냥 {@code 3202} 다.
     * 003 이 {@code 3004}·{@code 3108} 로 그것을 구분하는 것과 다른 점이다.
     */
    @Transactional
    public ExpenseDeleteResponse delete(AuthPrincipal principal, Long expenseId) {
        UserExpense expense = findOwned(principal, expenseId);
        expenseRepository.delete(expense);
        return new ExpenseDeleteResponse(expense.getIdx(), "지출이 삭제되었습니다");
    }

    /**
     * 본인 소유 지출 1건. 없거나 남의 것이면 {@code 3202} 다.
     *
     * <p>조회 조건에 {@code id_key} 를 <b>함께 건다.</b> 먼저 꺼내 놓고 소유자를 비교하는
     * 방식은 비교를 빠뜨린 자리가 곧 구멍이 된다.
     */
    private UserExpense findOwned(AuthPrincipal principal, Long expenseId) {
        return expenseRepository.findByIdxAndUserIdKey(expenseId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
    }

    /** 감사 컬럼과 FK 에 쓸 소유 회원. 토큰이 가리키는 회원이 없다면 인증 자체가 이상한 것이다. */
    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 할부 구조를 바꾸려는 요청인가. 보냈으면 {@code 3203} 이다.
     *
     * <p>{@link PatchFields} 를 만들기 <b>전에</b> 원본 Body 를 본다 — 허용 목록에 없는
     * 이름이라 {@code PatchFields.of} 가 먼저 {@code 9001} 로 막아 버리기 때문이다.
     * 명세가 이 경우를 따로 {@code 3203} 으로 정했으므로 순서를 지켜야 한다.
     */
    private static void rejectInstallmentChange(Map<String, Object> body) {
        if (body == null) {
            return;
        }
        for (String name : body.keySet()) {
            if (INSTALLMENT_FIELDS.contains(name)) {
                throw new BusinessException(ErrorCode.EXPENSE_INSTALLMENT_IMMUTABLE);
            }
        }
    }

    /**
     * PATCH 로 온 참조 ID. 보내지 않았으면 {@code null} 이며 그것이 곧 omit 이다.
     *
     * <p>{@code "paymentMethodId": null} 을 <b>명시적으로</b> 보낸 요청은 {@code 9001} 로
     * 막는다 — 컬럼이 NOT NULL 이라 "참조를 비운다"는 조작이 없는데, 그대로 두면
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

    /**
     * PATCH 로 온 금액.
     *
     * <p>소수점·문자를 {@link PatchFields#longNumber} 에 맡기면 {@code 9001} 이 나가는데,
     * 금액의 값 오류는 <b>{@code 3201}</b> 이다(FR-305). 그래서 먼저 가려낸다.
     */
    private static Long amount(PatchFields fields) {
        if (fields.hasNonIntegerNumber("amount")) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID,
                    "금액은 0보다 큰 정수여야 합니다.");
        }
        return fields.longNumber("amount");
    }
}
