package com.example.myapplication.constants;

/**
 * @auther: Bob;
 * @phone:17688910400;
 * @email:932458047@qq.com
 * @BelongsPackage: com.yk.iot.bbase.constants
 * @CreateTime: 2023-09-07  10:03
 * @Description:
 * @Version: 1.0
 */
public enum RetCode {
    // 系统公共返回码
    SUCCESS("200", "Execution successful"),
    FAIL("1", "Execution failed"),
    BLE_NOT_SUPPORTED("2", "Phone does not support Bluetooth"),
    BLE_SERVICE_NOT_INIT("3", "Bluetooth service initialization failed"),
    BLE_DEVICE_NOT_FIND("4", "Bluetooth device not found"),
    RUNTIME_EXCEPTION("5", "Runtime exception"),
    BLE_IS_NOT_ENABLED("6", "Bluetooth is not enabled"),
    BLE_MAC_ADDRESS_NOT_MATCH("7", "Bluetooth macAddress is not match"),
    BLE_NOT_CONNECTED("8", "Bluetooth device not connected"),
    USER_CANCELED_OPERATION("9", "User canceled the operation"),
    PERMISSION_ERROR("9", "User has not granted the appropriate permissions"),
    PARAMETER_ERROR("10", "Parameter error"),
    MQTT_CONNECT_FAIL("11", "MQTT connection failed"),
    MQTT_CURRENT_NOT_CONNECTED("12", "MQTT is currently not connected"),
    METHOD_INVOCATION_EXCEPTION("000", "Method invocation exception");


    private String code;
    private String desc;

    RetCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
