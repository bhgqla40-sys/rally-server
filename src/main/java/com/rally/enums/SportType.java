package com.rally.enums;

/**
 * 运动类型，和前端 types/common.ts 的 SportType 一致。
 * 枚举 ≈ TS 的联合类型 'BADMINTON' | 'BASKETBALL' | ...，
 * 数据库里存的是名字字符串（'BADMINTON'），JSON 里也是。
 */
public enum SportType {
    BADMINTON,
    BASKETBALL,
    FOOTBALL,
    FRISBEE,
    OTHER
}
