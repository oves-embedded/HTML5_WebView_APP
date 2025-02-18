package com.example.myapplication.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.util.ImageUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.hjq.toast.Toaster;
import com.orhanobut.logger.Logger;
import com.theartofdev.edmodo.cropper.CropImageView;


public class OcrActivity extends AppCompatActivity {

    CropImageView cropImageView;
    TitleBar titleBar;
    EditText et_result;
    ImageButton ib_recognize;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        cropImageView = findViewById(R.id.cropImageView);
        et_result=findViewById(R.id.et_result);
        ib_recognize=findViewById(R.id.ib_recognize);
        titleBar=findViewById(R.id.titleBar);
        cropImageView.setFixedAspectRatio(false);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);
        cropImageView.setShowCropOverlay(true);
        cropImageView.setCropShape(CropImageView.CropShape.RECTANGLE);
        cropImageView.setScaleType(CropImageView.ScaleType.CENTER_INSIDE);

        String uri = getIntent().getStringExtra("uri");

        if(uri==null||uri.length()==0){
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ocrStr", et_result.getText().toString());
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        cropImageView.setImageUriAsync(Uri.parse(uri));
        titleBar.setOnTitleBarListener(new OnTitleBarListener() {
            @Override
            public void onLeftClick(TitleBar titleBar) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("ocrStr", et_result.getText().toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onTitleClick(TitleBar titleBar) {
                OnTitleBarListener.super.onTitleClick(titleBar);

            }

            @Override
            public void onRightClick(TitleBar titleBar) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("ocrStr", et_result.getText().toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        ib_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOcr();
            }
        });
    }


    public void startOcr() {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Bitmap croppedImage = cropImageView.getCroppedImage();
        if(croppedImage==null){
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ocrStr", et_result.getText().toString());
            setResult(RESULT_OK, resultIntent);
            Toaster.show("Image Error");
            finish();
            return;
        }
        InputImage image = InputImage.fromBitmap(croppedImage, 0);
        Task<Text> result1 =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text text) {
                                String resultText = text.getText();
                                String test = "";
                                for (Text.TextBlock block : text.getTextBlocks()) {
                                    String blockText = block.getText();
                                    Logger.d(blockText);
                                    test += blockText;
//                                    Point[] blockCornerPoints = block.getCornerPoints();
//                                    Rect blockFrame = block.getBoundingBox();
//                                    for (Text.Line line : block.getLines()) {
//                                        String lineText = line.getText();
//                                        Point[] lineCornerPoints = line.getCornerPoints();
//                                        Rect lineFrame = line.getBoundingBox();
//                                        for (Text.Element element : line.getElements()) {
//                                            String elementText = element.getText();
//                                            Point[] elementCornerPoints = element.getCornerPoints();
//                                            Rect elementFrame = element.getBoundingBox();
//                                            for (Text.Symbol symbol : element.getSymbols()) {
//                                                String symbolText = symbol.getText();
//                                                Point[] symbolCornerPoints = symbol.getCornerPoints();
//                                                Rect symbolFrame = symbol.getBoundingBox();
//                                            }
//                                        }
//                                    }
                                }
                                et_result.setText(test==null?"":test);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                        recognizer.close();

                                        Toaster.show("OCR ERROR!");
                                    }
                                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
