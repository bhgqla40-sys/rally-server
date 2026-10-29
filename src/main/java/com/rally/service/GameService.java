package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.common.Member;
import com.rally.dto.game.GameBrief;
import com.rally.dto.game.GameDetail;
import com.rally.dto.user.SignupRecord;
import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.entity.GroupMember;
import com.rally.entity.RallyGroup;
import com.rally.entity.User;
import com.rally.enums.GameFilter;
import com.rally.enums.GameStatus;
import com.rally.enums.MySignupStatus;
import com.rally.enums.SignupRecordStatus;
import com.rally.enums.SignupScope;
import com.rally.enums.SignupStatus;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameSignupMapper;
import com.rally.mapper.GroupMemberMapper;
import com.rally.mapper.RallyGroupMapper;
import com.rally.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 场次的查询：首页列表、详情、我的报名记录，以及把场次转成前端要的 GameBrief / GameDetail。
 * 报名、取消这些「改数据」的操作在 SignupService。
 */
@Service
@RequiredArgsConstructor
public class GameService {

    /** 我的报名记录里「已结束」那一页最多显示多少条 */
    private static final int HISTORY_LIMIT = 100;

    private final GameMapper gameMapper;
    private final GameSignupMapper signupMapper;
    private final RallyGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final UserMapper userMapper;

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

