package com.rally.dto.bill;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 添加费用项：{ name, amount }，金额单位是分 */
public record AddBillItemRequest(
        @NotBlank(message = "请填写费用名称")
        @Size(max = 20, message = "费用名称最多 20 个字")
        String name,

        @NotNull(message = "请填写金额")
        @Min(value = 1, message = "金额要大于 0")
        @Max(value = 10_000_000, message = "单项金额最多 10 万元")
        Integer amount
) {
}
