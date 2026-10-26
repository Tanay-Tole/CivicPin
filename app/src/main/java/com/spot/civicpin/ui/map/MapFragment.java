package com.spot.civicpin.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.ChipGroup;
import com.spot.civicpin.R;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.ui.details.ReportDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";

    private MapView mapView;
    private GoogleMap googleMap;
    private CivicReportRepository repository;
    private List<CivicReportEntity> allReports = new ArrayList<>();
    private String selectedFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        repository = new CivicReportRepository(requireContext());
        mapView = view.findViewById(R.id.all_reports_map_view);

        try {
            mapView.onCreate(savedInstanceState);
        } catch (Exception e) {
            Log.e(TAG, "Error in MapView onCreate", e);
        }

        ChipGroup cgMapFilters = view.findViewById(R.id.cg_map_filters);
        cgMapFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_map_garbage)) {
                selectedFilter = "Garbage";
            } else if (checkedIds.contains(R.id.chip_map_water)) {
                selectedFilter = "Water_Leak_Sewer";
            } else if (checkedIds.contains(R.id.chip_map_potholes)) {
                selectedFilter = "Potholes";
            } else {
                selectedFilter = "ALL";
            }
            updateMapMarkers();
        });

        mapView.getMapAsync(map -> {
            googleMap = map;
            LatLng bengaluru = new LatLng(12.9716, 77.5946);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bengaluru, 11f));

            googleMap.setOnInfoWindowClickListener(marker -> {
                if (marker.getTag() instanceof String) {
                    String reportId = (String) marker.getTag();
                    Intent intent = new Intent(requireContext(), ReportDetailActivity.class);
                    intent.putExtra("report_id", reportId);
                    startActivity(intent);
                }
            });

            observeReports();
        });

        return view;
    }

    private void observeReports() {
        repository.getAllReports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null) {
                allReports = reports;
                updateMapMarkers();
            }
        });
    }

    private void updateMapMarkers() {
        if (googleMap == null || !isAdded()) return;
        googleMap.clear();

        for (CivicReportEntity report : allReports) {
            String category = report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory;
            if (!"ALL".equalsIgnoreCase(selectedFilter) && !selectedFilter.equalsIgnoreCase(category)) {
                continue;
            }

            if (report.latitude != 0.0 && report.longitude != 0.0) {
                LatLng pos = new LatLng(report.latitude, report.longitude);
                float hue = BitmapDescriptorFactory.HUE_RED;

                if ("Garbage".equalsIgnoreCase(category)) {
                    hue = BitmapDescriptorFactory.HUE_ORANGE;
                } else if ("Water_Leak_Sewer".equalsIgnoreCase(category)) {
                    hue = BitmapDescriptorFactory.HUE_AZURE;
                } else if ("Potholes".equalsIgnoreCase(category)) {
                    hue = BitmapDescriptorFactory.HUE_RED;
                }

                String title = CivicIssueClassifier.getDisplayNameForLabel(category);
                String snippet = report.locality != null ? report.locality : "Bengaluru";

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(title)
                        .snippet(snippet + " • " + (report.status != null ? report.status.replace("_", " ") : "DRAFT"))
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                );
                if (marker != null) {
                    marker.setTag(report.reportId);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
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
    public void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
