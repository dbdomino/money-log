package com.dbdomino.moneylog.repository;

import com.dbdomino.moneylog.entity.ExpendGroup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * C `save()`
 * R `findOne()`
 * R `findByName()`
 * U `update()`
 * D `delete()`
 */
@Repository
@Transactional
public class ExpendClassificationRepository {
    @PersistenceContext  // 엔티티 메니저(EntityManager) 주입
    private EntityManager em;

    // C
    public void save(ExpendGroup expendGroup) {
        em.persist(expendGroup);
    }

    // R
    public ExpendGroup findOne(Long id) {
        return em.find(ExpendGroup.class, id);
    }
    public List<ExpendGroup> findByName(String memberId) {
        // named 쿼리
        TypedQuery<ExpendGroup> query = em.createNamedQuery("ExpendClassification.findByUsername", ExpendGroup.class)
                .setParameter("memberId", memberId);
        List<ExpendGroup> expendGroups = query.getResultList();
        // jpql 쿼리
//        TypedQuery<ExpendClassification> query2 = em.createQuery("select m from ExpendClassification m where m.memberId = :memberId",ExpendClassification.class)
//                .setParameter("memberId", memberId);
//        List<ExpendClassification> expendClassifications2 = query.getResultList(); // arrayList로 반환
        return expendGroups;
    }

    // U
    public ExpendGroup updateExpendClassification(Long classificationId, String classificationName, Long limitCost, String sequence, String stat) {
        ExpendGroup expendGroup = findOne(classificationId);
        if (expendGroup == null) {
            throw new IllegalArgumentException("updateExpendClassification fail, Invalid classification ID: " + classificationId);
        }

        expendGroup.setExpendGroupName(classificationName);
        expendGroup.setLimitCost(limitCost);
        expendGroup.setSequence(sequence);
        expendGroup.setStat(stat);

        em.merge(expendGroup);

        return expendGroup;
    }

    // D
    public void delete(Long id) {
        ExpendGroup expendGroup = findOne(id);
        if (expendGroup == null) {
            throw new IllegalArgumentException("delete fail, Invalid classification ID: " + id);
        }
        em.remove(expendGroup);
    }
}
