package com.dbdomino.moneylog.front.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 화면 모듈이 백엔드로 나가는 <b>유일한 통로</b>.
 *
 * <p>008~012 는 이 클래스만 보고 호출하며 {@code resCode} 를 직접 보지 않는다. 성공이면
 * 값이 오고, 아니면 예외가 온다.
 *
 * <h2>봉투를 푸는 자리가 하나다</h2>
 *
 * <p>백엔드는 성공·실패를 같은 봉투에 담아 답하고 <b>실패도 HTTP 200</b> 이다(헌장 원칙
 * III). 화면마다 이걸 풀면 어느 화면 하나가 코드 확인을 빠뜨렸을 때 실패가 성공으로 읽히고
 * 빈 화면이 정상처럼 뜬다. 그래서 푸는 자리를 이 클래스 하나로 묶는다.
 *
 * <h2>PUT 이 없다</h2>
 *
 * <p>규칙을 검증으로 세는 대신 <b>부를 수 없게</b> 했다. {@code put()} 메서드가 없으면
 * 호출할 방법이 없고, 시험으로 위반 건수를 셀 이유도 사라진다. GET 도 같은 이유로 Path 용과
 * Query 용 시그니처를 나눴다 — 혼용 금지를 주석이 아니라 형태로 막는다.
 */
@Component
public class BackendApiClient {

