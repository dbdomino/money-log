package com.dbdomino.moneylog.backend.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * 업로드된 아이콘의 형식을 <b>파일 내용으로</b> 판정한다(FR-219).
 *
 * <p><b>확장자를 믿지 않는다.</b> 조회 API(2.10)가 저장된 확장자를 보고
 * {@code Content-Type} 을 정하므로, 확장자만 {@code .png} 로 바꾼 파일이 통과하면 서버가
 * "image/png 다"라고 말하면서 전혀 다른 바이트를 내보내게 된다. 브라우저가 그것을 어떻게
 * 해석할지는 서버가 통제할 수 없다.
 *
 * <p>판정을 {@code IconStorage} 와 분리한 이유는 <b>판정이 저장과 독립</b>이기 때문이다 —
 * 판정에 실패하면 저장을 시작조차 하지 않는다.
 *
 * <p>크기 검사는 여기서 하지 않는다. 호출자가 <b>디코딩 전에</b> 먼저 막아야 한다 —
 * 큰 파일을 먼저 디코딩하면 그만큼 메모리를 쓴다(icon-storage.md §2).
 */
@Component
public class ImageTypeDetector {

    /** 허용 형식 3종(FR-219). 이 목록 밖은 전부 {@code 3102} 다. */
    private static final Set<String> ALLOWED = Set.of("png", "jpg", "gif");

    /**
     * 바이트에서 형식을 알아내 <b>저장에 쓸 확장자</b>를 돌려준다.
     *
     * <p>{@code ImageIO} 는 JPEG 를 {@code JPEG} 로 보고하므로 {@code jpg} 로 정규화한다 —
     * 파일명에 들어갈 값이라 표기를 하나로 고정해야 조회 시 경로가 갈리지 않는다.
     *
     * @return 허용 형식이면 {@code png}·{@code jpg}·{@code gif} 중 하나, 아니면 빈 값
     */
    public Optional<String> detectExtension(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Optional.empty();
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                // 어떤 리더도 이 바이트를 이미지로 읽지 못한다 — 이미지가 아니다.
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension = "jpeg".equals(format) ? "jpg" : format;
                return ALLOWED.contains(extension) ? Optional.of(extension) : Optional.empty();
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            // 읽다가 깨진 파일이다. 형식을 정할 수 없으므로 거절 대상과 같게 다룬다.
            return Optional.empty();
        }
    }
}
