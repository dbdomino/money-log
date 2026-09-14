package com.dbdomino.moneylog.front.support;

import java.util.regex.Pattern;

/**
 * 백엔드가 준 아이콘 주소에서 <b>파일명만 꺼낸다</b>.
 *
 * <h2>왜 파일명만인가</h2>
 *
 * <p>화면이 주소 전체를 들고 있으면 그것이 그대로 이미지에 걸릴 길이 생기고, 그 순간
 * <b>브라우저가 백엔드를 직접 부른다.</b> 아이콘 조회에도 인증이 필요한데 브라우저의 이미지
 * 태그는 인증을 붙이지 못해 전부 실패하며, 그 실패는 이미지 깨짐으로만 보여 원인을 짐작하기
 * 어렵다.
 *
 * <p>파일명만 들고 있으면 화면이 쓸 수 있는 주소는 007 의 중계 경로 하나뿐이다. 백엔드가
 * 주소의 앞부분을 바꿔도 화면이 따라 고칠 것이 없다.
 *
 * <h2>틀에 맞지 않으면 없는 것으로 본다</h2>
 *
 * <p>007 의 중계가 같은 틀을 검사하므로, 여기서 거르지 않아도 그쪽에서 막힌다. 그래도 앞에서
 * 거르는 이유는 <b>빈 주소를 건 이미지가 브라우저에게는 현재 페이지를 다시 받아 오라는 뜻</b>
 * 이기 때문이다 — 이미지 자리에 페이지가 들어오면서 깨진 표시가 남는다. 걸지 않는 것과
 * 빈 값을 거는 것은 다르다.
 */
public final class IconUrls {

    /**
     * 백엔드가 정한 파일명 틀 {@code {회원키}_{지출유형번호}.{확장자}}.
     *
     * <p>007 의 중계가 쓰는 것과 같은 틀이다. 두 곳에 적혀 있지만 한쪽은 <b>화면에 걸지 말지</b>
     * 를 정하고 다른 쪽은 <b>백엔드를 부를지</b> 를 정한다 — 역할이 달라 한쪽을 지우면 다른
     * 쪽이 하던 일이 사라진다.
     */
    private static final Pattern FILENAME = Pattern.compile("^\\d+_\\d+\\.(png|jpg|gif)$");

    private IconUrls() {
    }

    /**
     * 아이콘 주소에서 파일명을 꺼낸다.
     *
     * @param iconUrl 백엔드가 준 주소. 아이콘이 없으면 {@code null} 이다
     * @return 파일명. 주소가 없거나 틀에 맞지 않으면 {@code null}
     */
    public static String filenameOf(String iconUrl) {
        if (iconUrl == null || iconUrl.isBlank()) {
            return null;
        }
        String candidate = iconUrl.substring(iconUrl.lastIndexOf('/') + 1);
        return FILENAME.matcher(candidate).matches() ? candidate : null;
    }

    /**
     * 화면에 아이콘을 걸 수 있는가.
     *
     * <p>템플릿이 이 판단 하나로 이미지를 그릴지 대체 표시를 그릴지 정한다. 파일명이
     * {@code null} 인지 보는 것과 같지만, 템플릿에서 읽을 때 뜻이 분명하다.
     */
    public static boolean hasIcon(String filename) {
        return filename != null && !filename.isBlank();
    }
}
