package com.dbdomino.moneylog.front.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 목록 화면의 페이지 번호를 백엔드가 요구하는 조회 구간으로 바꾼다.
 *
 * <p>백엔드 목록 API 는 페이지 번호가 아니라 <b>시작점과 개수</b>를 받고 둘 다 필수다.
 * 시작점이 개수의 배수가 아니면 목록이 통째로 실패한다.
 *
 * <h2>시작점을 받는 생성자를 두지 않았다</h2>
 *
 * <p>배수가 아닌 값을 걸러내는 검증을 넣는 대신 <b>만들 수 없게</b> 했다. 시작점은 페이지
 * 번호에 개수를 곱해서만 나오므로 어긋난 값이 생길 경로 자체가 없다. 검증은 빠뜨릴 수 있고
 * 빠뜨린 자리는 사람이 세야 하지만, 없는 길은 셀 것도 없다.
 *
 * <p>화면에 사용자가 시작점을 직접 넣는 입력을 두지 않는 것도 같은 이유다.
 */
public final class Paging {

    /** 한 쪽에 담는 기본 개수. 화면이 고르게 하려면 {@link #of(int, int)} 를 쓴다. */
    public static final int DEFAULT_LIMIT = 10;

    private final int page;
    private final int limit;

    private Paging(int page, int limit) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이다: " + page);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("한 쪽에 담는 개수는 1 이상이다: " + limit);
        }
        this.page = page;
        this.limit = limit;
    }

    /** 첫 쪽. 개수는 기본값을 쓴다. */
    public static Paging first() {
        return new Paging(0, DEFAULT_LIMIT);
    }

    /**
     * 쪽 번호와 개수로 만든다.
     *
     * @param page 0 부터 세는 쪽 번호
     * @param limit 한 쪽에 담는 개수
     */
    public static Paging of(int page, int limit) {
        return new Paging(page, limit);
    }

    /** 백엔드에 보낼 조회 시작점. 언제나 {@link #limit()} 의 배수다. */
    public int offset() {
        return page * limit;
    }

    public int page() {
        return page;
    }

    public int limit() {
        return limit;
    }

    /** 다음 쪽. 마지막 쪽인지는 화면이 {@link #totalPages(long, int)} 로 판단한다. */
    public Paging next() {
        return new Paging(page + 1, limit);
    }

    /** 이전 쪽. 첫 쪽에서는 그대로다. */
    public Paging previous() {
        return page == 0 ? this : new Paging(page - 1, limit);
    }

    /**
     * 한 쪽에 담는 개수를 바꾼다. <b>쪽 번호는 첫 쪽으로 되돌린다.</b>
     *
     * <p>번호를 그대로 두면 시작점이 새 개수의 배수가 아니게 되어 목록이 통째로 실패한다.
     * 사용자에게는 "개수를 바꿨더니 목록이 사라졌다"로 보인다.
     */
    public Paging withLimit(int newLimit) {
        return new Paging(0, newLimit);
    }

    /** 목록 API 에 실을 Query. {@code BackendApiClient.getByQuery} 에 그대로 넘긴다. */
    public Map<String, Object> toQuery() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("offset", offset());
        query.put("limit", limit);
        return query;
    }

    /** 전체 쪽 수. 건수가 0 이면 0 쪽이다. */
    public static int totalPages(long totalCount, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("한 쪽에 담는 개수는 1 이상이다: " + limit);
        }
        if (totalCount <= 0) {
            return 0;
        }
        return (int) ((totalCount + limit - 1) / limit);
    }

    /**
     * 응답이 돌려준 시작점으로 지금 몇 쪽인지 센다. 쪽 번호 표시에만 쓴다.
     *
     * <p>여기서 나온 번호로 {@link #of(int, int)} 를 부르면 시작점이 다시 배수로 맞춰진다.
     */
    public static int pageOf(int offset, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("한 쪽에 담는 개수는 1 이상이다: " + limit);
        }
        return Math.max(offset, 0) / limit;
    }
}
