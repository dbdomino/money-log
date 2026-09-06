package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.request.PaymentMethodCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodListResponse;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodResponse;
import com.dbdomino.moneylog.backend.mapper.PaymentMethodMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출·소득 수단 관리 — 등록(2.1)·관리 목록(2.2)·상세(2.3)·수정(2.4)·삭제 표시(2.5).
 *
 * <h2>소유자는 토큰이 정한다</h2>
 *
 * <p>모든 메서드가 {@link AuthPrincipal#idKey()} 로만 회원을 얻는다. 요청 Body 나 경로가
 * 소유자를 지정할 방법이 없다(FR-201). "본인 데이터만 접근한다"를 뒤늦은 검사로 지키면 한
 * 곳만 빠뜨려도 뚫리므로, 조회 자체를 {@code idx + id_key} 로 건다.
 *
 * <p>그래서 <b>없는 수단과 남의 수단이 같은 {@code 3003}</b> 이 된다. 코드를 갈라 두면
 * ID 를 훑는 것만으로 남의 자원이 존재한다는 사실이 새어 나간다.
 *
 * <h2>값 검증을 Bean Validation 에 맡기지 않는다</h2>
 *
 * <p>{@code type}·{@code purpose} 가 허용 값 밖이면 {@code 3001}, {@code cardExpiry} 형식이
 * 어긋나면 {@code 3002} 다. Bean Validation 실패는 전역 처리에서 전부 {@code 9001} 로 나가므로
 * 여기서 검사해야 명세의 코드가 나온다. 002 의 가입이 비밀번호 규칙({@code 2004})을 같은
 * 이유로 서비스에 둔 것과 같은 판단이다.
 *
 * @see <a href="../../../../../../../../specs/003-backend-payment-expend-group/contracts/api-contract.md">api-contract.md §5</a>
 */
@Service
public class PaymentMethodService {

    /** 2.4 가 수정할 수 있는 필드. 그 밖의 이름이 오면 {@code 9001} 이다. */
    private static final Set<String> UPDATABLE_FIELDS =
            Set.of("name", "type", "purpose", "inUse", "cardExpiry");

    /**
     * 카드 유효기간 {@code YYYY-MM}.
     *
     * <p><b>월을 {@code 01}~{@code 12} 로 좁힌다.</b> {@code \d{2}} 로 두면 {@code 2028-13}
     * 이 통과하는데, 컬럼이 {@code CHAR(7)} 이라 길이 검사로는 걸러지지 않아 그대로 저장된다.
     */
    private static final Pattern CARD_EXPIRY = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final UserExpenseRepository expenseRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserFixedExpenseRepository fixedExpenseRepository;
    private final UserFixedExpenseMonthlyRepository fixedExpenseMonthlyRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    public PaymentMethodService(UserPaymentMethodRepository paymentMethodRepository,
                                UserRepository userRepository,
                                UserExpenseRepository expenseRepository,
                                UserIncomeRepository incomeRepository,
                                UserFixedExpenseRepository fixedExpenseRepository,
                                UserFixedExpenseMonthlyRepository fixedExpenseMonthlyRepository,
                                PaymentMethodMapper paymentMethodMapper) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.fixedExpenseMonthlyRepository = fixedExpenseMonthlyRepository;
        this.paymentMethodMapper = paymentMethodMapper;
    }

    /**
     * 2.1 수단 등록.
     *
     * <p>{@code type=ACCOUNT} 면 {@code cardExpiry} 를 <b>보냈더라도 버린다</b>(FR-204).
     * 거절하지 않고 무시하는 것은 명세가 그렇게 정했기 때문이다 — 계좌에 유효기간이 있다는
     * 상태 자체를 만들지 않는 것이 목적이고, 화면이 종류를 바꿔 가며 입력하는 흐름에서
     * 남은 값을 매번 지우게 하면 실수로 거절만 늘어난다.
     */
    @Transactional
    public PaymentMethodResponse create(AuthPrincipal principal, PaymentMethodCreateRequest request) {
        String type = requireType(request.type());
        String purpose = requirePurpose(request.purpose());
        String cardExpiry = normalizeCardExpiry(type, request.cardExpiry());

        UserPaymentMethod method = new UserPaymentMethod();
        method.setUser(ownerOf(principal));
        method.setName(request.name().trim());
        method.setType(type);
        method.setPurpose(purpose);
        method.setInUse(request.inUse());
        method.setCardExpiry(cardExpiry);
        method.setDeleted(false);

        return paymentMethodMapper.toResponse(paymentMethodRepository.save(method));
    }

    /**
     * 2.2 관리 목록. <b>삭제 표시된 수단까지 전부</b> 돌려준다(FR-207).
     *
     * <p>페이징을 두지 않는다(FR-217) — 본인 보유 수단은 수가 제한적이다.
     */
    @Transactional(readOnly = true)
    public PaymentMethodListResponse list(AuthPrincipal principal) {
        return new PaymentMethodListResponse(paymentMethodMapper.toResponses(
                paymentMethodRepository.findByUserIdKeyOrderByIdxAsc(principal.idKey())));
    }

    /** 2.3 상세 조회. 삭제 표시된 수단도 보인다 — 관리 화면이 읽는다. */
    @Transactional(readOnly = true)
    public PaymentMethodResponse get(AuthPrincipal principal, Long paymentMethodId) {
        return paymentMethodMapper.toResponse(findOwned(principal, paymentMethodId));
    }

    /**
     * 2.4 수단 수정. 판정 순서를 api-contract.md §5 그대로 따른다.
     *
     * <pre>{@code
     * 1. 대상 조회(본인 소유?)            없음·타인 → 3003
     * 2. 보낸 필드의 값 검증              type·purpose → 3001 / cardExpiry → 3002
     * 3. purpose 를 실제로 보냈고 값이 바뀌는가 → 참조 검사 → 3005
     * 4. UPDATE
     * }</pre>
     *
     * <p>순서를 바꾸면 코드가 달라진다. 참조 검사를 먼저 하면 남의 수단에 대해서도
     * {@code 3005} 가 나가 그 수단에 사용 내역이 있다는 사실이 새어 나간다.
     *
     * <p><b>삭제 표시된 수단도 수정된다.</b> 삭제는 읽기 전용이 아니다 — 이름 정리처럼
     * 지운 뒤에 해야 하는 일이 있다.
     */
    @Transactional
    public PaymentMethodResponse update(AuthPrincipal principal, Long paymentMethodId,
                                        Map<String, Object> body) {
        UserPaymentMethod method = findOwned(principal, paymentMethodId);
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);

        String type = fields.has("type") ? requireType(fields.string("type")) : method.getType();
        String purpose = fields.has("purpose")
                ? requirePurpose(fields.string("purpose"))
                : method.getPurpose();

        // cardExpiry 는 보내지 않았어도 type 이 ACCOUNT 로 바뀌면 비워야 한다 —
        // 카드에서 계좌로 바꾼 수단에 옛 유효기간이 남아 있으면 안 된다(FR-204).
        String cardExpiry = normalizeCardExpiry(type,
                fields.has("cardExpiry") ? fields.string("cardExpiry") : method.getCardExpiry());

        if (fields.has("purpose") && !purpose.equals(method.getPurpose())) {
            requireNoReferences(method.getIdx());
        }

        if (fields.has("name")) {
            method.setName(requireName(fields.string("name")));
        }
        if (fields.has("inUse")) {
            method.setInUse(requireInUse(fields.bool("inUse")));
        }
        method.setType(type);
        method.setPurpose(purpose);
        method.setCardExpiry(cardExpiry);

        return paymentMethodMapper.toResponse(method);
    }

    /**
     * 2.5 삭제 — <b>표시만 한다</b>(FR-206). 행은 남는다.
     *
     * <p>이미 {@code deleted=true} 면 {@code 3004} 로 거절한다. 멱등 성공으로 흘리면 화면이
     * "방금 지웠다"와 "이미 지워져 있었다"를 구분할 수 없다.
     */
    @Transactional
    public PaymentMethodDeleteResponse delete(AuthPrincipal principal, Long paymentMethodId) {
        UserPaymentMethod method = findOwned(principal, paymentMethodId);
        if (Boolean.TRUE.equals(method.getDeleted())) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_ALREADY_DELETED);
        }
        method.setDeleted(true);
        return new PaymentMethodDeleteResponse(method.getIdx(), true, "결제 수단이 삭제되었습니다");
    }

    /**
     * 본인 소유 수단 1건. 없거나 남의 것이면 {@code 3003} 이다.
     *
     * <p>조회 조건에 {@code id_key} 를 <b>함께 건다.</b> 먼저 꺼내 놓고 소유자를 비교하는
     * 방식은 비교를 빠뜨린 자리가 곧 구멍이 된다.
     */
    private UserPaymentMethod findOwned(AuthPrincipal principal, Long paymentMethodId) {
        return paymentMethodRepository.findByIdxAndUserIdKey(paymentMethodId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
    }

    /** 감사 컬럼과 FK 에 쓸 소유 회원. 토큰이 가리키는 회원이 사라졌다면 인증 자체가 이상한 것이다. */
    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * {@code purpose} 를 바꿔도 되는가. <b>4개 테이블을 전부 본다</b>(FR-205).
     *
     * <p>하나라도 빠뜨리면 "소득 수단으로 낸 지출"이 만들어져 월별 집계와 통계의 수단별
     * 요약이 어긋난다. DB CHECK 으로는 대신할 수 없다 — {@code purpose} 는 수단 쪽 컬럼이라
     * 다른 테이블의 참조 건수를 볼 수 없다.
     */
    private void requireNoReferences(Long paymentMethodId) {
        boolean referenced = expenseRepository.existsByPaymentMethodIdx(paymentMethodId)
                || incomeRepository.existsByPaymentMethodIdx(paymentMethodId)
                || fixedExpenseRepository.existsByPaymentMethodIdx(paymentMethodId)
                || fixedExpenseMonthlyRepository.existsByPaymentMethodIdx(paymentMethodId);
        if (referenced) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_PURPOSE_LOCKED);
        }
    }

    private static String requireType(String type) {
        if (UserPaymentMethod.TYPE_CARD.equals(type) || UserPaymentMethod.TYPE_ACCOUNT.equals(type)) {
            return type;
        }
        throw new BusinessException(ErrorCode.PAYMENT_METHOD_TYPE_INVALID);
    }

    private static String requirePurpose(String purpose) {
        if (UserPaymentMethod.PURPOSE_EXPENSE.equals(purpose)
                || UserPaymentMethod.PURPOSE_INCOME.equals(purpose)) {
            return purpose;
        }
        throw new BusinessException(ErrorCode.PAYMENT_METHOD_TYPE_INVALID);
    }

    /** PATCH 로 온 이름. {@code null}·빈 값은 저장할 수 없다 — 컬럼이 NOT NULL 이다. */
    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "수단 이름은 비울 수 없습니다.");
        }
        return name.trim();
    }

    /** PATCH 로 온 사용 여부. {@code null} 을 저장할 수 없다 — 컬럼이 NOT NULL 이다. */
    private static Boolean requireInUse(Boolean inUse) {
        if (inUse == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "inUse 는 비울 수 없습니다.");
        }
        return inUse;
    }

    /**
     * 저장할 {@code cardExpiry}. 계좌면 {@code null}, 카드면 {@code YYYY-MM} 이어야 한다.
     *
     * <p>계좌일 때 값을 <b>거절하지 않고 버린다</b>(FR-204) — 형식 검사보다 먼저 판정하므로
     * 계좌에 {@code "2028/12"} 를 실어 보내도 {@code 3002} 가 아니라 그냥 무시된다.
     */
    private static String normalizeCardExpiry(String type, String cardExpiry) {
        if (UserPaymentMethod.TYPE_ACCOUNT.equals(type)) {
            return null;
        }
        if (cardExpiry == null || cardExpiry.isBlank()) {
            return null;
        }
        String value = cardExpiry.trim();
        if (!CARD_EXPIRY.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_EXPIRY_INVALID);
        }
        return value;
    }
}
