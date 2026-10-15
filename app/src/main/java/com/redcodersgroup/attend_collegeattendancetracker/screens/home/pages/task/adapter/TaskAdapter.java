package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<AssignmentSchema> assignmentSchemaList;

    public TaskAdapter(Context context, List<AssignmentSchema> schemas) {
        this.assignmentSchemaList = schemas;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(LayoutInflater.from(context).inflate(R.layout.task_card_rv, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        if (assignmentSchemaList == null || position < 0 || position >= assignmentSchemaList.size()) return;

        AssignmentSchema assignment = assignmentSchemaList.get(position);
        String date = DateUtil.getDateString(assignment.getDueDate());

        holder.taskTitle.setText(assignment.getAssignmentTitle());
        if (holder.subHeading != null) holder.subHeading.setText(generateSubHeading(assignment));

        // Format Subject Badge
        String subjectName = assignment.getSubjectName() != null ? assignment.getSubjectName() : "Assignment";
        if (holder.subjectNameTag != null) holder.subjectNameTag.setText(subjectName);
        if (holder.taskAccentBar != null) holder.taskAccentBar.setBackgroundColor(getSubjectColor(subjectName, position));

        // Due date formatting
        if (holder.taskDueDate != null) {
            String[] dateParts = date.split("/");
            if (dateParts.length >= 3) {
                try {
                    String monthName = DateUtil.getMonthName(Integer.parseInt(dateParts[1]) - 1);
                    holder.taskDueDate.setText("Due " + monthName + " " + dateParts[0]);
                } catch (Exception e) {
                    holder.taskDueDate.setText("Due " + date);
                }
            } else {
                holder.taskDueDate.setText("Due " + date);
            }
        }

        // Reminder formatting
        if (holder.taskReminder != null) {
            holder.taskReminder.setText(getReminderText(assignment));
        }

        String[] dateParts = date.split("/");
        if (dateParts.length >= 2) {
            if (holder.day != null) holder.day.setText(dateParts[0]);
            try {
                if (holder.month != null) holder.month.setText(DateUtil.getMonthName(Integer.parseInt(dateParts[1]) - 1));
            } catch (Exception ignored) {}
        }

        bindStatusAndActions(holder, assignment);

        holder.optionsBtn.setOnClickListener(v -> handleOptionBtnClick(holder.getAbsoluteAdapterPosition(), v));

        holder.btnSubmitToggle.setOnClickListener(v -> {
            boolean newStatus = !assignment.isSubmitted();
            DbHelper.setAssignmentSubmitted(assignment.getAid(), newStatus);
            ToastUtil.showToastShort(newStatus ? "Marked as submitted!" : "Marked as pending!");
        });
    }

    private int getSubjectColor(String subject, int pos) {
        int[] palette = {
            Color.parseColor("#F5A623"), // Accent warm
            Color.parseColor("#4A90E2"), // Blue
            Color.parseColor("#2ECC71"), // Green
            Color.parseColor("#9B59B6"), // Purple
            Color.parseColor("#E74C3C"), // Red
            Color.parseColor("#1ABC9C")  // Teal
        };
        int hash = Math.abs(subject.hashCode());
        return palette[hash % palette.length];
    }

    private String getReminderText(AssignmentSchema assignment) {
        String schedule = assignment.getReminderSchedule();
        if (schedule == null || schedule.isEmpty() || schedule.equalsIgnoreCase("none")) {
            return "No reminder";
        }
        if (schedule.equalsIgnoreCase("on_due_day") || schedule.equalsIgnoreCase("same_day")) {
            return "Alert: on due day";
        }
        if (schedule.equalsIgnoreCase("1_day_before") || schedule.equalsIgnoreCase("one_day_before")) {
            return "Alert: 1 day before";
        }
        if (schedule.equalsIgnoreCase("2_days_before") || schedule.equalsIgnoreCase("two_days_before")) {
            return "Alert: 2 days before";
        }
        return "Alert set";
    }

    private String generateSubHeading(AssignmentSchema a) {
        long now = System.currentTimeMillis();
        long diff = a.getDueDate() - now;
        long days = diff / (1000 * 60 * 60 * 24);

        if (a.isSubmitted()) {
            return "Completed";
        } else if (diff < 0) {
            long overdueDays = Math.abs(days);
            return overdueDays == 0 ? "Overdue today" : "Overdue by " + overdueDays + (overdueDays == 1 ? " day" : " days");
        } else if (days == 0) {
            return "Due today";
        } else if (days == 1) {
            return "Due tomorrow";
        } else {
            return "Due in " + days + " days";
        }
    }

    private void bindStatusAndActions(TaskViewHolder holder, AssignmentSchema assignment) {
        long now = System.currentTimeMillis();
        boolean isOverdue = !assignment.isSubmitted() && assignment.getDueDate() < now;

        if (assignment.isSubmitted()) {
            holder.status.setText("Submitted");
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface2));

            // Completed CTA state
            holder.submitBtnText.setText("Done");
            holder.submitBtnText.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.btnSubmitToggle.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface2));
            holder.btnSubmitToggle.setStrokeColor(ContextCompat.getColor(context, R.color.success));
            holder.submitCheckIcon.setColorFilter(ContextCompat.getColor(context, R.color.success));
            holder.submitCheckIcon.setImageResource(R.drawable.ic_check);

            // Dim text for completed tasks
            holder.taskTitle.setAlpha(0.6f);
        } else if (isOverdue) {
            holder.status.setText("Overdue");
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.danger));
            holder.statusCard.setCardBackgroundColor(Color.parseColor("#26E74C3C"));

            // Pending CTA state
            holder.submitBtnText.setText("Mark done");
            holder.submitBtnText.setTextColor(ContextCompat.getColor(context, R.color.text_dim));
            holder.btnSubmitToggle.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
            holder.btnSubmitToggle.setStrokeColor(ContextCompat.getColor(context, R.color.border));
            holder.submitCheckIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_dim));
            holder.submitCheckIcon.setImageResource(R.drawable.ic_check);

            holder.taskTitle.setAlpha(1.0f);
        } else {
            holder.status.setText("Pending");
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.accent));
            holder.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent_dim));

            // Pending CTA state
            holder.submitBtnText.setText("Mark done");
            holder.submitBtnText.setTextColor(ContextCompat.getColor(context, R.color.text_dim));
            holder.btnSubmitToggle.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
            holder.btnSubmitToggle.setStrokeColor(ContextCompat.getColor(context, R.color.border));
            holder.submitCheckIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_dim));
            holder.submitCheckIcon.setImageResource(R.drawable.ic_check);

            holder.taskTitle.setAlpha(1.0f);
        }
    }

    private void handleOptionBtnClick(int position, View v) {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= assignmentSchemaList.size()) return;

        AssignmentSchema assignment = assignmentSchemaList.get(position);

        if (!(context instanceof androidx.fragment.app.FragmentActivity)) return;
        androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();

        java.util.List<com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem> options = new java.util.ArrayList<>();
        options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                "toggle",
                assignment.isSubmitted() ? "Mark as Pending" : "Mark as Completed",
                assignment.isSubmitted() ? "Reopen assignment" : "Finished and turned in",
                R.drawable.ic_check,
                false
        ));
        options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                "edit",
                "Edit Assignment",
                "Change title, date, or reminder",
                R.drawable.ic_edit,
                false
        ));
        options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                "delete",
                "Delete Assignment",
                "Remove this task permanently",
                R.drawable.ic_delete,
                true
        ));

        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionsModalBottomSheet.show(
                fm,
                assignment.getAssignmentTitle(),
                assignment.getSubjectName() != null ? assignment.getSubjectName() : "Assignment",
                options,
                optionId -> {
                    if ("toggle".equals(optionId)) {
                        boolean newStatus = !assignment.isSubmitted();
                        DbHelper.setAssignmentSubmitted(assignment.getAid(), newStatus);
                        ToastUtil.showToastShort(newStatus ? "Marked as submitted!" : "Marked as pending!");
                    } else if ("edit".equals(optionId)) {
                        com.redcodersgroup.attend_collegeattendancetracker.screens.addAssignment.AddAssignmentDialogFragment.showDialog(
                                fm,
                                assignment.getAid()
                        );
                    } else if ("delete".equals(optionId)) {
                        showDeleteTaskConfirmation(assignment, fm);
                    }
                }
        );
    }

    private void showDeleteTaskConfirmation(AssignmentSchema assignment, androidx.fragment.app.FragmentManager fm) {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                fm,
                "Delete Assignment?",
                "Are you sure you want to delete \"" + assignment.getAssignmentTitle() + "\"? This action cannot be undone.",
                () -> {
                    DbHelper.removeAssignment(assignment.getAid());
                    ToastUtil.showToastShort("Assignment deleted");
                }
        );
    }

    @Override
    public int getItemCount() {
        return assignmentSchemaList != null ? assignmentSchemaList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView taskTitle, subHeading, day, month, status, submitBtnText;
        AppCompatTextView subjectNameTag, taskDueDate, taskReminder;
        AppCompatImageView optionsBtn, submitCheckIcon;
        MaterialCardView statusCard, btnSubmitToggle, subjectTagCard;
        View taskAccentBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            subHeading = itemView.findViewById(R.id.subHeading);
            day = itemView.findViewById(R.id.day);
            month = itemView.findViewById(R.id.month);
            optionsBtn = itemView.findViewById(R.id.options_btn);
            status = itemView.findViewById(R.id.statusText);
            statusCard = itemView.findViewById(R.id.statusCard);
            btnSubmitToggle = itemView.findViewById(R.id.btnSubmitToggle);
            submitCheckIcon = itemView.findViewById(R.id.submitCheckIcon);
            submitBtnText = itemView.findViewById(R.id.submitBtnText);

            subjectNameTag = itemView.findViewById(R.id.subjectNameTag);
            taskDueDate = itemView.findViewById(R.id.taskDueDate);
            taskReminder = itemView.findViewById(R.id.taskReminder);
            subjectTagCard = itemView.findViewById(R.id.subjectTagCard);
            taskAccentBar = itemView.findViewById(R.id.taskAccentBar);
        }
    }
}
