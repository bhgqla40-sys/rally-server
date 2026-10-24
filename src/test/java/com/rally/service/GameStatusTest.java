package com.rally.service;

import com.rally.entity.Game;
import com.rally.enums.GameStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 场次状态完全由时间决定，这里把每个时间段都测一遍 */
class GameStatusTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 31, 19, 0);

    private Game game(boolean cancelled) {
        Game game = new Game();
        game.setOpenAt(START.minusDays(3));     // 提前 3 天开放
        game.setDeadlineAt(START.minusHours(2)); // 开局前 2 小时截止
        game.setStartAt(START);
        game.setEndAt(START.plusHours(2));
        game.setCancelled(cancelled);
        return game;
    }

    @Test
    void beforeOpenIsUpcoming() {
        assertEquals(GameStatus.UPCOMING, GameService.statusOf(game(false), START.minusDays(4)));
    }

    @Test
    void betweenOpenAndDeadlineIsOpen() {
        assertEquals(GameStatus.OPEN, GameService.statusOf(game(false), START.minusDays(3)));
        assertEquals(GameStatus.OPEN, GameService.statusOf(game(false), START.minusHours(3)));
    }

    @Test
    void afterDeadlineIsClosed() {
        assertEquals(GameStatus.CLOSED, GameService.statusOf(game(false), START.minusHours(2)));
        assertEquals(GameStatus.CLOSED, GameService.statusOf(game(false), START.plusHours(1)));
    }

    @Test
    void afterEndIsFinished() {
        assertEquals(GameStatus.FINISHED, GameService.statusOf(game(false), START.plusHours(2)));
    }

    @Test
    void cancelledWinsOverTime() {
        assertEquals(GameStatus.CANCELLED, GameService.statusOf(game(true), START.minusHours(3)));
    }
}
