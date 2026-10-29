package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.game.AttendanceRequest;
import com.rally.dto.game.CancelGameRequest;
import com.rally.dto.game.GameDetail;
import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.enums.GameStatus;
import com.rally.enums.SignupStatus;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameSignupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 场次管理（组织者）：取消这一场、结束后确认出勤。移出报名的人在 SignupService */
@Service
@RequiredArgsConstructor
public class GameManageService {

    private final GameMapper gameMapper;
    private final GameSignupMapper signupMapper;
    private final GameService gameService;
    private final BillService billService;

    /** 取消这一场：开始之前才能取消。报名记录保留，「我的报名记录」里会显示为已取消 */
    @Transactional
    public GameDetail cancelGame(Long userId, Long gameId, CancelGameRequest request) {
        Game game = lockGame(gameId);
        SignupService.requireOrganizer(game, userId);
        GameStatus status = GameService.statusOf(game, LocalDateTime.now());
        if (status == GameStatus.CANCELLED || status == GameStatus.FINISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "这一场已经结束或取消了");
        }
        if (!LocalDateTime.now().isBefore(game.getStartAt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已经开始了，不能取消");
        }

        Game update = new Game();
        update.setId(gameId);
        update.setCancelled(true);
        // @NotBlank 已经保证不是空的，这里去掉首尾空格再存
        update.setCancelReason(request.reason().trim());
        gameMapper.updateById(update);
        // TODO 以后加消息通知：告诉报了名的人这一场取消了
        return gameService.toDetail(gameMapper.selectById(gameId), userId);
    }

    /**
     * 结束后确认出勤：传入没来的人（鸽子）的 id，其余报名的人都算到场。
     * 可以重复确认（改错了再提交一次）。结算 AA 费用时不算鸽子；
     * 如果已经建了账单，账单成员跟着出勤名单一起改，重新分摊。
     */
    @Transactional
    public GameDetail confirmAttendance(Long userId, Long gameId, AttendanceRequest request) {
        Game game = lockGame(gameId);
        SignupService.requireOrganizer(game, userId);
        if (GameService.statusOf(game, LocalDateTime.now()) != GameStatus.FINISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "结束后才能确认出勤");
        }

        // 先把这一场所有报名的人都标记为到场，再把传进来的人标记为鸽子（不在报名名单里的 id 自动忽略）
        signupMapper.update(null, new LambdaUpdateWrapper<GameSignup>()
                .set(GameSignup::getNoShow, false)
                .eq(GameSignup::getGameId, gameId)
                .eq(GameSignup::getStatus, SignupStatus.JOINED));
        List<Long> noShowIds = request.noShowIds().stream().distinct().toList();
        if (!noShowIds.isEmpty()) {
            signupMapper.update(null, new LambdaUpdateWrapper<GameSignup>()
                    .set(GameSignup::getNoShow, true)
                    .eq(GameSignup::getGameId, gameId)
                    .eq(GameSignup::getStatus, SignupStatus.JOINED)
                    .in(GameSignup::getUserId, noShowIds));
        }

        Game update = new Game();
        update.setId(gameId);
        update.setAttendanceConfirmed(true);
        gameMapper.updateById(update);
        billService.syncMembers(game);
        return gameService.toDetail(gameMapper.selectById(gameId), userId);
    }

    private Game lockGame(Long gameId) {
        Game game = gameMapper.selectByIdForUpdate(gameId);
        if (game == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "场次不存在");
        }
        return game;
    }
}
