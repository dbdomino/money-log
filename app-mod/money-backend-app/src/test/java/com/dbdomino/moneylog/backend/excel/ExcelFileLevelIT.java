package com.dbdomino.moneylog.backend.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.12 의 파일 단위 판정 — quickstart #42·#43·#44 (FR-319).
 *
 * <p><b>행 검증에 들어가기 전에 판정한다.</b> 300행 초과 파일에 행 오류가 섞여 있어도
 * {@code 3502} 가 아니라 <b>{@code 3504}</b> 가 나와야 한다 — 사용자가 할 일이 "표를
 * 고치는 것"이 아니라 "파일을 나누는 것"이기 때문이다.
 *
 * <p>코드를 나눠 둔 이유가 그것이다. {@code 3503}·{@code 3504}·{@code 3505} 는 "파일을
 * 다시 고르세요", {@code 3502} 는 "표의 N행 M열을 고치세요"다. 한 코드로 묶으면 프론트가
 * {@code errors[]} 의 유무로 분기해야 하는데 그건 계약이 아니라 추측이다.
 */
class ExcelFileLevelIT extends AbstractExcelIT {

    /** 정상 지출 행 {@code count} 개. */
    private List<String[]> validRows(int count) {
        List<String[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"));
        }
        return rows;
    }

    @Test
    @DisplayName("#42 301행이면 3504 다")
    void overThreeHundredRowsIs3504() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = upload(fixture, workbook(validRows(301)));

        assertThat(resCode(response)).isEqualTo(3504);
        assertThat(countExpenses(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#42 정확히 300행은 통과한다 — 경계값")
    void exactlyThreeHundredRowsPasses() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = upload(fixture, workbook(validRows(300)));

        // 상한을 하나 낮게 잡은 구현은 여기서만 걸린다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(countExpenses(fixture.member())).isEqualTo(300);
    }

    @Test
    @DisplayName("#42 300행을 넘으면 행 오류가 섞여 있어도 3504 다 — 파일 판정이 먼저다")
    void rowLimitBeatsRowErrors() throws Exception {
        Fixture fixture = prepare();
        List<String[]> rows = validRows(300);
        rows.add(expenseRow("2026-03-15", "0", "국민카드", "식비", "편의점", "점심"));   // 301행째, 금액 오류

        JsonNode response = upload(fixture, workbook(rows));

        // 3502 가 나오면 행 검증이 파일 판정보다 앞선 것이다.
        assertThat(resCode(response)).isEqualTo(3504);
        assertThat(response.get("data").has("errors")).isFalse();
    }

    @Test
    @DisplayName("#43 .xlsx 가 아닌 파일이면 3503 이다")
    void notAnXlsxIs3503() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = upload(fixture,
                "이건 엑셀이 아니라 그냥 텍스트다".getBytes(StandardCharsets.UTF_8), "ledger.xlsx");

        // 확장자가 .xlsx 여도 내용으로 판정한다.
        assertThat(resCode(response)).isEqualTo(3503);
    }

    @Test
    @DisplayName("#43 확장자만 바꾼 파일도 3503 이다 — 내용으로 판정한다")
    void renamedFileIsAlso3503() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(upload(fixture,
                "%PDF-1.4 가짜".getBytes(StandardCharsets.UTF_8), "ledger.xlsx"))).isEqualTo(3503);
    }

    @Test
    @DisplayName("#44 헤더만 있고 데이터가 0행이면 3505 다")
    void headerOnlyIs3505() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = upload(fixture, workbook(List.<String[]>of()));

        // 안내 행을 두지 않기로 해서 "헤더만 = 데이터 0행"이 곧바로 성립한다.
        assertThat(resCode(response)).isEqualTo(3505);
    }

    @Test
    @DisplayName("#44 빈 줄만 있는 파일도 3505 다 — 빈 줄은 데이터가 아니다")
    void blankRowsOnlyIsAlso3505() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                new String[] {null, null, null, null, null, null, null},
                new String[] {"", "", "", "", "", "", ""}));

        assertThat(resCode(upload(fixture, file))).isEqualTo(3505);
    }

    @Test
    @DisplayName("#44 빈 파일을 올리면 3505 다")
    void emptyUploadIs3505() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(upload(fixture, new byte[0]))).isEqualTo(3505);
    }

    @Test
    @DisplayName("파일 단위 거절은 errors[] 를 싣지 않는다 — data 는 message 한 칸이다")
    void fileLevelFailuresCarryOnlyAMessage() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = upload(fixture, workbook(List.<String[]>of())).get("data");

        assertThat(data.get("message").asString()).isNotBlank();
        assertThat(data.size()).isEqualTo(1);
    }
}
