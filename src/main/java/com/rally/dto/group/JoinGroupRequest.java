package com.rally.dto.group;

import jakarta.validation.constraints.NotBlank;

/** 用邀请码加入群组的请求体：{ code } */
public record JoinGroupRequest(
        @NotBlank(message = "请输入邀请码")
        String code
) {
}
