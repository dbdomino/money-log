package com.dbdomino.moneylog.repository;

import com.dbdomino.moneylog.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * C `save()`
 * R `findOne()`
 * R `findByName()`
 * R `findByIdPw()`
 * U `update()`
 * U `updateMemberPw()`
 * D `delete()`
 */
@Repository
@Transactional
@Slf4j
public class MemberRepository {
    @PersistenceContext  // 엔티티 메니저(EntityManager) 주입
    private EntityManager em;

    // C
    public void save(String memberId, String memberPw, String memberName, String memberEmail, String memberPhone, String memberAddress) {
        Member member = Member.builder()
                .id(memberId)
                .pw(memberPw)
                .name(memberName)
                .email(memberEmail)
                .phone(memberPhone)
                .address(memberAddress)
                .build();
//        Member member = new member();
//        member.setMember(memberId, memberPw,memberName,"dom@mii.com","01023242321","ddhh주소소");
        em.persist(member);
    }

    // R
    public Member findOne(String id) {
        TypedQuery<Member> query = em.createQuery("select m from Member m where m.memberId = :memberId and memberDel = 'N'",Member.class)
                .setParameter("memberId", id);
        Member member = query.getSingleResult();
        // 단일 객체로 반환
        return member;
    }
    public List<Member> findByName(String memberName) {
        // named 쿼리
//        TypedQuery<ExpendClassification> query = em.createNamedQuery("ExpendClassification.findByUsername",ExpendClassification.class)
//                .setParameter("memberId", memberId);
//        List<ExpendClassification> expendClassifications = query.getResultList();
        // jpql 쿼리
        TypedQuery<Member> query = em.createQuery("select m from Member m where m.memberName = :memberName and memberDel='N' ",Member.class)
                .setParameter("memberName", memberName);
        List<Member> members = query.getResultList();
        // arrayList로 반환
        return members;
    }


    public Optional<Member> findByIdPw(String id, String pw) {
        log.info("findById({}, {})",id, pw);

        TypedQuery<Member> query = em.createQuery("select m from Member m where m.memberId = :id and memberPw = :pw and memberDel = 'N'",Member.class)
                .setParameter("id", id)
                .setParameter("pw", pw);
        // Optional로 감싸서 반환
        Optional<Member> resultMember = query.getResultStream().findFirst();


        return resultMember;
    }

    // U
    public Member updateMember(String MemberId , String memberName, String memberEmail, String memberPhone) {
        Member member = findOne(MemberId);
        if (member == null) {
            throw new IllegalArgumentException("updateMember fail, Invalid member ID: " + MemberId);
        }

        member.setName(memberName);
        member.setEmail(memberEmail);
        member.setPhone(memberPhone);

        em.merge(member);

        return member;
    }
    public Member updateMemberPw(String memberId , String pw) {
        Member member = findOne(memberId);
        if (member == null) {
            throw new IllegalArgumentException("updateMember fail, Invalid member ID: " + memberId);
        }

        // encrypt pw
        member.setName(pw);
        // encrypt pw

        em.merge(member);

        return member;
    }

    // D
    public void delete(String memberId) {
        Member member = findOne(memberId);
        if (member == null) {
            throw new IllegalArgumentException("delete fail, Invalid member ID: " + memberId);
        }
        // encrypt pw
        member.setIsDel("Y");
        // encrypt pw

        em.merge(member);
    }
}
