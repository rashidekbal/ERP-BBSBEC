package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.CalendarDayItemBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;

import java.util.List;

public class AttendanceCalendarAdapter extends RecyclerView.Adapter<AttendanceCalendarAdapter.CalendarViewHolder> {

    private final Context context;
    private final List<CalendarDayModel> calendarDays;
    private OnDayClickListener onDayClickListener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDayModel dayModel);
    }

    public AttendanceCalendarAdapter(Context context, List<CalendarDayModel> calendarDays) {
        this.context = context;
        this.calendarDays = calendarDays;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CalendarDayItemBinding binding = CalendarDayItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CalendarViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDayModel day = calendarDays.get(position);
        holder.bind(context, day, onDayClickListener);
    }

    @Override
    public int getItemCount() {
        return calendarDays != null ? calendarDays.size() : 0;
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final CalendarDayItemBinding binding;

        public CalendarViewHolder(@NonNull CalendarDayItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Context context, CalendarDayModel day, OnDayClickListener listener) {
            if (day == null || day.getDayNumber() <= 0) {
                binding.dayText.setText("");
                binding.dayContainer.setBackgroundResource(android.R.color.transparent);
                binding.getRoot().setOnClickListener(null);
                binding.getRoot().setClickable(false);
                return;
            }

            binding.dayText.setText(String.valueOf(day.getDayNumber()));
            binding.getRoot().setClickable(true);


            if (!day.isCurrentMonth()) {
                // Dimmed dates from previous or next month
                if (day.hasAttendance()) {
                    if (day.getAbsentCount() > 0 && day.getPresentCount() == 0) {
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_absent);
                    } else if (day.getPresentCount() > 0) {
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_present);
                    } else {
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                    }
                } else {
                    binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                }
                binding.dayContainer.setAlpha(0.35f);
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.text_dim));
                binding.dayText.getPaint().setFakeBoldText(false);

            } else {
                binding.dayContainer.setAlpha(1.0f);
                if (day.isToday()) {
                    // Today cell
                    binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_today);
                    binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.all_white));
                    binding.dayText.getPaint().setFakeBoldText(true);

                } else if (day.hasAttendance()) {
                    if (day.getAbsentCount() > 0 && day.getPresentCount() == 0) {
                        // All absent that day - Red outline
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_absent);
                        binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                        binding.dayText.getPaint().setFakeBoldText(true);
                    } else if (day.getPresentCount() > 0) {
                        // Present (or attended classes) - Green outline
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_present);
                        binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.success));
                        binding.dayText.getPaint().setFakeBoldText(true);
                    } else {
                        binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                        binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.text));
                        binding.dayText.getPaint().setFakeBoldText(false);
                    }
                } else if (day.isHoliday() || day.isWeekend()) {
                    // Holiday / Weekend off-day - Red outline & red text matching 'o Holiday' legend
                    binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_holiday);
                    binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                    binding.dayText.getPaint().setFakeBoldText(false);

                } else {
                    // Regular day
                    binding.dayContainer.setBackgroundResource(R.drawable.calander_cell_inactive);
                    binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.text));
                    binding.dayText.getPaint().setFakeBoldText(false);
                }
            }

            if (listener != null) {
                binding.getRoot().setClickable(true);
                binding.getRoot().setOnClickListener(v -> listener.onDayClick(day));
            } else {
                binding.getRoot().setOnClickListener(null);
                binding.getRoot().setClickable(false);
            }
        }
    }
}
