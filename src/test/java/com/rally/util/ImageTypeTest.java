package com.rally.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 上传头像时靠文件头判断是不是图片 */
class ImageTypeTest {

    /** 把文件头补到 16 个字节（判断时至少要 12 个字节） */
    private static byte[] header(int... head) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < head.length; i++) {
            bytes[i] = (byte) head[i];
        }
        return bytes;
    }

    @Test
    void detectsCommonImageTypes() {
        assertEquals("jpg", ImageType.detectExtension(header(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("png", ImageType.detectExtension(header(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals("gif", ImageType.detectExtension(header('G', 'I', 'F', '8', '9', 'a')));
        assertEquals("webp", ImageType.detectExtension(header('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')));
    }

    @Test
    void rejectsNonImages() {
        // 一个改了后缀的 HTML 文件
        byte[] html = Arrays.copyOf("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8), 32);
        assertNull(ImageType.detectExtension(html));
        // SVG 里能写脚本，不支持
        assertNull(ImageType.detectExtension("<svg xmlns=\"http://www.w3.org/2000/svg\">".getBytes(StandardCharsets.UTF_8)));
        // 太短
        assertNull(ImageType.detectExtension(new byte[]{(byte) 0xFF, (byte) 0xD8}));
        assertNull(ImageType.detectExtension(null));
    }
}
