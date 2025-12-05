package com.example.quotehub;

public class ActivityLog {
    private String id;
    private String action;
    private String description;
    private long timestamp;
    private String quoteId;

    public ActivityLog() {
    }

    public ActivityLog(String id, String action, String description, long timestamp, String quoteId) {
        this.id = id;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
        this.quoteId = quoteId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }
}