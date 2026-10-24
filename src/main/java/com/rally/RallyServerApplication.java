package com.rally;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        SpringApplication.run(RallyServerApplication.class, args);
    }
}
