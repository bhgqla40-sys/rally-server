package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rally.enums.SportType;
import lombok.Data;

import java.time.LocalDateTime;

/** 群组，对应 rally_group 表（group 是 MySQL 关键字，所以表名和类名都加了 Rally 前缀） */
@Data
@TableName("rally_group")
public class RallyGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 枚举字段：数据库里存的是名字，例如 'BADMINTON' */
    private SportType sport;

    /** 群主 user.id */
    private Long ownerId;

    /** 6 位邀请码 */
    private String inviteCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
