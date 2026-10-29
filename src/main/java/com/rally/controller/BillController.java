package com.rally.controller;

import com.rally.common.Result;
import com.rally.common.UserContext;
import com.rally.dto.bill.AddBillItemRequest;
import com.rally.dto.bill.BillInfo;
import com.rally.dto.bill.PendingBill;
import com.rally.dto.bill.SetPaidRequest;
import com.rally.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 费用结算接口（需要登录），对应前端 api/bill.ts。金额单位都是分 */
@RestController
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /** GET /api/games/{gameId}/bill  某一场的账单，还没有就新建（组织者 / 群管理，结束后） */
    @GetMapping("/games/{gameId}/bill")
    public Result<BillInfo> byGame(@PathVariable Long gameId) {
        return Result.success(billService.getOrCreateByGame(UserContext.getUserId(), gameId));
    }

    /** POST /api/bills/{billId}/items  { name, amount }  添加费用项（组织者） */
    @PostMapping("/bills/{billId}/items")
    public Result<BillInfo> addItem(@PathVariable Long billId, @Valid @RequestBody AddBillItemRequest request) {
        return Result.success(billService.addItem(UserContext.getUserId(), billId, request));
    }

    /** DELETE /api/bills/{billId}/items/{itemId}  删除费用项（组织者） */
    @DeleteMapping("/bills/{billId}/items/{itemId}")
    public Result<BillInfo> removeItem(@PathVariable Long billId, @PathVariable Long itemId) {
        return Result.success(billService.removeItem(UserContext.getUserId(), billId, itemId));
    }

    /** PUT /api/bills/{billId}/members/{memberId}  { paid }  标记已付 / 未付（组织者） */
    @PutMapping("/bills/{billId}/members/{memberId}")
    public Result<BillInfo> setPaid(@PathVariable Long billId, @PathVariable Long memberId,
                                    @Valid @RequestBody SetPaidRequest request) {
        return Result.success(billService.setPaid(UserContext.getUserId(), billId, memberId, request.paid()));
    }

    /** POST /api/bills/{billId}/claim  成员点「我已转账」，等组织者确认 */
    @PostMapping("/bills/{billId}/claim")
    public Result<Void> claim(@PathVariable Long billId) {
        billService.claimPaid(UserContext.getUserId(), billId);
        return Result.success();
    }

    /** GET /api/users/me/pending-bills  我的待付款 */
    @GetMapping("/users/me/pending-bills")
    public Result<List<PendingBill>> myPendingBills() {
        return Result.success(billService.myPendingBills(UserContext.getUserId()));
    }
}
