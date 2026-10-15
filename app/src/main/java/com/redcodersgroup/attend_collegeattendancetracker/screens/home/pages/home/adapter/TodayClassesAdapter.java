package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemTodayClassBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.model.TodayClassModel;

import java.util.List;
import java.util.Locale;

public class TodayClassesAdapter extends RecyclerView.Adapter<TodayClassesAdapter.ViewHolder> {

    public interface OnClassClickListener {
        void onClassClick(TodayClassModel model);
    }

    private final Context context;
    private final List<TodayClassModel> classList;
    private final OnClassClickListener clickListener;

    public TodayClassesAdapter(Context context, List<TodayClassModel> classList) {
        this(context, classList, null);
    }

    public TodayClassesAdapter(Context context, List<TodayClassModel> classList, OnClassClickListener clickListener) {
        this.context = context;
        this.classList = classList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTodayClassBinding binding = ItemTodayClassBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(classList.get(position));
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTodayClassBinding binding;

        public ViewHolder(@NonNull ItemTodayClassBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TodayClassModel model) {
            binding.className.setText(model.getSubjectName());
            
            com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema sub = model.getSubject();
            String faculty = (sub != null && sub.getTeacherName() != null) ? sub.getTeacherName().trim() : null;
            String room = (sub != null && sub.getRoomNo() != null) ? sub.getRoomNo().trim() : null;

            if (faculty != null && !faculty.isEmpty()) {
                binding.classFaculty.setText(faculty);
                binding.classFaculty.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.classFaculty.setVisibility(android.view.View.GONE);
            }

            String timeRangeText = model.getTimeRange();
            if (room != null && !room.isEmpty()) {
                timeRangeText += " · Room " + room;
            }
            binding.classTime.setText(timeRangeText);

            // Subject color indicator
            try {
                if (model.getColorCode() != null && !model.getColorCode().isEmpty()) {
                    binding.colorIndicator.setCardBackgroundColor(Color.parseColor(model.getColorCode()));
                } else {
                    binding.colorIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
                }
            } catch (Exception e) {
                binding.colorIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            }

            String status = model.getStatus() != null ? model.getStatus().toUpperCase(Locale.getDefault()) : "";

            if ("PRESENT".equals(status) || "ATTENDED".equals(status)) {
                binding.classBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_dim));
                binding.classBadgeText.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.classBadgeText.setText("Present");
            } else if ("ABSENT".equals(status) || "MISSED".equals(status)) {
                binding.classBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.danger_dim));
                binding.classBadgeText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.classBadgeText.setText("Absent");
            } else if ("CANCELLED".equals(status) || "CANCEL".equals(status)) {
                binding.classBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface2));
                binding.classBadgeText.setTextColor(ContextCompat.getColor(context, R.color.text_dim));
                binding.classBadgeText.setText("Cancelled");
            } else {
                binding.classBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface2));
                binding.classBadgeText.setTextColor(ContextCompat.getColor(context, R.color.text_dim));
                binding.classBadgeText.setText("Period " + model.getPeriodNumber());
            }

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClassClick(model);
                }
            });
        }
    }
}
