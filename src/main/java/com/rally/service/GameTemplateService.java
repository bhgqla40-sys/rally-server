package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.game.GameTemplateBrief;
import com.rally.entity.GameTemplate;
import com.rally.mapper.GameTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 固定局规则。
 * 目前有：某个群的固定局列表、暂停 / 恢复；创建固定局（并生成第一场）做场次模块时加在这里。
 */
@Service
@RequiredArgsConstructor
public class GameTemplateService {

    private final GameTemplateMapper templateMapper;
    private final GroupService groupService;

    /** 某个群的固定局，按星期几、开始时间排序；群成员都能看 */
    public List<GameTemplateBrief> listByGroup(Long userId, Long groupId) {
        groupService.getGroupOrThrow(groupId);
        groupService.requireMember(groupId, userId);
        return templateMapper.selectList(new LambdaQueryWrapper<GameTemplate>()
                        .eq(GameTemplate::getGroupId, groupId)
                        .orderByAsc(GameTemplate::getWeekday)
                        .orderByAsc(GameTemplate::getStartTime))
                .stream()
                .map(GameTemplateBrief::of)
                .toList();
    }

    /** 暂停 / 恢复：群主、管理员。只影响以后自动生成的场次，已经生成的不受影响 */
    public GameTemplateBrief setActive(Long userId, Long templateId, boolean active) {
        GameTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "固定局不存在");
        }
        groupService.requireManager(template.getGroupId(), userId);

        GameTemplate update = new GameTemplate();
        update.setId(templateId);
        update.setEnabled(active);
        templateMapper.updateById(update);

        template.setEnabled(active);
        return GameTemplateBrief.of(template);
    }
}
