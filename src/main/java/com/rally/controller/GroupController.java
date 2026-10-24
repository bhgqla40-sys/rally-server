package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.group.CreateGroupRequest;
import com.rally.dto.group.GroupDetail;
import com.rally.dto.group.GroupInfo;
import com.rally.dto.group.JoinGroupRequest;
import com.rally.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
