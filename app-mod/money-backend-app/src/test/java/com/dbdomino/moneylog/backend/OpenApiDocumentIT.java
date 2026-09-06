package com.dbdomino.moneylog.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;

/**
 * springdoc 이 만드는 OpenAPI 문서가 실제 API 를 빠짐없이 담는가.
 *
 * <p><b>문서가 조용히 비는 것을 막는 시험이다.</b> springdoc 은 설정이 어긋나도 예외를
 * 던지지 않고 <b>빈 {@code paths} 를 돌려준다</b> — {@code paths-to-match} 를 잘못 적거나
 * 컴포넌트 스캔 범위가 어긋나면 문서가 "정상 응답"으로 비어서 나온다. 그러면 아무도
 * 모르는 채 리뷰용 문서가 쓸모없어진다.
 *
 * <p>그래서 <b>건수와 대표 경로를 함께</b> 단언한다. 건수만 보면 엉뚱한 경로가 들어와도
 * 통과하고, 경로만 보면 새 API 가 빠져도 통과한다.
 */
class OpenApiDocumentIT extends AbstractApiIT {

    private static final String DOCS_URL = "/v3/api-docs";

    /** 이 문서에 반드시 있어야 하는 경로. 기능별로 하나씩 골랐다. */
    private static final List<String> REPRESENTATIVE_PATHS = List.of(
            "/api/v1/auth/login",                      // 002 인증
            "/api/v1/members/me",                      // 002 회원
            "/api/v1/admin/members",                   // 002 관리자
            "/api/v1/payment-methods",                 // 003 수단
            "/api/v1/expend-groups",                   // 003 지출유형
            "/api/v1/expenses",                        // 004 지출
            "/api/v1/incomes",                         // 004 소득
            "/api/v1/expense-incomes/excel/upload");   // 004 엑셀

    private JsonNode fetchDocument() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.get(DOCS_URL))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    @Test
    @DisplayName("토큰 없이도 문서를 읽을 수 있다 — 문서는 공개다")
    void documentIsPublic() throws Exception {
        // 문서를 보려면 토큰이 필요하다면, 토큰 얻는 법을 보려는 첫 사용자가 막힌다.
        assertThat(mockMvc.perform(MockMvcRequestBuilders.get(DOCS_URL))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("문서가 비어 있지 않고 대표 경로를 전부 담는다")
    void documentCoversEveryFeature() throws Exception {
        JsonNode paths = fetchDocument().get("paths");

        assertThat(paths).as("paths 가 없다 — springdoc 설정이 어긋났다").isNotNull();
        List<String> documented = new ArrayList<>();
        paths.propertyNames().forEach(documented::add);

        assertThat(documented).containsAll(REPRESENTATIVE_PATHS);
        // 004 까지 37개다. 005 가 9건을 더하면 이 하한도 함께 올린다.
        assertThat(documented).as("경로 수가 갑자기 줄었다면 스캔 범위를 의심한다")
                .hasSizeGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("Bearer 인증 스킴이 정의되어 있다 — Authorize 버튼이 이것으로 뜬다")
    void bearerSchemeIsDeclared() throws Exception {
        JsonNode scheme = fetchDocument()
                .get("components").get("securitySchemes").get("bearerAuth");

        assertThat(scheme).isNotNull();
        assertThat(scheme.get("scheme").asString()).isEqualTo("bearer");
        assertThat(scheme.get("bearerFormat").asString()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("PUT 을 쓰는 API 가 하나도 없다 — 헌장 원칙 III")
    void noPutOperations() throws Exception {
        JsonNode paths = fetchDocument().get("paths");

        List<String> withPut = new ArrayList<>();
        paths.propertyNames().forEach(path -> {
            if (paths.get(path).has("put")) {
                withPut.add(path);
            }
        });

        // 문서가 곧 감시자다 — 누군가 PUT 을 추가하면 코드 리뷰보다 먼저 여기서 걸린다.
        assertThat(withPut).isEmpty();
    }

    @Test
    @DisplayName("생성된 문서를 build/ 에 떨궈 둔다 — 정적 계약 문서와 대조할 때 쓴다")
    void dumpForComparison() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.get(DOCS_URL))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Path out = Path.of("build", "openapi", "generated-api-docs.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, body, StandardCharsets.UTF_8);

        assertThat(Files.size(out)).isPositive();
    }

    /**
     * {@code openapi.yaml} 이 담은 (메서드, 경로) 쌍.
     *
     * <p>SnakeYAML 로 읽는다 — springdoc 이 {@code /v3/api-docs.yaml} 을 내기 위해 이미
     * 클래스패스에 올려 둔 것이라 의존성이 늘지 않는다.
     */
    @SuppressWarnings("unchecked")
    private Set<String> operationsInContractFile() throws Exception {
        Path contract = Path.of("openapi.yaml");
        assertThat(Files.exists(contract))
                .as("app-mod/money-backend-app/openapi.yaml 이 없다").isTrue();

        Map<String, Object> spec;
        try (var in = Files.newInputStream(contract)) {
            spec = new Yaml().load(in);
        }
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        Set<String> operations = new TreeSet<>();
        paths.forEach((path, node) -> ((Map<String, Object>) node).keySet().stream()
                .filter(HTTP_METHODS::contains)
                .forEach(method -> operations.add(method.toUpperCase(Locale.ROOT) + " " + path)));
        return operations;
    }

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete");

    @Test
    @DisplayName("openapi.yaml 이 실제 API 를 빠짐없이 담는다 — 계약 문서가 낡는 것을 막는다")
    void contractFileCoversEveryOperation() throws Exception {
        JsonNode paths = fetchDocument().get("paths");
        Set<String> live = new TreeSet<>();
        paths.propertyNames().forEach(path -> paths.get(path).propertyNames()
                .forEach(method -> live.add(method.toUpperCase(Locale.ROOT) + " " + path)));

        Set<String> documented = operationsInContractFile();

        // 새 API 를 붙이고 openapi.yaml 을 잊으면 여기서 걸린다. 계약 문서는 사람이 쓰므로
        // 그냥 두면 반드시 낡는다 — 낡은 계약서는 없는 것보다 나쁘다.
        assertThat(live)
                .as("openapi.yaml 에 빠진 API 가 있다. 구현했으면 계약 문서도 함께 쓴다")
                .allSatisfy(operation -> assertThat(documented).contains(operation));

        // 반대 방향도 본다 — 지운 API 가 문서에 남아 있으면 프론트가 없는 것을 부른다.
        assertThat(documented)
                .as("openapi.yaml 에만 있는 API 가 있다. 구현에서 사라졌는지 확인한다")
                .allSatisfy(operation -> assertThat(live).contains(operation));
    }
}
