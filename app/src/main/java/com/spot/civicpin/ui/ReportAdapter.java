package com.spot.civicpin.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    // Shared look-ups so every screen shows a category and a status the same way.

    public static int iconFor(String category) {
        if ("Garbage".equalsIgnoreCase(category)) return R.drawable.ic_garbage;
        if ("Water_Leak_Sewer".equalsIgnoreCase(category)) return R.drawable.ic_water;
        if ("Potholes".equalsIgnoreCase(category)) return R.drawable.ic_pothole;
        return R.drawable.ic_others;
    }

    public static int colorFor(Context c, String category) {
        int res = R.color.cat_others;
        if ("Garbage".equalsIgnoreCase(category)) res = R.color.cat_garbage;
        else if ("Water_Leak_Sewer".equalsIgnoreCase(category)) res = R.color.cat_water;
        else if ("Potholes".equalsIgnoreCase(category)) res = R.color.cat_potholes;
        return ContextCompat.getColor(c, res);
    }

    public static int softColorFor(Context c, String category) {
        int res = R.color.cat_others_soft;
        if ("Garbage".equalsIgnoreCase(category)) res = R.color.cat_garbage_soft;
        else if ("Water_Leak_Sewer".equalsIgnoreCase(category)) res = R.color.cat_water_soft;
        else if ("Potholes".equalsIgnoreCase(category)) res = R.color.cat_potholes_soft;
        return ContextCompat.getColor(c, res);
    }

    public static int statusLabelRes(String status) {
        if ("USER_MARKED_SUBMITTED".equals(status)) return R.string.status_submitted;
        if ("USER_MARKED_RESOLVED".equals(status)) return R.string.status_resolved;
        if ("EMAIL_COMPOSER_OPENED".equals(status)) return R.string.status_composer;
        return R.string.status_draft;
    }

    /** English fallback for places without a Context (map preview line). */
    public static String statusLabel(String status) {
        if ("USER_MARKED_SUBMITTED".equals(status)) return "Submitted";
        if ("USER_MARKED_RESOLVED".equals(status)) return "Resolved";
        if ("EMAIL_COMPOSER_OPENED".equals(status)) return "Email opened";
        return "Draft";
    }

    public static int statusColor(Context c, String status) {
        int res = R.color.status_draft;
        if ("USER_MARKED_SUBMITTED".equals(status)) res = R.color.status_submitted;
        else if ("USER_MARKED_RESOLVED".equals(status)) res = R.color.status_resolved;
        else if ("EMAIL_COMPOSER_OPENED".equals(status)) res = R.color.status_composer;
        return ContextCompat.getColor(c, res);
    }

    /** Decode a small version of the photo so list scrolling stays smooth. */
    public static Bitmap loadThumb(String path, int targetPx) {
        if (path == null || path.isEmpty() || !new File(path).exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int sample = 1;
        while (Math.min(o.outWidth, o.outHeight) / (sample * 2) >= targetPx) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, o);
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reports.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumbnail, imgCategoryIcon;
        final View thumbIconBg, statusDot;
        final TextView tvCategory, tvLocality, tvRefAndDate, tvStatusBadge;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            imgCategoryIcon = itemView.findViewById(R.id.img_category_icon);
            thumbIconBg = itemView.findViewById(R.id.thumb_icon_bg);
            statusDot = itemView.findViewById(R.id.status_dot);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocality = itemView.findViewById(R.id.tv_locality);
            tvRefAndDate = itemView.findViewById(R.id.tv_ref_and_date);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
        }

        void bind(CivicReportEntity report, OnReportClickListener listener) {
            Context c = itemView.getContext();
            String category = report.confirmedCategory != null ? report.confirmedCategory : report.aiPredictedCategory;
            tvCategory.setText(CivicIssueClassifier.getDisplayNameForLabel(category));
            tvLocality.setText(report.locality != null && !report.locality.isEmpty() ? report.locality : "Bengaluru");

            String dateStr = new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(report.createdAt));
            String refShort = report.reportId.length() >= 8 ? report.reportId.substring(0, 8).toUpperCase() : report.reportId.toUpperCase();
            tvRefAndDate.setText(String.format("%s  ·  CP-%s", dateStr, refShort));

            tvStatusBadge.setText(statusLabelRes(report.status));
            statusDot.setBackgroundTintList(ColorStateList.valueOf(statusColor(c, report.status)));

            imgCategoryIcon.setImageResource(iconFor(category));
            imgCategoryIcon.setImageTintList(ColorStateList.valueOf(colorFor(c, category)));
            thumbIconBg.setBackgroundTintList(ColorStateList.valueOf(softColorFor(c, category)));

            Bitmap thumb = loadThumb(report.imagePath, 160);
            if (thumb != null) {
                imgThumbnail.setImageBitmap(thumb);
                imgThumbnail.setVisibility(View.VISIBLE);
            } else {
                imgThumbnail.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(report);
            });
        }
    }
}
