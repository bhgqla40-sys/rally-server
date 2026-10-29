package com.rally.dto.game;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 组织者确认出勤：{ noShowIds }，传没来的人（鸽子）的用户 id，全到了就传空数组 */
public record AttendanceRequest(
        @NotNull(message = "请传入没来的人的名单（全到了传空数组）")
        List<Long> noShowIds
) {
}
