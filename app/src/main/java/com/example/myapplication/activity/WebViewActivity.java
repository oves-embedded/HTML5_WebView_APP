package com.example.myapplication.activity;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.WebViewJavascriptBridge;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class WebViewActivity extends BaseWebViewActivity {


    private Gson gson = new Gson();

    private int REQUEST_CODE_SCAN_ONE = 999;
    @Override
    public void initView() {
        setContentView(R.layout.activity_webview);
        // Ensure cache directory exists
        createWebViewCacheDir();
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
    public void initWebView() {
        bridgeWebView = findViewById(R.id.bridgeWebView);

        if (bridgeWebView != null) {
            // Configure WebView settings
            WebSettings settings = bridgeWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setBuiltInZoomControls(false);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);

            // Configure cache
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
//            settings.setAppCacheEnabled(true);
//            settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());

            // Set database path
            settings.setDatabaseEnabled(true);
            settings.setDatabasePath(getApplicationContext().getDir("databases", Context.MODE_PRIVATE).getPath());

            bridgeWebView.setWebViewClient(new BridgeWebViewClient(bridgeWebView) {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    registerMethod();
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    Log.e(TAG, "WebView error: " + error.getDescription());
                }
            });

            // Load URL
            String url = getIntent().getStringExtra("url");
            if (TextUtils.isEmpty(url)) {
                url = "file:///android_asset/webview/index.html";
            }
            bridgeWebView.loadUrl(url);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request necessary permissions
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 1001);
        }
    }

    private void sendInitialMessage() {
        if (bridgeWebView != null) {
            bridgeWebView.callHandler("print", "Bridge initialized from Android", new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                    Log.d(TAG, "Bridge initialization response: " + data);
                }
            });
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
//                            Toast.makeText(MainActivity.this, "Location permission is required for this feature.", Toast.LENGTH_SHORT).show();
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
    public void registerMethod() {
        if (bridgeWebView != null) {
            // Register your bridge methods here
            bridgeWebView.registerHandler("androidMethod", new BridgeHandler() {
                @Override
                public void handler(String data, CallBackFunction function) {
                    // Handle calls from JavaScript
                    Log.d(TAG, "Received from JS: " + data);
                    function.onCallBack("Response from Android");
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (bridgeWebView != null) {
            bridgeWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            bridgeWebView.clearHistory();
            ((ViewGroup) bridgeWebView.getParent()).removeView(bridgeWebView);
            bridgeWebView.destroy();
            bridgeWebView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Allow navigation back within the WebView
        if (bridgeWebView != null && bridgeWebView.canGoBack()) {
            bridgeWebView.goBack();
        } else {
            // Exit the activity if there are no pages to go back to
            super.onBackPressed();
        }
    }

}