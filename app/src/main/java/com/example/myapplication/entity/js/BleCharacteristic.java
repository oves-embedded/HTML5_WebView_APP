package com.example.myapplication.entity.js;

import com.example.myapplication.entity.CharacteristicDomain;

public class BleCharacteristic {
    private String macAddress;
    private CharacteristicDomain characteristicDomain;

    public BleCharacteristic() {
    }

    public BleCharacteristic(String macAddress, CharacteristicDomain characteristicDomain) {
        this.macAddress = macAddress;
        this.characteristicDomain = characteristicDomain;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public CharacteristicDomain getCharacteristicDomain() {
        return characteristicDomain;
    }

    public void setCharacteristicDomain(CharacteristicDomain characteristicDomain) {
        this.characteristicDomain = characteristicDomain;
    }
}
