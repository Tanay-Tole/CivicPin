package com.spot.civicpin.routing;

import android.content.Context;

import com.spot.civicpin.location.BengaluruLocalityResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CivicDepartmentRouter {

    private static class ContactRecord {
        String waterTo = "callcenter@bwssb.gov.in";
        String waterCc = "bwssbcallcenter@gmail.com, pro@bwssb.gov.in";
        String garbageTo = "contactusbbmp@gmail.com";
        String garbageCc = "comm@bbmp.gov.in, dceeswm@gmail.com";
    }

    private final Map<String, ContactRecord> directory = new HashMap<>();

    public CivicDepartmentRouter(Context context) {
        loadDirectory(context);
    }

    private void loadDirectory(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("email civic project.txt")))) {
            String line;
            String currentLocality = null;
            String currentType = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("=") || line.startsWith("-")) continue;

                if (line.contains("water:") || line.contains("garbage:")) {
                    if (line.contains("water:")) currentType = "water";
                    else if (line.contains("garbage:")) currentType = "garbage";
                    continue;
                }

                if (line.startsWith("To:") || line.startsWith("CC:") || line.startsWith("BCC:")) {
                    if (currentLocality != null && currentType != null) {
                        ContactRecord record = directory.get(currentLocality);
                        if (record == null) {
                            record = new ContactRecord();
                            directory.put(currentLocality, record);
                        }
                        String emails = line.substring(line.indexOf(":") + 1).trim();
                        if (!emails.isEmpty()) {
                            if ("water".equals(currentType)) {
                                if (line.startsWith("To:")) record.waterTo = emails;
                                else if (line.startsWith("CC:")) record.waterCc = emails;
                            } else if ("garbage".equals(currentType)) {
                                if (line.startsWith("To:")) record.garbageTo = emails;
                                else if (line.startsWith("CC:")) record.garbageCc = emails;
                            }
                        }
                    }
                    continue;
                }

                // Locality name block header
                if (!line.contains("@") && !line.contains("CENTRAL HELPLINES")) {
                    currentLocality = line;
                    currentType = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DepartmentRoute routeReport(String category, String locality, String issueSubtype) {
        String matchedKey = BengaluruLocalityResolver.findBestMatch(locality);
        ContactRecord record = null;

        if (matchedKey != null) {
            for (Map.Entry<String, ContactRecord> entry : directory.entrySet()) {
                if (entry.getKey().toLowerCase().contains(matchedKey.toLowerCase()) ||
                    matchedKey.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    record = entry.getValue();
                    break;
                }
            }
        }

        if (record == null) {
            record = new ContactRecord();
        }

        if ("Garbage".equalsIgnoreCase(category)) {
            return new DepartmentRoute(
                    "BBMP Solid Waste Management (SWM)",
                    locality != null ? locality : "Bengaluru",
                    "Garbage",
                    record.garbageTo,
                    record.garbageCc,
                    matchedKey != null ? "VERIFIED_DIRECTORY" : "UNVERIFIED_DEFAULT",
                    "Garbage complaints routed via BBMP Ward / Zone Solid Waste Management Contacts."
            );
        } else if ("Water_Leak_Sewer".equalsIgnoreCase(category)) {
            String deptName = "BWSSB (Bangalore Water Supply and Sewerage Board)";
            if (issueSubtype != null && issueSubtype.toLowerCase().contains("drain")) {
                deptName = "BBMP Stormwater Drain Department & BWSSB";
            }
            return new DepartmentRoute(
                    deptName,
                    locality != null ? locality : "Bengaluru",
                    "Water_Leak_Sewer",
                    record.waterTo,
                    record.waterCc,
                    matchedKey != null ? "VERIFIED_DIRECTORY" : "UNVERIFIED_DEFAULT",
                    "Water & Sewer complaints routed via BWSSB Sub-Division Contacts."
            );
        } else if ("Potholes".equalsIgnoreCase(category)) {
            return new DepartmentRoute(
                    "BBMP Road Infrastructure & Maintenance Department",
                    locality != null ? locality : "Bengaluru",
                    "Potholes",
                    "comm@bbmp.gov.in, ce.road@bbmp.gov.in",
                    "contactusbbmp@gmail.com",
                    "UNVERIFIED_DEFAULT",
                    "Pothole routing is based on default BBMP Road Maintenance contacts. Please verify recipients before sending."
            );
        } else {
            return new DepartmentRoute(
                    "Bengaluru Civic Authority (General)",
                    locality != null ? locality : "Bengaluru",
                    "Others",
                    "comm@bbmp.gov.in",
                    "contactusbbmp@gmail.com",
                    "UNVERIFIED_DEFAULT",
                    "General civic issue. Please manually specify the target authority email."
            );
        }
    }
}
