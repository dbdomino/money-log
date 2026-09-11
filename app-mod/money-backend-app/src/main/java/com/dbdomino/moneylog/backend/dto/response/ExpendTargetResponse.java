package com.dbdomino.moneylog.backend.dto.response;

/**
 * 5.1 목록의 한 줄 — 한 지출유형의 두 층 목표.
 *
 * <h2>두 필드의 "미설정"이 의도적으로 다르다 (FR-507)</h2>
 *
 * <table border="1">
 *   <caption>행이 없을 때</caption>
 *   <tr><th>필드</th><th>값</th><th>뜻</th></tr>
 *   <tr><td>{@code monthlyTargetAmount}</td><td><b>{@code null}</b></td>
 *       <td>그 달은 따로 정하지 않았다 — 기본값을 쓴다</td></tr>
 *   <tr><td>{@code defaultTargetAmount}</td><td><b>{@code 0}</b></td>
 *       <td>한도를 정한 적 없다</td></tr>
 * </table>
 *
 * <p><b>이 비대칭이 적용 금액을 항상 숫자로 만든다.</b> 적용 금액 =
 * {@code monthly ?? default} 인데 기본이 절대 {@code null} 이 아니므로 결과도 {@code null}
 * 이 되지 않는다. 통계 저장 때 {@code tbl_statistics_expend_group.target_amount} 가
 * NOT NULL 이라 이게 필요하다.
 *
 * <p><b>적용 금액 필드는 없다.</b> 5.1·5.2 의 필드 표와 FR-507 이 정한 것은 두 값이며
 * 적용 금액은 화면이 {@code monthly ?? default} 로 낸다 — 서버가 같은 정보를 두 형태로
 * 내리면 둘이 어긋날 여지만 생긴다. 서버가 적용 금액을 직접 쓰는 곳은 통계(5.5·5.6)뿐이고
 * 거기서는 {@code targetAmount} 한 필드로 나간다.
 *
 * <p><b>{@code monthlyTargetAmount} 를 {@code Long} 으로 둔다.</b> {@code long} 이면
 * {@code null} 을 표현할 수 없어 "없음"이 {@code 0}("그 달엔 쓰지 않겠다")으로 뭉개진다.
 * 그리고 {@code null} 일 때 <b>필드를 생략하지 않는다</b> — 프론트가
 * {@code 'monthlyTargetAmount' in obj} 로 분기하면 생략과 {@code null} 이 다른 결과를
 * 낸다. 003 의 {@code iconUrl}(SC-209)과 같은 주의점이다.
 *
 * @param expendGroupId       지출유형 PK
 * @param expendGroupName     지출유형의 <b>현재</b> 이름. 목표금액은 지금 유효한 설정이라
 *                            원본에서 읽는다 — 통계 상세의 스냅샷과 반대다(api-contract §9)
 * @param defaultTargetAmount 기본(템플릿) 월 목표금액. 행이 없으면 {@code 0}
 * @param monthlyTargetAmount 그 연·월에 별도 저장된 목표. 행이 없으면 {@code null}
 */
public record ExpendTargetResponse(
        long expendGroupId,
        String expendGroupName,
        long defaultTargetAmount,
        Long monthlyTargetAmount) {
}
