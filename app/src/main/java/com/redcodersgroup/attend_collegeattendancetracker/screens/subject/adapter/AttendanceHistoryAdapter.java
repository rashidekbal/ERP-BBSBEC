package com.redcodersgroup.attend_collegeattendancetracker.screens.subject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemAttendanceHistoryBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<AttendanceSchema> historyList;
    private List<TimeTableDaySchema> days;
    private List<DayScheduleSchema> schedules;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(AttendanceSchema item);
    }

    public AttendanceHistoryAdapter(Context context, List<AttendanceSchema> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    public void setScheduleContext(List<TimeTableDaySchema> days, List<DayScheduleSchema> schedules) {
        this.days = days;
        this.schedules = schedules;
        notifyDataSetChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceHistoryBinding binding = ItemAttendanceHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceSchema item = historyList.get(position);
        holder.bind(context, item, days, schedules, onDeleteClickListener);
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceHistoryBinding binding;

        public ViewHolder(@NonNull ItemAttendanceHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Context context, AttendanceSchema item, List<TimeTableDaySchema> days, List<DayScheduleSchema> schedules, OnDeleteClickListener onDeleteClickListener) {
            String dateStr = item.getDate();
            try {
                SimpleDateFormat inputSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                Date parsedDate = inputSdf.parse(dateStr);
                if (parsedDate != null) {
                    SimpleDateFormat outputSdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
                    binding.historyDate.setText(outputSdf.format(parsedDate));
                } else {
                    binding.historyDate.setText(dateStr);
                }
            } catch (Exception e) {
                binding.historyDate.setText(dateStr);
            }

            int periods = AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, item.getSid(), days, schedules);
            if (item.isLab()) {
                binding.historyType.setText(periods > 1 ? "Laboratory Practical (" + periods + " Labs)" : "Laboratory Practical");
            } else {
                binding.historyType.setText(periods > 1 ? "Theory Class (" + periods + " Periods)" : "Theory Class");
            }

            String status = item.getStatus();
            if (AttendanceConstants.isPresent(status)) {
                binding.statusText.setText("PRESENT");
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.success));
                binding.statusIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success));
            } else if (AttendanceConstants.isCancelled(status)) {
                binding.statusText.setText("CANCELLED");
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.color_teal));
                binding.statusIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_teal));
            } else {
                binding.statusText.setText("ABSENT");
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.danger));
                binding.statusIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.danger));
            }

            binding.deleteHistoryBtn.setVisibility(View.GONE);
            binding.deleteHistoryBtn.setOnClickListener(null);
        }
    }
}
