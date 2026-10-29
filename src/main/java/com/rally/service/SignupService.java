package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.game.GameDetail;
import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.enums.GameStatus;
import com.rally.enums.SignupStatus;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameSignupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报名、取消报名、候补递补。
 *
 * 并发安全的两个关键点（面试常问）：
 * 1. 抢名额用「条件更新」：update ... set joined_count = joined_count + 1 where joined_count < capacity，
 *    判断和占位在同一条 SQL 里完成，不会超员（见 GameMapper.tryTakeSeat）。
 * 2. 取消、递补这种要改好几处的操作，先用 select ... for update 锁住这一场，同一场的修改排队执行，
 *    不会出现两个人同时取消、把同一个候补递补两次之类的问题（见 GameMapper.selectByIdForUpdate）。
 * 方法上都加了 @Transactional：中间任何一步失败，前面改过的人数会一起回滚。
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    private static final DateTimeFormatter OPEN_TIME = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final GameMapper gameMapper;
    private final GameSignupMapper signupMapper;
    private final GameService gameService;
    private final GroupService groupService;

    /** 报名：有名额直接报上，满员了进候补（允许候补的话） */
    @Transactional
    public GameDetail signup(Long userId, Long gameId) {
        Game game = gameService.getGameOrThrow(gameId);
        switch (GameService.statusOf(game, LocalDateTime.now())) {
            case UPCOMING -> throw badRequest("还没到报名时间，" + game.getOpenAt().format(OPEN_TIME) + " 开放报名");
            case CLOSED, FINISHED -> throw badRequest("报名已截止");
            case CANCELLED -> throw badRequest("这一场已经取消了");
            case OPEN -> {
                // 可以报名
            }
        }

        // 通过分享链接点进来的人可能还不在群里，报名时自动加入群组
        groupService.ensureMember(game.getGroupId(), userId);

        GameSignup existing = signupMapper.selectOne(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, gameId)
                .eq(GameSignup::getUserId, userId));
        if (existing != null && existing.getStatus() != SignupStatus.CANCELLED) {
            throw badRequest("你已经报过名了");
        }

        // 先抢名额；抢不到再看能不能排候补
        SignupStatus status;
        if (gameMapper.tryTakeSeat(gameId) == 1) {
            status = SignupStatus.JOINED;
        } else if (Boolean.TRUE.equals(game.getAllowWaitlist())) {
            gameMapper.adjustCounts(gameId, 0, 1);
            status = SignupStatus.WAITLIST;
        } else {
            throw badRequest("名额已满");
        }

        if (existing == null) {
            GameSignup signup = new GameSignup();
            signup.setGameId(gameId);
            signup.setUserId(userId);
            signup.setStatus(status);
            signup.setQueuedAt(LocalDateTime.now());
            signup.setNoShow(false);
            try {
                signupMapper.insert(signup);
            } catch (DuplicateKeyException e) {
                // 同一个人连点两次：唯一索引 (game_id, user_id) 拦住第二次，抛异常让上面加的人数回滚
                throw badRequest("你已经报过名了");
            }
        } else if (signupMapper.reactivate(existing.getId(), status) == 0) {
            throw badRequest("你已经报过名了");
        }

        return gameService.toDetail(gameMapper.selectById(gameId), userId);
    }

    /** 取消自己的报名 / 退出候补：截止前才能取消；空出名额时候补第一位自动递补 */
    @Transactional
    public GameDetail cancelMine(Long userId, Long gameId) {
        Game game = lockGame(gameId);
        GameStatus status = GameService.statusOf(game, LocalDateTime.now());
        if (status == GameStatus.CANCELLED) {
            throw badRequest("这一场已经取消了");
        }
        if (status != GameStatus.OPEN) {
            throw badRequest("已过截止时间，不能取消了");
        }

        GameSignup mine = findActiveSignup(gameId, userId);
        if (mine == null) {
            throw badRequest("你还没有报名");
        }
        cancelSignup(game, mine);
        return gameService.toDetail(gameMapper.selectById(gameId), userId);
    }

    /** 组织者把某人移出报名名单或候补：空出名额时候补第一位自动递补 */
    @Transactional
    public GameDetail removeByOrganizer(Long userId, Long gameId, Long memberId) {
        Game game = lockGame(gameId);
        requireOrganizer(game, userId);
        GameStatus status = GameService.statusOf(game, LocalDateTime.now());
        if (status == GameStatus.CANCELLED) {
            throw badRequest("这一场已经取消了");
        }
        if (status == GameStatus.FINISHED) {
            throw badRequest("这一场已经结束了");
        }

        GameSignup target = findActiveSignup(gameId, memberId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "这个人没有报名");
        }
        cancelSignup(game, target);
        return gameService.toDetail(gameMapper.selectById(gameId), userId);
    }

    /**
     * 成员被移出群组时调用：取消他在这个群里还没开始的场次中的报名和候补。
     * 调用方（移出成员）已经开了事务时，@Transactional 默认会加入那个事务，移出失败的话这里的取消也会一起回滚。
     */
    @Transactional
    public void cancelUpcomingSignupsInGroup(Long groupId, Long userId) {
        List<Long> gameIds = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                        .eq(Game::getGroupId, groupId)
                        .eq(Game::getCancelled, false)
                        .gt(Game::getStartAt, LocalDateTime.now()))
                .stream()
                .map(Game::getId)
                .toList();
        if (gameIds.isEmpty()) {
            return;
        }
        List<GameSignup> signups = signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getUserId, userId)
                .in(GameSignup::getGameId, gameIds)
                .in(GameSignup::getStatus, SignupStatus.JOINED, SignupStatus.WAITLIST));
        for (GameSignup signup : signups) {
            cancelSignup(lockGame(signup.getGameId()), signup);
        }
    }

    // ------------------------------------------------------------------

    /**
     * 取消一条报名，调整人数；如果空出了名额并且开了自动递补，让候补第一位补上。
     * 调用前必须已经用 lockGame 锁住这一场。
     */
    private void cancelSignup(Game game, GameSignup signup) {
        if (signupMapper.cancelActive(signup.getId()) == 0) {
            throw badRequest("这条报名已经取消过了");
        }
        if (signup.getStatus() == SignupStatus.JOINED) {
            gameMapper.adjustCounts(game.getId(), -1, 0);
            if (Boolean.TRUE.equals(game.getAllowWaitlist()) && Boolean.TRUE.equals(game.getAutoPromote())) {
                promoteFirstInWaitlist(game.getId());
            }
        } else {
            gameMapper.adjustCounts(game.getId(), 0, -1);
        }
    }

    /** 候补第一位（排队最早的）递补成已报名 */
    private void promoteFirstInWaitlist(Long gameId) {
        GameSignup first = signupMapper.selectOne(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, gameId)
                .eq(GameSignup::getStatus, SignupStatus.WAITLIST)
                .orderByAsc(GameSignup::getQueuedAt)
                .orderByAsc(GameSignup::getId)
                .last("limit 1"));
        if (first != null && signupMapper.promote(first.getId()) == 1) {
            gameMapper.adjustCounts(gameId, 1, -1);
            // TODO 以后加消息通知：告诉他「你已从候补递补为正式报名」
        }
    }

    /** 查询并锁住这一场（必须在事务里） */
    private Game lockGame(Long gameId) {
        Game game = gameMapper.selectByIdForUpdate(gameId);
        if (game == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "场次不存在");
        }
        return game;
    }

    private GameSignup findActiveSignup(Long gameId, Long userId) {
        return signupMapper.selectOne(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, gameId)
                .eq(GameSignup::getUserId, userId)
                .in(GameSignup::getStatus, SignupStatus.JOINED, SignupStatus.WAITLIST));
    }

    static void requireOrganizer(Game game, Long userId) {
        if (!game.getOrganizerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有组织者可以操作");
        }
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
