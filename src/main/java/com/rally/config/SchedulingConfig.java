package com.rally.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 打开定时任务功能（@Scheduled 注解要靠它才会生效） */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
