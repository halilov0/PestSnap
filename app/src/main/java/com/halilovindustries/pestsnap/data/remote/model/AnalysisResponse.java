package com.halilovindustries.pestsnap.data.remote.model;

import java.util.List;

public class AnalysisResponse {
    private String status;
    private String trapId;
    private List<DetectedPest> detectedPests;
    private String recommendation;
    private boolean requiresAction;

    public static class DetectedPest {
        private String commonName;
        private String scientificName;
        private int count;
        private float confidence;

        // Getters
        public String getCommonName() { return commonName; }
        public String getScientificName() { return scientificName; }
        public int getCount() { return count; }
        public float getConfidence() { return confidence; }
    }

    // Getters
    public String getStatus() { return status; }
    public String getTrapId() { return trapId; }
    public List<DetectedPest> getDetectedPests() { return detectedPests; }
    public String getRecommendation() { return recommendation; }
    public boolean isRequiresAction() { return requiresAction; }
}