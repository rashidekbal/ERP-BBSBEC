package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemTimetablePeriodBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model.TimeTablePeriodModel;

import java.util.ArrayList;
import java.util.List;

public class TimeTablePeriodAdapter extends RecyclerView.Adapter<TimeTablePeriodAdapter.ViewHolder> {

    public interface OnPeriodClickListener {
        void onPeriodClick(TimeTablePeriodModel periodModel);
    }

    private final List<TimeTablePeriodModel> periods = new ArrayList<>();
    private final OnPeriodClickListener listener;

    public TimeTablePeriodAdapter(OnPeriodClickListener listener) {
        this.listener = listener;
    }

    public void setPeriods(List<TimeTablePeriodModel> newPeriods) {
        this.periods.clear();
        if (newPeriods != null) {
            this.periods.addAll(newPeriods);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimetablePeriodBinding binding = ItemTimetablePeriodBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(periods.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return periods.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimetablePeriodBinding binding;

        public ViewHolder(@NonNull ItemTimetablePeriodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TimeTablePeriodModel model, OnPeriodClickListener listener) {
            binding.periodNumberText.setText(String.format("PERIOD %d", model.getPeriodNumber()));
            binding.periodTimeRangeText.setText(model.getTimeRange());

            SubjectSchema subject = model.getSubject();
            if (model.isAssigned() && subject != null) {
                // Color strip
                int color = com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager.getAccentColorInt();
                if (subject.getUniqueColorCode() != null && !subject.getUniqueColorCode().isEmpty()) {
                    try {
                        color = Color.parseColor(subject.getUniqueColorCode());
                    } catch (Exception ignored) {}
                }
                binding.colorStrip.setBackgroundColor(color);
                binding.colorStrip.setVisibility(View.VISIBLE);

                // Subject info
                binding.subjectNameText.setText(subject.getSubjectName());
                binding.subjectNameText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text));

                StringBuilder subtext = new StringBuilder();
                if (subject.getTeacherName() != null && !subject.getTeacherName().trim().isEmpty()) {
                    subtext.append(subject.getTeacherName().trim());
                }
                if (subject.getRoomNo() != null && !subject.getRoomNo().trim().isEmpty()) {
                    if (subtext.length() > 0) subtext.append(" · Room ");
                    else subtext.append("Room ");
                    subtext.append(subject.getRoomNo().trim());
                }
                if (subtext.length() > 0) {
                    subtext.append(" • ");
                }
                subtext.append("Criteria: ").append(subject.getAttendanceCriteria()).append("% • ")
                        .append(subject.isLab() ? (subject.getLabsPerWeek() > 0 ? subject.getLabsPerWeek() : subject.getClassesPerWeek()) + " labs/wk" : subject.getClassesPerWeek() + " classes/wk");
                binding.subjectSubtext.setText(subtext.toString());
                binding.subjectSubtext.setVisibility(View.VISIBLE);

                // Status tag chip (Lecture / Lab)
                binding.labTag.setVisibility(View.VISIBLE);
                if (subject.isLab()) {
                    binding.labTag.setText(subject.getLabsPerWeek() >= 2 ? "2-Period Lab" : "Lab");
                    binding.labTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.danger));
                } else {
                    binding.labTag.setText("Lecture");
                    binding.labTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dim));
                }

                binding.actionIcon.setVisibility(View.GONE);
            } else {
                // Free Period / Unassigned
                binding.colorStrip.setVisibility(View.INVISIBLE);
                binding.subjectNameText.setText("Free Period");
                binding.subjectNameText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dim));
                binding.subjectSubtext.setText("No class scheduled");
                binding.subjectSubtext.setVisibility(View.VISIBLE);
                binding.labTag.setText("Free Slot");
                binding.labTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dim));
                binding.labTag.setVisibility(View.VISIBLE);

                binding.actionIcon.setVisibility(View.GONE);
            }

            if (listener != null) {
                binding.getRoot().setClickable(true);
                binding.getRoot().setOnClickListener(v -> listener.onPeriodClick(model));
            } else {
                binding.getRoot().setClickable(false);
                binding.getRoot().setOnClickListener(null);
            }
        }
    }
}
