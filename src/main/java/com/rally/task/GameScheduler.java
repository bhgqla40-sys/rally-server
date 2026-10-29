package com.rally.task;

import com.rally.service.GameTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务 ≈ 前端的 setInterval，只不过由 Spring 按 cron 表达式在后台跑。
 * 需要在配置类上加 @EnableScheduling 才会生效（见 SchedulingConfig）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameScheduler {

    private final GameTemplateService templateService;

    /**
     * 每 10 分钟检查一次：哪个固定局的这一场已经开始了，就生成下一周的那一场。
     * cron 有六位：秒 分 时 日 月 星期；下面这个表示「每 10 分钟的第 0 秒」。
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void generateNextGames() {
        int created = templateService.generateNextGames();
        if (created > 0) {
            log.info("自动生成了 {} 场固定局场次", created);
        }
    }
}
