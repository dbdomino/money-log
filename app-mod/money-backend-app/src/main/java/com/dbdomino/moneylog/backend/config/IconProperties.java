package com.dbdomino.moneylog.backend.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 지출유형 아이콘의 저장 위치.
 *
 * <p><b>기본값을 두지 않는다.</b> 기본값이 있으면 설정을 빠뜨린 채 기동해 임시 디렉터리
 * 같은 곳에 파일이 쌓이고, 재기동하면 사라지는 사고가 조용히 난다. 기동에서 막으면
 * 배포 시점에 드러난다.
 *
 * <p>클래스패스 안({@code resources/})을 가리킬 수 없다 — 배포하면 jar 내부가 되어 쓸 수
 * 없다. 시드 원본은 읽기 전용으로 클래스패스에 두고, 회원별 복사본만 이 디렉터리에 쌓인다.
 *
 * @see <a href="../../../../../../../../specs/003-backend-payment-expend-group/contracts/icon-storage.md">003 icon-storage.md</a>
 */
@Component
@ConfigurationProperties(prefix = "icon.storage")
public class IconProperties {

    /** 회원별 아이콘 복사본을 두는 디렉터리. */
    private String dir;

    @PostConstruct
    void validate() throws IOException {
        if (dir == null || dir.isBlank()) {
            throw new IllegalStateException(
                    "icon.storage.dir 이 비어 있다. ICON_STORAGE_DIR 환경변수를 주입해야 한다.");
        }
        // 없으면 만든다. 있는데 파일이면 여기서 걸린다 — 첫 가입 요청에서 터지는 것보다 낫다.
        Files.createDirectories(Path.of(dir));
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    /** 저장 디렉터리 경로. */
    public Path directory() {
        return Path.of(dir);
    }
}
