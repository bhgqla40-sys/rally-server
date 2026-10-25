package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.user.MyProfile;
import com.rally.dto.user.UpdateProfileRequest;
import com.rally.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 用户相关接口（需要登录），对应前端 api/user.ts */
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

    /** PUT /api/users/me  { nickname, bio }  修改昵称和个人介绍 */
    @PutMapping("/me")
    public Result<MyProfile> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(UserContext.getUserId(), request));
    }

    /**
     * POST /api/users/me/avatar  上传头像
     * 请求格式是 multipart/form-data（文件上传的格式），文件字段名叫 file，
     * 前端用 uni.uploadFile({ name: 'file', ... }) 上传。
     */
    @PostMapping("/me/avatar")
    public Result<MyProfile> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(UserContext.getUserId(), file));
    }
}
