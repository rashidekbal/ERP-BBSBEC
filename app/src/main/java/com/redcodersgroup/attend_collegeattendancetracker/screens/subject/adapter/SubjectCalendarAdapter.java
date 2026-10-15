package com.redcodersgroup.attend_collegeattendancetracker.screens.subject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemSubjectCalendarDayBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;

import java.util.List;

public class SubjectCalendarAdapter extends RecyclerView.Adapter<SubjectCalendarAdapter.ViewHolder> {

    private final Context context;
    private final List<CalendarDayModel> calendarDays;
    private OnDayClickListener onDayClickListener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDayModel dayModel);
    }

    public SubjectCalendarAdapter(Context context, List<CalendarDayModel> calendarDays) {
        this.context = context;
        this.calendarDays = calendarDays;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubjectCalendarDayBinding binding = ItemSubjectCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDayModel day = calendarDays.get(position);
        holder.bind(context, day, onDayClickListener);
    }

    @Override
    public int getItemCount() {
        return calendarDays != null ? calendarDays.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubjectCalendarDayBinding binding;

        public ViewHolder(@NonNull ItemSubjectCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Context context, CalendarDayModel day, OnDayClickListener listener) {
            if (day == null || day.getDayNumber() <= 0) {
                binding.dayText.setText("");
                binding.dayContainer.setBackgroundResource(android.R.color.transparent);
                binding.statusDot.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(null);
                binding.getRoot().setClickable(false);
                return;
            }

            binding.dayText.setText(String.valueOf(day.getDayNumber()));
            binding.getRoot().setClickable(true);

            // Configure cell container and text styling matching defined status colors
            if (day.getPresentCount() > 0) {
                // Attended: text color success
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.dayText.getPaint().setFakeBoldText(true);
            } else if (day.getAbsentCount() > 0) {
                // Missed: text color danger
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.dayText.getPaint().setFakeBoldText(true);
            } else if (day.getCancelledCount() > 0) {
                // Cancelled: text color color_teal
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.color_teal));
                binding.dayText.getPaint().setFakeBoldText(true);
            } else if (day.isToday()) {
                // Unmarked today cell
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_today);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.all_white));
                binding.dayText.getPaint().setFakeBoldText(true);
            } else if (day.isHoliday() || day.isWeekend()) {
                // Holiday / Weekend off-day: danger color outline & red text matching 'o Holiday' legend
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_holiday);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.dayText.getPaint().setFakeBoldText(false);
            } else {
                binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                binding.dayText.setTextColor(ContextCompat.getColor(context, day.isCurrentMonth() ? R.color.text : R.color.text_dim));
                binding.dayText.getPaint().setFakeBoldText(false);
            }

            // Alpha opacity for non-current month dates
            binding.dayContainer.setAlpha(day.isCurrentMonth() ? 1.0f : 0.4f);

            // Show distinct status dot for attendance records
            binding.statusDot.setBackgroundTintList(null);
            if (day.getPresentCount() > 0) {
                binding.statusDot.setVisibility(View.VISIBLE);
                binding.statusDot.setBackgroundResource(R.drawable.dot_shape_present);
            } else if (day.getAbsentCount() > 0) {
                binding.statusDot.setVisibility(View.VISIBLE);
                binding.statusDot.setBackgroundResource(R.drawable.dot_shape_absent);
            } else if (day.getCancelledCount() > 0) {
                binding.statusDot.setVisibility(View.VISIBLE);
                binding.statusDot.setBackgroundResource(R.drawable.dot_shape_cancelled);
            } else {
                binding.statusDot.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
            });
        }
    }
}
