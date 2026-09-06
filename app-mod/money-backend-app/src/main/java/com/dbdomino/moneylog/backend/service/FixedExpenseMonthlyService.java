package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.FixedExpenseMonthlyListQuery;
import com.dbdomino.moneylog.backend.dto.request.FixedExpenseMonthlyUpdateRequest;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseMonthlyListResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseMonthlyResponse;
import com.dbdomino.moneylog.backend.mapper.FixedExpenseMonthlyMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.FixedExpenseMonthlyFactory.MonthlyValues;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월별 고정지출 내역 — 4.5 목록(lazy 생성 포함).
 *
 * <h2>조회가 쓰기를 일으킨다</h2>
 *
 * <p>그 연·월을 <b>처음 조회할 때</b> 설정에서 복사해 만든다(FR-406). GET 이 상태를 바꾸는
 * 셈이라 놀랄 수 있는데, 대안인 "등록 시 전 기간 생성"을 FR-402 가 명시적으로 금지한다 —
 * 적용 기간이 10년이면 등록 한 번에 120행이 생기고 그중 사용자가 실제로 여는 달은 몇
 * 개뿐이다. plan.md 의 Complexity Tracking 에 이 결정과 근거가 기록돼 있다.
 *
 * <p>생성을 일으키는 것은 <b>4.5·4.8·4.9 셋뿐</b>이다. 006 의 통계 조회는 일으키지 않으므로
 * 한 번도 열지 않은 달의 통계는 고정지출 합계가 0 으로 나온다.
 *
 * <h2>경합을 예외로 다루지 않는다</h2>
 *
 * <p>같은 달을 두 화면이 동시에 처음 여는 것은 <b>정상 경로</b>다. 애플리케이션의
 * "있으면 건너뛴다" 검사만 두면 두 트랜잭션이 같은 순간 "없음"을 보는 창이 열리고,
 * 하나가 유니크 위반으로 실패해 사용자 화면이 깨진다. {@code INSERT ... ON CONFLICT
 * DO NOTHING} 이 그 충돌을 <b>예외 없이 흡수</b>하고, 삽입 직후 다시 조회하면 어느 쪽
 * 요청이든 같은 1건을 본다(SC-403).
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/monthly-lifecycle.md">monthly-lifecycle.md §1·§2</a>
 */
@Service
public class FixedExpenseMonthlyService {

    private final UserFixedExpenseRepository fixedExpenseRepository;
    private final UserFixedExpenseMonthlyRepository monthlyRepository;
    private final FixedExpenseMonthlyFactory monthlyFactory;
    private final FixedExpenseMonthlyMapper monthlyMapper;
    private final ReferenceResolver referenceResolver;

