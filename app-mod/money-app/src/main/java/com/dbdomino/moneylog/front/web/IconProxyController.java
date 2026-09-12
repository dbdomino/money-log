package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.client.BackendUnavailableException;
import com.dbdomino.moneylog.front.client.BinaryPayload;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 지출유형 아이콘을 <b>화면 모듈 주소로</b> 내보낸다.
 *
 * <p>브라우저가 백엔드를 직접 부르지 않기로 한 결정의 결과다. 토큰이 서버 세션에만 있으므로
 * 이미지 요청에도 인증을 실을 수 있는 것은 서버뿐이다. 화면은 백엔드 주소가 아니라 이 주소를
 * 이미지에 걸고, 여기서 백엔드에게 받아 바이트 그대로 흘려보낸다.
 *
 * <h2>판정을 건너뛰는 것이 아니다</h2>
 *
 * <p>이 경로는 진입 판정에서 빠진다 — 지출유형이 20개인 화면을 열면 아이콘 요청이 20건
 * 나가는데 그때마다 토큰 확인이 따라붙으면 화면 한 번에 백엔드 왕복이 40건이 된다. 대신
 * 이 컨트롤러가 세션의 토큰을 실어 백엔드를 부르므로 <b>소유자 판정과 토큰 검증은 백엔드가
 * 그대로 한다.</b> 세션이 비어 있으면 토큰 없이 나가 인증 실패를 받고, 이미지를 내보내지
 * 않는다.
 *
 * <h2>매핑 순서</h2>
 *
 * <p>이 경로가 지출유형 상세 redirect({@code /expend-groups/{id}})보다 <b>먼저</b> 잡혀야
 * 한다. 순서가 뒤집히면 아이콘 요청이 상세 모달 redirect 로 새어 나가고, 증상은 "아이콘이
 * 하나도 안 보인다"로만 나타나 원인이 주소 매핑이라는 것을 짐작하기 어렵다. 자리표시자
 * 하나짜리 경로보다 고정 구간이 긴 이 경로가 먼저 매칭되므로 Spring 의 기본 우선순위로
 * 충족되며, 시험으로 고정해 둔다.
 */
@Controller
public class IconProxyController {

    /** 화면이 이미지에 거는 주소. 백엔드의 같은 이름 경로와 자리만 맞춰 둔다. */
    public static final String ICON_PATH = "/expend-groups/icons/{filename}";

    /**
     * 백엔드가 정한 파일명 규칙 {@code {회원키}_{지출유형번호}.{확장자}}.
     *
     * <p>브라우저가 보내는 값이라 믿지 않는다. 이 틀에서 벗어나면 백엔드를 부르지도 않고
     * 끝낸다 — 경로 이탈({@code ..}·{@code /})이 백엔드까지 가지 않게 앞에서 막는다.
     */
    private static final Pattern FILENAME = Pattern.compile("^\\d+_\\d+\\.(png|jpg|gif)$");

    private final BackendApiClient backendApiClient;

    public IconProxyController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    @GetMapping(ICON_PATH)
    public ResponseEntity<byte[]> icon(@PathVariable String filename) {
        if (!FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }

        BinaryPayload payload;
        try {
            payload = backendApiClient.getBinary(ICON_PATH, filename);
        } catch (BackendApiException | BackendUnavailableException e) {
            // 실패를 오류 화면으로 보내지 않는다. 이 주소의 응답은 이미지 하나이고,
            // 화면 안에서 <img> 가 깨지는 것으로 끝나야 한다. 모델·캐시에도 담지 않는다.
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(mediaType(payload.contentType()))
                .body(payload.bytes());
    }

    private static MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
