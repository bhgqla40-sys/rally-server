package com.rally.dto.group;

import com.rally.dto.common.Member;
import com.rally.enums.GroupRole;

import java.time.LocalDateTime;

/**
 * 群成员（带角色和入群时间），对应前端 types/group.ts 的 GroupMember。
 * （后端的 GroupMember 已经是实体类的名字了，这里叫 GroupMemberInfo，JSON 字段一样）
 */
public record GroupMemberInfo(
        Member member,
        GroupRole role,
        LocalDateTime joinedAt
) {
}
