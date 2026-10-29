package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.BillItem;
import org.apache.ibatis.annotations.Mapper;

/** 读写账单费用项 bill_item 表 */
@Mapper
public interface BillItemMapper extends BaseMapper<BillItem> {
}
