package com.dbdomino.moneylog.repository;

import com.dbdomino.moneylog.entity.PaymentMethod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * C `save()`
 * R `findOne()`
 * R `findPaymentMethodList()`
 * U `update()`
 * D `delete()`
 */
@Repository
@Transactional
public class PaymentRepository {
    @PersistenceContext  // 엔티티 메니저(EntityManager) 주입
    private EntityManager em;

    // C
    public void save(PaymentMethod paymentMethod) {
        em.persist(paymentMethod);
    }

    // R
    public PaymentMethod findOne(Long paymentMethodId) {
        TypedQuery<PaymentMethod> query = em.createQuery("select m from PaymentMethod m where m.paymentMethodId = :id ",PaymentMethod.class)
                .setParameter("id", paymentMethodId);

        PaymentMethod paymentMethod = query.getSingleResult(); // arrayList로 반환
        return paymentMethod;
    }
    public List<PaymentMethod> findPaymentMethodList(String memberId) {
        // named 쿼리
//        TypedQuery<ExpendClassification> query = em.createNamedQuery("ExpendClassification.findByUsername",ExpendClassification.class)
//                .setParameter("memberId", memberId);
//        List<ExpendClassification> expendClassifications = query.getResultList();
        // jpql 쿼리
        TypedQuery<PaymentMethod> query = em.createQuery("select m from PaymentMethod m where m.memberId = :memberId order by sequence desc",PaymentMethod.class)
                .setParameter("memberId", memberId);
//        List<PaymentMethod> paymentMethodList = query.getResultList(); // arrayList로 반환
        return query.getResultList();
    }

    // U
    public PaymentMethod update(Long paymentMethodId , String paymentMethodName, String sequence) {
        PaymentMethod paymentMethod = findOne(paymentMethodId);
        if (paymentMethod == null) {
            throw new IllegalArgumentException("update fail, Invalid paymentMethod ID: " + paymentMethodId);
        }

        paymentMethod.setPaymentMethodName(paymentMethodName);
        paymentMethod.setSequence(sequence);

        em.merge(paymentMethod);

        return paymentMethod;
    }

    // D
    public void delete(Long paymentMethodId) {
        PaymentMethod paymentMethod = findOne(paymentMethodId);
        if (paymentMethod == null) {
            throw new IllegalArgumentException("delete fail, Invalid paymentMethod ID: " + paymentMethodId);
        }

        em.remove(paymentMethod);
    }
}
