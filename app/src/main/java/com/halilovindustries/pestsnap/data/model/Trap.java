package com.halilovindustries.pestsnap.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "traps")
public class Trap {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private String title;
    private String imagePath;
    private double latitude;
    private double longitude;
    private long capturedAt;
    private String status; // "captured", "uploading", "uploaded", "analyzed"
    private float imageSize; // in MB
    private int sharpnessScore;
    private boolean isQualityPassed;

    public Trap(int userId, String title, String imagePath, double latitude,
                double longitude, float imageSize, int sharpnessScore, boolean isQualityPassed) {
        this.userId = userId;
        this.title = title;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capturedAt = System.currentTimeMillis();
        this.status = "captured";
        this.imageSize = imageSize;
        this.sharpnessScore = sharpnessScore;
        this.isQualityPassed = isQualityPassed;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public long getCapturedAt() { return capturedAt; }
    public void setCapturedAt(long capturedAt) { this.capturedAt = capturedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public float getImageSize() { return imageSize; }
    public void setImageSize(float imageSize) { this.imageSize = imageSize; }
    public int getSharpnessScore() { return sharpnessScore; }
    public void setSharpnessScore(int sharpnessScore) { this.sharpnessScore = sharpnessScore; }
    public boolean isQualityPassed() { return isQualityPassed; }
    public void setQualityPassed(boolean qualityPassed) { isQualityPassed = qualityPassed; }
}