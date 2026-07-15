package com.dbdomino.moneylog.repository;

import com.dbdomino.moneylog.entity.Ammount;
import com.dbdomino.moneylog.repository.itf.AmmountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** @since  2024-12-28
 * @author dbdomino
 *
 * Ammount 잔고 를 나타내는 객체의 Repository를 구현
 *
 * @method
 * - List<Ammount> findAll()
 * - Ammount findById(Long id)
 * - void save(Ammount ammount)
 * - void delete(Ammount ammount)
 */
@Repository
public class AmmountRepositoryImpl implements AmmountRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Ammount> findAll() {
        return em.createQuery("SELECT a FROM Ammount a", Ammount.class).getResultList();
    }

    public Optional<Ammount> findById(Long id) {
        return Optional.ofNullable(em.find(Ammount.class, id));
    }

    @Transactional
    public void save(Ammount ammount) {
        if (ammount.getId() == null) {
            em.persist(ammount); // id가 없으면 ammount 신규생성
        } else {
            em.merge(ammount); // id 있으면 ammount 수정, 수정사항을 영속성컨텍스트에서 db로 적용수행
        }
    }

    @Transactional
    public void delete(Ammount ammount) {
        em.remove(ammount);
    }

    public List<Ammount> findByMemberId(String memberId) {
        return em.createQuery("SELECT a FROM Ammount a WHERE a.memberId = :memberId", Ammount.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

}
