-- V3：场次加「取消原因」和「是否已确认出勤」
-- 对应前端 GameDetail.cancelReason / GameDetail.attendanceConfirmed
alter table game
    add column cancel_reason        varchar(100) null comment '组织者取消这一场时填写的原因' after cancelled,
    add column attendance_confirmed tinyint(1)   not null default 0 comment '结束后组织者是否已确认出勤（标记鸽子）' after cancel_reason;
