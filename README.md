# rally-server

Rally 组局助手的后端服务，前端项目是 [rally-app](https://github.com/bhgqla40-sys/rally-app)。

## 技术栈

| 用途 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5 |
| 构建工具 | Maven（项目自带 Maven Wrapper，不用单独安装） |
| 数据库 | MySQL 8.4（Docker 运行） |
| 数据库访问 | MyBatis-Plus |
| 表结构管理 | Flyway（启动时自动建表 / 改表） |
| 缓存 | Redis 7.4（Docker 运行） |

## 环境准备

- **JDK 21**：终端里运行 `java -version`，看到 `21.x.x` 就可以了
- **Docker Desktop**：用来跑 MySQL 和 Redis，不用在电脑上单独安装它们

## 启动数据库（MySQL + Redis）

```bash
cp .env.example .env      # 第一次需要：复制配置模板，按需改密码（已有 .env 就跳过）
docker compose up -d      # 启动（第一次会下载镜像）
docker compose ps         # 两个容器的 STATUS 都显示 healthy 就是正常
```

| 服务 | 地址 | 账号 / 密码 |
| --- | --- | --- |
| MySQL | `localhost:3306`，库名 `rally` | 见 `.env` 里的 `MYSQL_USER` / `MYSQL_PASSWORD` |
| Redis | `localhost:6379` | 见 `.env` 里的 `REDIS_PASSWORD` |

停止用 `docker compose down`，数据不会丢。

## 启动

> 先确认数据库已经启动（`docker compose ps` 两个都是 healthy），否则后端会启动失败。
> 第一次启动时 Flyway 会自动在 `rally` 库里建好所有表。

**方式一：IntelliJ IDEA（推荐）**

1. IDEA 打开 `rally-server` 文件夹，等右下角 Maven 依赖下载完
2. 打开 `src/main/java/com/rally/RallyServerApplication.java`，点 `main` 方法左边的绿色 ▶ 运行

**方式二：命令行**

```bash
./mvnw spring-boot:run
```

看到 `Tomcat started on port 8080` 和 `Started RallyServerApplication` 就是启动成功了。

## 试一下接口

```bash
# 健康检查（mysql 和 redis 都显示 UP 就是连上了）
curl http://localhost:8080/api/health

# GET 带参数
curl "http://localhost:8080/api/demo/hello?name=Rally"

# POST JSON + 参数校验（把 name 改成空字符串试试校验失败的效果）
curl -X POST http://localhost:8080/api/demo/echo \
  -H "Content-Type: application/json" \
  -d '{"name":"周末羽毛球","maxPeople":8}'

# 业务异常
curl http://localhost:8080/api/demo/fail
```

所有接口统一返回：

```json
{ "code": 0, "message": "成功", "data": {} }
```

`code` 为 `0` 表示成功，其他值表示失败，`message` 可以直接提示给用户。完整的返回码见 `ResultCode.java`。

## 常用命令

| 命令 | 作用 | 类比前端 |
| --- | --- | --- |
| `./mvnw spring-boot:run` | 启动开发服务 | `npm run dev` |
| `./mvnw test` | 跑测试 | `npm run test` |
| `./mvnw clean package` | 打包成 `target/rally-server-0.0.1-SNAPSHOT.jar` | `npm run build` |
| `java -jar target/rally-server-0.0.1-SNAPSHOT.jar` | 运行打包好的 jar | `npm run preview` |

## 项目结构

```
rally-server
├── pom.xml                         # 依赖和构建配置（≈ package.json）
├── docker-compose.yml              # 本地 MySQL + Redis
├── .env.example                    # docker-compose 配置模板（真正的 .env 不提交）
├── mvnw / mvnw.cmd                 # Maven Wrapper 脚本（≈ npx，自动下载 Maven）
├── .mvn/wrapper/                   # Maven Wrapper 的配置
├── docs/api.md                     # 接口清单（和前端 rally-app 对齐）
└── src
    ├── main
    │   ├── java/com/rally
    │   │   ├── RallyServerApplication.java   # 启动入口（≈ main.ts）
    │   │   ├── common/                       # 公共代码：统一返回格式、异常处理
    │   │   │   ├── Result.java
    │   │   │   ├── ResultCode.java
    │   │   │   ├── BusinessException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── UserContext.java          # 当前登录用户是谁
    │   │   ├── config/                       # 配置类：跨域、登录拦截器注册
    │   │   ├── interceptor/                  # 登录拦截器：校验 token（≈ 路由守卫）
    │   │   ├── controller/                   # 接口层：定义 URL，收参数、返回结果
    │   │   ├── service/                      # 业务层：真正的业务逻辑
    │   │   ├── mapper/                       # 数据层：读写数据库
    │   │   ├── entity/                       # 实体类：和数据库表一一对应
    │   │   ├── dto/                          # 接口的请求体 / 返回数据（和前端类型对应）
    │   │   └── util/                         # 工具类：JWT 等
    │   └── resources
    │       ├── application.yml               # 主配置（≈ .env）
    │       ├── application-dev.yml           # 开发环境配置（≈ .env.development）：数据库、Redis 地址
    │       └── db/migration/                 # 建表 / 改表 SQL，Flyway 按版本号自动执行
    │           └── V1__init_schema.sql
    └── test                                  # 测试代码
```

一个请求的流转顺序：

```
登录拦截器（校验 token） → controller（接收请求） → service（业务逻辑） → mapper（读写数据库） → MySQL
```

以注册为例：`AuthController.register` → `AuthService.register`（检查手机号、加密密码）→ `UserMapper.insert` → `user` 表。

## 路线

- [x] Spring Boot 基础骨架
- [x] Docker 运行 MySQL + Redis
- [x] 接入数据库（MyBatis-Plus + Flyway），设计表结构
- [x] 用户注册登录（JWT），H5 用手机号 + 密码
- [ ] 微信小程序登录
- [x] 群组：创建、我的群组、群组详情、邀请码加入
- [ ] 群组管理：成员名单、改名、重置邀请码、设置管理员、移出成员
- [ ] 固定局规则 + 场次：发布、列表、详情
- [ ] 报名 / 取消 / 候补自动递补（并发控制）
- [ ] AA 记账
- [ ] 部署上线（云服务器 + Docker + Nginx + HTTPS）
