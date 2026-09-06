package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.response.ExcelImportResponse;
import com.dbdomino.moneylog.backend.dto.response.ExcelRowError;
import com.dbdomino.moneylog.backend.excel.ExcelColumn;
import com.dbdomino.moneylog.backend.excel.ExcelRow;
import com.dbdomino.moneylog.backend.excel.ExcelRowReader;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.DetailedBusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserIncome;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 3.12 엑셀 일괄 업로드.
 *
 * <h2>3단계다 — 검증을 저장보다 완전히 앞에 끝낸다</h2>
 *
 * <pre>{@code
 * [1] 파일 단위 (행을 읽기 전)            FR-319
 *     ├ .xlsx 가 아님        → 3503
 *     ├ 데이터 행 > 300      → 3504
 *     └ 데이터 행 == 0       → 3505
 *
 * [2] 행 단위 — 전 행을 읽고 전부 검증한다
 *     └ 오류가 하나라도 있으면 errors[] 를 모아 3502. 아무것도 저장하지 않는다
 *
 * [3] 저장 — 2단계를 전부 통과했을 때만. 한 트랜잭션
 * }</pre>
 *
 * <p><b>저장하다 실패해서 롤백하는 것이 아니다.</b> 그 순서면 첫 실패에서 멈춰
 * {@code errors[]} 에 하나밖에 담을 수 없고, 사용자가 파일을 여러 번 왕복하며 고치게 된다.
 * 300행 상한이 있으므로 전 행을 검증해도 비용이 크지 않다(research.md §8).
 *
 * <p><b>파일 단위와 행 단위의 코드를 나눈 이유</b>는 프론트가 안내를 다르게 내야 해서다 —
 * {@code 3503}·{@code 3504}·{@code 3505} 는 "파일을 다시 고르세요"이고 {@code 3502} 는
 * "표의 N행 M열을 고치세요"다. 한 코드로 묶으면 프론트가 {@code errors[]} 의 유무로
 * 분기해야 하는데, 그건 계약이 아니라 추측이다.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/excel-contract.md">excel-contract.md</a>
 */
@Service
public class ExcelImportService {

    /** 헤더를 제외한 데이터 행의 상한(FR-319). */
    private static final int MAX_DATA_ROWS = 300;

    private final ExcelRowReader rowReader;
    private final ReferenceResolver referenceResolver;
    private final UserExpenseRepository expenseRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public ExcelImportService(ExcelRowReader rowReader,
                              ReferenceResolver referenceResolver,
                              UserExpenseRepository expenseRepository,
                              UserIncomeRepository incomeRepository,
                              UserRepository userRepository) {
        this.rowReader = rowReader;
        this.referenceResolver = referenceResolver;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
    }

    /** 검증을 통과한 한 행. 저장 단계가 이것만 본다. */
    private record ValidRow(String kind, LocalDate paymentDate, long amount,
                            UserPaymentMethod paymentMethod, UserExpendGroup expendGroup,
                            String place, String content) {
    }

    /**
     * 업로드된 파일을 읽어 지출·소득으로 저장한다.
     *
     * <p>업로드가 만드는 행의 규칙은 <b>3.1·3.7 과 완전히 같다</b>(excel-contract.md §4) —
     * 이름 스냅샷, 사용 중 참조만 허용, 소유자는 토큰, 중복 허용. 엑셀이라고 예외를 두지
     * 않는다.
     */
    @Transactional
    public ExcelImportResponse importFile(AuthPrincipal principal, MultipartFile file) {
        List<ExcelRow> rows = readRows(file);
        requireRowCountInRange(rows.size());

        List<ExcelRowError> errors = new ArrayList<>();
        List<ValidRow> valid = new ArrayList<>(rows.size());
        for (ExcelRow row : rows) {
            validate(principal, row, errors).ifPresent(valid::add);
        }
        if (!errors.isEmpty()) {
            // 전체 롤백이다. 저장을 시작조차 하지 않았으므로 되돌릴 것도 없다.
            throw new DetailedBusinessException(ErrorCode.EXCEL_ROW_VALIDATION_FAILED,
                    ExcelImportResponse.failed(errors));
        }

        return save(principal, valid);
    }

