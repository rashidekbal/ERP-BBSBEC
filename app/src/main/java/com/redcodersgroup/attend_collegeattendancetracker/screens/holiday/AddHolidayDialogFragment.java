package com.redcodersgroup.attend_collegeattendancetracker.screens.holiday;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.ScheduleConstants;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAddHolidayBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.Calendar;
import java.util.Objects;

public class AddHolidayDialogFragment extends DialogFragment {

    public static final String TAG = "AddHolidayDialogFragment";
    private static final String ARG_HOLIDAY_ID = "HOLIDAY_ID";

    public static AddHolidayDialogFragment newInstance() {
        return new AddHolidayDialogFragment();
    }

    public static AddHolidayDialogFragment newInstance(int holidayId) {
        AddHolidayDialogFragment fragment = new AddHolidayDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HOLIDAY_ID, holidayId);
        fragment.setArguments(args);
        return fragment;
    }

    public static void showDialog(FragmentManager fragmentManager) {
        AddHolidayDialogFragment fragment = newInstance();
        fragment.show(fragmentManager, TAG);
    }

    public static void showDialog(FragmentManager fragmentManager, int holidayId) {
        AddHolidayDialogFragment fragment = newInstance(holidayId);
        fragment.show(fragmentManager, TAG);
    }

    private DialogAddHolidayBinding binding;
    private String startDate;
    private String endDate;
    private int editHolidayId = -1;
    private HolidaySchema existingHoliday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAddHolidayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setEventListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void init() {
        applyAccentColor();
        if (getArguments() != null) {
            editHolidayId = getArguments().getInt(ARG_HOLIDAY_ID, -1);
        }
        if (editHolidayId != -1) {
            binding.headerTitle.setText("Edit Holiday");
            binding.saveBtn.setText("Update");
            loadExistingHoliday();
        }
    }

    private void loadExistingHoliday() {
        DbHelper.execute(() -> {
            existingHoliday = com.redcodersgroup.attend_collegeattendancetracker.db.Database.getInstance().getHolidayDao().getHolidayById(editHolidayId);
            if (existingHoliday != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.holidayNameInput.setText(existingHoliday.getEventName());
                    startDate = DateUtil.getDateString(existingHoliday.getStartDate());
                    endDate = DateUtil.getDateString(existingHoliday.getEndDate());
                    binding.startDateTextView.setText(startDate);
                    binding.endDateTextView.setText(endDate);

                    String rem = existingHoliday.getReminderSchedule();
                    if ("T_0".equalsIgnoreCase(rem) || "0".equals(rem)) {
                        binding.reminderChipGroup.check(R.id.t_0);
                    } else if ("T_1".equalsIgnoreCase(rem) || "1".equals(rem)) {
                        binding.reminderChipGroup.check(R.id.t_1);
                    } else {
                        binding.reminderChipGroup.check(R.id.t_2);
                    }
                });
            }
        });
    }

    private void applyAccentColor() {
        if (getContext() == null) return;
        int accent = ThemeManager.getAccentColorInt();
        binding.saveBtn.setBackgroundColor(accent);
        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);
        ColorStateList chipCsl = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, surfaceColor}
        );
        for (int i = 0; i < binding.reminderChipGroup.getChildCount(); i++) {
            View child = binding.reminderChipGroup.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                ((com.google.android.material.chip.Chip) child).setChipBackgroundColor(chipCsl);
            }
        }
    }

    private void setEventListeners() {
        binding.closeBtn.setOnClickListener(v -> dismiss());
        binding.cancelBtn.setOnClickListener(v -> dismiss());
        binding.startDate.setOnClickListener(this::handleStartDate);
        binding.endDate.setOnClickListener(this::handleEndDate);
        binding.saveBtn.setOnClickListener(this::handleSaveHoliday);
    }

    private void handleSaveHoliday(View view) {
        binding.saveBtn.setEnabled(false);
        String holidayName = Objects.requireNonNull(binding.holidayNameInput.getText()).toString().trim();
        int checkedChipId = binding.reminderChipGroup.getCheckedChipId();
        String reminderSchedule;
        if (checkedChipId == R.id.t_0) {
            reminderSchedule = ScheduleConstants.T_0.toString();
        } else if (checkedChipId == R.id.t_1) {
            reminderSchedule = ScheduleConstants.T_1.toString();
        } else {
            reminderSchedule = ScheduleConstants.T_2.toString();
        }
        if (!isFormValid(holidayName, startDate, endDate)) {
            binding.saveBtn.setEnabled(true);
            return;
        }
        if (editHolidayId != -1) {
            HolidaySchema updated = new HolidaySchema(editHolidayId, holidayName, DateUtil.getDateMs(startDate), DateUtil.getDateMs(endDate), reminderSchedule);
            DbHelper.updateHoliday(updated);
            ToastUtil.showToastShort("Holiday updated successfully");
        } else {
            DbHelper.addHoliday(new HolidaySchema(holidayName, DateUtil.getDateMs(startDate), DateUtil.getDateMs(endDate), reminderSchedule));
            ToastUtil.showToastShort("Holiday added successfully");
        }
        dismissAllowingStateLoss();
    }

    private boolean isFormValid(String holidayName, String startDate, String endDate) {
        if (holidayName.isEmpty()) {
            binding.holidayNameInput.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.holidayNameInput.setError("Enter holiday name");
            return false;
        }
        if (startDate == null || startDate.isEmpty()) {
            binding.startDate.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("Please select start date");
            return false;
        }
        if (endDate == null || endDate.isEmpty()) {
            binding.endDate.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("Please select end date");
            return false;
        }
        return true;
    }

    private void handleStartDate(View view) {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view1, selectedYear, selectedMonth, selectedDay) -> {
            startDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            binding.startDateTextView.setText(startDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void handleEndDate(View view) {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view1, selectedYear, selectedMonth, selectedDay) -> {
            endDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            binding.endDateTextView.setText(endDate);
        }, year, month, day);
        datePickerDialog.show();
    }
}
