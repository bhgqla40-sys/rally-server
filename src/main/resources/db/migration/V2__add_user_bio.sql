-- V2：用户加「个人介绍」字段
-- V1 已经执行过了，表结构有变化就新建一个版本号更大的脚本，Flyway 启动时会自动执行它
ALTER TABLE `user`
    ADD COLUMN bio VARCHAR(100) NULL COMMENT '个人介绍' AFTER avatar;
