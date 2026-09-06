package com.dbdomino.moneylog.backend.storage;

import com.dbdomino.moneylog.backend.config.IconProperties;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 지출유형 아이콘 파일. 이 프로젝트에서 <b>DB 밖에 실체가 있는 유일한 자원</b>이다.
 *
 * <p>파일명 규칙은 {@code {id_key}_{expendGroupId}.{확장자}} 하나이며 이 클래스가 소유한다.
 * 두 곳에서 각자 조립하면 규칙이 갈린다 — {@code 003}이 업로드·조회를 붙일 때 여기를
 * 재사용한다(003 icon-storage.md).
 *
 * <p><b>유형 이름과 로그인 아이디를 파일명에 넣지 않는다.</b> 파일명이 조회 URL 경로에
 * 그대로 실리므로 이름을 넣으면 {@code /}·{@code ..} 로 경로를 벗어나거나 공백·유니코드로
 * 인코딩이 어긋나고, 아이디를 넣으면 자식 데이터에 {@code user_id} 를 복사하는 것이 된다.
 * ID 기반이라 <b>유형 이름을 바꿔도 파일명이 그대로 유효하다</b>.
 */
@Component
public class IconStorage {

    private static final Logger log = LoggerFactory.getLogger(IconStorage.class);

    /** 시드 아이콘이 있는 클래스패스 경로. 읽기 전용이다. */
    private static final String SEED_DIRECTORY = "seed/expend-group-icons/";

    /** 시드 아이콘의 형식. 기본 10종은 전부 30×30 PNG 다. */
    private static final String SEED_EXTENSION = "png";

    private final IconProperties properties;

    public IconStorage(IconProperties properties) {
        this.properties = properties;
    }

    /**
     * 시드 아이콘을 회원별 복사본으로 만든다.
     *
     * <p><b>먼저 행을 저장해 {@code expendGroupId}({@code idx})를 받아야 부를 수 있다.</b>
     * 파일명에 그 값이 들어가기 때문이다. 순서를 뒤집으면 이름을 정할 수 없다.
     *
     * @param seedName      시드 파일 이름(확장자 제외). 지출유형 이름과 같다
     * @param idKey         회원 대리키
     * @param expendGroupId 방금 저장한 {@code tbl_user_expend_group.idx}
     * @return 저장된 파일명. 이 값만 DB 에 넣는다(경로·Base URL 은 넣지 않는다)
     */
    public String copyFromSeed(String seedName, Long idKey, Long expendGroupId) {
        String filename = filename(idKey, expendGroupId, SEED_EXTENSION);
        Path target = properties.directory().resolve(filename);
        ClassPathResource seed = new ClassPathResource(SEED_DIRECTORY + seedName + "." + SEED_EXTENSION);

        try (InputStream in = seed.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            // 아이콘 복사가 실패하면 가입 전체를 되돌린다. 유형은 있는데 아이콘만 없는
            // 회원을 만들면, 그 상태를 나중에 발견해 고칠 방법이 없다.
            log.error("seed icon copy failed seed={} target={}", seedName, target, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 업로드된 아이콘을 저장하고 파일명을 돌려준다.
     *
     * <p>같은 유형이면 파일명이 같으므로 <b>기존 파일을 덮어쓴다</b>. 확장자가 바뀌면
     * (png → jpg) 옛 파일이 남지만 {@code icon_filename} 이 새 이름을 가리키므로
     * 참조되지 않는 고아 파일이 된다 — 지울지는 정하지 않았다(icon-storage.md § 남은 판단).
     *
     * <p><b>행을 먼저 저장해 {@code expendGroupId} 를 받아야 부를 수 있다.</b> 파일명에 그
     * 값이 들어가기 때문이다.
     *
     * @param extension {@code ImageTypeDetector} 가 <b>내용으로</b> 판정한 확장자.
     *                  업로드 파일명의 확장자를 그대로 넘기지 않는다
     * @return 저장된 파일명. 이 값만 DB 에 넣는다
     */
    public String save(Long idKey, Long expendGroupId, String extension, byte[] bytes) {
        String filename = filename(idKey, expendGroupId, extension);
        try {
            Files.write(properties.directory().resolve(filename), bytes);
            return filename;
        } catch (IOException e) {
            log.error("icon save failed filename={}", filename, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 저장된 아이콘을 읽는다. 없으면 빈 값이다({@code 3104} 판정은 호출자가 한다).
     *
     * <p><b>경로 정규화를 반드시 거친다.</b> {@code filename} 은 조회 API(2.10)의 Path
     * Variable 이라 {@code ../../etc/passwd} 같은 값이 올 수 있다 — 서버가 <b>만드는</b>
     * 파일명이 ID 기반인 것(FR-224)과 조회 요청의 값을 믿는 것은 다른 문제다.
     */
    public Optional<byte[]> read(String filename) {
        return resolveInsideRoot(filename)
                .filter(Files::isRegularFile)
                .flatMap(path -> {
                    try {
                        return Optional.of(Files.readAllBytes(path));
                    } catch (IOException e) {
                        log.error("icon read failed path={}", path, e);
                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
                });
    }

    /** 저장된 아이콘이 있는가. 경로 정규화는 {@link #read(String)} 과 같다. */
    public boolean exists(String filename) {
        return resolveInsideRoot(filename).filter(Files::isRegularFile).isPresent();
    }

    /**
     * 요청받은 파일명을 저장 루트 안의 경로로 바꾼다. <b>루트를 벗어나면 빈 값이다.</b>
     *
     * <p>{@code normalize()} 로 {@code ..} 를 접은 뒤 루트로 시작하는지 확인한다 —
     * 접기 전에 문자열만 보고 판단하면 {@code a/../../b} 같은 입력을 놓친다.
     */
    private Optional<Path> resolveInsideRoot(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        Path root = properties.directory().toAbsolutePath().normalize();
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            log.warn("icon path escapes storage root filename={}", filename);
            return Optional.empty();
        }
        return Optional.of(target);
    }

    /** 파일명 규칙. 이 조립을 다른 곳에서 반복하지 않는다. */
    public String filename(Long idKey, Long expendGroupId, String extension) {
        return idKey + "_" + expendGroupId + "." + extension;
    }
}
