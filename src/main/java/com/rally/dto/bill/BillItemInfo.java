package com.rally.dto.bill;

import com.rally.entity.BillItem;

/** 费用项，对应前端 types/bill.ts 的 BillItem */
public record BillItemInfo(
        Long id,
        String name,
        /* 金额（分） */
        Integer amount
) {
    public static BillItemInfo of(BillItem item) {
        return new BillItemInfo(item.getId(), item.getName(), item.getAmount());
    }
}
