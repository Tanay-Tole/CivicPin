package com.spot.civicpin.ui.map;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.spot.civicpin.R;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.ui.ReportAdapter;
import com.spot.civicpin.ui.details.ReportDetailActivity;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final GeoPoint BENGALURU = new GeoPoint(12.9716, 77.5946);

    private MapView mapView;
    private CivicReportRepository repository;
    private List<CivicReportEntity> allReports = new ArrayList<>();
    private final List<GeoPoint> shownPoints = new ArrayList<>();
    private String selectedFilter = "ALL";

    private TextView tvCount, tvPreviewTitle, tvPreviewMeta;
    private MaterialCardView cardPreview;
    private View previewIconBg;
    private ImageView imgPreviewIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        OsmMap.init(requireContext());
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        repository = new CivicReportRepository(requireContext());
        mapView = view.findViewById(R.id.all_reports_map_view);
        tvCount = view.findViewById(R.id.tv_map_count);
        cardPreview = view.findViewById(R.id.card_map_preview);
        tvPreviewTitle = view.findViewById(R.id.tv_preview_title);
        tvPreviewMeta = view.findViewById(R.id.tv_preview_meta);
        previewIconBg = view.findViewById(R.id.preview_icon_bg);
        imgPreviewIcon = view.findViewById(R.id.img_preview_icon);

        OsmMap.setup(mapView);
        mapView.getController().setZoom(11.5);
        mapView.getController().setCenter(BENGALURU);

        ChipGroup cgMapFilters = view.findViewById(R.id.cg_map_filters);
        cgMapFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_map_garbage)) selectedFilter = "Garbage";
            else if (checkedIds.contains(R.id.chip_map_water)) selectedFilter = "Water_Leak_Sewer";
            else if (checkedIds.contains(R.id.chip_map_potholes)) selectedFilter = "Potholes";
            else if (checkedIds.contains(R.id.chip_map_others)) selectedFilter = "Others";
            else selectedFilter = "ALL";
            cardPreview.setVisibility(View.GONE);
            updateMapMarkers();
            fitToPins(true);
        });

        view.findViewById(R.id.btn_map_fit).setOnClickListener(v -> fitToPins(true));

        observeReports();
        return view;
    }

    private void observeReports() {
        repository.getAllReports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null) {
                boolean first = allReports.isEmpty();
                allReports = reports;
                updateMapMarkers();
                if (first) fitToPins(false);
            }
        });
    }

    private static String categoryOf(CivicReportEntity r) {
        String c = r.confirmedCategory != null ? r.confirmedCategory : r.aiPredictedCategory;
        return c != null ? c : "Others";
    }

    private void updateMapMarkers() {
        if (mapView == null || !isAdded()) return;
        // keep the copyright overlay, replace everything else
        List<Overlay> keep = new ArrayList<>();
        for (Overlay o : mapView.getOverlays()) {
            if (o instanceof org.osmdroid.views.overlay.CopyrightOverlay) keep.add(o);
        }
        mapView.getOverlays().clear();
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                cardPreview.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        shownPoints.clear();

        for (CivicReportEntity report : allReports) {
            String category = categoryOf(report);
            if (!"ALL".equals(selectedFilter) && !selectedFilter.equalsIgnoreCase(category)) continue;
            if (report.latitude == 0.0 && report.longitude == 0.0) continue;

            GeoPoint pos = new GeoPoint(report.latitude, report.longitude);
            Marker marker = new Marker(mapView);
            marker.setPosition(pos);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(OsmMap.pin(requireContext(), OsmMap.colorFor(requireContext(), category)));
            marker.setTitle(CivicIssueClassifier.getDisplayNameForLabel(category));
            marker.setOnMarkerClickListener((m, mv) -> {
                showPreview(report);
                mv.getController().animateTo(m.getPosition());
                return true;
            });
            mapView.getOverlays().add(marker);
            shownPoints.add(pos);
        }
        mapView.getOverlays().addAll(keep);
        mapView.invalidate();

        int n = shownPoints.size();
        tvCount.setText(n == 0 ? getString(R.string.map_count_empty)
                : getResources().getQuantityString(R.plurals.map_count, n, n));
    }

    private void showPreview(CivicReportEntity report) {
        String category = categoryOf(report);
        int color = OsmMap.colorFor(requireContext(), category);
        tvPreviewTitle.setText(CivicIssueClassifier.getDisplayNameForLabel(category));
        String place = report.locality != null && !report.locality.isEmpty() ? report.locality : "Bengaluru";
        tvPreviewMeta.setText(place + "  ·  " + ReportAdapter.statusLabel(report.status));
        imgPreviewIcon.setImageResource(ReportAdapter.iconFor(category));
        imgPreviewIcon.setImageTintList(ColorStateList.valueOf(color));
        previewIconBg.setBackgroundTintList(ColorStateList.valueOf(ReportAdapter.softColorFor(requireContext(), category)));
        cardPreview.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);
            intent.putExtra("report_id", report.reportId);
            startActivity(intent);
        });
        if (cardPreview.getVisibility() != View.VISIBLE) {
            cardPreview.setAlpha(0f);
            cardPreview.setTranslationY(40f);
            cardPreview.setVisibility(View.VISIBLE);
            cardPreview.animate().alpha(1f).translationY(0f).setDuration(220).start();
        }
    }

    private void fitToPins(boolean animate) {
        if (mapView == null) return;
        if (shownPoints.isEmpty()) {
            if (animate) mapView.getController().animateTo(BENGALURU, 11.5, 400L);
            return;
        }
        if (shownPoints.size() == 1) {
            IGeoPoint only = shownPoints.get(0);
            if (animate) mapView.getController().animateTo(only, 15.5, 400L);
            else {
                mapView.getController().setZoom(15.5);
                mapView.getController().setCenter(only);
            }
            return;
        }
        BoundingBox box = BoundingBox.fromGeoPointsSafe(shownPoints);
        int pad = Math.round(72 * getResources().getDisplayMetrics().density);
        mapView.post(() -> mapView.zoomToBoundingBox(box.increaseByScale(1.15f), animate, pad));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDetach();
        mapView = null;
        super.onDestroyView();
    }
}
