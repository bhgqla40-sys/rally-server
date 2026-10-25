package com.rally.dto.user;

/** 「我的」页面数据，对应前端 types/user.ts 的 MyProfile */
public record MyProfile(
        Long id,
        String nickname,
        String avatar,
        /* 个人介绍 */
        String bio,
        int groupCount,
        int monthAttendance,
        int totalAttendance,
        /* 截止后临时请假的次数 */
        int noShowCount,
        int pendingBillCount
) {
}
