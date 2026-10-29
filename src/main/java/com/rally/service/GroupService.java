package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.common.Member;
import com.rally.dto.group.CreateGroupRequest;
import com.rally.dto.group.GroupDetail;
import com.rally.dto.group.GroupInfo;
import com.rally.dto.group.JoinGroupRequest;
import com.rally.entity.Game;
import com.rally.entity.GameTemplate;
import com.rally.entity.GroupMember;
import com.rally.entity.RallyGroup;
import com.rally.entity.User;
import com.rally.enums.GroupRole;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameTemplateMapper;
import com.rally.mapper.GroupMemberMapper;
import com.rally.mapper.RallyGroupMapper;
import com.rally.mapper.UserMapper;
import com.rally.mapper.result.GroupMemberCount;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    /** 邀请码字符：去掉了容易看错的 0/O、1/I */
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    /** 群组详情里成员预览的人数 */
    private static final int MEMBER_PREVIEW_SIZE = 10;

    /** 最近结束的场次显示几场 */
    private static final int RECENT_FINISHED_SIZE = 3;

    private static final String[] WEEKDAY_TEXT = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /** 成员排序：群主 → 管理员 → 普通成员，同角色按入群先后 */
    public static final Comparator<GroupMember> MEMBER_ORDER = Comparator
            .comparing((GroupMember m) -> m.getRole().ordinal())
            .thenComparing(GroupMember::getJoinedAt)
            .thenComparing(GroupMember::getId);

    private final SecureRandom random = new SecureRandom();

    private final RallyGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final GameTemplateMapper templateMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;
    private final GameService gameService;

    /**
     * 创建群组：建群 + 把自己加为群主。
     * Transactional（事务）：方法里的多条 SQL 要么全部成功，要么全部撤销，不会出现"群建了但没有群主"的情况。
     */
    @Transactional
    public GroupInfo create(Long userId, CreateGroupRequest request) {
        RallyGroup group = new RallyGroup();
        group.setName(request.name().trim());
        group.setSport(request.sport());
        group.setOwnerId(userId);
        insertWithUniqueInviteCode(group);

        GroupMember owner = new GroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(userId);
        owner.setRole(GroupRole.OWNER);
        memberMapper.insert(owner);

        return toInfo(group, GroupRole.OWNER, 1, scheduleText(List.of()));
    }

    /** 我加入的群组，按入群先后排序 */
    public List<GroupInfo> myGroups(Long userId) {
        List<GroupMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getUserId, userId)
                .orderByAsc(GroupMember::getJoinedAt)
                .orderByAsc(GroupMember::getId));
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = memberships.stream().map(GroupMember::getGroupId).toList();
        Map<Long, RallyGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<RallyGroup>()
                        .in(RallyGroup::getId, groupIds))
                .stream()
                .collect(Collectors.toMap(RallyGroup::getId, Function.identity()));
        Map<Long, Long> memberCounts = memberMapper.countByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(GroupMemberCount::getGroupId, GroupMemberCount::getMemberCount));
        Map<Long, String> scheduleTexts = scheduleTexts(groupIds);

        return memberships.stream()
                .filter(m -> groups.containsKey(m.getGroupId()))
                .map(m -> toInfo(
                        groups.get(m.getGroupId()),
                        m.getRole(),
                        memberCounts.getOrDefault(m.getGroupId(), 0L).intValue(),
                        scheduleTexts.get(m.getGroupId())))
                .toList();
    }

    /** 群组详情：只有群成员能看 */
    public GroupDetail detail(Long userId, Long groupId) {
        RallyGroup group = getGroupOrThrow(groupId);
        GroupMember me = requireMember(groupId, userId);
        List<GroupMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId));

        // 成员预览：排好序取前 10 个，再一次查出这些人的昵称头像
        List<GroupMember> preview = memberships.stream().sorted(MEMBER_ORDER).limit(MEMBER_PREVIEW_SIZE).toList();
        Map<Long, User> users = usersByIds(preview.stream().map(GroupMember::getUserId).toList());
        List<Member> members = preview.stream()
                .map(m -> users.get(m.getUserId()))
                .filter(u -> u != null)
                .map(Member::of)
                .toList();
        User ownerUser = users.containsKey(group.getOwnerId())
                ? users.get(group.getOwnerId())
                : userMapper.selectById(group.getOwnerId());

        // 接下来的场次：没取消、还没结束；最近结束的：没取消、已经结束，取最近 3 场
        LocalDateTime now = LocalDateTime.now();
        List<Game> upcoming = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                .eq(Game::getGroupId, groupId)
                .eq(Game::getCancelled, false)
                .gt(Game::getEndAt, now)
                .orderByAsc(Game::getStartAt));
        List<Game> recentFinished = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                .eq(Game::getGroupId, groupId)
                .eq(Game::getCancelled, false)
                .le(Game::getEndAt, now)
                .orderByDesc(Game::getStartAt)
                .last("LIMIT " + RECENT_FINISHED_SIZE));

        GroupInfo info = toInfo(group, me.getRole(), memberships.size(), scheduleTexts(List.of(groupId)).get(groupId));
        return GroupDetail.of(
                info,
                Member.of(ownerUser),
                members,
                gameService.toBriefs(upcoming, userId),
                gameService.toBriefs(recentFinished, userId));
    }

    /** 用邀请码加入群组 */
    public GroupInfo join(Long userId, JoinGroupRequest request) {
        // 前端输入框不区分大小写，统一转大写再查
        String code = request.code().trim().toUpperCase();
        RallyGroup group = groupMapper.selectOne(new LambdaQueryWrapper<RallyGroup>()
                .eq(RallyGroup::getInviteCode, code));
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "邀请码不正确，请向群主确认");
        }

        String alreadyJoined = "你已经在「" + group.getName() + "」里了";
        boolean exists = memberMapper.exists(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, group.getId())
                .eq(GroupMember::getUserId, userId));
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), alreadyJoined);
        }

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole(GroupRole.MEMBER);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 同一个人连点两次「加入」，数据库唯一索引 (group_id, user_id) 兜底
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), alreadyJoined);
        }

        return toInfo(group, GroupRole.MEMBER);
    }

    // ---------------- 公共方法：群组管理、固定局、场次模块也会用到 ----------------

    /** 查群组，不存在就报 404 */
    public RallyGroup getGroupOrThrow(Long groupId) {
        RallyGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "群组不存在");
        }
        return group;
    }

    /** 必须是群成员，返回我在群里的成员记录（里面有我的角色） */
    public GroupMember requireMember(Long groupId, Long userId) {
        GroupMember me = memberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (me == null) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "你还不是这个群的成员，先用邀请码加入吧");
        }
        return me;
    }

    /** 不是群成员就以普通成员身份加进来（通过分享链接报名的人用） */
    public void ensureMember(Long groupId, Long userId) {
        boolean exists = memberMapper.exists(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (exists) {
            return;
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(GroupRole.MEMBER);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // 并发时别的请求已经加进来了，不用管
        }
    }

    /** 必须是群主或管理员 */
    public GroupMember requireManager(Long groupId, Long userId) {
        GroupMember me = requireMember(groupId, userId);
        if (!me.getRole().canManage()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有群主和管理员可以操作");
        }
        return me;
    }

    /** 单个群组转成 GroupInfo（人数、固定局文字现查） */
    public GroupInfo toInfo(RallyGroup group, GroupRole myRole) {
        long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, group.getId()));
        return toInfo(group, myRole, (int) memberCount, scheduleTexts(List.of(group.getId())).get(group.getId()));
    }

    /** 随机生成一个 6 位邀请码（不保证不重复，插入 / 更新时靠唯一索引兜底） */
    public String randomInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        return code.toString();
    }

    // ---------------- 内部方法 ----------------

    private GroupInfo toInfo(RallyGroup group, GroupRole myRole, int memberCount, String scheduleText) {
        return new GroupInfo(group.getId(), group.getName(), group.getSport(), memberCount, myRole,
                scheduleText, group.getInviteCode());
    }

    /** 生成邀请码并插入；极小概率和已有的重复，重复就换一个再试 */
    private void insertWithUniqueInviteCode(RallyGroup group) {
        for (int attempt = 0; attempt < 5; attempt++) {
            group.setInviteCode(randomInviteCode());
            try {
                groupMapper.insert(group);
                return;
            } catch (DuplicateKeyException e) {
                // 邀请码撞了（唯一索引），换一个
            }
        }
        throw new BusinessException("生成邀请码失败，请重试");
    }

    private Map<Long, User> usersByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** 每个群的「每周六」文字，由群里的固定局算出来 */
    private Map<Long, String> scheduleTexts(Collection<Long> groupIds) {
        Map<Long, List<GameTemplate>> templatesByGroup = templateMapper.selectList(new LambdaQueryWrapper<GameTemplate>()
                        .in(GameTemplate::getGroupId, groupIds))
                .stream()
                .collect(Collectors.groupingBy(GameTemplate::getGroupId));
        return groupIds.stream().distinct().collect(Collectors.toMap(
                Function.identity(),
                id -> scheduleText(templatesByGroup.getOrDefault(id, List.of()))));
    }

    /**
     * 没有固定局 → 「还没有固定局」
     * 都暂停了 → 「固定局已暂停」
     * 否则 → 「每周二、周六」
     */
    static String scheduleText(List<GameTemplate> templates) {
        if (templates.isEmpty()) {
            return "还没有固定局";
        }
        List<String> weekdays = templates.stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .map(GameTemplate::getWeekday)
                .distinct()
                .sorted()
                .map(w -> WEEKDAY_TEXT[w])
                .toList();
        if (weekdays.isEmpty()) {
            return "固定局已暂停";
        }
        return "每" + String.join("、", weekdays);
    }
}
