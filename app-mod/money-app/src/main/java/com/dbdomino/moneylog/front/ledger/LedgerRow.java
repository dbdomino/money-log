package com.dbdomino.moneylog.front.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 화면이 보여 주는 가계부 행. <b>한 목록에 네 종류가 섞인다.</b>
 *
 * <p>저장은 세 곳에 나뉘어 있고 <b>가계부라는 저장 단위는 없다</b> — 조회 시점에 합쳐서
 * 온다. 그래서 이 타입 하나가 지출·할부·소득·고정지출을 모두 담고, 종류마다 비는 칸이
 * 다르다.
 *
 * <h2>비는 칸에 값을 지어내지 않는다</h2>
 *
 * <p>소득에는 지출유형과 장소가 없고, 고정지출에는 장소가 없다. <b>응답에 없는 것이
 * 정상</b>이라 화면이 「없음」 같은 말을 채우면 사용자는 <b>"비어 있는 값"과 "없는 항목"을
 * 구분하지 못한다.</b>
 *
 * <p>그래서 이 타입에 기본값을 두지 않는다. {@code null} 이 그대로 템플릿까지 가고, 템플릿은
 * 빈 칸을 그린다.
 *
 * <h2>이름을 다시 조회해 덮어쓰지 않는다</h2>
 *
 * <p>같은 수단 이름이라도 <b>고정지출 행만 조회 시점 현재 이름</b>이고 나머지 셋은 등록
 * 당시 스냅샷이다. 수단 이름을 바꾼 뒤 열면 고정지출 행만 새 이름으로 보이는데 <b>버그가
 * 아니라 설계다</b> — 고정지출은 "지금 유효한 설정"이고 지출·소득은 "이미 일어난 일의
 * 기록"이다.
 *
 * <p>화면이 이 차이를 숨기려고 이름을 맞추면 <b>과거 기록이 사후에 바뀐다.</b> 가계부가
 * 해서는 안 되는 일이라 받은 값을 그대로 들고 있는다.
 *
 * <h2>종류를 말로 바꾸는 자리가 여기다</h2>
 *
 * <p>{@code EXPENSE}·{@code INSTALLMENT} 는 화면 밖의 값이다. 바꾸는 자리를 한 곳에 두면
 * 목록과 뱃지가 같은 말을 쓴다 — 화면마다 따로 바꾸면 어느 자리에서는 「할부」이고 다른
 * 자리에서는 「분할」이 된다.
 *
 * <h2>열 수 있는 것을 가리는 판단도 여기 둔다</h2>
 *
 * <p>수정·삭제·중도상환을 그릴지는 <b>행 종류가 정한다.</b> 템플릿이 종류 문자열을 직접
 * 비교하면 같은 판단이 화면 여러 곳에 흩어지고, 한 곳만 고쳐도 다른 곳이 남는다.
 *
 * @param ledgerItemId 목록 안에서 행을 가리키는 값. 화면에 보이지 않는다
 * @param type 행 종류. {@code EXPENSE} · {@code INSTALLMENT} · {@code INCOME} · {@code FIXED}
 * @param sourceId 원본 식별자. 수정·삭제 주소에 싣는다
 * @param paymentDate 결제일 {@code YYYY-MM-DD}
 * @param amount 금액. 원 단위 정수
 * @param paymentMethodId 수단 식별자. 없을 수 있다
 * @param paymentMethodName 수단 이름. <b>받은 값 그대로</b>다
 * @param expendGroupId 지출유형 식별자. 소득에는 없다
 * @param expendGroupName 지출유형 이름. <b>소득에는 없다</b>
 * @param place 장소. <b>소득·고정지출에는 없다</b>
 * @param content 내용. 네 종류 모두 갖는다
 * @param fixedExpenseName 고정지출 이름. <b>고정지출에만</b> 있다
 * @param installmentGroupId 할부 그룹. <b>할부에만</b> 있다. 중도상환이 이 값을 쓴다
 * @param installmentIndex 할부 회차. 할부에만 있다
 * @param installmentTotal 할부 총 개월. 할부에만 있다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerRow(
        String ledgerItemId,
        String type,
        Long sourceId,
        String paymentDate,
        Long amount,
        Long paymentMethodId,
        String paymentMethodName,
        Long expendGroupId,
        String expendGroupName,
        String place,
        String content,
        String fixedExpenseName,
        Long installmentGroupId,
        Integer installmentIndex,
        Integer installmentTotal) {

    /** 백엔드가 쓰는 행 종류 값. 화면은 이 문자열을 사용자에게 보이지 않는다. */
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INSTALLMENT = "INSTALLMENT";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_FIXED = "FIXED";

    // ── 종류를 가리는 판단 ──────────────────────────────────────────────

    public boolean isExpense() {
        return TYPE_EXPENSE.equals(type);
    }

    public boolean isInstallment() {
        return TYPE_INSTALLMENT.equals(type);
    }

    public boolean isIncome() {
        return TYPE_INCOME.equals(type);
    }

    /** 고정지출 행인가. <b>이 화면에서는 읽기만 한다.</b> */
    public boolean isFixed() {
        return TYPE_FIXED.equals(type);
    }

    /** 종류를 화면에 보일 말로 바꾼다. */
    public String typeLabel() {
        if (isInstallment()) {
            return "할부";
        }
        if (isIncome()) {
            return "소득";
        }
        if (isFixed()) {
            return "고정지출";
        }
        return "지출";
    }

    /**
     * 뱃지에 붙일 모양 이름. 프로토타입이 쓰던 네 가지를 그대로 쓴다.
     *
     * <p>조각이 이 값으로 색을 고른다. 종류 문자열을 조각이 직접 비교하면 같은 판단이 또 한
     * 벌 생긴다.
     */
    public String typeBadgeClass() {
        if (isInstallment()) {
            return "badge-installment";
        }
        if (isIncome()) {
            return "badge-income";
        }
        if (isFixed()) {
            return "badge-fixed";
        }
        return "badge-expense";
    }

    // ── 행이 열 수 있는 것 (data-model §1.2) ────────────────────────────

    /**
     * 수정을 그릴 수 있는가. <b>고정지출 행에는 그리지 않는다.</b>
     *
     * <p>만들고 고치는 것은 011 의 화면이다. 여기서 고치면 그 달 내역만 바뀔지 설정 전체가
     * 바뀔지 사용자가 알 수 없다.
     */
    public boolean canEdit() {
        return !isFixed();
    }

    /**
     * 삭제를 그릴 수 있는가. <b>고정지출 행에는 그리지 않는다.</b>
     *
     * <p>여기서 지우면 그 달 내역만 사라질지 설정 전체가 사라질지 사용자가 알 수 없다 —
     * <b>되돌릴 수 없는 동작의 범위가 불분명한 버튼</b>을 두지 않는다.
     */
    public boolean canDelete() {
        return !isFixed();
    }

    /**
     * 중도상환을 그릴 수 있는가. <b>할부 행에만</b> 그린다.
     *
     * <p>일반 지출 행에 두면 사용자는 자기 지출이 할부인 줄 안다. 할부 그룹이 없으면 부를
     * 대상 자체가 없으므로 함께 본다.
     */
    public boolean canSettleInstallment() {
        return isInstallment() && installmentGroupId != null;
    }

    /** 수정·삭제가 지출 쪽으로 가는가. 소득과 주소가 다르다. */
    public boolean isExpenseSide() {
        return isExpense() || isInstallment();
    }

    // ── 비는 칸 (data-model §1.1) ───────────────────────────────────────

    /** 할부 회차를 보일 수 있는가. 둘 다 있어야 「3/12」를 만들 수 있다. */
    public boolean hasInstallmentIndex() {
        return installmentIndex != null && installmentTotal != null;
    }

    /**
     * 할부 회차 표시. 「3/12」처럼 보인다.
     *
     * <p>적지 않으면 사용자는 같은 지출이 여러 번 들어간 줄 안다. 값이 없으면 <b>빈
     * 문자열</b>이다 — 「-」 같은 말을 채우지 않는다.
     */
    public String installmentLabel() {
        return hasInstallmentIndex() ? installmentIndex + "/" + installmentTotal : "";
    }

    /** 장소를 보일 자리가 있는가. 소득·고정지출에는 그 값 자체가 없다. */
    public boolean hasPlace() {
        return place != null && !place.isBlank();
    }

    /** 내용을 보일 자리가 있는가. */
    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    /** 고정지출 이름을 보일 자리가 있는가. 고정지출 행에만 있다. */
    public boolean hasFixedExpenseName() {
        return fixedExpenseName != null && !fixedExpenseName.isBlank();
    }

    /**
     * 확인 다이얼로그에 담을 대상 설명 — 일자·금액·내용.
     *
     * <p>표의 어느 행을 눌렀는지 다이얼로그만 보고 확인할 수 있어야 한다. <b>비는 칸은 그냥
     * 빠진다</b> — 「없음」을 채우면 다이얼로그에까지 지어낸 말이 들어간다.
     */
    public String describe() {
        StringBuilder text = new StringBuilder();
        if (paymentDate != null) {
            text.append(paymentDate);
        }
        if (amount != null) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(amount).append("원");
        }
        if (hasContent()) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(content);
        }
        return text.toString();
    }
}
