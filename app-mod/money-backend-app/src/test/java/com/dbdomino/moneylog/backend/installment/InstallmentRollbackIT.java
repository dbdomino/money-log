package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;

/**
 * 할부 등록의 전체 롤백 — quickstart #23 (FR-310·SC-303).
 *
 * <p><b>부분 생성이 왜 위험한가.</b> 12개 중 5개만 들어간 상태에서 사용자가 실패를 보고
 * 다시 등록하면 앞 5개가 <b>중복 등록</b>된다. 004 는 업무 유일 제약을 두지 않으므로
 * (FR-309 — 같은 날짜·금액·수단의 지출을 여러 건 등록할 수 있어야 한다) DB 가 그 중복을
 * 막아주지 않는다. 전체 롤백이 유일하게 안전한 선택이다.
 *
 * <h2>실패를 어떻게 주입하는가</h2>
 *
 * <p>Repository 를 스파이로 감싸 <b>실제 저장을 먼저 수행한 뒤</b> 예외를 던진다.
 * 행이 실제로 INSERT 된 상태에서 트랜잭션이 되감기는지를 보려는 것이라, 저장 자체를
 * 막아 버리면 "애초에 아무것도 안 들어갔다"를 확인하는 데 그친다.
 *
 * <p>DB 제약을 잠시 좁히는 방법({@code ALTER TABLE})은 쓰지 않는다 — 요청이 쓰는 커넥션과
 * DDL 이 서로를 막아 롤백 자체가 실패하고({@code Unable to rollback against JDBC
 * Connection}), 그러면 시험이 검증하려던 것과 다른 이유로 깨진다.
 */
class InstallmentRollbackIT extends AbstractInstallmentIT {

    @MockitoSpyBean
    private UserExpenseRepository expenseRepositorySpy;

    /** 저장은 실제로 하고, 그 직후에 실패시킨다. */
    private void failAfterSaving() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new DataIntegrityViolationException("주입한 실패");
        }).when(expenseRepositorySpy).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("#23 저장 도중 실패하면 전부 롤백되어 한 행도 남지 않는다")
    void aFailureMidwayRollsBackEveryRow() throws Exception {
        Fixture fixture = prepare();
        failAfterSaving();

        JsonNode response = createInstallment(fixture, 100000L, 12, "2026-07");

        assertThat(resCode(response)).isEqualTo(3205);
        // 12건이 실제로 INSERT 된 뒤 되감겼는지를 본다 — 한 행도 남으면 안 된다.
        assertThat(countExpenses(fixture.member()))
                .as("전체 롤백이므로 한 행도 남지 않아야 한다")
                .isZero();
    }

    @Test
    @DisplayName("#23 롤백된 뒤 같은 요청을 다시 보내면 정확히 12건만 생긴다")
    void retryingAfterRollbackCreatesExactlyOneGroup() throws Exception {
        Fixture fixture = prepare();
        failAfterSaving();
        assertThat(resCode(createInstallment(fixture, 100000L, 12, "2026-07"))).isEqualTo(3205);

        // 스텁을 걷어내 원래 동작으로 되돌린다 — 인터페이스 프록시라 doCallRealMethod 는 쓸 수 없다.
        reset(expenseRepositorySpy);
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));

        // 부분 생성이 남아 있었다면 여기서 12건을 넘는다 — 그것이 중복 등록이다.
        assertThat(countRows(groupId)).isEqualTo(12);
        assertThat(countExpenses(fixture.member())).isEqualTo(12);
    }

    @Test
    @DisplayName("#23 롤백해도 그룹 식별자는 재사용되지 않는다 — 시퀀스는 되감기지 않는다")
    void theSequenceDoesNotRewind() throws Exception {
        Fixture fixture = prepare();
        failAfterSaving();
        assertThat(resCode(createInstallment(fixture, 100000L, 3, "2026-07"))).isEqualTo(3205);

        reset(expenseRepositorySpy);
        long first = groupIdOf(createInstallment(fixture, 100000L, 3, "2026-07"));
        long second = groupIdOf(createInstallment(fixture, 100000L, 3, "2026-10"));

        // 시퀀스는 트랜잭션 밖에서 도므로 롤백돼도 값이 소모된다. 그래서 두 그룹이
        // 겹칠 일이 없다 — 겹치면 중도상환이 남의 회차를 지운다.
        assertThat(first).isNotEqualTo(second);
    }
}
