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

    @Test
    public void testOwnModelIsBundledAndOldOneIsGone() throws Exception {
        java.io.File assets = new java.io.File("src/main/assets");
        java.io.File model = new java.io.File(assets, "civicpin_model.tflite");
        assertEquals(true, model.isFile() && model.length() > 500_000);
        assertEquals(false, new java.io.File(assets, "model_unquant.tflite").exists());
        // the model has 4 outputs, in this order
        assertEquals(java.util.List.of("0 Garbage", "1 Water_Leak_Sewer", "2 Potholes", "3 Others"),
                java.nio.file.Files.readAllLines(new java.io.File(assets, "labels.txt").toPath()));
    }
}
