package com.rally.dto.common;

import com.rally.entity.User;

/** 用户的精简信息（列表、名单里展示用），对应前端 types/common.ts 的 Member */
public record Member(
        Long id,
        String nickname,
        String avatar
) {
    public static Member of(User user) {
        return new Member(user.getId(), user.getNickname(), user.getAvatar());
    }
}
