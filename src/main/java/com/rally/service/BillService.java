package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.bill.AddBillItemRequest;
import com.rally.dto.bill.BillInfo;
import com.rally.dto.bill.BillItemInfo;
import com.rally.dto.bill.BillMemberInfo;
import com.rally.dto.bill.PendingBill;
import com.rally.dto.common.Member;
import com.rally.entity.Bill;
import com.rally.entity.BillItem;
import com.rally.entity.BillMember;
import com.rally.entity.Game;
import com.rally.entity.GameSignup;
import com.rally.entity.GroupMember;
import com.rally.entity.RallyGroup;
import com.rally.entity.User;
import com.rally.enums.GameStatus;
import com.rally.enums.SignupStatus;
import com.rally.mapper.BillItemMapper;
import com.rally.mapper.BillMapper;
import com.rally.mapper.BillMemberMapper;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameSignupMapper;
import com.rally.mapper.GroupMemberMapper;
import com.rally.mapper.RallyGroupMapper;
import com.rally.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AA 记账。
 * 金额全部用「分」存和算（整数），不用小数，避免 0.1 + 0.2 这种精度问题。
 * 到场成员 = 报名名单 - 鸽子，按报名顺序排；总费用平均分摊，除不尽的几分钱由排在前面的人各多出 1 分。
 * 组织者垫付了全部费用，自己那份默认已付。
 */
@Service
@RequiredArgsConstructor
public class BillService {

    /** 一个账单最多几个费用项 */
    static final int MAX_ITEMS = 20;

    private final BillMapper billMapper;
    private final BillItemMapper itemMapper;
    private final BillMemberMapper memberMapper;
    private final GameMapper gameMapper;
    private final GameSignupMapper signupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final RallyGroupMapper groupMapper;
    private final UserMapper userMapper;

    /**
     * 某一场的账单；还没有就新建一个空账单。
     * - 新建：只有组织者、群主 / 管理员能建，而且这一场要已经结束
     * - 查看：群成员，或者在账单里的人（被移出群了也还能看到自己欠的钱）
     */
    @Transactional
    public BillInfo getOrCreateByGame(Long userId, Long gameId) {
        Bill bill = findByGame(gameId);
        if (bill != null) {
            Game game = gameMapper.selectById(gameId);
            requireViewer(bill, game, userId);
            return toInfo(bill, game);
        }

        // 锁住这一场再查一次：两个人同时打开结算页时，只有一个人会建账单
        Game game = gameMapper.selectByIdForUpdate(gameId);
        if (game == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "场次不存在");
        }
        bill = findByGame(gameId);
        if (bill != null) {
            requireViewer(bill, game, userId);
            return toInfo(bill, game);
        }

