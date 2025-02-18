package com.example.myapplication.activity;

import android.location.LocationListener;
import android.location.LocationManager;
import android.text.TextUtils;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.example.myapplication.R;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.WebViewJavascriptBridge;
import com.google.gson.Gson;
import com.hjq.toast.Toaster;

public class LaunchActivity extends BaseWebViewActivity {

    private WebViewJavascriptBridge jsBridge;

    private Gson gson = new Gson();


    @Override
    public void initPermissions() {

    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_webview);
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

    @Override
    public void registerMethod() {

    }
}
