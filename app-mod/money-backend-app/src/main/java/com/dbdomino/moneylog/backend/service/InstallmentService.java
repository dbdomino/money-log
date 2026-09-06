package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.InstallmentCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.InstallmentCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.InstallmentSettleResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 할부 등록(3.5)과 중도상환(3.6).
 *
 * <p>할부 회차의 <b>조회·수정·삭제는 여기가 아니라 {@link ExpenseService}</b> 가 한다 —
 * 명세가 일시불과 같은 API 를 쓰도록 정했고(US3 시나리오 5·6) 대상이 언제나 "그 달 1건"
 * 이라 다를 이유가 없다(FR-313). 이 클래스는 <b>그룹 단위 연산</b> 둘만 맡는다.
 *
 * <h2>결제일은 매월 1일이다 (FR-324)</h2>
 *
 * <p>회차 n 의 결제일은 {@code startYearMonth} 에 n-1개월을 더한 달의 1일이다. 요청이
 * 일(day)을 받지 않고 수단에도 결제일 컬럼이 없어 카드별 결제일을 쓸 수 없다.
 * 1일 고정이면 <b>말일 보정이 필요 없다</b> — 31일 시작 할부의 2월 회차 문제가 생기지 않는다.
 *
 * <h2>중도상환의 경계는 {@code >} 다 (FR-315)</h2>
 *
 * <p>{@code payment_date > today} 인 회차만 지운다. {@code >=} 로 잡으면 <b>오늘 결제된
 * 회차까지 사라져 이번 달 합계가 소급해 줄어든다</b> — 사용자가 이미 본 숫자가 바뀐다.
 * "미결제"를 기준으로 삼지 않는 것은 {@code tbl_expense} 에 결제 완료 여부 컬럼이 없어
 * 날짜 말고는 판정할 근거가 없기 때문이다.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/api-contract.md">api-contract.md §6</a>
 */
@Service
public class InstallmentService {

    private final UserExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ReferenceResolver referenceResolver;

