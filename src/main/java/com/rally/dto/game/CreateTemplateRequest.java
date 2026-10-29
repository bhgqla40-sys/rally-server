package com.rally.dto.game;

import com.rally.enums.FeeMode;
import com.rally.enums.SportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新建固定局，对应前端 types/game.ts 的 GameTemplateForm。
 * 取值范围比前端页面上的选项放宽一点，留出以后改页面的余地。
 */
public record CreateTemplateRequest(
        @NotNull(message = "请选择群组")
        Long groupId,

        @NotBlank(message = "请填写名称")
        @Size(max = 20, message = "名称最多 20 个字")
        String name,

        @NotNull(message = "请选择运动类型")
        SportType sport,

        @NotBlank(message = "请填写地点")
        @Size(max = 30, message = "地点最多 30 个字")
        String venue,

        /* 每周几：1 = 周一 … 7 = 周日 */
        @NotNull(message = "请选择每周几")
        @Min(value = 1, message = "每周几只能是 1 到 7")
        @Max(value = 7, message = "每周几只能是 1 到 7")
        Integer weekday,

        @NotNull(message = "请选择开始时间")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式应为 HH:mm")
        String startTime,

        @NotNull(message = "请选择时长")
        @Min(value = 30, message = "时长至少 30 分钟")
        @Max(value = 600, message = "时长最多 10 小时")
        Integer durationMinutes,

        @NotNull(message = "请填写人数上限")
        @Min(value = 2, message = "人数上限至少 2 人")
        @Max(value = 100, message = "人数上限最多 100 人")
        Integer capacity,

        /* 提前几天开放报名 */
        @NotNull(message = "请选择开放报名时间")
        @Min(value = 1, message = "至少提前 1 天开放报名")
        @Max(value = 14, message = "最多提前 14 天开放报名")
        Integer openDaysBefore,

        /* 开放报名的时刻 HH:mm */
        @NotNull(message = "请选择开放报名时间")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开放时间格式应为 HH:mm")
        String openTime,

        /* 开局前几小时截止报名 */
        @NotNull(message = "请选择截止报名时间")
        @Min(value = 0, message = "截止时间不能晚于开局")
        @Max(value = 72, message = "最多开局前 72 小时截止")
        Integer deadlineHoursBefore,

        @NotNull(message = "请选择是否允许候补")
        Boolean allowWaitlist,

        @NotNull(message = "请选择是否自动递补")
        Boolean autoPromote,

        @NotNull(message = "请选择收费方式")
        FeeMode feeMode,

        /* 固定收费时每人金额（分），AA 时不用传 */
        @Min(value = 1, message = "每人金额要大于 0")
        @Max(value = 1_000_000, message = "每人金额太大了")
        Integer fixedFee
) {
}
