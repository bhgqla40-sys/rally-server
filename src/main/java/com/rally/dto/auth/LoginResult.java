package com.rally.dto.auth;

/** 登录 / 注册成功后返回给前端的数据，对应前端 api/auth.ts 的 LoginResult */
public record LoginResult(
        String token,
        Long userId,
        String nickname,
        /*
         * 只有微信一键登录会返回：true 表示这次是新建的账号（昵称是自动起的「微信用户xxxx」），
         * 前端可以引导用户去编辑资料页设置头像和昵称。手机号登录 / 注册不返回这个字段。
         */
        Boolean newUser,
        /*
         * 只有在小程序里用手机号登录 / 注册、并且传了 wxCode 时返回：
         * true 绑定成功；false 没绑上（这个微信已经绑了别的账号，或者这个账号已经绑了别的微信），登录本身不受影响
         */
        Boolean wxBound
) {
    public static LoginResult of(String token, Long userId, String nickname) {
        return new LoginResult(token, userId, nickname, null, null);
    }
}
