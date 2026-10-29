package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.game.CreateTemplateRequest;
import com.rally.dto.game.CreateTemplateResult;
import com.rally.dto.game.GameTemplateBrief;
import com.rally.dto.game.SetTemplateActiveRequest;
import com.rally.service.GameTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 固定局接口（需要登录），对应前端 api/template.ts */
@RestController
@RequiredArgsConstructor
public class GameTemplateController {

    private final GameTemplateService templateService;

    /** POST /api/game-templates  GameTemplateForm  新建固定局，同时生成第一场 */
    @PostMapping("/game-templates")
    public Result<CreateTemplateResult> create(@Valid @RequestBody CreateTemplateRequest request) {
        return Result.success(templateService.create(UserContext.getUserId(), request));
    }

    /** GET /api/groups/{groupId}/game-templates  某个群的固定局 */
    @GetMapping("/groups/{groupId}/game-templates")
    public Result<List<GameTemplateBrief>> listByGroup(@PathVariable Long groupId) {
        return Result.success(templateService.listByGroup(UserContext.getUserId(), groupId));
    }

    /** PUT /api/game-templates/{id}  { active }  暂停 / 恢复固定局 */
    @PutMapping("/game-templates/{id}")
    public Result<GameTemplateBrief> setActive(@PathVariable Long id, @Valid @RequestBody SetTemplateActiveRequest request) {
        return Result.success(templateService.setActive(UserContext.getUserId(), id, request.active()));
    }
}
