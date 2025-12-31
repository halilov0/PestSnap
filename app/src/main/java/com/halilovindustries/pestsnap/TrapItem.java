package com.halilovindustries.pestsnap;

public class TrapItem {
    private String title;
    private String timestamp;
    private String status;
    private boolean isComplete;

    public TrapItem(String title, String timestamp, String status, boolean isComplete) {
        this.title = title;
        this.timestamp = timestamp;
        this.status = status;
        this.isComplete = isComplete;
    }

    public String getTitle() { return title; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public boolean isComplete() { return isComplete; }
}