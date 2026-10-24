package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.GameSignup;
import org.apache.ibatis.annotations.Mapper;

/** 读写报名记录 game_signup 表（常用的增删改查 BaseMapper 都提供了） */
@Mapper
public interface GameSignupMapper extends BaseMapper<GameSignup> {
}
