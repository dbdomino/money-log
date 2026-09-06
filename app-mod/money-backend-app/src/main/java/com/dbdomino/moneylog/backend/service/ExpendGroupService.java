package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.response.ExpendGroupActiveResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupListResponse;
import com.dbdomino.moneylog.backend.mapper.ExpendGroupMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출유형 — 현재는 사용 중 목록(2.13)만 선다.
 *
 * <p>등록(2.7)·관리 목록(2.8)·상세(2.9)·수정(2.11)·삭제 표시(2.12)는 US3 에서 이 클래스에
 * 붙는다. 가입 시 기본 10종을 만드는 일은 {@link DefaultExpendGroupService} 가 따로 맡는다 —
 * 그쪽은 인증 이전에 도는 흐름이라 {@link AuthPrincipal} 이 없다.
 *
 * <p>소유자는 토큰의 {@code id_key} 로만 정한다(FR-201). 수단과 같은 규칙이다.
 */
@Service
public class ExpendGroupService {

    private final UserExpendGroupRepository expendGroupRepository;
    private final ExpendGroupMapper expendGroupMapper;

    public ExpendGroupService(UserExpendGroupRepository expendGroupRepository,
                              ExpendGroupMapper expendGroupMapper) {
        this.expendGroupRepository = expendGroupRepository;
        this.expendGroupMapper = expendGroupMapper;
    }

    /**
     * 2.13 사용 중 목록. 지출 등록·고정지출 화면이 고를 수 있는 유형만 돌려준다.
     *
     * <p><b>두 조건만 건다</b>: {@code in_use=true} · {@code deleted=false}.
     * 수단의 사용 중 목록(2.6)이 세 조건인 것과 다르다 — <b>지출유형에는 용도 구분이
     * 없다.</b> 인덱스 {@code ix_user_expend_group_active (id_key, in_use, deleted)} 에도
     * {@code purpose} 가 없어 그 사실이 DB 구조에 그대로 드러나 있다.
     *
     * <p>{@code purpose} 같은 입력을 받지 않으므로 이 API 에는 값 오류가 없다 —
     * 실패 코드가 {@code 1001}(로그인 필요) 하나뿐인 이유다.
     */
    @Transactional(readOnly = true)
    public ExpendGroupListResponse<ExpendGroupActiveResponse> listActive(AuthPrincipal principal) {
        return new ExpendGroupListResponse<>(expendGroupMapper.toActiveResponses(
                expendGroupRepository.findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(
                        principal.idKey())));
    }
}
