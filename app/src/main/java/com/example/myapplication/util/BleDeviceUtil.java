package com.example.myapplication.util;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

import static com.example.myapplication.enums.EventBusEnum.BLE_CONNECT;
import static com.example.myapplication.enums.EventBusEnum.BLE_DISCONNECT;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.myapplication.callback.InitBleDataCallBack;
import com.example.myapplication.constants.DataConvert;
import com.example.myapplication.constants.Result;
import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.DescriptorDomain;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.event.BleStatus;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.enums.ServiceNameEnum;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class BleDeviceUtil {

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

    /**
     * Created by dsr on 2023/10/11.
     */
    private Context context;
    private DeviceConnStatEnum connected;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;
    private static final long TIME_OUT = 3000;
    private CountDownLatch countDownLatch;
    private Map<String, ServicesPropertiesDomain> serviceDataDtoMap = null;
    private int maxCharacteristicCount = 0;
    private StringBuffer notifyBuff = new StringBuffer();

    public BleDeviceUtil(BluetoothDevice bluetoothDevice, Context context) {
        this.bluetoothDevice = bluetoothDevice;
        this.context = context;
        this.connected = DeviceConnStatEnum.DISCONNECTED;
        serviceDataDtoMap = new ConcurrentHashMap<>();
    }

    /**
     * 连接蓝牙
     *
     * @return
     */
    @SuppressLint("MissingPermission")
    public synchronized DeviceConnStatEnum connectGatt() throws Exception {
        if (serviceDataDtoMap == null) throw new Exception("connectGatt");
        countDownLatch = new CountDownLatch(1);
        //autoConnect==false,表示立即发起连接，否则等蓝牙空闲才会连接
        bluetoothDevice.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_AUTO);
        try {
            countDownLatch.await(20000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connected;
    }

    @SuppressLint("MissingPermission")
    public synchronized void setMtu(int maxByteSize) throws ExecutionException, InterruptedException {
        bluetoothGatt.requestMtu(maxByteSize);
        try {
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized ServicesPropertiesDomain initService(String serviceUUID) {
        ServicesPropertiesDomain servicesPropertiesDomain = serviceDataDtoMap.get(serviceUUID);
        Map<String, CharacteristicDomain> characterMap = servicesPropertiesDomain.getCharacterMap();
        Collection<CharacteristicDomain> chValues = characterMap.values();
        for (CharacteristicDomain characteristicDomain : chValues) {
            String chUUID = characteristicDomain.getUuid();
            Map<String, DescriptorDomain> descMap = characteristicDomain.getDescMap();
            Collection<DescriptorDomain> descValues = descMap.values();
            for (DescriptorDomain descriptorDomain : descValues) {
                readDescriptor(serviceUUID, chUUID, descriptorDomain.getUuid());
            }
            readCharacteristic(serviceUUID, chUUID);
        }
        return servicesPropertiesDomain;
    }

//    public synchronized void initData(InitBleDataCallBack callBack) {
//        try {
//            Collection<ServicesPropertiesDomain> values = serviceDataDtoMap.values();
//            int totalCount = 0;
//            int current = 0;
//            for (ServicesPropertiesDomain servicesPropertiesDomain : values) {
//                totalCount += servicesPropertiesDomain.getCharacterMap().values().size();
//            }
//            for (ServicesPropertiesDomain servicesPropertiesDomain : values) {
//                Map<String, CharacteristicDomain> characterMap = servicesPropertiesDomain.getCharacterMap();
//                Collection<CharacteristicDomain> chValues = characterMap.values();
//                String serviceUUID = servicesPropertiesDomain.getUuid();
//                for (CharacteristicDomain characteristicDomain : chValues) {
//                    String chUUID = characteristicDomain.getUuid();
//                    Map<String, DescriptorDomain> descMap = characteristicDomain.getDescMap();
//                    Collection<DescriptorDomain> descValues = descMap.values();
//                    for (DescriptorDomain descriptorDomain : descValues) {
//                        readDescriptor(serviceUUID, chUUID, descriptorDomain.getUuid());
//                    }
//                    readCharacteristic(serviceUUID, chUUID);
//                    current++;
//                    callBack.onProgress(totalCount, current, characteristicDomain);
//                }
//            }
//            callBack.onComplete(serviceDataDtoMap);
//        } catch (Exception e) {
//            e.printStackTrace();
//            callBack.onFailure(e.getMessage());
//        }
//    }

    public synchronized void initData(InitBleDataCallBack callBack) {
        try {
            Collection<ServicesPropertiesDomain> values = serviceDataDtoMap.values();
            int totalCount = 0;
            int current = 0;

            for (ServicesPropertiesDomain servicesPropertiesDomain : values) {
                totalCount += servicesPropertiesDomain.getCharacterMap().values().size();
            }

            for (ServicesPropertiesDomain servicesPropertiesDomain : values) {
                Map<String, CharacteristicDomain> characterMap = servicesPropertiesDomain.getCharacterMap();
                Collection<CharacteristicDomain> chValues = characterMap.values();
                String serviceUUID = servicesPropertiesDomain.getUuid();

                for (CharacteristicDomain characteristicDomain : chValues) {
                    String chUUID = characteristicDomain.getUuid();
                    Map<String, DescriptorDomain> descMap = characteristicDomain.getDescMap();
                    Collection<DescriptorDomain> descValues = descMap.values();

                    for (DescriptorDomain descriptorDomain : descValues) {
                        readDescriptor(serviceUUID, chUUID, descriptorDomain.getUuid());
                    }

                    new Thread(() -> {
                        readCharacteristic(serviceUUID, chUUID);
                        try {
                            Thread.sleep(1000); // Wait for read to complete
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("🚀 Updated Real Value AFTER Read: " + characteristicDomain.getRealVal());
                    }).start();

                    current++;
                    callBack.onProgress(totalCount, current, characteristicDomain);
                }
            }
            callBack.onComplete(serviceDataDtoMap);
        } catch (Exception e) {
            e.printStackTrace();
            callBack.onFailure(e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public synchronized Result<Void> writeCharacteristic(String serviceUUID, String characteristicUUID, String value) throws ExecutionException, InterruptedException {
        ServicesPropertiesDomain servicesPropertiesDomain = serviceDataDtoMap.get(serviceUUID);
        if (servicesPropertiesDomain == null) {
            return Result.fail("serviceUUID not match！");
        }
        CharacteristicDomain characteristicDomain = servicesPropertiesDomain.getCharacterMap().get(characteristicUUID);
        if (characteristicDomain == null) {
            return Result.fail("characteristicUUID not match！");
        }
        byte[] bytes = DataConvert.convert2Arr(value, characteristicDomain.getValType());
        return writeCharacteristic(serviceUUID, characteristicUUID, bytes);
    }

    @SuppressLint("MissingPermission")
    public synchronized Result<Void> writeCharacteristic(String serviceUUID, String characteristicUUID, byte[] value) throws ExecutionException, InterruptedException {
        notifyBuff.delete(0, notifyBuff.length());
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    characteristic.setValue(value);
                    countDownLatch = new CountDownLatch(1);
                    bluetoothGatt.writeCharacteristic(characteristic);
                    try {
                        countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
                        return Result.ok();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return Result.fail(e.getMessage());
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }

    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> readCharacteristic(String serviceUUID, String characteristicUUID) {
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    countDownLatch = new CountDownLatch(1);
                    bluetoothGatt.readCharacteristic(characteristic);
                    try {
                        countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
                        CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
                        return Result.ok(characteristicDataDto);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return Result.fail(e.getMessage());
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }

    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> setCharacteristicNotification(String serviceUUID, String characteristicUUID, String descriptorUUID, Boolean enable) {
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    try {
                        boolean b = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                        if (b) {
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID));
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                countDownLatch = new CountDownLatch(1);
                                bluetoothGatt.writeDescriptor(descriptor);
                                countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
                            }
                            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
                            int properties = characteristic.getProperties();
                            characteristicDataDto.setProperties(properties);
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                characteristicDataDto.setEnableRead(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                characteristicDataDto.setEnableWrite(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                characteristicDataDto.setEnableNotify(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) > 0) {
                                characteristicDataDto.setEnableWriteNoResp(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                                characteristicDataDto.setEnableIndicate(true);
                            }
                            return Result.ok(characteristicDataDto);
                        } else {
                            return Result.fail("设置Notify失败！");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Result.fail(e.getMessage());
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }

    /**
     * 启用指令通知
     */
    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> enableIndicateNotification(String serviceUUID, String characteristicUUID, Boolean enable) {
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    try {
                        boolean b = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                        if (b) {
                            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
                            int properties = characteristic.getProperties();
                            characteristicDataDto.setProperties(properties);
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                characteristicDataDto.setEnableRead(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                characteristicDataDto.setEnableWrite(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                characteristicDataDto.setEnableNotify(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) > 0) {
                                characteristicDataDto.setEnableWriteNoResp(true);
                            }
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                                characteristicDataDto.setEnableIndicate(true);
                            }
                            return Result.ok(characteristicDataDto);
                        } else {
                            return Result.fail("设置Notify失败！");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Result.fail(e.getMessage());
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }

    @SuppressLint("MissingPermission")
    public synchronized Result<DescriptorDomain> readDescriptor(String serviceUUID, String characteristicUUID, String descriptorUUID) {
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID));
                    if (descriptor != null) {
                        bluetoothGatt.readDescriptor(descriptor);
                        countDownLatch = new CountDownLatch(1);
                        try {
                            countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
                            DescriptorDomain descriptorDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID).getDescMap().get(descriptorUUID);
                            return Result.ok(descriptorDataDto);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return Result.fail(e.getMessage());
                        }
                    } else {
                        return Result.fail("未查找到对应的Descriptor！");
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }


    @SuppressLint("MissingPermission")
    public synchronized Result<DescriptorDomain> writeDescriptor(String serviceUUID, String characteristicUUID, String descriptorUUID, byte[] data) {
        if (connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID));
                    if (descriptor != null) {
                        descriptor.setValue(data);
                        bluetoothGatt.writeDescriptor(descriptor);
                        countDownLatch = new CountDownLatch(1);
                        try {
                            countDownLatch.await(TIME_OUT, TimeUnit.MILLISECONDS);
                            DescriptorDomain descriptorDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID).getDescMap().get(descriptorUUID);
                            return Result.ok(descriptorDataDto);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return Result.fail(e.getMessage());
                        }
                    } else {
                        return Result.fail("未查找到对应的Descriptor！");
                    }
                } else {
                    return Result.fail("未查找到对应的GattCharacteristic！");
                }
            } else {
                return Result.fail("未查找到对应的GattService！");
            }
        } else {
            return Result.fail("蓝牙已断开，请重新连接！");
        }
    }


    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        /**
         * 物理层改变回调 触发这个方法需要使用gatt.setPreferredPhy()方法设置接收和发送的速率，然后蓝牙设备给我回一个消息，就触发onPhyUpdate（）方法了
         * 设置 2M
         * gatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
         *
         * @param gatt
         * @param txPhy
         * @param rxPhy
         * @param status
         */
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            LogUtil.debug("BluetoothGattCallback onPhyUpdate");
        }

        /**
         * 设备物理层读取回调
         *
         * @param gatt
         * @param txPhy
         * @param rxPhy
         * @param status
         */
        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            LogUtil.debug("BluetoothGattCallback onPhyRead");
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            LogUtil.debug("BluetoothGattCallback onConnectionStateChange:" + newState);
            if (newState == BluetoothGattServer.STATE_CONNECTED) {
                connected = DeviceConnStatEnum.CONNECTED;
                gatt.discoverServices();
                bluetoothGatt = gatt;
            } else {//蓝牙断开
                connected = DeviceConnStatEnum.DISCONNECTED;
                bluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);


            //可以开始进行service读写了
            LogUtil.debug("BluetoothGattCallback onServicesDiscovered:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();
                if (services != null && services.size() > 0) {
                    for (int i = 0; i < services.size(); i++) {
                        BluetoothGattService bluetoothGattService = services.get(i);
                        if (!ServiceNameEnum.contain(bluetoothGattService.getUuid().toString())) {
                            continue;
                        }
                        ServicesPropertiesDomain bleServiceDataDto = serviceDataDtoMap.get(bluetoothGattService.getUuid().toString());
                        if (bleServiceDataDto == null) {
                            bleServiceDataDto = new ServicesPropertiesDomain();
                            bleServiceDataDto.setUuid(bluetoothGattService.getUuid().toString());
                            serviceDataDtoMap.put(bluetoothGattService.getUuid().toString(), bleServiceDataDto);
                        }
                        bleServiceDataDto.setServiceNameEnum(ServiceNameEnum.getServiceNameFromUUID(bleServiceDataDto.getUuid()));
                        bleServiceDataDto.setServiceProperty(bluetoothGattService.getType() == SERVICE_TYPE_PRIMARY ? "PRIMARY SERVICE" : "SECONDARY SERVICE");
                        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                        if (characteristics != null && characteristics.size() > 0) {
                            for (int j = 0; j < characteristics.size(); j++) {
                                maxCharacteristicCount++;
                                BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(j);
                                Map<String, CharacteristicDomain> characteristicDataMap = bleServiceDataDto.getCharacterMap();
                                if (characteristicDataMap == null) {
                                    characteristicDataMap = new ConcurrentHashMap<>();
                                    bleServiceDataDto.setCharacterMap(characteristicDataMap);
                                }
                                CharacteristicDomain characteristicDataDto = characteristicDataMap.get(bluetoothGattCharacteristic.getUuid().toString());
                                if (characteristicDataDto == null) {
                                    characteristicDataDto = new CharacteristicDomain();
                                    characteristicDataDto.setUuid(bluetoothGattCharacteristic.getUuid().toString());

                                    characteristicDataMap.put(bluetoothGattCharacteristic.getUuid().toString(), characteristicDataDto);
                                }

                                int properties = bluetoothGattCharacteristic.getProperties();
                                characteristicDataDto.setProperties(properties);
                                if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                    characteristicDataDto.setEnableRead(true);
                                }
                                if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                    characteristicDataDto.setEnableWrite(true);
                                }
                                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    characteristicDataDto.setEnableNotify(true);
                                }
                                if ((properties & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) > 0) {
                                    characteristicDataDto.setEnableWriteNoResp(true);
                                }
                                if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                                    characteristicDataDto.setEnableIndicate(true);
                                }
                                characteristicDataDto.setServiceUuid(bleServiceDataDto.getUuid());
                                List<BluetoothGattDescriptor> descriptors = bluetoothGattCharacteristic.getDescriptors();
                                if (descriptors != null && descriptors.size() > 0) {
                                    for (int f = 0; f < descriptors.size(); f++) {
                                        BluetoothGattDescriptor bluetoothGattDescriptor = descriptors.get(f);
                                        Map<String, DescriptorDomain> descriptorDataMap = characteristicDataDto.getDescMap();
                                        if (descriptorDataMap == null) {
                                            descriptorDataMap = new ConcurrentHashMap<>();
                                            characteristicDataDto.setDescMap(descriptorDataMap);
                                        }

                                        DescriptorDomain descriptorDataDto = new DescriptorDomain();
                                        descriptorDataDto.setUuid(bluetoothGattDescriptor.getUuid().toString());
                                        descriptorDataMap.put(descriptorDataDto.getUuid(), descriptorDataDto);
                                    }
                                }
                            }
                        }
                    }
                }
                if (countDownLatch != null) countDownLatch.countDown();
                try {
                    setMtu(64);
                } catch (Exception e) {
                }
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            String characteristicUUID = characteristic.getUuid().toString();
            String serviceUUID = characteristic.getService().getUuid().toString();
            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);

            // 🔹 Log characteristic UUID
            System.out.println("✅ onCharacteristicRead called for UUID: " + characteristicUUID);

            // Check if value is received
            byte[] rawData = characteristic.getValue();
            if (rawData == null || rawData.length == 0) {
                System.out.println("⚠️ No Data Read from: " + characteristicUUID);
                return;
            }

            // 🔹 Log raw BLE data before conversion
            System.out.println("📡 Raw BLE Data (Hex): " + bytesToHex(rawData));

            // Store raw values
            characteristicDataDto.setValues(rawData);

            // Convert to real value
            Object convertedValue = DataConvert.convert2Obj(rawData, characteristicDataDto.getValType());
            characteristicDataDto.setRealVal(convertedValue);

            // 🔹 Log converted real value
            System.out.println("📡 Converted Real Value: " + convertedValue);
        }

        // Utility function to convert byte array to Hex string
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }

