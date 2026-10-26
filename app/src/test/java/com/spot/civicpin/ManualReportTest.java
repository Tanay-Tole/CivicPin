package com.spot.civicpin;

import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.email.ComplaintEmailGenerator;
import com.spot.civicpin.location.BengaluruLocalityResolver;
import com.spot.civicpin.routing.CivicDepartmentRouter;
import com.spot.civicpin.routing.DepartmentRoute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ManualReportTest {

    @Test
    public void testManualSelfPinReportingWorkflow() {
        CivicReportEntity report = new CivicReportEntity();
        report.confirmedCategory = "Water_Leak_Sewer";
        report.latitude = 12.9250;
        report.longitude = 77.5850;
        report.locationSource = "USER_MAP_PIN";

        // Resolve locality manually or from pin
        String locality = BengaluruLocalityResolver.findBestMatch("Jayanagar 4th Block");
        assertNotNull(locality);
        assertEquals("Jayanagar", locality);

        report.locality = locality;
        report.address = "4th Block, Jayanagar, Bengaluru";
        report.description = "Self-pinned water leakage on main road.";

        // Test Routing
        CivicDepartmentRouter router = new CivicDepartmentRouter(null);
        DepartmentRoute route = router.routeReport(report.confirmedCategory, report.locality, "Water pipe leakage");
        assertNotNull(route);
        assertEquals("VERIFIED_DIRECTORY", route.verificationStatus);

        report.departmentName = route.departmentName;
        report.toEmails = route.toEmails;
        report.ccEmails = route.ccEmails;

        // Test Direct Email Subject & Body Generation
        String subject = ComplaintEmailGenerator.generateSubject(report);
        String body = ComplaintEmailGenerator.generateBody(report, "Bengaluru Resident");

        assertNotNull(subject);
        assertNotNull(body);

        assertTrue(subject.contains("Water / Sewage Leakage at Jayanagar"));
        assertTrue(body.contains("12.925000, 77.585000"));
        assertTrue(body.contains("USER_MAP_PIN") || body.contains("GPS Coordinates"));
    }
}
