package com.example.myapplication.callback;

import com.example.myapplication.entity.CharacteristicDomain;
import com.example.myapplication.entity.ServicesPropertiesDomain;

import java.util.Map;

public interface InitBleServiceDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(ServicesPropertiesDomain domain);

    void onFailure(String error);


}
