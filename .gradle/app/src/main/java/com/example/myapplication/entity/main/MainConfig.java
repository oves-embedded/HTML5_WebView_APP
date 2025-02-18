package com.example.myapplication.entity.main;

import java.io.Serializable;
import java.util.List;


public class MainConfig implements Serializable {

//    private String barBackgroundColor;
    private String itemBackgroundColor;
    private String itemSelBackgroundColor;
    private String itemTextColor;
    private String itemSelTextColor;

    private List<ItemConfig> items;

//    public String getBarBackgroundColor() {
//        return barBackgroundColor;
//    }
//
//    public void setBarBackgroundColor(String barBackgroundColor) {
//        this.barBackgroundColor = barBackgroundColor;
//    }

    public String getItemBackgroundColor() {
        return itemBackgroundColor;
    }

    public void setItemBackgroundColor(String itemBackgroundColor) {
        this.itemBackgroundColor = itemBackgroundColor;
    }

    public String getItemSelBackgroundColor() {
        return itemSelBackgroundColor;
    }

    public void setItemSelBackgroundColor(String itemSelBackgroundColor) {
        this.itemSelBackgroundColor = itemSelBackgroundColor;
    }

    public String getItemTextColor() {
        return itemTextColor;
    }

    public void setItemTextColor(String itemTextColor) {
        this.itemTextColor = itemTextColor;
    }

    public String getItemSelTextColor() {
        return itemSelTextColor;
    }

    public void setItemSelTextColor(String itemSelTextColor) {
        this.itemSelTextColor = itemSelTextColor;
    }

    public List<ItemConfig> getItems() {
        return items;
    }

    public void setItems(List<ItemConfig> items) {
        this.items = items;
    }
}
