package com.rally.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改群组：{ name } */
public record UpdateGroupRequest(
        @NotBlank(message = "请填写群名称")
        @Size(max = 20, message = "群名称最多 20 个字")
        String name
) {
}
