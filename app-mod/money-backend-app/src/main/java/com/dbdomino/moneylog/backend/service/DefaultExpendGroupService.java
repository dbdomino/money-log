package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.config.SelfAuditorContext;
import com.dbdomino.moneylog.backend.storage.IconStorage;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입(1.2)·관리자 회원 추가(1.12)가 만드는 <b>기본 지출유형 10종</b>(FR-106).
 *
 * <p>이 서비스는 {@code 003} 이 소유한 테이블에 쓴다. 경계가 어색해 보이지만 대안이 더
 * 나쁘다 — 가입 시점에 003 의 REST API 를 부르면 백엔드가 자기 자신에게 HTTP 요청을 보내는
 * 꼴이고, 가입 트랜잭션과 유형 생성 트랜잭션이 갈라져 <b>"회원은 생겼는데 유형이 없는"</b>
 * 상태가 가능해진다(research.md §11). 설계 명세도 "내부 로직으로 자동 등록"으로 정했다.
 *
 * <h2>저장 순서가 강제된다</h2>
 *
 * <p>아이콘 파일명에 {@code expendGroupId}({@code idx})가 들어가므로
 * ① 행을 저장해 PK 를 받고 → ② 시드 아이콘을 복사하고 → ③ 파일명을 갱신한다.
 * 순서를 뒤집으면 파일명을 정할 수 없다.
 */
@Service
public class DefaultExpendGroupService {

    /**
     * 기본 지출유형 10종. <b>"등"으로 열어 두지 않는다</b> — 회원마다 같은 목록이 생겨야
     * 유형 이름이 화면·통계에서 일관되게 읽힌다. 시드 아이콘 파일 이름도 이 값과 같다.
     */
    private static final List<String> DEFAULT_NAMES = List.of(
            "식비", "교통", "주거", "통신", "쇼핑", "장보기", "의료", "교육", "문화", "기타");

    private final UserExpendGroupRepository expendGroupRepository;
    private final IconStorage iconStorage;

    public DefaultExpendGroupService(UserExpendGroupRepository expendGroupRepository,
                                     IconStorage iconStorage) {
        this.expendGroupRepository = expendGroupRepository;
        this.iconStorage = iconStorage;
    }

    /**
     * 회원 1명에게 기본 10종을 만든다.
     *
     * <p>호출자의 트랜잭션에 참여한다({@code REQUIRED}). 가입이 실패하면 유형도 함께
     * 사라져야 하기 때문이다.
     *
     * <p>감사 컬럼은 <b>방금 만든 그 회원의 {@code id_key}</b> 다. 관리자가 회원을 추가한
     * 경우에도 마찬가지다 — 이 행들의 주인은 관리자가 아니라 그 회원이고, 가입 경로는
     * 인증 이전이라 {@code AuditorAware} 가 값을 주지도 못한다.
     */
    @Transactional
    public void createDefaults(User user) {
        // 감사자를 그 회원으로 두고 10건을 만든다. 가입은 인증 이전이라 SecurityContext 가
        // 비어 있고 이 테이블의 감사 컬럼은 NOT NULL 이다 — 값을 채우는 것은 여전히
        // AuditingEntityListener 이며 엔티티 세터로 직접 쓰지 않는다.
        SelfAuditorContext.runAs(user.getIdKey(), () -> {
            for (String name : DEFAULT_NAMES) {
                UserExpendGroup group = new UserExpendGroup();
                group.setUser(user);
                group.setName(name);
                group.setInUse(true);
                group.setDefaultGroup(true);
                group.setDeleted(false);

                // ① PK 를 받는다. 파일명이 이 값을 필요로 한다.
                UserExpendGroup saved = expendGroupRepository.saveAndFlush(group);
                // ② 시드를 회원별 복사본으로 만든다.
                String filename = iconStorage.copyFromSeed(name, user.getIdKey(), saved.getIdx());
                // ③ 파일명만 저장한다. 조회 경로·Base URL 은 응답을 만들 때 앞에 붙인다.
                saved.setIconFilename(filename);
                expendGroupRepository.saveAndFlush(saved);
            }
        });
    }

    /** 기본 유형 이름 10종. 테스트와 003 이 같은 목록을 참조하도록 열어 둔다. */
    public static List<String> defaultNames() {
        return DEFAULT_NAMES;
    }
}
