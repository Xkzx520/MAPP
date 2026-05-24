package com.example.mapp.api;

import com.example.mapp.BuildConfig;

/**
 * 后端地址配置：
 * - Android 模拟器访问电脑：默认 10.0.2.2
 * - 真机调试：在项目根目录 local.properties 增加一行 API_HOST=你的电脑局域网IP
 *   （CMD 执行 ipconfig 查看 IPv4，手机与电脑需同一 WiFi）
 */
public final class ApiConfig {

    private ApiConfig() {
    }

    public static String getBaseUrl() {
        return "http://" + BuildConfig.API_HOST + ":" + BuildConfig.API_PORT + "/";
    }
}
