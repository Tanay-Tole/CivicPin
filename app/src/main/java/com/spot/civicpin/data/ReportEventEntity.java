package com.spot.civicpin.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "report_events")
public class ReportEventEntity {

    @PrimaryKey(autoGenerate = true)
    public long eventId;

    public String reportId;
    public long timestamp;
    public String eventType;
    public String description;

    public ReportEventEntity(String reportId, String eventType, String description) {
        this.reportId = reportId;
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
        this.description = description;
    }
}
