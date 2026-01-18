package com.example.myapplication.util;

import android.content.Context;
import android.text.TextUtils;

import com.example.myapplication.entity.ProductTypeConfig;
import com.example.myapplication.entity.ServicesPropertiesDomain;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 产品类型配置管理器
 * 负责提取产品类型、加载/保存配置
 */
public class ProductTypeConfigManager {
    
    private static final String PREFIX_KEY = "PRODUCT_TYPE_CONFIG_";
    private static Map<String, ProductTypeConfig> configCache = new ConcurrentHashMap<>();
    
    /**
     * 从设备名称提取产品类型
     * 设备名称格式: "OVES DFAN XXXXXX" (公司名 产品类型 设备ID后6位)
     * 
     * @param deviceName 设备名称
     * @return 产品类型，如 "DFAN"，如果无法解析返回null
     */
    public static String extractProductType(String deviceName) {
        if (TextUtils.isEmpty(deviceName)) {
            return null;
        }
        
        String trimmedName = deviceName.trim();
        // 检查是否以 "OVES " 开头
        if (trimmedName.startsWith("OVES ")) {
            String[] parts = trimmedName.split("\\s+");
            // 期望格式: ["OVES", "DFAN", "XXXXXX"]
            if (parts.length >= 2) {
                return parts[1].toUpperCase(); // 返回产品类型，转为大写
            }
        }
        
        return null;
    }
    
    /**
     * 获取产品类型配置
     * 优先级：内存缓存 > SharedPreferences > null
     * 
     * @param context 上下文
     * @param productType 产品类型
     * @return 产品类型配置，如果不存在返回null
     */
    public static ProductTypeConfig getConfig(Context context, String productType) {
        if (TextUtils.isEmpty(productType)) {
            return null;
        }
        
        // 1. 先查内存缓存
        ProductTypeConfig config = configCache.get(productType);
        if (config != null) {
            return config;
        }
        
        // 2. 从SharedPreferences加载
        if (context != null) {
            config = loadConfigFromPrefs(context, productType);
            if (config != null) {
                // 加载到内存缓存
                configCache.put(productType, config);
                return config;
            }
        }
        
        return null;
    }
    
    /**
     * 保存产品类型配置到本地
     * 使用 SharedPreferencesUtils 工具类保存配置
     * 
     * @param context 上下文
     * @param productType 产品类型
     * @param config 配置对象
     */
    public static void saveConfig(Context context, String productType, ProductTypeConfig config) {
        if (context == null || TextUtils.isEmpty(productType) || config == null) {
            return;
        }
        
        try {
            // 1. 保存到内存缓存
            configCache.put(productType, config);
            
            // 2. 使用 SharedPreferencesUtils 保存到本地（序列化为JSON字符串）
            String configJson = new Gson().toJson(config);
            String key = PREFIX_KEY + productType;
            SharedPreferencesUtils.setParam(context, key, configJson);
            
            LogUtil.debug("ProductTypeConfig saved for: " + productType);
        } catch (Exception e) {
            LogUtil.error("Failed to save ProductTypeConfig: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从SharedPreferences加载配置
     * 使用 SharedPreferencesUtils 工具类加载配置
     */
    private static ProductTypeConfig loadConfigFromPrefs(Context context, String productType) {
        try {
            String key = PREFIX_KEY + productType;
            // 使用 SharedPreferencesUtils 获取配置JSON字符串
            Object configObj = SharedPreferencesUtils.getParam(context, key, "");
            String configJson = configObj != null ? (String) configObj : null;
            
            if (!TextUtils.isEmpty(configJson)) {
                ProductTypeConfig config = new Gson().fromJson(configJson, ProductTypeConfig.class);
                LogUtil.debug("ProductTypeConfig loaded from prefs for: " + productType);
                return config;
            }
        } catch (Exception e) {
            LogUtil.error("Failed to load ProductTypeConfig from prefs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 从当前的serviceDataDtoMap构建ProductTypeConfig
     * 用于首次连接后保存配置
     * 
     * @param productType 产品类型
     * @param serviceDataDtoMap 当前已构建的Service数据Map
     * @return ProductTypeConfig对象
     */
    public static ProductTypeConfig buildConfigFromServiceData(String productType, 
                                                               Map<String, ServicesPropertiesDomain> serviceDataDtoMap) {
        ProductTypeConfig config = new ProductTypeConfig();
        config.setProductType(productType);
        
        Map<String, ProductTypeConfig.ServiceConfig> services = new HashMap<>();
        
        for (ServicesPropertiesDomain serviceDomain : serviceDataDtoMap.values()) {
            ProductTypeConfig.ServiceConfig serviceConfig = new ProductTypeConfig.ServiceConfig();
            serviceConfig.setServiceUUID(serviceDomain.getUuid());
            serviceConfig.setServiceProperty(serviceDomain.getServiceProperty());
            
            if (serviceDomain.getServiceNameEnum() != null) {
                serviceConfig.setServiceNameEnum(serviceDomain.getServiceNameEnum().getServiceName());
            }
            
            Map<String, ProductTypeConfig.CharacteristicConfig> characteristics = new HashMap<>();
            
            if (serviceDomain.getCharacterMap() != null) {
                for (com.example.myapplication.entity.CharacteristicDomain charDomain : serviceDomain.getCharacterMap().values()) {
                    ProductTypeConfig.CharacteristicConfig charConfig = new ProductTypeConfig.CharacteristicConfig();
                    charConfig.setCharacteristicUUID(charDomain.getUuid());
                    charConfig.setName(charDomain.getName());
                    charConfig.setValType(charDomain.getValType());
                    charConfig.setDesc(charDomain.getDesc());
                    charConfig.setProperties(charDomain.getProperties());
                    charConfig.setServiceUuid(charDomain.getServiceUuid());
                    // 保存所有enable标志（这些是根据properties计算出来的，但为了完整性也保存）
                    charConfig.setEnableWrite(charDomain.isEnableWrite());
                    charConfig.setEnableRead(charDomain.isEnableRead());
                    charConfig.setEnableIndicate(charDomain.isEnableIndicate());
                    charConfig.setEnableNotify(charDomain.isEnableNotify());
                    charConfig.setEnableWriteNoResp(charDomain.isEnableWriteNoResp());
                    // 注意：values 和 realVal 不保存，因为它们会在读取时更新
                    
                    // 收集Descriptor UUIDs
                    List<String> descriptorUUIDs = new ArrayList<>();
                    if (charDomain.getDescMap() != null) {
                        for (com.example.myapplication.entity.DescriptorDomain descDomain : charDomain.getDescMap().values()) {
                            descriptorUUIDs.add(descDomain.getUuid());
                        }
                    }
                    charConfig.setDescriptorUUIDs(descriptorUUIDs);
                    
                    characteristics.put(charDomain.getUuid(), charConfig);
                }
            }
            
            serviceConfig.setCharacteristics(characteristics);
            services.put(serviceDomain.getUuid(), serviceConfig);
        }
        
        config.setServices(services);
        return config;
    }
    
    /**
     * 清除内存缓存（可选，用于调试或内存管理）
     */
    public static void clearCache() {
        configCache.clear();
    }
}

