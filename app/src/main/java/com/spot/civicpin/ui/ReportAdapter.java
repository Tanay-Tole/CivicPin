package com.spot.civicpin.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spot.civicpin.R;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.data.CivicReportEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(CivicReportEntity report);
    }

    private List<CivicReportEntity> reports = new ArrayList<>();
    private final OnReportClickListener listener;

    public ReportAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void setReports(List<CivicReportEntity> newReports) {
        this.reports = newReports != null ? newReports : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        CivicReportEntity report = reports.get(position);
        holder.bind(report, listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView tvCategory;
        TextView tvLocality;
        TextView tvRefAndDate;
        TextView tvStatusBadge;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocality = itemView.findViewById(R.id.tv_locality);
            tvRefAndDate = itemView.findViewById(R.id.tv_ref_and_date);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
        }

        public void bind(CivicReportEntity report, OnReportClickListener listener) {
            String categoryDisplay = CivicIssueClassifier.getDisplayNameForLabel(
                    report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory
            );
            tvCategory.setText(categoryDisplay);

            String localityStr = report.locality != null && !report.locality.isEmpty() ? report.locality : "Bengaluru";
            tvLocality.setText(localityStr);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(report.createdAt));
            String refShort = report.reportId.length() >= 8 ? report.reportId.substring(0, 8).toUpperCase() : report.reportId.toUpperCase();
            tvRefAndDate.setText(String.format("Ref: CP-%s • %s", refShort, dateStr));

            // Status Badge Formatting
            String status = report.status != null ? report.status : "DRAFT";
            tvStatusBadge.setText(status.replace("_", " "));
            switch (status) {
                case "USER_MARKED_SUBMITTED":
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#27AE60"));
                    break;
                case "USER_MARKED_RESOLVED":
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#8E44AD"));
                    break;
                case "EMAIL_COMPOSER_OPENED":
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#2980B9"));
                    break;
                case "DRAFT":
                default:
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#7F8C8D"));
                    break;
            }

            // Thumbnail Image Loading
            if (report.imagePath != null && !report.imagePath.isEmpty()) {
                File file = new File(report.imagePath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bitmap != null) {
                        imgThumbnail.setImageBitmap(bitmap);
                    } else {
                        imgThumbnail.setImageResource(R.drawable.ic_report);
                    }
                } else {
                    imgThumbnail.setImageResource(R.drawable.ic_report);
                }
            } else {
                imgThumbnail.setImageResource(R.drawable.ic_report);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(report);
            });
        }
    }
}
