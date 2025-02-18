package com.example.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.adapter.FragmentAdapter;
import com.example.myapplication.activity.fragment.WebViewFragment;
import com.example.myapplication.entity.BleDeviceInfo;
import com.example.myapplication.entity.event.EventBusMsg;
import com.example.myapplication.entity.main.BarGroup;
import com.example.myapplication.entity.main.ItemConfig;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.enums.EventBusEnum;
import com.example.myapplication.service.BleService;
import com.example.myapplication.util.LogUtil;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class MainActivity extends AppCompatActivity {
    ViewPager viewPager;
    LinearLayout llBottom;
    List<Fragment> fragments = new CopyOnWriteArrayList<>();
    List<BarGroup> groups = new ArrayList<>();

    private Gson gson = new Gson();

    private FragmentAdapter fragmentAdapter;

    private BleService bleService;

    private MainConfig mainConfig;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
//        etUrl = findViewById(R.id.etUrl);
//        btnConfirm = findViewById(R.id.btnConfirm);
        llBottom = findViewById(R.id.ll_bottom);
        viewPager = findViewById(R.id.viewPager);

        Intent intent = new Intent(this, BleService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                bleService = ((BleService.BleServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_AUTO_CREATE);
        String data = getIntent().getStringExtra("data");
        mainConfig = new Gson().fromJson(data, MainConfig.class);
        initFragment();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMsg message) {
        LogUtil.info("onMessageEvent"+gson.toJson(message));

        try {
            WebViewFragment fragment = (WebViewFragment) fragments.get(viewPager.getCurrentItem());
            BridgeWebView bridgeWebView = fragment.getBridgeWebView();
            if (message.getTagEnum() == EventBusEnum.BLE_FIND) {
                BleDeviceInfo info = (BleDeviceInfo) message.getT();
                bridgeWebView.callHandler("findBleDeviceCallBack", gson.toJson(info), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            }
            else if (message.getTagEnum() == EventBusEnum.BLE_CONNECT) {
                String macAddress = (String) message.getT();
                bridgeWebView.callHandler("bleConnectSuccessCallBack", macAddress, new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            }
            else if (message.getTagEnum() == EventBusEnum.BLE_CONNECT_FAIL) {
                String macAddress = (String) message.getT();
                bridgeWebView.callHandler("bleConnectFailCallBack", macAddress, new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            }else if(message.getTagEnum() == EventBusEnum.MQTT_MSG_ARRIVED){
                bridgeWebView.callHandler("mqttMsgArrivedCallBack", (String)message.getT(), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            }else if(message.getTagEnum() == EventBusEnum.GPS_CHANGE){
                bridgeWebView.callHandler("locationCallBack", (String)message.getT(), new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void initFragment() {
        mainConfig.getItems().sort(new Comparator<ItemConfig>() {
            @Override
            public int compare(ItemConfig o1, ItemConfig o2) {
                if (o1.getSortIndex() > o2.getSortIndex()) {
                    return 1;
                } else if (o1.getSortIndex() < o2.getSortIndex()) {
                    return -1;
                }
                return 0;
            }
        });

        for (int i = 0; i < mainConfig.getItems().size(); i++) {
            ItemConfig itemConfig = mainConfig.getItems().get(i);
            WebViewFragment webViewFragment = new WebViewFragment(itemConfig.getContentUrl());
            fragments.add(webViewFragment);
            // 创建新的 LinearLayout
            LinearLayout innerLinearLayout = new LinearLayout(this);
            int index = i;
            innerLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(index);
                    refreshItemBar();
                }
            });

            LinearLayout.LayoutParams innerParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            innerLinearLayout.setLayoutParams(innerParams);
            innerLinearLayout.setBackgroundColor(Color.parseColor(mainConfig.getItemBackgroundColor()));
            innerLinearLayout.setOrientation(LinearLayout.VERTICAL);
            innerLinearLayout.setPadding(5, 5, 5, 5);
            innerLinearLayout.setGravity(Gravity.CENTER);
            // 创建 ImageView
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(
                    60,
                    60, 1
            );
            imageView.setLayoutParams(imageViewParams);
            Glide.with(this).load(itemConfig.getIconUrl()).error(R.mipmap.ic_launcher).into(imageView);
            // 创建 TextView
            TextView textView = new TextView(this);
            LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            textView.setLayoutParams(textViewParams);
            textView.setText(itemConfig.getItemText());
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(12.0f);

            // 将 ImageView 和 TextView 添加到新的 LinearLayout
            innerLinearLayout.addView(imageView);
            innerLinearLayout.addView(textView);

            // 将新的 LinearLayout 添加到 llBottom
            llBottom.addView(innerLinearLayout);

            BarGroup barGroup = new BarGroup();
            barGroup.setLlGroup(innerLinearLayout);
            barGroup.setImageView(imageView);
            barGroup.setTextView(textView);
            groups.add(barGroup);
        }
        fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(fragmentAdapter);
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 禁止处理触摸事件，从而禁止滑动
                viewPager.setCurrentItem(viewPager.getCurrentItem(), false);
                return true;
            }
        });
        viewPager.setCurrentItem(0);
        refreshItemBar();
    }

    private void refreshItemBar() {
        int currentIndex = viewPager.getCurrentItem();
        for (int i = 0; i < groups.size(); i++) {
            if (currentIndex == i) {
                if (!TextUtils.isEmpty(mainConfig.getItemSelBackgroundColor()))
                    groups.get(i).getLlGroup().setBackgroundColor(Color.parseColor(mainConfig.getItemSelBackgroundColor()));
                Glide.with(this).load(mainConfig.getItems().get(currentIndex).getIconSelUrl()).error(R.mipmap.ic_launcher).into(groups.get(i).getImageView());
                groups.get(i).getTextView().setTextColor(Color.parseColor(mainConfig.getItemSelTextColor()));
            } else {
                if (!TextUtils.isEmpty(mainConfig.getItemBackgroundColor()))
                    groups.get(i).getLlGroup().setBackgroundColor(Color.parseColor(mainConfig.getItemBackgroundColor()));
                Glide.with(this).load(mainConfig.getItems().get(currentIndex).getIconUrl()).error(R.mipmap.ic_launcher).into(groups.get(i).getImageView());
                groups.get(i).getTextView().setTextColor(Color.parseColor(mainConfig.getItemTextColor()));
            }
        }
    }

    public BleService getBleService() {
        return bleService;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
