package com.rally.service;

import com.rally.entity.GameTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 群组列表里「每周六」这段文字 */
class ScheduleTextTest {

    private GameTemplate template(int weekday, boolean enabled) {
        GameTemplate t = new GameTemplate();
        t.setWeekday(weekday);
        t.setEnabled(enabled);
        return t;
    }

    @Test
    void noTemplate() {
        assertEquals("还没有固定局", GroupService.scheduleText(List.of()));
    }

    @Test
    void oneTemplate() {
        assertEquals("每周六", GroupService.scheduleText(List.of(template(6, true))));
    }

    @Test
    void severalTemplatesSortedAndDeduplicated() {
        assertEquals("每周二、周六、周日", GroupService.scheduleText(List.of(
                template(7, true), template(2, true), template(6, true), template(6, true))));
    }

    @Test
    void pausedTemplatesAreSkipped() {
        assertEquals("每周二", GroupService.scheduleText(List.of(template(2, true), template(6, false))));
        assertEquals("固定局已暂停", GroupService.scheduleText(List.of(template(6, false))));
    }
}
