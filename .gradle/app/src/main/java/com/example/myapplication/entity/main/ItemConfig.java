package com.example.myapplication.entity.main;

import java.io.Serializable;

public class ItemConfig implements Serializable {
    private String iconUrl;
    private String iconSelUrl;
    private String itemText;
    private String contentUrl;
    private int sortIndex;

    public ItemConfig() {
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconSelUrl() {
        return iconSelUrl;
    }

    public void setIconSelUrl(String iconSelUrl) {
        this.iconSelUrl = iconSelUrl;
    }

    public String getItemText() {
        return itemText;
    }

    public void setItemText(String itemText) {
        this.itemText = itemText;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }
}
