package com.rally.dto.group;

import com.rally.enums.GroupRole;
import jakarta.validation.constraints.NotNull;

/** 设置成员角色：{ role: 'ADMIN' | 'MEMBER' }（不能设成 OWNER，Service 里会检查） */
public record SetMemberRoleRequest(
        @NotNull(message = "请选择角色")
        GroupRole role
) {
}
