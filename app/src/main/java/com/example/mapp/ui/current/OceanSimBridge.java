package com.example.mapp.ui.current;

import android.webkit.JavascriptInterface;
import com.example.mapp.api.ApiConfig;

/** WebView 与本地 API 地址桥接 */
public class OceanSimBridge {

    @JavascriptInterface
    public String getApiBase() {
        return ApiConfig.getBaseUrl();
    }
}
