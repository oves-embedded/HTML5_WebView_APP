package com.example.myapplication.activity;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.entity.main.ItemConfig;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.util.ImageUtil;
import com.example.myapplication.util.SharedPreferencesUtils;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;
import com.orhanobut.logger.Logger;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class FirstActivity extends AppCompatActivity {

    EditText etWebSite;

    Button btnWebSite;

    Button btnDemo;
    Button btnImageCropper;

    private static final String TAG = "FirstActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        etWebSite = findViewById(R.id.etWebsite);
        btnDemo = findViewById(R.id.btnDemo);
        btnWebSite = findViewById(R.id.btnWebSite);
        btnImageCropper = findViewById(R.id.btnImageCropper);
        String cacheUrl= (String) SharedPreferencesUtils.getParam(FirstActivity.this,TAG,"");
        etWebSite.setText(cacheUrl);

        btnDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstActivity.this, WebViewActivity.class);

//                MainConfig mainConfig=new MainConfig();
//                mainConfig.setBarBackgroundColor("#A3A6A1");
//                mainConfig.setItemBackgroundColor("#ffffff");
//                mainConfig.setItemSelBackgroundColor("#000000");
//                mainConfig.setItemTextColor("#000000");
//                mainConfig.setItemSelTextColor("#202ED1");
//
//                List<ItemConfig> list = new ArrayList<ItemConfig>();
//                ItemConfig itemConfig = new ItemConfig();
//                itemConfig.setContentUrl("https://www.baidu.com/");
//                itemConfig.setIconUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig.setIconSelUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig.setSortIndex(0);
//                itemConfig.setItemText("baidu");
//                list.add(itemConfig);
//
//                ItemConfig itemConfig2 = new ItemConfig();
//                itemConfig2.setContentUrl("https://www.sougou.com/");
//                itemConfig2.setIconUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig2.setIconSelUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig2.setSortIndex(3);
//                itemConfig2.setItemText("sougou");
//                list.add(itemConfig2);
//
//                ItemConfig itemConfig4 = new ItemConfig();
//                itemConfig4.setContentUrl("https://cn.bing.com/");
//                itemConfig4.setIconUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig4.setIconSelUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig4.setSortIndex(1);
//                itemConfig4.setItemText("bing");
//                list.add(itemConfig4);
//
//                ItemConfig itemConfig3 = new ItemConfig();
//                itemConfig3.setContentUrl("https://www.google.com/");
//                itemConfig3.setIconUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig3.setIconSelUrl("https://tse4-mm.cn.bing.net/th/id/OIP-C.MD5FdM4LTeNRm9dUmRasVgHaHa?rs=1&pid=ImgDetMain");
//                itemConfig3.setSortIndex(2);
//                itemConfig3.setItemText("google");
//                list.add(itemConfig3);
//                mainConfig.setItems(list);
//
//                intent.putExtra("data",new Gson().toJson(mainConfig));
                startActivity(intent);
            }
        });
        btnWebSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = etWebSite.getText().toString();
                if (TextUtils.isEmpty(s)) {
                    Toaster.show("Please enter the URL of the page you want to navigate to");
                    return;
                }

                SharedPreferencesUtils.setParam(FirstActivity.this,TAG,s);

                Intent intent = new Intent(FirstActivity.this, WebViewActivity.class);
                intent.putExtra("url", s.trim());
                startActivity(intent);
            }
        });
        btnImageCropper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 参数2：设置BottomSheetDialog的主题样式；将背景设置为transparent，这样我们写的shape_bottom_sheet_dialog.xml才会起作用
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(FirstActivity.this, R.style.BottomSheetDialog);
                //不传第二个参数
                //BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                // 底部弹出的布局
                View bottomView = LayoutInflater.from(FirstActivity.this).inflate(R.layout.bottom_sheet_layout, null);
                bottomSheetDialog.setContentView(bottomView);

                TextView tvPhoto = (TextView) bottomView.findViewById(R.id.choose_photo);
                TextView tvCamera = (TextView) bottomView.findViewById(R.id.choose_camera);
                TextView tvCancel = (TextView) bottomView.findViewById(R.id.cancel);
                tvPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.dismiss();

                        //	启动取景器获取用于裁剪的图像，然后在裁剪Activity中使用该图像
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setShowCropOverlay(true)
                                .start(FirstActivity.this);
                    }
                });

                tvCamera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 跳转到相机
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, 100);
                        bottomSheetDialog.dismiss();
                    }
                });
                tvCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomSheetDialog.dismiss();
                    }
                });

                //设置点击dialog外部不消失
                //bottomSheetDialog.setCanceledOnTouchOutside(false);
                bottomSheetDialog.show();
