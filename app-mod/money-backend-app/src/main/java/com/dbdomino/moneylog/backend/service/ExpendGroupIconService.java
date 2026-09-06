package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.config.IconProperties;
import com.dbdomino.moneylog.backend.storage.IconStorage;
import com.dbdomino.moneylog.backend.storage.ImageTypeDetector;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 지출유형 아이콘의 업로드 검사·저장·조회.
 *
 * <p>{@link ExpendGroupService} 에서 갈라 둔 이유는 <b>이 일이 트랜잭션 밖에 있기</b>
 * 때문이다. 파일 시스템은 롤백되지 않으므로 DB 작업과 같은 자리에 섞어 두면 순서를
 * 지키기 어려워진다(icon-storage.md §2).
 *
 * <h2>검사 순서</h2>
 *
 * <pre>{@code
 * 1. 크기 (1MB 초과 → 3102)
 * 2. 내용으로 형식 판정 (png·jpg·gif 아니면 → 3102)
 * 3. 판정된 형식에서 확장자를 정한다
 * }</pre>
 *
 * <p><b>크기를 디코딩보다 먼저 본다.</b> 큰 파일을 먼저 디코딩하면 그만큼 메모리를 쓴다 —
 * 거절할 요청에 자원을 들이는 셈이다.
 *
 * <p><b>확장자를 믿지 않는다</b>(FR-219). 2.10 이 저장된 확장자를 보고
 * {@code Content-Type} 을 정하므로, 확장자만 {@code .png} 로 바꾼 파일이 통과하면 서버가
 * "image/png 다"라고 말하면서 전혀 다른 바이트를 내보내게 된다.
 *
 * @see <a href="../../../../../../../../specs/003-backend-payment-expend-group/contracts/icon-storage.md">icon-storage.md</a>
 */
@Service
public class ExpendGroupIconService {

    private static final Logger log = LoggerFactory.getLogger(ExpendGroupIconService.class);

    /**
     * 저장 확장자 → 응답 {@code Content-Type}.
     *
     * <p>확장자는 {@link ImageTypeDetector} 가 <b>내용으로</b> 판정해 만든 값이라 이 시점에는
     * 믿어도 된다(icon-storage.md §3 주의 3). {@code jpeg} 를 {@code jpg} 로 정규화해 두었기
     * 때문에 여기 키도 셋뿐이다.
     */
    private static final Map<String, MediaType> CONTENT_TYPES = Map.of(
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "gif", MediaType.IMAGE_GIF);

    private final IconStorage iconStorage;
    private final ImageTypeDetector imageTypeDetector;
    private final IconProperties iconProperties;

    public ExpendGroupIconService(IconStorage iconStorage,
                                  ImageTypeDetector imageTypeDetector,
                                  IconProperties iconProperties) {
        this.iconStorage = iconStorage;
        this.imageTypeDetector = imageTypeDetector;
        this.iconProperties = iconProperties;
    }

    /** 검사를 통과한 아이콘. 저장은 아직 하지 않았다. */
    public record IconContent(String extension, byte[] bytes) {
    }

    /** 디스크에서 읽어 온 아이콘. */
    public record StoredIcon(MediaType contentType, byte[] bytes) {
    }

    /**
     * 업로드 파트를 검사한다. <b>아직 저장하지 않는다</b> — 저장은 행이 있어야 파일명을
     * 정할 수 있어서 커밋 이후로 미룬다.
     *
     * <p>이 메서드를 저장과 나눈 것이 순서 계약의 핵심이다. 검사는 트랜잭션 안에서(값 판정
     * 다음에) 부르고, 저장은 커밋 뒤에 부른다.
     *
     * @param file 업로드 파트. 없거나 비어 있으면 <b>보내지 않은 것</b>으로 본다
     *             (2.11 의 omit = 기존 아이콘 유지)
     * @return 검사를 통과한 아이콘, 파트가 없으면 {@code null}
     * @throws BusinessException {@code 3102} — 1MB 초과이거나 png·jpg·gif 가 아니다
     */
    public IconContent validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > iconProperties.maxFileSizeBytes()) {
            // 디코딩 전에 막는다. 여기서 통과시키면 거절할 파일을 메모리에 펼치게 된다.
            throw new BusinessException(ErrorCode.EXPEND_GROUP_ICON_INVALID);
        }
        byte[] bytes = read(file);
        String extension = imageTypeDetector.detectExtension(bytes)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_ICON_INVALID));
        return new IconContent(extension, bytes);
    }

    /**
     * 검사를 마친 아이콘을 저장하고 파일명을 돌려준다.
     *
     * <p><b>행을 저장해 {@code expendGroupId} 를 받은 뒤</b>에만 부를 수 있다 — 파일명에 그
     * 값이 들어간다. 같은 유형이면 파일명이 같으므로 기존 파일을 덮어쓴다.
     */
    public String store(Long idKey, Long expendGroupId, IconContent icon) {
        return iconStorage.save(idKey, expendGroupId, icon.extension(), icon.bytes());
    }

    /**
     * 2.10 아이콘 조회. 파일이 없으면 {@code 3104} 다.
     *
     * <p>경로 정규화는 {@link IconStorage} 가 한다 — {@code filename} 이 Path Variable 이라
     * {@code ../../etc/passwd} 같은 값이 올 수 있다. 파일명을 <b>서버가 만드는 것</b>
     * (FR-224)과 조회 요청의 값을 믿는 것은 다른 문제다.
     *
     * <p>{@code Content-Type} 은 저장된 확장자에서 정한다. 알 수 없는 확장자면 파일이
     * 있어도 {@code 3104} 로 다룬다 — 형식을 말할 수 없는 바이트를 이미지라고 내보내지
     * 않는다.
     */
    public StoredIcon read(String filename) {
        MediaType contentType = CONTENT_TYPES.get(extensionOf(filename));
        if (contentType == null) {
            log.warn("icon extension not servable filename={}", filename);
            throw new BusinessException(ErrorCode.EXPEND_GROUP_ICON_NOT_FOUND);
        }
        byte[] bytes = iconStorage.read(filename)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPEND_GROUP_ICON_NOT_FOUND));
        return new StoredIcon(contentType, bytes);
    }

    /** 파일명의 확장자(소문자). 점이 없으면 빈 문자열이다. */
    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    /** 업로드 스트림을 읽는다. 읽다 끊긴 요청은 형식을 정할 수 없으므로 거절과 같게 다룬다. */
    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            log.warn("icon upload read failed name={}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.EXPEND_GROUP_ICON_INVALID);
        }
    }
}
