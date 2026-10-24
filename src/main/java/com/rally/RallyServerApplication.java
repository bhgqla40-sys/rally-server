package com.rally;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 启动类 ≈ 前端的 main.ts 入口。
 * 运行这个类的 main 方法，整个后端服务就启动了。
 *
 * 注解 @SpringBootApplication 会自动扫描 com.rally 包及其子包下的所有组件
 * （Controller、Service、配置类等），所以新写的类都要放在 com.rally 下面。
 */
@SpringBootApplication
public class RallyServerApplication {

    public static void main(String[] args) {
        // 统一使用北京时间。服务器（尤其是 Docker 里）默认是 UTC，不设置的话时间会差 8 小时
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(RallyServerApplication.class, args);
    }
}
