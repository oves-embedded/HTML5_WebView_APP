package com.example.myapplication.entity.js;

import com.example.myapplication.enums.ServiceNameEnum;

import java.io.Serializable;
import java.util.List;

public class ServicesPropertiesDto implements Serializable {

    private String uuid;

    private String serviceProperty;

    private String type;

    private ServiceNameEnum serviceNameEnum;


    private List<CharacteristicDto>characteristicList;


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

    public List<CharacteristicDto> getCharacteristicList() {
        return characteristicList;
    }

    public void setCharacteristicList(List<CharacteristicDto> characteristicList) {
        this.characteristicList = characteristicList;
    }
}
