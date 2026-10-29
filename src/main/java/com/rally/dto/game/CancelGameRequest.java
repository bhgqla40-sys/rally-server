package com.rally.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 组织者取消场次：{ reason }，原因必填（会显示给报了名的人看），最多 100 个字 */
public record CancelGameRequest(
        @NotBlank(message = "请填写取消原因")
        @Size(max = 100, message = "取消原因最多 100 个字")
        String reason
) {
}
