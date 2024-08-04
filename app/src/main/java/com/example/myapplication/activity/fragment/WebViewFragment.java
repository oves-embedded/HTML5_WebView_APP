package com.example.myapplication.activity.fragment;

import static com.example.myapplication.constants.RetCode.BLE_IS_NOT_ENABLED;
import static com.example.myapplication.constants.RetCode.BLE_MAC_ADDRESS_NOT_MATCH;
import static com.example.myapplication.constants.RetCode.BLE_NOT_CONNECTED;
import static com.example.myapplication.constants.RetCode.BLE_NOT_SUPPORTED;
import static com.example.myapplication.constants.RetCode.BLE_SERVICE_NOT_INIT;
import static com.example.myapplication.constants.RetCode.RUNTIME_EXCEPTION;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.activity.BaseWebViewActivity;
import com.example.myapplication.constants.Result;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.js.BleData;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.thread.ThreadPool;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;
import com.huawei.hms.hmsscankit.ScanKitActivity;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class WebViewFragment extends Fragment {
    private BridgeWebView bridgeWebView;

    private String contentUrl;

    private int REQUEST_CODE_SCAN_ONE = 999;

    private Gson gson = new Gson();

    public WebViewFragment(String url) {
        this.contentUrl = url;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        bridgeWebView = view.findViewById(R.id.bridgeWebView);
        initWebView();

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStart(owner);
            }

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onResume(owner);
            }
        });
        return view;
    }


    public BridgeWebView getBridgeWebView() {
        return bridgeWebView;
    }

    public void initWebView() {
        bridgeWebView.getSettings().setBuiltInZoomControls(false); //显示放大缩小 controler
        bridgeWebView.setNetworkAvailable(true);
        bridgeWebView.setWebViewClient(new BridgeWebViewClient(bridgeWebView) {
            // 修复 页面还没加载完成，注册代码还没初始完成，就调用了callHandle
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });
//        bridgeWebView.loadUrl(contentUrl);
        bridgeWebView.loadUrl("file:///android_asset/webview/index.html");//h5地址

        registerCommMethod();
    }


    public void registerCommMethod() {
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
        bridgeWebView.registerHandler("startBleScan", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android
                if (((MainActivity)getActivity()).getBleService() == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (((MainActivity)getActivity()).getBleService() != null) {
                                ((MainActivity)getActivity()).getBleService().stopScan();
                                ((MainActivity)getActivity()).getBleService().startBleScan(data == null ? "" : data);
                                function.onCallBack(gson.toJson(Result.ok(true)));
                            } else {
                                function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                            }
//                                    Toaster.show(String.format(getString(R.string.demo_obtain_permission_success_hint), PermissionNameConvert.getPermissionString(getActivity(), permissions)));
                        }
                    });
                }
            }
        });


        bridgeWebView.registerHandler("getScannedDevices", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android
                if (((MainActivity)getActivity()).getBleService() == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (((MainActivity)getActivity()).getBleService() != null) {
                                Map<String, BleDeviceInfo> bleDeviceInfoMap = ((MainActivity)getActivity()).getBleService().getBleDeviceInfoMap();
                                function.onCallBack(gson.toJson(Result.ok(gson.toJson(bleDeviceInfoMap.values()))));
                            } else {
                                function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                            }
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
                XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        if (((MainActivity)getActivity()).getBleService() != null) {
                            ((MainActivity)getActivity()).getBleService().stopScan();
                            function.onCallBack(gson.toJson(Result.ok(true)));
                        } else {
                            function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                        }
                    }
                });
            }
        });

        bridgeWebView.registerHandler("connBleByMacAddress", new BridgeHandler() {
            @Override
            public void handler(String macAddress, CallBackFunction function) {
                //JS传递给Android
                BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                if (defaultAdapter != null) {
                    if (!defaultAdapter.isEnabled()) {
                        function.onCallBack(gson.toJson(Result.fail(BLE_IS_NOT_ENABLED, false)));
                        return;
                    }
                } else {
                    function.onCallBack(gson.toJson(Result.fail(BLE_NOT_SUPPORTED, false)));
                    return;
                }
                if (((MainActivity)getActivity()).getBleService() != null) {
                    boolean b = ((MainActivity)getActivity()).getBleService().getBleDeviceInfoMap().containsKey(macAddress);
                    if (b) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
                    }
                    ThreadPool.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BleDeviceUtil bleDeviceUtil = ((MainActivity)getActivity()).getBleService().connectBle(macAddress);
                            } catch (Exception e) {
                                e.printStackTrace();
                                bridgeWebView.callHandler("print", e.getMessage(), new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                    }
                                });
                            }
                        }
                    });
                } else {
                    function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                }
            }
        });

        bridgeWebView.registerHandler("initBleData", new BridgeHandler() {
            @Override
            public void handler(String macAddress, CallBackFunction function) {
                //JS传递给Android
                if (((MainActivity)getActivity()).getBleService() != null) {
                    BleDeviceUtil bleDeviceUtil = ((MainActivity)getActivity()).getBleService().getBleDeviceUtil();
                    String address = bleDeviceUtil.getBluetoothDevice().getAddress();
                    if (bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
                        if (address.equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.ok(true)));
                            ThreadPool.getExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    Map<String, ServicesPropertiesDomain> resultMap = bleDeviceUtil.initData();
                                    LogUtil.error("resultMap:" + gson.toJson(resultMap));
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            BleData bleData = new BleData(bleDeviceUtil.getBluetoothDevice().getAddress(), new ArrayList<>(resultMap.values()));
                                            bridgeWebView.callHandler("bleInitDataCallBack", gson.toJson(bleData), new CallBackFunction() {
                                                @Override
                                                public void onCallBack(String data) {

                                                }
                                            });
                                            ServicesPropertiesDomain remove = bleData.getDataList().remove(0);
                                            LogUtil.error("resultMap:" + gson.toJson(remove));
                                        }
                                    });
                                }
                            });
                        } else {
                            function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
                        }
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_NOT_CONNECTED, false)));
                    }
                } else {
                    function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                }

            }
        });

