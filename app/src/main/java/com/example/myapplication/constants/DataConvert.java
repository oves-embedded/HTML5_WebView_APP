package com.example.myapplication.constants;

import android.text.TextUtils;


import com.example.myapplication.util.ByteUtil;

import java.nio.charset.StandardCharsets;

public class DataConvert {

    public static Object convert2Obj(byte[] b, int valType) {
        try{
            if (b != null && b.length > 0) {
                switch (valType) {
                    case 0:
                    case 1:
                        return ByteUtil.byte2short(new byte[]{b[1], b[0]});
                    case 2:
                    case 3:
//                        if(b.length!=4){
//                            LogUtil.error("非法参数：b==>"+ByteUtil.bytes2HexString(b)+"  valType==>"+valType);
//                        }
//                        return ByteUtil.byte2int(ByteUtil.reverse(b));
                        return ByteUtil.byte2int(new byte[]{b[3],b[2],b[1],b[0]});
                    case 4:
                        return null;
                    case 5:
                        return new String(b, StandardCharsets.US_ASCII).trim();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] convert2Arr(String value, int valType) {
        if (!TextUtils.isEmpty(value)) {
            switch (valType) {
                case 0:
                    byte[] bytes = ByteUtil.short2byte(Integer.valueOf(value));
                    return ByteUtil.reverse(bytes);
                case 1:
                    return ByteUtil.reverse(ByteUtil.short2byte(Integer.valueOf(value)));
                case 2:
                case 3:
                    return ByteUtil.reverse(ByteUtil.int2byte(Integer.valueOf(value)));
                case 4:
                    break;
                case 5:
                    return value.getBytes(StandardCharsets.US_ASCII);
            }
        }
        return null;
    }
}
