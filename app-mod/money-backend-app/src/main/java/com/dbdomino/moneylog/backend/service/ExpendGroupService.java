package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.ExpendGroupCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupActiveResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupListResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupResponse;
import com.dbdomino.moneylog.backend.mapper.ExpendGroupMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지출유형 — 등록(2.7)·관리 목록(2.8)·상세(2.9)·수정(2.11)·삭제 표시(2.12)·
 * 사용 중 목록(2.13).
 *
 * <p>가입 시 기본 10종을 만드는 일은 {@link DefaultExpendGroupService} 가 따로 맡는다 —
 * 그쪽은 인증 이전에 도는 흐름이라 {@link AuthPrincipal} 이 없다.
 *
 * <h2>소유자는 토큰이 정한다</h2>
 *
 * <p>조회를 {@code idx + id_key} 로 걸어 없는 유형과 남의 유형이 같은 {@code 3103} 이 된다.
 * 코드를 갈라 두면 ID 를 훑는 것만으로 남의 자원이 존재한다는 사실이 새어 나간다(FR-201).
 *
 * <h2>판정 순서가 응답 코드를 정한다</h2>
 *
 * <p>수정(2.11)과 삭제(2.12)는 조건이 여럿이라 <b>어느 것을 먼저 보느냐가 곧 코드</b>다.
 * api-contract.md §5 가 정한 순서를 그대로 따른다 — 각 메서드의 문서에 적어 두었다.
 *
 * @see <a href="../../../../../../../../specs/003-backend-payment-expend-group/contracts/api-contract.md">api-contract.md §5</a>
 */
@Service
public class ExpendGroupService {

    private final UserExpendGroupRepository expendGroupRepository;
    private final UserExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpendGroupMapper expendGroupMapper;

    public ExpendGroupService(UserExpendGroupRepository expendGroupRepository,
                              UserExpenseRepository expenseRepository,
                              UserRepository userRepository,
                              ExpendGroupMapper expendGroupMapper) {
        this.expendGroupRepository = expendGroupRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.expendGroupMapper = expendGroupMapper;
    }

    /**
     * 2.7 지출유형 등록.
     *
     * <p>{@code default_group} 은 <b>{@code false} 고정</b>이다. 요청이 정할 수 없다 —
     * 정할 수 있으면 사용자가 스스로에게 {@code 3105}·{@code 3107} 보호를 걸어 두고
     * 그 유형을 영영 지우지 못하게 된다(api-contract.md §6).
     *
     * <p>이름 유일성은 <b>선검사와 유니크 위반 처리를 둘 다</b> 둔다(research.md §7).
     * 선검사만으로는 두 요청이 동시에 들어오는 창을 닫지 못하고, 위반 처리만 두면 정상적인
     * 중복이 전부 예외 경로로 흐른다.
     */
    @Transactional
    public ExpendGroupResponse create(AuthPrincipal principal, ExpendGroupCreateRequest request) {
        String name = request.name().trim();
        requireNameAvailable(principal.idKey(), name);

        UserExpendGroup group = new UserExpendGroup();
        group.setUser(ownerOf(principal));
        group.setName(name);
        group.setInUse(request.inUse());
        group.setDefaultGroup(false);
        group.setDeleted(false);

        return expendGroupMapper.toResponse(save(group));
    }

    /**
     * 2.8 관리 목록. <b>삭제 표시된 유형까지 전부</b> 돌려준다(FR-223).
     *
     * <p>페이징을 두지 않는다(FR-217) — 기본 10종에서 몇 개 늘어나는 정도다.
     */
    @Transactional(readOnly = true)
    public ExpendGroupListResponse<ExpendGroupResponse> list(AuthPrincipal principal) {
        return new ExpendGroupListResponse<>(expendGroupMapper.toResponses(
                expendGroupRepository.findByUserIdKeyOrderByIdxAsc(principal.idKey())));
    }

