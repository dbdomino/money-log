package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원 조회 — {@code tbl_user}.
 *
 * <p>{@code JpaRepository}의 키 타입은 대리키 {@code id_key}(Long)다. API가 다루는
 * {@code memberId}는 {@code user_id}이므로, 로그인 아이디로 들어온 요청은
 * {@link #findByUserId(String)}으로 회원을 찾아 {@code id_key}를 얻은 뒤 자식
 * 데이터를 조회한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인 아이디로 회원 1건을 찾는다. 인증·관리자 조회의 진입점. */
    Optional<User> findByUserId(String userId);

    /** 아이디 중복 검사(가입·관리자 회원 추가). 중복이면 {@code 2002}. */
    boolean existsByUserId(String userId);

    /**
     * 이메일 중복 검사. 중복이면 {@code 2003}.
     *
     * <p>파생 쿼리({@code existsByEmail})를 쓰지 않는다. Spring Data는 파라미터가
     * {@code null}이면 {@code email = ?}를 <b>{@code email IS NULL}로 바꿔</b> 생성하는데,
     * 이메일은 선택 항목이라 비어 있는 회원이 이미 여럿 존재한다. 그러면 이메일을
     * 입력하지 않은 가입이 "중복"으로 판정돼 막힌다.
     *
     * <p>술어를 직접 고정해 {@code null}이 들어와도 항상 {@code false}가 되게 한다.
     * 부분 유니크 인덱스 {@code ux_user_email}(값이 있을 때만 유일)과 조건이 같다.
     */
    @Query("select count(u) > 0 from User u where u.email is not null and u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 이메일로 회원 1건을 찾는다. 아이디 찾기(1.9)의 본인 확인 수단이다.
     *
     * <p>{@link #existsByEmail(String)}과 <b>같은 이유</b>로 파생 쿼리를 쓰지 않는다.
     * Spring Data 는 파라미터가 {@code null}이면 {@code email = ?}를
     * {@code email IS NULL}로 바꿔 생성하는데, 이메일은 선택 항목이라 비어 있는 회원이
     * 여럿이다. 그러면 이메일 없이 보낸 요청이 <b>아무 회원이나</b> 찾아내게 된다.
     *
     * <p>부분 유니크 인덱스 {@code ux_user_email}이 값이 있을 때의 유일성을 보장하므로
     * 결과는 0건 또는 1건이다.
     */
    @Query("select u from User u where u.email is not null and u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 관리자 회원 목록(1.13). 아이디·닉네임 <b>부분 일치</b> 검색이며 둘 다 주어지면 AND 다.
     *
     * <p><b>검색어가 없으면 빈 문자열을 넘긴다.</b> {@code null} 을 넘기고
     * {@code :memberId is null or ...} 로 분기하면 PostgreSQL 이 파라미터 타입을 정하지
     * 못해 {@code function lower(bytea) does not exist} 로 실패한다 — 드라이버가 타입 없는
     * {@code null} 을 보내기 때문이다. 빈 문자열이면 {@code like '%%'} 가 되어 모든 행이
     * 걸리므로 "검색하지 않음"과 뜻이 같고, 분기 자체가 사라진다({@code user_id}·
     * {@code nickname} 은 NOT NULL 이라 빠지는 행도 없다).
     *
     * <p>정렬을 고정한다({@code id_key} 오름차순). 정렬이 없으면 PostgreSQL 이 페이지마다
     * 다른 순서를 줄 수 있어 같은 행이 두 페이지에 나오거나 아예 빠진다.
     *
     * <p>{@code Pageable} 을 받지만 <b>Controller 에서 자동 바인딩하지 않는다</b>. 계약이
     * {@code offset}·{@code limit} 이고 기본값이 없어서(둘 다 필수, 없으면 {@code 9001}),
     * 자동 바인딩을 쓰면 값이 빠졌을 때 기본값으로 조용히 통과한다.
     */
    @Query("""
            select u from User u
             where lower(u.userId) like lower(concat('%', :memberId, '%'))
               and lower(u.nickname) like lower(concat('%', :nickname, '%'))
             order by u.idKey asc
            """)
    List<User> search(@Param("memberId") String memberId,
                      @Param("nickname") String nickname,
                      Pageable pageable);

    /**
     * 위 검색 조건에 걸리는 <b>전체 건수</b>. 응답의 {@code totalCount} 다.
     *
     * <p>현재 페이지 건수가 아니다 — 화면이 마지막 페이지를 계산하려면 전체가 필요하다.
     */
    @Query("""
            select count(u) from User u
             where lower(u.userId) like lower(concat('%', :memberId, '%'))
               and lower(u.nickname) like lower(concat('%', :nickname, '%'))
            """)
    long countSearch(@Param("memberId") String memberId, @Param("nickname") String nickname);
}
