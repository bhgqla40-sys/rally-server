-- =====================================================================
-- V1：初始化表结构
-- 字段对应前端 rally-app/src/types/index.ts 里的数据结构
--
-- Flyway 规则：文件名 V<版本号>__<描述>.sql，启动时按版本号顺序执行没执行过的脚本。
-- 已经执行过的脚本【不要再改】，要改表结构就新建 V2__xxx.sql、V3__xxx.sql……
--
-- 约定：
--   1. 主键 id 都是 BIGINT 自增
--   2. 金额统一用「分」存 INT（¥35 = 3500），避免小数精度问题
--   3. 时间用 DATETIME，存北京时间
--   4. 枚举值（运动类型、状态等）存英文字符串，和前端的类型定义一致，例如 'BADMINTON'
--   5. 不建外键约束（互联网项目的常见做法，关联关系由代码保证），但关联字段都建了索引
-- =====================================================================


-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
CREATE TABLE `user`
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone         VARCHAR(20)  NULL COMMENT '手机号（H5 用手机号 + 密码登录）',
    password_hash VARCHAR(100) NULL COMMENT '加密后的密码（BCrypt），绝不存明文',
    wx_openid     VARCHAR(64)  NULL COMMENT '微信小程序 openid（小程序登录用）',
    nickname      VARCHAR(32)  NOT NULL COMMENT '昵称',
    avatar        VARCHAR(255) NULL COMMENT '头像 URL',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_wx_openid (wx_openid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '用户';


-- ---------------------------------------------------------------------
-- 群组（group 是 MySQL 关键字，所以表名叫 rally_group）
-- ---------------------------------------------------------------------
CREATE TABLE rally_group
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(30) NOT NULL COMMENT '群组名称',
    sport       VARCHAR(20) NOT NULL COMMENT '运动类型：BADMINTON / BASKETBALL / FOOTBALL / FRISBEE / OTHER',
    owner_id    BIGINT      NOT NULL COMMENT '群主 user.id',
    invite_code CHAR(6)     NOT NULL COMMENT '邀请码，6 位大写字母和数字',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_code (invite_code),
    KEY idx_owner_id (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '群组';


-- ---------------------------------------------------------------------
-- 群组成员
-- ---------------------------------------------------------------------
CREATE TABLE group_member
(
    id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id  BIGINT      NOT NULL COMMENT '群组 rally_group.id',
    user_id   BIGINT      NOT NULL COMMENT '成员 user.id',
    role      VARCHAR(10) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：OWNER 群主 / ADMIN 管理员 / MEMBER 成员',
    joined_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入群时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_user (group_id, user_id) COMMENT '同一个人在一个群里只有一条记录',
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '群组成员';


-- ---------------------------------------------------------------------
-- 固定局规则：每周按这个规则自动生成一场
-- ---------------------------------------------------------------------
CREATE TABLE game_template
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id              BIGINT       NOT NULL COMMENT '所属群组 rally_group.id',
    creator_id            BIGINT       NOT NULL COMMENT '创建人 user.id，也是生成出来的场次的组织者',
    name                  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '名称，例如「周六晚羽毛球」；为空时自动生成',
    sport                 VARCHAR(20)  NOT NULL COMMENT '运动类型',
    venue                 VARCHAR(100) NOT NULL COMMENT '场地',
    weekday               TINYINT      NOT NULL COMMENT '每周几：1 = 周一 … 7 = 周日',
    start_time            TIME         NOT NULL COMMENT '开始时间',
    duration_minutes      INT          NOT NULL COMMENT '时长（分钟）',
    capacity              INT          NOT NULL COMMENT '人数上限',
    open_days_before      INT          NOT NULL COMMENT '提前几天开放报名',
    open_time             TIME         NOT NULL COMMENT '开放报名的时刻',
    deadline_hours_before INT          NOT NULL COMMENT '开局前几小时截止报名',
    allow_waitlist        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许候补',
    auto_promote          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '有人取消时是否自动从候补递补',
    fee_mode              VARCHAR(10)  NOT NULL DEFAULT 'AA' COMMENT '收费方式：AA 分摊 / FIXED 固定每人',
    fixed_fee             INT          NULL COMMENT '固定收费时每人金额（分）',
    enabled               TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用，停用后不再自动生成场次',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_group_id (group_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '固定局规则';


-- ---------------------------------------------------------------------
-- 场次：每一次具体的球局
-- 状态里只存「已取消」，其余状态由时间推算（不存库，避免和时间对不上）：
--   now < open_at → UPCOMING 未开放；open_at ≤ now < deadline_at → OPEN 报名中；
--   deadline_at ≤ now < end_at → CLOSED 已截止；now ≥ end_at → FINISHED 已结束
-- ---------------------------------------------------------------------
CREATE TABLE game
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id    BIGINT       NULL COMMENT '由哪个固定局规则生成；单独发起的场次为空',
    group_id       BIGINT       NOT NULL COMMENT '所属群组 rally_group.id',
    organizer_id   BIGINT       NOT NULL COMMENT '组织者 user.id',
    title          VARCHAR(50)  NOT NULL COMMENT '标题',
    sport          VARCHAR(20)  NOT NULL COMMENT '运动类型',
    venue          VARCHAR(100) NOT NULL COMMENT '场地',
    start_at       DATETIME     NOT NULL COMMENT '开始时间',
    end_at         DATETIME     NOT NULL COMMENT '结束时间',
    open_at        DATETIME     NOT NULL COMMENT '开放报名时间',
    deadline_at    DATETIME     NOT NULL COMMENT '截止报名时间，之后不能报名，也不能取消',
    capacity       INT          NOT NULL COMMENT '人数上限',
    joined_count   INT          NOT NULL DEFAULT 0 COMMENT '已报名人数（冗余计数，报名时靠它防止超员）',
    waitlist_count INT          NOT NULL DEFAULT 0 COMMENT '候补人数（冗余计数）',
    allow_waitlist TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许候补',
    auto_promote   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '有人取消时是否自动从候补递补',
    fee_mode       VARCHAR(10)  NOT NULL DEFAULT 'AA' COMMENT '收费方式：AA / FIXED',
    fee_per_person INT          NULL COMMENT '预计每人费用（分）',
    cancelled      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已取消',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_start (template_id, start_at) COMMENT '同一个固定局同一时间只有一场，防止定时任务重复生成',
    KEY idx_group_start (group_id, start_at),
    KEY idx_start_at (start_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '场次';


-- ---------------------------------------------------------------------
-- 报名记录（已报名和候补都在这张表，用 status 区分）
-- ---------------------------------------------------------------------
CREATE TABLE game_signup
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    game_id      BIGINT      NOT NULL COMMENT '场次 game.id',
    user_id      BIGINT      NOT NULL COMMENT '报名人 user.id',
    status       VARCHAR(10) NOT NULL COMMENT '状态：JOINED 已报名 / WAITLIST 候补中 / CANCELLED 已取消',
    queued_at    DATETIME(3) NOT NULL COMMENT '排队时间（精确到毫秒），报名名单和候补名单都按它排序；取消后重新报名会刷新',
    promoted_at  DATETIME    NULL COMMENT '从候补递补为已报名的时间',
    cancelled_at DATETIME    NULL COMMENT '取消时间',
    no_show      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '截止后临时请假（记一次「鸽子」）',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_user (game_id, user_id) COMMENT '一个人在一场里只有一条记录，取消后再报名是更新这一条',
    KEY idx_game_status_queue (game_id, status, queued_at),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '报名记录';


-- ---------------------------------------------------------------------
-- 费用账单：一场一个账单
-- ---------------------------------------------------------------------
CREATE TABLE bill
(
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    game_id    BIGINT   NOT NULL COMMENT '场次 game.id',
    created_by BIGINT   NOT NULL COMMENT '创建人 user.id（组织者）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_id (game_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '费用账单';


-- ---------------------------------------------------------------------
-- 账单费用项，例如「场地费 ¥240」「球费 ¥60」
-- ---------------------------------------------------------------------
CREATE TABLE bill_item
(
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_id    BIGINT      NOT NULL COMMENT '账单 bill.id',
    name       VARCHAR(30) NOT NULL COMMENT '费用名称',
    amount     INT         NOT NULL COMMENT '金额（分）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_bill_id (bill_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账单费用项';


-- ---------------------------------------------------------------------
-- 账单成员：每个人应付多少、付了没有
-- ---------------------------------------------------------------------
CREATE TABLE bill_member
(
    id           BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_id      BIGINT     NOT NULL COMMENT '账单 bill.id',
    user_id      BIGINT     NOT NULL COMMENT '成员 user.id',
    amount       INT        NOT NULL DEFAULT 0 COMMENT '应付金额（分），费用项变化时重新分摊',
    paid         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已付',
    paid_at      DATETIME   NULL COMMENT '付款时间',
    is_organizer TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否组织者（垫付了全部费用，自己不用转账）',
    created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_user (bill_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '账单成员';
