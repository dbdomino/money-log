package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.response.ExcelImportResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ExcelImportService;
import com.dbdomino.moneylog.backend.service.ExcelTemplateService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 엑셀 양식·일괄 등록 — 3.11 · 3.12.
 *
 * <h2>3.11 만 응답 규격의 예외다</h2>
 *
 * <table border="1">
 *   <caption>excel-contract.md §2·§3</caption>
 *   <tr><th>API</th><th>성공</th><th>실패</th></tr>
 *   <tr><td>3.11 양식 다운로드</td><td><b>{@code .xlsx} 바이너리</b> + {@code attachment}</td>
 *       <td><b>래퍼를 쓴다</b> — {@code 1001} · {@code 9000}</td></tr>
 *   <tr><td>3.12 업로드</td><td>래퍼</td><td>래퍼</td></tr>
 * </table>
 *
 * <p><b>003 의 아이콘 조회(2.10)와 다르다.</b> 2.10 은 인증 실패도 래퍼 없는 401 이지만
 * 3.11 은 <b>인증 실패에 래퍼를 쓴다</b>(FR-322). 사용 흐름이 달라서다 — 2.10 은
 * {@code <img>}/fetch 로 받는 이미지라 상태 코드만으로 판정하지만, 3.11 은 사용자가
 * <b>다운로드 버튼을 누르는</b> 흐름이라 실패 사유를 화면에 띄워야 한다.
 *
 * <p>그래서 {@code RestAuthEntryPoint} 에 이 경로를 <b>추가하지 않는다</b> — 002 의 기본
 * 동작(래퍼 + HTTP 200)이 그대로 맞다.
 *
 * <p><b>3.12 는 예외가 아니다.</b> 파일을 돌려주지 않고 성공·실패 모두 래퍼를 쓴다.
 */
@RestController
@RequestMapping("/api/v1/expense-incomes/excel")
public class ExpenseIncomeExcelController {

    /** 내려받는 파일 이름. 설계 명세 3.11 이 정한 값이다. */
    private static final String TEMPLATE_FILENAME = "expense_income_template.xlsx";

    /** {@code .xlsx} 의 MIME 타입. */
    private static final MediaType XLSX =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelTemplateService templateService;
    private final ExcelImportService importService;

    public ExpenseIncomeExcelController(ExcelTemplateService templateService,
                                        ExcelImportService importService) {
        this.templateService = templateService;
        this.importService = importService;
    }

    /**
     * 3.11 양식 다운로드 — <b>성공은 파일이다</b>.
     *
     * <p>{@code produces} 를 지정하지 않는다. 성공은 {@code .xlsx}, 실패는 JSON 래퍼라
     * 하나로 고정하면 한쪽이 깨진다 — {@code Content-Type} 은 {@link ResponseEntity} 에
     * 직접 싣고 실패는 전역 예외 처리가 알아서 JSON 으로 낸다.
     *
     * <p>{@code Content-Disposition: attachment} 를 붙여 브라우저가 저장 대화상자를 띄우게
     * 한다. 붙이지 않으면 브라우저가 파일을 열려고 시도한다.
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @AuthenticationPrincipal AuthPrincipal principal) {
        byte[] body = templateService.createTemplate(principal);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(TEMPLATE_FILENAME).build().toString())
                .body(body);
    }

    /**
     * 3.12 일괄 업로드 — <b>파일을 돌려주지 않는다</b>.
     *
     * <p>파트 이름은 {@code file} 이다(설계 명세 3.12). 003 의 아이콘이 {@code iconFile} 인
     * 것과 다르므로 베끼지 않는다.
     *
     * <p>성공은 {@code { resCode: 200, data: { importedCount, ... } }}, 행별 오류는
     * {@code { resCode: 3502, data: { message, errors[] } }} 다 — 둘 다 래퍼를 쓴다.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<ExcelImportResponse> upload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return RestResponseDto.ok(importService.importFile(principal, file));
    }
}
