package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 지출·소득이 거는 <b>참조 하나</b>를 검증하고 그 시점 이름을 내준다.
 *
 * <p>004 에서 가장 틀리기 쉬운 두 규칙이 여기 모여 있다. 필요한 경로가 다섯이라
 * (3.1 지출 등록 · 3.3 지출 수정 · 3.7 소득 등록 · 3.9 소득 수정 · 3.12 엑셀 업로드)
 * 각자 구현하면 <b>한 곳만 어긋나도 과거 데이터가 조용히 오염된다</b> — 예외도 오류 응답도
 * 나지 않고 이름만 바뀌므로 한참 뒤에 발견된다.
 *
 * <h2>규칙 1 — 새로 거는 참조만 "사용 중"을 요구한다 (FR-325 ↔ FR-326)</h2>
 *
 * <table border="1">
 *   <caption>참조 대상이 {@code in_use=false} 이거나 {@code deleted=true} 일 때</caption>
 *   <tr><th>경로</th><th>결과</th></tr>
 *   <tr><td>새로 참조를 건다(등록, 또는 수정에서 참조 변경)</td>
 *       <td>거절 — 수단 {@code 3003} · 지출유형 {@code 3103}</td></tr>
 *   <tr><td>이미 저장된 행을 조회·수정·삭제</td><td><b>정상 동작</b></td></tr>
 * </table>
 *
 * <p>이 비대칭이 없으면 003 의 삭제 표시가 004 를 망가뜨린다. 수단을 삭제 표시하는 순간
 * 그 수단으로 적은 과거 지출 전부가 손댈 수 없게 되고, 003 이 물리 삭제 대신 삭제 표시를
 * 고른 이유("과거 기록 보존") 자체가 무너진다.
 *
 * <h2>규칙 2 — 스냅샷은 참조가 실제로 바뀔 때만 갱신한다 (FR-304)</h2>
 *
 * <pre>{@code
 * 요청이 참조 필드를 보냈는가?
 *   ├ 아니오(omit)        → 참조·스냅샷 둘 다 그대로. 검증도 하지 않는다
 *   ├ 예, 값이 기존과 같음 → 그대로
 *   └ 예, 값이 다름        → 새 참조를 검증하고 그 현재 이름으로 스냅샷 갱신
 * }</pre>
 *
 * <p>첫 갈래가 FR-326 을 성립시킨다 — 죽은 수단을 쓰던 과거 지출의 <b>금액만 고치는
 * 수정</b>이 성공해야 하는데, 참조를 안 보냈으니 검증할 것도 없다.
 *
 * <p>두 번째 갈래를 빠뜨리면("수정 요청이 왔으니 최신화한다") 같은 수단을 유지한 채 금액만
 * 고쳤을 때 이름이 조용히 바뀌어 <b>003 의 SC-205 가 깨진다</b>.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/api-contract.md">api-contract.md §4·§5</a>
 */
@Component
public class ReferenceResolver {

    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserExpendGroupRepository expendGroupRepository;

    public ReferenceResolver(UserPaymentMethodRepository paymentMethodRepository,
                             UserExpendGroupRepository expendGroupRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.expendGroupRepository = expendGroupRepository;
    }

    // ── 규칙 1: 새로 거는 참조 ──────────────────────────────────────────────

    /**
     * 등록(3.1·3.5·3.7)이 거는 수단. 없거나 남의 것이거나 사용 중이 아니면 {@code 3003}.
     *
     * <p><b>소유자를 조회 조건에 함께 건다.</b> 먼저 꺼내 놓고 비교하는 방식은 비교를
     * 빠뜨린 자리가 곧 구멍이 된다 — 남의 수단으로 내 지출을 만들 수 있게 된다.
     *
     * <p>"없음"·"타인 소유"·"사용 안 함"·"삭제 표시"를 <b>같은 코드로 묶는다</b>(FR-325).
     * 사용자가 취할 조치가 넷 다 "다른 수단을 고른다"로 같고, 코드를 나누면 ID 를 훑는
     * 것만으로 남의 수단이 존재한다는 사실이 새어 나간다.
     */
    public UserPaymentMethod requireUsablePaymentMethod(AuthPrincipal principal, Long paymentMethodId) {
        return paymentMethodRepository.findByIdxAndUserIdKey(paymentMethodId, principal.idKey())
                .filter(ReferenceResolver::isUsable)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
    }

