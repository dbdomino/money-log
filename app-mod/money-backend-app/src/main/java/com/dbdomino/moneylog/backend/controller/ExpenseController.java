package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.ExpenseCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.InstallmentCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.ExpenseCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpenseDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpenseResponse;
import com.dbdomino.moneylog.backend.dto.response.InstallmentCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.InstallmentSettleResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ExpenseService;
import com.dbdomino.moneylog.backend.service.InstallmentService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지출 API — 3.1~3.6.
 *
 * <p><b>할부도 지출이라 같은 경로 아래에 둔다.</b> 3.5·3.6 만 그룹 단위 연산이라
 * {@code InstallmentService} 가 맡고, 할부 회차의 <b>조회·수정·삭제는 3.2·3.3·3.4</b> 가
 * 일시불과 똑같이 처리한다(US3 시나리오 5·6) — 자원을 나누면 화면이 "이 지출이 할부인지"를
 * 먼저 알아야 어느 API 를 부를지 정할 수 있게 된다.
 *
 * <p>경로에 회원 식별자를 두지 않는다. 대상 회원은 토큰이 정하며 요청이 지정할 수
 * 없다(FR-301). 경로가 지시하는 것은 <b>지출</b>뿐이고, 그것이 본인 것인지는 서비스가
 * 조회 조건으로 건다.
 *
 * <p>인가 애너테이션을 붙이지 않는다 — 002 의 {@code SecurityFilterChain} 이 이 경로를
 * {@code authenticated} 로 이미 덮는다.
 *
 * <p>Controller 는 Service 만 부른다(헌장 원칙 II). 예외를 잡지 않는다 — 실패 응답 변환은
 * 전역 예외 처리의 몫이다(원칙 III). <b>{@code PUT} 을 쓰지 않는다.</b>
 */
@RestController
@RequestMapping(value = "/api/v1/expenses", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExpenseController {

    private final ExpenseService expenseService;
    private final InstallmentService installmentService;

    public ExpenseController(ExpenseService expenseService,
                             InstallmentService installmentService) {
        this.expenseService = expenseService;
        this.installmentService = installmentService;
    }

    /**
     * 3.1 지출 등록.
     *
     * <p>응답은 <b>생성 PK 한 칸</b>이다. 상세는 3.2 로 읽는다 — 방금 보낸 값을 그대로
     * 되돌려 받는 것보다 화면이 필요할 때 읽는 편이 낫다는 명세의 판단이다.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<ExpenseCreateResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ExpenseCreateRequest request) {
        return RestResponseDto.ok(expenseService.create(principal, request));
    }

    /**
     * 3.5 할부 등록. 한 번의 요청이 <b>{@code installmentMonths} 개의 지출 행</b>을 만든다.
     *
     * <p><b>{@code /installments} 와 {@code /{expenseId}} 는 같은 깊이다.</b> 스프링이
     * 리터럴을 템플릿보다 먼저 고르므로 순서를 바꿔도 동작은 같지만, 그 우선순위는 코드에
     * 보이지 않는 규칙이라 리터럴을 위에 둔다 — 003 의 {@code /active} 와 같은 상황이다.
     * 매핑이 어긋나면 {@code installments} 가 {@code Long} 변환에 실패해 {@code 9001} 이
     * 나가므로, 3.5 의 실패가 {@code 3204} 가 아니라 {@code 9001} 이면 이 자리를 의심한다.
     */
    @PostMapping(value = "/installments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<InstallmentCreateResponse> createInstallment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody InstallmentCreateRequest request) {
        return RestResponseDto.ok(installmentService.create(principal, request));
    }

    /**
     * 3.6 중도상환 — 남은 회차를 정리한다.
     *
     * <p><b>{@code DELETE} 가 아니라 {@code PATCH} 다</b>(FR-316). 삭제되는 것이
     * {@code installmentGroupId} 가 가리키는 자원이 아니라 <b>그 그룹의 미래 회차 일부</b>
     * 이기 때문이다 — {@code DELETE} 로 표현하면 "그룹을 지운다"로 읽혀 과거 회차까지
     * 사라지는 것으로 오해된다. 실제로 남는 것은 {@code payment_date <= today} 인 회차다.
     *
     * <p>Body 를 받지 않는다. 처리 대상은 경로의 그룹 식별자만으로 정해지고, 기준일은
     * <b>서버의 오늘</b>이라 클라이언트가 지정할 수 없다.
     */
    @PatchMapping("/installments/{installmentGroupId}/remainder")
    public RestResponseDto<InstallmentSettleResponse> settleInstallmentRemainder(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long installmentGroupId) {
        return RestResponseDto.ok(
                installmentService.settleRemainder(principal, installmentGroupId));
    }

    /** 3.2 상세 조회. 할부 회차도 <b>같은 API</b> 로 읽힌다(US3 시나리오 5). */
    @GetMapping("/{expenseId}")
    public RestResponseDto<ExpenseResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expenseId) {
        return RestResponseDto.ok(expenseService.get(principal, expenseId));
    }

    /**
     * 3.3 지출 수정.
     *
     * <p>Body 를 {@code Map} 으로 받는다. 레코드로 받으면 Jackson 이 "보내지 않은 필드"와
     * "{@code null} 을 보낸 필드"를 똑같이 {@code null} 로 만들어 버리는데, 이 API 에서는
     * 그 구분이 <b>스냅샷 갱신 여부를 가른다</b> — 참조를 안 보낸 수정은 이름을 건드리지
     * 않아야 한다(FR-304). 1.8·2.4 와 같은 이유다.
     *
     * <p>응답은 <b>갱신된 Expense 전체</b>다. 등록과 다른데, 스냅샷이 실제로 갱신됐는지를
     * 화면이 바로 확인해야 하기 때문이다.
     */
    @PatchMapping(value = "/{expenseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<ExpenseResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expenseId,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(expenseService.update(principal, expenseId, body));
    }

    /**
     * 3.4 지출 삭제 — <b>물리 삭제</b>다(FR-308).
     *
     * <p>003 의 {@code DELETE} 가 삭제 표시(UPDATE)였던 것과 정반대다. 할부 건이어도
     * 그 달 1건만 지운다(FR-313).
     */
    @DeleteMapping("/{expenseId}")
    public RestResponseDto<ExpenseDeleteResponse> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expenseId) {
        return RestResponseDto.ok(expenseService.delete(principal, expenseId));
    }
}
