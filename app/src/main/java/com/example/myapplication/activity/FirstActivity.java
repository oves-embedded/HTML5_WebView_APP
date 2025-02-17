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
    Button btnBleScanner;
    private static final String TAG = "FirstActivity";
    private static final String DEFAULT_URL = "https://wvapp.omnivoltaic.com/"; // Set your default URL here

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get cached URL or use default
        String url = (String) SharedPreferencesUtils.getParam(FirstActivity.this, TAG, DEFAULT_URL);

        // Directly start WebViewActivity with the URL
        Intent intent = new Intent(FirstActivity.this, WebViewActivity.class);
        intent.putExtra("url", url.trim());
        startActivity(intent);

        // Close FirstActivity since we don't need it anymore
        finish();
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
