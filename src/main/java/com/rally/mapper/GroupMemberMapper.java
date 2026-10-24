package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.GroupMember;
import com.rally.mapper.result.GroupMemberCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 读写群组成员 group_member 表 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /**
     * 一次查出多个群各有多少人（GROUP BY 分组计数），避免每个群单独查一次。
     * <script> 里可以写 MyBatis 的动态 SQL：<foreach> 把 id 列表拼成 IN (1, 2, 3)。
     */
    @Select("""
            <script>
            SELECT group_id, COUNT(*) AS member_count
            FROM group_member
            WHERE group_id IN
            <foreach collection="groupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY group_id
            </script>
            """)
    List<GroupMemberCount> countByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
