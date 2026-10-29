package com.rally.dto.game;

import com.rally.dto.common.Member;
import com.rally.enums.FeeMode;
import com.rally.enums.GameStatus;
import com.rally.enums.MySignupStatus;
import com.rally.enums.SportType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场次详情，对应前端 types/game.ts 的 GameDetail（= GameBrief 的字段 + 下面这些）。
 * record 不能继承，所以把 GameBrief 的字段也写了一遍，JSON 结构和前端完全一致。
 */
public record GameDetail(
        // ----- GameBrief 的字段 -----
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
        Integer myWaitlistRank,
        // ----- 详情页多出来的字段 -----
        Member organizer,
        FeeMode feeMode,
        /* 每人费用（分）：固定收费时是确定值；AA 时为空，结束后在账单里算 */
        Integer feePerPerson,
        boolean allowWaitlist,
        boolean autoPromote,
        /* 报名名单，按报名先后 */
        List<Member> joined,
        /* 候补名单，按排队先后 */
        List<Member> waitlist,
        /* 群规，每条一句话 */
        List<String> rules,
        String cancelReason,
        boolean attendanceConfirmed,
        /* 组织者确认的鸽子名单（确认出勤后才有） */
        List<Long> noShowIds
) {
    public static GameDetail of(GameBrief b, Member organizer, FeeMode feeMode, Integer feePerPerson,
                                boolean allowWaitlist, boolean autoPromote, List<Member> joined, List<Member> waitlist,
                                List<String> rules, String cancelReason, boolean attendanceConfirmed, List<Long> noShowIds) {
        return new GameDetail(b.id(), b.title(), b.groupId(), b.groupName(), b.sport(), b.venue(),
                b.startAt(), b.endAt(), b.openAt(), b.deadlineAt(), b.capacity(), b.joinedCount(), b.waitlistCount(),
                b.status(), b.mySignup(), b.myWaitlistRank(),
                organizer, feeMode, feePerPerson, allowWaitlist, autoPromote, joined, waitlist, rules,
                cancelReason, attendanceConfirmed, noShowIds);
    }
}
