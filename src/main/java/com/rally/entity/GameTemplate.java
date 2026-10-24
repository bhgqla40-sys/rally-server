package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rally.enums.FeeMode;
import com.rally.enums.SportType;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** 固定局规则，对应 game_template 表：每周按这个规则自动生成一场 */
@Data
@TableName("game_template")
public class GameTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    /** 创建人，也是生成出来的场次的组织者 */
    private Long creatorId;

    private String name;

    private SportType sport;

    private String venue;

    /** 每周几：1 = 周一 … 7 = 周日 */
    private Integer weekday;

    private LocalTime startTime;

    private Integer durationMinutes;

    private Integer capacity;

    private Integer openDaysBefore;

    private LocalTime openTime;

    private Integer deadlineHoursBefore;

    private Boolean allowWaitlist;

    private Boolean autoPromote;

    private FeeMode feeMode;

    /** 固定收费时每人金额（分） */
    private Integer fixedFee;

    /** 是否启用（前端叫 active），停用后不再自动生成场次 */
    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
