package com.innova.visual_retail_discovery.model;

public class ImageResult {

    private String label;
    private double confidence;
    private String description;

    // Required by Jackson for deserialization
    public ImageResult() {}

    public ImageResult(String label, double confidence, String description) {
        this.label = label;
        this.confidence = confidence;
        this.description = description;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}