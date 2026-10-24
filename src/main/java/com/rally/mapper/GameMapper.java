package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.Game;
import org.apache.ibatis.annotations.Mapper;

/** 读写场次 game 表（常用的增删改查 BaseMapper 都提供了） */
@Mapper
public interface GameMapper extends BaseMapper<Game> {
}
