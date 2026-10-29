package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.common.Member;
import com.rally.dto.group.GroupInfo;
import com.rally.dto.group.GroupMemberInfo;
import com.rally.dto.group.InviteCodeResult;
import com.rally.dto.group.SetMemberRoleRequest;
import com.rally.dto.group.UpdateGroupRequest;
import com.rally.entity.GroupMember;
import com.rally.entity.RallyGroup;
import com.rally.entity.User;
import com.rally.enums.GroupRole;
import com.rally.mapper.GroupMemberMapper;
import com.rally.mapper.RallyGroupMapper;
import com.rally.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 群组管理（群主 / 管理员）：成员名单、改群名、重置邀请码、设置管理员、移出成员。
 * 权限规则和前端 mock 保持一致，见 docs/api.md 的「权限和状态规则」。
 */
@Service
@RequiredArgsConstructor
public class GroupManageService {

    private final GroupService groupService;
    private final RallyGroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final SignupService signupService;

    /** 成员名单：群成员都能看 */
    public List<GroupMemberInfo> members(Long userId, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.requireMember(groupId, userId);
        return memberList(groupId);
    }

    /** 修改群名称：群主、管理员。场次里的群名是关联查出来的，会自动跟着变 */
    public GroupInfo rename(Long userId, Long groupId, UpdateGroupRequest request) {
        RallyGroup group = groupService.getGroupOrThrow(groupId);
        GroupMember me = groupService.requireManager(groupId, userId);

        RallyGroup update = new RallyGroup();
        update.setId(groupId);
        update.setName(request.name().trim());
        groupMapper.updateById(update);

        group.setName(update.getName());
        return groupService.toInfo(group, me.getRole());
    }

    /** 重新生成邀请码：群主、管理员。旧邀请码立即失效 */
    public InviteCodeResult resetInviteCode(Long userId, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.requireManager(groupId, userId);

        for (int attempt = 0; attempt < 5; attempt++) {
            RallyGroup update = new RallyGroup();
            update.setId(groupId);
            update.setInviteCode(groupService.randomInviteCode());
            try {
                groupMapper.updateById(update);
                return new InviteCodeResult(update.getInviteCode());
            } catch (DuplicateKeyException e) {
                // 和别的群的邀请码撞了，换一个
            }
        }
        throw new BusinessException("生成邀请码失败，请重试");
    }

    /** 设置 / 取消管理员：只有群主可以 */
    public List<GroupMemberInfo> setRole(Long userId, Long groupId, Long memberId, SetMemberRoleRequest request) {
        groupService.getGroupOrThrow(groupId);
        GroupMember me = groupService.requireManager(groupId, userId);
        if (me.getRole() != GroupRole.OWNER) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只有群主可以设置管理员");
        }
        if (request.role() == GroupRole.OWNER) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能设置为管理员或普通成员");
        }

        GroupMember target = findMember(groupId, memberId);
        if (target.getRole() == GroupRole.OWNER) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能修改群主的角色");
        }
        if (target.getRole() != request.role()) {
            GroupMember update = new GroupMember();
            update.setId(target.getId());
            update.setRole(request.role());
            memberMapper.updateById(update);
        }
        return memberList(groupId);
    }

    /**
     * 移出群组：群主可以移出任何人，管理员只能移出普通成员；不能移出群主和自己。
     * 同时取消他在本群还没开始的场次里的报名和候补（空出的名额会让候补递补）。
     */
    @Transactional
    public List<GroupMemberInfo> removeMember(Long userId, Long groupId, Long memberId) {
        groupService.getGroupOrThrow(groupId);
        GroupMember me = groupService.requireManager(groupId, userId);

        GroupMember target = findMember(groupId, memberId);
        if (target.getRole() == GroupRole.OWNER || memberId.equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能移出群主或自己");
        }
        if (me.getRole() == GroupRole.ADMIN && target.getRole() == GroupRole.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "管理员不能移出其他管理员");
        }

        memberMapper.deleteById(target.getId());
        signupService.cancelUpcomingSignupsInGroup(groupId, memberId);
        return memberList(groupId);
    }

    // ------------------------------------------------------------------

    private GroupMember findMember(Long groupId, Long userId) {
        GroupMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "这个人不在群里");
        }
        return member;
    }

    /** 完整成员名单：群主 → 管理员 → 普通成员，同角色按入群先后 */
    private List<GroupMemberInfo> memberList(Long groupId) {
        List<GroupMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId))
                .stream()
                .sorted(GroupService.MEMBER_ORDER)
                .toList();
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, memberships.stream().map(GroupMember::getUserId).toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return memberships.stream()
                .filter(m -> users.containsKey(m.getUserId()))
                .map(m -> new GroupMemberInfo(Member.of(users.get(m.getUserId())), m.getRole(), m.getJoinedAt()))
                .toList();
    }
}
