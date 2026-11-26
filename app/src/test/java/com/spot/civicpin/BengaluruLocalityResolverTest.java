package com.spot.civicpin;

import com.spot.civicpin.location.BengaluruLocalityResolver;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BengaluruLocalityResolverTest {

    @Test
    public void testFindBestMatch() {
        String match1 = BengaluruLocalityResolver.findBestMatch("Koramangala 4th Block");
        assertNotNull(match1);
        assertEquals("Koramangala", match1);

        String match2 = BengaluruLocalityResolver.findBestMatch("BTM 2nd Stage");
        assertNotNull(match2);
        assertEquals("BTM Layout", match2);

        String match3 = BengaluruLocalityResolver.findBestMatch("ITPL Main Road, Whitefield");
        assertNotNull(match3);
        assertTrue(match3.contains("Whitefield"));
    }

    @Test
    public void halliAddressesAreNotIndiranagar() {
        assertEquals("Yelahanka (New & Old Town)", BengaluruLocalityResolver.findBestMatch(
                "Doddaballapur Main Rd, Avalahalli, Yelahanka, Bengaluru, Karnataka 560064"));
        assertNull(BengaluruLocalityResolver.findBestMatch("Kodihalli, Bengaluru"));
        assertEquals("Indiranagar / Domlur / HAL", BengaluruLocalityResolver.findBestMatch(
                "HAL 2nd Stage, Indiranagar, Bengaluru"));
        assertEquals("JP Nagar (Phases 1 to 9)", BengaluruLocalityResolver.findBestMatch(
                "7th Phase, JP Nagar, Bengaluru"));
    }

    @Test
    public void testSearchLocalities() {
        List<String> results = BengaluruLocalityResolver.searchLocalities("nagar");
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
}