    private static final Logger log = LoggerFactory.getLogger(BackendApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BackendApiClient(RestClient backendRestClient, ObjectMapper objectMapper) {
        this.restClient = backendRestClient;
        this.objectMapper = objectMapper;
    }

    // ── 호출 ────────────────────────────────────────────────────────────

    /**
     * Path 만 쓰는 조회. Query 를 섞지 않는다.
     *
     * @param pathTemplate {@code /payment-methods/{idx}} 처럼 자리표시자를 쓴 경로
     * @param pathVariables 자리표시자에 채울 값
     */
    public <T> T get(String pathTemplate, Class<T> type, Object... pathVariables) {
        rejectQueryString(pathTemplate);
        return exchangeEnvelope(HttpMethod.GET, pathTemplate, null, null, null, type, pathVariables);
    }

    /**
     * Query 만 쓰는 조회. 경로에 자리표시자를 두지 않는다.
     *
     * <p>목록 조회가 이쪽이다. {@code offset}·{@code limit} 은 {@link com.dbdomino.moneylog.front.web.Paging}
     * 이 만들어 넘긴다 — 화면이 직접 계산하면 배수가 아닌 값이 새어 나간다.
     */
    public <T> T getByQuery(String path, Map<String, ?> query, Class<T> type) {
        rejectPathVariables(path);
        rejectQueryString(path);
        return exchangeEnvelope(HttpMethod.GET, path, query, null, null, type);
    }

    /**
     * Path 와 Query 를 함께 쓰는 <b>유일한 조회</b> — 월별 통계({@code StatisticsMonthlyGet})다.
     *
     * <p>연·월은 Path 로, 저장본이냐 즉시 계산이냐를 가르는 값은 Query 로 받는다. 다른
     * 호출이 이 메서드를 쓰면 혼용 금지가 무너지므로 새 호출을 여기 얹지 않는다.
     */
    public <T> T getStatisticsMonthly(String pathTemplate, Map<String, ?> query, Class<T> type,
            Object... pathVariables) {
        rejectQueryString(pathTemplate);
        return exchangeEnvelope(HttpMethod.GET, pathTemplate, query, null, null, type, pathVariables);
    }

    /** 생성. Body 만 쓴다 — 경로에 자리표시자도 Query 도 두지 않는다. */
    public <T> T post(String path, Object body, Class<T> type) {
        rejectPathVariables(path);
        rejectQueryString(path);
        return exchangeEnvelope(HttpMethod.POST, path, null, body, MediaType.APPLICATION_JSON, type);
    }

    /**
     * 파일을 실어 보내는 생성. 아이콘 업로드(009)와 엑셀 일괄 등록(010)이 쓴다.
     *
     * <p>multipart 는 POST 에만 둔다. 수정(PATCH)에 파일을 실을 일이 생기면 그때 백엔드
     * 계약부터 정하고 여기 더한다.
     */
    public <T> T postMultipart(String path, MultiValueMap<String, ?> parts, Class<T> type) {
        rejectPathVariables(path);
        rejectQueryString(path);
        return exchangeEnvelope(HttpMethod.POST, path, null, parts, MediaType.MULTIPART_FORM_DATA, type);
    }

    /**
     * 수정. Path 로 키를, Body 로 <b>바뀐 항목만</b> 보낸다.
     *
     * <p>보내지 않은 항목은 백엔드가 그대로 유지한다. 화면이 빈 값을 채워 보내면 그것은
     * "비워 달라"는 요청이 되므로, 폼에서 건드리지 않은 칸은 실어 보내지 않는다.
     */
    public <T> T patch(String pathTemplate, Object body, Class<T> type, Object... pathVariables) {
        rejectQueryString(pathTemplate);
        return exchangeEnvelope(HttpMethod.PATCH, pathTemplate, null, body, MediaType.APPLICATION_JSON,
                type, pathVariables);
    }

    /** 삭제. Path 만 쓰고 Body 를 싣지 않는다. */
    public void delete(String pathTemplate, Object... pathVariables) {
        rejectQueryString(pathTemplate);
        exchangeEnvelope(HttpMethod.DELETE, pathTemplate, null, null, null, null, pathVariables);
    }

    /**
     * 본문이 파일인 조회. 아이콘(003 의 2.10)과 엑셀 양식(004 의 3.11) 둘뿐이다.
     *
     * <p><b>Content-Type 을 보고 자동으로 갈라지지 않는다.</b> 호출부가 이 메서드를 부르는
     * 것이 곧 "이건 파일이다"라는 선언이다. 자동 판정은 두 방향으로 틀린다 — 아이콘 요청에
     * 실패 봉투가 오면 JSON 이라 봉투로 읽히는데 호출부는 바이트를 기다리고 있고, 반대로
     * 바이트가 와야 할 자리에 JSON 이 오면 조용히 바이트로 넘어간다.
     *
     * <p>그래서 <b>이 호출에 봉투가 오면 그것은 실패다.</b> 실패 봉투면 그 코드를 그대로
     * 올리고, 성공 봉투면 계약이 어긋난 것이므로 닿지 못한 것으로 본다.
     */
    public BinaryPayload getBinary(String pathTemplate, Object... pathVariables) {
        rejectQueryString(pathTemplate);
        long startNanos = System.nanoTime();
        RawResponse raw = execute(HttpMethod.GET, pathTemplate, null, null, null, pathVariables);

        if (isJson(raw.contentType())) {
            ApiEnvelope envelope = parseEnvelope(raw);
            logFailure(HttpMethod.GET, pathTemplate, envelope, startNanos);
            if (envelope.isSuccess()) {
                throw new BackendUnavailableException(
                        "파일을 기다린 호출에 성공 봉투가 왔습니다. 백엔드 응답 규격이 어긋났습니다.");
            }
            throw new BackendApiException(envelope.resCode(), envelope.message());
        }

        if (raw.body() == null || raw.body().length == 0) {
            throw new BackendUnavailableException("백엔드가 빈 본문을 돌려주었습니다.");
        }
        log.info("백엔드 호출 {} {} bytes={} {}ms", HttpMethod.GET, pathTemplate, raw.body().length,
                elapsedMillis(startNanos));
        return new BinaryPayload(raw.body(), contentTypeValue(raw), filenameOf(raw.contentDisposition()));
    }

    // ── 봉투 해석 ───────────────────────────────────────────────────────

    private <T> T exchangeEnvelope(HttpMethod method, String pathTemplate, Map<String, ?> query,
            Object body, MediaType contentType, Class<T> type, Object... pathVariables) {
        long startNanos = System.nanoTime();
        try {
            RawResponse raw = execute(method, pathTemplate, query, body, contentType, pathVariables);
            ApiEnvelope envelope = parseEnvelope(raw);

            if (!envelope.isSuccess()) {
                logFailure(method, pathTemplate, envelope, startNanos);
                throw new BackendApiException(envelope.resCode(), envelope.message());
            }

            log.info("백엔드 호출 {} {} resCode={} {}ms", method, pathTemplate, envelope.resCode(),
                    elapsedMillis(startNanos));
            return convert(envelope.data(), type);

        } catch (BackendUnavailableException e) {
            log.warn("백엔드 미도달 {} {} {}ms: {}", method, pathTemplate, elapsedMillis(startNanos),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 응답 본문을 봉투로 읽는다. 봉투가 아니면 닿지 못한 것으로 본다.
     *
     * <p>{@code resCode} 가 없는 JSON 도 봉투가 아니다. 그대로 두면 코드가 {@code 0} 인
     * 실패로 읽혀 화면에 뜻 모를 숫자가 나간다.
     */
    private ApiEnvelope parseEnvelope(RawResponse raw) {
        if (raw.body() == null || raw.body().length == 0) {
            throw new BackendUnavailableException("백엔드가 빈 본문을 돌려주었습니다.");
        }
        ApiEnvelope envelope;
        try {
            envelope = objectMapper.readValue(raw.body(), ApiEnvelope.class);
        } catch (JacksonException e) {
            throw new BackendUnavailableException("백엔드 응답이 규격 봉투가 아닙니다.", e);
        }
        if (envelope == null || envelope.resCode() == 0) {
            throw new BackendUnavailableException("백엔드 응답에 resCode 가 없습니다.");
        }
        return envelope;
    }

    /**
     * 봉투 안의 값을 호출부가 요청한 타입으로 바꾼다.
     *
     * <p>변환에 실패하면 화면은 어차피 데이터를 얻지 못한 것이므로 닿지 못한 것과 같이
     * 다룬다. 코드를 지어내지 않는다(FR-607).
     */
    private <T> T convert(JsonNode data, Class<T> type) {
        if (type == null || type == Void.class) {
            return null;
        }
        if (data == null || data.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(data, type);
        } catch (JacksonException e) {
            throw new BackendUnavailableException(
                    "백엔드 응답의 data 를 " + type.getSimpleName() + " 로 읽지 못했습니다.", e);
        }
    }

    // ── 전송 ────────────────────────────────────────────────────────────

    private RawResponse execute(HttpMethod method, String pathTemplate, Map<String, ?> query,
            Object body, MediaType contentType, Object... pathVariables) {
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(uriBuilder -> {
                uriBuilder.path(pathTemplate);
                if (query != null) {
                    query.forEach((name, value) -> {
                        if (value != null) {
                            uriBuilder.queryParam(name, value);
                        }
                    });
                }
                return uriBuilder.build(pathVariables);
            });

            RestClient.RequestHeadersSpec<?> ready = spec;
            if (body != null) {
                ready = spec.contentType(contentType).body(body);
            }

            return ready.exchange((request, response) -> new RawResponse(
                    response.getBody().readAllBytes(),
                    response.getHeaders().getContentType(),
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)));

        } catch (ResourceAccessException e) {
            // 연결 거부·타임아웃, 그리고 본문을 읽는 도중 끊긴 경우가 전부 여기로 온다.
            // RestClient 가 I/O 오류를 이 하나로 묶어 주므로 따로 잡을 것이 없다.
            throw new BackendUnavailableException("백엔드에 닿지 못했습니다.", e);
        }
    }

    /** 응답에서 꺼낸 날것. 본문·Content-Type·Content-Disposition 셋만 쓴다. */
    private record RawResponse(byte[] body, MediaType contentType, String contentDisposition) {
    }

    // ── 로그 (원칙 IV) ──────────────────────────────────────────────────

    /**
     * 실패 한 건을 남긴다.
     *
     * <p><b>남기는 것은 경로 템플릿이다.</b> 채워진 주소를 남기면 식별자와 Query 값이 로그로
     * 흘러 들어간다. 인증 헤더·토큰 필드·비밀번호는 애초에 이 클래스가 로그로 넘기지 않는다.
     */
    private void logFailure(HttpMethod method, String pathTemplate, ApiEnvelope envelope, long startNanos) {
        log.warn("백엔드 호출 실패 {} {} resCode={} {}ms message={}", method, pathTemplate,
                envelope.resCode(), elapsedMillis(startNanos), envelope.message());
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    // ── 호출 규칙 (FR-609) ──────────────────────────────────────────────

    private static void rejectQueryString(String path) {
        if (path.indexOf('?') >= 0) {
            throw new IllegalArgumentException(
                    "경로에 Query 를 직접 붙이지 않는다. Query 를 받는 메서드를 쓴다: " + path);
        }
    }

    private static void rejectPathVariables(String path) {
        if (path.indexOf('{') >= 0) {
            throw new IllegalArgumentException(
                    "이 호출은 경로 자리표시자를 쓰지 않는다. Path 를 받는 메서드를 쓴다: " + path);
        }
    }

    // ── 파일 응답 도우미 ────────────────────────────────────────────────

    private static boolean isJson(MediaType contentType) {
        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
    }

    private static String contentTypeValue(RawResponse raw) {
        return raw.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : raw.contentType().toString();
    }

    /** {@code attachment; filename="양식.xlsx"} 에서 파일명만 꺼낸다. 없으면 {@code null}. */
    private static String filenameOf(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }
        int start = contentDisposition.indexOf("filename=");
        if (start < 0) {
            return null;
        }
        String value = contentDisposition.substring(start + "filename=".length()).trim();
        int semicolon = value.indexOf(';');
        if (semicolon >= 0) {
            value = value.substring(0, semicolon);
        }
        return value.replace("\"", "").trim();
    }
}
