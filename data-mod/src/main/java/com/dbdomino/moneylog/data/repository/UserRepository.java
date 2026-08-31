package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 이메일 중복 검사. 이메일은 선택 항목이라 {@code null}은 여러 건 허용되며,
     * 값이 있을 때만 이 검사가 의미를 갖는다. 중복이면 {@code 2003}.
     */
    boolean existsByEmail(String email);
}
