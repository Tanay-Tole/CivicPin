package com.spot.civicpin.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CivicReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReport(CivicReportEntity report);

    @Update
    void updateReport(CivicReportEntity report);

    @Delete
    void deleteReport(CivicReportEntity report);

    @Query("SELECT * FROM civic_reports WHERE reportId = :id LIMIT 1")
    CivicReportEntity getReportByIdSync(String id);

    @Query("SELECT * FROM civic_reports WHERE reportId = :id LIMIT 1")
    LiveData<CivicReportEntity> getReportById(String id);

    @Query("SELECT * FROM civic_reports ORDER BY createdAt DESC")
    LiveData<List<CivicReportEntity>> getAllReports();

    @Query("SELECT * FROM civic_reports ORDER BY createdAt DESC")
    List<CivicReportEntity> getAllReportsSync();

    @Query("SELECT * FROM civic_reports ORDER BY createdAt DESC LIMIT 3")
    LiveData<List<CivicReportEntity>> getRecentReports();

    @Query("SELECT COUNT(*) FROM civic_reports")
    LiveData<Integer> getTotalCount();

    @Query("SELECT COUNT(*) FROM civic_reports WHERE status = 'DRAFT'")
    LiveData<Integer> getDraftCount();

    @Query("SELECT COUNT(*) FROM civic_reports WHERE status = 'EMAIL_COMPOSER_OPENED'")
    LiveData<Integer> getComposerOpenedCount();

    @Query("SELECT COUNT(*) FROM civic_reports WHERE status = 'USER_MARKED_SUBMITTED'")
    LiveData<Integer> getSubmittedCount();

    @Query("SELECT COUNT(*) FROM civic_reports WHERE status = 'USER_MARKED_RESOLVED'")
    LiveData<Integer> getResolvedCount();

    @Query("DELETE FROM civic_reports")
    void deleteAllReports();
}
