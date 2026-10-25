package com.rally.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改个人信息：{ nickname, bio }
 * bio 不传（null）表示不改；传空字符串表示清空。
 */
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 12, message = "昵称最多 12 个字")
        String nickname,

        @Size(max = 100, message = "个人介绍最多 100 个字")
        String bio
) {
}
