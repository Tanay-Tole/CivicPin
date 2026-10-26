package com.spot.civicpin.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReportEventDao {

    @Insert
    void insertEvent(ReportEventEntity event);

    @Query("SELECT * FROM report_events WHERE reportId = :reportId ORDER BY timestamp ASC")
    LiveData<List<ReportEventEntity>> getEventsForReport(String reportId);

    @Query("SELECT * FROM report_events WHERE reportId = :reportId ORDER BY timestamp ASC")
    List<ReportEventEntity> getEventsForReportSync(String reportId);
}
