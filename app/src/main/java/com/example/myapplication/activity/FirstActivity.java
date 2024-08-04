package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.entity.main.ItemConfig;
import com.example.myapplication.entity.main.MainConfig;
import com.example.myapplication.util.permission.PermissionInterceptor;
import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.ArrayList;
import java.util.List;

public class FirstActivity extends AppCompatActivity {

    EditText etWebSite;

    Button btnWebSite;

    Button btnDemo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        etWebSite = findViewById(R.id.etWebsite);
        btnDemo = findViewById(R.id.btnDemo);
        btnWebSite=findViewById(R.id.btnWebSite);

        btnDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstActivity.this,WebViewActivity.class);

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
                if(TextUtils.isEmpty(s)){
                    Toaster.show("Please enter the URL of the page you want to navigate to");
                    return;
                }
                Intent intent = new Intent(FirstActivity.this,WebViewActivity.class);
                intent.putExtra("url",s.trim());
                startActivity(intent);
            }
        });
        XXPermissions.with(this).permission(Permission.BLUETOOTH_SCAN).permission(Permission.BLUETOOTH_CONNECT).permission(Permission.BLUETOOTH_ADVERTISE).permission(Permission.ACCESS_FINE_LOCATION).permission(Permission.ACCESS_COARSE_LOCATION).interceptor(new PermissionInterceptor()).request(new OnPermissionCallback() {
            @Override
            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                if (!allGranted) {
                    return;
                }
            }
        });
    }



}
