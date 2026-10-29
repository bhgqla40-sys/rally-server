package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 账单里的一项费用，对应 bill_item 表（场地费、用球……） */
@Data
@TableName("bill_item")
public class BillItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private String name;

    /** 金额（分） */
    private Integer amount;

    private LocalDateTime createdAt;
}
