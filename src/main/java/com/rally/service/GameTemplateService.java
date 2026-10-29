package com.rally.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import com.rally.dto.game.CreateTemplateRequest;
import com.rally.dto.game.CreateTemplateResult;
import com.rally.dto.game.GameTemplateBrief;
import com.rally.entity.Game;
import com.rally.entity.GameTemplate;
import com.rally.enums.FeeMode;
import com.rally.mapper.GameMapper;
import com.rally.mapper.GameTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 固定局规则：新建（同时生成第一场）、列表、暂停 / 恢复，以及每周自动生成下一场。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTemplateService {

    private final GameTemplateMapper templateMapper;
    private final GameMapper gameMapper;
    private final GroupService groupService;

    /** 新建固定局：群主、管理员。建好后马上生成第一场，返回第一场的 id */
    @Transactional
    public CreateTemplateResult create(Long userId, CreateTemplateRequest request) {
        groupService.getGroupOrThrow(request.groupId());
        groupService.requireManager(request.groupId(), userId);

        boolean fixedFee = request.feeMode() == FeeMode.FIXED;
        if (fixedFee && request.fixedFee() == null) {
            throw badRequest("请填写每人金额");
        }
        LocalTime startTime = LocalTime.parse(request.startTime());
        LocalTime openTime = LocalTime.parse(request.openTime());

        // 和前端一样，用一个示例日期检查：开放报名要早于截止报名
        LocalDateTime sampleStart = LocalDate.of(2026, 1, 10).atTime(startTime);
        if (!GameSchedule.openAt(sampleStart, request.openDaysBefore(), openTime)
                .isBefore(GameSchedule.deadlineAt(sampleStart, request.deadlineHoursBefore()))) {
            throw badRequest("开放报名的时间要早于截止时间");
        }

        GameTemplate template = new GameTemplate();
        template.setGroupId(request.groupId());
        template.setCreatorId(userId);
        template.setName(request.name().trim());
        template.setSport(request.sport());
        template.setVenue(request.venue().trim());
        template.setWeekday(request.weekday());
        template.setStartTime(startTime);
        template.setDurationMinutes(request.durationMinutes());
        template.setCapacity(request.capacity());
        template.setOpenDaysBefore(request.openDaysBefore());
        template.setOpenTime(openTime);
        template.setDeadlineHoursBefore(request.deadlineHoursBefore());
        template.setAllowWaitlist(request.allowWaitlist());
        // 不允许候补的话，自动递补也就没有意义了
        template.setAutoPromote(request.allowWaitlist() && request.autoPromote());
        template.setFeeMode(request.feeMode());
        template.setFixedFee(fixedFee ? request.fixedFee() : null);
        template.setEnabled(true);
        templateMapper.insert(template);

        LocalDateTime firstStart = GameSchedule.firstStart(
                template.getWeekday(), startTime, template.getDeadlineHoursBefore(), LocalDateTime.now());
        Game game = buildGame(template, firstStart);
        gameMapper.insert(game);
        return new CreateTemplateResult(game.getId());
    }

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

    /**
     * 给每个启用中的固定局补上下一场（定时任务每 10 分钟调用一次，见 GameScheduler）。
     * 规则：每个固定局保持「有一场还没开始」。上一场开始了，就生成一周后的那一场。
     * 返回这次生成了几场。
     */
    public int generateNextGames() {
        List<GameTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<GameTemplate>()
                .eq(GameTemplate::getEnabled, true));
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (GameTemplate template : templates) {
            Game latest = gameMapper.selectOne(new LambdaQueryWrapper<Game>()
                    .eq(Game::getTemplateId, template.getId())
                    .orderByDesc(Game::getStartAt)
                    .last("limit 1"));
            if (latest != null && latest.getStartAt().isAfter(now)) {
                continue; // 还有一场没开始，不用生成
            }
            LocalDateTime start = latest == null
                    ? GameSchedule.firstStart(template.getWeekday(), template.getStartTime(), template.getDeadlineHoursBefore(), now)
                    : GameSchedule.nextStart(latest.getStartAt(), template.getDeadlineHoursBefore(), now);
            try {
                gameMapper.insert(buildGame(template, start));
                created++;
            } catch (DuplicateKeyException e) {
                // 同一个固定局同一时间只能有一场（唯一索引 template_id + start_at），
                // 以后部署多台服务器、定时任务同时跑时，靠它防止重复生成
                log.info("固定局 {} 在 {} 的场次已经存在，跳过", template.getId(), start);
            }
        }
        return created;
    }

    /** 按固定局规则生成某一天的那一场 */
    static Game buildGame(GameTemplate template, LocalDateTime start) {
        Game game = new Game();
        game.setTemplateId(template.getId());
        game.setGroupId(template.getGroupId());
        game.setOrganizerId(template.getCreatorId());
        game.setTitle(template.getName());
        game.setSport(template.getSport());
        game.setVenue(template.getVenue());
        game.setStartAt(start);
        game.setEndAt(start.plusMinutes(template.getDurationMinutes()));
        game.setOpenAt(GameSchedule.openAt(start, template.getOpenDaysBefore(), template.getOpenTime()));
        game.setDeadlineAt(GameSchedule.deadlineAt(start, template.getDeadlineHoursBefore()));
        game.setCapacity(template.getCapacity());
        game.setJoinedCount(0);
        game.setWaitlistCount(0);
        game.setAllowWaitlist(template.getAllowWaitlist());
        game.setAutoPromote(template.getAutoPromote());
        game.setFeeMode(template.getFeeMode());
        game.setFeePerPerson(template.getFeeMode() == FeeMode.FIXED ? template.getFixedFee() : null);
        game.setCancelled(false);
        game.setAttendanceConfirmed(false);
        return game;
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
