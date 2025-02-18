package com.example.myapplication.activity.fragment;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.example.myapplication.constants.RetCode.BLE_IS_NOT_ENABLED;
import static com.example.myapplication.constants.RetCode.BLE_MAC_ADDRESS_NOT_MATCH;
import static com.example.myapplication.constants.RetCode.BLE_NOT_CONNECTED;
import static com.example.myapplication.constants.RetCode.BLE_NOT_SUPPORTED;
import static com.example.myapplication.constants.RetCode.BLE_SERVICE_NOT_INIT;
import static com.example.myapplication.constants.RetCode.FAIL;
import static com.example.myapplication.constants.RetCode.MQTT_CONNECT_FAIL;
import static com.example.myapplication.constants.RetCode.MQTT_CURRENT_NOT_CONNECTED;
import static com.example.myapplication.constants.RetCode.PARAMETER_ERROR;
import static com.example.myapplication.constants.RetCode.PERMISSION_ERROR;
import static com.example.myapplication.constants.RetCode.RUNTIME_EXCEPTION;
import static com.example.myapplication.constants.RetCode.USER_CANCELED_OPERATION;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.activity.BaseWebViewActivity;
import com.example.myapplication.activity.OcrActivity;
import com.example.myapplication.callback.InitBleDataCallBack;
import com.example.myapplication.callback.InitBleServiceDataCallBack;
import com.example.myapplication.constants.Result;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.MqttConfig;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.js.CharacteristicDto;
import com.example.myapplication.entity.js.ServicesPropertiesDto;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.thread.ThreadPool;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.ImageUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.MqttClientUtil;
import com.example.myapplication.util.SharedPreferencesUtils;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
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
import com.orhanobut.logger.Logger;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebViewFragment extends Fragment {
    private BridgeWebView bridgeWebView;
    private String contentUrl;
    private int REQUEST_CODE_SCAN_ONE = 999;
    private int CHOOSE_PIC_REQUEST_CODE = 111;

    private int REQUEST_CODE_OCR = 222;
    private Uri takePhotoUri;

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
        bridgeWebView.loadUrl(contentUrl);
//        bridgeWebView.loadUrl("file:///android_asset/webview/index.html");//h5地址
        bridgeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

        });
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
                if (((MainActivity) getActivity()).getBleService() == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (((MainActivity) getActivity()).getBleService() != null) {
                                ((MainActivity) getActivity()).getBleService().stopScan();
                                ((MainActivity) getActivity()).getBleService().startBleScan(data == null ? "" : data);
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
                if (((MainActivity) getActivity()).getBleService() == null) {
                    function.onCallBack(gson.toJson(Result.fail("Android bleService is not available!")));
                } else {
                    XXPermissions.with(getActivity()).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                return;
                            }
                            if (((MainActivity) getActivity()).getBleService() != null) {
                                Map<String, BleDeviceInfo> bleDeviceInfoMap = ((MainActivity) getActivity()).getBleService().getBleDeviceInfoMap();
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
                        if (((MainActivity) getActivity()).getBleService() != null) {
                            ((MainActivity) getActivity()).getBleService().stopScan();
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
                if (((MainActivity) getActivity()).getBleService() != null) {
                    boolean b = ((MainActivity) getActivity()).getBleService().getBleDeviceInfoMap().containsKey(macAddress);
                    if (b) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(BLE_MAC_ADDRESS_NOT_MATCH, false)));
                    }
                    ThreadPool.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BleDeviceUtil bleDeviceUtil = ((MainActivity) getActivity()).getBleService().connectBle(macAddress);
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
        bridgeWebView.registerHandler("initServiceBleData", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                String serviceName = null, macAddress = null;
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    serviceName = jsonObject.getString("serviceName");
                    macAddress = jsonObject.getString("macAddress");
                } catch (Exception e) {
                    function.onCallBack(gson.toJson(Result.fail(FAIL.getCode(), e.getMessage(), false)));
                    return;
                }

                //JS传递给Android
                if (((MainActivity) getActivity()).getBleService() != null) {
                    BleDeviceUtil bleDeviceUtil = ((MainActivity) getActivity()).getBleService().getBleDeviceUtil();
                    if (bleDeviceUtil != null && bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
                        String address = bleDeviceUtil.getBluetoothDevice().getAddress();
                        if (address.equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.ok(true)));
                            String finalServiceName = serviceName;
                            ThreadPool.getExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    bleDeviceUtil.initService(finalServiceName, new InitBleServiceDataCallBack() {
                                        @Override
                                        public void onProgress(int total, int progress, CharacteristicDomain domain) {
                                            Map<String, Object> map = new HashMap<>();
                                            map.put("total", total);
                                            map.put("progress", progress);
                                            map.put("data", domain);
                                            map.put("macAddress", bleDeviceUtil.getBluetoothDevice().getAddress());
                                            bridgeWebView.callHandler("bleInitServiceDataOnProgressCallBack", gson.toJson(map), new CallBackFunction() {
                                                @Override
                                                public void onCallBack(String data) {

                                                }
                                            });
                                        }

                                        @Override
                                        public void onComplete(ServicesPropertiesDomain domain) {
                                            ServicesPropertiesDto servicesPropertiesDto = new ServicesPropertiesDto();
                                            servicesPropertiesDto.setUuid(domain.getUuid());
                                            servicesPropertiesDto.setServiceNameEnum(domain.getServiceNameEnum());
                                            servicesPropertiesDto.setServiceProperty(domain.getServiceProperty());

                                            Collection<CharacteristicDomain> values1 = domain.getCharacterMap().values();
                                            for (CharacteristicDomain characteristicDomain : values1) {
                                                CharacteristicDto characteristicDto = new CharacteristicDto();
                                                characteristicDto.setDesc(characteristicDomain.getDesc());
                                                characteristicDto.setUuid(characteristicDomain.getUuid());
                                                characteristicDto.setDescriptors(new ArrayList<>(characteristicDomain.getDescMap().values()));
                                                characteristicDto.setName(characteristicDomain.getName());
                                                characteristicDto.setServiceUuid(characteristicDomain.getServiceUuid());
                                                characteristicDto.setProperties(characteristicDomain.getProperties());
                                                characteristicDto.setRealVal(characteristicDomain.getRealVal());
                                                characteristicDto.setValType(characteristicDomain.getValType());
                                                characteristicDto.setValues(characteristicDomain.getValues());
                                                if (servicesPropertiesDto.getCharacteristicList() == null) {
                                                    servicesPropertiesDto.setCharacteristicList(new ArrayList<>());
                                                }
                                                servicesPropertiesDto.getCharacteristicList().add(characteristicDto);
                                            }

                                            bridgeWebView.callHandler("bleInitServiceDataOnCompleteCallBack", gson.toJson(servicesPropertiesDto), new CallBackFunction() {
                                                @Override
                                                public void onCallBack(String data) {

                                                }
                                            });

                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            bridgeWebView.callHandler("bleInitServiceDataFailureCallBack", error, new CallBackFunction() {
                                                @Override
                                                public void onCallBack(String data) {

                                                }
                                            });
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

        bridgeWebView.registerHandler("initBleData", new BridgeHandler() {
            @Override
            public void handler(String macAddress, CallBackFunction function) {
                //JS传递给Android
                if (((MainActivity) getActivity()).getBleService() != null) {
                    BleDeviceUtil bleDeviceUtil = ((MainActivity) getActivity()).getBleService().getBleDeviceUtil();
                    if (bleDeviceUtil != null && bleDeviceUtil.getConnectStat() == DeviceConnStatEnum.CONNECTED) {
                        String address = bleDeviceUtil.getBluetoothDevice().getAddress();
                        if (address.equals(macAddress)) {
                            function.onCallBack(gson.toJson(Result.ok(true)));
                            ThreadPool.getExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    bleDeviceUtil.initData(new InitBleDataCallBack() {
                                        @Override
                                        public void onProgress(int total, int progress, CharacteristicDomain domain) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Map<String, Object> map = new HashMap<>();
                                                    map.put("total", total);
                                                    map.put("progress", progress);
                                                    map.put("data", domain);
                                                    map.put("macAddress", bleDeviceUtil.getBluetoothDevice().getAddress());
                                                    bridgeWebView.callHandler("bleInitDataOnProgressCallBack", gson.toJson(map), new CallBackFunction() {
                                                        @Override
                                                        public void onCallBack(String data) {

                                                        }
                                                    });
                                                }
                                            });
                                        }

                                        @Override
                                        public void onComplete(Map<String, ServicesPropertiesDomain> data) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Collection<ServicesPropertiesDomain> values = data.values();
                                                    List<ServicesPropertiesDto> dataList = null;
                                                    for (ServicesPropertiesDomain domain : values) {
                                                        ServicesPropertiesDto servicesPropertiesDto = new ServicesPropertiesDto();
                                                        servicesPropertiesDto.setUuid(domain.getUuid());
                                                        servicesPropertiesDto.setServiceNameEnum(domain.getServiceNameEnum());
                                                        servicesPropertiesDto.setServiceProperty(domain.getServiceProperty());
                                                        if (dataList == null)
                                                            dataList = new ArrayList<>();
                                                        dataList.add(servicesPropertiesDto);
                                                        Collection<CharacteristicDomain> values1 = domain.getCharacterMap().values();
                                                        for (CharacteristicDomain characteristicDomain : values1) {
                                                            CharacteristicDto characteristicDto = new CharacteristicDto();
                                                            characteristicDto.setDesc(characteristicDomain.getDesc());
                                                            characteristicDto.setUuid(characteristicDomain.getUuid());
                                                            characteristicDto.setDescriptors(new ArrayList<>(characteristicDomain.getDescMap().values()));
                                                            characteristicDto.setName(characteristicDomain.getName());
                                                            characteristicDto.setServiceUuid(characteristicDomain.getServiceUuid());
                                                            characteristicDto.setProperties(characteristicDomain.getProperties());
                                                            characteristicDto.setRealVal(characteristicDomain.getRealVal());
                                                            characteristicDto.setValType(characteristicDomain.getValType());
                                                            characteristicDto.setValues(characteristicDomain.getValues());
                                                            if (servicesPropertiesDto.getCharacteristicList() == null) {
                                                                servicesPropertiesDto.setCharacteristicList(new ArrayList<>());
                                                            }
                                                            servicesPropertiesDto.getCharacteristicList().add(characteristicDto);
                                                        }
                                                    }
                                                    Map<String, Object> obj = new HashMap<>();
                                                    obj.put("macAddress", macAddress);
                                                    obj.put("dataList", dataList);
                                                    bridgeWebView.callHandler("bleInitDataOnCompleteCallBack", gson.toJson(obj), new CallBackFunction() {
                                                        @Override
                                                        public void onCallBack(String data) {

                                                        }
                                                    });
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Map<String, Object> objectMap = new HashMap<>();
                                                    objectMap.put("errorMsg", error);
                                                    objectMap.put("macAddress", bleDeviceUtil.getBluetoothDevice().getAddress());
                                                    bridgeWebView.callHandler("bleInitDataFailureCallBack", gson.toJson(objectMap), new CallBackFunction() {
                                                        @Override
                                                        public void onCallBack(String data) {

                                                        }
                                                    });
                                                }
                                            });
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
                if (((MainActivity) getActivity()).getBleService() != null) {
                    BleDeviceUtil bleDeviceUtil = ((MainActivity) getActivity()).getBleService().getBleDeviceUtil();
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
                                getActivity().runOnUiThread(new Runnable() {
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
                BleService bleService = ((MainActivity) getActivity()).getBleService();
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
                                getActivity().runOnUiThread(new Runnable() {
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
                BleService bleService = ((MainActivity) getActivity()).getBleService();
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
                                getActivity().runOnUiThread(new Runnable() {
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


        bridgeWebView.registerHandler("startQrCodeScan", new BridgeHandler() {
            @Override
            public void handler(String requestCode, CallBackFunction function) {

                XXPermissions.with(getActivity()).permission(Permission.CAMERA, Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO, Permission.READ_MEDIA_AUDIO).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
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

                            HmsScanAnalyzerOptions.Creator creator = new HmsScanAnalyzerOptions.Creator();
                            creator.setHmsScanTypes(HmsScan.ALL_SCAN_TYPE);
                            creator.setShowGuide(true);
                            creator.setPhotoMode(true);
                            HmsScanAnalyzerOptions hmsScanAnalyzerOptions = creator.create();
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


        bridgeWebView.registerHandler("callPhone", new
                BridgeHandler() {
                    @Override
                    public void handler(String data, CallBackFunction function) {

                        XXPermissions.with(getActivity())
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
                            XXPermissions.with(getActivity())
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
        bridgeWebView.registerHandler("choosePicture", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                CHOOSE_PIC_REQUEST_CODE = Integer.parseInt(data);
                XXPermissions.with(getActivity()).permission(Permission.MANAGE_EXTERNAL_STORAGE).permission(Permission.CAMERA).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("requestCode", CHOOSE_PIC_REQUEST_CODE);
                            bridgeWebView.callHandler("choosePictureCallBack", gson.toJson(Result.fail(PERMISSION_ERROR, map)), new CallBackFunction() {
                                @Override
                                public void onCallBack(String data) {

                                }
                            });
                            return;
                        }
                        showBottomSheetDialog();
                    }
                });
            }
        });


        bridgeWebView.registerHandler("connectMqtt", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {

                if (TextUtils.isEmpty(data)) {
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                    return;
                }
                MqttConfig mqttConfig = gson.fromJson(data, MqttConfig.class);
                if (TextUtils.isEmpty(mqttConfig.getHostname()) || mqttConfig.getPort() == null || TextUtils.isEmpty(mqttConfig.getClientId())) {
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                    return;
                }

                function.onCallBack(gson.toJson(Result.ok(true)));

                BleService bleService = ((MainActivity) getActivity()).getBleService();
                ThreadPool.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        boolean b = bleService.connectMqtt(mqttConfig);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (b) {
                                    bridgeWebView.callHandler("connectMqttCallBack", gson.toJson(Result.ok(true)), new CallBackFunction() {
                                        @Override
                                        public void onCallBack(String data) {

                                        }
                                    });
                                } else {
                                    bridgeWebView.callHandler("connectMqttCallBack", gson.toJson(Result.fail(MQTT_CONNECT_FAIL, false)), new CallBackFunction() {
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
        });


        bridgeWebView.registerHandler("mqttSubTopic", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String topic = jsonObject.getString("topic");
                    int qos = jsonObject.getInt("qos");
                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                    MqttClientUtil mqttClientUtil = bleService.getMqttClientUtil();

                    if (mqttClientUtil == null || !mqttClientUtil.isConnected()) {
                        function.onCallBack(gson.toJson(Result.fail(MQTT_CURRENT_NOT_CONNECTED, false)));
                        return;
                    }
                    boolean subscribe = mqttClientUtil.subscribe(topic, qos);
                    if (subscribe) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(FAIL, false)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("mqttUnSubTopic", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String topic = jsonObject.getString("topic");
                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                    MqttClientUtil mqttClientUtil = bleService.getMqttClientUtil();

                    if (mqttClientUtil == null || !mqttClientUtil.isConnected()) {
                        function.onCallBack(gson.toJson(Result.fail(MQTT_CURRENT_NOT_CONNECTED, false)));
                        return;
                    }
                    boolean subscribe = mqttClientUtil.unSubscribe(topic);
                    if (subscribe) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(FAIL, false)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("mqttPublishMsg", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String topic = jsonObject.getString("topic");
                    String content = jsonObject.getString("content");
                    int qos = jsonObject.getInt("qos");
                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                    MqttClientUtil mqttClientUtil = bleService.getMqttClientUtil();
                    if (mqttClientUtil == null || !mqttClientUtil.isConnected()) {
                        function.onCallBack(gson.toJson(Result.fail(MQTT_CURRENT_NOT_CONNECTED, false)));
                        return;
                    }
                    boolean subscribe = mqttClientUtil.publish(topic, qos, content.getBytes(StandardCharsets.UTF_8));
                    if (subscribe) {
                        function.onCallBack(gson.toJson(Result.ok(true)));
                    } else {
                        function.onCallBack(gson.toJson(Result.fail(FAIL, false)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });


        bridgeWebView.registerHandler("saveParam", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String key = jsonObject.getString("key");
                    String value = jsonObject.getString("value");
                    SharedPreferencesUtils.setParam(getActivity(), key, value);
                    function.onCallBack(gson.toJson(Result.ok(true)));
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("getParam", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String key = jsonObject.getString("key");
                    String value = (String) SharedPreferencesUtils.getParam(getActivity(), key, "");
                    function.onCallBack(gson.toJson(Result.ok(value)));
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, null)));
                }
            }
        });

        bridgeWebView.registerHandler("removeParam", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String key = jsonObject.getString("key");
                    SharedPreferencesUtils.removeParam(getActivity(), key);
                    function.onCallBack(gson.toJson(Result.ok(true)));
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });


        bridgeWebView.registerHandler("fingerprintVerification", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                function.onCallBack(gson.toJson(Result.ok(true)));

                BiometricPrompt biometricPrompt = new BiometricPrompt(getActivity(),
                        ContextCompat.getMainExecutor(getActivity()), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Logger.e("Authentication error: " + errString);
                        bridgeWebView.callHandler("fingerprintVerificationCallBack", gson.toJson(Result.fail(FAIL.getCode(), "Authentication error: " + errString, false)), new CallBackFunction() {
                            @Override
                            public void onCallBack(String data) {
                            }
                        });

                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Logger.d("Authentication succeeded! ");

                        bridgeWebView.callHandler("fingerprintVerificationCallBack", gson.toJson(Result.ok(true)), new CallBackFunction() {
                            @Override
                            public void onCallBack(String data) {
                            }
                        });
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Logger.e("Authentication failed! ");

                        bridgeWebView.callHandler("fingerprintVerificationCallBack", gson.toJson(Result.fail(FAIL.getCode(), "Authentication failed!", false)), new CallBackFunction() {
                            @Override
                            public void onCallBack(String data) {
                            }
                        });
                    }
                });
                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Fingerprint Verification")
                        .setSubtitle("Verifying your fingerprints")
                        .setNegativeButtonText("Cancel")
                        .build();
                biometricPrompt.authenticate(promptInfo);
            }
        });


        bridgeWebView.registerHandler("openOcr", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    if (data == null || data.isEmpty()) {
                        function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                        return;
                    }
                    Uri uri = ImageUtil.saceCacheImageAndGetUri(getActivity(), ImageUtil.base64ImgTrimPre(data));
                    Intent intent = new Intent(getActivity(), OcrActivity.class);
                    intent.putExtra("uri", uri.toString());
                    startActivityForResult(intent, REQUEST_CODE_OCR);


                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("saveImg", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    if (data == null || data.isEmpty()) {
                        function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                        return;
                    }

                    function.onCallBack(gson.toJson(Result.ok(true)));
                    ThreadPool.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            ImageUtil.saveImageToGallery(getActivity(), data);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("startLocationListener", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {

                    XXPermissions.with(getActivity()).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION)
                            .interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                    if (!allGranted) {
                                        function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
                                        return;
                                    }
                                    function.onCallBack(gson.toJson(Result.ok(true)));
                                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                                    bleService.startGps();
                                }
                            });


                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("stopLocationListener", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    XXPermissions.with(getActivity()).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION)
                            .interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                    if (!allGranted) {
                                        function.onCallBack(gson.toJson(Result.fail(PERMISSION_ERROR, false)));
                                        return;
                                    }
                                    function.onCallBack(gson.toJson(Result.ok(true)));
                                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                                    bleService.stopGps();
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, false)));
                }
            }
        });

        bridgeWebView.registerHandler("getLastLocation", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    BleService bleService = ((MainActivity) getActivity()).getBleService();
                    String lastLocation = bleService.getLastLocation();
                    function.onCallBack(gson.toJson(Result.ok(lastLocation)));
                } catch (Exception e) {
                    e.printStackTrace();
                    function.onCallBack(gson.toJson(Result.fail(PARAMETER_ERROR, "")));
                }
            }
        });
    }


    public void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog);
        //不传第二个参数
        //BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        // 底部弹出的布局
        View bottomView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_layout, null);
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
                takePhotoUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                        getActivity().runOnUiThread(new Runnable() {
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
        if (requestCode == REQUEST_CODE_OCR && resultCode == RESULT_OK) {
            String resultData = data.getStringExtra("ocrStr");
            // 处理结果
            bridgeWebView.callHandler("openOcrCallBack", resultData, new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                }
            });
        }
    }

    private String getImagePath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }
        return null;
    }

}
