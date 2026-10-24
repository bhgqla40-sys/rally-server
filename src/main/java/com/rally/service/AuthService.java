package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.dto.auth.LoginRequest;
import com.rally.dto.auth.LoginResult;
import com.rally.dto.auth.RegisterRequest;
import com.rally.entity.User;
import com.rally.mapper.UserMapper;
import com.rally.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service：业务逻辑层。Controller 只负责收参数、返回结果，真正的"做事"都在 Service 里。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * BCrypt：专门用来存密码的加密算法。
     * 同一个密码每次加密结果都不一样（自带随机盐），而且无法解密，只能用 matches() 比对。
     * 这样即使数据库泄露，别人也拿不到用户的原始密码。
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResult register(RegisterRequest request) {
        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.phone()));
        if (exists) {
            throw new BusinessException("这个手机号已经注册过了，直接登录吧");
        }

        User user = new User();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        try {
            userMapper.insert(user); // 插入后 user.getId() 自动有值
        } catch (DuplicateKeyException e) {
            // 两个请求同时注册同一个手机号时，数据库的唯一索引会兜底拦住第二个
            throw new BusinessException("这个手机号已经注册过了，直接登录吧");
        }
        return toLoginResult(user);
    }

    public LoginResult login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.phone()));
        // 手机号不存在和密码错误给同一个提示，避免别人用登录接口试探哪些手机号注册过
        if (user == null
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("手机号或密码错误");
        }
        return toLoginResult(user);
    }

    private LoginResult toLoginResult(User user) {
        return new LoginResult(jwtUtil.createToken(user.getId()), user.getId(), user.getNickname());
    }
}