        GroupMember me = findGroupMember(game.getGroupId(), userId);
        boolean canCreate = game.getOrganizerId().equals(userId) || (me != null && me.getRole().canManage());
        if (!canCreate) {
            if (me == null) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "你不在这个群里，看不了这场的账单");
            }
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "组织者还没开始结算这一场");
        }
        GameStatus status = GameService.statusOf(game, LocalDateTime.now());
        if (status == GameStatus.CANCELLED) {
            throw badRequest("这一场已经取消了，不用结算");
        }
        if (status != GameStatus.FINISHED) {
            throw badRequest("结束后才能结算费用");
        }

        bill = new Bill();
        bill.setGameId(gameId);
        bill.setCreatedBy(userId);
        billMapper.insert(bill);
        for (Long attendee : attendeeIds(game)) {
            insertMember(bill.getId(), attendee, attendee.equals(game.getOrganizerId()));
        }
        return toInfo(bill, game);
    }

    /** 添加费用项（组织者），然后重新分摊 */
    @Transactional
    public BillInfo addItem(Long userId, Long billId, AddBillItemRequest request) {
        Bill bill = lockBill(billId);
        Game game = requireOrganizer(bill, userId);
        long count = itemMapper.selectCount(new LambdaQueryWrapper<BillItem>().eq(BillItem::getBillId, billId));
        if (count >= MAX_ITEMS) {
            throw badRequest("费用项最多 " + MAX_ITEMS + " 个");
        }
        BillItem item = new BillItem();
        item.setBillId(billId);
        item.setName(request.name().trim());
        item.setAmount(request.amount());
        itemMapper.insert(item);
        recalc(billId);
        return toInfo(bill, game);
    }

    /** 删除费用项（组织者），然后重新分摊 */
    @Transactional
    public BillInfo removeItem(Long userId, Long billId, Long itemId) {
        Bill bill = lockBill(billId);
        Game game = requireOrganizer(bill, userId);
        int deleted = itemMapper.delete(new LambdaQueryWrapper<BillItem>()
                .eq(BillItem::getId, itemId)
                .eq(BillItem::getBillId, billId));
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "费用项不存在");
        }
        recalc(billId);
        return toInfo(bill, game);
    }

    /** 组织者标记某人已付 / 未付。标记已付时清掉他的「我已转账」 */
    @Transactional
    public BillInfo setPaid(Long userId, Long billId, Long memberId, boolean paid) {
        Bill bill = lockBill(billId);
        Game game = requireOrganizer(bill, userId);
        BillMember target = findMember(billId, memberId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "这个人不在账单里");
        }
        if (target.getIsOrganizer()) {
            throw badRequest("组织者已经垫付了全部费用，不用标记");
        }
        if (target.getPaid() != paid) {
            memberMapper.update(null, new LambdaUpdateWrapper<BillMember>()
                    .set(BillMember::getPaid, paid)
                    .set(BillMember::getPaidAt, paid ? LocalDateTime.now() : null)
                    .set(BillMember::getClaimedAt, null)
                    .eq(BillMember::getId, target.getId()));
        }
        return toInfo(bill, game);
    }

    /** 成员点「我已转账」：只是告诉组织者一声，真正算已付要组织者确认 */
    @Transactional
    public void claimPaid(Long userId, Long billId) {
        lockBill(billId);
        BillMember mine = findMember(billId, userId);
        if (mine == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "你不在这个账单里");
        }
        if (mine.getIsOrganizer()) {
            throw badRequest("你是组织者，不用转账");
        }
        if (mine.getPaid()) {
            throw badRequest("组织者已经确认收到了");
        }
        if (mine.getAmount() <= 0) {
            throw badRequest("还没有要付的费用");
        }
        BillMember update = new BillMember();
        update.setId(mine.getId());
        update.setClaimedAt(LocalDateTime.now());
        memberMapper.updateById(update);
        // TODO 以后加消息通知：告诉组织者有人说已转账
    }

    /** 我的待付款：我在账单里、还没付、不是组织者、金额大于 0；最近的场次在前 */
    public List<PendingBill> myPendingBills(Long userId) {
        List<BillMember> mine = memberMapper.selectList(new LambdaQueryWrapper<BillMember>()
                .eq(BillMember::getUserId, userId)
                .eq(BillMember::getPaid, false)
                .eq(BillMember::getIsOrganizer, false)
                .gt(BillMember::getAmount, 0));
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, Bill> bills = billMapper.selectList(new LambdaQueryWrapper<Bill>()
                        .in(Bill::getId, mine.stream().map(BillMember::getBillId).toList()))
                .stream()
                .collect(Collectors.toMap(Bill::getId, Function.identity()));
        Map<Long, Game> games = gameMapper.selectList(new LambdaQueryWrapper<Game>()
                        .in(Game::getId, bills.values().stream().map(Bill::getGameId).toList()))
                .stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        Map<Long, String> groupNames = groupNames(games.values().stream().map(Game::getGroupId).toList());
        Map<Long, User> organizers = usersByIds(games.values().stream().map(Game::getOrganizerId).toList());

        List<PendingBill> result = new ArrayList<>();
        for (BillMember m : mine) {
            Bill bill = bills.get(m.getBillId());
            Game game = bill == null ? null : games.get(bill.getGameId());
            if (game == null) {
                continue;
            }
            result.add(new PendingBill(bill.getId(), game.getId(), game.getTitle(), game.getStartAt(),
                    groupNames.get(game.getGroupId()), toMember(organizers.get(game.getOrganizerId())),
                    m.getAmount(), m.getClaimedAt()));
        }
        result.sort(Comparator.comparing(PendingBill::gameStartAt).reversed());
        return result;
    }

    /**
     * 组织者确认出勤后调用：如果这一场已经有账单，按最新的出勤名单增删账单成员，再重新分摊。
     * 调用方（GameManageService.confirmAttendance）已经在事务里锁住了这一场。
     */
    public void syncMembers(Game game) {
        Bill bill = findByGame(game.getId());
        if (bill == null) {
            return;
        }
        lockBill(bill.getId());
        List<Long> attendees = attendeeIds(game);
        Set<Long> attendeeSet = new HashSet<>(attendees);
        List<BillMember> existing = membersOf(bill.getId());
        Set<Long> existingIds = existing.stream().map(BillMember::getUserId).collect(Collectors.toSet());

        List<Long> toRemove = existing.stream()
                .filter(m -> !attendeeSet.contains(m.getUserId()))
                .map(BillMember::getId)
                .toList();
        if (!toRemove.isEmpty()) {
            memberMapper.delete(new LambdaQueryWrapper<BillMember>().in(BillMember::getId, toRemove));
        }
        for (Long attendee : attendees) {
            if (!existingIds.contains(attendee)) {
                insertMember(bill.getId(), attendee, attendee.equals(game.getOrganizerId()));
            }
        }
        recalc(bill.getId());
    }

    /**
     * 平均分摊：total 分钱分给 count 个人，除不尽的余数由前几个人各多出 1 分，
     * 保证加起来正好等于 total。例如 100 分给 3 人 → [34, 33, 33]
     */
    static List<Integer> splitEvenly(int total, int count) {
        if (count <= 0) {
            return List.of();
        }
        int base = total / count;
        int remainder = total - base * count;
        List<Integer> shares = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            shares.add(base + (i < remainder ? 1 : 0));
        }
        return shares;
    }

    // ---------------- 内部方法 ----------------

    /** 按当前费用项重新算每个人应付多少，金额变了的才更新 */
    private void recalc(Long billId) {
        int total = itemMapper.selectList(new LambdaQueryWrapper<BillItem>().eq(BillItem::getBillId, billId))
                .stream()
                .mapToInt(BillItem::getAmount)
                .sum();
        List<BillMember> members = membersOf(billId);
        List<Integer> shares = splitEvenly(total, members.size());
        for (int i = 0; i < members.size(); i++) {
            BillMember m = members.get(i);
            if (!m.getAmount().equals(shares.get(i))) {
                BillMember update = new BillMember();
                update.setId(m.getId());
                update.setAmount(shares.get(i));
                memberMapper.updateById(update);
            }
        }
    }

    /** 到场的人：已报名、没被标记成鸽子，按报名顺序 */
    private List<Long> attendeeIds(Game game) {
        return signupMapper.selectList(new LambdaQueryWrapper<GameSignup>()
                        .eq(GameSignup::getGameId, game.getId())
                        .eq(GameSignup::getStatus, SignupStatus.JOINED)
                        .eq(GameSignup::getNoShow, false)
                        .orderByAsc(GameSignup::getQueuedAt))
                .stream()
                .map(GameSignup::getUserId)
                .toList();
    }

    private void insertMember(Long billId, Long userId, boolean isOrganizer) {
        BillMember member = new BillMember();
        member.setBillId(billId);
        member.setUserId(userId);
        member.setAmount(0);
        member.setPaid(isOrganizer);
        member.setIsOrganizer(isOrganizer);
        memberMapper.insert(member);
    }

    private Bill findByGame(Long gameId) {
        return billMapper.selectOne(new LambdaQueryWrapper<Bill>().eq(Bill::getGameId, gameId));
    }

    private Bill lockBill(Long billId) {
        Bill bill = billMapper.selectByIdForUpdate(billId);
        if (bill == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "账单不存在");
        }
        return bill;
    }

    private Game requireOrganizer(Bill bill, Long userId) {
        Game game = gameMapper.selectById(bill.getGameId());
        if (!game.getOrganizerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有组织者可以操作");
        }
        return game;
    }

    /** 能看账单的人：组织者、群成员、账单里的人 */
    private void requireViewer(Bill bill, Game game, Long userId) {
        boolean ok = game.getOrganizerId().equals(userId)
                || findGroupMember(game.getGroupId(), userId) != null
                || findMember(bill.getId(), userId) != null;
        if (!ok) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "你不在这个群里，看不了这场的账单");
        }
    }

    private GroupMember findGroupMember(Long groupId, Long userId) {
        return groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
    }

    private BillMember findMember(Long billId, Long userId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<BillMember>()
                .eq(BillMember::getBillId, billId)
                .eq(BillMember::getUserId, userId));
    }

    /** 账单成员按加入顺序（= 报名顺序）排，分摊余数也按这个顺序给 */
    private List<BillMember> membersOf(Long billId) {
        return memberMapper.selectList(new LambdaQueryWrapper<BillMember>()
                .eq(BillMember::getBillId, billId)
                .orderByAsc(BillMember::getId));
    }

    private BillInfo toInfo(Bill bill, Game game) {
        List<BillItemInfo> items = itemMapper.selectList(new LambdaQueryWrapper<BillItem>()
                        .eq(BillItem::getBillId, bill.getId())
                        .orderByAsc(BillItem::getId))
                .stream()
                .map(BillItemInfo::of)
                .toList();
        List<BillMember> members = membersOf(bill.getId());
        Map<Long, User> users = usersByIds(Stream.concat(
                members.stream().map(BillMember::getUserId),
                Stream.of(game.getOrganizerId())).collect(Collectors.toSet()));
        List<BillMemberInfo> memberInfos = members.stream()
                .map(m -> new BillMemberInfo(toMember(users.get(m.getUserId())), m.getAmount(), m.getPaid(),
                        m.getPaidAt(), m.getIsOrganizer(), m.getClaimedAt()))
                .toList();
        RallyGroup group = groupMapper.selectById(game.getGroupId());
        return new BillInfo(bill.getId(), game.getId(), game.getTitle(), game.getStartAt(),
                group == null ? null : group.getName(), toMember(users.get(game.getOrganizerId())),
                items, memberInfos);
    }

    private static Member toMember(User user) {
        return user == null ? null : Member.of(user);
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

    private static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
