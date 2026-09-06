package com.dbdomino.moneylog.backend.icon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 아이콘 시험용 바이트를 만든다.
 *
 * <p>고정 파일을 저장소에 두지 않고 <b>그때그때 만든다.</b> 바이너리 픽스처는 diff 에
 * 보이지 않아 무엇이 들어 있는지 알 수 없고, 형식 판정을 시험하는 자리에서 "그 파일이
 * 정말 PNG 인지"를 아무도 확인하지 않게 된다. {@code ImageIO} 가 쓴 바이트는 정의상
 * 그 형식이다.
 */
final class IconTestImages {

    private IconTestImages() {
    }

    /** 실제 이미지 바이트. {@code format} 은 {@code png}·{@code jpg}·{@code gif} 다. */
    static byte[] image(String format, int size) {
        // JPEG 는 알파 채널을 쓰지 못해 TYPE_INT_ARGB 로 만들면 쓰기가 실패한다.
        int type = "jpg".equals(format) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage image = new BufferedImage(size, size, type);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format, out)) {
                throw new IllegalStateException("이 JVM 에 " + format + " writer 가 없다");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** 30×30 PNG. 기본 아이콘의 표시 규격과 같다. */
    static byte[] png() {
        return image("png", 30);
    }

    /**
     * 업로드 파트. <b>파일명과 내용을 따로 준다</b> — 확장자만 바꾼 파일을 만들 수 있어야
     * FR-219(내용으로 판정)를 시험할 수 있다.
     */
    static MockMultipartFile part(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("iconFile", filename, contentType, bytes);
    }

    /** 확장자·Content-Type 은 PNG 라고 말하지만 내용은 텍스트인 파트. */
    static MockMultipartFile disguisedText() {
        return part("hobby.png", MediaType.IMAGE_PNG_VALUE,
                "이것은 이미지가 아니라 그냥 텍스트다".getBytes(StandardCharsets.UTF_8));
    }

    /** 1MB 를 넘는 파트. 크기 검사가 형식 판정보다 먼저인지 보려고 내용은 이미지가 아니다. */
    static MockMultipartFile oversized() {
        return part("big.png", MediaType.IMAGE_PNG_VALUE, new byte[1024 * 1024 + 1]);
    }
}
