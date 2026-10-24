package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rally.enums.FeeMode;
import com.rally.enums.SportType;
import lombok.Data;

import java.time.LocalDateTime;

/** 场次，对应 game 表：每一次具体的球局 */
@Data
@TableName("game")
public class Game {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 由哪个固定局生成；单独发起的为空 */
    private Long templateId;

    private Long groupId;

    /** 组织者 user.id */
    private Long organizerId;

    private String title;

    private SportType sport;

    private String venue;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** 开放报名时间 */
    private LocalDateTime openAt;

    /** 截止报名时间 */
    private LocalDateTime deadlineAt;

    private Integer capacity;

    /** 已报名人数（冗余计数，报名时靠它防止超员） */
    private Integer joinedCount;

    /** 候补人数（冗余计数） */
    private Integer waitlistCount;

    private Boolean allowWaitlist;

    private Boolean autoPromote;

    private FeeMode feeMode;

    /** 预计每人费用（分） */
    private Integer feePerPerson;

    private Boolean cancelled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
