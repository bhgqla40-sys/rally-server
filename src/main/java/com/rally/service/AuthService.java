package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.client.WxClient;
import com.rally.common.BusinessException;
import com.rally.dto.auth.LoginRequest;
import com.rally.dto.auth.LoginResult;
import com.rally.dto.auth.RegisterRequest;
import com.rally.dto.auth.WxLoginRequest;
import com.rally.entity.User;
import com.rally.mapper.UserMapper;
import com.rally.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Service：业务逻辑层。Controller 只负责收参数、返回结果，真正的"做事"都在 Service 里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WxClient wxClient;

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
        return withWxBound(toLoginResult(user), bindWechat(user, request.wxCode()));
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
        return withWxBound(toLoginResult(user), bindWechat(user, request.wxCode()));
    }

    /**
     * 小程序一键登录：code 换 openid → 找到绑定这个微信的账号就直接登录；
     * 第一次用微信登录的新用户，自动建一个账号（昵称「微信用户xxxx」，没有手机号和密码），newUser = true。
     */
    public LoginResult wxLogin(WxLoginRequest request) {
        String openid = wxClient.code2Openid(request.code());
        User user = findByOpenid(openid);
        if (user != null) {
            return new LoginResult(jwtUtil.createToken(user.getId()), user.getId(), user.getNickname(), false, null);
        }

        User created = new User();
        created.setWxOpenid(openid);
        created.setNickname(randomWxNickname());
        try {
            userMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 同一个微信连点两次登录，第一个请求已经建好账号了，直接用它
            User existing = findByOpenid(openid);
            return new LoginResult(jwtUtil.createToken(existing.getId()), existing.getId(), existing.getNickname(), false, null);
        }
        return new LoginResult(jwtUtil.createToken(created.getId()), created.getId(), created.getNickname(), true, null);
    }

    /**
     * 小程序里用手机号登录 / 注册时，顺便把这个微信绑定到手机号账号上。
     * 绑定是「顺便做的」，失败了也不影响登录：
     * - 没传 wxCode（H5）→ 返回 null
     * - 这个账号已经绑了同一个微信 → true；绑了别的微信 → false（一个账号只绑一个微信）
     * - 这个微信已经是别的账号的了（比如之前点过一键登录、自动建了账号）→ false
     */
    private Boolean bindWechat(User user, String wxCode) {
        if (wxCode == null || wxCode.isBlank()) {
            return null;
        }
        String openid;
        try {
            openid = wxClient.code2Openid(wxCode);
        } catch (BusinessException e) {
            log.info("登录时绑定微信失败，userId={}：{}", user.getId(), e.getMessage());
            return false;
        }
        if (user.getWxOpenid() != null) {
            return user.getWxOpenid().equals(openid);
        }
        if (findByOpenid(openid) != null) {
            return false;
        }
        try {
            // 带上 wx_openid is null 条件，防止并发时覆盖掉别的请求刚绑上的微信
            int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .set(User::getWxOpenid, openid)
                    .eq(User::getId, user.getId())
                    .isNull(User::getWxOpenid));
            return updated == 1;
        } catch (DuplicateKeyException e) {
            return false; // 同一时刻这个微信被别的账号绑走了（wx_openid 有唯一索引）
        }
    }

    private User findByOpenid(String openid) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWxOpenid, openid));
    }

    /** 新微信用户的默认昵称，例如「微信用户4821」 */
    private static String randomWxNickname() {
        return "微信用户" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private static LoginResult withWxBound(LoginResult result, Boolean wxBound) {
        return new LoginResult(result.token(), result.userId(), result.nickname(), null, wxBound);
    }

    private LoginResult toLoginResult(User user) {
        return LoginResult.of(jwtUtil.createToken(user.getId()), user.getId(), user.getNickname());
    }
}
