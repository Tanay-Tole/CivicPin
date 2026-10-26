package com.spot.civicpin.ai;

import java.util.Map;

public class ClassificationResult {
    private final String internalLabel;
    private final String displayName;
    private final float confidence;
    private final Map<String, Float> probabilities;

    public ClassificationResult(String internalLabel, String displayName, float confidence, Map<String, Float> probabilities) {
        this.internalLabel = internalLabel;
        this.displayName = displayName;
        this.confidence = confidence;
        this.probabilities = probabilities;
    }

    public String getInternalLabel() {
        return internalLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getConfidence() {
        return confidence;
    }

    public Map<String, Float> getProbabilities() {
        return probabilities;
    }

    public int getConfidencePercentage() {
        return Math.round(confidence * 100);
    }
}
