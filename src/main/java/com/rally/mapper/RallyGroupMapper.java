package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.RallyGroup;
import org.apache.ibatis.annotations.Mapper;

/** 读写群组 rally_group 表（常用的增删改查 BaseMapper 都提供了） */
@Mapper
public interface RallyGroupMapper extends BaseMapper<RallyGroup> {
}