//        bridgeWebView.registerHandler("readBleCharacteristic", new BridgeHandler() {
//            @Override
//            public void handler(String data, CallBackFunction function) {
//
//
//
//                //JS传递给Android
//                if (bleService != null) {
//                    BleDeviceUtil bleDeviceUtil = bleService.getBleDeviceUtil();
//                    String address = bleDeviceUtil.getBluetoothDevice().getAddress();
//                    if (bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
//                        if (address.equals(macAddress)) {
//                            function.onCallBack(gson.toJson(Result.ok(true)));
//                            ThreadPool.getExecutor().execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Map<String, ServicesPropertiesDomain> resultMap = bleDeviceUtil.readCharacteristic();
//                                    bridgeWebView.callHandler("bleInitDataCallBack", macAddress, new CallBackFunction() {
//                                        @Override
//                                        public void onCallBack(String data) {
//
//                                        }
//                                    });
//                                }
//                            });
//                        } else {
//                            function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
//                        }
//                    } else {
//                        function.onCallBack(gson.toJson(Result.fail(BLE_NOT_CONNECTED, false)));
//                    }
//                } else {
//                    function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
//                }
//            }
//        });


        bridgeWebView.registerHandler("startQrCodeScan", new BridgeHandler() {
            @Override
            public void handler(String requestCode, CallBackFunction function) {

                XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        REQUEST_CODE_SCAN_ONE = TextUtils.isEmpty(requestCode) ? 0 : Integer.valueOf(requestCode);
                        //JS传递给Android
                        try {
//                                    int i = ScanUtil.startScan(getActivity(), REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
//                                    Toaster.show(i);

                            HmsScanAnalyzerOptions hmsScanAnalyzerOptions = new HmsScanAnalyzerOptions.Creator().create();
                            Intent intent = new Intent(getActivity(), ScanKitActivity.class);
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

                XXPermissions.with(getActivity())
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
                                    function.onCallBack(gson.toJson(Result.fail(BLE_NOT_SUPPORTED, false)));
                                }
                            }
                        });
            }
        });

        bridgeWebView.registerHandler("jump2MainActivity", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    MainConfig mainConfig = gson.fromJson(data, MainConfig.class);
                    function.onCallBack(gson.toJson(Result.ok(true)));
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("data", gson.toJson(mainConfig));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(RUNTIME_EXCEPTION.getCode(), e.getMessage(), false)));
                }
            }
        });
    }


}
