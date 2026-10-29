package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.BillMember;
import org.apache.ibatis.annotations.Mapper;

/** 读写账单成员 bill_member 表 */
@Mapper
public interface BillMemberMapper extends BaseMapper<BillMember> {
}
