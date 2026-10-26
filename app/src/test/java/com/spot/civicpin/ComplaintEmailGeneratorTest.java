package com.spot.civicpin;

import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.email.ComplaintEmailGenerator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ComplaintEmailGeneratorTest {

    @Test
    public void testGenerateSubjectAndBody() {
        CivicReportEntity report = new CivicReportEntity();
        report.confirmedCategory = "Garbage";
        report.locality = "Koramangala";
        report.address = "5th Block, Koramangala, Bengaluru";
        report.description = "Garbage piling up near park gate.";
        report.latitude = 12.93521;
        report.longitude = 77.62448;
        report.issueSubtype = "Overflowing Bin";

        String subject = ComplaintEmailGenerator.generateSubject(report);
        String body = ComplaintEmailGenerator.generateBody(report, "Tanay");

        assertNotNull(subject);
        assertNotNull(body);

        assertTrue(subject.contains("Garbage Accumulation at Koramangala"));
        assertTrue(body.contains("12.935210, 77.624480"));
        assertTrue(body.contains("https://maps.google.com/?q=12.935210,77.624480"));
        assertTrue(body.contains("Tanay"));
    }
}
