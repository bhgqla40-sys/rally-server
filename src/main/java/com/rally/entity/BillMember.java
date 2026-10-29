package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 账单里每个人的应付情况，对应 bill_member 表 */
@Data
@TableName("bill_member")
public class BillMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private Long userId;

    /** 应付金额（分），费用项变化时重新分摊 */
    private Integer amount;

    private Boolean paid;

    private LocalDateTime paidAt;

    /** 成员点「我已转账」的时间，等组织者确认 */
    private LocalDateTime claimedAt;

    /** 组织者：垫付了全部费用，自己不用转账 */
    private Boolean isOrganizer;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
