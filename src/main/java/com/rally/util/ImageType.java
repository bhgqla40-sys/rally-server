package com.rally.util;

/**
 * 根据文件开头的几个字节（文件头）判断图片类型。
 *
 * 不能只看文件名后缀或浏览器传来的 Content-Type，这两个都可以随便伪造，
 * 比如把一个 .html 文件改名成 .png 上传，就可能被用来做 XSS 攻击。
 * 文件头是文件内容本身的一部分，伪造不了。
 */
public final class ImageType {

    private ImageType() {
    }

    /** 返回扩展名（jpg / png / gif / webp）；不是支持的图片返回 null */
    public static String detectExtension(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        // JPEG：FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG：89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        // GIF：GIF87a / GIF89a
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a') {
            return "gif";
        }
        // WEBP：RIFF????WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
