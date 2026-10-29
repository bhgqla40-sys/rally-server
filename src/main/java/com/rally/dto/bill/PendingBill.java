package com.rally.dto.bill;

import com.rally.dto.common.Member;

import java.time.LocalDateTime;

/** 我的待付款：我在某个账单里还没付的那一笔，对应前端 types/bill.ts 的 PendingBill */
public record PendingBill(
        Long billId,
        Long gameId,
        String gameTitle,
        LocalDateTime gameStartAt,
        String groupName,
        Member organizer,
        /* 我应付的金额（分） */
        Integer amount,
        LocalDateTime claimedAt
) {
}
