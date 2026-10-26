package com.spot.civicpin.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spot.civicpin.MainActivity;
import com.spot.civicpin.R;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.ui.ReportAdapter;
import com.spot.civicpin.ui.details.ReportDetailActivity;

public class HomeFragment extends Fragment {

    private CivicReportRepository repository;
    private ReportAdapter adapter;

    private TextView tvStatTotal;
    private TextView tvStatDrafts;
    private TextView tvStatComposer;
    private TextView tvStatSubmitted;
    private TextView tvEmptyRecent;
    private RecyclerView rvRecentReports;

    private int skylineTapCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        repository = new CivicReportRepository(requireContext());

        tvStatTotal = view.findViewById(R.id.tv_stat_total);
        tvStatDrafts = view.findViewById(R.id.tv_stat_drafts);
        tvStatComposer = view.findViewById(R.id.tv_stat_composer);
        tvStatSubmitted = view.findViewById(R.id.tv_stat_submitted);
        tvEmptyRecent = view.findViewById(R.id.tv_empty_recent);
        rvRecentReports = view.findViewById(R.id.rv_recent_reports);
        ImageView imgSkyline = view.findViewById(R.id.img_skyline);

        MaterialButton btnReportIssue = view.findViewById(R.id.btn_report_issue);
        MaterialButton btnFastManual = view.findViewById(R.id.btn_fast_manual);

        rvRecentReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(report -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);
            intent.putExtra("report_id", report.reportId);
            startActivity(intent);
        });
        rvRecentReports.setAdapter(adapter);

        btnReportIssue.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToReportTab();
            }
        });

        btnFastManual.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToReportTab();
            }
        });

        // Easter Egg 1 — Namma Bengaluru (5 taps on skyline)
        imgSkyline.setOnClickListener(v -> {
            skylineTapCount++;
            if (skylineTapCount >= 5) {
                skylineTapCount = 0;
                Toast.makeText(requireContext(), getString(R.string.ee_namma_ooru), Toast.LENGTH_LONG).show();
            }
        });

        observeData();

        return view;
    }

    private void observeData() {
        repository.getTotalCount().observe(getViewLifecycleOwner(), count ->
                tvStatTotal.setText(String.valueOf(count != null ? count : 0))
        );

        repository.getDraftCount().observe(getViewLifecycleOwner(), count ->
                tvStatDrafts.setText(String.valueOf(count != null ? count : 0))
        );

        repository.getComposerOpenedCount().observe(getViewLifecycleOwner(), count ->
                tvStatComposer.setText(String.valueOf(count != null ? count : 0))
        );

        repository.getSubmittedCount().observe(getViewLifecycleOwner(), count ->
                tvStatSubmitted.setText(String.valueOf(count != null ? count : 0))
        );

        repository.getRecentReports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null && !reports.isEmpty()) {
                tvEmptyRecent.setVisibility(View.GONE);
                rvRecentReports.setVisibility(View.VISIBLE);
                adapter.setReports(reports);
            } else {
                tvEmptyRecent.setVisibility(View.VISIBLE);
                rvRecentReports.setVisibility(View.GONE);
            }
        });
    }
}