//                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(intent, 110);
//                CropImage.activity()
//                        .setGuidelines(CropImageView.Guidelines.ON) //开启选择器
//                        .setActivityTitle("头像裁剪")
//                        .setCropShape(CropImageView.CropShape.RECTANGLE)  //选择矩形裁剪
//                        .start(FirstActivity.this);
            }
        });
        XXPermissions.with(this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION)
                .interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            return;
                        }
                    }
                });





    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //用户没有进行有效的设置操作，返回
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplication(), "取消", Toast.LENGTH_LONG).show();
            return;
        }

        switch (requestCode) {
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    final Uri resultUri = result.getUri();  //获取裁减后的图片的Uri

                    String base64 = ImageUtil.imageToBase64(resultUri.getPath());
                    Uri uri = null;
                    try {
                        uri = ImageUtil.saceCacheImageAndGetUri(this, base64);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(FirstActivity.this, OcrActivity.class);
                    intent.putExtra("uri",uri.toString());
                    startActivity(intent);
//                    String s1 = FirstActivity.this.getCacheDir() + File.separator + UUID.randomUUID().toString();
//                    try {
//                        Uri uri = ImageUtil.saveImageAndGetUri(this, s);
//                        CropImage.activity(uri)
//                                .setGuidelines(CropImageView.Guidelines.ON)
//                                .setShowCropOverlay(true)
//                                .start(FirstActivity.this);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }

                    return;
//                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
//
//                    InputImage image=null;
//                    try {
//                        image = InputImage.fromFilePath(FirstActivity.this, resultUri);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    Task<Text> result1 =
//                            recognizer.process(image)
//                                    .addOnSuccessListener(new OnSuccessListener<Text>() {
//                                        @Override
//                                        public void onSuccess(Text text) {
//                                            String resultText = text.getText();
//                                            String test="";
//                                            for (Text.TextBlock block : text.getTextBlocks()) {
//                                                String blockText = block.getText();
//                                                Logger.d(blockText);
//                                                test+=blockText;
//                                                Point[] blockCornerPoints = block.getCornerPoints();
//                                                Rect blockFrame = block.getBoundingBox();
//                                                for (Text.Line line : block.getLines()) {
//                                                    String lineText = line.getText();
//                                                    Point[] lineCornerPoints = line.getCornerPoints();
//                                                    Rect lineFrame = line.getBoundingBox();
//                                                    for (Text.Element element : line.getElements()) {
//                                                        String elementText = element.getText();
//                                                        Point[] elementCornerPoints = element.getCornerPoints();
//                                                        Rect elementFrame = element.getBoundingBox();
//                                                        for (Text.Symbol symbol : element.getSymbols()) {
//                                                            String symbolText = symbol.getText();
//                                                            Point[] symbolCornerPoints = symbol.getCornerPoints();
//                                                            Rect symbolFrame = symbol.getBoundingBox();
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                            Logger.d(test);
//
//                                        }
//                                    })
//                                    .addOnFailureListener(
//                                            new OnFailureListener() {
//                                                @Override
//                                                public void onFailure(@NonNull Exception e) {
//                                                    e.printStackTrace();
//                                                    recognizer.close();
//                                                }
//                                            });


                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Log.d("PhotoActivity", "onActivityResult: Error");
                    Exception exception = result.getError();
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
