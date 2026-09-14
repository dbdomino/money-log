package com.dbdomino.moneylog.front.expendgroup.form;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * 2.6 지출유형 등록과 2.8 수정이 함께 쓰는 폼. 칸 셋이다.
 *
 * <h2>파일을 고르지 않았으면 그 칸을 아예 싣지 않는다</h2>
 *
 * <p>백엔드 수정은 "보낸 칸만 갱신"이라 파일 칸을 빼면 기존 아이콘이 남는다. 그런데
 * <b>브라우저의 파일 입력은 고르지 않아도 빈 값을 함께 보낸다.</b> 그것을 그대로 옮기면
 * 0바이트 파일을 올리는 요청이 되어 형식 오류로 거절되고, 사용자에게는 <b>"이름만 고쳤는데
 * 아이콘이 잘못됐다고 나온다"</b>로 보인다.
 *
 * <p>그래서 비어 있는지 판정하는 자리를 여기 한 곳에 둔다 — 등록과 수정이 같은 판정을 쓴다.
 *
 * <h2>아이콘을 지우는 길이 없다</h2>
 *
 * <p>백엔드에 그 동작이 없다. 파일 칸을 비운 것이 "지우라"는 뜻이 되지 않으며, 화면에 지우기
 * 버튼을 두면 누를 수 없는 버튼이 된다.
 *
 * @param name 유형 이름 (필수)
 * @param inUse 사용 여부 (필수)
 * @param iconFile 아이콘 파일 (선택). 고르지 않았으면 비어 있다
 */
public record ExpendGroupForm(String name, Boolean inUse, MultipartFile iconFile) {

    /**
     * 백엔드 요청 본문으로 옮긴다. 등록과 수정이 같은 모양을 쓴다.
     *
     * <p>등록에서는 파일 칸을 빼면 아이콘 없는 유형이 되고, 수정에서는 기존 아이콘이 남는다 —
     * 보내는 쪽에서는 같은 일이고 뜻만 화면마다 다르다.
     */
    public MultiValueMap<String, Object> toParts() {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("name", name);
        parts.add("inUse", inUse);
        if (hasIconFile()) {
            parts.add("iconFile", toResource(iconFile));
        }
        return parts;
    }

    /**
     * 사용자가 파일을 실제로 골랐는가.
     *
     * <p>{@code null} 인지와 비어 있는지를 함께 본다 — 고르지 않은 파일 칸은 {@code null} 이
     * 아니라 <b>내용이 없는 값</b>으로 온다.
     */
    public boolean hasIconFile() {
        return iconFile != null && !iconFile.isEmpty();
    }

    /**
     * 파일을 이름과 함께 실을 수 있는 모양으로 바꾼다.
     *
     * <p>파일 이름을 넘기지 않으면 보내는 쪽이 그 칸을 일반 값으로 다뤄 백엔드가 파일로 받지
     * 못한다. 이름은 <b>사용자가 고른 그대로</b> 넘기고 화면이 손대지 않는다 — 저장 이름은
     * 백엔드가 자기 규칙으로 다시 정한다.
     */
    private static Resource toResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (java.io.IOException e) {
            // 브라우저가 보낸 파일을 읽지 못했다. 화면이 지어낼 값이 없으므로 그대로 올린다.
            throw new IllegalStateException("올린 파일을 읽지 못했습니다.", e);
        }
    }
}
