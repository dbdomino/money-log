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
 * <p><b>005 는 사용자가 참조를 직접 고르는 경로에서만 이 클래스를 쓴다</b>(4.1 등록 ·
 * 4.4 수정 · 4.6 월별 단건 수정). 월별 내역을 <b>자동으로 만드는</b> 경로(4.5·4.8·4.9)는
 * 설정이 이미 들고 있는 참조를 그대로 복사하며 <b>여기를 거치지 않는다</b> — 거치면
 * 삭제 표시된 유형을 쓰던 고정지출 때문에 평범한 달 조회가 {@code 3103} 으로 죽는다
 * (FR-426. {@code FixedExpenseMonthlyFactory} 의 javadoc 에 자세히 적었다).
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
     * 등록(3.1·3.5·3.7)이 거는 수단. 조건에 하나라도 걸리면 {@code 3003} 이다.
     *
     * <p><b>소유자를 조회 조건에 함께 건다.</b> 먼저 꺼내 놓고 비교하는 방식은 비교를
     * 빠뜨린 자리가 곧 구멍이 된다 — 남의 수단으로 내 지출을 만들 수 있게 된다.
     *
     * <p>"없음"·"타인 소유"·"사용 안 함"·"삭제 표시"·"용도 불일치"를 <b>같은 코드로
     * 묶는다</b>(FR-325). 사용자가 취할 조치가 다섯 다 "다른 수단을 고른다"로 같고,
     * 코드를 나누면 ID 를 훑는 것만으로 남의 수단이 존재한다는 사실이 새어 나간다.
     *
     * @param purpose 이 자원이 요구하는 용도 — 지출은 {@code EXPENSE}, 소득은
     *                {@code INCOME}. <b>한 수단은 한쪽만 갖는다</b>(003 FR-033)는 규칙을
     *                004 가 지키는 자리다. 검사하지 않으면 지출용 카드로 소득을 적을 수
     *                있게 되고, 그러면 003 이 {@code purpose} 변경을 참조 0건일 때만
     *                허용하는({@code 3005}) 이유 자체가 무너진다
     */
    public UserPaymentMethod requireUsablePaymentMethod(AuthPrincipal principal,
                                                        Long paymentMethodId, String purpose) {
        return paymentMethodRepository.findByIdxAndUserIdKey(paymentMethodId, principal.idKey())
                .filter(ReferenceResolver::isUsable)
                .filter(method -> purpose.equals(method.getPurpose()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
    }

    /** 등록(3.1·3.5·4.1)이 거는 지출유형. 같은 규칙이며 실패 코드만 {@code 3103} 이다. */
    public UserExpendGroup requireUsableExpendGroup(AuthPrincipal principal, Long expendGroupId) {
        return expendGroupRepository.findByIdxAndUserIdKey(expendGroupId, principal.idKey())
                .filter(ReferenceResolver::isUsable)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_NOT_FOUND));
    }

    // ── 005 가 쓰는 갈래: 용도 실패를 다른 코드로 낸다 ──────────────────────

    /**
     * 소유·사용 가능 여부<b>까지만</b> 보는 수단 조회. 실패는 {@code 3003} 이다.
     *
     * <p>{@link #requireUsablePaymentMethod} 와 나뉘는 지점이 <b>용도 불일치를 어느 코드로
     * 내느냐</b>다.
     *
     * <table border="1">
     *   <caption>같은 상황, 다른 코드</caption>
     *   <tr><th>기능</th><th>수단 없음·타인·사용 안 함</th><th>용도 불일치</th></tr>
     *   <tr><td>004 (3.1·3.7 등)</td><td>{@code 3003}</td><td><b>{@code 3003}</b></td></tr>
     *   <tr><td>005 (4.1·4.6)</td><td>{@code 3003}</td><td><b>{@code 3401}</b></td></tr>
     * </table>
     *
     * <p>004 가 용도까지 {@code 3003} 으로 묶은 것은 <b>존재를 감추기 위해서</b>였다 —
     * ID 를 훑어 남의 수단을 찾아내지 못하게. 005 는 그럴 필요가 없다. 4.1 의 수단은
     * 사용자가 <b>자기 목록에서 고른 것</b>이라 존재가 이미 드러나 있고, 사용자가 취할
     * 조치도 다르다("다른 수단을 고른다"가 아니라 "지출용 수단을 고른다").
     *
     * <p><b>004 의 메서드는 손대지 않는다.</b> 그쪽 시그니처를 바꾸면 3.1·3.3·3.5·3.7·3.9·
     * 3.12 여섯 경로의 실패 코드가 함께 움직인다.
     *
     * @see #requirePurpose(UserPaymentMethod, String, ErrorCode)
     */
    public UserPaymentMethod requireOwnedUsablePaymentMethod(AuthPrincipal principal,
                                                             Long paymentMethodId) {
        return paymentMethodRepository.findByIdxAndUserIdKey(paymentMethodId, principal.idKey())
                .filter(ReferenceResolver::isUsable)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
    }

    /**
     * 그 수단이 요구한 용도인가. 아니면 <b>호출자가 정한 코드</b>로 거절한다.
     *
     * <p>{@link #requireOwnedUsablePaymentMethod} 와 짝으로 쓴다. 둘을 나눠 둔 것은
     * 판정 순서가 결과를 바꾸기 때문이다 — 참조({@code 3003}) 가 용도({@code 3401}) 보다
     * <b>먼저</b>다. 한 메서드로 합치면 그 순서가 구현 안에 숨는다(api-contract.md §6).
     *
     * @param purpose   요구하는 용도. 고정지출은 {@code EXPENSE} 뿐이다 — 고정"지출"이므로
     *                  소득용 수단이 들어올 자리가 없다
     * @param errorCode 용도가 어긋났을 때 낼 코드. 005 는 {@code 3401} 을 넘긴다
     */
    public UserPaymentMethod requirePurpose(UserPaymentMethod method, String purpose,
                                            ErrorCode errorCode) {
        if (!purpose.equals(method.getPurpose())) {
            throw new BusinessException(errorCode,
                    "이 기능에는 %s 용도의 수단만 쓸 수 있습니다.".formatted(purpose));
        }
        return method;
    }

    // ── 006 이 쓰는 갈래: 소유(3103)와 사용 여부(3601)를 나눈다 ──────────────

    /**
     * 소유 여부<b>까지만</b> 보는 지출유형 조회. 실패는 {@code 3103} 이다.
     *
     * <p>{@link #requireUsableExpendGroup} 와 나뉘는 지점이 <b>사용 여부를 여기서
     * 보느냐</b>다. 004·005 는 "없음·타인·사용 안 함" 셋을 {@code 3103} 하나로 묶었지만
     * 006 은 사용 안 함을 {@code 3601} 로 따로 낸다 — 사용자가 취할 조치가 다르기
     * 때문이다("다른 유형을 고른다"가 아니라 "그 유형을 다시 사용 중으로 돌린다").
     *
     * <p>그래서 <b>둘을 나눠 두고 순서를 호출자가 정한다</b>. 소유({@code 3103})가
     * 사용 여부({@code 3601})보다 <b>먼저</b>여야 한다 — 남의 유형 ID 로 접근했는데 그게
     * 마침 {@code in_use=false} 라면 {@code 3601} 을 내는 순간 <b>그 ID 가 실재한다는
     * 사실이 코드 차이로 새어 나간다</b>(quickstart #14).
     *
     * <p><b>{@code deleted} 는 보지 않는다.</b> 목표금액에서 삭제 표시는 조건이 아니다
     * (target-amount.md §5) — 삭제 표시된 유형의 목표 행은 유지되어야 하고(FR-511)
     * 그 행을 읽으려면 유형을 찾을 수 있어야 한다.
     *
     * <p><b>004·005 의 메서드는 손대지 않는다.</b> 그쪽 시그니처를 바꾸면 여섯 경로의
     * 실패 코드가 함께 움직인다.
     *
     * @see #requireInUse(UserExpendGroup, ErrorCode)
     */
    public UserExpendGroup requireOwnedExpendGroup(AuthPrincipal principal, Long expendGroupId) {
        return expendGroupRepository.findByIdxAndUserIdKey(expendGroupId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_NOT_FOUND));
    }

    /**
     * 그 유형이 사용 중인가. 아니면 <b>호출자가 정한 코드</b>로 거절한다.
     *
     * <p>{@link #requireOwnedExpendGroup} 와 짝으로 쓴다. 006 은 {@code 3601} 을 넘긴다.
     *
     * <p><b>{@code deleted} 를 함께 보지 않는다.</b> 조건은 {@code in_use} 뿐이다
     * (target-amount.md §5).
     */
    public UserExpendGroup requireInUse(UserExpendGroup group, ErrorCode errorCode) {
        if (!Boolean.TRUE.equals(group.getInUse())) {
            throw new BusinessException(errorCode, "사용하지 않는 지출유형입니다.");
        }
        return group;
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
     *
     * <p><b>용도로 한 번 더 거른다.</b> 양식의 수단 드롭다운은 용도 구분 없이 전부를
     * 담지만(excel-contract.md §2), 실제로 어느 용도가 필요한지는 그 행의 A열
     * ({@code EXPENSE}·{@code INCOME})이 정한다 — 지출 행에 소득용 수단을 적었으면
     * 그 행은 오류다.
     *
     * @param purpose 그 행이 요구하는 용도
     */
    public List<UserPaymentMethod> findUsablePaymentMethodsByName(AuthPrincipal principal,
                                                                  String name, String purpose) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return paymentMethodRepository
                .findByUserIdKeyAndNameAndInUseTrueAndDeletedFalseOrderByIdxAsc(
                        principal.idKey(), name.trim())
                .stream()
                .filter(method -> purpose.equals(method.getPurpose()))
                .toList();
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
     * @param purpose     이 자원이 요구하는 용도. 등록과 같은 값을 넘긴다 — 지출을
     *                    소득용 수단으로 <b>바꿔치기</b> 할 수 없어야 한다
     * @return 바꿔야 하면 검증을 통과한 새 수단, 그대로 두어야 하면 빈 값
     */
    public Optional<UserPaymentMethod> resolvePaymentMethodChange(
            AuthPrincipal principal, Long requestedId, Long currentId, String purpose) {
        if (unchanged(requestedId, currentId)) {
            return Optional.empty();
        }
        return Optional.of(requireUsablePaymentMethod(principal, requestedId, purpose));
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
