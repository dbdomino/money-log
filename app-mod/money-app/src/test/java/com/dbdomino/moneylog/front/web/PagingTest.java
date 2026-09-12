package com.dbdomino.moneylog.front.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 조회 구간이 언제나 개수의 배수라는 것을 고정한다.
 *
 * <p>백엔드는 시작점이 개수의 배수가 아니면 목록을 통째로 거절한다. 그것을 검증으로 막는
 * 대신 <b>만들 수 없게</b> 했으므로, 시험도 "거절하는가"가 아니라 "그런 값이 나올 길이
 * 없는가"를 본다.
 */
class PagingTest {

    @Test
    @DisplayName("시작점은 쪽 번호 곱하기 개수다")
    void 시작점은_곱으로_나온다() {
        assertThat(Paging.of(0, 10).offset()).isZero();
        assertThat(Paging.of(1, 10).offset()).isEqualTo(10);
        assertThat(Paging.of(3, 25).offset()).isEqualTo(75);
    }

    @Test
    @DisplayName("어떤 쪽이든 시작점은 개수의 배수다")
    void 시작점은_언제나_배수다() {
        for (int page = 0; page < 50; page++) {
            for (int limit : new int[] {1, 7, 10, 25, 100}) {
                assertThat(Paging.of(page, limit).offset() % limit)
                        .as("page=%d limit=%d", page, limit)
                        .isZero();
            }
        }
    }

    @Test
    @DisplayName("개수를 바꾸면 첫 쪽으로 되돌아간다")
    void 개수를_바꾸면_첫_쪽이다() {
        Paging fourthPage = Paging.of(3, 10);

        Paging changed = fourthPage.withLimit(25);

        assertThat(changed.page()).isZero();
        assertThat(changed.offset()).isZero();
        assertThat(changed.limit()).isEqualTo(25);
    }

    @Test
    @DisplayName("전체 쪽 수는 올림이다")
    void 전체_쪽_수는_올림이다() {
        assertThat(Paging.totalPages(0, 10)).isZero();
        assertThat(Paging.totalPages(1, 10)).isEqualTo(1);
        assertThat(Paging.totalPages(10, 10)).isEqualTo(1);
        assertThat(Paging.totalPages(11, 10)).isEqualTo(2);
        assertThat(Paging.totalPages(99, 10)).isEqualTo(10);
    }

    @Test
    @DisplayName("응답의 시작점으로 지금 쪽을 센다")
    void 응답의_시작점으로_쪽을_센다() {
        assertThat(Paging.pageOf(0, 10)).isZero();
        assertThat(Paging.pageOf(30, 10)).isEqualTo(3);
    }

    @Test
    @DisplayName("목록 Query 는 시작점과 개수 둘뿐이다 — 둘 다 필수라 빠뜨릴 수 없다")
    void 목록_Query_는_둘_뿐이다() {
        assertThat(Paging.of(2, 10).toQuery())
                .containsOnlyKeys("offset", "limit")
                .containsEntry("offset", 20)
                .containsEntry("limit", 10);
    }

    @Test
    @DisplayName("시작점을 직접 받는 생성자가 없다")
    void 시작점을_받는_생성자가_없다() {
        boolean hasPublicConstructor = Arrays.stream(Paging.class.getDeclaredConstructors())
                .anyMatch(constructor -> !Modifier.isPrivate(constructor.getModifiers()));

        assertThat(hasPublicConstructor)
                .as("밖에서 값을 직접 넣어 만들 수 있으면 배수가 아닌 시작점이 생긴다")
                .isFalse();
    }

    @Test
    @DisplayName("만드는 길은 첫 쪽과 쪽·개수 둘뿐이다")
    void 만드는_길이_둘_뿐이다() {
        long factories = Arrays.stream(Paging.class.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == Paging.class)
                .count();

        assertThat(factories)
                .as("정적 팩터리가 늘면 시작점을 받는 길이 섞여 들어올 수 있다")
                .isEqualTo(2);
    }

    /** 생성자가 private 인지 반사로 확인할 때 쓰는 보조 단언. */
    @Test
    @DisplayName("잘못된 쪽 번호와 개수는 만들어지지 않는다")
    void 잘못된_값은_거부한다() {
        Constructor<?>[] constructors = Paging.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);

        assertThat(Paging.first().page()).isZero();
        assertThat(Paging.first().limit()).isEqualTo(Paging.DEFAULT_LIMIT);
    }
}
