package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpendTargetDefault;
import com.dbdomino.moneylog.data.entity.UserExpendTargetMonthly;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserExpendTargetDefaultRepository;
import com.dbdomino.moneylog.data.repository.UserExpendTargetMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 목표금액의 유일성·독립성·범위 — quickstart.md §3 시나리오 #8·#18.
 *
 * <p>확인하려는 것 셋이다.
 *
 * <ol>
 *   <li>지출유형이 <b>삭제 표시돼도</b> 목표금액 행과 참조가 유지되는가(FR-038).
 *       유형이 물리 삭제였다면 FK RESTRICT가 유형 삭제를 막거나 이 행이 함께
 *       사라져야 했다. 삭제 표시(UPDATE)라 둘 다 일어나지 않는다.</li>
 *   <li>기본 목표와 월별 목표가 <b>독립</b>인가(FR-072). 기본값을 바꿔도 월별 값은
 *       그대로여야 한다 — 통계의 적용 금액이 {@code 월별 ?? 기본}이라, 둘이 얽히면
 *       어느 값이 쓰였는지 되짚을 수 없다.</li>
 *   <li>금액 상한 1억을 DB가 강제하는가(#18).</li>
 * </ol>
 *
 * <p>행이 없는 것과 {@code target_amount = 0}이 다른 상태라는 점도 함께 본다(FR-073).
 */
class ExpendTargetIT extends AbstractSchemaIT {

    /** 명세가 정한 상한(1억). 이 값은 통과하고 +1은 막혀야 한다. */
    private static final long MAX_TARGET_AMOUNT = 100_000_000L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserExpendTargetDefaultRepository targetDefaultRepository;

    @Autowired
    private UserExpendTargetMonthlyRepository targetMonthlyRepository;

    @Test
    @DisplayName("#8 지출유형을 삭제 표시해도 목표금액 행과 참조가 그대로 남는다")
    void targetSurvivesExpendGroupSoftDelete() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        UserExpendTargetDefault saved = inTx(() ->
                targetDefaultRepository.saveAndFlush(newTargetDefault(user, group, 300_000L)));

        inTx(() -> {
            UserExpendGroup target = expendGroupRepository.findById(group.getIdx()).orElseThrow();
            target.setDeleted(true);
            expendGroupRepository.saveAndFlush(target);
        });

        // 연관이 지연 로딩이라 단언을 트랜잭션 안에서 한다
        inTx(() -> {
            UserExpendTargetDefault found =
                    targetDefaultRepository.findById(saved.getIdx()).orElseThrow();

            assertThat(found.getTargetAmount()).isEqualTo(300_000L);
            assertThat(found.getExpendGroup().getIdx()).isEqualTo(group.getIdx());
            assertThat(found.getExpendGroup().getDeleted()).isTrue();
        });
    }

    @Test
    @DisplayName("같은 회원·같은 유형의 기본 목표를 두 번 만들면 두 번째가 거부된다")
    void rejectsDuplicateDefaultTarget() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        inTx(() -> targetDefaultRepository.saveAndFlush(newTargetDefault(user, group, 300_000L)));

        assertViolatesConstraint(() ->
                targetDefaultRepository.saveAndFlush(newTargetDefault(user, group, 400_000L)),
                "ux_target_default");
    }

    @Test
    @DisplayName("같은 회원·연·월·유형의 월별 목표를 두 번 만들면 두 번째가 거부된다")
    void rejectsDuplicateMonthlyTarget() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        inTx(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 12, 350_000L)));

        assertViolatesConstraint(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 12, 400_000L)),
                "ux_target_monthly");
    }

    @Test
    @DisplayName("같은 유형이라도 달이 다르면 월별 목표를 각각 가질 수 있다")
    void allowsSameGroupAcrossDifferentMonths() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        inTx(() -> {
            targetMonthlyRepository.saveAndFlush(newTargetMonthly(user, group, 2026, 12, 350_000L));
            targetMonthlyRepository.saveAndFlush(newTargetMonthly(user, group, 2027, 1, 400_000L));
        });

        assertThat(inTx(() -> targetMonthlyRepository.findByUserIdKeyAndYearAndMonth(
                user.getIdKey(), 2026, 12))).hasSize(1);
        assertThat(inTx(() -> targetMonthlyRepository.findByUserIdKeyAndYearAndMonth(
                user.getIdKey(), 2027, 1))).hasSize(1);
    }

    @Test
    @DisplayName("기본 목표를 바꿔도 월별 목표는 따라가지 않는다")
    void defaultAndMonthlyTargetsAreIndependent() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        UserExpendTargetDefault base = inTx(() ->
                targetDefaultRepository.saveAndFlush(newTargetDefault(user, group, 300_000L)));
        inTx(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 12, 350_000L)));

        inTx(() -> {
            UserExpendTargetDefault target =
                    targetDefaultRepository.findById(base.getIdx()).orElseThrow();
            target.setTargetAmount(500_000L);
            targetDefaultRepository.saveAndFlush(target);
        });

        assertThat(inTx(() -> targetMonthlyRepository
                .findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
                        user.getIdKey(), 2026, 12, group.getIdx())
                .orElseThrow().getTargetAmount()))
                .isEqualTo(350_000L);
    }

    @Test
    @DisplayName("월별 목표가 없는 달과 0원으로 정한 달은 다른 상태다")
    void absentMonthlyTargetDiffersFromZero() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        inTx(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 12, 0L)));

        // 0원으로 정한 달 — 행이 있고 값이 0이다
        assertThat(inTx(() -> targetMonthlyRepository
                .findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
                        user.getIdKey(), 2026, 12, group.getIdx())))
                .get()
                .extracting(UserExpendTargetMonthly::getTargetAmount)
                .isEqualTo(0L);

        // 정하지 않은 달 — 행 자체가 없다. 응답 monthlyTargetAmount 가 null 이 되는 쪽이다
        assertThat(inTx(() -> targetMonthlyRepository
                .findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
                        user.getIdKey(), 2027, 1, group.getIdx())))
                .isEmpty();
    }

    @Test
    @DisplayName("#18 기본 목표금액 1억 초과는 CHECK 제약에 막힌다")
    void rejectsDefaultTargetAboveUpperBound() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        assertViolatesConstraint(() -> targetDefaultRepository.saveAndFlush(
                newTargetDefault(user, group, MAX_TARGET_AMOUNT + 1)),
                "ck_target_default_amount");
    }

    @Test
    @DisplayName("#18 월별 목표금액 1억 초과는 CHECK 제약에 막히고, 1억 정확히는 통과한다")
    void monthlyTargetUpperBoundIsInclusive() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        // 경계값은 허용된다 — BETWEEN 이 양 끝을 포함한다
        inTx(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 12, MAX_TARGET_AMOUNT)));

        assertViolatesConstraint(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2027, 1, MAX_TARGET_AMOUNT + 1)),
                "ck_target_monthly_amount");
    }

    @Test
    @DisplayName("월 13인 월별 목표는 CHECK 제약에 막힌다")
    void rejectsMonthlyTargetMonthOutOfRange() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        assertViolatesConstraint(() -> targetMonthlyRepository.saveAndFlush(
                newTargetMonthly(user, group, 2026, 13, 350_000L)),
                "ck_target_monthly_month");
    }

    private UserExpendTargetDefault newTargetDefault(User user, UserExpendGroup group,
                                                     long targetAmount) {
        UserExpendTargetDefault target = new UserExpendTargetDefault();
        target.setUser(user);
        target.setExpendGroup(group);
        target.setTargetAmount(targetAmount);
        stampAudit(target, user.getIdKey());
        return target;
    }

    private UserExpendTargetMonthly newTargetMonthly(User user, UserExpendGroup group,
                                                     int year, int month, long targetAmount) {
        UserExpendTargetMonthly target = new UserExpendTargetMonthly();
        target.setUser(user);
        target.setExpendGroup(group);
        target.setYear(year);
        target.setMonth(month);
        target.setTargetAmount(targetAmount);
        stampAudit(target, user.getIdKey());
        return target;
    }
}
