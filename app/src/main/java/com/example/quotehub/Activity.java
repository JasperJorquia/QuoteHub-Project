package com.example.quotehub;

public class Activity {
    private String id;
    private String title;
    private String time;
    private String type;

    public Activity() {
    }

    public Activity(String id, String title, String time, String type) {
        this.id = id;
        this.title = title;
        this.time = time;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}