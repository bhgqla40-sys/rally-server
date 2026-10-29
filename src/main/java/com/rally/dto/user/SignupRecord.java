package com.rally.dto.user;

import com.rally.enums.SignupRecordStatus;

import java.time.LocalDateTime;

/** 我的报名记录，对应前端 types/user.ts 的 SignupRecord */
public record SignupRecord(
        Long gameId,
        String title,
        String groupName,
        String venue,
        LocalDateTime startAt,
        LocalDateTime endAt,
        SignupRecordStatus status,
        /* 候补第几位，status 为 WAITLIST 时才有 */
        Integer waitlistRank
) {
}
