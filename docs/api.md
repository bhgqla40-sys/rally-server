# 接口清单

以前端 rally-app 的 `src/api/*.ts`（按模块分文件）和 `src/types/*.ts`（同样按模块分文件，`index.ts` 统一导出）为准。前端已经全部接入后端，不再使用 mock。

## 通用约定

- 所有路径前面都有 `/api`，下表省略
- 返回格式：`{ code, message, data }`，`code === 0` 表示成功
- 登录后请求头带 `Authorization: Bearer <token>`；token 无效或过期返回 **HTTP 401**（前端收到会自动退出登录）
- 时间：ISO 8601 字符串，带时区，例如 `2026-10-31T19:00:00+08:00`
- 金额：「分」为单位的整数，例如 ¥35 = `3500`
- 枚举值：英文大写字符串，和前端 `types/index.ts` 一致，例如 `BADMINTON`、`OPEN`
- 可选字段没有值时不出现在 JSON 里（不是 `null`），对应 TS 里的 `avatar?: string`
- 上传的图片通过 `/api/uploads/...` 访问，不需要登录

## 接口

### 登录注册 & 我的

| 方法 | 路径 | 请求体 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/auth/register` | `{ phone, password, nickname, wxCode? }` | `LoginResult { token, userId, nickname, wxBound? }` | ✅ |
| POST | `/auth/login` | `{ phone, password, wxCode? }` | `LoginResult`（小程序里传 `wxCode` 会顺便绑定微信，`wxBound` 表示有没有绑上） | ✅ |
| POST | `/auth/wx-login` | `{ code }` | `LoginResult { token, userId, nickname, newUser }`（小程序一键登录；新微信自动建账号，`newUser = true`） | ✅ |
| GET | `/users/me` | - | `MyProfile` | ✅ |
| PUT | `/users/me` | `{ nickname, bio? }` | `MyProfile`（bio 不传表示不改，传空字符串表示清空） | ✅ |
| POST | `/users/me/avatar` | `multipart/form-data`，文件字段名 `file` | `MyProfile`（avatar 是新头像地址；JPG/PNG/WEBP/GIF，≤ 2MB） | ✅ |
| GET | `/users/me/signups` | `?scope=UPCOMING\|HISTORY` | `SignupRecord[]`（UPCOMING 按开始时间正序；HISTORY 倒序，最多 50 条） | ✅ |

### 群组

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/groups` | `{ name, sport }` | `Group` | ✅ |
| GET | `/groups/mine` | - | `Group[]` | ✅ |
| GET | `/groups/{id}` | - | `GroupDetail` | ✅ |
| POST | `/groups/join` | `{ code }` | `Group` | ✅ |

#### 群组管理（群主 / 管理员）

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| GET | `/groups/{groupId}/members` | - | `GroupMember[]`（成员名单，带角色和入群时间） | ✅ |
| PUT | `/groups/{groupId}` | `{ name }` | `Group`（改群名，已有场次的群名一起变） | ✅ |
| POST | `/groups/{groupId}/invite-code` | - | `{ inviteCode }`（重新生成，旧码立即失效） | ✅ |
| PUT | `/groups/{groupId}/members/{memberId}` | `{ role: 'ADMIN' \| 'MEMBER' }` | `GroupMember[]`（设置 / 取消管理员） | ✅ |
| DELETE | `/groups/{groupId}/members/{memberId}` | - | `GroupMember[]`（移出群组） | ✅ |

