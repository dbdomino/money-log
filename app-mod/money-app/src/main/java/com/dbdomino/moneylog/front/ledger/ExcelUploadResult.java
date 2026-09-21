package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.front.client.BackendApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * 엑셀 일괄 등록의 결과. <b>성공과 실패의 모양이 다르다.</b>
 *
 * <p>성공은 <b>건수 셋</b>이고 실패는 <b>행 번호·열·사유의 목록</b>이다. 한 타입에 섞지
 * 않는 이유는 섞으면 화면이 "어느 쪽인가"를 값의 유무로 짐작하게 되고, 그 짐작이 틀리는
 * 날 성공 화면에 오류 표가 뜨기 때문이다.
 *
 * <h2>행 오류는 실패한 그 응답에서만 나온다</h2>
 *
 * <p>화면이 백엔드를 따로 불러 받아 올 수 없다 — 그 요청의 응답에만 들어 있는 값이다.
 * 007 의 실패 예외가 <b>봉투의 값을 통째로</b> 들고 올라오게 고친 것이 이 때문이다.
 *
 * @param importedCount 등록된 전체 건수
 * @param expenseCount 등록된 지출 건수
 * @param incomeCount 등록된 소득 건수
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExcelUploadResult(
        Integer importedCount,
        Integer expenseCount,
        Integer incomeCount) {

    public int imported() {
        return importedCount == null ? 0 : importedCount;
    }

    public int expenses() {
        return expenseCount == null ? 0 : expenseCount;
    }

    public int incomes() {
        return incomeCount == null ? 0 : incomeCount;
    }

    /**
     * 행 하나의 오류. 사용자가 엑셀을 열어 고칠 수 있도록 <b>어디를</b> 가리킨다.
     *
     * @param row 엑셀 행 번호. 헤더가 1이다
     * @param column 열 이름 또는 문자
     * @param message 사유. 백엔드 문구를 그대로 쓴다
     */
    public record RowError(Integer row, String column, String message) {

        /** 행 번호가 없으면 맨 뒤로 보낸다. 순서를 잃는 것보다 낫다. */
        int sortKey() {
            return row == null ? Integer.MAX_VALUE : row;
        }
    }

    /**
     * 실패 예외가 실어 온 봉투에서 행 오류를 꺼낸다. 행 오류가 아니면 <b>빈 목록</b>이다.
     *
     * <p><b>행 번호 순으로 정렬한다.</b> 사용자가 엑셀을 열어 위에서부터 고치기 때문이다 —
     * 응답이 어떤 순서로 오든 화면이 그 순서를 보장한다.
     *
     * <p>꺼내지 못해도 터지지 않는다. 그때는 폼 상단의 문구만 남고, 그것이 지금까지의
     * 동작이다.
     */
    public static List<RowError> rowErrorsOf(BackendApiException exception) {
        if (!exception.hasPayload()) {
            return List.of();
        }
        JsonNode errors = exception.payload().get("errors");
        if (errors == null || !errors.isArray()) {
            return List.of();
        }

        List<RowError> parsed = new ArrayList<>();
        for (JsonNode error : errors) {
            parsed.add(new RowError(
                    intOrNull(error, "row"),
                    textOrNull(error, "column"),
                    textOrNull(error, "message")));
        }
        parsed.sort(Comparator.comparingInt(RowError::sortKey));
        return List.copyOf(parsed);
    }

    private static Integer intOrNull(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || !value.isNumber() ? null : value.asInt();
    }

    private static String textOrNull(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value.asString();
    }
}
