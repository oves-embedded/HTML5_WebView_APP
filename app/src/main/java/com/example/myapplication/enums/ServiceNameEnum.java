package com.example.myapplication.enums;

public enum ServiceNameEnum {
    ATT_SERVICE("9b071","att"),
    CMD_SERVICE("9b072","cmd"),
    STS_SERVICE("9b073","sts"),
    DTA_SERVICE("9b074","dta"),
    DIA_SERVICE("9b075","dia"),
    ;

    private String prefixCode;
    private String serviceName;

    ServiceNameEnum(String prefixCode, String serviceName) {
        this.prefixCode = prefixCode;
        this.serviceName = serviceName;
    }

    public String getPrefixCode() {
        return prefixCode;
    }


    public String getServiceName() {
        return serviceName;
    }

    public static ServiceNameEnum  getServiceNameFromUUID(String serviceUUID){
        ServiceNameEnum[] values = values();
        for(ServiceNameEnum serviceNameEnum:values){
            if(serviceUUID.startsWith(serviceNameEnum.getPrefixCode())){
                return serviceNameEnum;
            }
        }
        return null;
    }

    public static boolean contain(String serviceUUID){
        return getServiceNameFromUUID(serviceUUID)!=null;
    }
}
