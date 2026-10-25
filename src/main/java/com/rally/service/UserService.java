package com.rally.service;

import com.rally.common.UnauthorizedException;
import com.rally.dto.user.MyProfile;
import com.rally.dto.user.UpdateProfileRequest;
import com.rally.entity.User;
import com.rally.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    public MyProfile getMyProfile(Long userId) {
        User user = getUserOrUnauthorized(userId);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new MyProfile(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                userMapper.countGroups(userId),
                userMapper.countAttendanceSince(userId, monthStart),
                userMapper.countTotalAttendance(userId),
                userMapper.countNoShow(userId),
                userMapper.countPendingBills(userId)
        );
    }

    /** 修改昵称和个人介绍，返回修改后的「我的」数据 */
    public MyProfile updateProfile(Long userId, UpdateProfileRequest request) {
        getUserOrUnauthorized(userId);

        // 只设置要改的字段：updateById 只会更新不为 null 的字段，其他字段保持原样
        User update = new User();
        update.setId(userId);
        update.setNickname(request.nickname().trim());
        if (request.bio() != null) {
            update.setBio(request.bio().trim());
        }
        userMapper.updateById(update);
        return getMyProfile(userId);
    }

    /** 上传新头像：保存图片 → 更新头像地址 → 删掉旧头像文件 */
    public MyProfile uploadAvatar(Long userId, MultipartFile file) {
        User user = getUserOrUnauthorized(userId);
        String oldAvatar = user.getAvatar();

        String avatarUrl = fileStorageService.saveImage(file, "avatars");
        User update = new User();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        userMapper.updateById(update);

        fileStorageService.deleteIfLocal(oldAvatar);
        return getMyProfile(userId);
    }

    private User getUserOrUnauthorized(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            // token 是有效的，但用户已经不存在了（比如被删除），当作未登录处理
            throw new UnauthorizedException();
        }
        return user;
    }
}
