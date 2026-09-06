package com.dbdomino.moneylog.backend.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.12 의 행 단위 검증 — quickstart #40·#41·#46·#49 (FR-320).
 *
 * <p><b>#41 이 "오류를 전부 모은다"를 지킨다.</b> 첫 오류에서 멈추면 사용자가 파일을 여러 번
 * 왕복하며 고치게 된다. 300행 상한이 있으므로 전 행을 검증해도 비용이 크지 않다.
 *
 * <p><b>검증을 저장보다 완전히 앞에 끝낸다.</b> 저장하다 실패해서 롤백하는 것이 아니다 —
 * 그 순서면 첫 실패에서 멈춰 {@code errors[]} 에 하나밖에 담을 수 없다.
 */
class ExcelValidationIT extends AbstractExcelIT {

    /** 응답의 {@code errors[]} 를 "행-열" 문자열로 만든다. */
    private List<String> errorPositions(JsonNode response) {
        List<String> positions = new ArrayList<>();
        for (JsonNode error : response.get("data").get("errors")) {
            positions.add(error.get("row").asInt() + "-" + error.get("column").asString());
        }
        return positions;
    }

    @Test
    @DisplayName("#40 중간 한 행에 오류가 있으면 3502 이고 전체가 롤백된다")
    void oneBadRowRollsBackEverything() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"),
                expenseRow("2026-03-16", "0", "국민카드", "식비", "편의점", "저녁"),   // 금액 0
                expenseRow("2026-03-17", "9000", "국민카드", "식비", "편의점", "간식")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        // 앞뒤 두 행이 정상이어도 한 건도 저장하지 않는다 — 부분 성공은 재등록에서
        // 중복을 만든다.
        assertThat(countExpenses(fixture.member())).isZero();
        assertThat(countIncomes(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#40 errors[] 가 row·column·message 로 위치를 짚어 준다")
    void errorsPointAtTheCell() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"),
                expenseRow("2026-03-16", "0", "국민카드", "식비", "편의점", "저녁")));

        JsonNode response = upload(fixture, file);

        JsonNode errors = response.get("data").get("errors");
        assertThat(errors).hasSize(1);
        // 헤더가 1행이므로 두 번째 데이터 행은 3행이다 — 사용자가 엑셀에서 보는 번호다.
        assertThat(errors.get(0).get("row").asInt()).isEqualTo(3);
        assertThat(errors.get(0).get("column").asString()).isEqualTo("C");
        assertThat(errors.get(0).get("message").asString()).isNotBlank();
        // 건수는 전부 0이다 — "몇 건은 들어갔다"로 읽힐 여지를 남기지 않는다.
        assertThat(response.get("data").get("importedCount").asInt()).isZero();
    }

    @Test
    @DisplayName("#41 오류가 여러 행에 있으면 errors[] 에 전부 담긴다 — 첫 오류에서 멈추지 않는다")
    void everyErrorIsCollected() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "0", "국민카드", "식비", "편의점", "점심"),        // 2행 C
                expenseRow("2026/03/16", "12000", "국민카드", "식비", "편의점", "저녁"),   // 3행 B
                expenseRow("2026-03-17", "9000", "없는카드", "식비", "편의점", "간식")));  // 4행 D

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactlyInAnyOrder("2-C", "3-B", "4-D");
    }

    @Test
    @DisplayName("#41 한 행에서 여러 칸이 잘못되면 칸마다 담긴다")
    void multipleErrorsInOneRowAreAllCollected() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("잘못된날짜", "-100", "없는카드", "식비", "편의점", "점심")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).contains("2-B", "2-C", "2-D");
    }

    @Test
    @DisplayName("#46 파일의 수단 이름이 DB 에 없으면 그 행을 오류로 보고 전체 롤백한다")
    void unknownPaymentMethodNameIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "없는카드", "식비", "편의점", "점심")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactly("2-D");
        assertThat(countExpenses(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#46 삭제 표시된 수단 이름도 찾지 못한다 — 사용 중만 건다")
    void deletedPaymentMethodNameIsNotFound() throws Exception {
        Fixture fixture = prepare();
        long deadId = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + deadId, fixture.token())))
                .isEqualTo(200);
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "옛 카드", "식비", "편의점", "점심")));

        assertThat(resCode(upload(fixture, file))).isEqualTo(3502);
    }

    @Test
    @DisplayName("#46 지출 행에 소득용 수단을 적으면 오류다 — 용도가 갈린다")
    void wrongPurposeMethodIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "월급통장", "식비", "편의점", "점심")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactly("2-D");
    }

    @Test
    @DisplayName("같은 이름의 수단이 둘이면 그 행을 오류로 본다 — 어느 쪽인지 정할 수 없다")
    void ambiguousPaymentMethodNameIsAnError() throws Exception {
        Fixture fixture = prepare();
        createExpensePaymentMethod(fixture.token(), "국민카드");   // 같은 이름 하나 더
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심")));

        JsonNode response = upload(fixture, file);

        // 첫 매치를 고르면 금액이 붙는 자리를 서버가 임의로 정하는 셈이 된다.
        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactly("2-D");
    }

    @Test
    @DisplayName("#49 소득 행에 지출유형·장소가 채워져 있으면 3502 다")
    void incomeRowWithExpenseOnlyColumnsIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(new String[] {
                ExcelColumn.KIND_INCOME, "2026-03-25", "3000000", "월급통장", "식비", "회사", "급여"}));

        JsonNode response = upload(fixture, file);

        // tbl_income 에 그 컬럼이 없어 저장할 자리가 없는데, 조용히 버리면 사용자는
        // 저장됐다고 믿는다.
        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactlyInAnyOrder("2-E", "2-F");
    }

    @Test
    @DisplayName("A열 구분이 허용 값 밖이면 3502 다")
    void unknownKindIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(new String[] {
                "BOTH", "2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"}));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactly("2-A");
    }

    @Test
    @DisplayName("지출 행에서 필수 열이 비어 있으면 3502 다")
    void missingRequiredColumnIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(new String[] {
                ExcelColumn.KIND_EXPENSE, "2026-03-15", "12000", "국민카드", "식비", null, null}));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactlyInAnyOrder("2-F", "2-G");
    }

    @Test
    @DisplayName("길이 초과도 3502 다 — 9000 이 아니다")
    void tooLongValueIsAnError() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "가".repeat(101), "점심")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(3502);
        assertThat(errorPositions(response)).containsExactly("2-F");
    }
}
