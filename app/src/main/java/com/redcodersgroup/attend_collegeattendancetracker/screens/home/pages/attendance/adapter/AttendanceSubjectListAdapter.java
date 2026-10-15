package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.SubjectAttendanceListCardRvBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.SubjectAttendanceModel;

import java.util.List;

public class AttendanceSubjectListAdapter extends RecyclerView.Adapter<AttendanceSubjectListAdapter.SubjectViewHolder> {

    private final Context context;
    private final List<SubjectAttendanceModel> subjectAttendanceList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(SubjectAttendanceModel item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(SubjectAttendanceModel item);
    }

    public AttendanceSubjectListAdapter(Context context, List<SubjectAttendanceModel> subjectAttendanceList) {
        this.context = context;
        this.subjectAttendanceList = subjectAttendanceList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SubjectAttendanceListCardRvBinding binding = SubjectAttendanceListCardRvBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SubjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        if (subjectAttendanceList == null || position < 0 || position >= subjectAttendanceList.size()) return;

        SubjectAttendanceModel item = subjectAttendanceList.get(position);
        holder.bind(item, context, listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return subjectAttendanceList != null ? subjectAttendanceList.size() : 0;
    }

    public static class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final SubjectAttendanceListCardRvBinding binding;
        private int lastBoundPct = -1;

        public SubjectViewHolder(@NonNull SubjectAttendanceListCardRvBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SubjectAttendanceModel item, Context context, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
            if (item.getSubject() != null && item.getSubject().getUniqueColorCode() != null
                    && !item.getSubject().getUniqueColorCode().isEmpty()) {
                try {
                    binding.accentColor
                            .setCardBackgroundColor(Color.parseColor(item.getSubject().getUniqueColorCode()));
                } catch (Exception e) {
                    binding.accentColor.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
                }
            } else {
                binding.accentColor.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            }

            binding.ssubjectName.setText(getDisplayName(item));

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

            String unit = item.isLab() ? "labs" : "classes";
            String attendedText = item.getAttendedClasses() + " / " + item.getTotalClasses() + " " + unit + " attended";
            binding.classAttended.setText(attendedText);

            int pct = item.getPercentage();
            int criteria = item.getCriteria() > 0 ? item.getCriteria() : 75;
            int statusColor = ContextCompat.getColor(context, pct >= criteria ? R.color.success : R.color.danger);

            binding.attendancePercentage.setTextColor(statusColor);

            if (binding.miniRingIndicator != null) {
                binding.miniRingIndicator.setIndicatorColor(statusColor);
                if (lastBoundPct != pct) {
                    lastBoundPct = pct;
                    com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil.animateProgress(
                            binding.miniRingIndicator, pct, binding.attendancePercentage);
                } else {
                    binding.miniRingIndicator.setProgress(pct);
                    binding.attendancePercentage.setText(pct + "%");
                }
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item);
                    return true;
                }
                return false;
            });
        }
    }

    private static String getDisplayName(SubjectAttendanceModel item) {
        if (item == null) return "";
        String name = item.getDisplayName();
        if (item.isLab() && name != null && !name.toLowerCase().contains("lab")) {
            return name + " Lab";
        }
        return name != null ? name : "";
    }
}
