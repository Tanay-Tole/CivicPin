package com.spot.civicpin.ui.details;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.spot.civicpin.R;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.email.EmailIntentHelper;
import com.spot.civicpin.ui.EventTimelineAdapter;
import com.spot.civicpin.utils.LocaleHelper;

import java.io.File;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "ReportDetailActivity";

    private CivicReportRepository repository;
    private CivicReportEntity report;

    private ImageView imgPhoto;
    private TextView tvCategory, tvStatusBadge, tvAiInfo, tvLocality, tvAddress;
    private TextView tvDeptName, tvToEmails, tvCcEmails;
    private MapView mapView;
    private EventTimelineAdapter timelineAdapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        repository = new CivicReportRepository(this);

        String reportId = getIntent().getStringExtra("report_id");
        if (reportId == null) {
            finish();
            return;
        }

        imgPhoto = findViewById(R.id.detail_img_photo);
        tvCategory = findViewById(R.id.detail_tv_category);
        tvStatusBadge = findViewById(R.id.detail_tv_status_badge);
        tvAiInfo = findViewById(R.id.detail_tv_ai_info);
        tvLocality = findViewById(R.id.detail_tv_locality);
        tvAddress = findViewById(R.id.detail_tv_address);
        tvDeptName = findViewById(R.id.detail_tv_dept_name);
        tvToEmails = findViewById(R.id.detail_tv_to_emails);
        tvCcEmails = findViewById(R.id.detail_tv_cc_emails);

        mapView = findViewById(R.id.detail_map_view);
        try {
            mapView.onCreate(savedInstanceState);
        } catch (Exception e) {
            Log.e(TAG, "Error in MapView onCreate", e);
        }

        RecyclerView rvTimeline = findViewById(R.id.rv_detail_timeline);
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        timelineAdapter = new EventTimelineAdapter();
        rvTimeline.setAdapter(timelineAdapter);

        MaterialButton btnReopenEmail = findViewById(R.id.btn_reopen_email);
        MaterialButton btnMarkSubmitted = findViewById(R.id.btn_mark_submitted);
        MaterialButton btnMarkResolved = findViewById(R.id.btn_mark_resolved);
        MaterialButton btnDelete = findViewById(R.id.btn_delete_report);

        btnReopenEmail.setOnClickListener(v -> {
            if (report != null) {
                startActivity(EmailIntentHelper.createEmailIntent(this, report));
            }
        });

        btnMarkSubmitted.setOnClickListener(v -> {
            if (report != null) {
                report.status = "USER_MARKED_SUBMITTED";
                report.userConfirmedSubmittedAt = System.currentTimeMillis();
                repository.updateReport(report, "SUBMITTED", "User manually marked report as submitted");
                Toast.makeText(this, "Marked as Submitted!", Toast.LENGTH_SHORT).show();
            }
        });

        btnMarkResolved.setOnClickListener(v -> {
            if (report != null) {
                report.status = "USER_MARKED_RESOLVED";
                report.resolvedAt = System.currentTimeMillis();
                repository.updateReport(report, "RESOLVED", "User marked report as resolved");
                Toast.makeText(this, "Marked as Resolved!", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.btn_delete)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (report != null) {
                        repository.deleteReport(report);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show());

        repository.getReportById(reportId).observe(this, reportEntity -> {
            if (reportEntity != null) {
                report = reportEntity;
                bindData();
            }
        });

        repository.getEventsForReport(reportId).observe(this, events -> {
            if (events != null) {
                timelineAdapter.setEvents(events);
            }
        });
    }

    private void bindData() {
        String categoryDisplay = CivicIssueClassifier.getDisplayNameForLabel(
                report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory
        );
        tvCategory.setText(categoryDisplay);
        tvStatusBadge.setText(report.status != null ? report.status.replace("_", " ") : "DRAFT");

        tvAiInfo.setText(String.format(Locale.getDefault(), "AI Predicted: %s (%.0f%% confidence)",
                CivicIssueClassifier.getDisplayNameForLabel(report.aiPredictedCategory),
                report.aiConfidence * 100));

        tvLocality.setText(String.format("Locality: %s", report.locality != null ? report.locality : "Bengaluru"));
        tvAddress.setText(String.format("Address: %s", report.address != null ? report.address : "N/A"));

        tvDeptName.setText(String.format("Department: %s", report.departmentName != null ? report.departmentName : "N/A"));
        tvToEmails.setText(String.format("To: %s", report.toEmails != null ? report.toEmails : "N/A"));
        tvCcEmails.setText(String.format("CC: %s", report.ccEmails != null ? report.ccEmails : "N/A"));

        if (report.imagePath != null && !report.imagePath.isEmpty()) {
            File file = new File(report.imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    imgPhoto.setImageBitmap(bitmap);
                }
            }
        }

        if (mapView != null && report.latitude != 0.0 && report.longitude != 0.0) {
            mapView.getMapAsync(map -> {
                LatLng pos = new LatLng(report.latitude, report.longitude);
                map.addMarker(new MarkerOptions().position(pos).title(categoryDisplay));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
