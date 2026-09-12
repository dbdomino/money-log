package com.dbdomino.moneylog.front.session;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 1.4 가 확인한 아이디·닉네임을 1.5 로 넘기는 표식. <b>008 이 만드는 유일한 상태</b>다.
 *
 * <p>읽고 쓰는 자리를 이 클래스 하나로 묶는다. {@link LoginSession} 과 같은 규율이며, 속성
 * 이름이 흩어지면 오타 하나가 "비밀번호 찾기가 자꾸 처음으로 되돌아간다"는 증상이 된다 —
 * 증상만 보고는 오타라는 것을 짐작하기 어렵다.
 *
 * <h2>왜 세션인가</h2>
 *
 * <p>1.5 의 저장 API 가 아이디·닉네임을 다시 요구하는데 사용자에게 두 번 묻지 않는 것이
 * FR-712 다. 주소에 실으면 아이디가 주소창·방문 기록·Referer 에 남는다. flash 로 넘기면 한
 * 번의 이동만 살아남아 <b>사용자가 1.5 에서 새로고침하면 처음으로 되돌아간다</b> — 비밀번호를
 * 고르는 중에 일어나기 쉬운 일이다.
 *
 * <h2>담지 않는 것</h2>
 *
 * <p><b>새 비밀번호를 담는 통로가 없다.</b> 세션에 평문이 머무는 시간을 만들지 않는다 — 그
 * 값은 저장 요청에만 실려 나간다. 확인 시각과 시도 횟수도 담지 않는다. 잠금·만료 정책은
 * 백엔드가 정할 일이고 002 에 없어서, 화면이 임의로 만들면 백엔드와 판단이 갈린다.
 *
 * <h2>실패해도 지우지 않는다</h2>
 *
 * <p>지우면 규칙에 안 맞는 비밀번호를 한 번 넣은 사용자가 1.4 부터 다시 해야 한다. 지우는
 * 시점은 <b>저장 성공 한 곳뿐</b>이다 — 남겨 두면 뒤로 가기로 같은 화면에 돌아와 다시 바꿀
 * 수 있다.
 */
@Component
public class PasswordResetMark {

    private static final String MEMBER_ID = "resetMemberId";
    private static final String NICKNAME = "resetNickname";

    /** 1.4 가 본인 확인에 성공했을 때 담는다. */
    public void mark(String memberId, String nickname) {
        HttpSession session = currentSession(true);
        if (session == null) {
            throw new IllegalStateException("요청 밖에서 재설정 표식을 담을 수 없다.");
        }
        session.setAttribute(MEMBER_ID, memberId);
        session.setAttribute(NICKNAME, nickname);
    }

    /** 확인된 아이디. 표식이 없으면 {@code null}. */
    public String memberId() {
        return attribute(MEMBER_ID);
    }

    /** 확인된 닉네임. 표식이 없으면 {@code null}. */
    public String nickname() {
        return attribute(NICKNAME);
    }

    /**
     * 표식이 있는가. 1.5 의 조회가 이것 하나로 폼을 그릴지 1.4 로 안내할지 정한다.
     *
     * <p>둘 중 하나만 있는 상태를 "있다"로 보지 않는다. 저장 요청에는 둘 다 필요해서, 반쪽인
     * 채로 폼을 그리면 사용자가 비밀번호를 고른 뒤에 실패한다.
     */
    public boolean exists() {
        return memberId() != null && nickname() != null;
    }

    /** 저장에 성공했을 때만 지운다. 세션 자체는 버리지 않는다 — 로그인 세션이 아니다. */
    public void clear() {
        HttpSession session = currentSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(MEMBER_ID);
        session.removeAttribute(NICKNAME);
    }

    private String attribute(String name) {
        HttpSession session = currentSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(name);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    /** 요청 밖에서 불려도 터지지 않는다. 읽기는 {@code null}, 담기만 예외다. */
    private HttpSession currentSession(boolean create) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(create);
    }
}
