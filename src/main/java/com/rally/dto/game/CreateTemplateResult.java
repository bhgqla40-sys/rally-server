package com.rally.dto.game;

/** 新建固定局的返回：{ firstGameId }，前端拿它跳到第一场的详情页 */
public record CreateTemplateResult(Long firstGameId) {
}
