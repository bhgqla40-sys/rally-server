package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.dto.game.GameBrief;
import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.entity.RallyGroup;
import com.rally.enums.GameStatus;
import com.rally.enums.MySignupStatus;
import com.rally.enums.SignupStatus;
import com.rally.mapper.GameSignupMapper;
import com.rally.mapper.RallyGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 场次相关的业务逻辑。
 * 目前只有「把场次转成前端要的 GameBrief」，群组详情、首页列表、我的报名都会用到；
 * 报名、取消、候补递补等之后做场次模块时加在这里。
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameSignupMapper signupMapper;
    private final RallyGroupMapper groupMapper;

    /**
     * 场次状态由时间算出来（数据库只存「是否取消」），这样永远和当前时间对得上：
     * 开放报名前 UPCOMING → 报名中 OPEN → 截止后 CLOSED → 结束后 FINISHED
     */
    public static GameStatus statusOf(Game game, LocalDateTime now) {
        if (Boolean.TRUE.equals(game.getCancelled())) {
            return GameStatus.CANCELLED;
        }
        if (!now.isBefore(game.getEndAt())) {
            return GameStatus.FINISHED;
        }
        if (!now.isBefore(game.getDeadlineAt())) {
            return GameStatus.CLOSED;
        }
        if (!now.isBefore(game.getOpenAt())) {
            return GameStatus.OPEN;
        }
        return GameStatus.UPCOMING;
    }

    /**
     * 把一批场次转成 GameBrief，并带上「我」的报名状态。
     * 群名、我的报名记录都是一次查出整批，不在循环里逐个查数据库。
     */
    public List<GameBrief> toBriefs(List<Game> games, Long userId) {
        if (games.isEmpty()) {
            return List.of();
        }
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toSet());
        Set<Long> groupIds = games.stream().map(Game::getGroupId).collect(Collectors.toSet());

        Map<Long, String> groupNames = groupMapper.selectList(new LambdaQueryWrapper<RallyGroup>()
                        .in(RallyGroup::getId, groupIds))
                .stream()
                .collect(Collectors.toMap(RallyGroup::getId, RallyGroup::getName));

        Map<Long, GameSignup> mySignups = signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                        .eq(GameSignup::getUserId, userId)
                        .in(GameSignup::getGameId, gameIds)
                        .in(GameSignup::getStatus, SignupStatus.JOINED, SignupStatus.WAITLIST))
                .stream()
                .collect(Collectors.toMap(GameSignup::getGameId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        return games.stream().map(game -> {
            GameSignup mine = mySignups.get(game.getId());
            MySignupStatus mySignup = mine == null ? MySignupStatus.NONE
                    : mine.getStatus() == SignupStatus.JOINED ? MySignupStatus.JOINED
                    : MySignupStatus.WAITLIST;
            Integer waitlistRank = mySignup == MySignupStatus.WAITLIST ? waitlistRank(mine) : null;

            return new GameBrief(
                    game.getId(),
                    game.getTitle(),
                    game.getGroupId(),
                    groupNames.get(game.getGroupId()),
                    game.getSport(),
                    game.getVenue(),
                    game.getStartAt(),
                    game.getEndAt(),
                    game.getOpenAt(),
                    game.getDeadlineAt(),
                    game.getCapacity(),
                    game.getJoinedCount(),
                    game.getWaitlistCount(),
                    statusOf(game, now),
                    mySignup,
                    waitlistRank
            );
        }).toList();
    }

    /** 我在候补里排第几：排在我前面的人数 + 1 */
    private int waitlistRank(GameSignup mine) {
        long ahead = signupMapper.selectCount(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, mine.getGameId())
                .eq(GameSignup::getStatus, SignupStatus.WAITLIST)
                .lt(GameSignup::getQueuedAt, mine.getQueuedAt()));
        return (int) ahead + 1;
    }
}
