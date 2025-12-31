package com.halilovindustries.pestsnap.data.remote.model;

public class UploadRequest {
    private String farmerId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String imageBase64;

    public UploadRequest(String farmerId, double latitude, double longitude,
                         long timestamp, String imageBase64) {
        this.farmerId = farmerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.imageBase64 = imageBase64;
    }

    // Getters
    public String getFarmerId() { return farmerId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
    public String getImageBase64() { return imageBase64; }
}