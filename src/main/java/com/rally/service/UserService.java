package com.rally.service;

import com.rally.common.UnauthorizedException;
import com.rally.dto.user.MyProfile;
import com.rally.entity.User;
import com.rally.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public MyProfile getMyProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // token 是有效的，但用户已经不存在了（比如被删除），当作未登录处理
            throw new UnauthorizedException();
        }

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new MyProfile(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                userMapper.countGroups(userId),
                userMapper.countAttendanceSince(userId, monthStart),
                userMapper.countTotalAttendance(userId),
                userMapper.countNoShow(userId),
                userMapper.countPendingBills(userId)
        );
    }
}
