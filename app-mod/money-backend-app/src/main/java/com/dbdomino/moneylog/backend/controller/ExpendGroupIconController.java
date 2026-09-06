package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.service.ExpendGroupIconService;
import com.dbdomino.moneylog.backend.service.ExpendGroupIconService.StoredIcon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2.10 지출유형 아이콘 조회 — <b>{@code { resCode, data }} 래퍼를 쓰지 않는 유일한 API</b>.
 *
 * <h2>세 갈래 응답</h2>
 *
 * <table border="1">
 *   <caption>icon-storage.md §3</caption>
 *   <tr><th>상황</th><th>HTTP</th><th>본문</th></tr>
 *   <tr><td>성공</td><td>200</td><td>이미지 바이너리. {@code Content-Type: image/*}</td></tr>
 *   <tr><td>파일 없음</td><td>200</td><td>{@code { resCode: 3104, data: { message } }} — 래퍼를 쓴다</td></tr>
 *   <tr><td>인증 실패</td><td>401</td><td><b>본문 없음.</b> 래퍼도 없다</td></tr>
 * </table>
 *
 * <p>세 줄이 함께 있어야 계약이 완결된다. 인증 실패만 래퍼를 벗는 이유는 이 API 에
 * <b>4자리 코드를 실을 JSON 본문 자체가 없기</b> 때문이다 — 그 처리는
 * {@code RestAuthEntryPoint} 가 {@link #ICON_PATH} 를 보고 한다.
 *
 * <h2>Controller 를 따로 두는 이유</h2>
 *
 * <p>{@code ExpendGroupController} 는 {@code produces = application/json} 이다. 같은
 * Controller 에 두면 이미지 응답이 그 협상에 걸린다. 응답 규격이 다른 API 는 자리를
 * 나누는 것이 안전하다.
 *
 * <p>{@code produces} 를 지정하지 않는다 — {@code Content-Type} 은 저장된 확장자에서
 * 정해 {@link ResponseEntity} 에 직접 싣는다.
 *
 * <p>실패는 예외로 던져 {@code GlobalExceptionHandler} 가 래퍼로 바꾼다. <b>성공은 예외가
 * 아니므로 그 핸들러에 닿지 않는다</b> — 공통 처리가 {@code Content-Type} 을 덮어쓰는 일이
 * 없어야 한다(icon-storage.md §3 주의 1).
 */
@RestController
public class ExpendGroupIconController {

    /**
     * 아이콘 조회 경로의 앞부분. <b>이 상수가 단일 출처다.</b>
     *
     * <p>세 곳이 같은 문자열을 알아야 한다 — 이 Controller 의 매핑, 목록·상세가 내려보내는
     * {@code iconUrl}({@code ExpendGroupMapper}), 그리고 인증 실패를 빈 401 로 돌리는
     * {@code RestAuthEntryPoint}. 각자 적어 두면 경로를 바꿀 때 한 곳만 고쳐 놓고
     * "목록이 알려 준 주소로 아이콘이 안 온다"가 된다.
     */
    public static final String ICON_PATH = "/api/v1/expend-groups/icons/";

    private final ExpendGroupIconService iconService;

    public ExpendGroupIconController(ExpendGroupIconService iconService) {
        this.iconService = iconService;
    }

    /**
     * 2.10 아이콘 조회.
     *
     * <p>{@code filename} 은 <b>클라이언트가 보내는 값</b>이라 믿지 않는다. 저장 루트를
     * 벗어나는지는 {@code IconStorage} 가 정규화해서 확인하며, 벗어나면 없는 파일과 같은
     * {@code 3104} 다 — 코드를 갈라 두면 경로 탐색 시도에 서버가 답을 해 주는 셈이 된다.
     */
    @GetMapping(ICON_PATH + "{filename}")
    public ResponseEntity<byte[]> get(@PathVariable String filename) {
        StoredIcon icon = iconService.read(filename);
        return ResponseEntity.ok().contentType(icon.contentType()).body(icon.bytes());
    }
}
