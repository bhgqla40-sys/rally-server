package com.rally.dto.group;

import com.rally.enums.SportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建群组的请求体：{ name, sport } */
public record CreateGroupRequest(
        @NotBlank(message = "请填写群名称")
        @Size(max = 20, message = "群名称最多 20 个字")
        String name,

        @NotNull(message = "请选择运动类型")
        SportType sport
) {
}
