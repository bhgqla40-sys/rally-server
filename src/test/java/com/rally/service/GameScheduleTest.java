package com.rally.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 固定局开局时间的计算。2026-10-29 是周四 */
class GameScheduleTest {

    private static final LocalTime SEVEN_PM = LocalTime.of(19, 0);
    private static final int SATURDAY = 6;
    private static final int THURSDAY = 4;

    @Test
    void firstStartIsThisWeekWhenStillInTime() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 10, 0); // 周四上午
        assertEquals(LocalDateTime.of(2026, 10, 31, 19, 0), GameSchedule.firstStart(SATURDAY, SEVEN_PM, 6, now));
    }

    @Test
    void firstStartCanBeToday() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 10, 0); // 周四 10 点，今晚 7 点开局，6 小时前截止 = 13 点
        assertEquals(LocalDateTime.of(2026, 10, 29, 19, 0), GameSchedule.firstStart(THURSDAY, SEVEN_PM, 6, now));
    }

    @Test
    void firstStartSkipsToNextWeekWhenTodaysDeadlinePassed() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 14, 0); // 已经过了今天 13 点的截止时间
        assertEquals(LocalDateTime.of(2026, 11, 5, 19, 0), GameSchedule.firstStart(THURSDAY, SEVEN_PM, 6, now));
    }

    @Test
    void nextStartIsOneWeekLater() {
        LocalDateTime previous = LocalDateTime.of(2026, 10, 31, 19, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 31, 19, 5); // 上一场刚开始
        assertEquals(LocalDateTime.of(2026, 11, 7, 19, 0), GameSchedule.nextStart(previous, 6, now));
    }

    @Test
    void nextStartSkipsWeeksThatAreAlreadyPast() {
        // 固定局暂停了一个月又恢复：从上一场往后推，跳过已经来不及的那几周
        LocalDateTime previous = LocalDateTime.of(2026, 10, 3, 19, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 10, 0);
        assertEquals(LocalDateTime.of(2026, 10, 31, 19, 0), GameSchedule.nextStart(previous, 6, now));
    }

    @Test
    void openAtIsDaysBeforeAtGivenTime() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 31, 19, 0);
        assertEquals(LocalDateTime.of(2026, 10, 28, 20, 0), GameSchedule.openAt(start, 3, LocalTime.of(20, 0)));
        assertEquals(LocalDateTime.of(2026, 10, 31, 13, 0), GameSchedule.deadlineAt(start, 6));
    }
}
