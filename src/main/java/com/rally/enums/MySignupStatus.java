package com.rally.enums;

/** 「我」在某一场的报名状态，和前端 types/game.ts 的 MySignupStatus 一致 */
public enum MySignupStatus {
    /** 没报名（或者报了又取消了） */
    NONE,
    JOINED,
    WAITLIST
}
