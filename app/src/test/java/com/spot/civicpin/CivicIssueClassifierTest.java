package com.spot.civicpin;

import com.spot.civicpin.ai.CivicIssueClassifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CivicIssueClassifierTest {

    @Test
    public void testGetDisplayNameForLabels() {
        assertEquals("Garbage Accumulation", CivicIssueClassifier.getDisplayNameForLabel("Garbage"));
        assertEquals("Water Leakage / Sewage", CivicIssueClassifier.getDisplayNameForLabel("Water_Leak_Sewer"));
        assertEquals("Potholes / Road Damage", CivicIssueClassifier.getDisplayNameForLabel("Potholes"));
        assertEquals("Other / No Supported Issue", CivicIssueClassifier.getDisplayNameForLabel("Others"));
        assertEquals("Other / No Supported Issue", CivicIssueClassifier.getDisplayNameForLabel(null));
    }
}
