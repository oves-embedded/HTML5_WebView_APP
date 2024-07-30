package com.example.myapplication.activity;

import static com.example.myapplication.constants.RetCode.BLE_DEVICE_NOT_FIND;
import static com.example.myapplication.constants.RetCode.BLE_NOT_SUPPORTED;
import static com.example.myapplication.constants.RetCode.BLE_SERVICE_NOT_INIT;
import static com.example.myapplication.constants.RetCode.METHOD_INVOCATION_EXCEPTION;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.constants.Result;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.EventBusMsg;
import com.example.myapplication.enums.EventBusEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.WebViewJavascriptBridge;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;
import com.huawei.hms.hmsscankit.ScanKitActivity;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WebViewActivity extends AppCompatActivity {

    private BridgeWebView bridgeWebView;

    private WebViewJavascriptBridge jsBridge;


    private LocationManager mLocationManager;
    private LocationListener locationListener;

    private Gson gson = new Gson();


    private int REQUEST_CODE_SCAN_ONE = 999;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        EventBus.getDefault().register(this);
        initService();

        bridgeWebView = findViewById(R.id.bridgeWebView);
        bridgeWebView.getSettings().setBuiltInZoomControls(false); //显示放大缩小 controler
        bridgeWebView.setNetworkAvailable(true);
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
        XXPermissions.with(WebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
            @Override
            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                if (!allGranted) {
                    return;
                }
//                        Toaster.show(String.format(getString(R.string.demo_obtain_permission_success_hint), PermissionNameConvert.getPermissionString(WebViewActivity.this, permissions)));
            }
        });

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

        registerHandler();
    }


    BleService bleService;

    public void initService() {
        Intent intent = new Intent(WebViewActivity.this, BleService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleService = ((BleService.BleServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
        }
    };

    public void registerHandler() {
        //JS 通过 JSBridge 调用 Android
        bridgeWebView.registerHandler("toastMsg", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android
                Toaster.show(data);
                //Android返回给JS的消息
                function.onCallBack(gson.toJson(Result.ok("Android Callback")));
            }
        });

        //JS 通过 JSBridge 调用 Android
        bridgeWebView.registerHandler("startBleScan", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android
                if (bleService == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(WebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (bleService != null) {
                                bleService.stopScan();
                                bleService.startBleScan();
                                function.onCallBack(gson.toJson(Result.ok(true)));
                            } else {
                                function.onCallBack(gson.toJson(Result.ok(BLE_SERVICE_NOT_INIT)));
                            }
//                                    Toaster.show(String.format(getString(R.string.demo_obtain_permission_success_hint), PermissionNameConvert.getPermissionString(WebViewActivity.this, permissions)));
                        }
                    });
                }
            }
        });

        //JS 通过 JSBridge 调用 Android
        bridgeWebView.registerHandler("stopBleScan", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android

                XXPermissions.with(WebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        if (bleService != null) {
                            bleService.stopScan();
                            function.onCallBack(gson.toJson(Result.ok(true)));
                        } else {
                            function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT)));
                        }
                    }
                });
            }
        });


        bridgeWebView.registerHandler("connBleByMacAddress", new BridgeHandler() {
            @Override
            public void handler(String macAddress, CallBackFunction function) {
                //JS传递给Android

                XXPermissions.with(WebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            function.onCallBack(gson.toJson(Result.fail("Android permission error!")));
                            return;
                        }
                        if (bleService != null) {
                            try {
                                BleDeviceUtil bleDeviceUtil = bleService.connectBle(macAddress);
                                if (bleDeviceUtil == null) {
                                    function.onCallBack(gson.toJson(Result.fail(BLE_DEVICE_NOT_FIND)));
                                } else {
                                    function.onCallBack(gson.toJson(Result.ok(true)));
                                }
                            } catch (Exception e) {
                                function.onCallBack(gson.toJson(Result.fail(METHOD_INVOCATION_EXCEPTION.getCode(), e.getMessage())));
                            }
                        } else {
                            function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT)));
                        }
                    }
                });
            }
        });


        bridgeWebView.registerHandler("startQrCodeScan", new BridgeHandler() {
            @Override
            public void handler(String requestCode, CallBackFunction function) {

                XXPermissions.with(WebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        REQUEST_CODE_SCAN_ONE = TextUtils.isEmpty(requestCode) ? 0 : Integer.valueOf(requestCode);
                        //JS传递给Android
                        try {
//                                    int i = ScanUtil.startScan(WebViewActivity.this, REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
//                                    Toaster.show(i);

                            HmsScanAnalyzerOptions hmsScanAnalyzerOptions = new HmsScanAnalyzerOptions.Creator().create();
                            Intent intent = new Intent(WebViewActivity.this, ScanKitActivity.class);
                            if (intent != null) {
                                intent.putExtra("ScanFormatValue", hmsScanAnalyzerOptions.mode);
                            }
                            startActivityForResult(intent, REQUEST_CODE_SCAN_ONE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });


        bridgeWebView.registerHandler("getPhoneBleStatus", new BridgeHandler() {
            @Override
            public void handler(String requestCode, CallBackFunction function) {

                XXPermissions.with(WebViewActivity.this)
//                        .permission(Permission.CAMERA)
//                        .permission(Permission.READ_EXTERNAL_STORAGE)
                        .permission(Permission.READ_MEDIA_IMAGES).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_AUDIO).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                if (!allGranted) {
                                    return;
                                }
                                BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                                if (defaultAdapter != null) {
                                    function.onCallBack(gson.toJson(Result.ok(defaultAdapter.isEnabled())));
                                } else {
                                    function.onCallBack(gson.toJson(Result.fail(BLE_NOT_SUPPORTED)));
                                }
                            }
                        });
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMsg message) {
        if (message.getTagEnum() == EventBusEnum.BLE_FIND) {
            BleDeviceInfo info = (BleDeviceInfo) message.getT();
            bridgeWebView.callHandler("findBleDeviceCallBack", gson.toJson(info), new CallBackFunction() {
                @Override
                public void onCallBack(String data) {

                    Toaster.show(String.format("findBleDeviceCallBack return:[%s]", data));
                }
            });
        }
    }


    @Override
    protected void onDestroy() {
        if (mLocationManager != null && locationListener != null) {
            mLocationManager.removeUpdates(locationListener);
        }

        if (bridgeWebView != null) {
            bridgeWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            bridgeWebView.clearHistory();

            ((ViewGroup) bridgeWebView.getParent()).removeView(bridgeWebView);
            bridgeWebView.destroy();
            bridgeWebView = null;
        }

        EventBus.getDefault().unregister(this);

        super.onDestroy();
    }


    //Activity回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);

            Toaster.show(obj.originalValue);
            if (obj != null) {

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("value", obj.originalValue);
                map.put("requestCode", REQUEST_CODE_SCAN_ONE);

                bridgeWebView.callHandler("scanQrcodeResultCallBack", gson.toJson(Result.ok(map)), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        LogUtil.debug(String.format("scanQrcodeResultCallBack return:[%s]", data));
                    }
                });
            } else {
                bridgeWebView.callHandler("scanQrcodeResultCallBack", gson.toJson(Result.fail()), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        LogUtil.debug(String.format("scanQrcodeResultCallBack return:[%s]", data));
                    }
                });
            }
        }
    }


}
