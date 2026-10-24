package com.rally.controller;

import com.rally.common.BusinessException;
import com.rally.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习用的示例接口，演示 GET 参数、POST JSON + 参数校验、业务异常三种常见写法。
 * 前端可以先拿这几个接口练手联调，等真正的业务接口写好后，这个文件可以删掉。
 */
@RestController
@RequestMapping("/demo") // 这个类下所有接口的路径前缀
public class DemoController {

    /**
     * GET 请求 + URL 参数
     * 访问：GET /api/demo/hello?name=小明
     */
    @GetMapping("/hello")
    public Result<String> hello(@RequestParam(defaultValue = "Rally") String name) {
        return Result.success("你好，" + name + "！后端已经跑起来了");
    }

    /**
     * POST 请求 + JSON 请求体 + 参数校验
     * 访问：POST /api/demo/echo
     * 请求体：{ "name": "周末羽毛球", "maxPeople": 8 }
     *
     * 参数前的 @Valid 表示按 EchoRequest 里的注解规则校验，不通过会自动返回 code=400 和错误提示
     */
    @PostMapping("/echo")
    public Result<EchoRequest> echo(@Valid @RequestBody EchoRequest request) {
        return Result.success(request);
    }

    /**
     * 演示业务异常：直接 throw，前端会收到 { code: 1000, message: "...", data: null }
     * 访问：GET /api/demo/fail
     */
    @GetMapping("/fail")
    public Result<Void> fail() {
        throw new BusinessException("这是一个演示用的业务错误");
    }

    /**
     * 请求体的结构（类似前端的 interface）。
     * record 是 Java 的简洁写法：自动生成构造方法和取值方法，适合只用来装数据的类。
     */
    public record EchoRequest(
            @NotBlank(message = "活动名称不能为空")
            @Size(max = 30, message = "活动名称最多 30 个字")
            String name,

            @NotNull(message = "人数上限不能为空")
            @Min(value = 2, message = "人数上限至少 2 人")
            @Max(value = 100, message = "人数上限最多 100 人")
            Integer maxPeople
    ) {
    }
}
