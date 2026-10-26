package com.spot.civicpin.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CivicReportRepository {

    private final CivicReportDao reportDao;
    private final ReportEventDao eventDao;
    private final ExecutorService executorService;

    public CivicReportRepository(Context context) {
        CivicDatabase db = CivicDatabase.getInstance(context);
        this.reportDao = db.reportDao();
        this.eventDao = db.eventDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void insertReport(CivicReportEntity report, String initialEventDescription) {
        executorService.execute(() -> {
            reportDao.insertReport(report);
            if (initialEventDescription != null) {
                eventDao.insertEvent(new ReportEventEntity(report.reportId, "CREATED", initialEventDescription));
            }
        });
    }

    public void updateReport(CivicReportEntity report, String eventType, String eventDescription) {
        executorService.execute(() -> {
            report.updatedAt = System.currentTimeMillis();
            reportDao.updateReport(report);
            if (eventDescription != null) {
                eventDao.insertEvent(new ReportEventEntity(report.reportId, eventType != null ? eventType : "UPDATED", eventDescription));
            }
        });
    }

    public void deleteReport(CivicReportEntity report) {
        executorService.execute(() -> reportDao.deleteReport(report));
    }

    public LiveData<CivicReportEntity> getReportById(String id) {
        return reportDao.getReportById(id);
    }

    public LiveData<List<CivicReportEntity>> getAllReports() {
        return reportDao.getAllReports();
    }

    public LiveData<List<CivicReportEntity>> getRecentReports() {
        return reportDao.getRecentReports();
    }

    public LiveData<Integer> getTotalCount() {
        return reportDao.getTotalCount();
    }

    public LiveData<Integer> getDraftCount() {
        return reportDao.getDraftCount();
    }

    public LiveData<Integer> getComposerOpenedCount() {
        return reportDao.getComposerOpenedCount();
    }

    public LiveData<Integer> getSubmittedCount() {
        return reportDao.getSubmittedCount();
    }

    public LiveData<Integer> getResolvedCount() {
        return reportDao.getResolvedCount();
    }

    public LiveData<List<ReportEventEntity>> getEventsForReport(String reportId) {
        return eventDao.getEventsForReport(reportId);
    }

    public void addEvent(String reportId, String eventType, String description) {
        executorService.execute(() -> eventDao.insertEvent(new ReportEventEntity(reportId, eventType, description)));
    }

    public void deleteAllReports() {
        executorService.execute(reportDao::deleteAllReports);
    }
}
