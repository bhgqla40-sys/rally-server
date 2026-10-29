package com.rally.service;

import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.enums.SignupRecordStatus;
import com.rally.enums.SignupStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 「我的报名记录」里每条显示什么状态，以及详情页的群规 */
class SignupRecordStatusTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 31, 19, 0);
    private static final LocalDateTime BEFORE = START.minusDays(1);
    private static final LocalDateTime AFTER_END = START.plusHours(3);

    private Game game(boolean cancelled) {
        Game game = new Game();
        game.setStartAt(START);
        game.setEndAt(START.plusHours(2));
        game.setDeadlineAt(START.minusHours(6));
        game.setCancelled(cancelled);
        game.setAllowWaitlist(true);
        game.setAutoPromote(true);
        return game;
    }

    private GameSignup signup(SignupStatus status, boolean noShow) {
        GameSignup s = new GameSignup();
        s.setStatus(status);
        s.setNoShow(noShow);
        return s;
    }

    @Test
    void beforeEnd() {
        assertEquals(SignupRecordStatus.JOINED, GameService.recordStatus(game(false), signup(SignupStatus.JOINED, false), BEFORE));
        assertEquals(SignupRecordStatus.WAITLIST, GameService.recordStatus(game(false), signup(SignupStatus.WAITLIST, false), BEFORE));
        assertEquals(SignupRecordStatus.CANCELLED, GameService.recordStatus(game(false), signup(SignupStatus.CANCELLED, false), BEFORE));
    }

    @Test
    void afterEnd() {
        assertEquals(SignupRecordStatus.ATTENDED, GameService.recordStatus(game(false), signup(SignupStatus.JOINED, false), AFTER_END));
        assertEquals(SignupRecordStatus.NO_SHOW, GameService.recordStatus(game(false), signup(SignupStatus.JOINED, true), AFTER_END));
        // 候补到结束都没轮上：不算出勤，也不显示
        assertNull(GameService.recordStatus(game(false), signup(SignupStatus.WAITLIST, false), AFTER_END));
    }

    @Test
    void cancelledGameShowsCancelled() {
        assertEquals(SignupRecordStatus.CANCELLED, GameService.recordStatus(game(true), signup(SignupStatus.JOINED, false), BEFORE));
        assertEquals(SignupRecordStatus.CANCELLED, GameService.recordStatus(game(true), signup(SignupStatus.JOINED, false), AFTER_END));
    }

    @Test
    void rules() {
        assertEquals(List.of("开局前 6 小时停止报名和取消", "截止后临时请假，会记一次「鸽子」", "满员后可以排候补，有人取消时按排队顺序自动递补"),
                GameService.rulesOf(game(false)));
        Game noWaitlist = game(false);
        noWaitlist.setAllowWaitlist(false);
        assertEquals("满员后不能排候补", GameService.rulesOf(noWaitlist).get(2));
    }
}
