package com.example.quotehub;

public class Quote {
    private String id;
    private String text;
    private String author;
    private String category;
    private boolean liked;
    private long timestamp;
    private String userId;

    public Quote() {
    }

    public Quote(String id, String text, String author, String category, boolean liked, long timestamp) {
        this.id = id;
        this.text = text;
        this.author = author;
        this.category = category;
        this.liked = liked;
        this.timestamp = timestamp;
    }

    public Quote(String id, String text, String author, String category, boolean liked, long timestamp, String userId) {
        this.id = id;
        this.text = text;
        this.author = author;
        this.category = category;
        this.liked = liked;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Added getter and setter for userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}