package com.example.myapplication.service;

import static com.example.myapplication.enums.EventBusEnum.BLE_CONNECT;
import static com.example.myapplication.enums.EventBusEnum.BLE_CONNECT_FAIL;
import static com.example.myapplication.enums.EventBusEnum.BLE_INIT_ERROR;
import static com.example.myapplication.enums.EventBusEnum.MQTT_MSG_ARRIVED;
import static com.example.myapplication.enums.EventBusEnum.NOT_ENABLE_LE;
import static com.example.myapplication.enums.EventBusEnum.NOT_SUPPORT_LE;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.myapplication.constants.Result;
import com.example.myapplication.constants.RetCode;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.MqttConfig;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.enums.EventBusEnum;
import com.example.myapplication.thread.ThreadPool;
import com.example.myapplication.util.BleDeviceUtil;
import com.example.myapplication.util.LogUtil;
import com.example.myapplication.util.MqttClientUtil;
import com.example.myapplication.util.SharedPreferencesUtils;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BleService extends Service implements MqttCallback {
    private LocationManager mLocationManager;
    private Map<String, BluetoothDevice> bleDeviceMap = new ConcurrentHashMap<>();

    private Map<String, BleDeviceInfo> bleDeviceInfoMap = new ConcurrentHashMap<>();

    private BluetoothAdapter bluetoothAdapter;

    //low power ble
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean bleIsInit = false;

    private BleDeviceUtil bleDeviceUtil = null;

    public boolean isBleIsInit() {
        return bleIsInit;
    }

    private LocationListener locationListener;

    private String bleKeyword = "";

    private MqttClientUtil mqttClientUtil = null;

    @Override
    public void onCreate() {
        super.onCreate();
        initBleConfig();
    }


    public boolean connectMqtt(MqttConfig mqttConfig){
        if(mqttClientUtil!=null){
            try {
                mqttClientUtil.disConnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        mqttClientUtil = new MqttClientUtil(mqttConfig);
        boolean connect = mqttClientUtil.createConnect(BleService.this);
        if(connect){
            return true;
        }else{
            return false;
        }
    }

    public DeviceConnStatEnum getBleConnect() {
        if (bleDeviceUtil == null) return DeviceConnStatEnum.DISCONNECTED;
        return bleDeviceUtil.getConnectStat();
    }

    public Map<String, BleDeviceInfo> getBleDeviceInfoMap() {
        return bleDeviceInfoMap;
    }

    /**
     * 初始化蓝牙扫描相关参数
     */
    public void initBleConfig() {
        try {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                EventBus.getDefault().post(new EventBusMsg(NOT_SUPPORT_LE, null));
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                EventBus.getDefault().post(new EventBusMsg(NOT_ENABLE_LE, null));
                return;
            }
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        } catch (Exception e) {
            e.printStackTrace();
            EventBus.getDefault().post(new EventBusMsg(BLE_INIT_ERROR, e.getMessage()));
        }
    }


    @SuppressLint("MissingPermission")
    public void startBleScan(String keyword) {
        this.bleKeyword=keyword;
        bleDeviceMap.clear();
        bleDeviceInfoMap.clear();
        if (bluetoothLeScanner == null) {
            initBleConfig();
        }
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner.startScan(scanCallback);
        }
    }

    public ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String bleName = device.getName();
            if (!TextUtils.isEmpty(bleName)) {
                bleName = bleName.trim();
                if (!TextUtils.isEmpty(bleKeyword) && !bleName.contains(bleKeyword)) {
                    return;
                }
                LogUtil.debug(device.getAddress() + "," + device.getName());
                String typeStr = "Unknown";
                if (device.getType() == 1) {
                    typeStr = "Classic";
                } else if (device.getType() == 2) {
                    typeStr = "Low Energy";
                } else if (device.getType() == 3) {
                    typeStr = "DUAL";
                }
                BleDeviceInfo checkRecord = new BleDeviceInfo();
                checkRecord.setMacAddress(device.getAddress());
                checkRecord.setName(bleName);
                checkRecord.setRssi(result.getRssi());

                bleDeviceMap.put(device.getAddress(), device);
                bleDeviceInfoMap.put(device.getAddress(), checkRecord);
                EventBus.getDefault().post(new EventBusMsg(EventBusEnum.BLE_FIND, checkRecord));
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            LogUtil.debug("ScanCallback==>onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            LogUtil.error("ScanCallback==>onScanFailed:" + errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    public void stopScan() {
        try {
            bluetoothLeScanner.stopScan(scanCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BleDeviceUtil getBleDeviceUtil() {
        return bleDeviceUtil;
    }

    public BleDeviceUtil connectBle(String mac) throws Exception {
        if (bleDeviceUtil != null) {
                bleDeviceUtil.destroy();
                bleDeviceUtil = null;
        }
        bleIsInit = false;
        BluetoothDevice bleDevice = bleDeviceMap.get(mac);
        if (bleDevice != null) {
            bleDeviceUtil = new BleDeviceUtil(bleDevice, BleService.this);
            if (bleDeviceUtil.connectGatt() == DeviceConnStatEnum.CONNECTED) {
                EventBus.getDefault().post(new EventBusMsg<>(BLE_CONNECT, mac));
            } else {
                bleDeviceUtil.destroy();
                bleDeviceUtil = null;
                EventBus.getDefault().post(new EventBusMsg<>(BLE_CONNECT_FAIL, mac));
            }
        }
        return bleDeviceUtil;
    }

    public MqttClientUtil getMqttClientUtil() {
        return mqttClientUtil;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BleServiceBinder();
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LogUtil.debug("MqttService==>messageArrived topic:" + topic + ",message:" + new String(message.getPayload()));
        Map<String,String>map=new HashMap<>();
        map.put("topic",topic);
        map.put("message",new String(message.getPayload()));
        EventBus.getDefault().post(new EventBusMsg<>(MQTT_MSG_ARRIVED,new Gson().toJson(map)));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }


    public class BleServiceBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null && locationListener != null) {
            mLocationManager.removeUpdates(locationListener);
        }
    }
}
