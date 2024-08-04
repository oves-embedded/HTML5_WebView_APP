package com.example.myapplication.constants;


import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * @auther: Bob;
 * @phone:17688910400;
 * @email:932458047@qq.com
 * @BelongsPackage: com.yk.iot.bbase.constants
 * @CreateTime: 2023-09-07  10:05
 * @Description:
 * @Version: 1.0
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = -2736613200301615469L;

    private String respCode;
    private String respDesc;
    private T respData;

    public Result() {
    }

    private Result(String respCode, String respDesc, T respData) {
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.respData = respData;
    }

    public static <T> Result<T> ok() {
        return newInstance(RetCode.SUCCESS, null);
    }

    public static <T> Result<T> ok(T respData) {
        return newInstance(RetCode.SUCCESS, respData);
    }

    public static <T> Result<T> fail() {
        return new Result<>(RetCode.FAIL.getCode(), RetCode.FAIL.getDesc(), null);
    }

    public static <T> Result<T> fail(RetCode retCode) {
        return new Result<>(retCode.getCode(), retCode.getDesc(), null);
    }

    public static <T> Result<T> fail(RetCode retCode,T respData) {
        return new Result<>(retCode.getCode(), retCode.getDesc(), respData);
    }

    public static <T> Result<T> fail(String respCode,String respDesc,T respData) {
        return new Result<>(respCode, respDesc, respData);
    }

    public static <T> Result<T> fail(String errMsg) {
        return new Result<>(RetCode.FAIL.getCode(), errMsg, null);
    }

    public static <T> Result<T> fail(String respCode, String respDesc) {
        return new Result<>(respCode, respDesc, null);
    }

    public static <T> Result<T> newInstance(Result<T> result) {
        return new Result<>(result.getRespCode(), result.getRespDesc(), null);
    }

    public static <T> Result<T> newInstance(RetCode retCode, T respData) {
        return new Result<>(retCode.getCode(), retCode.getDesc(), respData);
    }

    public static <T> Result<T> newInstance(String respCode, String respDesc, T respData) {
        return new Result<>(respCode, respDesc, respData);
    }

    public boolean success() {
        return RetCode.SUCCESS.getCode().equals(this.getRespCode());
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }

    public String getRespDesc() {
        return respDesc;
    }

    public void setRespDesc(String respDesc) {
        this.respDesc = respDesc;
    }

    public T getRespData() {
        return respData;
    }

    public void setRespData(T respData) {
        this.respData = respData;
    }
}
