package com.spot.civicpin.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.spot.civicpin.R;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.ui.ReportAdapter;
import com.spot.civicpin.ui.details.ReportDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private CivicReportRepository repository;
    private ReportAdapter adapter;
    private List<CivicReportEntity> masterList = new ArrayList<>();

    private TextInputEditText etSearch;
    private ChipGroup cgHistoryFilters;
    private TextView tvEmptyHistory;
    private RecyclerView rvHistory;

    private String selectedStatus = "ALL";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        repository = new CivicReportRepository(requireContext());

        etSearch = view.findViewById(R.id.et_search_history);
        cgHistoryFilters = view.findViewById(R.id.cg_history_filters);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        rvHistory = view.findViewById(R.id.rv_history);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(report -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);
            intent.putExtra("report_id", report.reportId);
            startActivity(intent);
        });
        rvHistory.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                filterAndApply();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cgHistoryFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_hist_draft)) {
                selectedStatus = "DRAFT";
            } else if (checkedIds.contains(R.id.chip_hist_submitted)) {
                selectedStatus = "USER_MARKED_SUBMITTED";
            } else if (checkedIds.contains(R.id.chip_hist_resolved)) {
                selectedStatus = "USER_MARKED_RESOLVED";
            } else {
                selectedStatus = "ALL";
            }
            filterAndApply();
        });

        observeReports();

        return view;
    }

    private void observeReports() {
        repository.getAllReports().observe(getViewLifecycleOwner(), reports -> {
            masterList = reports != null ? reports : new ArrayList<>();
            filterAndApply();
        });
    }

    private void filterAndApply() {
        List<CivicReportEntity> filtered = new ArrayList<>();
        for (CivicReportEntity r : masterList) {
            boolean matchesStatus = "ALL".equalsIgnoreCase(selectedStatus) || selectedStatus.equalsIgnoreCase(r.status);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (r.locality != null && r.locality.toLowerCase().contains(searchQuery)) ||
                    (r.confirmedCategory != null && r.confirmedCategory.toLowerCase().contains(searchQuery)) ||
                    (r.description != null && r.description.toLowerCase().contains(searchQuery)) ||
                    (r.reportId != null && r.reportId.toLowerCase().contains(searchQuery));

            if (matchesStatus && matchesSearch) {
                filtered.add(r);
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.setReports(filtered);
        }
    }
}
