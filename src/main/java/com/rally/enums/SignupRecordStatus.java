package com.rally.enums;

/** 报名记录里显示的状态，和前端 types/user.ts 的 SignupRecordStatus 一致 */
public enum SignupRecordStatus {
    /** 已报名（还没开始） */
    JOINED,
    /** 候补中 */
    WAITLIST,
    /** 已出勤 */
    ATTENDED,
    /** 鸽子（报了名没来） */
    NO_SHOW,
    /** 自己取消了，或者这一场被组织者取消了 */
    CANCELLED
}