    /** 查场次，不存在就报 404 */
    public Game getGameOrThrow(Long gameId) {
        Game game = gameMapper.selectById(gameId);
        if (game == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "场次不存在");
        }
        return game;
    }

    /** 首页列表：我所在群组里还没结束、没取消的场次，按开始时间排序 */
    public List<GameBrief> list(Long userId, GameFilter filter) {
        List<Long> groupIds = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getUserId, userId))
                .stream()
                .map(GroupMember::getGroupId)
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }

        List<Game> games = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                .in(Game::getGroupId, groupIds)
                .eq(Game::getCancelled, false)
                .gt(Game::getEndAt, LocalDateTime.now())
                .orderByAsc(Game::getStartAt)
                .orderByAsc(Game::getId));

        return toBriefs(games, userId).stream()
                .filter(g -> switch (filter) {
                    case ALL -> true;
                    case MINE -> g.mySignup() != MySignupStatus.NONE;
                    case AVAILABLE -> g.status() == GameStatus.OPEN && g.mySignup() == MySignupStatus.NONE;
                })
                .toList();
    }

    /**
     * 场次详情。登录了就能看（不要求是群成员），这样分享到微信群的链接点开就能看到。
     */
    public GameDetail detail(Long userId, Long gameId) {
        return toDetail(getGameOrThrow(gameId), userId);
    }

    /** 我的报名记录：UPCOMING 还没结束的（按时间正序），HISTORY 已经结束的（按时间倒序） */
    public List<SignupRecord> mySignups(Long userId, SignupScope scope) {
        List<GameSignup> signups = signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getUserId, userId));
        if (signups.isEmpty()) {
            return List.of();
        }
        Map<Long, Game> games = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                        .in(Game::getId, signups.stream().map(GameSignup::getGameId).toList()))
                .stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        Map<Long, String> groupNames = groupNames(games.values().stream().map(Game::getGroupId).toList());

        LocalDateTime now = LocalDateTime.now();
        List<SignupRecord> records = new ArrayList<>();
        for (GameSignup signup : signups) {
            Game game = games.get(signup.getGameId());
            if (game == null) {
                continue;
            }
            SignupRecordStatus status = recordStatus(game, signup, now);
            if (status == null) {
                continue;
            }
            boolean upcoming = status == SignupRecordStatus.JOINED || status == SignupRecordStatus.WAITLIST;
            if (upcoming != (scope == SignupScope.UPCOMING)) {
                continue;
            }
            Integer rank = status == SignupRecordStatus.WAITLIST ? waitlistRank(signup) : null;
            records.add(new SignupRecord(game.getId(), game.getTitle(), groupNames.get(game.getGroupId()),
                    game.getVenue(), game.getStartAt(), game.getEndAt(), status, rank));
        }

        Comparator<SignupRecord> byStart = Comparator.comparing(SignupRecord::startAt);
        return scope == SignupScope.UPCOMING
                ? records.stream().sorted(byStart).toList()
                : records.stream().sorted(byStart.reversed()).limit(HISTORY_LIMIT).toList();
    }

    /**
     * 场次状态 + 我的报名状态 → 报名记录里显示的状态。
     * 返回 null 表示这条不显示（候补到结束都没轮上）。
     */
    static SignupRecordStatus recordStatus(Game game, GameSignup signup, LocalDateTime now) {
        if (Boolean.TRUE.equals(game.getCancelled()) || signup.getStatus() == SignupStatus.CANCELLED) {
            return SignupRecordStatus.CANCELLED;
        }
        if (!now.isBefore(game.getEndAt())) {
            if (signup.getStatus() == SignupStatus.JOINED) {
                return Boolean.TRUE.equals(signup.getNoShow()) ? SignupRecordStatus.NO_SHOW : SignupRecordStatus.ATTENDED;
            }
            return null;
        }
        return signup.getStatus() == SignupStatus.JOINED ? SignupRecordStatus.JOINED : SignupRecordStatus.WAITLIST;
    }

    /**
     * 把一批场次转成 GameBrief，并带上「我」的报名状态。
     * 群名、我的报名记录都是一次查出整批，不在循环里逐个查数据库。
     */
    public List<GameBrief> toBriefs(List<Game> games, Long userId) {
        if (games.isEmpty()) {
            return List.of();
        }
        Map<Long, String> groupNames = groupNames(games.stream().map(Game::getGroupId).toList());
        Map<Long, GameSignup> mySignups = signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                        .eq(GameSignup::getUserId, userId)
                        .in(GameSignup::getGameId, games.stream().map(Game::getId).toList())
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

    /** 场次详情：GameBrief + 组织者、名单、候补、群规等 */
    public GameDetail toDetail(Game game, Long userId) {
        GameBrief brief = toBriefs(List.of(game), userId).get(0);

        // 报名和候补的人一次查出来，按排队先后排好
        List<GameSignup> active = signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, game.getId())
                .in(GameSignup::getStatus, SignupStatus.JOINED, SignupStatus.WAITLIST)
                .orderByAsc(GameSignup::getQueuedAt)
                .orderByAsc(GameSignup::getId));
        Set<Long> userIds = new HashSet<>();
        userIds.add(game.getOrganizerId());
        active.forEach(s -> userIds.add(s.getUserId()));
        Map<Long, User> users = usersByIds(userIds);

        List<Member> joined = membersOf(active, SignupStatus.JOINED, users);
        List<Member> waitlist = membersOf(active, SignupStatus.WAITLIST, users);
        List<Long> noShowIds = Boolean.TRUE.equals(game.getAttendanceConfirmed())
                ? active.stream()
                .filter(s -> s.getStatus() == SignupStatus.JOINED && Boolean.TRUE.equals(s.getNoShow()))
                .map(GameSignup::getUserId)
                .toList()
                : null;
        User organizer = users.get(game.getOrganizerId());

        return GameDetail.of(
                brief,
                organizer == null ? null : Member.of(organizer),
                game.getFeeMode(),
                game.getFeePerPerson(),
                Boolean.TRUE.equals(game.getAllowWaitlist()),
                Boolean.TRUE.equals(game.getAutoPromote()),
                joined,
                waitlist,
                rulesOf(game),
                game.getCancelReason(),
                Boolean.TRUE.equals(game.getAttendanceConfirmed()),
                noShowIds);
    }

    /** 群规：根据这一场的设置生成，每条一句话 */
    static List<String> rulesOf(Game game) {
        List<String> rules = new ArrayList<>();
        long hours = Duration.between(game.getDeadlineAt(), game.getStartAt()).toHours();
        rules.add(hours > 0 ? "开局前 " + hours + " 小时停止报名和取消" : "开局后停止报名和取消");
        rules.add("截止后临时请假，会记一次「鸽子」");
        if (!Boolean.TRUE.equals(game.getAllowWaitlist())) {
            rules.add("满员后不能排候补");
        } else if (Boolean.TRUE.equals(game.getAutoPromote())) {
            rules.add("满员后可以排候补，有人取消时按排队顺序自动递补");
        }
        return rules;
    }

    /** 我在候补里排第几：排在我前面的人数 + 1（排队时间相同按 id 先后） */
    int waitlistRank(GameSignup mine) {
        long ahead = signupMapper.selectCount(new LambdaQueryWrapper<GameSignup>()
                .eq(GameSignup::getGameId, mine.getGameId())
                .eq(GameSignup::getStatus, SignupStatus.WAITLIST)
                .and(w -> w.lt(GameSignup::getQueuedAt, mine.getQueuedAt())
                        .or(o -> o.eq(GameSignup::getQueuedAt, mine.getQueuedAt()).lt(GameSignup::getId, mine.getId()))));
        return (int) ahead + 1;
    }

    // ------------------------------------------------------------------

    private List<Member> membersOf(List<GameSignup> signups, SignupStatus status, Map<Long, User> users) {
        return signups.stream()
                .filter(s -> s.getStatus() == status)
                .map(s -> users.get(s.getUserId()))
                .filter(Objects::nonNull)
                .map(Member::of)
                .toList();
    }

    private Map<Long, String> groupNames(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return groupMapper.selectList(new LambdaQueryWrapper<RallyGroup>().in(RallyGroup::getId, groupIds))
                .stream()
                .collect(Collectors.toMap(RallyGroup::getId, RallyGroup::getName));
    }

    private Map<Long, User> usersByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
