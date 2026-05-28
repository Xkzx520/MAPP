package com.example.mapp.ui.current;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mapp.R;
import com.example.mapp.api.ApiConfig;

public class CurrentSimFragment extends Fragment {

    private WebView webView;
    private ProgressBar progressBar;
    private boolean pageLoaded;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_sim, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = view.findViewById(R.id.web_sim);
        progressBar = view.findViewById(R.id.progress_bar);

        if (!pageLoaded) {
            setupWebView();
            progressBar.setVisibility(View.VISIBLE);
            webView.loadUrl("file:///android_asset/ocean_sim/index.html");
            pageLoaded = true;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setOffscreenPreRaster(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.addJavascriptInterface(new OceanSimBridge(requireContext()), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isAdded() || progressBar == null) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                String base = ApiConfig.getBaseUrl().replace("'", "\\'");
                view.evaluateJavascript("window.setApiBase('" + base + "');", null);
                view.evaluateJavascript("window.loadMapFromAndroid && window.loadMapFromAndroid();", null);
                view.postDelayed(() -> view.evaluateJavascript("window.resize && window.resize();", null), 120);
                if (isResumed()) {
                    setSimRunning(true);
                }
            }
        });
    }

    private void setSimRunning(boolean running) {
        if (webView == null) {
            return;
        }
        webView.evaluateJavascript("window.setSimRunning(" + running + ");", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
            setSimRunning(true);
        }
    }

    @Override
    public void onPause() {
        setSimRunning(false);
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        setSimRunning(false);
        if (webView != null) {
            webView.stopLoading();
            webView.onPause();
        }
        super.onDestroyView();
    }
}
