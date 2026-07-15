package com.dbdomino.moneylog.service;

import com.dbdomino.moneylog.entity.Member;
import com.dbdomino.moneylog.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member memberSearch(String memberId, String memberPw) {
        Optional<Member> memberOptional = memberRepository.findByIdPw(memberId, memberPw);
        if (memberOptional.isPresent()){
            return memberOptional.get();
        } else {
            throw new RuntimeException("E110");
        }


    }

    public boolean login(String memberId, String memberPw) {
        Optional<Member> memberOptional = memberRepository.findByIdPw(memberId, memberPw);
        return memberOptional.isPresent();
        // 로그인 성공이면 true, 실패면 false
    }

    public Member signUp(String memberId, String memberPw, String memberName, String memberEmail, String memberPhone, String memberAddress) {

        memberRepository.save( memberId, memberPw,memberName,memberEmail,memberPhone,memberAddress);
        // 로그인 성공이면 true, 실패면 false
        return memberRepository.findOne(memberId);
    }
}
