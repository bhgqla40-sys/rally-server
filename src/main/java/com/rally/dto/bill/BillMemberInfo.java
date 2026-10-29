package com.rally.dto.bill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rally.dto.common.Member;

import java.time.LocalDateTime;

/**
 * 账单里每个人的应付情况，对应前端 types/bill.ts 的 BillMember。
 * isOrganizer 加了 @JsonProperty：boolean 字段以 is 开头时，Jackson 有时会把 is 去掉变成 organizer，这里写死名字。
 */
public record BillMemberInfo(
        Member member,
        /* 应付金额（分） */
        Integer amount,
        Boolean paid,
        LocalDateTime paidAt,
        @JsonProperty("isOrganizer")
        Boolean isOrganizer,
        LocalDateTime claimedAt
) {
}
