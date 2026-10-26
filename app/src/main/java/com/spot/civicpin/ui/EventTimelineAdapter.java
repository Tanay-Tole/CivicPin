package com.spot.civicpin.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spot.civicpin.R;
import com.spot.civicpin.data.ReportEventEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventTimelineAdapter extends RecyclerView.Adapter<EventTimelineAdapter.EventViewHolder> {

    private List<ReportEventEntity> events = new ArrayList<>();

    public void setEvents(List<ReportEventEntity> newEvents) {
        this.events = newEvents != null ? newEvents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        ReportEventEntity event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventDesc;
        TextView tvEventTime;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventDesc = itemView.findViewById(R.id.tv_event_desc);
            tvEventTime = itemView.findViewById(R.id.tv_event_time);
        }

        public void bind(ReportEventEntity event) {
            tvEventDesc.setText(event.description);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvEventTime.setText(sdf.format(new Date(event.timestamp)));
        }
    }
}
