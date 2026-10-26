package com.spot.civicpin.email;

import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ComplaintEmailGenerator {

    public static String generateSubject(CivicReportEntity report) {
        String categoryName = CivicIssueClassifier.getDisplayNameForLabel(report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory);
        String locality = (report.locality != null && !report.locality.isEmpty()) ? report.locality : "Bengaluru";
        String refShort = report.reportId.length() >= 8 ? report.reportId.substring(0, 8).toUpperCase() : report.reportId.toUpperCase();

        if ("Garbage".equalsIgnoreCase(report.confirmedCategory)) {
            return String.format(Locale.getDefault(), "Civic Complaint: Garbage Accumulation at %s, Bengaluru [Ref: CP-%s]", locality, refShort);
        } else if ("Water_Leak_Sewer".equalsIgnoreCase(report.confirmedCategory)) {
            return String.format(Locale.getDefault(), "Urgent Civic Complaint: Water / Sewage Leakage at %s, Bengaluru [Ref: CP-%s]", locality, refShort);
        } else if ("Potholes".equalsIgnoreCase(report.confirmedCategory)) {
            return String.format(Locale.getDefault(), "Road Safety Complaint: Pothole / Road Damage at %s, Bengaluru [Ref: CP-%s]", locality, refShort);
        } else {
            return String.format(Locale.getDefault(), "Civic Complaint: %s at %s, Bengaluru [Ref: CP-%s]", categoryName, locality, refShort);
        }
    }

    public static String generateBody(CivicReportEntity report, String userName) {
        String categoryName = CivicIssueClassifier.getDisplayNameForLabel(report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory);
        String locality = (report.locality != null && !report.locality.isEmpty()) ? report.locality : "Bengaluru";
        String address = (report.address != null && !report.address.isEmpty()) ? report.address : locality;
        String description = (report.description != null && !report.description.isEmpty()) ? report.description : "Photograph attached for reference.";
        String subtypeStr = (report.issueSubtype != null && !report.issueSubtype.isEmpty()) ? report.issueSubtype : "General";

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault());
        String dateStr = sdf.format(new Date(report.createdAt > 0 ? report.createdAt : System.currentTimeMillis()));

        String mapsLink = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", report.latitude, report.longitude);
        String senderName = (userName != null && !userName.trim().isEmpty()) ? userName : "Concerned Citizen";
        String refShort = report.reportId.length() >= 8 ? report.reportId.substring(0, 8).toUpperCase() : report.reportId.toUpperCase();

        return String.format(
                Locale.getDefault(),
                "Respected Sir/Madam,\n\n" +
                "I would like to bring to your attention a civic issue observed in %s, Bengaluru.\n\n" +
                "Issue Category:\n%s\n\n" +
                "Issue Subtype / Details:\n%s\n\n" +
                "User Description:\n%s\n\n" +
                "Location Address:\n%s\n\n" +
                "GPS Coordinates:\n%.6f, %.6f\n\n" +
                "Google Maps Location:\n%s\n\n" +
                "Date & Time of Report:\n%s\n\n" +
                "Report Reference Code:\nCP-%s\n\n" +
                "I have attached a photograph of the issue for your inspection.\n" +
                "I request the concerned department to inspect the location and resolve the issue at the earliest.\n\n" +
                "Thank you.\n\n" +
                "Regards,\n%s\n" +
                "Civic Pin App User",
                locality,
                categoryName,
                subtypeStr,
                description,
                address,
                report.latitude,
                report.longitude,
                mapsLink,
                dateStr,
                refShort,
                senderName
        );
    }
}