    /** 등록(3.1·3.5)이 거는 지출유형. 같은 규칙이며 실패 코드만 {@code 3103} 이다. */
    public UserExpendGroup requireUsableExpendGroup(AuthPrincipal principal, Long expendGroupId) {
        return expendGroupRepository.findByIdxAndUserIdKey(expendGroupId, principal.idKey())
                .filter(ReferenceResolver::isUsable)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_NOT_FOUND));
    }

    // ── 규칙 1: 엑셀(3.12)은 ID 가 아니라 이름으로 찾는다 ──────────────────

    /**
     * 엑셀 업로드가 이름으로 거는 수단. 양식이 드롭다운으로 이름을 넣게 해 파일에 ID 가 없다.
     *
     * <p><b>여러 건이 나올 수 있다.</b> {@code tbl_user_payment_method} 에는 이름 유니크
     * 제약이 없어 한 회원이 "국민카드"를 두 개 만들 수 있다. 그래서 {@code List} 를 그대로
     * 돌려주고 <b>어느 쪽을 고를지는 호출자가 정한다</b> — 첫 매치로 갈지 중복을
     * {@code 3502} 오류로 볼지는 업로드의 정책이지 참조 검증의 몫이 아니다.
     *
     * <p>못 찾으면 빈 목록이다. 그 행을 오류로 보는 것도 호출자가 한다 — 엑셀은 실패를
     * 예외로 던지지 않고 {@code errors[]} 에 모아야 하기 때문이다.
     */
    public List<UserPaymentMethod> findUsablePaymentMethodsByName(AuthPrincipal principal, String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return paymentMethodRepository
                .findByUserIdKeyAndNameAndInUseTrueAndDeletedFalseOrderByIdxAsc(
                        principal.idKey(), name.trim());
    }

    /**
     * 엑셀 업로드가 이름으로 거는 지출유형.
     *
     * <p><b>{@code Optional} 이다</b> — {@code ux_user_expend_group_name (id_key, name)} 이
     * 회원 안에서 이름을 하나로 강제한다. 수단 쪽이 {@code List} 인 것과 다른 이유가
     * 그 제약의 유무다.
     */
    public Optional<UserExpendGroup> findUsableExpendGroupByName(AuthPrincipal principal, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return expendGroupRepository.findByUserIdKeyAndNameAndInUseTrueAndDeletedFalse(
                principal.idKey(), name.trim());
    }

    // ── 규칙 2: 수정 경로의 3갈래 판정 ────────────────────────────────────

    /**
     * 수정(3.3·3.9)에서 수단 참조를 바꿔야 하는가.
     *
     * <p><b>빈 값이면 아무것도 하지 않는다.</b> 참조도 스냅샷도 건드리지 않으며 검증조차
     * 하지 않는다 — 죽은 수단을 쓰던 과거 지출의 금액만 고치는 수정이 여기서 성공한다.
     *
     * <p>값이 있으면 <b>검증을 통과한 새 수단</b>이다. 호출자는 참조를 그것으로 바꾸고
     * 스냅샷을 {@code getName()} 으로 갱신한다 — 둘을 함께 바꾸는 것이 FR-304 다.
     *
     * @param requestedId 요청이 보낸 값. <b>{@code null} 은 "보내지 않았다"는 뜻</b>이다.
     *                    {@code "paymentMethodId": null} 을 명시적으로 보낸 요청은
     *                    호출자가 {@code 9001} 로 먼저 막는다 — 이 컬럼은 NOT NULL 이라
     *                    "비운다"는 조작이 없다
     * @param currentId   지금 걸려 있는 참조의 {@code idx}
     * @return 바꿔야 하면 검증을 통과한 새 수단, 그대로 두어야 하면 빈 값
     */
    public Optional<UserPaymentMethod> resolvePaymentMethodChange(
            AuthPrincipal principal, Long requestedId, Long currentId) {
        if (unchanged(requestedId, currentId)) {
            return Optional.empty();
        }
        return Optional.of(requireUsablePaymentMethod(principal, requestedId));
    }

    /** 수정(3.3)에서 지출유형 참조를 바꿔야 하는가. 규칙은 수단과 같고 코드만 {@code 3103} 이다. */
    public Optional<UserExpendGroup> resolveExpendGroupChange(
            AuthPrincipal principal, Long requestedId, Long currentId) {
        if (unchanged(requestedId, currentId)) {
            return Optional.empty();
        }
        return Optional.of(requireUsableExpendGroup(principal, requestedId));
    }

    /**
     * 앞 두 갈래("보내지 않음"·"값이 같음")를 하나로 묶는다.
     *
     * <p>둘을 가르지 않는 것은 <b>해야 할 일이 같기 때문</b>이다 — 어느 쪽이든 참조가
     * 바뀌지 않으므로 검증할 것도, 갱신할 스냅샷도 없다.
     */
    private static boolean unchanged(Long requestedId, Long currentId) {
        return requestedId == null || requestedId.equals(currentId);
    }

    /** 새 참조로 걸 수 있는 상태인가 — 사용 중이고 삭제 표시되지 않았는가. */
    private static boolean isUsable(UserPaymentMethod method) {
        return Boolean.TRUE.equals(method.getInUse()) && !Boolean.TRUE.equals(method.getDeleted());
    }

    /** 지출유형판. 컬럼 이름이 같아 판정도 같다. */
    private static boolean isUsable(UserExpendGroup group) {
        return Boolean.TRUE.equals(group.getInUse()) && !Boolean.TRUE.equals(group.getDeleted());
    }
}
