package com.dbdomino.moneylog.backend.service.statistics;

import org.springframework.stereotype.Component;

/**
 * 적용 목표금액 판정 — <b>{@code 월별 값 ?? 기본 값}</b>(FR-507).
 *
 * <h2>{@code null} 과 {@code 0} 은 다른 상태다 (FR-506)</h2>
 *
 * <p>이 기능에서 가장 조용히 틀리는 지점이다.
 *
 * <table border="1">
 *   <caption>월별 목표의 두 상태</caption>
 *   <tr><th>월별 값</th><th>뜻</th><th>적용 금액</th></tr>
 *   <tr><td>{@code null}</td><td>그 달에 <b>저장한 적 없다</b> — 기본값을 쓰겠다</td>
 *       <td>기본 값</td></tr>
 *   <tr><td>{@code 0}</td><td>그 달엔 <b>쓰지 않겠다</b> — 사용자가 일부러 넣은 값이다</td>
 *       <td><b>{@code 0}</b></td></tr>
 * </table>
 *
 * <p><b>둘을 같게 다루면 0원 목표가 기본값으로 덮인다.</b> "이번 달은 쇼핑 안 한다"고
 * 정한 사용자에게 지난달 한도가 되살아나고, 통계의 사용률·상태가 전부 어긋난다.
 * 예외도 오류 응답도 나지 않아 한참 뒤에 발견된다.
 *
 * <p>그래서 월별 값을 <b>{@code Long}</b> 으로 들고 다닌다. {@code long} 이면
 * {@code null} 을 표현할 수 없어 "없음"이 {@code 0} 으로 뭉개진다.
 *
 * <h2>기본 값은 항상 숫자다</h2>
 *
 * <p>기본 목표를 저장한 적 없으면 <b>{@code 0}</b> 으로 본다({@code null} 이 아니다).
 * 그래서 적용 금액은 <b>언제나 숫자</b>이고 통계의 나눗셈이 성립한다 — 목표가 0 이면
 * 사용률을 0 으로 둔다는 규칙(FR-522)이 그 위에 선다.
 *
 * <h2>왜 세 곳이 이걸 쓰나</h2>
 *
 * <p>5.1 목록 · 5.2 단건 · 5.5 통계의 유형별 요약이 모두 적용 금액을 낸다. 각자 계산하면
 * 목록에 보이는 한도와 통계가 쓰는 한도가 달라진다.
 *
 * @see <a href="../../../../../../../../../specs/006-backend-target-statistics/contracts/target-amount.md">target-amount.md §2</a>
 */
@Component
public class TargetResolver {

    /** 기본 목표를 저장한 적 없을 때의 값. <b>{@code null} 이 아니라 0</b> 이다. */
    public static final long NO_DEFAULT = 0L;

    /**
     * 적용 금액을 낸다.
     *
     * @param monthlyTargetAmount 그 달 월별 목표. <b>{@code null} 은 "저장한 적 없음"</b>이며
     *                            {@code 0} 은 "0원으로 정했다"다
     * @param defaultTargetAmount 기본 목표. {@code null} 이면 {@link #NO_DEFAULT} 로 본다
     * @return 언제나 숫자다
     */
    public long resolve(Long monthlyTargetAmount, Long defaultTargetAmount) {
        if (monthlyTargetAmount != null) {
            // 0 이어도 여기서 걸린다 — 그것이 이 판정의 전부다.
            return monthlyTargetAmount;
        }
        return defaultTargetAmount == null ? NO_DEFAULT : defaultTargetAmount;
    }

    /** 기본 목표를 응답에 실을 값. 저장한 적 없으면 {@code 0} 이다. */
    public long defaultOrZero(Long defaultTargetAmount) {
        return defaultTargetAmount == null ? NO_DEFAULT : defaultTargetAmount;
    }
}
