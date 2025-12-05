package com.example.quotehub;

public class ActivityItem {
    private String title;
    private String value;
    private String icon;

    public ActivityItem() {
    }

    public ActivityItem(String title, String value, String icon) {
        this.title = title;
        this.value = value;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}