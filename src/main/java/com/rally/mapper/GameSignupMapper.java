package com.rally.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.entity.GameSignup;
import com.rally.enums.SignupStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 读写报名记录 game_signup 表 */
@Mapper
public interface GameSignupMapper extends BaseMapper<GameSignup> {

    /**
     * 取消报名 / 退出候补。
     * 带上 status 条件：只有还在「已报名 / 候补中」才改得动，返回 0 说明已经被取消过了（比如连点了两次）。
     */
    @Update("""
            update game_signup
            set status = 'CANCELLED', cancelled_at = now()
            where id = #{id}
              and status in ('JOINED', 'WAITLIST')
            """)
    int cancelActive(@Param("id") Long id);

    /**
     * 取消后重新报名：复用原来那条记录（一个人在一场里只有一条），排队时间刷新成现在，排到最后。
     * 同样带 status 条件，防止同一个人连点两次报名被处理两遍。
     */
    @Update("""
            update game_signup
            set status = #{status}, queued_at = now(3), cancelled_at = null, promoted_at = null, no_show = 0
            where id = #{id}
              and status = 'CANCELLED'
            """)
    int reactivate(@Param("id") Long id, @Param("status") SignupStatus status);

    /** 候补递补成已报名 */
    @Update("""
            update game_signup
            set status = 'JOINED', promoted_at = now()
            where id = #{id}
              and status = 'WAITLIST'
            """)
    int promote(@Param("id") Long id);
}