//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            super.onCharacteristicRead(gatt, characteristic, status);
//            String characteristicUUID = characteristic.getUuid().toString();
//            String serviceUUID = characteristic.getService().getUuid().toString();
//            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
//
//            try {
//                if (characteristic.getValue() == null || characteristic.getValue().length <= 0) {
//                    LogUtil.debug("BluetoothGattCallback onCharacteristicRead：value is null");
//                    return;
//                } else {
//                    LogUtil.debug("BluetoothGattCallback onCharacteristicRead  " + (characteristicDataDto.getName() != null ? characteristicDataDto.getName() : "?") + "   :" + ByteUtil.bytes2HexString(characteristic.getValue()));
//                }
//                characteristicDataDto.setValues(characteristic.getValue());
//                characteristicDataDto.setRealVal(DataConvert.convert2Obj(characteristicDataDto.getValues(), characteristicDataDto.getValType()));
//                if (countDownLatch != null) {
//                    countDownLatch.countDown();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                LogUtil.error("DataConvert.convert2Obj ====>" + new Gson().toJson(characteristicDataDto));
//            }
//        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            LogUtil.debug("BluetoothGattCallback onCharacteristicRead2：" + ByteUtil.bytes2HexString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            LogUtil.debug("BluetoothGattCallback onCharacteristicWrite：" + ByteUtil.bytes2HexString(characteristic.getValue()));
            if (countDownLatch != null) countDownLatch.countDown();
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String s = new String(characteristic.getValue(), StandardCharsets.US_ASCII);
            LogUtil.debug("BluetoothGattCallback onCharacteristicChanged:" + s);
            notifyBuff.append(s);
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (descriptor.getValue() == null || descriptor.getValue().length <= 0) {
                LogUtil.debug("BluetoothGattCallback onDescriptorRead：value is null");
                return;
            }
            LogUtil.debug("BluetoothGattCallback onDescriptorRead：" + ByteUtil.bytes2HexString(descriptor.getValue()));
            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String descriptorUUID = descriptor.getUuid().toString();

            CharacteristicDomain characteristicDomain = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
            DescriptorDomain descriptorDomain = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID).getDescMap().get(descriptorUUID);
            descriptorDomain.setUuid(descriptorUUID);
            descriptorDomain.setDesc(descriptor.getValue() == null ? "" : new String(descriptor.getValue()));

            if (descriptorDomain.getDesc() != null && descriptorDomain.getDesc().trim().length() > 0 && descriptorDomain.getDesc().contains(":")) {
                String[] split = descriptorDomain.getDesc().split(":");
                if (split.length >= 3) {
                    characteristicDomain.setName(split[0]);
                    characteristicDomain.setValType(Integer.valueOf(split[1]));
                    characteristicDomain.setDesc(split[2]);
                }
            }
            if (countDownLatch != null) countDownLatch.countDown();

        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
            LogUtil.debug("BluetoothGattCallback onDescriptorRead :" + ByteUtil.bytes2HexString(descriptor.getValue()));

        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            LogUtil.debug("BluetoothGattCallback onDescriptorWrite :" + ByteUtil.bytes2HexString(descriptor.getValue()));
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            LogUtil.debug("BluetoothGattCallback onReliableWriteCompleted");

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            LogUtil.debug("BluetoothGattCallback onReadRemoteRssi");

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            LogUtil.debug("BluetoothGattCallback onMtuChanged");
            if (countDownLatch != null) countDownLatch.countDown();
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
            LogUtil.debug("BluetoothGattCallback onServiceChanged");
        }
    };


    @SuppressLint("MissingPermission")
    public void destroy() {
        try {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt = null;
            }
            countDownLatch = null;
            serviceDataDtoMap = null;
            bluetoothGattCallback = null;
            executor.shutdownNow();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public DeviceConnStatEnum getConnectStat() {
        return connected;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public Map<String, ServicesPropertiesDomain> getServiceDataDtoMap() {
        return serviceDataDtoMap;
    }

    public int getMaxCharacteristicCount() {
        return maxCharacteristicCount;
    }

    public StringBuffer getNotifyBuff() {
        return notifyBuff;
    }


}
