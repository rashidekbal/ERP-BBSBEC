package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemAssignSubjectOptionBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignSubjectAdapter extends RecyclerView.Adapter<AssignSubjectAdapter.ViewHolder> {

    public interface OnSubjectSelectedListener {
        void onSubjectSelected(SubjectSchema subject);
    }

    private final List<SubjectSchema> subjects = new ArrayList<>();
    private final Integer currentAssignedSid;
    private final Map<Integer, Integer> slotCountMap = new HashMap<>();
    private final OnSubjectSelectedListener listener;

    public AssignSubjectAdapter(List<SubjectSchema> subjects, Integer currentAssignedSid, Map<Integer, Integer> slotCountMap, OnSubjectSelectedListener listener) {
        if (subjects != null) {
            this.subjects.addAll(subjects);
        }
        this.currentAssignedSid = currentAssignedSid;
        if (slotCountMap != null) {
            this.slotCountMap.putAll(slotCountMap);
        }
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAssignSubjectOptionBinding binding = ItemAssignSubjectOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectSchema subject = subjects.get(position);
        int bookedCount = slotCountMap.getOrDefault(subject.getSid(), 0);
        holder.bind(subject, currentAssignedSid, bookedCount, listener);
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAssignSubjectOptionBinding binding;

        public ViewHolder(@NonNull ItemAssignSubjectOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SubjectSchema subject, Integer currentAssignedSid, int bookedCount, OnSubjectSelectedListener listener) {
            binding.subjectName.setText(subject.getSubjectName());

            int color = com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager.getAccentColorInt();
            if (subject.getUniqueColorCode() != null && !subject.getUniqueColorCode().isEmpty()) {
                try {
                    color = Color.parseColor(subject.getUniqueColorCode());
                } catch (Exception ignored) {}
            }
            binding.subjectColorDot.setCardBackgroundColor(color);

            int maxSlots = subject.isLab()
                    ? (subject.getLabsPerWeek() > 0 ? subject.getLabsPerWeek() : subject.getClassesPerWeek())
                    : (subject.getClassesPerWeek() > 0 ? subject.getClassesPerWeek() : 1);
            if (maxSlots <= 0) maxSlots = 1;

            boolean isSelected = (currentAssignedSid != null && currentAssignedSid == subject.getSid());
            boolean isFull = (!isSelected && bookedCount >= maxSlots);

            String typeStr = subject.isLab() ? "labs/wk" : "classes/wk";
            String slotStatus = bookedCount + "/" + maxSlots + " " + typeStr;
            if (isFull) {
                slotStatus += " (Full)";
            }

            String details = "Criteria: " + subject.getAttendanceCriteria() + "% • " + slotStatus;
            binding.subjectDetails.setText(details);

            if (isFull) {
                binding.subjectDetails.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.danger));
                binding.getRoot().setAlpha(0.6f);
            } else {
                binding.subjectDetails.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dim));
                binding.getRoot().setAlpha(1.0f);
            }

            binding.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            if (isSelected) {
                binding.checkIcon.setImageTintList(android.content.res.ColorStateList.valueOf(com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager.getAccentColorInt()));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubjectSelected(subject);
                }
            });
        }
    }
}
