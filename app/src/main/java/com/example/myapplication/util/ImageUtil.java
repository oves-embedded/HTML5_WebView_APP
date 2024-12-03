package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtil {

    /**
     * 将图片转换成Base64编码的字符串
     */
    public static String imageToBase64(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        InputStream is = null;
        byte[] data = null;
        String result = null;
        try {
            is = new FileInputStream(path);
            //创建一个字符流大小的数组。
            data = new byte[is.available()];
            //写入数组
            is.read(data);
            //用默认的编码格式进行编码
            result = Base64.encodeToString(data, Base64.NO_CLOSE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }


    /**
     * 将Base64编码转换为图片
     *
     * @param base64Str
     * @param path
     * @return true
     */
    public static boolean base64ToFile(String base64Str, String path) {
        byte[] data = Base64.decode(base64Str, Base64.NO_WRAP);
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0) {
                //调整异常数据
                data[i] += 256;
            }
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(path);
            os.write(data);
            os.flush();
            os.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Uri saceCacheImageAndGetUri(Context context, String base64String) throws IOException {
        // 创建一个临时文件来存储图片
        File tempFile = File.createTempFile("image", ".jpg", context.getExternalCacheDir());
        // 将字节数组写入文件
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(Base64.decode(base64String, Base64.DEFAULT));
            fos.flush();
        }
        // 获取文件的Uri
        return Uri.fromFile(tempFile);
    }

    public static boolean saveImageToGallery(Context context,Bitmap bitmap) {
        if(bitmap==null){
            return false;
        }
        // 获取图片存储的路径
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        // 创建文件
        File file = new File(path, "image.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            // 通知图库更新
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), "image", null);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean saveImageToGallery(Context context,String imgBase64) {
        imgBase64 = base64ImgTrimPre(imgBase64);
        if(TextUtils.isEmpty(imgBase64)){
            return false;
        }
        Bitmap bitmap = base64ToBitmap(imgBase64);
        // 获取图片存储的路径
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        // 创建文件
        File file = new File(path, "image.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            // 通知图库更新
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), "image", null);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Bitmap base64ToBitmap(String base64Image) {
        try {
            // 解码 Base64 字符串
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            // 将字节数组转换为 Bitmap 对象
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null; // 返回 null，表示解码失败
        }
    }

    public static String base64ImgTrimPre(String base64Image){
        if(TextUtils.isEmpty(base64Image)){
            return "";
        }
        // 检查 src 是否是一个 base64 编码的图片
        if (base64Image.startsWith("data:image")) {
            // 去掉 data:image/...;base64, 前缀
            int base64StartIndex = base64Image.indexOf(",") + 1;
            base64Image=base64Image.substring(base64StartIndex);
        }
        return base64Image;
    }
}
