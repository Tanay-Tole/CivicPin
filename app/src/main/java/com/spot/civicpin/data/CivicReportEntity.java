package com.spot.civicpin.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "civic_reports")
public class CivicReportEntity {

    @PrimaryKey
    @NonNull
    public String reportId;

    public String title;
    public String description;

    public String aiPredictedCategory;
    public float aiConfidence;

    public String confirmedCategory;
    public String issueSubtype;

    public String imagePath;
    public String thumbnailPath;

    public double latitude;
    public double longitude;

    public String address;
    public String locality;

    public float locationAccuracy;
    public long locationTimestamp;
    public String locationSource;

    public String departmentName;

    public String toEmails;
    public String ccEmails;
    public String bccEmails;

    public String emailSubject;
    public String emailBody;

    public String status; // DRAFT, READY_TO_SEND, EMAIL_COMPOSER_OPENED, USER_MARKED_SUBMITTED, FOLLOW_UP_NEEDED, USER_MARKED_RESOLVED, CANCELLED

    public long createdAt;
    public long updatedAt;

    public Long emailComposerOpenedAt;
    public Long userConfirmedSubmittedAt;
    public Long resolvedAt;

    public String userNotes;

    public CivicReportEntity() {
        this.reportId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.status = "DRAFT";
    }
}
