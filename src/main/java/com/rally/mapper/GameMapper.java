package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 读写场次 game 表 */
@Mapper
public interface GameMapper extends BaseMapper<Game> {

    /**
     * 查询并锁住这一场（悲观锁）。
     * for update 会给这一行加锁，直到当前事务结束；同一场的其他修改要排队等着，
     * 取消报名、候补递补这种要改好几处数据的操作，用它保证不会被别的请求插进来改乱。
     * 必须在 @Transactional 方法里调用，否则查完锁就释放了。
     */
    @Select("select * from game where id = #{id} for update")
    Game selectByIdForUpdate(@Param("id") Long id);

    /**
     * 抢一个名额（乐观的条件更新）：还有空位才 +1，返回 1 表示抢到了，0 表示满了。
     * 「判断有没有空位」和「占位」在同一条 SQL 里完成，数据库保证不会两个人同时抢到最后一个位置，
     * 不会出现先查出还有 1 个位置、两个人都以为自己能报上的超员问题。
     */
    @Update("""
            update game
            set joined_count = joined_count + 1
            where id = #{id}
              and cancelled = 0
              and joined_count < capacity
            """)
    int tryTakeSeat(@Param("id") Long id);

    /** 调整已报名 / 候补人数，delta 可以是负数 */
    @Update("""
            update game
            set joined_count   = joined_count + #{joinedDelta},
                waitlist_count = waitlist_count + #{waitlistDelta}
            where id = #{id}
            """)
    int adjustCounts(@Param("id") Long id,
                     @Param("joinedDelta") int joinedDelta,
                     @Param("waitlistDelta") int waitlistDelta);
}
