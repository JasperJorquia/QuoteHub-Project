package com.example.quotehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<ActivityItem> activityList;

    public ActivityAdapter(List<ActivityItem> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityItem item = activityList.get(position);
        holder.activityIcon.setText(item.getIcon());
        holder.activityTitle.setText(item.getTitle());
        holder.activityValue.setText(item.getValue());
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView activityIcon;
        TextView activityTitle;
        TextView activityValue;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityIcon = itemView.findViewById(R.id.activityIcon);
            activityTitle = itemView.findViewById(R.id.activityTitle);
            activityValue = itemView.findViewById(R.id.activityValue);
        }
    }
}