    public InstallmentService(UserExpenseRepository expenseRepository,
                              UserRepository userRepository,
                              ReferenceResolver referenceResolver) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.referenceResolver = referenceResolver;
    }

    /**
     * 3.5 할부 등록. 판정 순서를 api-contract.md §6 그대로 따른다.
     *
     * <pre>{@code
     * 1. 개월 수·금액 검증                     → 3204
     *    ├ months < 2  (1개월 할부는 일시불이다, FR-311)
     *    └ amount <= 0
     * 2. 참조 검증 (사용 중인가)                → 3003 / 3103
     * 3. seq_installment_group.nextval → groupId
     * 4. N개 행을 한 트랜잭션에 INSERT
     *    └ 도중 실패 → 전체 롤백 → 3205
     * }</pre>
     *
     * <p><b>4번이 전체 롤백이다</b>(FR-310). 부분 생성되면 사용자가 재등록할 때 앞부분이
     * 중복되는데, 004 는 업무 유일 제약을 두지 않으므로(FR-309) DB 가 그 중복을 막아주지
     * 않는다. {@code @Transactional} 이 그것을 보장한다 — 예외가 나가면 이미 넣은 행도
     * 함께 사라진다.
     *
     * <p><b>시퀀스는 그룹당 한 번만 뽑는다</b>(FR-312). 행마다 뽑으면 그룹이 흩어져
     * 중도상환이 아무것도 찾지 못한다.
     */
    @Transactional
    public InstallmentCreateResponse create(AuthPrincipal principal,
                                            InstallmentCreateRequest request) {
        int months = requireMonths(request.installmentMonths());
        long monthlyAmount = LedgerFieldRules.requireAmount(
                request.monthlyAmount(), ErrorCode.INSTALLMENT_VALIDATION_FAILED);
        YearMonth start = requireStartYearMonth(request.startYearMonth());

        UserPaymentMethod paymentMethod = referenceResolver.requireUsablePaymentMethod(
                principal, request.paymentMethodId(), UserPaymentMethod.PURPOSE_EXPENSE);
        UserExpendGroup expendGroup =
                referenceResolver.requireUsableExpendGroup(principal, request.expendGroupId());

        // 값 검증은 회차를 만들기 전에 한 번만 한다 — 전 회차가 같은 값을 갖는다.
        String place = LedgerFieldRules.requirePlace(
                request.place(), ErrorCode.INSTALLMENT_VALIDATION_FAILED);
        String content = LedgerFieldRules.requireContent(
                request.content(), ErrorCode.INSTALLMENT_VALIDATION_FAILED);

        Long groupId = expenseRepository.nextInstallmentGroupId();
        User owner = ownerOf(principal);

        List<UserExpense> rows = new ArrayList<>(months);
        for (int index = 1; index <= months; index++) {
            UserExpense expense = new UserExpense();
            expense.setUser(owner);
            expense.setAmount(monthlyAmount);
            // 회차 n 의 결제일 = 시작 연월 + (n-1)개월의 1일. 말일 보정이 필요 없다.
            expense.setPaymentDate(start.plusMonths(index - 1L).atDay(1));
            expense.setPlace(place);
            expense.setContent(content);
            expense.setPaymentMethod(paymentMethod);
            expense.setPaymentMethodName(paymentMethod.getName());
            expense.setExpendGroup(expendGroup);
            expense.setExpendGroupName(expendGroup.getName());
            InstallmentColumns.markAsInstallment(expense, groupId, index, months);
            rows.add(expense);
        }

        try {
            expenseRepository.saveAllAndFlush(rows);
        } catch (RuntimeException e) {
            // 한 건이라도 실패하면 전체 롤백이다. 어느 회차에서 깨졌는지는 응답에 싣지
            // 않는다 — 사용자가 할 조치가 "다시 등록한다"로 같기 때문이다.
            throw new BusinessException(ErrorCode.INSTALLMENT_CREATE_FAILED);
        }
        return new InstallmentCreateResponse(groupId, months);
    }

    /**
     * 3.6 중도상환. 판정 순서를 api-contract.md §6 그대로 따른다.
     *
     * <pre>{@code
     * 1. installmentGroupId 로 그룹 조회 (본인 소유?)  없음·타인 → 3206
     * 2. payment_date > today 인 회차를 센다
     *    └ 0건                                       → 3207
     * 3. 그 회차들만 물리 삭제. payment_date <= today 는 남긴다
     * }</pre>
     *
     * <p><b>2번을 삭제 전에 한다.</b> 삭제 결과가 0건인 것을 보고 판정할 수도 있지만,
     * 그러면 "지울 게 없었다"를 확인하려고 DELETE 를 먼저 실행하게 된다.
     *
     * <p><b>{@code 3207} 을 멱등 성공으로 흘리지 않는다.</b> "방금 정리했다"와 "이미
     * 정리되어 있었다"를 화면이 구분해야 한다.
     *
     * <p>기준일은 <b>서버의 오늘</b>이다. 요청이 날짜를 주지 않으므로 클라이언트 시계에
     * 흔들리지 않는다.
     */
    @Transactional
    public InstallmentSettleResponse settleRemainder(AuthPrincipal principal,
                                                     Long installmentGroupId) {
        requireOwnedGroup(principal, installmentGroupId);

        LocalDate today = LocalDate.now();
        long remaining = expenseRepository
                .countByInstallmentGroupIdAndPaymentDateAfter(installmentGroupId, today);
        if (remaining == 0L) {
            throw new BusinessException(ErrorCode.INSTALLMENT_NOTHING_TO_SETTLE);
        }

        long settled = expenseRepository
                .deleteByInstallmentGroupIdAndPaymentDateAfter(installmentGroupId, today);
        return new InstallmentSettleResponse(installmentGroupId, settled,
                "남은 할부 " + settled + "건이 중도상환 처리되었습니다");
    }

    /**
     * 본인 소유 할부 그룹인가. 없거나 남의 것이면 {@code 3206} 이다.
     *
     * <p>조회에 <b>회원을 함께 건다</b> — {@code installmentGroupId} 는 요청이 주는 값이라
     * 남의 그룹을 가리킬 수 있다. 없는 그룹과 남의 그룹을 같은 코드로 묶어야 ID 를 훑는
     * 것만으로 남의 할부가 존재한다는 사실이 새어 나가지 않는다.
     */
    private void requireOwnedGroup(AuthPrincipal principal, Long installmentGroupId) {
        List<UserExpense> rows = expenseRepository
                .findByInstallmentGroupIdAndUserIdKeyOrderByInstallmentIndexAsc(
                        installmentGroupId, principal.idKey());
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.INSTALLMENT_GROUP_NOT_FOUND);
        }
    }

    /** 감사 컬럼과 FK 에 쓸 소유 회원. */
    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 할부 개월 수. <b>2 이상</b>이어야 한다(FR-311).
     *
     * <p>1개월 할부는 일시불이므로 3.1 을 쓴다. 여기서 허용하면 "할부인데 회차가 하나뿐인"
     * 행이 생겨 중도상환·집계가 그것을 어떻게 다룰지 정해야 한다.
     */
    private static int requireMonths(Integer months) {
        if (months == null || months < InstallmentColumns.MIN_MONTHS) {
            throw new BusinessException(ErrorCode.INSTALLMENT_VALIDATION_FAILED,
                    "할부 개월 수는 2 이상이어야 합니다.");
        }
        return months;
    }

    /** 시작 연월 {@code YYYY-MM}. 형식이 어긋나면 {@code 3204} 다. */
    private static YearMonth requireStartYearMonth(String text) {
        try {
            return YearMonth.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INSTALLMENT_VALIDATION_FAILED,
                    "할부 시작 연월은 YYYY-MM 형식이어야 합니다.");
        }
    }
}
