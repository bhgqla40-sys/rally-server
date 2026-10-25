package com.rally.dto.game;

import com.rally.entity.GameTemplate;
import com.rally.enums.SportType;

import java.time.format.DateTimeFormatter;

/** 固定局（群组管理页的列表用），对应前端 types/game.ts 的 GameTemplateBrief */
public record GameTemplateBrief(
        Long id,
        Long groupId,
        String name,
        SportType sport,
        String venue,
        /* 每周几：1 = 周一 … 7 = 周日 */
        int weekday,
        /* 开始时间 HH:mm */
        String startTime,
        int durationMinutes,
        int capacity,
        /* 暂停后不再每周自动开局（数据库里叫 enabled） */
        boolean active
) {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static GameTemplateBrief of(GameTemplate t) {
        return new GameTemplateBrief(t.getId(), t.getGroupId(), t.getName(), t.getSport(), t.getVenue(),
                t.getWeekday(), t.getStartTime().format(HH_MM), t.getDurationMinutes(), t.getCapacity(),
                Boolean.TRUE.equals(t.getEnabled()));
    }
}
