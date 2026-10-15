package com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;

import java.util.List;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.ViewHolder> {

    private final Context context;
    private final List<HolidaySchema> holidayList;

    public HolidayAdapter(Context context, List<HolidaySchema> holidayList) {
        this.context = context;
        this.holidayList = holidayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.holiday_card_rv, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holidayList == null || position < 0 || position >= holidayList.size()) return;

        HolidaySchema holiday = holidayList.get(position);
        holder.holidayName.setText(holiday.getEventName());
        holder.optionBtn.setOnClickListener(v -> handleOptionClick(position, v));

        boolean isSingleDayHoliday = DateUtil.isSingleDay(holiday.getStartDate(), holiday.getEndDate());
        holder.holidayDate.setText(isSingleDayHoliday
                ? DateUtil.getDateString(holiday.getStartDate()) + " college closed"
                : DateUtil.getDateString(holiday.getStartDate()) + " - " + DateUtil.getDateString(holiday.getEndDate()) + " college closed");
    }

    private void handleOptionClick(int position, View anchor) {
        if (position < 0 || position >= holidayList.size()) return;
        HolidaySchema holiday = holidayList.get(position);

        if (!(context instanceof androidx.fragment.app.FragmentActivity)) return;
        androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();

        java.util.List<com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem> options = new java.util.ArrayList<>();
        options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                "edit", "Edit Holiday", "Modify name or dates", R.drawable.ic_edit, false
        ));
        options.add(new com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem(
                "delete", "Delete Holiday", "Remove permanently", R.drawable.ic_delete, true
        ));

        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionsModalBottomSheet.show(
                fm,
                holiday.getEventName(),
                "College Holiday",
                options,
                optionId -> {
                    if ("edit".equals(optionId)) {
                        com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.AddHolidayDialogFragment.showDialog(
                                fm,
                                holiday.getHid()
                        );
                    } else if ("delete".equals(optionId)) {
                        showDeleteHolidayConfirmation(holiday, fm);
                    }
                }
        );
    }

    private void showDeleteHolidayConfirmation(HolidaySchema holiday, androidx.fragment.app.FragmentManager fm) {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                fm,
                "Delete Holiday?",
                "Are you sure you want to delete \"" + holiday.getEventName() + "\"? This action cannot be undone.",
                () -> {
                    com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper.removeHoliday(holiday.getHid());
                    com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Holiday deleted");
                }
        );
    }

    @Override
    public int getItemCount() {
        return holidayList != null ? holidayList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView holidayName, holidayDate;
        AppCompatImageView optionBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            holidayName = itemView.findViewById(R.id.holiday_name);
            holidayDate = itemView.findViewById(R.id.holiday_date);
            optionBtn = itemView.findViewById(R.id.options_btn);
        }
    }
}
