package com.example.myapplication.entity.js;

import com.example.myapplication.entity.DescriptorDomain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CharacteristicDto implements Serializable {

    private String uuid;
    private String name;
    private Integer properties;
    private String desc;
    private byte[] values;
    private Integer valType;
    private Object realVal;
    private String serviceUuid;
    private List<DescriptorDomain> descriptors;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getProperties() {
        return properties;
    }

    public void setProperties(Integer properties) {
        this.properties = properties;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public byte[] getValues() {
        return values;
    }

    public void setValues(byte[] values) {
        this.values = values;
    }

    public Integer getValType() {
        return valType;
    }

    public void setValType(Integer valType) {
        this.valType = valType;
    }

    public Object getRealVal() {
        return realVal;
    }

    public void setRealVal(Object realVal) {
        this.realVal = realVal;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public List<DescriptorDomain> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<DescriptorDomain> descriptors) {
        this.descriptors = descriptors;
    }
}
