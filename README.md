# rally-server

Rally 组局助手的后端服务，前端项目是 [rally-app](https://github.com/bhgqla40-sys/rally-app)。

## 技术栈

| 用途 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5 |
| 构建工具 | Maven（项目自带 Maven Wrapper，不用单独安装） |
| 数据库 | MySQL（Docker 运行，下一步接入） |

## 环境准备

只需要 **JDK 21**。终端里运行 `java -version`，看到 `21.x.x` 就可以了。

## 启动

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
# 健康检查
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
├── mvnw / mvnw.cmd                 # Maven Wrapper 脚本（≈ npx，自动下载 Maven）
├── .mvn/wrapper/                   # Maven Wrapper 的配置
└── src
    ├── main
    │   ├── java/com/rally
    │   │   ├── RallyServerApplication.java   # 启动入口（≈ main.ts）
    │   │   ├── common/                       # 公共代码：统一返回格式、异常处理
    │   │   │   ├── Result.java
    │   │   │   ├── ResultCode.java
    │   │   │   ├── BusinessException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── config/                       # 配置类：跨域等
    │   │   │   └── CorsConfig.java
    │   │   └── controller/                   # 接口层：定义 URL 和处理请求
    │   │       ├── HealthController.java
    │   │       └── DemoController.java       # 学习示例，之后可删
    │   └── resources
    │       ├── application.yml               # 主配置（≈ .env）
    │       └── application-dev.yml           # 开发环境配置（≈ .env.development）
    └── test                                  # 测试代码
```

后面加业务时会逐步加上这几层（一个请求的流转顺序）：

```
controller（接收请求） → service（业务逻辑） → mapper（读写数据库） → MySQL
```

## 路线

- [x] Spring Boot 基础骨架
- [ ] Docker 运行 MySQL，接入数据库
- [ ] 用户登录（JWT）
- [ ] 组局相关业务接口
- [ ] 部署上线
