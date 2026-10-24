package com.rally.dto.group;

import com.rally.enums.GroupRole;
import com.rally.enums.SportType;

/**
 * 群组，对应前端 types/group.ts 的 Group。
 * （Java 里 Group 这个名字太常见，后端叫 GroupInfo，JSON 字段完全一样）
 */
public record GroupInfo(
        Long id,
        String name,
        SportType sport,
        int memberCount,
        GroupRole myRole,
        /* 例如「每周六」，由固定局规则生成 */
        String scheduleText,
        String inviteCode
) {
}
