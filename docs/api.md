# 接口清单

以前端 rally-app 的 `src/api/*.ts` 和 `src/types/index.ts` 为准。前端现在用 mock，后端每完成一个接口，前端把对应函数里的 mock 换成注释里的 `http` 调用即可。

## 通用约定

- 所有路径前面都有 `/api`，下表省略
- 返回格式：`{ code, message, data }`，`code === 0` 表示成功
- 登录后请求头带 `Authorization: Bearer <token>`；token 无效或过期返回 **HTTP 401**（前端收到会自动退出登录）
- 时间：ISO 8601 字符串，带时区，例如 `2026-10-31T19:00:00+08:00`
- 金额：「分」为单位的整数，例如 ¥35 = `3500`
- 枚举值：英文大写字符串，和前端 `types/index.ts` 一致，例如 `BADMINTON`、`OPEN`

## 接口

### 登录注册

| 方法 | 路径 | 请求体 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/auth/register` | `{ phone, password, nickname }` | `LoginResult { token, userId, nickname }` | ✅ |
| POST | `/auth/login` | `{ phone, password }` | `LoginResult` | ✅ |
| GET | `/users/me` | - | `MyProfile` | ✅ |

### 群组

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/groups` | `{ name, sport }` | `Group` | ⬜ |
| GET | `/groups/mine` | - | `Group[]` | ⬜ |
| GET | `/groups/{id}` | - | `GroupDetail` | ⬜ |
| POST | `/groups/join` | `{ code }` | `Group` | ⬜ |

### 固定局 & 场次

| 方法 | 路径 | 请求体 / 参数 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| POST | `/game-templates` | `GameTemplateForm` | `{ firstGameId }` | ⬜ |
| GET | `/games` | `?filter=ALL\|MINE\|AVAILABLE` | `GameBrief[]` | ⬜ |
| GET | `/games/{id}` | - | `GameDetail` | ⬜ |
| POST | `/games/{id}/signup` | - | `GameDetail`（有名额直接报上，满员进候补） | ⬜ |
| DELETE | `/games/{id}/signup` | - | `GameDetail`（空出名额时候补第一位自动递补） | ⬜ |

### 费用结算

| 方法 | 路径 | 请求体 | 返回 data | 状态 |
| --- | --- | --- | --- | --- |
| GET | `/games/{gameId}/bill` | - | `Bill`（还没有就新建空账单） | ⬜ |
| POST | `/bills/{billId}/items` | `{ name, amount }` | `Bill` | ⬜ |
| DELETE | `/bills/{billId}/items/{itemId}` | - | `Bill` | ⬜ |
| PUT | `/bills/{billId}/members/{memberId}` | `{ paid }` | `Bill` | ⬜ |

## 和表的对应关系

| 前端类型 | 表 |
| --- | --- |
| `Member` / `MyProfile` | `user`（统计数字由 `group_member`、`game_signup`、`bill_member` 算出来） |
| `Group` / `GroupDetail` | `rally_group` + `group_member` |
| `GameTemplateForm` | `game_template` |
| `GameBrief` / `GameDetail` | `game` + `game_signup` |
| `Bill` | `bill` + `bill_item` + `bill_member` |

表结构见 `src/main/resources/db/migration/V1__init_schema.sql`。
