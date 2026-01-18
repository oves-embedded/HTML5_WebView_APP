package com.example.myapplication.entity;

import com.example.myapplication.enums.ServiceNameEnum;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 产品类型配置实体类
 * 用于缓存每个产品类型对应的Service、Characteristic、Descriptor UUID配置
 */
public class ProductTypeConfig implements Serializable {
    
    @SerializedName("productType")
    private String productType; // 产品类型，如 "DFAN"
    
    @SerializedName("services")
    private Map<String, ServiceConfig> services; // Service UUID -> ServiceConfig
    
    public String getProductType() {
        return productType;
    }
    
    public void setProductType(String productType) {
        this.productType = productType;
    }
    
    public Map<String, ServiceConfig> getServices() {
        return services;
    }
    
    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
    
    /**
     * Service配置
     */
    public static class ServiceConfig implements Serializable {
        @SerializedName("serviceUUID")
        private String serviceUUID;
        
        @SerializedName("serviceNameEnum")
        private String serviceNameEnum; // ServiceNameEnum的name值，如 "att", "cmd"等
        
        @SerializedName("serviceProperty")
        private String serviceProperty; // "PRIMARY SERVICE" or "SECONDARY SERVICE"
        
        @SerializedName("characteristics")
        private Map<String, CharacteristicConfig> characteristics; // Characteristic UUID -> CharacteristicConfig
        
        public String getServiceUUID() {
            return serviceUUID;
        }
        
        public void setServiceUUID(String serviceUUID) {
            this.serviceUUID = serviceUUID;
        }
        
        public String getServiceNameEnum() {
            return serviceNameEnum;
        }
        
        public void setServiceNameEnum(String serviceNameEnum) {
            this.serviceNameEnum = serviceNameEnum;
        }
        
        public String getServiceProperty() {
            return serviceProperty;
        }
        
        public void setServiceProperty(String serviceProperty) {
            this.serviceProperty = serviceProperty;
        }
        
        public Map<String, CharacteristicConfig> getCharacteristics() {
            return characteristics;
        }
        
        public void setCharacteristics(Map<String, CharacteristicConfig> characteristics) {
            this.characteristics = characteristics;
        }
        
        /**
         * 转换为ServiceNameEnum
         */
        public ServiceNameEnum getServiceNameEnumValue() {
            if (serviceNameEnum == null) return null;
            for (ServiceNameEnum enumValue : ServiceNameEnum.values()) {
                if (enumValue.getServiceName().equalsIgnoreCase(serviceNameEnum)) {
                    return enumValue;
                }
            }
            return null;
        }
    }
    
    /**
     * Characteristic配置
     */
    public static class CharacteristicConfig implements Serializable {
        @SerializedName("characteristicUUID")
        private String characteristicUUID;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("valType")
        private Integer valType; // 数据类型：0-5
        
        @SerializedName("desc")
        private String desc;
        
        @SerializedName("properties")
        private Integer properties; // Characteristic属性
        
        @SerializedName("serviceUuid")
        private String serviceUuid; // 所属Service的UUID
        
        @SerializedName("enableWrite")
        private boolean enableWrite = false;
        
        @SerializedName("enableRead")
        private boolean enableRead = false;
        
        @SerializedName("enableIndicate")
        private boolean enableIndicate = false;
        
        @SerializedName("enableNotify")
        private boolean enableNotify = false;
        
        @SerializedName("enableWriteNoResp")
        private boolean enableWriteNoResp = false;
        
        @SerializedName("descriptorUUIDs")
        private List<String> descriptorUUIDs; // 该Characteristic包含的Descriptor UUID列表
        
        public String getCharacteristicUUID() {
            return characteristicUUID;
        }
        
        public void setCharacteristicUUID(String characteristicUUID) {
            this.characteristicUUID = characteristicUUID;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Integer getValType() {
            return valType;
        }
        
        public void setValType(Integer valType) {
            this.valType = valType;
        }
        
        public String getDesc() {
            return desc;
        }
        
        public void setDesc(String desc) {
            this.desc = desc;
        }
        
        public Integer getProperties() {
            return properties;
        }
        
        public void setProperties(Integer properties) {
            this.properties = properties;
        }
        
        public List<String> getDescriptorUUIDs() {
            return descriptorUUIDs;
        }
        
        public void setDescriptorUUIDs(List<String> descriptorUUIDs) {
            this.descriptorUUIDs = descriptorUUIDs;
        }
        
        public String getServiceUuid() {
            return serviceUuid;
        }
        
        public void setServiceUuid(String serviceUuid) {
            this.serviceUuid = serviceUuid;
        }
        
        public boolean isEnableWrite() {
            return enableWrite;
        }
        
        public void setEnableWrite(boolean enableWrite) {
            this.enableWrite = enableWrite;
        }
        
        public boolean isEnableRead() {
            return enableRead;
        }
        
        public void setEnableRead(boolean enableRead) {
            this.enableRead = enableRead;
        }
        
        public boolean isEnableIndicate() {
            return enableIndicate;
        }
        
        public void setEnableIndicate(boolean enableIndicate) {
            this.enableIndicate = enableIndicate;
        }
        
        public boolean isEnableNotify() {
            return enableNotify;
        }
        
        public void setEnableNotify(boolean enableNotify) {
            this.enableNotify = enableNotify;
        }
        
        public boolean isEnableWriteNoResp() {
            return enableWriteNoResp;
        }
        
        public void setEnableWriteNoResp(boolean enableWriteNoResp) {
            this.enableWriteNoResp = enableWriteNoResp;
        }
    }
}

