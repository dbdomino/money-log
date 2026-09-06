package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.service.FixedExpenseMonthlyFactory.MonthlyValues;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설정을 월별 내역에 <b>반영</b>한다 — 자동(FR-412)과 수동 재작성(4.9, FR-414) 둘 다.
 *
 * <h2>왜 한 클래스인가</h2>
 *
 * <p>둘 다 "설정의 값을 그 달 행에 옮겨 담는다"는 같은 일이고, <b>대상 범위만</b> 다르다.
 *
 * <table border="1">
 *   <caption>자동과 수동</caption>
 *   <tr><th></th><th>자동 반영 (4.4 가 부른다)</th><th>재작성 (4.9)</th></tr>
 *   <tr><td>대상</td><td>미래 달 + {@code modified=false} 만</td><td>지정한 <b>한 연·월</b> 전부</td></tr>
 *   <tr><td>지난 달</td><td>건드리지 않는다</td><td>대상이 될 수 있다</td></tr>
 *   <tr><td>직접 고친 달</td><td>건드리지 않는다</td><td>{@code overwriteModified} 로 덮을 수 있다</td></tr>
 *   <tr><td>삭제</td><td><b>하지 않는다</b></td><td>기간 밖 행을 지운다(④)</td></tr>
 * </table>
 *
 * <p>{@code FixedExpenseService} 에 자동 반영을 두면 설정 CRUD 가 다른 테이블을 쓰게 되어
 * 두 서비스의 경계가 흐려진다.
 *
 * <h2>자동과 수동의 경계</h2>
 *
 * <p>자동 반영은 <b>값 갱신만</b> 하고 삭제하지 않는다. 그래서 적용 기간을 줄이면
 * (예: 종료를 2027-02 → 2026-12) 이미 만들어진 2027-01·02 행이 기간 밖으로 남는다.
 * 그 정리는 4.9 의 ④가 맡는다 — 사용자가 명시적으로 부르는 경로다.
 *
 * <p><b>재작성(4.9)은 US4 에서 만든다.</b> 지금은 자동 반영만 있다.
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/monthly-lifecycle.md">monthly-lifecycle.md §3</a>
 */
@Service
public class FixedExpenseSyncService {

    private final UserFixedExpenseMonthlyRepository monthlyRepository;
    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserExpendGroupRepository expendGroupRepository;
    private final FixedExpenseMonthlyFactory monthlyFactory;

    public FixedExpenseSyncService(UserFixedExpenseMonthlyRepository monthlyRepository,
                                   UserPaymentMethodRepository paymentMethodRepository,
                                   UserExpendGroupRepository expendGroupRepository,
                                   FixedExpenseMonthlyFactory monthlyFactory) {
        this.monthlyRepository = monthlyRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.expendGroupRepository = expendGroupRepository;
        this.monthlyFactory = monthlyFactory;
    }

    /**
     * 4.4 수정의 자동 반영 — <b>미래 달이면서 {@code modified=false} 인 행만</b>(FR-412).
     *
     * <h2>이번 달을 포함하지 않는다</h2>
     *
     * <p>판정이 넷으로 갈린다.
     *
     * <table border="1">
     *   <caption>설정을 고쳤을 때 어느 달이 따라가나</caption>
     *   <tr><th>대상</th><th>갱신</th><th>이유</th></tr>
     *   <tr><td>지난 달</td><td>안 한다</td><td>이미 일어난 일이다</td></tr>
     *   <tr><td><b>이번 달</b></td><td><b>안 한다</b></td><td>진행 중이고 사용자가 이미 본 숫자다</td></tr>
     *   <tr><td>미래 달 + {@code modified=false}</td><td>한다</td><td>—</td></tr>
     *   <tr><td>미래 달 + {@code modified=true}</td><td>안 한다</td><td>사용자가 직접 손댄 달이다</td></tr>
     * </table>
     *
     * <p><b>이번 달을 넣으면 월세를 올렸을 때 이번 달 가계부 금액이 소급해 바뀐다.</b>
     * 004 의 중도상환 경계({@code > today})와 같은 성격의 결정이며, "사용자가 이미 본
     * 숫자를 바꾸지 않는다"가 그 규칙이다.
     *
     * <h2>기간 밖으로 나간 행은 어떻게 되나</h2>
     *
     * <p><b>그대로 남는다.</b> 적용 기간을 줄여도 자동 반영은 값만 갱신하고 삭제하지
     * 않는다. 다만 <b>기간 밖 행의 값은 건드리지 않는다</b> — 이미 설정의 대상이 아닌
     * 달이라 새 값을 씌울 근거가 없다. 정리는 4.9 의 ④가 한다.
     *
     * <p>월별 내역이 하나도 없으면(설정만 만들고 아무 달도 열지 않은 상태) 아무 일도
     * 하지 않는다 — US2 이전에는 항상 이 경우다.
     *
     * @param setting 이미 새 값으로 갱신된 설정
     * @return 실제로 갱신한 행 수. 호출자가 쓰지 않아도 로그·시험에 쓸모가 있다
     */
    @Transactional
    public int propagate(UserFixedExpense setting) {
        YearMonthValue currentMonth = YearMonthValue.now();
        List<UserFixedExpenseMonthly> targets = monthlyRepository.findFutureUnmodified(
                setting.getIdx(), currentMonth.value());

        YearMonthValue start = YearMonthValue.of(setting.getStartYear(), setting.getStartMonth());
        YearMonthValue end = YearMonthValue.of(setting.getEndYear(), setting.getEndMonth());

        int updated = 0;
        for (UserFixedExpenseMonthly row : targets) {
            YearMonthValue rowMonth = YearMonthValue.of(row.getYear(), row.getMonth());
            if (!rowMonth.isWithin(start, end)) {
                // 기간을 줄여 이 달이 밖으로 나갔다. 값을 씌우지 않고 그대로 둔다 —
                // 지우는 것은 4.9 의 ④가 명시적으로 할 일이다.
                continue;
            }
            apply(row, setting, rowMonth);
            updated++;
        }
        if (updated > 0) {
            monthlyRepository.saveAll(targets);
        }
        return updated;
    }

    /**
     * 설정값을 그 달 행에 옮겨 담는다.
     *
     * <p><b>{@code modified} 를 건드리지 않는다.</b> 여기 오는 행은 이미
     * {@code modified=false} 이고, 자동 반영이 그 표시를 세우면 다음 반영이 이 행을
     * 건너뛰게 된다.
     *
     * <p>값은 {@link FixedExpenseMonthlyFactory} 가 만든다 — 생성(4.5·4.8)과 재작성(4.9)이
     * 쓰는 것과 <b>같은 규칙</b>이어야 하기 때문이다. 특히 말일 보정을 여기서 따로 계산하면
     * 결제일 31 인 고정지출의 2월 값이 경로마다 달라진다.
     */
    private void apply(UserFixedExpenseMonthly row, UserFixedExpense setting,
                       YearMonthValue yearMonth) {
        MonthlyValues values = monthlyFactory.from(setting, yearMonth);
        row.setAmount(values.amount());
        row.setPaymentDate(values.paymentDate());
        row.setContent(values.content());
        // 참조는 식별자로 다시 로드한다. 설정의 연관 객체를 그대로 꽂으면 두 Entity 가
        // 같은 프록시를 공유해, 한쪽에서 참조를 바꿨을 때 다른 쪽까지 따라 움직인다.
        row.setPaymentMethod(paymentMethodRepository.getReferenceById(values.paymentMethodIdx()));
        row.setExpendGroup(expendGroupRepository.getReferenceById(values.expendGroupIdx()));
    }
}
