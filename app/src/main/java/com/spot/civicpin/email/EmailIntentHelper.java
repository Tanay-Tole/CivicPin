package com.spot.civicpin.email;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.spot.civicpin.data.CivicReportEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmailIntentHelper {

    public static Intent createEmailIntent(Context context, CivicReportEntity report) {
        Intent intent;
        boolean hasAttachment = false;

        if (report.imagePath != null && !report.imagePath.isEmpty()) {
            File photoFile = new File(report.imagePath);
            if (photoFile.exists()) {
                hasAttachment = true;
            }
        }

        if (hasAttachment) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            File photoFile = new File(report.imagePath);
            Uri photoUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    photoFile
            );
            intent.putExtra(Intent.EXTRA_STREAM, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
        }

        if (report.toEmails != null && !report.toEmails.trim().isEmpty()) {
            String[] toArr = splitEmails(report.toEmails);
            intent.putExtra(Intent.EXTRA_EMAIL, toArr);
        }

        if (report.ccEmails != null && !report.ccEmails.trim().isEmpty()) {
            String[] ccArr = splitEmails(report.ccEmails);
            intent.putExtra(Intent.EXTRA_CC, ccArr);
        }

        intent.putExtra(Intent.EXTRA_SUBJECT, report.emailSubject != null ? report.emailSubject : "Civic Complaint");
        intent.putExtra(Intent.EXTRA_TEXT, report.emailBody != null ? report.emailBody : "");

        return Intent.createChooser(intent, "Send Civic Complaint Email via...");
    }

    private static String[] splitEmails(String emailString) {
        if (emailString == null) return new String[0];
        String[] parts = emailString.split("[,;\\s]+");
        List<String> valid = new ArrayList<>();
        for (String p : parts) {
            String clean = p.trim();
            if (clean.contains("@")) {
                valid.add(clean);
            }
        }
        return valid.toArray(new String[0]);
    }
}
