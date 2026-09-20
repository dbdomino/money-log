package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.client.BinaryPayload;
import com.dbdomino.moneylog.front.support.FormFailure;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 3.5 엑셀 일괄 등록 — 양식 받기와 올리기.
 *
 * <p><b>같은 지출·소득을 다른 입구로 넣는 화면이다.</b> 독립 페이지이며 월별 가계부 목록을
 * 쓰지 않는다.
 *
 * <h2>어느 방향으로도 브라우저가 백엔드를 직접 부르지 않는다</h2>
 *
 * <pre>
 * 받기:   브라우저 ──주소──&gt; 화면 모듈 ──인증+조회──&gt; 백엔드 ──파일──&gt; 브라우저
 * 올리기: 브라우저 ──파일──&gt; 화면 모듈 ──파일──&gt; 백엔드
 * </pre>
 *
 * <p>양식 받기에도 인증이 필요한데 <b>브라우저의 링크는 인증을 붙이지 못한다.</b> 007 이
 * 파일 응답 통로를 만들며 "아이콘·엑셀 두 호출"이라고 적어 둔 자리가 여기다.
 *
 * <h2>화면이 엑셀을 파싱하지 않는다</h2>
 *
 * <p>파싱하면 <b>저장 규칙이 두 곳에 생긴다.</b> 행 수는 브라우저가 크기로 가늠해 경고하고
 * 정확한 판정은 서버가 한다.
 *
 * <h2>올린 파일의 바이트를 로그에 넘기지 않는다</h2>
 *
 * <p>엑셀은 아이콘보다 크고, 로그로 가면 파일이 부풀고 <b>거래 내역이 그대로 남는다.</b>
 */
@Controller
public class LedgerExcelController {

    static final String VIEW = "ledger/excel";

    static final String TEMPLATE_URL = "/ledger/excel/template";
    static final String TEMPLATE_PATH = "/expense-incomes/excel/template";
    static final String UPLOAD_PATH = "/expense-incomes/excel/upload";

    /** 양식 이름을 받지 못했을 때만 쓰는 값. 평소에는 백엔드가 준 이름을 그대로 쓴다. */
    private static final String FALLBACK_FILENAME = "expense_income_template.xlsx";

    private final BackendApiClient backendApiClient;

    public LedgerExcelController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    // ── 화면 ────────────────────────────────────────────────────────────

    /** 빈 화면. 서버 호출이 없고 <b>제약이 입력 전부터</b> 보인다. */
    @GetMapping("/ledger/excel")
    public String page(Model model) {
        model.addAttribute("activeMenu", "excel");
        return VIEW;
    }

    // ── 양식 받기 ───────────────────────────────────────────────────────

    /**
     * 양식을 받아 브라우저로 흘려보낸다.
     *
     * <p><b>받은 파일 이름을 그대로 넘긴다.</b> 화면이 이름을 지어내면 백엔드가 양식을 바꿀
     * 때 <b>파일 이름만 옛것으로 남는다.</b>
     *
     * <p>실패하면 <b>오류 화면으로 간다.</b> 아이콘과 다른 점이다 — 아이콘은 화면 한 귀퉁이가
     * 비는 것으로 끝나지만, 양식을 못 받으면 사용자가 이 화면에서 할 수 있는 일이 없다.
     */
    @GetMapping(TEMPLATE_URL)
    public ResponseEntity<Resource> template() {
        BinaryPayload payload = backendApiClient.getBinary(TEMPLATE_PATH);

        String filename = payload.filename() == null || payload.filename().isBlank()
                ? FALLBACK_FILENAME
                : payload.filename();

        return ResponseEntity.ok()
                .contentType(mediaType(payload.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(payload.bytes()));
    }

    private static MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    // ── 올리기 ──────────────────────────────────────────────────────────

    /**
     * 파일을 실어 백엔드로 보낸다.
     *
     * <p>성공이면 건수를, 행 오류면 <b>표로 보일 값</b>을 모델에 담는다. 한 행이라도 틀리면
     * 전체가 등록되지 않는 것이 계약이라 <b>화면이 "몇 건은 들어갔다"고 말하지 않는다.</b>
     */
    @PostMapping("/ledger/excel")
    public String upload(@RequestParam(name = "file", required = false) MultipartFile file,
            Model model) throws IOException {

        model.addAttribute("activeMenu", "excel");

        if (file == null || file.isEmpty()) {
            // 빈 파일도 서버가 판정하지만, 보낼 것이 없는 요청까지 왕복시킬 이유는 없다.
            FormFailure.applyTo(model, ErrorCode.EXCEL_FILE_EMPTY.code(),
                    ErrorCode.EXCEL_FILE_EMPTY.message());
            return VIEW;
        }

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new UploadedFile(file));

        model.addAttribute("uploadResult",
                backendApiClient.postMultipart(UPLOAD_PATH, parts, ExcelUploadResult.class));
        return VIEW;
    }

    /**
     * 올린 파일을 그대로 다시 싣는다. <b>이름과 형식을 지어내지 않는다.</b>
     *
     * <p>바이트를 문자열로 만들지 않는 것이 요점이다 — 만들면 그 값이 어딘가의 로그로 흘러
     * 들어갈 길이 생기고, 엑셀에는 거래 내역이 그대로 들어 있다.
     */
    private static final class UploadedFile extends ByteArrayResource {

        private final String filename;

        UploadedFile(MultipartFile file) throws IOException {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패를 이 화면 안에서 보인다.
     *
     * <p><b>행 오류는 폼이 아니라 결과 영역에 표로</b> 보인다 — 칸 하나짜리 폼이라 붙일 칸이
     * 없고, 오류가 여럿이라 한 줄로 보일 수 없다. 나머지 실패는 폼 상단이며 <b>거절 안내에
     * 제약을 다시 적는다</b>(허용 형식·한도).
     *
     * <p><b>양식 받기 실패는 이 화면으로 돌아오지 않는다.</b> 그 처리는 화면이 아니라 파일을
     * 돌려주고, 실패하면 오류 화면으로 가는 것이 맞다 — 양식을 못 받으면 사용자가 이 화면에서
     * 할 수 있는 일이 없다. 컨트롤러에 붙은 실패 선언이 007 의 공통 착지보다 먼저 잡으므로
     * <b>여기서 갈라 준다</b>: 가르지 않으면 양식을 못 받았는데 업로드 폼이 멀쩡히 뜬다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        if (TEMPLATE_URL.equals(request.getRequestURI())) {
            model.addAttribute("resCode", exception.getResCode());
            model.addAttribute("message", exception.getMessage());
            return "error";
        }

        model.addAttribute("activeMenu", "excel");
        FormFailure.applyToFormTop(model, exception);

        List<ExcelUploadResult.RowError> rowErrors = ExcelUploadResult.rowErrorsOf(exception);
        if (!rowErrors.isEmpty()) {
            model.addAttribute("rowErrors", rowErrors);
        }
        return VIEW;
    }
}
