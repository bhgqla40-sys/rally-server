package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rally.enums.SignupStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 报名记录，对应 game_signup 表（已报名和候补都在这张表，用 status 区分） */
@Data
@TableName("game_signup")
public class GameSignup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gameId;

    private Long userId;

    private SignupStatus status;

    /** 排队时间（毫秒精度），名单和候补都按它排序 */
    private LocalDateTime queuedAt;

    private LocalDateTime promotedAt;

    private LocalDateTime cancelledAt;

    /** 截止后临时请假（鸽子） */
    private Boolean noShow;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
