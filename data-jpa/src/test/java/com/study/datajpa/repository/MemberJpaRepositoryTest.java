package com.study.datajpa.repository;

import com.study.datajpa.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void 회원저장() throws Exception {
        // given
        Member member = new Member("memberA");
        // when
        Member savedMember = memberJpaRepository.save(member);
        // then
        assertThat(savedMember).isEqualTo(member);
    }

    @Test
    public void 회원찾기() throws Exception {
        // given
        Member member = new Member("memberA");
        Member savedMember = memberJpaRepository.save(member);
        // when
        Member findMember = memberJpaRepository.find(savedMember.getId());
        // then
        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);

    }

    @Test
    public void basicCRUD() throws Exception {
        // given
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);
        // 단건 조회 검증
        Member findMember1 = memberJpaRepository.findById(member1.getId()).get();
        Member findMember2 = memberJpaRepository.findById(member2.getId()).get();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);
        // 리스트 조회 검증
        List<Member> all = memberJpaRepository.findAll();
        assertThat(all.size()).isEqualTo(2);
        // 카운트 검증
        long count = memberJpaRepository.count();
        assertThat(count).isEqualTo(2);
        //삭제 검증
        memberJpaRepository.delete(member1);
        memberJpaRepository.delete(member2);
        //삭제 후 카운트 검증
        long deletedCount = memberJpaRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThan() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("AAA", 20);

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);
        // when
        List<Member> result = memberJpaRepository.findByUsernameAndAgeGreaterThan("AAA", 15);
        // then
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void NamedQuery() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("AAA", 20);

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);
        // when
        List<Member> result = memberJpaRepository.findByUsername("AAA");
        // then
        Member findMember = result.get(0);
        assertThat(findMember).isEqualTo(member1);
        assertThat(findMember.getUsername()).isEqualTo("AAA");
        assertThat(findMember.getAge()).isEqualTo(10);
    }
}