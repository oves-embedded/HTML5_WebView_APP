package com.example.myapplication.activity;

import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
    private OnBackPressedCallback onBackPressedCallback;

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
        
        // 获取WebSettings并进行配置
        WebSettings settings = bridgeWebView.getSettings();
        
        // 基础设置
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDomStorageEnabled(true);//开启DOM
        
        // 允许网页定位
        settings.setGeolocationEnabled(true);
        // 允许网页弹对话框
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 设置支持javascript
        settings.setJavaScriptEnabled(true);
        
        // ========== WebView 基础配置 ==========
        // 允许访问assets目录（用于加载本地HTML文件和资源）
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // 允许从file:// URL加载资源（JSBridge需要）
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        
        // 设置UserAgent
//        settings.setUserAgentString(settings.getUserAgentString() + "app");
        
        // 初始化返回键处理（使用新的OnBackPressedDispatcher API替代废弃的onBackPressed）
        initBackPressHandler();
    }
    
    /**
     * 初始化返回键处理（使用OnBackPressedDispatcher替代废弃的onBackPressed方法）
     */
    private void initBackPressHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }
    
    /**
     * 处理返回键逻辑（原onBackPressed的业务逻辑）
     */
    private void handleBackPressed() {
        if (bridgeWebView == null) {
            finish();
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
                finish();
            }
        }
        // Handle normal back navigation
        else if (bridgeWebView.canGoBack()) {
            bridgeWebView.goBack();
        }
        // No more pages to go back to
        else {
            finish();
        }
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
                        // 权限授予后不需要重新加载WebView，因为WebView已在BaseWebViewActivity.onCreate()中初始化
                        // initWebView() 和 registerCommMethod() 已在onCreate中按顺序调用完成
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
        initialUrl = "file:///android_asset/webview/index.html";
        bridgeWebView.loadUrl(initialUrl);
//        if (TextUtils.isEmpty(url)) {
//            initialUrl = "file:///android_asset/webview/index.html";
//            bridgeWebView.loadUrl(initialUrl);
//        } else {
//            initialUrl = url;
//            bridgeWebView.loadUrl(initialUrl);
//        }
    }

    public void registerMethod() {
        //JS 通过 JSBridge 调用 Android
    }

    @Override
    protected void onDestroy() {
        // 移除OnBackPressedCallback回调
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }
        
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