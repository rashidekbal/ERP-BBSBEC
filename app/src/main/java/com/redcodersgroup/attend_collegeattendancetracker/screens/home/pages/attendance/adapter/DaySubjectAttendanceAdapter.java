package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ItemDaySubjectAttendanceBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.DaySubjectAttendanceModel;

import java.util.List;

public class DaySubjectAttendanceAdapter extends RecyclerView.Adapter<DaySubjectAttendanceAdapter.ViewHolder> {

    public interface OnStatusChangeListener {
        void onStatusChange(DaySubjectAttendanceModel item, String newStatus);
    }

    private final Context context;
    private final List<DaySubjectAttendanceModel> list;
    private final OnStatusChangeListener listener;

    public DaySubjectAttendanceAdapter(Context context, List<DaySubjectAttendanceModel> list, OnStatusChangeListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDaySubjectAttendanceBinding binding = ItemDaySubjectAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDaySubjectAttendanceBinding binding;

        public ViewHolder(@NonNull ItemDaySubjectAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DaySubjectAttendanceModel item) {
            binding.subjectName.setText(item.getDisplayName());

            com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema sub = item.getSubject();
            String faculty = (sub != null && sub.getTeacherName() != null) ? sub.getTeacherName().trim() : null;
            String room = (sub != null && sub.getRoomNo() != null) ? sub.getRoomNo().trim() : null;
            StringBuilder facultyRoom = new StringBuilder();
            if (faculty != null && !faculty.isEmpty()) {
                facultyRoom.append(faculty);
            }
            if (room != null && !room.isEmpty()) {
                if (facultyRoom.length() > 0) facultyRoom.append(" · Room ");
                else facultyRoom.append("Room ");
                facultyRoom.append(room);
            }
            if (facultyRoom.length() > 0) {
                binding.subjectFacultyAndRoom.setText(facultyRoom.toString());
                binding.subjectFacultyAndRoom.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.subjectFacultyAndRoom.setVisibility(android.view.View.GONE);
            }

            // Subject color indicator
            try {
                if (item.getColorCode() != null && !item.getColorCode().isEmpty()) {
                    binding.colorIndicator.setCardBackgroundColor(Color.parseColor(item.getColorCode()));
                } else {
                    binding.colorIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
                }
            } catch (Exception e) {
                binding.colorIndicator.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            }

            // Status label & badge styling (strictly read-only from BBSBEC ERP)
            String status = item.getStatus();
            String typeStr = item.isLab() ? "Lab" : "Theory";

            if (AttendanceConstants.isPresent(status)) {
                int successColor = ContextCompat.getColor(context, R.color.success);
                int successBg = Color.argb(35, Color.red(successColor), Color.green(successColor), Color.blue(successColor));
                binding.statusChip.setCardBackgroundColor(successBg);
                binding.statusChipText.setText("Present");
                binding.statusChipText.setTextColor(successColor);

                binding.subjectStatusLabel.setText(typeStr + " · Attended");
                binding.subjectStatusLabel.setTextColor(successColor);
            } else if (AttendanceConstants.isAbsent(status)) {
                int dangerColor = ContextCompat.getColor(context, R.color.danger);
                int dangerBg = Color.argb(35, Color.red(dangerColor), Color.green(dangerColor), Color.blue(dangerColor));
                binding.statusChip.setCardBackgroundColor(dangerBg);
                binding.statusChipText.setText("Absent");
                binding.statusChipText.setTextColor(dangerColor);

                binding.subjectStatusLabel.setText(typeStr + " · Missed");
                binding.subjectStatusLabel.setTextColor(dangerColor);
            } else {
                int textDimColor = ContextCompat.getColor(context, R.color.text_dim);
                binding.statusChip.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface2));
                binding.statusChipText.setText("No Record");
                binding.statusChipText.setTextColor(textDimColor);

                binding.subjectStatusLabel.setText(typeStr + " · No ERP record logged");
                binding.subjectStatusLabel.setTextColor(textDimColor);
            }
        }
    }
}
