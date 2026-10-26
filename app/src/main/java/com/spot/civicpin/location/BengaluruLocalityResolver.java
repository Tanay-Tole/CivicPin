package com.spot.civicpin.location;

import android.location.Address;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BengaluruLocalityResolver {

    public static final List<String> KNOWN_LOCALITIES = Arrays.asList(
            "Koramangala",
            "BTM Layout",
            "HSR Layout",
            "Jayanagar",
            "JP Nagar (Phases 1 to 9)",
            "Basavanagudi / Banashankari",
            "Bommanahalli / Begur / Hongasandra",
            "Electronic City / Hulimavu / Bannerghatta Road",
            "Padmanabhanagar / Kumaraswamy Layout",
            "Marathahalli",
            "Whitefield / Kadugodi / ITPL",
            "Bellandur / Sarjapur Road / Haralur",
            "Indiranagar / Domlur / HAL",
            "KR Puram / Mahadevapura / Hoodi",
            "CV Raman Nagar / Old Airport Road",
            "Kothanur / Thanisandra",
            "Hennur / HRBR Layout / Kalyan Nagar",
            "Hebbal / RT Nagar / Ganganagar",
            "Yelahanka (New & Old Town)",
            "Sahakar Nagar / Vidyaranyapura / Byatarayanapura",
            "Banaswadi / Kammanahalli / Ramamurthy Nagar",
            "MG Road / Brigade Road / Shanthala Nagar",
            "Shivajinagar / Frazer Town / Commercial Street",
            "Malleswaram / Sadashivanagar / Vasanth Nagar",
            "Chamarajpet / Chickpet / Majestic (Gandhinagar)",
            "Rajajinagar / Basaveshwaranagar",
            "Vijayanagar / RPC Layout / Chandra Layout",
            "Nagarbhavi / Kengeri / Nayandahalli",
            "Rajarajeshwari Nagar (RR Nagar)",
            "Yeshwanthpur / Peenya / Jalahalli / Dasarahalli"
    );

    private static final List<String> GENERIC_WORDS = Arrays.asList(
            "road", "layout", "nagar", "town", "phases", "phase", "new", "old", "block", "stage", "sector", "main"
    );

    public static String resolveLocality(Address address) {
        if (address == null) return null;

        String subLocality = address.getSubLocality();
        String locality = address.getLocality();

        if (subLocality != null && !subLocality.trim().isEmpty()) {
            String matched = findBestMatch(subLocality);
            if (matched != null) return matched;
        }

        if (locality != null && !locality.trim().isEmpty()) {
            String matched = findBestMatch(locality);
            if (matched != null) return matched;
        }

        if (address.getMaxAddressLineIndex() >= 0) {
            String fullLine = address.getAddressLine(0);
            if (fullLine != null) {
                String matched = findBestMatch(fullLine);
                if (matched != null) return matched;
            }
        }

        return subLocality != null ? subLocality : locality;
    }

    public static String findBestMatch(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();

        // 1. First check full known locality parts
        for (String known : KNOWN_LOCALITIES) {
            String[] parts = known.split("[/,]");
            for (String part : parts) {
                String cleanPart = part.replaceAll("[\\(\\)]", "").trim().toLowerCase();
                if (cleanPart.length() >= 3 && !GENERIC_WORDS.contains(cleanPart) && lower.contains(cleanPart)) {
                    return known;
                }
            }
        }

        // 2. Fallback to individual words
        for (String known : KNOWN_LOCALITIES) {
            String[] words = known.split("[/\\(\\),\\s]+");
            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                if (cleanWord.length() >= 3 && !GENERIC_WORDS.contains(cleanWord) && lower.contains(cleanWord)) {
                    return known;
                }
            }
        }
        return null;
    }

    public static List<String> searchLocalities(String query) {
        if (query == null || query.trim().isEmpty()) {
            return KNOWN_LOCALITIES;
        }
        String q = query.trim().toLowerCase();
        List<String> results = new ArrayList<>();
        for (String loc : KNOWN_LOCALITIES) {
            if (loc.toLowerCase().contains(q)) {
                results.add(loc);
            }
        }
        return results;
    }
}
