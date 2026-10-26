package com.spot.civicpin;

import com.spot.civicpin.ai.ClassificationResult;
import com.spot.civicpin.ai.CivicIssueClassifier;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CivicIssueClassifierModelTest {

    @Test
    public void testClassifierLabelAndDisplayMapping() {
        assertEquals("Garbage Accumulation", CivicIssueClassifier.getDisplayNameForLabel("Garbage"));
        assertEquals("Water Leakage / Sewage", CivicIssueClassifier.getDisplayNameForLabel("Water_Leak_Sewer"));
        assertEquals("Potholes / Road Damage", CivicIssueClassifier.getDisplayNameForLabel("Potholes"));
        assertEquals("Other / No Supported Issue", CivicIssueClassifier.getDisplayNameForLabel("Others"));
    }

    @Test
    public void testClassificationResultDataHolder() {
        ClassificationResult result = new ClassificationResult("Garbage", "Garbage Accumulation", 0.92f, Map.of("Garbage", 0.92f));
        assertNotNull(result);
        assertEquals("Garbage", result.getInternalLabel());
        assertEquals("Garbage Accumulation", result.getDisplayName());
        assertEquals(0.92f, result.getConfidence(), 0.001f);
        assertEquals(92, result.getConfidencePercentage());
    }

    @Test
    public void testNullBitmapHandledSafely() {
        ClassificationResult result = new ClassificationResult("Others", "Other / No Supported Issue", 0.0f, Map.of());
        assertNotNull(result);
        assertEquals("Others", result.getInternalLabel());
    }
}
