package com.example.myapplication.callback;

import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.ServicesPropertiesDomain;

import java.util.Map;

public interface InitBleDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(Map<String, ServicesPropertiesDomain>data);

    void onFailure(String error);



}
