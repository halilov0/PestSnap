package com.halilovindustries.pestsnap.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pest_results")
public class PestResult {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int trapId;
    private String pestName;
    private String scientificName;
    private int count;
    private float confidence;
    private String recommendation;
    private boolean isWarning;
    private long analyzedAt;

    public PestResult(int trapId, String pestName, String scientificName,
                      int count, float confidence, String recommendation, boolean isWarning) {
        this.trapId = trapId;
        this.pestName = pestName;
        this.scientificName = scientificName;
        this.count = count;
        this.confidence = confidence;
        this.recommendation = recommendation;
        this.isWarning = isWarning;
        this.analyzedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTrapId() { return trapId; }
    public void setTrapId(int trapId) { this.trapId = trapId; }
    public String getPestName() { return pestName; }
    public void setPestName(String pestName) { this.pestName = pestName; }
    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public boolean isWarning() { return isWarning; }
    public void setWarning(boolean warning) { isWarning = warning; }
    public long getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(long analyzedAt) { this.analyzedAt = analyzedAt; }
}