package com.example.myapplication.activity;

import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;

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

import java.util.List;

public class WebViewActivity extends BaseWebViewActivity {


    private Gson gson = new Gson();

    private int REQUEST_CODE_SCAN_ONE = 999;


    @Override
    public void initView() {
        setContentView(R.layout.activity_webview);
//        bridgeWebView.setHorizontalScrollBarEnabled(false);
//        bridgeWebView.setVerticalScrollBarEnabled(false);
//        bridgeWebView.getSettings().setUseWideViewPort(true);
//        bridgeWebView.getSettings().setLoadWithOverviewMode(true);
//        bridgeWebView.getSettings().setDomStorageEnabled(true);//开启DOM

        // 允许网页定位
//        bridgeWebView.getSettings().setGeolocationEnabled(true);
//        // 允许网页弹对话框
//        bridgeWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
//        // 加快网页加载完成的速度，等页面完成再加载图片
//        bridgeWebView.getSettings().setLoadsImagesAutomatically(true);
//        // 设置支持javascript// 本地 DOM 存储（解决加载某些网页出现白板现象）
//        bridgeWebView.getSettings().setJavaScriptEnabled(true);
        // 设置UserAgent
//        bridgeWebView.getSettings().setUserAgentString(bridgeWebView.getSettings().getUserAgentString() + "app");
    }

    @Override
    public void initPermissions() {
        XXPermissions.with(this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
            @Override
            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                if (!allGranted) {
                    return;
                }
            }
        });
    }

    @Override
    public void initWebView() {
        bridgeWebView = findViewById(R.id.bridgeWebView);
        bridgeWebView.getSettings().setBuiltInZoomControls(false); //显示放大缩小 controler
        bridgeWebView.setNetworkAvailable(true);

        bridgeWebView.setWebViewClient(new BridgeWebViewClient(bridgeWebView) {
            // 修复 页面还没加载完成，注册代码还没初始完成，就调用了callHandle
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
        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            bridgeWebView.loadUrl("file:///android_asset/webview/index.html");//h5地址
        } else {
            bridgeWebView.loadUrl(url);
        }
    }




    public void registerMethod() {
        //JS 通过 JSBridge 调用 Android
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bridgeWebView != null) {
            bridgeWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            bridgeWebView.clearHistory();

            ((ViewGroup) bridgeWebView.getParent()).removeView(bridgeWebView);
            bridgeWebView.destroy();
            bridgeWebView = null;
        }
    }

}