    public FixedExpenseMonthlyService(UserFixedExpenseRepository fixedExpenseRepository,
                                      UserFixedExpenseMonthlyRepository monthlyRepository,
                                      FixedExpenseMonthlyFactory monthlyFactory,
                                      FixedExpenseMonthlyMapper monthlyMapper,
                                      ReferenceResolver referenceResolver) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.monthlyRepository = monthlyRepository;
        this.monthlyFactory = monthlyFactory;
        this.monthlyMapper = monthlyMapper;
        this.referenceResolver = referenceResolver;
    }

    /**
     * 4.5 목록.
     *
     * <p><b>순서가 규칙이다</b> — 생성을 <b>전부</b> 끝낸 뒤 필터로 결과를 좁힌다(FR-406).
     * 뒤집으면 {@code paymentMethodId=5} 로 그 달을 처음 열었을 때 수단 5 의 고정지출만
     * 만들어지고, 나중에 필터 없이 같은 달을 열면 나머지가 그때 생성된다. 그러면 같은
     * 달의 내역이 <b>"언제 어떤 필터로 처음 열었는가"에 따라 달라진다.</b>
     *
     * <p>{@code total} 은 <b>필터를 적용하지 않은</b> 그 달 전체 합계다 — 4.8 의
     * {@code expenseTotal} 과 같은 규칙이며, 합계는 화면 상단 요약이고 필터는 아래 목록을
     * 좁히는 도구이기 때문이다.
     */
    @Transactional
    public FixedExpenseMonthlyListResponse list(AuthPrincipal principal,
                                                FixedExpenseMonthlyListQuery query) {
        YearMonthValue yearMonth = query.yearMonth();

        // 1. 생성 — 필터와 무관하게 그 달 대상 전체를 만든다.
        ensureMonthlyRows(principal, yearMonth);

        // 2. 조회 — 삽입 직후 다시 읽는다. 경합이 있었어도 여기서는 같은 1건을 본다.
        List<UserFixedExpenseMonthly> rows = monthlyRepository.findByUserIdKeyAndYearAndMonth(
                principal.idKey(), yearMonth.year(), yearMonth.month());

        long total = rows.stream().mapToLong(UserFixedExpenseMonthly::getAmount).sum();

        // 3. 필터 — 여기서 처음 적용한다.
        List<FixedExpenseMonthlyResponse> items = rows.stream()
                .filter(row -> matches(row, query))
                .sorted(Comparator.comparing(UserFixedExpenseMonthly::getPaymentDate)
                        .thenComparing(row -> row.getFixedExpense().getIdx()))
                .map(monthlyMapper::toResponse)
                .toList();

        return new FixedExpenseMonthlyListResponse(
                items, yearMonth.year(), yearMonth.month(), total);
    }

    /**
     * 그 연·월에 걸리는 고정지출의 월별 내역을 <b>없는 것만</b> 만든다.
     *
     * <p>4.8(가계부 목록)과 4.9(재작성)도 이 메서드를 쓴다 — 세 API 가 같은 규칙으로
     * 만들어야 하고(FR-418), 각자 구현하면 같은 달이 어느 API 로 처음 열렸는지에 따라
     * 값이 갈린다.
     *
     * <p><b>참조의 사용 가능 여부를 묻지 않는다</b>(FR-426). 설정이 들고 있는 값을
     * 그대로 복사하며, 그 규칙은 {@link FixedExpenseMonthlyFactory} 가 지킨다.
     *
     * @return 실제로 새로 만들어진 행 수. 4.9 의 {@code createdCount} 가 이 값을 쓴다
     */
    @Transactional
    public int ensureMonthlyRows(AuthPrincipal principal, YearMonthValue yearMonth) {
        // 적용 기간에 걸리는 것만 고른다(FR-408). 기간 밖은 아예 대상이 아니다.
        List<UserFixedExpense> applicable = fixedExpenseRepository.findApplicableTo(
                principal.idKey(), yearMonth.value());

        int created = 0;
        for (UserFixedExpense setting : applicable) {
            MonthlyValues values = monthlyFactory.from(setting, yearMonth);
            // 이미 있으면 0행이다 — 예외가 아니다. 그래서 "있는지 먼저 확인"이 필요 없고,
            // 확인하지 않는 편이 오히려 안전하다(확인과 삽입 사이의 창이 사라진다).
            created += monthlyRepository.insertIfAbsent(
                    principal.idKey(),
                    setting.getIdx(),
                    yearMonth.year(),
                    yearMonth.month(),
                    values.amount(),
                    values.paymentDate(),
                    values.content(),
                    values.paymentMethodIdx(),
                    values.expendGroupIdx(),
                    principal.idKey());
        }
        return created;
    }

    /**
     * 4.6 단건 수정 — <b>판정 순서가 결과 코드를 바꾼다</b>(api-contract §6).
     *
     * <pre>{@code
     * 1. fixedExpenseId 로 설정 조회 (본인 소유?)  없음·타인 → 3402
     * 2. Path 의 year·month 범위                  오류    → 3403
     * 3. 그 연·월의 월별 내역이 있는가             없음    → 3405
     * 4. 값 검증                                          → 3401
     * 5. UPDATE + modified = true
     * }</pre>
     *
     * <p><b>1번이 가장 먼저인 이유</b>는 남의 설정의 존재가 코드 차이로 새어 나가는 것을
     * 막기 위해서다. 값 검증을 먼저 하면 "남의 설정 + 잘못된 값"이 {@code 3402} 가 아니라
     * {@code 3401} 로 나가 그 ID 가 실재함이 드러난다.
     *
     * <p><b>3번이 이 API 의 핵심 제약이다.</b> lazy 생성 모델의 대가로 열어 본 적 없는 달은
     * 고칠 수 없다 — 행이 없으니 UPDATE 할 대상이 없다. 사용자는 먼저 4.5 로 그 달을 열거나
     * 4.9 로 재작성한다.
     *
     * <p><b>수단은 사용 가능 여부까지 본다.</b> 자동 생성(4.5·4.8·4.9)이 죽은 참조를 그대로
     * 복사하는 것과 <b>방향이 반대</b>다(FR-426) — 여기는 사용자가 직접 고르는 경로라 죽은
     * 수단으로 갈아타는 것을 막아야 한다. 한쪽 규칙을 양쪽에 쓰면 반드시 한쪽이 틀린다.
     */
    @Transactional
    public FixedExpenseMonthlyResponse update(AuthPrincipal principal, Long fixedExpenseId,
                                              Integer year, Integer month,
                                              Map<String, Object> body) {
        // 1. 소유자 판정이 가장 먼저다.
        UserFixedExpense setting = fixedExpenseRepository
                .findByIdxAndUserIdKey(fixedExpenseId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_NOT_FOUND));

        // 2. Path 의 연·월 범위. 3405 보다 먼저다 — 범위 밖 값으로는 "행이 있는가"를
        //    물을 수조차 없다.
        YearMonthValue yearMonth =
                YearMonthValue.require(year, month, ErrorCode.FIXED_EXPENSE_MONTH_INVALID);

        // 3. 그 달 행이 있는가. lazy 생성 모델의 대가가 여기서 드러난다.
        UserFixedExpenseMonthly row = monthlyRepository
                .findByFixedExpenseIdxAndYearAndMonth(
                        setting.getIdx(), yearMonth.year(), yearMonth.month())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FIXED_EXPENSE_MONTHLY_NOT_CREATED));

        // 4. 값 검증. 대상 밖 필드는 9001, 빈 Body 는 3401 이다.
        PatchFields fields = FixedExpenseMonthlyUpdateRequest.of(body);
        applyChanges(principal, row, fields, yearMonth);

        // 5. 사용자가 직접 손댄 달임을 표시한다. 이 표시가 FR-412 의 자동 반영과
        //    FR-414 의 ③보존을 가르는 유일한 근거다.
        row.setModified(true);

        return monthlyMapper.toResponse(monthlyRepository.saveAndFlush(row));
    }

    /** 보낸 필드만 반영한다. omit 은 손대지 않는다. */
    private void applyChanges(AuthPrincipal principal, UserFixedExpenseMonthly row,
                              PatchFields fields, YearMonthValue yearMonth) {
        if (fields.has("amount")) {
            row.setAmount(requireAmount(fields));
        }
        if (fields.has("paymentDate")) {
            row.setPaymentDate(requirePaymentDate(fields, yearMonth));
        }
        if (fields.has("content")) {
            row.setContent(FixedExpenseFieldRules.requireContent(fields.string("content")));
        }
        if (fields.has("paymentMethodId")) {
            row.setPaymentMethod(requireExpenseMethod(principal, fields));
        }
    }

    private static long requireAmount(PatchFields fields) {
        if (fields.hasNonIntegerNumber("amount")) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "금액은 원 단위 정수여야 합니다.");
        }
        return FixedExpenseFieldRules.requireAmount(fields.longNumber("amount"));
    }

    /**
     * 결제일 — <b>Path 의 연·월과 같은 달이어야 한다</b>.
     *
     * <p>11월 행에 12월 날짜를 넣으면 그 행이 어느 달 것인지가 무너진다. 목록은
     * {@code year}·{@code month} 컬럼으로 고르는데 화면에 뜨는 날짜는 다른 달이 된다.
     */
    private static LocalDate requirePaymentDate(PatchFields fields, YearMonthValue yearMonth) {
        String raw = fields.string("paymentDate");
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(raw);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "결제일은 YYYY-MM-DD 형식이어야 합니다.");
        }
        if (!yearMonth.contains(parsed)) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "결제일은 %s 안의 날짜여야 합니다.".formatted(yearMonth));
        }
        return parsed;
    }

    /**
     * 새 수단 — 소유·사용 가능({@code 3003}) 다음 용도({@code 3401})다.
     *
     * <p>4.1 등록과 같은 조합이며 순서도 같다. 남의 수단은 존재를 감춰야 하므로
     * {@code 3003} 이고, 용도 불일치는 사용자가 자기 목록에서 고른 것이라 {@code 3401} 이다.
     */
    private UserPaymentMethod requireExpenseMethod(AuthPrincipal principal, PatchFields fields) {
        Long requestedId = fields.longNumber("paymentMethodId");
        if (requestedId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "paymentMethodId 은(는) 비울 수 없습니다.");
        }
        UserPaymentMethod method =
                referenceResolver.requireOwnedUsablePaymentMethod(principal, requestedId);
        return referenceResolver.requirePurpose(method, UserPaymentMethod.PURPOSE_EXPENSE,
                ErrorCode.FIXED_EXPENSE_FIELD_INVALID);
    }

    /** 두 필터를 적용한다. 걸리지 않은 필터는 통과다. */
    private static boolean matches(UserFixedExpenseMonthly row,
                                   FixedExpenseMonthlyListQuery query) {
        if (query.paymentMethodId() != null
                && !query.paymentMethodId().equals(row.getPaymentMethod().getIdx())) {
            return false;
        }
        return query.expendGroupId() == null
                || query.expendGroupId().equals(row.getExpendGroup().getIdx());
    }
}
