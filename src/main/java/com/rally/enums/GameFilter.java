package com.rally.enums;

/** 首页场次筛选，和前端 api/game.ts 的 GameFilter 一致 */
public enum GameFilter {
    /** 全部（我所在群组里还没结束的场次） */
    ALL,
    /** 我报名的（含候补） */
    MINE,
    /** 可报名：报名中，而且我还没报 */
    AVAILABLE
}
