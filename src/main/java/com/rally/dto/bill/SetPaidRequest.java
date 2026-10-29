package com.rally.dto.bill;

import jakarta.validation.constraints.NotNull;

/** 组织者标记已付 / 未付：{ paid } */
public record SetPaidRequest(
        @NotNull(message = "请指定已付还是未付")
        Boolean paid
) {
}
