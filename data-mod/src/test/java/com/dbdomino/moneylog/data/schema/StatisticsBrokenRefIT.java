package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.entity.UserStatisticsExpendGroup;
import com.dbdomino.moneylog.data.entity.UserStatisticsPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 통계 상세의 참조 무결성 부재 — quickstart.md §3 시나리오 #15, FR-078a.
 *
 * <p>확인하려는 것: 통계 상세의 {@code expend_group_idx}·{@code payment_method_idx}에
 * <b>FK가 없는가</b>다. 이 테스트는 다른 것들과 방향이 반대다 — 보통은 "제약이
 * 막는가"를 보지만 여기서는 <b>막지 않는가</b>를 본다.
 *
 * <p>왜 FK가 없어야 하는가 — 통계는 저장 시점의 사진이다(FR-075). 원본 유형·수단이
 * 나중에 사라져도 그 사진은 남아야 한다. FK가 있으면 둘 중 하나가 일어난다: RESTRICT면
 * 통계가 유형 삭제를 막고(사용자는 왜 못 지우는지 알 수 없다), CASCADE면 과거 통계가
 * 조용히 훼손된다. 그래서 대리키를 <b>값으로만</b> 들고 이름을 함께 저장해 화면을
 * 복원한다.
 *
 * <p>실수로 {@code @ManyToOne}을 붙이면 Hibernate가 FK를 만들어 이 성질이 깨진다.
 * 그 회귀를 잡기 위해 실재하지 않는 대리키로 INSERT를 시도한다 — FK가 생기면 여기서
 * {@code DataIntegrityViolationException}이 난다.
 */
class StatisticsBrokenRefIT extends AbstractSchemaIT {

    /**
     * 실재할 리 없는 대리키. IDENTITY가 이 근처까지 올라가지 않는다.
     *
     * <p>존재하지 않음을 매번 조회해 확인하지 않는 이유는, 확인 자체가 이 테스트가
     * 부정하려는 "참조가 유효해야 한다"는 전제를 되살리기 때문이다.
     */
    private static final Long MISSING_EXPEND_GROUP_IDX = 999_999_001L;
    private static final Long MISSING_PAYMENT_METHOD_IDX = 999_999_002L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserStatisticsRepository statisticsRepository;

    @Autowired
    private UserStatisticsExpendGroupRepository statExpendGroupRepository;

    @Autowired
    private UserStatisticsPaymentMethodRepository statPaymentMethodRepository;

    @Test
    @DisplayName("#15 실재하지 않는 지출유형 대리키로도 통계 유형별 요약이 저장된다")
    void expendGroupSummaryAcceptsMissingReference() {
        UserStatistics statistics = givenSnapshot();

        UserStatisticsExpendGroup saved = inTx(() -> statExpendGroupRepository.saveAndFlush(
                newStatisticsExpendGroup(statistics, MISSING_EXPEND_GROUP_IDX, "사라진유형")));

        UserStatisticsExpendGroup found = inTx(() ->
                statExpendGroupRepository.findById(saved.getIdx()).orElseThrow());

        assertThat(found.getExpendGroupIdx()).isEqualTo(MISSING_EXPEND_GROUP_IDX);
        // 화면 복원은 함께 저장한 이름이 맡는다 — 원본을 되짚을 수 없기 때문이다
        assertThat(found.getExpendGroupName()).isEqualTo("사라진유형");
        assertThat(found.getStatus()).isEqualTo(UserStatisticsExpendGroup.STATUS_UNDER);
    }

    @Test
    @DisplayName("#15 실재하지 않는 수단 대리키로도 통계 수단별 요약이 저장된다")
    void paymentMethodSummaryAcceptsMissingReference() {
        UserStatistics statistics = givenSnapshot();

        UserStatisticsPaymentMethod saved = inTx(() -> statPaymentMethodRepository.saveAndFlush(
                newStatisticsPaymentMethod(statistics, MISSING_PAYMENT_METHOD_IDX, "해지한카드")));

        UserStatisticsPaymentMethod found = inTx(() ->
                statPaymentMethodRepository.findById(saved.getIdx()).orElseThrow());

        assertThat(found.getPaymentMethodIdx()).isEqualTo(MISSING_PAYMENT_METHOD_IDX);
        assertThat(found.getPaymentMethodName()).isEqualTo("해지한카드");
    }

    @Test
    @DisplayName("#15 통계 상세 2종에는 유형·수단으로 나가는 FK가 하나도 없다")
    void detailTablesDeclareNoForeignKeyToMasters() {
        List<String> referencedTables = inTx(() -> jdbc.queryForList("""
                SELECT confrelid::regclass::text
                FROM pg_constraint
                WHERE contype = 'f'
                  AND connamespace = 'moneylog'::regnamespace
                  AND conrelid::regclass::text IN (
                      'tbl_statistics_expend_group',
                      'tbl_statistics_payment_method')
                """, String.class));

        // 남아 있어야 하는 FK는 회원과 소속 스냅샷뿐이다.
        // tbl_user_expend_group·tbl_user_payment_method 가 끼면 FR-078a 위반이다.
        assertThat(referencedTables)
                .containsOnly("tbl_user", "tbl_statistics")
                .doesNotContain("tbl_user_expend_group", "tbl_user_payment_method");
    }

    @Test
    @DisplayName("#15 원본 유형·수단을 삭제 표시해도 이미 저장된 통계 상세는 그대로다")
    void savedSummaryIsUnaffectedByMasterChanges() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));
        UserStatistics statistics = inTx(() ->
                statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)));

        // 실재하는 대리키로 저장한다 — 저장 당시에는 원본이 살아 있었던 상황이다
        UserStatisticsExpendGroup savedGroup = inTx(() -> statExpendGroupRepository.saveAndFlush(
                newStatisticsExpendGroup(statistics, group.getIdx(), "식비")));
        UserStatisticsPaymentMethod savedMethod = inTx(() ->
                statPaymentMethodRepository.saveAndFlush(
                        newStatisticsPaymentMethod(statistics, method.getIdx(), "국민카드")));

        // 원본의 이름을 바꾸고 삭제 표시한다
        inTx(() -> {
            UserExpendGroup targetGroup = expendGroupRepository.findById(group.getIdx()).orElseThrow();
            targetGroup.setName("외식");
            targetGroup.setDeleted(true);
            expendGroupRepository.saveAndFlush(targetGroup);

            UserPaymentMethod targetMethod =
                    paymentMethodRepository.findById(method.getIdx()).orElseThrow();
            targetMethod.setName("신한카드");
            targetMethod.setDeleted(true);
            paymentMethodRepository.saveAndFlush(targetMethod);
        });

        assertThat(inTx(() -> statExpendGroupRepository.findById(savedGroup.getIdx())
                .orElseThrow().getExpendGroupName())).isEqualTo("식비");
        assertThat(inTx(() -> statPaymentMethodRepository.findById(savedMethod.getIdx())
                .orElseThrow().getPaymentMethodName())).isEqualTo("국민카드");
    }

    private UserStatistics givenSnapshot() {
        User user = inTx(() -> userRepository.save(newUser()));
        return inTx(() -> statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)));
    }
}
