package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * 설정 1건 + 연·월 → <b>월별 내역 1행의 값</b>. 그 계산이 일어나는 단 하나의 자리다.
 *
 * <h2>왜 독립 클래스인가</h2>
 *
 * <p>생성 지점이 <b>셋</b>이다.
 *
 * <table border="1">
 *   <caption>월별 내역을 만드는 API</caption>
 *   <tr><th>API</th><th>근거</th><th>언제</th></tr>
 *   <tr><td>4.5 월별 내역 목록</td><td>FR-406</td><td>그 연·월을 처음 조회할 때</td></tr>
 *   <tr><td>4.8 월별 가계부 목록</td><td>FR-418</td><td>위와 같다</td></tr>
 *   <tr><td>4.9 재작성</td><td>FR-414</td><td>명시적 호출(①생성·②갱신)</td></tr>
 * </table>
 *
 * <p>각자 계산하면 <b>같은 달이 어느 API 로 처음 열렸는지에 따라 값이 갈린다.</b> 특히
 * 말일 보정을 한쪽만 빠뜨리면 같은 달의 결제일이 두 화면에서 다르게 보인다.
 *
 * <h2>참조를 다시 묻지 않는다 (FR-426)</h2>
 *
 * <p><b>설정이 들고 있는 {@code payment_method_idx}·{@code expend_group_idx} 를 그대로
 * 복사한다.</b> 그 수단·유형이 지금 사용 안 함({@code in_use=false})이거나 삭제
 * 표시({@code deleted=true})여도 생성을 막지 않는다.
 *
 * <p><b>여기서 {@link ReferenceResolver} 를 부르면 안 된다.</b> 그러면 이런 일이 벌어진다.
 *
 * <pre>{@code
 * 1. 사용자가 "월세" 고정지출을 만든다 (지출유형: 주거)
 * 2. "주거" 유형으로 일반 지출은 한 번도 적지 않는다
 * 3. 사용자가 "주거" 유형을 삭제한다
 *    → 003 의 ExpendGroupService.delete 는 tbl_expense 참조만 본다.
 *      고정지출 참조는 보지 않으므로 막히지 않고 삭제 표시된다.
 * 4. 다음 달 가계부를 연다
 *    → 생성 대상에 "월세"가 들어오는데 유형이 사용 불가다
 *    → requireUsableExpendGroup 이 3103 을 던진다
 *    → 평범한 달 조회가 실패하고, 사용자는 그 달을 영영 열 수 없다
 * }</pre>
 *
 * <p>사용자는 아무것도 잘못하지 않았다. 그래서 <b>자동으로 일어나는 일</b>(생성·재작성)은
 * 사용자가 이미 정해 둔 참조를 그대로 펼치고, <b>사용자가 값을 직접 고르는 경로</b>
 * (4.1 등록 · 4.4 수정 · 4.6 단건 수정)에서만 검증한다. 그쪽은 죽은 참조로 갈아타는 것을
 * 막아야 하므로 반대 방향이 맞다.
 *
 * <p>참조 <b>행 자체</b>가 사라지는 경우는 없다 — 수단·지출유형은 물리 삭제하지 않고
 * 삭제 표시만 한다(헌장 DB 저장 구조 규칙). 그래서 이름은 언제나 읽힌다.
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/monthly-lifecycle.md">monthly-lifecycle.md §1</a>
 */
@Component
public class FixedExpenseMonthlyFactory {

    /**
     * 그 달 행에 넣을 값 묶음.
     *
     * <p><b>Entity 가 아니라 값이다.</b> 쓰는 쪽이 둘로 갈리기 때문이다 — 생성은
     * {@code insertIfAbsent} 네이티브 INSERT 에 파라미터로 넘기고(Entity 를 만들지 않는다),
     * 재작성의 ②갱신은 이미 있는 Entity 의 컬럼에 옮겨 담는다. 어느 쪽이든 <b>값을
     * 정하는 규칙은 하나</b>여야 한다.
     *
     * <p>{@code modified} 가 없는 것은 이 클래스가 정하지 않기 때문이다. 생성은 언제나
     * {@code false} 이고(INSERT 문에 박혀 있다), 재작성의 {@code overwriteModified} 처리는
     * 그 서비스의 정책이다.
     *
     * @param amount           그 달 금액. 설정의 기본 금액을 그대로 쓴다
     * @param paymentDate      <b>말일 보정이 끝난</b> 결제일
     * @param content          그 달 내용
     * @param paymentMethodIdx 수단 참조. 사용 가능 여부를 묻지 않고 복사한 값이다
     * @param expendGroupIdx   지출유형 참조. 위와 같다
     */
    public record MonthlyValues(Long amount, LocalDate paymentDate, String content,
                                Long paymentMethodIdx, Long expendGroupIdx) {
    }

    /**
     * 설정을 그 연·월로 펼친다.
     *
     * <p>결제일만 계산이 들어가고 나머지는 그대로 복사다. 계산은
     * {@link YearMonthValue#paymentDateOn(int)} 이 하며 <b>여기서 한 번만</b> 일어난다 —
     * 결과를 저장하므로 조회 때 다시 계산할 일이 없다(FR-409).
     *
     * <p><b>기간 판정은 하지 않는다.</b> 이 설정이 그 달에 걸리는지는 호출자가
     * {@code findApplicableTo} 로 이미 걸렀다(FR-408). 여기서 또 보면 같은 판정이 두 곳이 된다.
     *
     * @param setting   원본 설정. {@code payment_day_of_month} 는 CHECK
     *                  {@code ck_fixed_expense_day} 로 1~31 이 보장된다
     * @param yearMonth 펼칠 연·월
     */
    public MonthlyValues from(UserFixedExpense setting, YearMonthValue yearMonth) {
        return new MonthlyValues(
                setting.getAmount(),
                yearMonth.paymentDateOn(setting.getPaymentDayOfMonth()),
                setting.getContent(),
                // LAZY 프록시라도 식별자는 로딩 없이 읽힌다. 이름을 읽으면 그때 쿼리가 나가는데
                // 여기서는 이름이 필요 없다 — 월별 내역에 이름 컬럼이 없기 때문이다(FR-405).
                setting.getPaymentMethod().getIdx(),
                setting.getExpendGroup().getIdx());
    }
}
