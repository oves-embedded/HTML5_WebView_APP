package com.example.myapplication.activity;

import static com.example.myapplication.constants.RetCode.BLE_DEVICE_NOT_FIND;
import static com.example.myapplication.constants.RetCode.BLE_IS_NOT_ENABLED;
import static com.example.myapplication.constants.RetCode.BLE_MAC_ADDRESS_NOT_MATCH;
import static com.example.myapplication.constants.RetCode.BLE_NOT_CONNECTED;
import static com.example.myapplication.constants.RetCode.BLE_NOT_SUPPORTED;
import static com.example.myapplication.constants.RetCode.BLE_SERVICE_NOT_INIT;
import static com.example.myapplication.constants.RetCode.FAIL;
import static com.example.myapplication.constants.RetCode.METHOD_INVOCATION_EXCEPTION;
import static com.example.myapplication.constants.RetCode.PARAMETER_ERROR;
import static com.example.myapplication.constants.RetCode.PERMISSION_ERROR;
import static com.example.myapplication.constants.RetCode.RUNTIME_EXCEPTION;
import static com.example.myapplication.constants.RetCode.USER_CANCELED_OPERATION;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.constants.Result;
import com.example.myapplication.constants.RetCode;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.entity.js.BleData;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.enums.EventBusEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.thread.ThreadPool;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.ImageUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class BaseWebViewActivity extends AppCompatActivity {

    private Gson gson = new Gson();
    public BridgeWebView bridgeWebView;
    public BleService bleService;
    private int REQUEST_CODE_SCAN_ONE = 999;
    private int CHOOSE_PIC_REQUEST_CODE = 111;
    private Uri takePhotoUri;

    private boolean SERVICE_BIND = false;

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
            SERVICE_BIND = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
            SERVICE_BIND = false;
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
                    if (b) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
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
                    if (bleDeviceUtil != null && bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
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


        bridgeWebView.registerHandler("writeBleCharacteristic", new BridgeHandler() {
            String serviceUUID = null, characteristicUUID = null, value = null, macAddress = null;

            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    serviceUUID = jsonObject.getString("serviceUUID");
                    characteristicUUID = jsonObject.getString("characteristicUUID");
                    value = jsonObject.getString("value");
                    macAddress = jsonObject.getString("macAddress");
                } catch (Exception e) {
                    function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), e.getMessage(), false)));
                    return;
                }

                if (TextUtils.isEmpty(serviceUUID) || TextUtils.isEmpty(characteristicUUID) || TextUtils.isEmpty(value) || TextUtils.isEmpty(macAddress)) {
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                    return;
                }
                //JS传递给Android
                if (bleService != null) {
                    BleDeviceUtil bleDeviceUtil = bleService.getBleDeviceUtil();
                    if (bleDeviceUtil != null && bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
                        if (bleDeviceUtil.getBluetoothDevice().getAddress().equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
                            return;
                        }
                        ThreadPool.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Result<Void> voidResult = null;
                                try {
                                    voidResult = bleDeviceUtil.writeCharacteristic(serviceUUID, characteristicUUID, value);
                                } catch (Exception e) {
                                    function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), e.getMessage(), false)));
                                }
                                Result<Void> finalVoidResult = voidResult;
                                BaseWebViewActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (finalVoidResult.success()) {
                                            function.onCallBack(gson.toJson(Result.ok(true)));
                                        } else {
                                            function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), finalVoidResult.getRespDesc(), false)));
                                        }
                                    }
                                });
                            }
                        });

                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_NOT_CONNECTED, false)));
                    }
                } else {
                    function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                }

            }
        });


        bridgeWebView.registerHandler("readBleCharacteristic", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                String serviceUUID = null, characteristicUUID = null, macAddress = null;
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    serviceUUID = jsonObject.getString("serviceUUID");
                    characteristicUUID = jsonObject.getString("characteristicUUID");
                    macAddress = jsonObject.getString("macAddress");
                    if (TextUtils.isEmpty(serviceUUID) || TextUtils.isEmpty(characteristicUUID)) {
                        function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                        return;
                    }
                } catch (Exception e) {
                    function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), e.getMessage(), false)));
                    return;
                }
                //JS传递给Android
                if (bleService != null) {
                    BleDeviceUtil bleDeviceUtil = bleService.getBleDeviceUtil();
                    if (bleDeviceUtil != null && bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {

                        if (bleDeviceUtil.getBluetoothDevice().getAddress().equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
                            return;
                        }

                        String finalServiceUUID = serviceUUID;
                        String finalCharacteristicUUID = characteristicUUID;
                        ThreadPool.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Result<CharacteristicDomain> characteristicDomainResult = bleDeviceUtil.readCharacteristic(finalServiceUUID, finalCharacteristicUUID);
                                BaseWebViewActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (characteristicDomainResult.success()) {
                                            function.onCallBack(gson.toJson(Result.ok(characteristicDomainResult.getRespData())));
                                        } else {
                                            function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), characteristicDomainResult.getRespDesc(), false)));
                                        }
                                    }
                                });
                            }
                        });

                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_NOT_CONNECTED, false)));
                    }
                } else {
                    function.onCallBack(gson.toJson(Result.fail(BLE_SERVICE_NOT_INIT, false)));
                }
            }
        });


        bridgeWebView.registerHandler("startQrCodeScan", new

                BridgeHandler() {
                    @Override
                    public void handler(String requestCode, CallBackFunction function) {

                        XXPermissions.with(BaseWebViewActivity.this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                if (!allGranted) {
                                    function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
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


        bridgeWebView.registerHandler("getPhoneBleStatus", new

                BridgeHandler() {
                    @Override
                    public void handler(String requestCode, CallBackFunction function) {

                        XXPermissions.with(BaseWebViewActivity.this)
//                        .permission(Permission.CAMERA)
//                        .permission(Permission.READ_EXTERNAL_STORAGE)
                                .permission(Permission.READ_MEDIA_IMAGES).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_AUDIO).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                                    @Override
                                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                        if (!allGranted) {
                                            function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
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

        bridgeWebView.registerHandler("jump2MainActivity", new
                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {
                        try {
                            MainConfig mainConfig = gson.fromJson(data, MainConfig.class);
                            function.onCallBack(gson.toJson(Result.ok(true)));
                            Intent intent = new Intent(BaseWebViewActivity.this, MainActivity.class);
                            intent.putExtra("data", gson.toJson(mainConfig));
                            startActivity(intent);

                            function.onCallBack(gson.toJson(Result.ok(true)));
                        } catch (Exception e) {
                            e.printStackTrace();
                            function.onCallBack(gson.toJson(Result.fail(RUNTIME_EXCEPTION.getCode(), e.getMessage(), false)));
                        }
                    }
                });


        bridgeWebView.registerHandler("callPhone", new
                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {

                        XXPermissions.with(BaseWebViewActivity.this)
                                .permission(Permission.CALL_PHONE)
                                .interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                                    @Override
                                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                        if (!allGranted) {
                                            function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
                                            return;
                                        }
                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                        Uri uri = Uri.parse("tel:" + data);
                                        intent.setData(uri);
                                        startActivity(intent);
                                        function.onCallBack(gson.toJson(Result.ok(true)));
                                    }
                                });


                    }
                });
        bridgeWebView.registerHandler("sendSms", new
                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String phone = jsonObject.getString("phone");
                            String content = jsonObject.getString("content");
                            XXPermissions.with(BaseWebViewActivity.this)
                                    .permission(Permission.SEND_SMS)
                                    .interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                                        @Override
                                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                            if (!allGranted) {
                                                function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
                                                return;
                                            }
                                            Uri smsToUri = Uri.parse("smsto:" + (TextUtils.isEmpty(phone) ? "" : phone));
                                            Intent intent = new Intent(Intent.ACTION_SENDTO, smsToUri);
                                            intent.putExtra("sms_body", TextUtils.isEmpty(content) ? "" : content);
                                            startActivity(intent);
                                        }
                                    });
                        } catch (Exception e) {
                            function.onCallBack(gson.toJson(Result.fail(RUNTIME_EXCEPTION.getCode(), e.getMessage(), false)));
                        }
                    }
                });


        bridgeWebView.registerHandler("choosePicture", new

                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {
                        CHOOSE_PIC_REQUEST_CODE = Integer.parseInt(data);
                        XXPermissions.with(BaseWebViewActivity.this).permission(Permission.MANAGE_EXTERNAL_STORAGE).permission(Permission.CAMERA).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                if (!allGranted) {
                                    function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
                                    return;
                                }
                                showBottomSheetDialog();
                                function.onCallBack(gson.toJson(Result.ok(true)));
                            }
                        });
                    }
                });
    }


    public void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(BaseWebViewActivity.this, R.style.BottomSheetDialog);
        //不传第二个参数
        //BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        // 底部弹出的布局
        View bottomView = LayoutInflater.from(BaseWebViewActivity.this).inflate(R.layout.bottom_sheet_layout, null);
        bottomSheetDialog.setContentView(bottomView);

        TextView tvPhoto = (TextView) bottomView.findViewById(R.id.choose_photo);
        TextView tvCamera = (TextView) bottomView.findViewById(R.id.choose_camera);
        TextView tvCancel = (TextView) bottomView.findViewById(R.id.cancel);
        tvPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //参数一:对应的数据的URI 参数二:使用该函数表示要查找文件的MIME类型
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, CHOOSE_PIC_REQUEST_CODE);
            }
        });

        tvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到相机
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                /***
                 * 需要说明一下，以下操作使用照相机拍照，拍照后的图片会存放在相册中的
                 * 这里使用的这种方式有一个好处就是获取的图片是拍照后的原图
                 * 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图不清晰
                 */
                ContentValues values = new ContentValues();
                takePhotoUri = BaseWebViewActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoUri);
                startActivityForResult(intent, CHOOSE_PIC_REQUEST_CODE);
                bottomSheetDialog.dismiss();
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });
        //设置点击dialog外部不消失
        //bottomSheetDialog.setCanceledOnTouchOutside(false);
        bottomSheetDialog.show();
        bottomSheetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Map<String, Object> map = new HashMap<>();
                map.put("requestCode", CHOOSE_PIC_REQUEST_CODE);
                bridgeWebView.callHandler("choosePictureCallBack", gson.toJson(Result.fail(USER_CANCELED_OPERATION, map)), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {

                    }
                });
            }
        });
    }


    private String getImagePath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bleService != null && SERVICE_BIND) {
                bleService.unbindService(serviceConnection);
                SERVICE_BIND = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventBus.getDefault().unregister(this);
    }

    //Activity回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_PIC_REQUEST_CODE) {
            Map<String, Object> map = new HashMap<>();
            //用户没有进行有效的设置操作，返回
            if (resultCode == RESULT_CANCELED) {
                bridgeWebView.callHandler("choosePictureCallBack", gson.toJson(Result.fail(USER_CANCELED_OPERATION, map)), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            } else if (resultCode == RESULT_OK) {

                ThreadPool.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        String imgBase64 = null;
                        if (data != null) {
                            Uri selectedImage = data.getData();
                            imgBase64 = ImageUtil.imageToBase64(getImagePath(selectedImage));
                            LogUtil.info(imgBase64);
                        } else {
                            Toaster.show("takePhotoUri is null?" + takePhotoUri == null);
                            if (takePhotoUri != null) {
                                imgBase64 = ImageUtil.imageToBase64(getImagePath(takePhotoUri));
                                LogUtil.info(imgBase64);
                                takePhotoUri = null;
                            }
                        }

                        map.put("requestCode", CHOOSE_PIC_REQUEST_CODE);
                        String finalImgBase6 = imgBase64;
                        BaseWebViewActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (finalImgBase6 != null) {
                                    map.put("base64Str", finalImgBase6);
                                    bridgeWebView.callHandler("choosePictureCallBack", gson.toJson(map), new CallBackFunction() {
                                        @Override
                                        public void onCallBack(String data) {
                                        }
                                    });
                                } else {
                                    bridgeWebView.callHandler("choosePictureCallBack", gson.toJson(Result.fail(USER_CANCELED_OPERATION, map)), new CallBackFunction() {
                                        @Override
                                        public void onCallBack(String data) {
                                        }
                                    });
                                }
                            }
                        });
                    }
                });

            }
        }

        if (requestCode == REQUEST_CODE_SCAN_ONE && resultCode == RESULT_OK) {
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