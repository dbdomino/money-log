package com.dbdomino.moneylog.service;

import com.dbdomino.moneylog.entity.Member;
import com.dbdomino.moneylog.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@Slf4j
@SpringBootTest
class MemberServiceTest {
    MemberRepository memberRepository = new MemberRepository();
    @Autowired
    MemberService memberService = new MemberService(memberRepository);

    @AfterEach
    void afterEach() {

    }
    @Test
    public void signUp() {
        String id = "hhoo1";
        String pw = "hhoopw1";
        String name = "멋져어";

        Member member = memberService.signUp(id,pw, name,"dfd@dfd.com","01011112222","address---");
        log.info("member : {}",member);
    }

    @Test
    public void loginTest() {
        //given
//        String id = "minod";
//        String pw = "minod";
        String id = "hhoo1";
        String pw = "hhoopw1";
        //when
        Optional<Member> memberOptional = memberRepository.findByIdPw(id, pw);
        //then
        Assertions.assertThat(memberOptional.isPresent()).isTrue();

    }
}