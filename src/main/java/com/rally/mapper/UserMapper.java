package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * Mapper：读写 user 表。
 *
 * 继承 BaseMapper 后，selectById、insert、updateById、selectOne 等常用方法都自动有了，不用写 SQL。
 * 复杂一点的查询，可以像下面这样用 @Select 直接写 SQL，#{xxx} 是参数占位符（会自动防 SQL 注入）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // ===== 下面是「我的」页面用到的统计数字 =====

    /** 加入了几个群 */
    @Select("SELECT COUNT(*) FROM group_member WHERE user_id = #{userId}")
    int countGroups(@Param("userId") Long userId);

    /** 累计出勤：报上名、场次没取消、并且已经打完了 */
    @Select("""
            SELECT COUNT(*)
            FROM game_signup s
            JOIN game g ON g.id = s.game_id
            WHERE s.user_id = #{userId}
              AND s.status = 'JOINED'
              AND g.cancelled = 0
              AND g.end_at <= NOW()
            """)
    int countTotalAttendance(@Param("userId") Long userId);

    /** 某个时间之后的出勤次数（用来算本月出勤） */
    @Select("""
            SELECT COUNT(*)
            FROM game_signup s
            JOIN game g ON g.id = s.game_id
            WHERE s.user_id = #{userId}
              AND s.status = 'JOINED'
              AND g.cancelled = 0
              AND g.end_at <= NOW()
              AND g.start_at >= #{since}
            """)
    int countAttendanceSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** 当「鸽子」的次数：截止后临时请假 */
    @Select("SELECT COUNT(*) FROM game_signup WHERE user_id = #{userId} AND no_show = 1")
    int countNoShow(@Param("userId") Long userId);

    /** 还没付的账单数（组织者自己垫付的不算） */
    @Select("""
            SELECT COUNT(*)
            FROM bill_member
            WHERE user_id = #{userId}
              AND paid = 0
              AND is_organizer = 0
              AND amount > 0
            """)
    int countPendingBills(@Param("userId") Long userId);
}
