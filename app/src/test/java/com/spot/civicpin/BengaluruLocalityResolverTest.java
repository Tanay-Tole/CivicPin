package com.spot.civicpin;

import com.spot.civicpin.location.BengaluruLocalityResolver;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void testSearchLocalities() {
        List<String> results = BengaluruLocalityResolver.searchLocalities("nagar");
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
}
