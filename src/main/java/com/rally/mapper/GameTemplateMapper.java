package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.GameTemplate;
import org.apache.ibatis.annotations.Mapper;

/** 读写固定局规则 game_template 表（常用的增删改查 BaseMapper 都提供了） */
@Mapper
public interface GameTemplateMapper extends BaseMapper<GameTemplate> {
}
