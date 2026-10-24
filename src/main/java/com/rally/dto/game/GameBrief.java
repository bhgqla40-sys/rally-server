package com.rally.dto.game;

import com.rally.enums.GameStatus;
import com.rally.enums.MySignupStatus;
import com.rally.enums.SportType;

import java.time.LocalDateTime;

/**
 * 场次（列表页用），对应前端 types/game.ts 的 GameBrief。
 * LocalDateTime 会被转成 2026-10-31T19:00:00+08:00 这种格式，见 JacksonConfig。
 */
public record GameBrief(
        Long id,
        String title,
        Long groupId,
        String groupName,
        SportType sport,
        String venue,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime openAt,
        LocalDateTime deadlineAt,
        int capacity,
        int joinedCount,
        int waitlistCount,
        GameStatus status,
        MySignupStatus mySignup,
        /* 我在候补里排第几位，mySignup 为 WAITLIST 时才有（其余为 null，JSON 里不输出） */
        Integer myWaitlistRank
) {
}
