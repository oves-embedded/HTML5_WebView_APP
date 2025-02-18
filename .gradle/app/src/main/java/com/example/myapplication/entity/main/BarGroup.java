package com.example.myapplication.entity.main;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BarGroup {

    LinearLayout llGroup;
    ImageView imageView;
    TextView textView;

    public LinearLayout getLlGroup() {
        return llGroup;
    }

    public void setLlGroup(LinearLayout llGroup) {
        this.llGroup = llGroup;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public TextView getTextView() {
        return textView;
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }
}
