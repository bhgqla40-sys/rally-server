package com.rally.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 固定局的时间计算（纯计算，不查数据库，方便写单元测试）。
 * 规则和前端 mock（api/template.ts 的 firstStart）一致。
 */
public final class GameSchedule {

    private GameSchedule() {
    }

    /**
     * 第一场的开局时间：最近一个「还来得及报名」的日子。
     * 比如今天就是周六，但已经过了截止时间，那第一场就排到下周六。
     *
     * @param weekday 每周几：1 = 周一 … 7 = 周日
     */
    public static LocalDateTime firstStart(int weekday, LocalTime startTime, int deadlineHoursBefore, LocalDateTime now) {
        LocalDateTime start = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday)))
                .atTime(startTime);
        return skipPastDeadline(start, deadlineHoursBefore, now);
    }

    /** 下一场的开局时间：上一场的一周后；如果那一场也已经过了截止时间，继续往后推 */
    public static LocalDateTime nextStart(LocalDateTime previousStart, int deadlineHoursBefore, LocalDateTime now) {
        return skipPastDeadline(previousStart.plusWeeks(1), deadlineHoursBefore, now);
    }

    /** 开放报名时间：开局那天往前推 N 天的某个时刻，例如「提前 3 天 20:00」 */
    public static LocalDateTime openAt(LocalDateTime start, int openDaysBefore, LocalTime openTime) {
        return start.toLocalDate().minusDays(openDaysBefore).atTime(openTime);
    }

    /** 截止报名时间：开局前 N 小时 */
    public static LocalDateTime deadlineAt(LocalDateTime start, int deadlineHoursBefore) {
        return start.minusHours(deadlineHoursBefore);
    }

    private static LocalDateTime skipPastDeadline(LocalDateTime start, int deadlineHoursBefore, LocalDateTime now) {
        while (!deadlineAt(start, deadlineHoursBefore).isAfter(now)) {
            start = start.plusWeeks(1);
        }
        return start;
    }
}
