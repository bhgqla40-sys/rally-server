package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.user.MyProfile;
import com.rally.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户相关接口（需要登录） */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/me  「我的」页面数据 */
    @GetMapping("/me")
    public Result<MyProfile> me() {
        return Result.success(userService.getMyProfile(UserContext.getUserId()));
    }
}