    // ── [1] 파일 단위 ──────────────────────────────────────────────────────

    /** 읽을 수 없으면 {@code 3503} 이다. 확장자가 아니라 <b>내용</b>으로 판정한다. */
    private List<ExcelRow> readRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_EMPTY);
        }
        try (InputStream input = file.getInputStream()) {
            return rowReader.read(input);
        } catch (IOException | ExcelRowReader.NotXlsxException e) {
            throw new BusinessException(ErrorCode.EXCEL_FORMAT_NOT_XLSX);
        }
    }

    /**
     * 데이터 행 수를 본다. <b>행 검증에 들어가기 전에</b> 판정한다(FR-319).
     *
     * <p>300행 초과 파일에 행 오류가 섞여 있어도 {@code 3502} 가 아니라 {@code 3504} 가
     * 나와야 한다 — 사용자가 할 일은 표를 고치는 것이 아니라 파일을 나누는 것이다.
     */
    private static void requireRowCountInRange(int dataRows) {
        if (dataRows > MAX_DATA_ROWS) {
            throw new BusinessException(ErrorCode.EXCEL_ROW_LIMIT_EXCEEDED);
        }
        if (dataRows == 0) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_EMPTY);
        }
    }

    // ── [2] 행 단위 ────────────────────────────────────────────────────────

    /**
     * 한 행을 검증한다. <b>예외를 던지지 않고</b> {@code errors} 에 쌓는다.
     *
     * <p>첫 오류에서 멈추면 사용자가 파일을 여러 번 왕복하게 된다(FR-320). 한 행 안에서도
     * 여러 칸이 잘못될 수 있으므로 <b>칸마다</b> 담는다.
     *
     * @return 통과하면 저장할 값, 하나라도 걸리면 빈 값
     */
    private Optional<ValidRow> validate(AuthPrincipal principal, ExcelRow row,
                                        List<ExcelRowError> errors) {
        int before = errors.size();
        String kind = row.kind();
        if (!ExcelColumn.KIND_EXPENSE.equals(kind) && !ExcelColumn.KIND_INCOME.equals(kind)) {
            add(errors, row, ExcelColumn.KIND, "구분은 EXPENSE 또는 INCOME 이어야 합니다.");
            // 구분을 모르면 나머지 열의 필수 여부를 정할 수 없다. 이 행은 여기서 끝낸다.
            return Optional.empty();
        }

        requireNotBlank(row, kind, errors);
        rejectFilledExpenseOnlyColumns(row, kind, errors);

        LocalDate paymentDate = parseDate(row, errors);
        Long amount = parseAmount(row, errors);
        UserPaymentMethod paymentMethod = resolvePaymentMethod(principal, row, kind, errors);
        UserExpendGroup expendGroup = resolveExpendGroup(principal, row, kind, errors);
        String place = trimmedOrNull(row, ExcelColumn.PLACE, ExcelColumn.PLACE.isRequiredFor(kind),
                LedgerFieldRules.PLACE_MAX, errors);
        String content = trimmedOrNull(row, ExcelColumn.CONTENT,
                ExcelColumn.CONTENT.isRequiredFor(kind), LedgerFieldRules.CONTENT_MAX, errors);

        if (errors.size() != before) {
            return Optional.empty();
        }
        return Optional.of(new ValidRow(kind, paymentDate, amount, paymentMethod, expendGroup,
                place, content));
    }

    /** 그 구분에서 필수인 열이 비어 있는가. */
    private static void requireNotBlank(ExcelRow row, String kind, List<ExcelRowError> errors) {
        for (ExcelColumn column : ExcelColumn.values()) {
            if (column.isRequiredFor(kind) && row.isBlank(column)) {
                add(errors, row, column, column.header() + "은(는) 비울 수 없습니다.");
            }
        }
    }

    /**
     * 소득 행에 지출 전용 열이 채워져 있는가 — 채워져 있으면 오류다(FR-306).
     *
     * <p>{@code tbl_income} 에 그 컬럼이 <b>없어</b> 저장할 자리가 없는데, 조용히 버리면
     * 사용자는 저장됐다고 믿는다.
     */
    private static void rejectFilledExpenseOnlyColumns(ExcelRow row, String kind,
                                                       List<ExcelRowError> errors) {
        for (ExcelColumn column : ExcelColumn.values()) {
            if (column.mustBeBlankFor(kind) && !row.isBlank(column)) {
                add(errors, row, column, "소득 행에는 " + column.header() + "을(를) 적을 수 없습니다.");
            }
        }
    }

    private static LocalDate parseDate(ExcelRow row, List<ExcelRowError> errors) {
        if (row.isBlank(ExcelColumn.PAYMENT_DATE)) {
            return null;
        }
        try {
            return LocalDate.parse(row.get(ExcelColumn.PAYMENT_DATE).trim());
        } catch (RuntimeException e) {
            add(errors, row, ExcelColumn.PAYMENT_DATE, "날짜는 YYYY-MM-DD 형식이어야 합니다.");
            return null;
        }
    }

    private static Long parseAmount(ExcelRow row, List<ExcelRowError> errors) {
        if (row.isBlank(ExcelColumn.AMOUNT)) {
            return null;
        }
        try {
            long amount = Long.parseLong(row.get(ExcelColumn.AMOUNT).trim());
            if (amount <= 0L) {
                add(errors, row, ExcelColumn.AMOUNT, "금액은 0보다 커야 합니다.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            // 소수점·문자·BIGINT 범위 초과가 전부 여기로 온다 — 사용자가 할 조치가 같다.
            add(errors, row, ExcelColumn.AMOUNT, "금액은 원 단위 정수여야 합니다.");
            return null;
        }
    }

    /**
     * 수단을 <b>이름으로</b> 찾는다. 양식이 드롭다운으로 이름을 넣게 해 파일에 ID 가 없다.
     *
     * <p><b>같은 이름이 둘 이상이면 오류다</b>(excel-contract.md § 정한 것). 첫 매치를
     * 고르지 않는 이유는 어느 쪽이 "첫"인지가 {@code idx} 순서라는 내부 사정이 정하는데
     * 사용자는 그것을 볼 수 없어서다 — 금액이 붙는 자리를 서버가 임의로 고르는 셈이 된다.
     */
    private UserPaymentMethod resolvePaymentMethod(AuthPrincipal principal, ExcelRow row,
                                                   String kind, List<ExcelRowError> errors) {
        if (row.isBlank(ExcelColumn.PAYMENT_METHOD)) {
            return null;
        }
        String name = row.get(ExcelColumn.PAYMENT_METHOD);
        String purpose = ExcelColumn.KIND_EXPENSE.equals(kind)
                ? UserPaymentMethod.PURPOSE_EXPENSE
                : UserPaymentMethod.PURPOSE_INCOME;
        List<UserPaymentMethod> matches =
                referenceResolver.findUsablePaymentMethodsByName(principal, name, purpose);

        if (matches.isEmpty()) {
            add(errors, row, ExcelColumn.PAYMENT_METHOD,
                    "사용 중인 " + (ExcelColumn.KIND_EXPENSE.equals(kind) ? "지출" : "소득")
                            + " 수단에 '" + name + "'이(가) 없습니다.");
            return null;
        }
        if (matches.size() > 1) {
            add(errors, row, ExcelColumn.PAYMENT_METHOD,
                    "'" + name + "' 이름의 수단이 " + matches.size()
                            + "개라 어느 것인지 정할 수 없습니다. 이름을 구분되게 바꿔 주세요.");
            return null;
        }
        return matches.get(0);
    }

    /** 지출유형을 이름으로 찾는다. 소득 행은 이 열이 비어 있어야 하므로 건너뛴다. */
    private UserExpendGroup resolveExpendGroup(AuthPrincipal principal, ExcelRow row,
                                               String kind, List<ExcelRowError> errors) {
        if (!ExcelColumn.KIND_EXPENSE.equals(kind) || row.isBlank(ExcelColumn.EXPEND_GROUP)) {
            return null;
        }
        String name = row.get(ExcelColumn.EXPEND_GROUP);
        Optional<UserExpendGroup> found =
                referenceResolver.findUsableExpendGroupByName(principal, name);
        if (found.isEmpty()) {
            add(errors, row, ExcelColumn.EXPEND_GROUP,
                    "사용 중인 지출유형에 '" + name + "'이(가) 없습니다.");
        }
        return found.orElse(null);
    }

    /** 길이를 보고 다듬는다. 비어 있는데 필수가 아니면 {@code null} 이다. */
    private static String trimmedOrNull(ExcelRow row, ExcelColumn column, boolean required,
                                        int max, List<ExcelRowError> errors) {
        if (row.isBlank(column)) {
            // 비었는데 필수인 경우는 requireNotBlank 가 이미 담았다.
            return null;
        }
        String value = row.get(column).trim();
        if (value.length() > max) {
            add(errors, row, column, column.header() + "은(는) " + max + "자를 넘을 수 없습니다.");
            return null;
        }
        return value;
    }

    private static void add(List<ExcelRowError> errors, ExcelRow row, ExcelColumn column,
                            String message) {
        errors.add(new ExcelRowError(row.rowNumber(), column.letter(), message));
    }

    // ── [3] 저장 ───────────────────────────────────────────────────────────

    /**
     * 검증을 통과한 행을 저장한다. 규칙은 3.1·3.7 과 같다.
     *
     * <p>업로드가 만드는 지출 행의 할부 3컬럼은 <b>전부 NULL</b> 이다 — 양식에 할부 열이
     * 없다(api-contract.md §8).
     *
     * <p><b>중복을 허용한다</b>(FR-309). 같은 내용의 행이 두 번 있으면 둘 다 저장한다 —
     * 실제로 같은 날 같은 금액을 두 번 쓸 수 있다.
     */
    private ExcelImportResponse save(AuthPrincipal principal, List<ValidRow> rows) {
        User owner = userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        List<UserExpense> expenses = new ArrayList<>();
        List<UserIncome> incomes = new ArrayList<>();
        for (ValidRow row : rows) {
            if (ExcelColumn.KIND_EXPENSE.equals(row.kind())) {
                expenses.add(toExpense(owner, row));
            } else {
                incomes.add(toIncome(owner, row));
            }
        }
        expenseRepository.saveAll(expenses);
        incomeRepository.saveAll(incomes);

        return ExcelImportResponse.succeeded(expenses.size(), incomes.size());
    }

    private static UserExpense toExpense(User owner, ValidRow row) {
        UserExpense expense = new UserExpense();
        expense.setUser(owner);
        expense.setAmount(row.amount());
        expense.setPaymentDate(row.paymentDate());
        expense.setPlace(row.place());
        expense.setContent(row.content());
        expense.setPaymentMethod(row.paymentMethod());
        expense.setPaymentMethodName(row.paymentMethod().getName());
        expense.setExpendGroup(row.expendGroup());
        expense.setExpendGroupName(row.expendGroup().getName());
        InstallmentColumns.markAsLumpSum(expense);
        return expense;
    }

    private static UserIncome toIncome(User owner, ValidRow row) {
        UserIncome income = new UserIncome();
        income.setUser(owner);
        income.setAmount(row.amount());
        income.setPaymentDate(row.paymentDate());
        income.setContent(row.content());
        income.setPaymentMethod(row.paymentMethod());
        income.setPaymentMethodName(row.paymentMethod().getName());
        return income;
    }
}
