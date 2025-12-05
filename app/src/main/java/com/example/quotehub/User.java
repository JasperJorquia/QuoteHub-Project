package com.example.quotehub;

public class User {
    private String id;
    private String email;
    private long createdAt;
    private long lastLogin;

    public User() {
    }

    public User(String id, String email) {
        this.id = id;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
    }

    public User(String id, String email, long createdAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
}