# 接口清单

以前端 rally-app 的 `src/api/*.ts`（按模块分文件）和 `src/types/*.ts`（同样按模块分文件，`index.ts` 统一导出）为准。前端现在用 mock，后端每完成一个接口，前端把对应函数里的 mock 换成注释里的 `http` 调用即可。

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
| POST | `/auth/register` | `{ phone, password, nickname }` | `LoginResult { token, userId, nickname }` | ✅ |
| POST | `/auth/login` | `{ phone, password }` | `LoginResult` | ✅ |
| GET | `/users/me` | - | `MyProfile` | ✅ |
| PUT | `/users/me` | `{ nickname, bio? }` | `MyProfile`（bio 不传表示不改，传空字符串表示清空） | ✅ |
| POST | `/users/me/avatar` | `multipart/form-data`，文件字段名 `file` | `MyProfile`（avatar 是新头像地址；JPG/PNG/WEBP/GIF，≤ 2MB） | ✅ |
| GET | `/users/me/signups` | `?scope=UPCOMING\|HISTORY` | `SignupRecord[]`（我的报名记录，依赖场次模块） | ⬜ |

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
| POST | `/game-templates` | `GameTemplateForm` | `{ firstGameId }` | ⬜ |
| GET | `/groups/{groupId}/game-templates` | - | `GameTemplateBrief[]`（某个群的固定局） | ✅ |
| PUT | `/game-templates/{templateId}` | `{ active }` | `GameTemplateBrief`（暂停 / 恢复，已生成的场次不受影响） | ✅ |
| GET | `/games` | `?filter=ALL\|MINE\|AVAILABLE` | `GameBrief[]` | ⬜ |
| GET | `/games/{id}` | - | `GameDetail` | ⬜ |
| POST | `/games/{id}/signup` | - | `GameDetail`（有名额直接报上，满员进候补） | ⬜ |
| DELETE | `/games/{id}/signup` | - | `GameDetail`（空出名额时候补第一位自动递补） | ⬜ |

#### 场次管理（组织者）

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| DELETE | `/games/{gameId}/signups/{memberId}` | - | `GameDetail`（把人移出报名或候补；空出名额时候补第一位自动递补） | ⬜ |
| POST | `/games/{gameId}/cancel` | `{ reason }` | `GameDetail`（取消这一场，开始前才能取消） | ⬜ |
| PUT | `/games/{gameId}/attendance` | `{ noShowIds }` | `GameDetail`（结束后确认出勤，传没来的人的 id） | ⬜ |

#### 权限和状态规则（前端 mock 里的校验，后端要照做）

| 操作 | 谁能做 | 什么时候能做 | 失败提示 |
| --- | --- | --- | --- |
| 群组管理（改名、邀请码、移出成员） | 群主、管理员 | - | 403「只有群主和管理员可以操作」 |
| 设置 / 取消管理员 | 只有群主 | 不能改群主自己的角色 | 403「只有群主可以设置管理员」/ 400「不能修改群主的角色」 |
| 移出群组 | 群主可移出任何人；管理员只能移出普通成员 | 不能移出群主和自己 | 400「不能移出群主或自己」/ 403「管理员不能移出其他管理员」 |
| 暂停 / 恢复固定局 | 群主、管理员 | - | 403「只有群主和管理员可以操作」 |
| 移出报名 / 候补 | 场次组织者 | 没结束、没取消 | 403「只有组织者可以操作」/ 400「这一场已经结束了」 |
| 取消场次 | 场次组织者 | 开始之前 | 400「已经开始了，不能取消」 |
| 确认出勤 | 场次组织者 | 结束之后 | 400「结束后才能确认出勤」 |

### 费用结算

| 方法 | 路径 | 请求体 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| GET | `/games/{gameId}/bill` | - | `Bill`（还没有就新建空账单；成员 = 报名名单 - 鸽子） | ⬜ |
| POST | `/bills/{billId}/items` | `{ name, amount }` | `Bill` | ⬜ |
| DELETE | `/bills/{billId}/items/{itemId}` | - | `Bill` | ⬜ |
| PUT | `/bills/{billId}/members/{memberId}` | `{ paid }` | `Bill`（组织者标记已付 / 未付） | ⬜ |
| GET | `/users/me/pending-bills` | - | `PendingBill[]`（我的待付款） | ⬜ |
| POST | `/bills/{billId}/claim` | - | 无（成员点「我已转账」，等组织者确认） | ⬜ |

## 和表的对应关系

| 前端类型 | 表 |
| --- | --- |
| `Member` / `MyProfile` / `SignupRecord` | `user`（统计数字由 `group_member`、`game_signup`、`bill_member` 算出来） |
| `Group` / `GroupDetail` / `GroupMember` | `rally_group` + `group_member` |
| `GameTemplateForm` / `GameTemplateBrief` | `game_template`（前端的 `active` 对应表里的 `enabled`） |
| `GameBrief` / `GameDetail` | `game` + `game_signup`（`noShowIds` 来自 `game_signup.no_show = 1` 的人） |
| `Bill` / `PendingBill` | `bill` + `bill_item` + `bill_member` |

表结构见 `src/main/resources/db/migration/V1__init_schema.sql`。

### V1 里还没有、要在 V2 补的字段

前端后来新加的字段，做到对应模块时写一个 `V2__xxx.sql` 加上（V1 已执行过，不能再改）：

| 前端字段 | 要加的列 |
| --- | --- |
| `GameDetail.cancelReason` | `game.cancel_reason VARCHAR(100) NULL` |
| `GameDetail.attendanceConfirmed` | `game.attendance_confirmed TINYINT(1) NOT NULL DEFAULT 0` |
| `BillMember.claimedAt` / `PendingBill.claimedAt` | `bill_member.claimed_at DATETIME NULL` |
