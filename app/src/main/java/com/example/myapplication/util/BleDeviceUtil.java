package com.example.myapplication.util;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

import static com.example.myapplication.enums.EventBusEnum.BLE_CONNECT;
import static com.example.myapplication.enums.EventBusEnum.BLE_DISCONNECT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.example.myapplication.callback.InitBleDataCallBack;
import com.example.myapplication.callback.InitBleServiceDataCallBack;
import com.example.myapplication.constants.DataConvert;
import com.example.myapplication.constants.Result;
import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.DescriptorDomain;
import com.example.myapplication.entity.ProductTypeConfig;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.example.myapplication.entity.event.BleStatus;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.enums.DeviceConnStatEnum;
import com.example.myapplication.enums.ServiceNameEnum;
import com.example.myapplication.thread.ThreadPool;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
    // 使用StringBuilder替代StringBuffer提升性能（单线程环境）
    private StringBuilder notifyBuff = new StringBuilder();
    // 标记是否使用了缓存配置（如果使用了配置，跳过onServicesDiscovered中的遍历）
    private boolean usedCachedConfig = false;

    public BleDeviceUtil(BluetoothDevice bluetoothDevice, Context context) {
        this.bluetoothDevice = bluetoothDevice;
        this.context = context;
        this.connected = DeviceConnStatEnum.DISCONNECTED;
        serviceDataDtoMap = new ConcurrentHashMap<>();
    }

    /**
     * 检查蓝牙连接状态（优化：提取公共方法避免重复代码）
     */
    private boolean isConnected() {
        return connected == DeviceConnStatEnum.CONNECTED && bluetoothGatt != null;
    }

    /**
     * 设置CharacteristicDomain的属性标志（优化：提取公共方法避免重复代码）
     */
    private void setCharacteristicProperties(CharacteristicDomain characteristicDataDto, int properties) {
        characteristicDataDto.setProperties(properties);
        characteristicDataDto.setEnableRead((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0);
        characteristicDataDto.setEnableWrite((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0);
        characteristicDataDto.setEnableNotify((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0);
        characteristicDataDto.setEnableWriteNoResp((properties & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) > 0);
        characteristicDataDto.setEnableIndicate((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0);
    }

    /**
     * 连接蓝牙
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    public synchronized DeviceConnStatEnum connectGatt() throws Exception {
        if (serviceDataDtoMap == null) throw new Exception("connectGatt");
        usedCachedConfig = false; // 重置标志，新连接开始时默认未使用缓存
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

    /**
     * 使用缓存配置快速构建 serviceDataDtoMap
     * 避免每次连接都遍历Service和Characteristic
     * 
     * @param config 产品类型配置
     */
    private void buildServiceDataFromConfig(ProductTypeConfig config) {
        if (config == null || config.getServices() == null) {
            return;
        }

        serviceDataDtoMap.clear();
        maxCharacteristicCount = 0;

        for (ProductTypeConfig.ServiceConfig serviceConfig : config.getServices().values()) {
            ServicesPropertiesDomain serviceDomain = new ServicesPropertiesDomain();
            serviceDomain.setUuid(serviceConfig.getServiceUUID());
            serviceDomain.setServiceProperty(serviceConfig.getServiceProperty());
            serviceDomain.setServiceNameEnum(serviceConfig.getServiceNameEnumValue());

            Map<String, CharacteristicDomain> characteristicMap = new ConcurrentHashMap<>();

            if (serviceConfig.getCharacteristics() != null) {
                for (ProductTypeConfig.CharacteristicConfig charConfig : serviceConfig.getCharacteristics().values()) {
                    maxCharacteristicCount++;
                    
                    CharacteristicDomain charDomain = new CharacteristicDomain();
                    charDomain.setUuid(charConfig.getCharacteristicUUID());
                    charDomain.setName(charConfig.getName());
                    charDomain.setValType(charConfig.getValType());
                    charDomain.setDesc(charConfig.getDesc());
                    charDomain.setProperties(charConfig.getProperties());
                    // 优先使用配置中的serviceUuid，如果没有则使用serviceConfig的UUID
                    charDomain.setServiceUuid(charConfig.getServiceUuid() != null ? charConfig.getServiceUuid() : serviceConfig.getServiceUUID());
                    
                    // 恢复enable标志（从配置中恢复，避免重复计算）
                    charDomain.setEnableWrite(charConfig.isEnableWrite());
                    charDomain.setEnableRead(charConfig.isEnableRead());
                    charDomain.setEnableIndicate(charConfig.isEnableIndicate());
                    charDomain.setEnableNotify(charConfig.isEnableNotify());
                    charDomain.setEnableWriteNoResp(charConfig.isEnableWriteNoResp());
                    
                    // 如果properties不为空但enable标志未设置，则通过properties计算（兼容旧数据）
                    if (charConfig.getProperties() != null && !charConfig.isEnableRead() && !charConfig.isEnableWrite()) {
                        setCharacteristicProperties(charDomain, charConfig.getProperties());
                    }

                    // 构建Descriptor Map
                    Map<String, DescriptorDomain> descMap = new ConcurrentHashMap<>();
                    if (charConfig.getDescriptorUUIDs() != null) {
                        for (String descUUID : charConfig.getDescriptorUUIDs()) {
                            DescriptorDomain descDomain = new DescriptorDomain();
                            descDomain.setUuid(descUUID);
                            descMap.put(descUUID, descDomain);
                        }
                    }
                    charDomain.setDescMap(descMap);

                    characteristicMap.put(charConfig.getCharacteristicUUID(), charDomain);
                }
            }

            serviceDomain.setCharacterMap(characteristicMap);
            serviceDataDtoMap.put(serviceConfig.getServiceUUID(), serviceDomain);
        }

        LogUtil.debug("ServiceData built from config, productType: " + config.getProductType() + ", characteristics count: " + maxCharacteristicCount);
    }

    /**
     * 如果是首次连接该产品类型，保存配置
     */
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void saveConfigIfNeeded() {
        String deviceName = bluetoothDevice.getName();
        String productType = ProductTypeConfigManager.extractProductType(deviceName);
        
        if (productType != null) {
            ProductTypeConfig existingConfig = ProductTypeConfigManager.getConfig(context, productType);
            if (existingConfig == null && serviceDataDtoMap != null && !serviceDataDtoMap.isEmpty()) {
                // 首次连接，从当前 serviceDataDtoMap 构建配置并保存
                ProductTypeConfig newConfig = ProductTypeConfigManager.buildConfigFromServiceData(productType, serviceDataDtoMap);
                ProductTypeConfigManager.saveConfig(context, productType, newConfig);
                LogUtil.debug("Config saved for productType: " + productType);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public synchronized void initService(String serviceName, InitBleServiceDataCallBack callBack) {
        try {
            Collection<ServicesPropertiesDomain> values = serviceDataDtoMap.values();
            ServicesPropertiesDomain servicesPropertiesDomain = null;
            for (ServicesPropertiesDomain domain : values) {
                boolean b = domain.getServiceNameEnum().getServiceName().equalsIgnoreCase(serviceName);
                if (b) {
                    servicesPropertiesDomain = domain;
                }
            }
            if (servicesPropertiesDomain == null) throw new Exception("Service name not found!");
            Map<String, CharacteristicDomain> characterMap = servicesPropertiesDomain.getCharacterMap();
            Collection<CharacteristicDomain> chValues = characterMap.values();
            int i = 0;
            for (CharacteristicDomain characteristicDomain : chValues) {
                String chUUID = characteristicDomain.getUuid();
                Map<String, DescriptorDomain> descMap = characteristicDomain.getDescMap();
                Collection<DescriptorDomain> descValues = descMap.values();
                for (DescriptorDomain descriptorDomain : descValues) {
                    readDescriptor(servicesPropertiesDomain.getUuid(), chUUID, descriptorDomain.getUuid());
                }
                readCharacteristic(servicesPropertiesDomain.getUuid(), chUUID);
                i++;
                callBack.onProgress(chValues.size(), i, characteristicDomain);
            }
            callBack.onComplete(servicesPropertiesDomain);
        } catch (Exception e) {
            e.printStackTrace();
            callBack.onFailure(e.getMessage());
        }
    }

    /**
     * 深度克隆 serviceDataDtoMap（使用 Gson 序列化和反序列化）
     */
    private Map<String, ServicesPropertiesDomain> cloneServiceDataDtoMap(Map<String, ServicesPropertiesDomain> source) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(source);
            java.lang.reflect.Type type = new TypeToken<Map<String, ServicesPropertiesDomain>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            LogUtil.error("Failed to clone serviceDataDtoMap: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public synchronized void initData(InitBleDataCallBack callBack) {
        try {
            // 如果使用了缓存配置且模板不为空，先返回默认数据
            if (usedCachedConfig && serviceDataDtoMap != null && !serviceDataDtoMap.isEmpty()) {
                // 克隆 serviceDataDtoMap 作为默认数据立即返回
                Map<String, ServicesPropertiesDomain> defaultData = cloneServiceDataDtoMap(serviceDataDtoMap);
                if (defaultData != null) {
                    // 立即返回默认数据
                    callBack.onComplete(defaultData);
                    LogUtil.debug("Returned default data from cached config, starting async initialization");
                } else {
                    // 克隆失败，直接使用原数据（安全起见）
                    LogUtil.debug("Clone failed, using original data");
                    callBack.onComplete(serviceDataDtoMap);
                }
                
                // 异步执行真实的初始化（读取实际值）
                ThreadPool.getExecutor().execute(new Runnable() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                    @Override
                    public void run() {
                        initDataAsync(callBack);
                    }
                });
                return;
            }
            
            // 未使用缓存配置，执行正常的同步初始化流程
            initDataSync(callBack);
        } catch (Exception e) {
            e.printStackTrace();
            callBack.onFailure(e.getMessage());
        }
    }

    /**
     * 异步初始化数据（用于使用缓存配置时）
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized void initDataAsync(InitBleDataCallBack callBack) {
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
                    readCharacteristic(serviceUUID, chUUID);
                    current++;
                    callBack.onProgress(totalCount, current, characteristicDomain);
                }
            }
            // 异步初始化完成后，更新缓存模板
            updateConfigAfterInit();
            
            // 再次调用 onComplete 返回更新后的数据
            callBack.onComplete(serviceDataDtoMap);
            LogUtil.debug("Async initialization completed, data updated");
        } catch (Exception e) {
            e.printStackTrace();
            callBack.onFailure(e.getMessage());
        }
    }

    /**
     * 同步初始化数据（用于未使用缓存配置时）
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized void initDataSync(InitBleDataCallBack callBack) {
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
                    readCharacteristic(serviceUUID, chUUID);
                    current++;
                    callBack.onProgress(totalCount, current, characteristicDomain);
                }
            }
            // 同步初始化完成后，更新缓存模板（如果是首次连接）
            updateConfigAfterInit();
            
            callBack.onComplete(serviceDataDtoMap);
        } catch (Exception e) {
            e.printStackTrace();
            callBack.onFailure(e.getMessage());
        }
    }

    /**
     * 在初始化完成后更新配置模板
     * 当读取了实际的Descriptor和Characteristic数据后，更新缓存中的配置
     */
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void updateConfigAfterInit() {
        try {
            String deviceName = bluetoothDevice.getName();
            String productType = ProductTypeConfigManager.extractProductType(deviceName);
            
            if (productType != null && serviceDataDtoMap != null && !serviceDataDtoMap.isEmpty()) {
                // 重新构建配置并保存（此时已包含从Descriptor读取的name、valType、desc等信息）
                ProductTypeConfig updatedConfig = ProductTypeConfigManager.buildConfigFromServiceData(productType, serviceDataDtoMap);
                ProductTypeConfigManager.saveConfig(context, productType, updatedConfig);
                LogUtil.debug("Config template updated after initialization for productType: " + productType);
            }
        } catch (Exception e) {
            LogUtil.error("Failed to update config after init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<Void> writeCharacteristic(String serviceUUID, String characteristicUUID, byte[] value) throws ExecutionException, InterruptedException {
        notifyBuff.setLength(0); // 使用setLength替代delete提升性能
        if (isConnected()) {
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> readCharacteristic(String serviceUUID, String characteristicUUID) {
        if (isConnected()) {
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> setCharacteristicNotification(String serviceUUID, String characteristicUUID, String descriptorUUID, Boolean enable) {
        if (isConnected()) {
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
                            setCharacteristicProperties(characteristicDataDto, characteristic.getProperties());
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
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<CharacteristicDomain> enableIndicateNotification(String serviceUUID, String characteristicUUID, Boolean enable) {
        if (isConnected()) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    try {
                        boolean b = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                        if (b) {
                            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);
                            setCharacteristicProperties(characteristicDataDto, characteristic.getProperties());
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<DescriptorDomain> readDescriptor(String serviceUUID, String characteristicUUID, String descriptorUUID) {
        if (isConnected()) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUUID));
                    if (descriptor != null) {
                        countDownLatch = new CountDownLatch(1); // 优化：在read之前创建countDownLatch
                        bluetoothGatt.readDescriptor(descriptor);
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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("MissingPermission")
    public synchronized Result<DescriptorDomain> writeDescriptor(String serviceUUID, String characteristicUUID, String descriptorUUID, byte[] data) {
        if (isConnected()) {
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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            LogUtil.debug("BluetoothGattCallback onConnectionStateChange:" + newState);
            if (newState == BluetoothGattServer.STATE_CONNECTED) {
                connected = DeviceConnStatEnum.CONNECTED;
                bluetoothGatt = gatt;
                
                // 尝试使用缓存配置快速构建 serviceDataDtoMap
                String deviceName = bluetoothDevice.getName();
                String productType = ProductTypeConfigManager.extractProductType(deviceName);
                
                if (productType != null) {
                    ProductTypeConfig config = ProductTypeConfigManager.getConfig(context, productType);
                    if (config != null) {
                        // 使用缓存配置构建 serviceDataDtoMap
                        buildServiceDataFromConfig(config);
                        usedCachedConfig = true; // 标记已使用缓存配置
                        LogUtil.debug("Using cached config for productType: " + productType);
                        // 使用配置时，仍然需要 discoverServices 来验证服务是否存在
                        // 但不需要等待遍历完成
                    }
                }
                
                gatt.discoverServices();
            } else {//蓝牙断开
                connected = DeviceConnStatEnum.DISCONNECTED;
                bluetoothGatt = null;
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);


            //可以开始进行service读写了
            LogUtil.debug("BluetoothGattCallback onServicesDiscovered:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                
                // 如果已使用缓存配置，跳过遍历（但需要验证配置是否有效）
                if (usedCachedConfig) {
                    LogUtil.debug("Using cached config, skip service discovery iteration");
                    // 验证配置是否与实际设备匹配（可选，简单检查Service数量）
                    List<BluetoothGattService> services = gatt.getServices();
                    if (services != null) {
                        int matchedServices = 0;
                        for (BluetoothGattService service : services) {
                            if (ServiceNameEnum.contain(service.getUuid().toString()) 
                                    && serviceDataDtoMap.containsKey(service.getUuid().toString())) {
                                matchedServices++;
                            }
                        }
                        LogUtil.debug("Config validation: matched " + matchedServices + " services");
                    }
                    // 首次连接后保存配置的逻辑移动到后面
                    saveConfigIfNeeded();
                    if (countDownLatch != null) countDownLatch.countDown();
                    try {
                        setMtu(64);
                    } catch (Exception e) {
                    }
                    return;
                }

                List<BluetoothGattService> services = gatt.getServices();
                if (services != null && services.size() > 0) {
                    // 优化：使用增强for循环提升性能和可读性
                    for (BluetoothGattService bluetoothGattService : services) {
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
                            // 优化：使用增强for循环提升性能和可读性
                            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
                                maxCharacteristicCount++;
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

                                setCharacteristicProperties(characteristicDataDto, bluetoothGattCharacteristic.getProperties());
                                characteristicDataDto.setServiceUuid(bleServiceDataDto.getUuid());
                                List<BluetoothGattDescriptor> descriptors = bluetoothGattCharacteristic.getDescriptors();
                                if (descriptors != null && descriptors.size() > 0) {
                                    // 优化：使用增强for循环提升性能和可读性
                                    for (BluetoothGattDescriptor bluetoothGattDescriptor : descriptors) {
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
                
                // 如果是首次连接该产品类型，遍历完成后保存配置
                saveConfigIfNeeded();
                
                if (countDownLatch != null) countDownLatch.countDown();
                try {
                    setMtu(64);
                } catch (Exception e) {
                }
            }

        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String characteristicUUID = characteristic.getUuid().toString();
            String serviceUUID = characteristic.getService().getUuid().toString();
            CharacteristicDomain characteristicDataDto = serviceDataDtoMap.get(serviceUUID).getCharacterMap().get(characteristicUUID);

            try {
                if (characteristic.getValue() == null || characteristic.getValue().length <= 0) {
                    LogUtil.debug("BluetoothGattCallback onCharacteristicRead：value is null");
                    return;
                } else {
                    LogUtil.debug("BluetoothGattCallback onCharacteristicRead  " + (characteristicDataDto.getName() != null ? characteristicDataDto.getName() : "?") + "   :" + ByteUtil.bytes2HexString(characteristic.getValue()));
                }
                characteristicDataDto.setValues(characteristic.getValue());
                // 检查 valType 是否为 null，避免 NullPointerException
                Integer valType = characteristicDataDto.getValType();
                if (valType != null) {
                    characteristicDataDto.setRealVal(DataConvert.convert2Obj(characteristicDataDto.getValues(), valType));
                } else {
                    // valType 为 null 时，直接设置为原始字节数组的十六进制字符串表示
                    LogUtil.debug("BluetoothGattCallback onCharacteristicRead：valType is null, skip convert2Obj");
                    characteristicDataDto.setRealVal(null);
                }
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.error("DataConvert.convert2Obj ====>" + new Gson().toJson(characteristicDataDto));
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            LogUtil.debug("BluetoothGattCallback onCharacteristicRead2：" + ByteUtil.bytes2HexString(characteristic.getValue()));
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            LogUtil.debug("BluetoothGattCallback onCharacteristicWrite：" + ByteUtil.bytes2HexString(characteristic.getValue()));
            if (countDownLatch != null) countDownLatch.countDown();
        }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String s = new String(characteristic.getValue(), StandardCharsets.US_ASCII);
            LogUtil.debug("BluetoothGattCallback onCharacteristicChanged:" + s);
            notifyBuff.append(s);
        }


        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
            LogUtil.debug("BluetoothGattCallback onDescriptorRead :" + ByteUtil.bytes2HexString(descriptor.getValue()));

        }


        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

    public StringBuilder getNotifyBuff() {
        return notifyBuff;
    }


}
