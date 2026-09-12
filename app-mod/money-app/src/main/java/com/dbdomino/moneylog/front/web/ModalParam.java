package com.dbdomino.moneylog.front.web;

import java.util.Optional;
import java.util.Set;
import org.springframework.ui.Model;

/**
 * 모달 딥링크 값을 <b>화이트리스트로</b> 판정한다.
 *
 * <p>아는 값이면 모델에 열 모달을 담고, 모르는 값이면 <b>키 자체를 담지 않는다.</b> 오류
 * 화면으로 보내지 않는 것이 요점이다 — 북마크를 잘못 저장했거나 주소를 오타로 친 사용자를
 * 막을 이유가 없다. 부모 페이지가 정상으로 뜨면 사용자는 하려던 일을 이어서 할 수 있다.
 *
 * <h2>서버가 판정하고 스크립트가 연다</h2>
 *
 * <p>여는 동작 자체는 브라우저의 모달 스크립트가 한다. 그런데도 서버가 판정하는 이유는
 * <b>상세·수정 모달이 열리기 전에 값을 가져와야 하기 때문</b>이다. 서버가 어떤 모달인지
 * 모르면 무엇을 조회해 넘길지 정할 수 없고, 결국 화면이 뜬 다음에 브라우저가 다시 요청하게
 * 된다.
 *
 * <p><b>화면별 값 목록은 007 이 정하지 않는다.</b> 008~012 가 자기 화면의 값을 모달 지도와
 * 함께 낸다. 여기 목록을 두면 화면이 늘 때마다 공통 기반을 고쳐야 한다.
 */
public final class ModalParam {

    /** 모델에 담기는 이름. 템플릿과 시험이 이 이름 하나를 본다. */
    public static final String MODEL_ATTRIBUTE = "openModal";

    private ModalParam() {
    }

    /**
     * 요청된 값이 그 화면이 아는 값인지 가린다.
     *
     * @param requested 주소에 실려 온 값. 없으면 {@code null}
     * @param allowed 그 화면이 여는 모달 값 목록
     * @return 열어도 되는 값, 아니면 빈 값
     */
    public static Optional<String> resolve(String requested, Set<String> allowed) {
        if (requested == null || requested.isBlank() || allowed == null) {
            return Optional.empty();
        }
        return allowed.contains(requested) ? Optional.of(requested) : Optional.empty();
    }

    /**
     * 판정 결과를 모델에 반영한다. 모르는 값이면 <b>아무것도 담지 않는다.</b>
     *
     * <p>빈 값을 담지 않는 이유는 템플릿이 "키가 있는가"만 보고 판단하게 하기 위해서다.
     * 빈 문자열을 담으면 화면마다 빈 값인지 없는 값인지 가리는 분기가 생긴다.
     */
    public static void applyTo(Model model, String requested, Set<String> allowed) {
        resolve(requested, allowed)
                .ifPresent(value -> model.addAttribute(MODEL_ATTRIBUTE, value));
    }
}
