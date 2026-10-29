package com.rally.enums;

/** 我的报名记录分两个标签页，和前端 api/user.ts 的 SignupScope 一致 */
public enum SignupScope {
    /** 还没结束的：已报名、候补中 */
    UPCOMING,
    /** 已经结束的：已出勤、鸽子、已取消 */
    HISTORY
}
