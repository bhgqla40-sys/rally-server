package com.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体类：和数据库 user 表一一对应，一个字段对应一列。
 * 驼峰命名（passwordHash）会自动对应下划线列名（password_hash）。
 *
 * 实体类只在后端内部使用，不直接返回给前端（里面有密码哈希这种不能外传的字段），
 * 返回给前端的数据放在 dto 包里单独定义。
 */
@Data
@TableName("`user`") // user 在 MySQL 里是关键字，加反引号更保险
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    /** BCrypt 加密后的密码 */
    private String passwordHash;

    /** 微信小程序 openid，以后做小程序登录时用 */
    private String wxOpenid;

    private String nickname;

    private String avatar;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
