package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 读写账单 bill 表 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 查询并锁住这个账单（悲观锁，用法和 GameMapper.selectByIdForUpdate 一样）。
     * 加减费用项要「改费用项 + 重新分摊每个人的金额」两步，锁住账单防止两个请求同时改、分摊算乱。
     */
    @Select("select * from bill where id = #{id} for update")
    Bill selectByIdForUpdate(@Param("id") Long id);
}
