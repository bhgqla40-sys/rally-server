package com.rally.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.common.BusinessException;
import com.rally.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * 调用微信服务器的接口（≈ 前端调第三方 API 的封装）。
 *
 * 小程序登录流程：
 * 1. 小程序里 uni.login() 拿到一个临时 code（5 分钟有效，只能用一次）
 * 2. 把 code 发给我们的后端
 * 3. 后端带上 AppID + AppSecret 调微信的 code2Session，换到用户的 openid（这个用户在这个小程序里的唯一 id）
 *
 * AppSecret 只能放在后端，绝不能写进小程序代码里（小程序代码可以被反编译）。
 */
@Slf4j
@Component
public class WxClient {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final String appId;
    private final String appSecret;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WxClient(@Value("${rally.wx.app-id:}") String appId,
                    @Value("${rally.wx.app-secret:}") String appSecret,
                    ObjectMapper objectMapper) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.objectMapper = objectMapper;
        // 设置超时：微信接口卡住时不要让登录请求一直挂着
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** 用小程序 uni.login() 拿到的 code 换 openid */
    public String code2Openid(String code) {
        if (appId.isBlank() || appSecret.isBlank()) {
            log.error("没有配置小程序 AppID / AppSecret（rally.wx.app-id / rally.wx.app-secret）");
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "服务器还没配置小程序登录，请先用手机号登录");
        }
        String body;
        try {
            // 微信这个接口返回的 Content-Type 是 text/plain，所以先按字符串拿，再自己解析 JSON
            body = restClient.get()
                    .uri(CODE2SESSION_URL, appId, appSecret, code)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("调用微信 code2Session 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "连不上微信服务器，请稍后再试");
        }
        return parseOpenid(objectMapper, body);
    }

    /**
     * 解析 code2Session 的返回：
     * 成功 { "openid": "...", "session_key": "...", "unionid": "..." }
     * 失败 { "errcode": 40029, "errmsg": "invalid code" }
     * session_key 是解密用户敏感数据用的，我们暂时用不到，也不要打印到日志里。
     */
    static String parseOpenid(ObjectMapper objectMapper, String body) {
        JsonNode json;
        try {
            json = objectMapper.readTree(body == null ? "" : body);
        } catch (Exception e) {
            log.warn("微信 code2Session 返回的不是 JSON: {}", body);
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "微信登录失败，请稍后再试");
        }
        int errcode = json.path("errcode").asInt(0);
        if (errcode != 0) {
            log.warn("微信 code2Session 返回错误 errcode={} errmsg={}", errcode, json.path("errmsg").asText());
            throw new BusinessException(errorMessage(errcode));
        }
        String openid = json.path("openid").asText("");
        if (openid.isBlank()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "微信登录失败，请稍后再试");
        }
        return openid;
    }

    /** 常见错误码转成用户能看懂的提示，其他的统一提示重试 */
    static String errorMessage(int errcode) {
        return switch (errcode) {
            case 40029, 40163 -> "登录凭证无效或已过期，请重新登录";
            case 45011 -> "登录太频繁了，请稍后再试";
            case 40013, 40125, 40226 -> "服务器的小程序配置有误，请先用手机号登录";
            case -1 -> "微信服务器繁忙，请稍后再试";
            default -> "微信登录失败（" + errcode + "），请稍后再试";
        };
    }
}
