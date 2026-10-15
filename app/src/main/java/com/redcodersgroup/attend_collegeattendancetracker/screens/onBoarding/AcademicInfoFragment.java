package com.redcodersgroup.attend_collegeattendancetracker.screens.onBoarding;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentAcademicInfoBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AcademicInfoFragment extends Fragment {
    private FragmentAcademicInfoBinding binding;
    private long selectedStartTimeMs;
    private boolean isUpdatingMaxMarks = false;

    public AcademicInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAcademicInfoBinding.inflate(inflater, container, false);
        init();
        setupMaxMarksListeners();
        setEventListeners();
        return binding.getRoot();
    }

    private void init() {
        selectedStartTimeMs = 0;
    }

    private void setupMaxMarksListeners() {
        binding.maxMarksChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isUpdatingMaxMarks) return;
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            int marks = 24;
            if (checkedId == R.id.m_20) marks = 20;
            else if (checkedId == R.id.m_24) marks = 24;
            else if (checkedId == R.id.m_25) marks = 25;
            else if (checkedId == R.id.m_30) marks = 30;
            else if (checkedId == R.id.m_50) marks = 50;

            isUpdatingMaxMarks = true;
            binding.maxMarksInput.setText(String.valueOf(marks));
            isUpdatingMaxMarks = false;
        });

        binding.maxMarksInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingMaxMarks) return;
                String text = s != null ? s.toString().trim() : "";
                isUpdatingMaxMarks = true;
                if ("20".equals(text)) {
                    binding.maxMarksChipGroup.check(R.id.m_20);
                } else if ("24".equals(text)) {
                    binding.maxMarksChipGroup.check(R.id.m_24);
                } else if ("25".equals(text)) {
                    binding.maxMarksChipGroup.check(R.id.m_25);
                } else if ("30".equals(text)) {
                    binding.maxMarksChipGroup.check(R.id.m_30);
                } else if ("50".equals(text)) {
                    binding.maxMarksChipGroup.check(R.id.m_50);
                } else {
                    binding.maxMarksChipGroup.clearCheck();
                }
                isUpdatingMaxMarks = false;
            }
        });
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.periodTime.setOnClickListener(this::handlePeriodTimeClick);
        binding.finishBtn.setOnClickListener(this::handleFinishBtnClick);
    }

    private void handlePeriodTimeClick(View view) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireActivity(), (view1, selectedHour, selectedMinute) -> {
            handleTimeSelected(selectedHour, selectedMinute);
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void handleTimeSelected(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        selectedStartTimeMs = calendar.getTimeInMillis();
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        String formattedTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm);
        binding.periodTimeTextView.setText(formattedTime);
    }

    private void handleFinishBtnClick(View view) {
        binding.finishBtn.setEnabled(false);

        int workingDays = getWorkingDays();
        int attendanceCriteria = getAttendanceCriteria();
        int maxMarks = getMaxMarks();

        if (!isFormValid(workingDays, attendanceCriteria, maxMarks)) {
            binding.finishBtn.setEnabled(true);
            return;
        }

        DbHelper.execute(() -> {
            UserSchema user = Database.getInstance().getUserDao().getUser();
            if (user == null) {
                requireActivity().runOnUiThread(() -> {
                    ToastUtil.showToastShort("user not found, please go back and fill info again");
                    binding.finishBtn.setEnabled(true);
                });
                return;
            }
            Database.getInstance().getUserDao().updateUserAcademicInfo(user.getUid(), workingDays, selectedStartTimeMs, attendanceCriteria, maxMarks);
            List<String> days = getDaysList(workingDays);
            List<TimeTableDaySchema> timeTableDaySchema = new ArrayList<>();
            for (String day : days) {
                timeTableDaySchema.add(new TimeTableDaySchema(day));
            }
            Database.getInstance().getTimeTableDayDao().insertMulti(timeTableDaySchema);
            List<TimeTableDaySchema> s = Database.getInstance().getTimeTableDayDao().getDays();
            List<DayScheduleSchema> dayScheduleSchemas = new ArrayList<>();
            for (TimeTableDaySchema day : s) {
                dayScheduleSchemas.add(new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null));
            }
            Database.getInstance().getDayScheduleDao().insertMulti(dayScheduleSchemas);
            requireActivity().runOnUiThread(() -> {
                SharedPrefUtil.setSetupComplete(true);
                startActivity(new Intent(requireActivity(), HomeActivity.class));
                requireActivity().finish();
            });
        });
    }

    private List<String> getDaysList(int workingDays) {
        if (workingDays == 5) {
            return List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        } else {
            return List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
        }
    }

    private int getWorkingDays() {
        int checkedId = binding.workingDayChipGroup.getCheckedChipId();
        if (checkedId == R.id.five_days) return 5;
        if (checkedId == R.id.sixDays) return 6;
        return 0;
    }

    private int getAttendanceCriteria() {
        int checkedId = binding.attendanceCriteriaChipGroup.getCheckedChipId();
        if (checkedId == R.id.a_65) return 65;
        if (checkedId == R.id.a_75) return 75;
        if (checkedId == R.id.a_80) return 80;
        return 0;
    }

    private int getMaxMarks() {
        String input = Objects.requireNonNull(binding.maxMarksInput.getText()).toString().trim();
        if (input.isEmpty()) return 24;
        try {
            int marks = Integer.parseInt(input);
            return marks > 0 ? marks : 24;
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    private boolean isFormValid(int workingDays, int attendanceCriteria, int maxMarks) {
        if (workingDays == 0) {
            binding.workingDayChipGroup.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("please select working days");
            return false;
        }
        if (selectedStartTimeMs == 0) {
            binding.periodTime.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("please select first period start time");
            return false;
        }
        if (attendanceCriteria == 0) {
            binding.attendanceCriteriaChipGroup.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("please select attendance criteria");
            return false;
        }
        if (maxMarks <= 0) {
            binding.maxMarksInput.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.maxMarksInput.setError("Please enter valid max marks");
            return false;
        }
        return true;
    }
}