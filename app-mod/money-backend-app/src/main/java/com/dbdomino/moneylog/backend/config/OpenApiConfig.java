package com.dbdomino.moneylog.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 와 {@code /v3/api-docs} 의 문서 머리말.
 *
 * <h2>읽는 사람이 먼저 알아야 하는 것</h2>
 *
 * <p>이 API 는 <b>실패도 HTTP 200 으로 돌려준다.</b> 성공·실패가 모두
 * {@code { resCode, data }} 한 형태이고 실패는 {@code resCode} 의 4자리 숫자로 구분한다
 * (헌장 원칙 III). Swagger UI 에서 "200 OK" 를 보고 성공으로 읽으면 안 되며
 * <b>본문의 {@code resCode} 를 봐야 한다</b> — 그래서 그 설명을 문서 설명문 맨 앞에 둔다.
 *
 * <h2>왜 스키마를 여기서 손대지 않는가</h2>
 *
 * <p>경로·파라미터·응답 스키마는 springdoc 이 컨트롤러와 DTO 에서 <b>읽어서</b> 만든다.
 * 여기에 손으로 적으면 코드가 바뀔 때 문서만 남아 갈린다 — 그 갈림이 정확히
 * {@code app-mod/money-backend-app/openapi.yaml}(사람이 쓴 계약 문서)과 이 실행 문서를
 * 나눠 둔 이유이기도 하다. 둘의 역할은 {@code docs/API-문서.md} 에 적어 두었다.
 *
 * @see <a href="../../../../../../../../../docs/API-문서.md">API-문서.md</a>
 */
@Configuration
public class OpenApiConfig {

    /** {@code Authorize} 버튼이 만들 헤더의 이름. 스킴 이름이자 참조 키다. */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI moneylogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("money-log 백엔드 API")
                        .version("v1")
                        .description("""
                                가계부 백엔드 REST API.

                                ## 응답 규격 — 실패도 HTTP 200 이다

                                성공·실패 모두 `{ "resCode": <숫자>, "data": { ... } }` 한 형태다.
                                성공은 `resCode: 200`, 비즈니스·검증 실패는 **정수 4자리** 에러코드다.
                                HTTP 상태만 보고 성공으로 읽으면 안 되고 본문의 `resCode` 를 봐야 한다.

                                실패 응답의 `data` 는 `{ "message": "..." }` 한 칸이며,
                                엑셀 행 검증 실패(`3502`)만 `errors[]` 를 더 싣는다.

                                ## 인증

                                로그인(`POST /api/v1/auth/login`)이 주는 `accessToken` 을
                                오른쪽 위 **Authorize** 에 넣으면 이후 호출에 `Bearer` 로 붙는다.
                                Access 토큰은 1일, Refresh 는 7일이며 회원당 활성 세션은 1건이라
                                새 로그인이 기존 세션을 폐기한다.

                                ## 에러코드 대역

                                | 대역 | 자원 |
                                |---|---|
                                | `1001`·`1002` | 인증·인가 |
                                | `11xx` | 회원·인증 |
                                | `30xx` | 지출 수단 |
                                | `31xx` | 지출유형 |
                                | `32xx`·`33xx` | 지출 · 소득 |
                                | `34xx` | 고정지출 |
                                | `35xx` | 월별 가계부 · 엑셀 |
                                | `9000`·`9001` | 서버 오류 · 잘못된 요청 |

                                ## HTTP 메서드

                                GET 조회 · POST 생성 · PATCH 수정(보내지 않은 필드는 유지) ·
                                DELETE 삭제. **PUT 은 쓰지 않는다.**
                                """))
                // 이름을 붙여 둬야 프론트가 "그 서버"를 고를 수 있다. 배포 URL 이 생기면 여기 늘린다.
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("로컬 개발 서버")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 accessToken. `Bearer ` 접두사는 UI 가 붙인다.")))
                // 기본이 "인증 필요"다 — SecurityConfig 의 anyRequest().authenticated() 와 같은 방향이라
                // 새 API 가 붙어도 문서가 저절로 보수적으로 표시된다. 공개 API 는 컨트롤러에서
                // @SecurityRequirements 로 덜어낸다.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
