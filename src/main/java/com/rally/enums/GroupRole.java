package com.rally.enums;

/** 群组里的角色，和前端 types/group.ts 的 GroupRole 一致 */
public enum GroupRole {
    /** 群主 */
    OWNER,
    /** 管理员 */
    ADMIN,
    /** 普通成员 */
    MEMBER;

    /** 群主和管理员都能管理群 */
    public boolean canManage() {
        return this == OWNER || this == ADMIN;
    }
}
