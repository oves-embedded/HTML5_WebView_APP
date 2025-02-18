package com.example.myapplication.entity;


import com.google.gson.Gson;

public class BleDeviceInfo {

    private int rssi;
    private String macAddress;
    private String name;


    public BleDeviceInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }



    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
