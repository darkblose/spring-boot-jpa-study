package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.dto.UserDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static com.study.querydsl.entity.QMember.member;
import static com.study.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    public void before() {
        query = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        // given
        // when
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {
        // given
        // when
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        // given
        // when
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() throws Exception {
        // given
        // when
        Member findMember = query
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() throws Exception {
        // given
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

//        Member fetchOne = query
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        int totalSize = query
                .selectFrom(member)
                .fetch().size();

        Long totalCount = query
                .select(member.count())
                .from(member)
                .fetchOne();
    }

    /**
     * ?????? ?????? ??????
     * 1.?????? ?????? ???????????? (desc)
     * 2.?????? ?????? ???????????? (asc)
     * ??? 2?????? ?????? ????????? ????????? ???????????? ?????? (nulls last)
     */
    @Test
    public void sort() throws Exception {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        // when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        // then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() throws Exception {
        // given
        // when
        List<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        // then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() throws Exception {
        // given
        // when
        QueryResults<Member> queryResults = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        // then
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() throws Exception {
        // given
        // when
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        // then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????.
     */
    @Test
    public void group() throws Exception {
        // when
        List<Tuple> result = query
                .select(team, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        // then
        System.out.println("teamA = " + teamA);
        System.out.println("teamB = " + teamB);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
    }

    /**
     * ??? A??? ????????? ?????? ??????
     */
    @Test
    public void join() throws Exception {
        // given
        // when
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ????????? ??????
     */
    @Test
    public void theta_join() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        // when
        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        // given
        // when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
//                .join(member.team, team)
//                .on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();
        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ??????????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    public void join_on_no_relation() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        // when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        // given
        em.flush();
        em.clear();
        // when
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {
        // given
        em.flush();
        em.clear();
        // when
        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        // then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("?????? ?????? ??????").isTrue();
    }

    /**
     * ????????? ?????? ?????? ?????? ??????
     */
    @Test
    public void subQuery() throws Exception {
        // given
        // when
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(
                        member.age.eq(
                                select(memberSub.age.max())
                                        .from(memberSub)
                        )
                )
                .fetch();
        // then
        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * ????????? ?????? ????????? ?????? ??????
     */
    @Test
    public void subQueryGoe() throws Exception {
        // given
        // when
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(
                        member.age.goe(
                                select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                )
                .fetch();
        // then
        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() throws Exception {
        // given
        // when
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(
                        member.age.in(
                                select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        )
                )
                .fetch();
        // then
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() throws Exception {
        // given
        // when
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = query
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();
        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() throws Exception {
        // given
        // when
        List<String> result = query
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        // given
        // when
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        // given
        // when
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        // given
        // when
        // {username}_{age}
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        // given
        // when
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {
        // given
        // when
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();
        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() throws Exception {
        // given
        // when
        List<MemberDto> result = em.createQuery(
                "select new com.study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class
        ).getResultList();
        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() throws Exception {
        // given
        // when
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() throws Exception {
        // given
        // when
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() throws Exception {
        // given
        // when
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        // given
        // when
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = query
                .select(Projections.fields
                        (UserDto.class,
                                member.username.as("name"),

                                ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();
        // then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * Constructor ????????? Runtime Error ??? ?????????.
     * QueryProjection ??? Compile Error ??? ????????? ??? ??????.
     * ?????????, 2?????? ????????? ?????????
     * 1. Q?????? ??????
     * 2. ???????????? ????????????: DTO ??? QueryDsl ??? ?????? ???????????? ?????? ???. DTO ??? ?????? layer ?????? ???????????? ?????????, ??? ???????????? Dto -> QueryDsl ??? ?????? ???????????? ???.
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        // given
        // when
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        // then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;
        // when
        List<Member> result = searchMember1(usernameParam, ageParam);
        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(usernameCond)) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;
        // when
        List<Member> result = searchMember2(usernameParam, ageParam);
        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (StringUtils.hasText(usernameCond)) {
            return member.username.eq(usernameCond);
        }
        return null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond != null) {
            return member.age.eq(ageCond);
        }
        return null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() throws Exception {
        // given
        // when
        /**
         * member1 = 10 -> ?????????
         * member2 = 20 -> ?????????
         * member3 = 30 -> ??????
         * member4 = 40 -> ??????
         */
        long count = query
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();
        // then
        List<Member> result = query
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }

        em.flush();
        em.clear(); //repeatable read ????????? ?????? ????????? ???????????? ?????????

        List<Member> renew = query
                .selectFrom(member)
                .fetch();

        for (Member member : renew) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void bulkAdd() throws Exception {
        // given
        // when
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        // then
    }

    @Test
    public void bulkDelete() throws Exception {
        // given
        // when
        long count = query
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        // then
    }

    @Test
    public void sqlFunction() throws Exception {
        // given
        // when
        List<String> result = query
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})", member.username, "member", "M")
                ).from(member)
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        // given
        // when
        List<String> result = query
                .select(member.username)
                .from(member)
                /*
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)
                ))
                */
                .where(member.username.eq(member.username.lower()))
                .fetch();
        // then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}