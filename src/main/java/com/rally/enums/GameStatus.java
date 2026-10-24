package com.rally.enums;

/**
 * 场次状态，和前端 types/game.ts 的 GameStatus 一致。
 * 数据库里只存「是否已取消」，其余状态都按当前时间算出来，见 GameService.statusOf。
 */
public enum GameStatus {
    /** 还没开放报名 */
    UPCOMING,
    /** 报名中 */
    OPEN,
    /** 已截止（截止后到结束前） */
    CLOSED,
    /** 已结束 */
    FINISHED,
    /** 已取消 */
    CANCELLED
}
