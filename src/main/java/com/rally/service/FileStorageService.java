package com.rally.service;

import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.util.ImageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储：现在存在服务器本地磁盘（application.yml 里的 rally.upload.dir），
 * 由 WebConfig 把这个目录映射成 /api/uploads/** 供前端访问。
 *
 * 以后用户多了可以换成阿里云 OSS 之类的对象存储：只改这个类的实现，调用的地方都不用动。
 */
@Slf4j
@Service
public class FileStorageService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    /** 文件保存的根目录（绝对路径） */
    private final Path rootDir;

    /** 访问文件的 URL 前缀，例如 /api/uploads */
    private final String urlPrefix;

    public FileStorageService(@Value("${rally.upload.dir}") String dir,
                              @Value("${rally.upload.url-prefix}") String urlPrefix) {
        this.rootDir = Paths.get(dir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    public Path getRootDir() {
        return rootDir;
    }

    /**
     * 保存一张图片，返回访问地址，例如 /api/uploads/avatars/202610/3f2a...c1.png
     *
     * @param category 子目录，例如 avatars
     */
    public String saveImage(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择图片");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = ImageType.detectExtension(bytes);
            if (extension == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只支持 JPG、PNG、WEBP、GIF 格式的图片");
            }

            // 文件名用随机 UUID：不会重名，也猜不到别人的文件地址；按月份分目录，避免一个目录里文件太多
            String key = category + "/" + YearMonth.now().format(MONTH) + "/"
                    + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = rootDir.resolve(key).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return urlPrefix + "/" + key;
        } catch (IOException e) {
            throw new UncheckedIOException("保存文件失败", e);
        }
    }

    /** 删除之前上传的文件（换头像时删掉旧的）；不是我们存的文件就不管 */
    public void deleteIfLocal(String url) {
        if (url == null || !url.startsWith(urlPrefix + "/")) {
            return;
        }
        Path path = rootDir.resolve(url.substring(urlPrefix.length() + 1)).normalize();
        // 防止 url 里带 ../ 删到上传目录以外的文件
        if (!path.startsWith(rootDir)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除旧文件失败：{}", path, e);
        }
    }
}