### 固定局 & 场次

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/game-templates` | `GameTemplateForm` | `{ firstGameId }`（群主 / 管理员；同时生成第一场，之后每 10 分钟检查一次自动生成下一场） | ✅ |
| GET | `/groups/{groupId}/game-templates` | - | `GameTemplateBrief[]`（某个群的固定局） | ✅ |
| PUT | `/game-templates/{templateId}` | `{ active }` | `GameTemplateBrief`（暂停 / 恢复，已生成的场次不受影响） | ✅ |
| GET | `/games` | `?filter=ALL\|MINE\|AVAILABLE` | `GameBrief[]`（我所在群里没结束、没取消的场次） | ✅ |
| GET | `/games/{id}` | - | `GameDetail`（登录即可看，方便分享链接） | ✅ |
| POST | `/games/{id}/signup` | - | `GameDetail`（有名额直接报上，满员进候补） | ✅ |
| DELETE | `/games/{id}/signup` | - | `GameDetail`（空出名额时候补第一位自动递补） | ✅ |

#### 场次管理（组织者）

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| DELETE | `/games/{gameId}/signups/{memberId}` | - | `GameDetail`（把人移出报名或候补；空出名额时候补第一位自动递补） | ✅ |
| POST | `/games/{gameId}/cancel` | `{ reason }` | `GameDetail`（取消这一场，开始前才能取消；reason 必填，≤ 100 字） | ✅ |
| PUT | `/games/{gameId}/attendance` | `{ noShowIds }` | `GameDetail`（结束后确认出勤，传没来的人的 id） | ✅ |

#### 权限和状态规则（后端已实现）

| 操作 | 谁能做 | 什么时候能做 | 失败提示 |
| --- | --- | --- | --- |
| 群组管理（改名、邀请码、移出成员） | 群主、管理员 | - | 403「只有群主和管理员可以操作」 |
| 设置 / 取消管理员 | 只有群主 | 不能改群主自己的角色 | 403「只有群主可以设置管理员」/ 400「不能修改群主的角色」 |
| 移出群组 | 群主可移出任何人；管理员只能移出普通成员 | 不能移出群主和自己 | 400「不能移出群主或自己」/ 403「管理员不能移出其他管理员」 |
| 暂停 / 恢复固定局 | 群主、管理员 | - | 403「只有群主和管理员可以操作」 |
| 新建固定局 | 群主、管理员 | 固定费用要填金额；开放报名要早于截止 | 403「只有群主和管理员可以操作」/ 400 |
| 报名 | 登录用户（不在群里会自动入群） | OPEN（开放后、截止前） | 400「报名还没开始」/「报名已截止」/「你已经报过名了」/「名额已满」 |
| 取消报名 / 退出候补 | 自己 | 截止之前 | 400「已过截止时间，不能取消了」 |
| 移出报名 / 候补 | 场次组织者 | 没结束、没取消 | 403「只有组织者可以操作」/ 400「这一场已经结束了」 |
| 取消场次 | 场次组织者 | 开始之前，必须填原因 | 400「请填写取消原因」/「已经开始了，不能取消」 |
| 确认出勤 | 场次组织者 | 结束之后 | 400「结束后才能确认出勤」 |
| 新建账单（打开结算页） | 场次组织者、群主 / 管理员 | 结束之后 | 400「结束后才能结算费用」/ 其他群成员 404「组织者还没开始结算这一场」 |
| 查看账单 | 群成员、账单里的人 | - | 403 |
| 加减费用项、标记已付 | 场次组织者 | - | 403「只有组织者可以操作」/ 400「组织者已经垫付了全部费用，不用标记」 |
| 我已转账 | 账单里的人（不是组织者） | 还没付、金额 > 0 | 400「组织者已经确认收到了」/「还没有要付的费用」 |

### 费用结算

| 方法 | 路径 | 请求体 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| GET | `/games/{gameId}/bill` | - | `Bill`（还没有就新建空账单，只有组织者 / 群管理能建，结束后才能建；成员 = 报名名单 - 鸽子，按报名顺序） | ✅ |
| POST | `/bills/{billId}/items` | `{ name, amount }` | `Bill`（组织者；名称 ≤ 20 字，金额 1 分 ～ 10 万元，最多 20 项；加完重新分摊） | ✅ |
| DELETE | `/bills/{billId}/items/{itemId}` | - | `Bill`（组织者；删完重新分摊） | ✅ |
| PUT | `/bills/{billId}/members/{memberId}` | `{ paid }` | `Bill`（组织者标记已付 / 未付，memberId 是用户 id；会清掉「我已转账」） | ✅ |
| GET | `/users/me/pending-bills` | - | `PendingBill[]`（我的待付款：没付、不是组织者、金额 > 0，最近的在前） | ✅ |
| POST | `/bills/{billId}/claim` | - | 无（成员点「我已转账」，等组织者确认） | ✅ |

#### 微信小程序登录

- `code` / `wxCode` 是小程序里 `uni.login({ provider: 'weixin' })` 拿到的临时凭证，5 分钟有效、只能用一次，每次都要重新拿
- 后端用 AppID + AppSecret 调微信 `code2Session` 换 openid，存在 `user.wx_openid`（唯一索引，一个微信只对应一个账号）
- 一键登录：openid 已绑定 → 直接登录；没绑定 → 自动建账号（昵称「微信用户xxxx」，没有手机号和密码）
- 手机号登录 / 注册时绑定：只有账号还没绑微信、这个微信也没被别的账号用过时才会绑上；绑不上不影响登录
- 错误提示：凭证无效或过期「登录凭证无效或已过期，请重新登录」；后端没配 AppSecret 返回 code 500「服务器还没配置小程序登录，请先用手机号登录」

## 和表的对应关系

| 前端类型 | 表 |
| --- | --- |
| `Member` / `MyProfile` / `SignupRecord` | `user`（统计数字由 `group_member`、`game_signup`、`bill_member` 算出来） |
| `Group` / `GroupDetail` / `GroupMember` | `rally_group` + `group_member` |
| `GameTemplateForm` / `GameTemplateBrief` | `game_template`（前端的 `active` 对应表里的 `enabled`） |
| `GameBrief` / `GameDetail` | `game` + `game_signup`（`noShowIds` 来自 `game_signup.no_show = 1` 的人） |
| `Bill` / `PendingBill` | `bill` + `bill_item` + `bill_member`（`Bill.organizer` 是后端新加的字段，来自 `game.organizer_id`） |

表结构见 `src/main/resources/db/migration/V1__init_schema.sql`。

### 后来补的字段

V1 已执行过，不能再改，新字段用新的迁移文件加：

| 前端字段 | 列 | 迁移 |
| --- | --- | --- |
| `MyProfile.bio` | `user.bio` | V2 ✅ |
| `GameDetail.cancelReason` | `game.cancel_reason varchar(100) null` | V3 ✅ |
| `GameDetail.attendanceConfirmed` | `game.attendance_confirmed tinyint(1) not null default 0` | V3 ✅ |
| `BillMember.claimedAt` / `PendingBill.claimedAt` | `bill_member.claimed_at datetime null` | V4 ✅ |
