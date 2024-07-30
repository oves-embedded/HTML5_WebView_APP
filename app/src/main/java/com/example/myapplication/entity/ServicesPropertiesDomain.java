package com.example.myapplication.entity;

import com.example.myapplication.enums.ServiceNameEnum;
import com.google.gson.Gson;

import java.util.Map;

public class ServicesPropertiesDomain {

    private String uuid;

    private String serviceProperty;

    private String type;

    private String deviceOpid;

    private ServiceNameEnum serviceNameEnum;

    private Map<String,CharacteristicDomain> characterMap;


    public Map<String, CharacteristicDomain> getCharacterMap() {
        return characterMap;
    }

    public void setCharacterMap(Map<String, CharacteristicDomain> characterMap) {
        this.characterMap = characterMap;
    }

    public String getDeviceOpid() {
        return deviceOpid;
    }

    public void setDeviceOpid(String deviceOpid) {
        this.deviceOpid = deviceOpid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServiceProperty() {
        return serviceProperty;
    }

    public void setServiceProperty(String serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ServiceNameEnum getServiceNameEnum() {
        return serviceNameEnum;
    }

    public void setServiceNameEnum(ServiceNameEnum serviceNameEnum) {
        this.serviceNameEnum = serviceNameEnum;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
