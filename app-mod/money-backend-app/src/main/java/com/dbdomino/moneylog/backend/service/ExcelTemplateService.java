package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.excel.ExcelTemplateWriter;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import java.io.UncheckedIOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 3.11 엑셀 양식 생성.
 *
 * <h2>양식은 회원마다 내용이 다르다</h2>
 *
 * <p>FR-317 이 본인의 <b>사용 중</b> 수단·지출유형을 드롭다운으로 넣으라고 요구한다.
 * 그래서 정적 파일로 미리 만들어 둘 수 없고 요청마다 목록을 읽어 생성한다 — plan.md 가
 * "리소스의 고정 {@code .xlsx}" 대안을 기각한 이유가 이것이다.
 *
 * <h2>수단은 용도 구분 없이 전부다</h2>
 *
 * <p>양식의 A열이 지출·소득을 <b>모두</b> 담으므로 한쪽 용도만 넣으면 다른 쪽 행을 채울 수
 * 없다(excel-contract.md §2). 어느 용도가 필요한지는 <b>그 행의 A열</b>이 정하고, 업로드가
 * 다시 검증한다.
 */
@Service
public class ExcelTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExcelTemplateService.class);

    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserExpendGroupRepository expendGroupRepository;
    private final ExcelTemplateWriter templateWriter;

    public ExcelTemplateService(UserPaymentMethodRepository paymentMethodRepository,
                                UserExpendGroupRepository expendGroupRepository,
                                ExcelTemplateWriter templateWriter) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.expendGroupRepository = expendGroupRepository;
        this.templateWriter = templateWriter;
    }

    /**
     * 그 회원의 양식을 만들어 바이트로 돌려준다.
     *
     * <p>생성 실패는 {@code 9000} 이다 — 사용자 입력 문제가 아니라 서버가 파일을 만들지
     * 못한 것이라 사용자가 요청을 고쳐서 될 일이 아니다. <b>실패해도 래퍼를 쓴다</b>
     * (FR-322) — 003 의 아이콘 조회(2.10)가 인증 실패에 래퍼를 벗는 것과 다르다.
     */
    @Transactional(readOnly = true)
    public byte[] createTemplate(AuthPrincipal principal) {
        List<String> paymentMethodNames = paymentMethodRepository
                .findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(principal.idKey())
                .stream()
                .map(UserPaymentMethod::getName)
                .distinct()
                .toList();
        List<String> expendGroupNames = expendGroupRepository
                .findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(principal.idKey())
                .stream()
                .map(UserExpendGroup::getName)
                .toList();

        try {
            return templateWriter.write(paymentMethodNames, expendGroupNames);
        } catch (UncheckedIOException e) {
            log.error("excel template creation failed idKey={}", principal.idKey(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
