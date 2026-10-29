package com.rally.dto.bill;

import com.rally.dto.common.Member;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一场的费用账单，对应前端 types/bill.ts 的 Bill。
 * organizer 是新加的字段：组织者自己没报名（不在到场成员里）时，前端也能靠它判断「我是不是组织者」。
 */
public record BillInfo(
        Long id,
        Long gameId,
        String gameTitle,
        LocalDateTime gameStartAt,
        String groupName,
        Member organizer,
        List<BillItemInfo> items,
        List<BillMemberInfo> members
) {
}
