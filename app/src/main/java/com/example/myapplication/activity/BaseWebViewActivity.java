package com.example.myapplication.activity;

import static com.example.myapplication.constants.RetCode.BLE_DEVICE_NOT_FIND;
import static com.example.myapplication.constants.RetCode.BLE_IS_NOT_ENABLED;
import static com.example.myapplication.constants.RetCode.BLE_MAC_ADDRESS_NOT_MATCH;
import static com.example.myapplication.constants.RetCode.BLE_NOT_CONNECTED;
import static com.example.myapplication.constants.RetCode.BLE_NOT_SUPPORTED;
import static com.example.myapplication.constants.RetCode.BLE_SERVICE_NOT_INIT;
import static com.example.myapplication.constants.RetCode.METHOD_INVOCATION_EXCEPTION;
import static com.example.myapplication.constants.RetCode.RUNTIME_EXCEPTION;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.constants.Result;
import com.example.myapplication.constants.RetCode;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.entity.js.BleData;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.enums.EventBusEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.thread.ThreadPool;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseWebViewActivity extends AppCompatActivity {

    private Gson gson = new Gson();
    public BridgeWebView bridgeWebView;
    public BleService bleService;
    private int REQUEST_CODE_SCAN_ONE = 999;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initWebView();
        registerCommMethod();
        registerMethod();
        registerCommMethod();
        initPermissions();
        //init service
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
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


    public abstract void initPermissions();

    public abstract void initView();

    public abstract void initWebView();

    public abstract void registerMethod();


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMsg message) {
        if (message.getTagEnum() == EventBusEnum.BLE_FIND) {
            BleDeviceInfo info = (BleDeviceInfo) message.getT();
            bridgeWebView.callHandler("findBleDeviceCallBack", gson.toJson(info), new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                }
            });
        }
        if (message.getTagEnum() == EventBusEnum.BLE_CONNECT) {
            String macAddress = (String) message.getT();
            bridgeWebView.callHandler("bleConnectSuccessCallBack", macAddress, new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                }
            });
        }
        if (message.getTagEnum() == EventBusEnum.BLE_CONNECT_FAIL) {
            String macAddress = (String) message.getT();
            bridgeWebView.callHandler("bleConnectFailCallBack", macAddress, new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                }
            });
        }
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
                if (bleService == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(BaseWebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (bleService != null) {
                                bleService.stopScan();
                                bleService.startBleScan(data == null ? "" : data);
                                function.onCallBack(gson.toJson(Result.ok(true)));
                            } else {
                                function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                            }
//                                    Toaster.show(String.format(getString(R.string.demo_obtain_permission_success_hint), PermissionNameConvert.getPermissionString(BaseWebViewActivity.this, permissions)));
                        }
                    });
                }
            }
        });


        bridgeWebView.registerHandler("getScannedDevices", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                //JS传递给Android
                if (bleService == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(BaseWebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (bleService != null) {
                                Map<String, BleDeviceInfo> bleDeviceInfoMap = bleService.getBleDeviceInfoMap();
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
                XXPermissions.with(BaseWebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        if (bleService != null) {
                            bleService.stopScan();
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
                if (bleService != null) {
                    boolean b = bleService.getBleDeviceInfoMap().containsKey(macAddress);
                    if(b){
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    }else{
                        function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH,false)));
                    }
                    ThreadPool.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BleDeviceUtil bleDeviceUtil = bleService.connectBle(macAddress);
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
                if (bleService != null) {
                    BleDeviceUtil bleDeviceUtil = bleService.getBleDeviceUtil();
                    String address = bleDeviceUtil.getBluetoothDevice().getAddress();
                    if (bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
                        if (address.equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.ok(true)));
                            ThreadPool.getExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    Map<String, ServicesPropertiesDomain> resultMap = bleDeviceUtil.initData();
                                    LogUtil.error("resultMap:" + gson.toJson(resultMap));
                                    BaseWebViewActivity.this.runOnUiThread(new Runnable() {
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

                XXPermissions.with(BaseWebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                        REQUEST_CODE_SCAN_ONE = TextUtils.isEmpty(requestCode) ? 0 : Integer.valueOf(requestCode);
                        //JS传递给Android
                        try {
//                                    int i = ScanUtil.startScan(BaseWebViewActivity.this, REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
//                                    Toaster.show(i);

                            HmsScanAnalyzerOptions hmsScanAnalyzerOptions = new HmsScanAnalyzerOptions.Creator().create();
                            Intent intent = new Intent(BaseWebViewActivity.this, ScanKitActivity.class);
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

                XXPermissions.with(BaseWebViewActivity.this)
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
                    Intent intent = new Intent(BaseWebViewActivity.this, MainActivity.class);
                    intent.putExtra("data", gson.toJson(mainConfig));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(RUNTIME_EXCEPTION.getCode(), e.getMessage(), false)));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleService != null) {
            bleService.unbindService(serviceConnection);
        }
        EventBus.getDefault().unregister(this);
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
