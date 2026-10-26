package com.spot.civicpin;

import com.spot.civicpin.routing.CivicDepartmentRouter;
import com.spot.civicpin.routing.DepartmentRoute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CivicDepartmentRouterTest {

    @Test
    public void testDefaultPotholesRouting() {
        CivicDepartmentRouter router = new CivicDepartmentRouter(null);
        DepartmentRoute route = router.routeReport("Potholes", "Jayanagar", null);

        assertNotNull(route);
        assertEquals("BBMP Road Infrastructure & Maintenance Department", route.departmentName);
        assertEquals("UNVERIFIED_DEFAULT", route.verificationStatus);
        assertTrue(route.toEmails.contains("comm@bbmp.gov.in"));
    }

    @Test
    public void testOthersRouting() {
        CivicDepartmentRouter router = new CivicDepartmentRouter(null);
        DepartmentRoute route = router.routeReport("Others", "Whitefield", null);

        assertNotNull(route);
        assertEquals("Bengaluru Civic Authority (General)", route.departmentName);
        assertEquals("UNVERIFIED_DEFAULT", route.verificationStatus);
    }
}
