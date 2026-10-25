package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.group.CreateGroupRequest;
import com.rally.dto.group.GroupDetail;
import com.rally.dto.group.GroupInfo;
import com.rally.dto.group.GroupMemberInfo;
import com.rally.dto.group.InviteCodeResult;
import com.rally.dto.group.JoinGroupRequest;
import com.rally.dto.group.SetMemberRoleRequest;
import com.rally.dto.group.UpdateGroupRequest;
import com.rally.service.GroupManageService;
import com.rally.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 群组接口（都需要登录），对应前端 api/group.ts */
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupManageService groupManageService;

    /** POST /api/groups  { name, sport }  创建群组，我是群主 */
    @PostMapping
    public Result<GroupInfo> create(@Valid @RequestBody CreateGroupRequest request) {
        return Result.success(groupService.create(UserContext.getUserId(), request));
    }

    /** GET /api/groups/mine  我加入的群组 */
    @GetMapping("/mine")
    public Result<List<GroupInfo>> mine() {
        return Result.success(groupService.myGroups(UserContext.getUserId()));
    }

    /** GET /api/groups/{id}  群组详情 */
    @GetMapping("/{id}")
    public Result<GroupDetail> detail(@PathVariable Long id) {
        return Result.success(groupService.detail(UserContext.getUserId(), id));
    }

    /** POST /api/groups/join  { code }  用邀请码加入群组 */
    @PostMapping("/join")
    public Result<GroupInfo> join(@Valid @RequestBody JoinGroupRequest request) {
        return Result.success(groupService.join(UserContext.getUserId(), request));
    }

    // ---------------- 群组管理（群主 / 管理员） ----------------

    /** GET /api/groups/{groupId}/members  成员名单（带角色） */
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberInfo>> members(@PathVariable Long groupId) {
        return Result.success(groupManageService.members(UserContext.getUserId(), groupId));
    }

    /** PUT /api/groups/{groupId}  { name }  修改群名称 */
    @PutMapping("/{groupId}")
    public Result<GroupInfo> update(@PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return Result.success(groupManageService.rename(UserContext.getUserId(), groupId, request));
    }

    /** POST /api/groups/{groupId}/invite-code  重新生成邀请码，旧的立即失效 */
    @PostMapping("/{groupId}/invite-code")
    public Result<InviteCodeResult> resetInviteCode(@PathVariable Long groupId) {
        return Result.success(groupManageService.resetInviteCode(UserContext.getUserId(), groupId));
    }

    /** PUT /api/groups/{groupId}/members/{memberId}  { role }  设置 / 取消管理员（只有群主） */
    @PutMapping("/{groupId}/members/{memberId}")
    public Result<List<GroupMemberInfo>> setRole(@PathVariable Long groupId,
                                                 @PathVariable Long memberId,
                                                 @Valid @RequestBody SetMemberRoleRequest request) {
        return Result.success(groupManageService.setRole(UserContext.getUserId(), groupId, memberId, request));
    }

    /** DELETE /api/groups/{groupId}/members/{memberId}  移出群组 */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public Result<List<GroupMemberInfo>> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        return Result.success(groupManageService.removeMember(UserContext.getUserId(), groupId, memberId));
    }
}
