package com.example.mapp.ui.current;

import android.content.Context;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import com.example.mapp.api.ApiConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** WebView 与本地 API / 地图资源桥接 */
public class OceanSimBridge {

    private final Context appContext;

    public OceanSimBridge(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @JavascriptInterface
    public String getApiBase() {
        return ApiConfig.getBaseUrl();
    }

    /** 从 assets 读取世界地图 PNG，供 WebView 内 JS 以 data URL 加载 */
    @JavascriptInterface
    public String getWorldMapBase64() {
        try (InputStream in = appContext.getAssets().open("ocean_sim/world_map.png")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        }
    }
}
