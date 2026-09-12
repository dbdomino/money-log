package com.dbdomino.moneylog.front.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 옛 주소로 들어온 사용자를 지금의 화면으로 보낸다.
 *
 * <p>대상이 두 갈래다. <b>실제로 매핑돼 있던 레거시 주소</b>와, 두 기획 문서가 예로 들어
 * 사용자가 손으로 칠 법한 <b>등록 전용 페이지 관례 주소</b>다. 뒤의 것들은 실재한 적이 없지만
 * 북마크와 링크로 들어올 수 있다.
 *
 * <h2>되살리지 않는 것</h2>
 *
 * <p>옛 로그인 제출 주소는 본문 형식이 백엔드 계약과 달라 그대로 보낼 수 없다. 옛 조회 주소는
 * 화면 주소가 아니라 애초에 redirect 대상이 아니다 — <b>화면 모듈이 조회 주소를 내놓으면
 * 브라우저가 화면 모듈을 데이터 서버처럼 부르는 길이 생긴다.</b> 007 이 만드는 화면 밖
 * 엔드포인트는 아이콘 중계 하나뿐이고 그것도 화면 자원이다.
 *
 * <h2>영구 이동인 이유</h2>
 *
 * <p>목적지가 고정이라 브라우저가 기억해도 문제가 없다. 루트 주소만 임시 이동인데, 그쪽은
 * 로그인 상태에 따라 목적지가 갈리기 때문이다.
 *
 * <p>여기 없는 주소는 보내지 않는다. 목록에 없으면 그냥 없는 주소다.
 */
@Controller
public class LegacyUrlController {

    // ── 실재했던 레거시 주소 ────────────────────────────────────────────

    /** 옛 로그인 페이지. */
    @GetMapping("/mem/login")
    public ResponseEntity<Void> legacyLogin() {
        return movedTo(AuthInterceptor.LOGIN_URL);
    }

    /** 옛 홈. 루트로 보내 로그인 상태에 따른 분기를 다시 타게 한다. */
    @GetMapping("/mem/ind")
    public ResponseEntity<Void> legacyHome() {
        return movedTo("/");
    }

    // ── 수단 (2.2~2.4) ──────────────────────────────────────────────────

    @GetMapping("/payments/new")
    public ResponseEntity<Void> paymentCreate() {
        return modalOf("/payments", "create", null);
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<Void> paymentDetail(@PathVariable String id) {
        return modalOf("/payments", "detail", id);
    }

    @GetMapping("/payments/{id}/edit")
    public ResponseEntity<Void> paymentEdit(@PathVariable String id) {
        return modalOf("/payments", "edit", id);
    }

    // ── 지출유형 (2.6~2.8) ──────────────────────────────────────────────

    @GetMapping("/expend-groups/new")
    public ResponseEntity<Void> expendGroupCreate() {
        return modalOf("/expend-groups", "create", null);
    }

    /**
     * 지출유형 상세.
     *
     * <p>아이콘 중계 주소({@code /expend-groups/icons/{파일명}})는 여기 걸리지 않는다. 칸 수가
     * 달라 애초에 겹치지 않고, 겹치더라도 고정 구간이 긴 쪽이 먼저 잡힌다. 순서를 잘못 두면
     * 아이콘 요청이 상세 모달로 새어 나가는데, 증상이 "아이콘이 하나도 안 보인다"라 원인이
     * 주소 매핑이라는 것을 짐작하기 어렵다. 그래서 시험으로 고정해 둔다.
     */
    @GetMapping("/expend-groups/{id}")
    public ResponseEntity<Void> expendGroupDetail(@PathVariable String id) {
        return modalOf("/expend-groups", "detail", id);
    }

    @GetMapping("/expend-groups/{id}/edit")
    public ResponseEntity<Void> expendGroupEdit(@PathVariable String id) {
        return modalOf("/expend-groups", "edit", id);
    }

    // ── 지출·소득 (3.1~3.4) ─────────────────────────────────────────────

    @GetMapping("/ledger/expenses/new")
    public ResponseEntity<Void> expenseCreate() {
        return modalOf("/ledger", "expense-create", null);
    }

    @GetMapping("/ledger/expenses/{id}/edit")
    public ResponseEntity<Void> expenseEdit(@PathVariable String id) {
        return modalOf("/ledger", "expense-edit", id);
    }

    @GetMapping("/ledger/incomes/new")
    public ResponseEntity<Void> incomeCreate() {
        return modalOf("/ledger", "income-create", null);
    }

    @GetMapping("/ledger/incomes/{id}/edit")
    public ResponseEntity<Void> incomeEdit(@PathVariable String id) {
        return modalOf("/ledger", "income-edit", id);
    }

    // ── 고정지출 (4.3~4.6) ──────────────────────────────────────────────

    @GetMapping("/fixed-expenses/new")
    public ResponseEntity<Void> fixedExpenseCreate() {
        return modalOf("/fixed-expenses", "create", null);
    }

    /** 월별 내역. 식별자 자리보다 먼저 잡히도록 고정 구간으로 둔다. */
    @GetMapping("/fixed-expenses/monthly")
    public ResponseEntity<Void> fixedExpenseMonthly() {
        return modalOf("/fixed-expenses", "monthly", null);
    }

    @GetMapping("/fixed-expenses/{id}")
    public ResponseEntity<Void> fixedExpenseDetail(@PathVariable String id) {
        return modalOf("/fixed-expenses", "detail", id);
    }

    @GetMapping("/fixed-expenses/{id}/edit")
    public ResponseEntity<Void> fixedExpenseEdit(@PathVariable String id) {
        return modalOf("/fixed-expenses", "edit", id);
    }

    // ── 회원 관리 (1.9~1.10) ────────────────────────────────────────────

    @GetMapping("/admin/members/new")
    public ResponseEntity<Void> adminMemberCreate() {
        return modalOf("/admin/members", "create", null);
    }

    @GetMapping("/admin/members/{id}/edit")
    public ResponseEntity<Void> adminMemberEdit(@PathVariable String id) {
        return modalOf("/admin/members", "edit", id);
    }

    // ── 주소 만들기 ─────────────────────────────────────────────────────

    /**
     * 부모 목록 주소에 모달 딥링크를 붙인다.
     *
     * <p>식별자가 비었으면 딥링크 없이 부모 페이지로만 보낸다. 값 없는 상세 모달을 열어 봐야
     * 무엇을 보여 줄지 정할 수 없고, 사용자는 목록에서 다시 고르면 된다.
     */
    private static ResponseEntity<Void> modalOf(String parent, String modal, String id) {
        if (id != null && id.isBlank()) {
            return movedTo(parent);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(parent).queryParam("m", modal);
        if (id != null) {
            builder.queryParam("id", id);
        }
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(builder.build().toUriString()))
                .build();
    }

    private static ResponseEntity<Void> movedTo(String target) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(target))
                .build();
    }
}
