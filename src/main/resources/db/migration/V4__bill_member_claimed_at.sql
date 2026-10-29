-- V4：账单成员加「我已转账」的时间
-- 成员点了「我已转账」先记下时间，等组织者确认收款后才算已付。对应前端 BillMember.claimedAt / PendingBill.claimedAt
alter table bill_member
    add column claimed_at datetime null comment '成员点「我已转账」的时间，组织者确认收款后清空' after paid_at;
