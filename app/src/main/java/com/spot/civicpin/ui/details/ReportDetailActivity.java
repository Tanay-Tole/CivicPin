package com.spot.civicpin.ui.details;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spot.civicpin.R;
import com.spot.civicpin.ui.ReportAdapter;
import com.spot.civicpin.ui.map.OsmMap;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.email.EmailIntentHelper;
import com.spot.civicpin.ui.EventTimelineAdapter;
import com.spot.civicpin.utils.LocaleHelper;

import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "ReportDetailActivity";

    private CivicReportRepository repository;
    private CivicReportEntity report;

    private ImageView imgPhoto;
    private TextView tvCategory, tvStatusBadge, tvAiInfo, tvLocality, tvAddress;
    private TextView tvDeptName, tvToEmails, tvCcEmails;
    private MapView mapView;
    private Marker pin;
    private EventTimelineAdapter timelineAdapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OsmMap.init(this);
        setContentView(R.layout.activity_report_detail);

        // Android 15 draws edge to edge: keep content clear of the system bars.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        findViewById(R.id.btn_detail_back).setOnClickListener(v -> finish());

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
        OsmMap.setup(mapView);
        mapView.getController().setZoom(11.5);
        mapView.getController().setCenter(new GeoPoint(12.9716, 77.5946));
        mapView.setOnTouchListener((v, e) -> true); // static preview: scrolling the page must not drag the map

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
        tvStatusBadge.setText(ReportAdapter.statusLabelRes(report.status));
        tvStatusBadge.getBackground().mutate().setTint(ReportAdapter.statusColor(this, report.status));

        if (report.aiPredictedCategory != null) {
            tvAiInfo.setText(String.format(Locale.getDefault(), "AI said %s, %.0f%% sure",
                    CivicIssueClassifier.getDisplayNameForLabel(report.aiPredictedCategory),
                    report.aiConfidence * 100));
        } else {
            tvAiInfo.setText("Category picked by you");
        }

        tvLocality.setText(report.locality != null && !report.locality.isEmpty() ? report.locality : "Bengaluru");
        tvAddress.setText(report.address != null ? report.address : "No address saved");

        tvDeptName.setText(report.departmentName != null ? report.departmentName : "No department yet");
        tvToEmails.setText(String.format("To: %s", report.toEmails != null && !report.toEmails.isEmpty() ? report.toEmails : "not set"));
        tvCcEmails.setText(String.format("CC: %s", report.ccEmails != null && !report.ccEmails.isEmpty() ? report.ccEmails : "not set"));

        // Show the photo block only when there is a photo to show.
        Bitmap photo = ReportAdapter.loadThumb(report.imagePath, 1080);
        findViewById(R.id.detail_photo_card).setVisibility(photo != null ? android.view.View.VISIBLE : android.view.View.GONE);
        if (photo != null) imgPhoto.setImageBitmap(photo);

        if (mapView != null && (report.latitude != 0.0 || report.longitude != 0.0)) {
            GeoPoint pos = new GeoPoint(report.latitude, report.longitude);
            if (pin == null) {
                pin = new Marker(mapView);
                pin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                pin.setInfoWindow(null);
                mapView.getOverlays().add(pin);
            }
            String category = report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory;
            pin.setIcon(OsmMap.pin(this, OsmMap.colorFor(this, category)));
            pin.setPosition(pos);
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(pos);
            mapView.invalidate();
        }
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
    protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }


}
