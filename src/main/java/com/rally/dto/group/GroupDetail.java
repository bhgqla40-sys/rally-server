package com.rally.dto.group;

import com.rally.dto.common.Member;
import com.rally.dto.game.GameBrief;
import com.rally.enums.GroupRole;
import com.rally.enums.SportType;

import java.util.List;

/**
 * 群组详情，对应前端 api/group.ts 的 GroupDetail（= Group 的字段 + 下面几个）。
 * record 不能继承，所以把 GroupInfo 的字段也写了一遍，JSON 结构和前端完全一致。
 */
public record GroupDetail(
        Long id,
        String name,
        SportType sport,
        int memberCount,
        GroupRole myRole,
        String scheduleText,
        String inviteCode,
        Member owner,
        /* 成员预览：群主、管理员排前面，最多 10 个 */
        List<Member> members,
        /* 接下来的场次（没结束、没取消），按开始时间正序 */
        List<GameBrief> upcomingGames,
        /* 最近结束的 3 场，按开始时间倒序（组织者在这里进入费用结算） */
        List<GameBrief> recentFinishedGames
) {
    public static GroupDetail of(GroupInfo info, Member owner, List<Member> members,
                                 List<GameBrief> upcomingGames, List<GameBrief> recentFinishedGames) {
        return new GroupDetail(info.id(), info.name(), info.sport(), info.memberCount(), info.myRole(),
                info.scheduleText(), info.inviteCode(), owner, members, upcomingGames, recentFinishedGames);
    }
}
