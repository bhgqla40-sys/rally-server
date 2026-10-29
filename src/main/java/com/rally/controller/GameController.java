package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.game.AttendanceRequest;
import com.rally.dto.game.CancelGameRequest;
import com.rally.dto.game.GameBrief;
import com.rally.dto.game.GameDetail;
import com.rally.enums.GameFilter;
import com.rally.service.GameManageService;
import com.rally.service.GameService;
import com.rally.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 场次接口（都需要登录），对应前端 api/game.ts */
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SignupService signupService;
    private final GameManageService gameManageService;

    /** GET /api/games?filter=ALL|MINE|AVAILABLE  首页列表：我所在群组里还没结束的场次 */
    @GetMapping
    public Result<List<GameBrief>> list(@RequestParam(defaultValue = "ALL") GameFilter filter) {
        return Result.success(gameService.list(UserContext.getUserId(), filter));
    }

    /** GET /api/games/{id}  场次详情 */
    @GetMapping("/{id}")
    public Result<GameDetail> detail(@PathVariable Long id) {
        return Result.success(gameService.detail(UserContext.getUserId(), id));
    }

    /** POST /api/games/{id}/signup  报名：有名额直接报上，满员进候补 */
    @PostMapping("/{id}/signup")
    public Result<GameDetail> signup(@PathVariable Long id) {
        return Result.success(signupService.signup(UserContext.getUserId(), id));
    }

    /** DELETE /api/games/{id}/signup  取消报名或退出候补；空出名额时候补第一位自动递补 */
    @DeleteMapping("/{id}/signup")
    public Result<GameDetail> cancelSignup(@PathVariable Long id) {
        return Result.success(signupService.cancelMine(UserContext.getUserId(), id));
    }

    // ---------------- 场次管理（组织者） ----------------

    /** DELETE /api/games/{gameId}/signups/{memberId}  把某人移出报名或候补 */
    @DeleteMapping("/{gameId}/signups/{memberId}")
    public Result<GameDetail> removeSignup(@PathVariable Long gameId, @PathVariable Long memberId) {
        return Result.success(signupService.removeByOrganizer(UserContext.getUserId(), gameId, memberId));
    }

    /** POST /api/games/{gameId}/cancel  { reason }  取消这一场 */
    @PostMapping("/{gameId}/cancel")
    public Result<GameDetail> cancelGame(@PathVariable Long gameId, @Valid @RequestBody CancelGameRequest request) {
        return Result.success(gameManageService.cancelGame(UserContext.getUserId(), gameId, request));
    }

    /** PUT /api/games/{gameId}/attendance  { noShowIds }  结束后确认出勤 */
    @PutMapping("/{gameId}/attendance")
    public Result<GameDetail> confirmAttendance(@PathVariable Long gameId, @Valid @RequestBody AttendanceRequest request) {
        return Result.success(gameManageService.confirmAttendance(UserContext.getUserId(), gameId, request));
    }
}
