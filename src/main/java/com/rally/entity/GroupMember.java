package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rally.enums.GroupRole;
import lombok.Data;

import java.time.LocalDateTime;

/** 群组成员，对应 group_member 表：谁在哪个群、是什么角色 */
@Data
@TableName("group_member")
public class GroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    private GroupRole role;

    private LocalDateTime joinedAt;
}
