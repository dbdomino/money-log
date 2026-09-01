package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.User;
import java.util.Optional;
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
}
