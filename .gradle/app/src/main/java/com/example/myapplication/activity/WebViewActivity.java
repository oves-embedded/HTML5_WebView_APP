package com.example.myapplication.activity;

import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.WebViewJavascriptBridge;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.io.File;
import java.util.List;

public class WebViewActivity extends BaseWebViewActivity {

    private Gson gson = new Gson();
    private int REQUEST_CODE_SCAN_ONE = 999;
    private static final int EXIT_INTERVAL = 2000; // 2 seconds
    private long lastBackPress = 0;
    private String initialUrl;

    @Override
    public void initView() {
        setContentView(R.layout.activity_webview);
        // Ensure cache directory exists
        createWebViewCacheDir();
        bridgeWebView = findViewById(R.id.bridgeWebView); // Ensure this is added
        if (bridgeWebView == null) {
            throw new NullPointerException("bridgeWebView is null. Check layout file.");
        }

        bridgeWebView.setHorizontalScrollBarEnabled(false);
//        bridgeWebView.setVerticalScrollBarEnabled(false);
        bridgeWebView.getSettings().setUseWideViewPort(true);
        bridgeWebView.getSettings().setLoadWithOverviewMode(true);
        bridgeWebView.getSettings().setDomStorageEnabled(true);//开启DOM
        bridgeWebView.getSettings().setDatabaseEnabled(true);
        // 允许网页定位
        bridgeWebView.getSettings().setGeolocationEnabled(true);
//        // 允许网页弹对话框
        bridgeWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
//        // 加快网页加载完成的速度，等页面完成再加载图片
//        bridgeWebView.getSettings().setLoadsImagesAutomatically(true);
//        // 设置支持javascript// 本地 DOM 存储（解决加载某些网页出现白板现象）
        bridgeWebView.getSettings().setJavaScriptEnabled(true);

        bridgeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

        });
        // 设置UserAgent
//        bridgeWebView.getSettings().setUserAgentString(bridgeWebView.getSettings().getUserAgentString() + "app");
    }

    private void createWebViewCacheDir() {
        File cacheDir = new File(getApplicationContext().getCacheDir(), "WebView");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File codeCacheDir = new File(cacheDir, "Code Cache");
        if (!codeCacheDir.exists()) {
            codeCacheDir.mkdirs();
        }
    }

    @Override
    public void initPermissions() {
        XXPermissions.with(this)
                .permission(Permission.BLUETOOTH_SCAN)
                .permission(Permission.BLUETOOTH_CONNECT)
                .permission(Permission.BLUETOOTH_ADVERTISE)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .permission(Permission.ACCESS_COARSE_LOCATION)
                .interceptor(new PermissionInterceptor()) // Optional for custom logic
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            // All permissions granted: Initialize or reload the WebView
                            initWebView();
                        } else {
                            // Show an alert or handle partial permission denial
//                            Toast.makeText(MainActivity.class, "Location permission is required for this feature.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        // Handle denied permissions
//                        Toast.makeText(MainActivity.this, "Permissions Denied. Enable them in settings.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public void initWebView() {
        // Since bridgeWebView is already initialized in initView(), we just configure additional settings
        bridgeWebView.getSettings().setBuiltInZoomControls(false);
        bridgeWebView.setNetworkAvailable(true);

        bridgeWebView.setWebViewClient(new BridgeWebViewClient(bridgeWebView) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                bridgeWebView.callHandler("print", "hello! this from android!", new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        Toaster.show(data);
                    }
                });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        // Load the URL passed from FirstActivity
        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            bridgeWebView.loadUrl("file:///android_asset/webview/index.html");
        } else {
            bridgeWebView.loadUrl(url);
        }
    }

    public void registerMethod() {
        //JS 通过 JSBridge 调用 Android
    }

    @Override
    public void onBackPressed() {
        if (bridgeWebView == null) {
            super.onBackPressed();
            return;
        }

        // Check if current URL is the initial URL
        String currentUrl = bridgeWebView.getUrl();
        if (currentUrl != null && currentUrl.equals(initialUrl)) {
            // We're at the initial URL, implement double-back to exit
            if (System.currentTimeMillis() - lastBackPress > EXIT_INTERVAL) {
                Toaster.show("Press back again to exit");
                lastBackPress = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }
        }
        // Handle normal back navigation
        else if (bridgeWebView.canGoBack()) {
            bridgeWebView.goBack();
        }
        // No more pages to go back to
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (bridgeWebView != null) {
            // Clear WebView data
            bridgeWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            bridgeWebView.clearHistory();
            bridgeWebView.clearCache(true);

            // Remove WebView from parent and destroy
            ((ViewGroup) bridgeWebView.getParent()).removeView(bridgeWebView);
            bridgeWebView.destroy();
            bridgeWebView = null;
        }
        super.onDestroy();
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (bridgeWebView != null) {
//            bridgeWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
//            bridgeWebView.clearHistory();
//
//            ((ViewGroup) bridgeWebView.getParent()).removeView(bridgeWebView);
//            bridgeWebView.destroy();
//            bridgeWebView = null;
//        }
//    }

}