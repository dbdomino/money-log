package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.service.FixedExpenseMonthlyFactory.MonthlyValues;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * <h2>손댄 행만 저장한다</h2>
 *
 * <p>두 메서드 다 <b>실제로 값을 바꾼 행만</b> 모아 {@code saveAll} 에 넘긴다. 조회한
 * 목록을 통째로 넘기지 않는 이유가 있다 — {@link #rewrite} 는 같은 트랜잭션에서
 * <b>삭제도</b> 하는데, Spring Data 의 파생 삭제 쿼리({@code deleteBy...})는 벌크
 * DELETE 가 아니라 "조회 후 {@code em.remove}" 라 그 행이 영속성 컨텍스트에서
 * {@code removed} 상태가 된다. 그 인스턴스가 {@code save} 로 들어가면 Hibernate 가
 * {@code ObjectDeletedException: deleted instance passed to merge} 를 던지고
 * 사용자에게는 {@code 9000} 이 나간다.
 *
 * <p>한 달에 <b>④삭제와 ②갱신이 함께</b> 일어날 때만 터지므로 둘 중 하나만 있는 시험으로는
 * 드러나지 않는다({@code SyncRewriteIT#deleteAndUpdateInTheSameMonth} 가 그 조합을 건다).
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/monthly-lifecycle.md">monthly-lifecycle.md §3</a>
 */
@Service
public class FixedExpenseSyncService {

    private final UserFixedExpenseMonthlyRepository monthlyRepository;
    private final UserFixedExpenseRepository fixedExpenseRepository;
    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserExpendGroupRepository expendGroupRepository;
    private final FixedExpenseMonthlyFactory monthlyFactory;

    public FixedExpenseSyncService(UserFixedExpenseMonthlyRepository monthlyRepository,
                                   UserFixedExpenseRepository fixedExpenseRepository,
                                   UserPaymentMethodRepository paymentMethodRepository,
                                   UserExpendGroupRepository expendGroupRepository,
                                   FixedExpenseMonthlyFactory monthlyFactory) {
        this.monthlyRepository = monthlyRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
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

        // **손댄 행만 담는다.** 건너뛴 행까지 저장 대상에 넣으면 "무엇을 바꿨는가"가
        // 코드에서 사라지고, 뒤에 삭제가 끼는 순간 rewrite 가 겪은 것과 같은 사고가 난다.
        List<UserFixedExpenseMonthly> applied = new ArrayList<>();
        for (UserFixedExpenseMonthly row : targets) {
            YearMonthValue rowMonth = YearMonthValue.of(row.getYear(), row.getMonth());
            if (!rowMonth.isWithin(start, end)) {
                // 기간을 줄여 이 달이 밖으로 나갔다. 값을 씌우지 않고 그대로 둔다 —
                // 지우는 것은 4.9 의 ④가 명시적으로 할 일이다.
                continue;
            }
            apply(row, setting, rowMonth);
            applied.add(row);
        }
        if (!applied.isEmpty()) {
            monthlyRepository.saveAll(applied);
        }
        return applied.size();
    }

    /**
     * 4.9 재작성 — <b>네 처리를 한 트랜잭션에서</b> 한다(FR-414).
     *
     * <pre>{@code
     * ① 생성  기간에 걸리는데 그 연·월 행이 없다        → createdCount
     * ② 갱신  행이 있고 modified=false                → updatedCount
     * ③ 보존  행이 있고 modified=true                 → keptCount
     * ④ 삭제  행이 있는데 기간이 그 연·월을 더는 포함하지 않는다 → deletedCount
     * }</pre>
     *
     * <h2>④가 필요한 이유</h2>
     *
     * <p>설정의 적용 기간을 줄이면(예: 종료를 2027-02 → 2026-12) 이미 만들어진 2027-01·02
     * 행이 기간 밖이 된다. <b>자동 반영(FR-412)은 값 갱신만 하고 삭제하지 않으므로</b>
     * 그 정리를 재작성이 맡는다. 자동과 수동의 경계가 여기다.
     *
     * <h2>지난 달에도 쓸 수 있다 (FR-413)</h2>
     *
     * <p>자동 반영이 미래 달만 건드리는 것과 다르다. "지난 달을 새 설정값으로 맞추고 싶다"는
     * 요구에 답하는 <b>명시적 경로</b>이며, 그래서 사용자가 버튼으로 부른다.
     *
     * <h2>{@code overwriteModified}</h2>
     *
     * <p>참이면 ③이 ②로 넘어가고 {@code modified} 표시가 내려간다(SC-406). 파괴적이라
     * 기본은 거짓이며 화면에서 확인을 받는 것을 전제한다.
     *
     * @return 네 건수와 재작성 <b>후</b>의 목록. 호출 후 재조회가 필요 없다(FR-415)
     */
    @Transactional
    public SyncResult rewrite(AuthPrincipal principal, YearMonthValue yearMonth,
                              boolean overwriteModified) {
        // 그 달에 기간이 걸리는 설정들. ①의 대상이자 ④의 판단 기준이다.
        List<UserFixedExpense> applicable =
                fixedExpenseRepository.findApplicableTo(principal.idKey(), yearMonth.value());
        Map<Long, UserFixedExpense> applicableByIdx = applicable.stream()
                .collect(Collectors.toMap(UserFixedExpense::getIdx, setting -> setting));

        // 그 달에 이미 있는 행 전부. ②③④의 대상이다.
        List<UserFixedExpenseMonthly> existing = monthlyRepository
                .findByUserIdKeyAndYearAndMonth(
                        principal.idKey(), yearMonth.year(), yearMonth.month());

        int kept = 0;
        List<Long> toDelete = new ArrayList<>();
        // ②의 대상만 담는다. existing 을 통째로 저장하면 안 된다 — 아래 참고.
        List<UserFixedExpenseMonthly> toUpdate = new ArrayList<>();

        for (UserFixedExpenseMonthly row : existing) {
            Long settingIdx = row.getFixedExpense().getIdx();
            UserFixedExpense setting = applicableByIdx.get(settingIdx);
            if (setting == null) {
                // ④ 기간이 이 연·월을 더는 포함하지 않는다.
                toDelete.add(settingIdx);
                continue;
            }
            if (Boolean.TRUE.equals(row.getModified()) && !overwriteModified) {
                // ③ 사용자가 직접 손댄 달이다. 그대로 둔다.
                kept++;
                continue;
            }
            // ② 관리 값으로 갱신한다. overwriteModified 면 표시도 내린다.
            apply(row, setting, yearMonth);
            if (Boolean.TRUE.equals(row.getModified())) {
                row.setModified(false);
            }
            toUpdate.add(row);
        }

        // 지운 행 수를 리포지토리가 돌려주는 값으로 센다. toDelete 의 크기는 "설정 개수"라
        // 유니크 제약 덕에 지금은 같지만, 세는 대상이 다르면 언젠가 갈린다.
        //
        // 소유자 조건이 이 삭제에는 없다. toDelete 가 principal 로 좁힌 existing 에서 나온
        // 값이라 안전하며, fixed_expense_idx 는 회원 하나에만 속한다.
        long deleted = 0;
        if (!toDelete.isEmpty()) {
            deleted = monthlyRepository.deleteByFixedExpenseIdxInAndYearAndMonth(
                    toDelete, yearMonth.year(), yearMonth.month());
        }
        // **삭제된 Entity 를 여기 넘기면 안 된다.** Spring Data 의 파생 삭제 쿼리는
        // 벌크 DELETE 가 아니라 "조회 후 em.remove"라, ④로 지운 행이 영속성 컨텍스트에서
        // removed 상태가 된다. 그 인스턴스가 save 로 들어가면 Hibernate 가
        // `ObjectDeletedException: deleted instance passed to merge` 를 던지고
        // 사용자에게는 9000 이 나간다.
        //
        // 한 달에 ④삭제와 ②갱신이 **함께** 일어날 때만 터지므로, 둘 중 하나만 있는
        // 시험으로는 드러나지 않는다.
        if (!toUpdate.isEmpty()) {
            monthlyRepository.saveAll(toUpdate);
        }
        int updated = toUpdate.size();

        // ① 없는 것만 만든다. 이미 있으면 0행이라 위에서 처리한 행과 겹치지 않는다.
        int created = 0;
        for (UserFixedExpense setting : applicable) {
            MonthlyValues values = monthlyFactory.from(setting, yearMonth);
            created += monthlyRepository.insertIfAbsent(
                    principal.idKey(), setting.getIdx(), yearMonth.year(), yearMonth.month(),
                    values.amount(), values.paymentDate(), values.content(),
                    values.paymentMethodIdx(), values.expendGroupIdx(), principal.idKey());
        }

        return new SyncResult(created, updated, kept, Math.toIntExact(deleted));
    }

    /**
     * 재작성의 네 건수.
     *
     * <p>목록과 합계는 호출자({@code FixedExpenseMonthlyService})가 재작성 <b>후</b>
     * 상태를 다시 읽어 만든다 — 이 클래스는 "설정을 내역에 반영한다"만 하고 조회 응답을
     * 조립하지 않는다.
     */
    public record SyncResult(int created, int updated, int kept, int deleted) {
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
