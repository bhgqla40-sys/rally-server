package com.rally.dto.game;

import jakarta.validation.constraints.NotNull;

/** 暂停 / 恢复固定局：{ active } */
public record SetTemplateActiveRequest(
        @NotNull(message = "请指定暂停还是恢复")
        Boolean active
) {
}