    /**
     * 2.13 사용 중 목록. 지출 등록·고정지출 화면이 고를 수 있는 유형만 돌려준다.
     *
     * <p><b>두 조건만 건다</b>: {@code in_use=true} · {@code deleted=false}.
     * 수단의 사용 중 목록(2.6)이 세 조건인 것과 다르다 — <b>지출유형에는 용도 구분이
     * 없다.</b> 인덱스 {@code ix_user_expend_group_active (id_key, in_use, deleted)} 에도
     * {@code purpose} 가 없어 그 사실이 DB 구조에 그대로 드러나 있다.
     *
     * <p>입력을 받지 않으므로 이 API 에는 값 오류가 없다 — 실패 코드가 {@code 1001} 하나뿐인
     * 이유다.
     */
    @Transactional(readOnly = true)
    public ExpendGroupListResponse<ExpendGroupActiveResponse> listActive(AuthPrincipal principal) {
        return new ExpendGroupListResponse<>(expendGroupMapper.toActiveResponses(
                expendGroupRepository.findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(
                        principal.idKey())));
    }

    /** 2.9 상세 조회. 삭제 표시된 유형도 보인다 — 관리 화면이 읽는다. */
    @Transactional(readOnly = true)
    public ExpendGroupResponse get(AuthPrincipal principal, Long expendGroupId) {
        return expendGroupMapper.toResponse(findOwned(principal, expendGroupId));
    }

    /**
     * 2.11 지출유형 수정. 판정 순서를 api-contract.md §5 그대로 따른다.
     *
     * <pre>{@code
     * 1. 대상 조회(본인 소유?)         없음·타인 → 3103
     * 2. name 을 보냈는가?
     *    ├ 예 + 기본 유형             → 3105
     *    └ 예 + 같은 회원에 그 이름   → 3101 (삭제 표시된 행도 센다)
     * 3. iconFile 을 보냈는가?        → 형식·크기 위반 → 3102 (US4)
     * 4. UPDATE
     * }</pre>
     *
     * <p><b>"필드가 왔는가"를 기본 유형 여부보다 먼저 본다.</b> 순서를 뒤집으면 기본 유형에
     * {@code inUse} 만 보낸 요청까지 {@code 3105} 로 막혀 SC-208 후반이 깨진다 — 기본 유형은
     * <b>이름만</b> 잠긴다(FR-220).
     *
     * <p><b>보내지 않은 필드는 그대로 둔다.</b> multipart 요청이라 "파트가 없음"이 곧 omit
     * 이며, 두 필드 모두 DB 가 NOT NULL 이라 "{@code null} 로 비우기"는 애초에 없다.
     *
     * @param name  보내지 않았으면 {@code null}
     * @param inUse 보내지 않았으면 {@code null}
     */
    @Transactional
    public ExpendGroupResponse update(AuthPrincipal principal, Long expendGroupId,
                                      String name, Boolean inUse) {
        UserExpendGroup group = findOwned(principal, expendGroupId);

        if (name != null) {
            String newName = requireName(name);
            if (!newName.equals(group.getName())) {
                if (Boolean.TRUE.equals(group.getDefaultGroup())) {
                    throw new BusinessException(ErrorCode.EXPEND_GROUP_DEFAULT_NAME_LOCKED);
                }
                requireNameAvailable(principal.idKey(), newName);
                group.setName(newName);
            }
            // 같은 이름을 그대로 보낸 요청은 통과시킨다. 자기 자신을 중복으로 세면
            // "이름은 두고 사용 여부만 바꾸는" 수정이 막힌다.
        }
        if (inUse != null) {
            group.setInUse(inUse);
        }

        return expendGroupMapper.toResponse(save(group));
    }

