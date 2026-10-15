package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemTimetableSubjectSummaryBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model.TimeTableSubjectSummaryModel;

import java.util.ArrayList;
import java.util.List;

public class TimeTableSubjectSummaryAdapter extends RecyclerView.Adapter<TimeTableSubjectSummaryAdapter.ViewHolder> {

    private final List<TimeTableSubjectSummaryModel> items = new ArrayList<>();

    public void setItems(List<TimeTableSubjectSummaryModel> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimetableSubjectSummaryBinding binding = ItemTimetableSubjectSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimetableSubjectSummaryBinding binding;

        public ViewHolder(@NonNull ItemTimetableSubjectSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TimeTableSubjectSummaryModel model) {
            SubjectSchema subject = model.getSubject();
            if (subject == null) return;

            binding.subjectName.setText(subject.getSubjectName());

            int color = com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager.getAccentColorInt();
            if (subject.getUniqueColorCode() != null && !subject.getUniqueColorCode().isEmpty()) {
                try {
                    color = Color.parseColor(subject.getUniqueColorCode());
                } catch (Exception ignored) {}
            }
            binding.subjectColorDot.setCardBackgroundColor(color);

            int scheduled = model.getScheduledSlotsCount();
            int target = subject.getClassesPerWeek();
            String slotsText = scheduled + " / " + target + " slots/wk";
            binding.slotsCount.setText(slotsText);

            if (scheduled >= target && target > 0) {
                binding.slotsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.success));
            } else if (scheduled > 0) {
                binding.slotsCount.setTextColor(com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager.getAccentColorInt());
            } else {
                binding.slotsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dim));
            }
        }
    }
}
