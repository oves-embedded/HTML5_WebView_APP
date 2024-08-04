package com.example.myapplication.entity.js;

import com.example.myapplication.entity.ServicesPropertiesDomain;

import java.util.List;

public class BleData {
    private String macAddress;

    private List<ServicesPropertiesDomain> dataList;

    public BleData() {
    }

    public BleData(String macAddress, List<ServicesPropertiesDomain> dataList) {
        this.macAddress = macAddress;
        this.dataList = dataList;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public List<ServicesPropertiesDomain> getDataList() {
        return dataList;
    }

    public void setDataList(List<ServicesPropertiesDomain> dataList) {
        this.dataList = dataList;
    }
}