    /**
     * 2.12 지출유형 삭제 — <b>표시만 한다</b>. 행도 아이콘 파일도 남는다(FR-215).
     *
     * <pre>{@code
     * 1. 대상 조회(본인 소유?)      없음·타인 → 3103
     * 2. 이미 deleted=true         → 3108
     * 3. default_group=true        → 3107
     * 4. 그 유형을 쓴 지출이 있음   → 3106
     * 5. deleted=true 로 UPDATE
     * }</pre>
     *
     * <p>순서를 바꾸면 코드가 달라진다 — <b>이미 삭제된 기본 유형</b>을 다시 지우면
     * {@code 3108} 이지 {@code 3107} 이 아니다.
     *
     * <p><b>{@code tbl_expense} 하나만 본다.</b> 수단의 {@code purpose} 변경이 4개 테이블을
     * 보는 것과 다른 점이다 — FR-211 이 "삭제 표시돼도 목표금액·통계의 참조는 유지되어야
     * 한다"고 정했으므로, 그쪽에 참조가 있어도 삭제 표시는 된다.
     */
    @Transactional
    public ExpendGroupDeleteResponse delete(AuthPrincipal principal, Long expendGroupId) {
        UserExpendGroup group = findOwned(principal, expendGroupId);
        if (Boolean.TRUE.equals(group.getDeleted())) {
            throw new BusinessException(ErrorCode.EXPEND_GROUP_ALREADY_DELETED);
        }
        if (Boolean.TRUE.equals(group.getDefaultGroup())) {
            throw new BusinessException(ErrorCode.EXPEND_GROUP_DEFAULT_UNDELETABLE);
        }
        if (expenseRepository.existsByExpendGroupIdx(group.getIdx())) {
            throw new BusinessException(ErrorCode.EXPEND_GROUP_IN_USE);
        }
        group.setDeleted(true);
        return new ExpendGroupDeleteResponse(group.getIdx(), "지출 유형이 삭제되었습니다");
    }

    /**
     * 본인 소유 유형 1건. 없거나 남의 것이면 {@code 3103} 이다.
     *
     * <p>조회 조건에 {@code id_key} 를 <b>함께 건다.</b> 먼저 꺼내 놓고 소유자를 비교하는
     * 방식은 비교를 빠뜨린 자리가 곧 구멍이 된다.
     */
    private UserExpendGroup findOwned(AuthPrincipal principal, Long expendGroupId) {
        return expendGroupRepository.findByIdxAndUserIdKey(expendGroupId, principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_NOT_FOUND));
    }

    /** 감사 컬럼과 FK 에 쓸 소유 회원. 토큰이 가리키는 회원이 없다면 인증 자체가 이상한 것이다. */
    private User ownerOf(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 그 회원이 이미 쓰고 있는 이름인가. 쓰고 있으면 {@code 3101} 이다.
     *
     * <p><b>삭제 표시된 행도 센다</b>(FR-209). 유니크 제약
     * {@code ux_user_expend_group_name (id_key, name)} 에 {@code WHERE} 절이 없어 DB 도 같은
     * 기준으로 막는다 — 여기서 {@code deleted=false} 만 세면 통과시켰다가 DB 에서 터지고,
     * 그때는 {@code 3101} 이 아니라 {@code 9000} 이 나간다.
     */
    private void requireNameAvailable(Long idKey, String name) {
        if (expendGroupRepository.existsByUserIdKeyAndName(idKey, name)) {
            throw new BusinessException(ErrorCode.EXPEND_GROUP_NAME_DUPLICATED);
        }
    }

    /**
     * 저장하면서 유니크 위반을 {@code 3101} 로 옮긴다.
     *
     * <p>선검사와 INSERT 사이에 다른 요청이 같은 이름을 넣으면 여기서 걸린다. <b>즉시
     * flush 해야</b> 이 자리에서 잡을 수 있다 — 트랜잭션이 끝날 때 나가면 예외가 서비스
     * 밖에서 터져 {@code 9000} 이 된다.
     */
    private UserExpendGroup save(UserExpendGroup group) {
        try {
            return expendGroupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException e) {
            // 이 테이블의 유니크 제약은 ux_user_expend_group_name 하나뿐이라 위반은 곧 이름
            // 중복이다. 제약이 늘면 여기서 무엇이 걸렸는지 가려야 한다.
            throw new BusinessException(ErrorCode.EXPEND_GROUP_NAME_DUPLICATED);
        }
    }

    /** 이름은 비울 수 없다 — 컬럼이 NOT NULL 이다. */
    private static String requireName(String name) {
        if (name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "유형 이름은 비울 수 없습니다.");
        }
        return name.trim();
    }
}
