package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityTimeTableBinding;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.BottomSheetAssignPeriodBinding;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogPeriodDurationBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter.AssignSubjectAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter.TimeTablePeriodAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.adapter.TimeTableSubjectSummaryAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model.TimeTablePeriodModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model.TimeTableSubjectSummaryModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimeTableActivity extends AppCompatActivity {
    private ActivityTimeTableBinding binding;
    private TimeTableViewModel viewModel;

    private TimeTablePeriodAdapter periodAdapter;
    private TimeTableSubjectSummaryAdapter subjectSummaryAdapter;

    private final List<TimeTableDaySchema> daysList = new ArrayList<>();
    private final List<DayScheduleSchema> schedulesList = new ArrayList<>();
    private final List<SubjectSchema> subjectsList = new ArrayList<>();
    private final Map<Integer, SubjectSchema> subjectMap = new HashMap<>();
    private UserSchema currentUser;

    private String selectedDayName = "Monday";
    private int periodDurationMinutes = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityTimeTableBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(TimeTableViewModel.class);

        initViews();
        setEventListeners();
        observeViewModel();
        observeTheme();
    }

    private void initViews() {
        periodDurationMinutes = SharedPrefUtil.getPeriodLength();
        binding.periodDurationText.setText(String.format(Locale.getDefault(), "%d mins", periodDurationMinutes));

        periodAdapter = new TimeTablePeriodAdapter(null);
        binding.rvPeriods.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPeriods.setAdapter(periodAdapter);

        subjectSummaryAdapter = new TimeTableSubjectSummaryAdapter();
        binding.rvSubjectSummary.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSubjectSummary.setAdapter(subjectSummaryAdapter);

        // Determine default day to select based on current day of week
        selectedDayName = getTodayDayName();
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());

        binding.dayChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipMonday) selectedDayName = "Monday";
            else if (checkedId == R.id.chipTuesday) selectedDayName = "Tuesday";
            else if (checkedId == R.id.chipWednesday) selectedDayName = "Wednesday";
            else if (checkedId == R.id.chipThursday) selectedDayName = "Thursday";
            else if (checkedId == R.id.chipFriday) selectedDayName = "Friday";
            else if (checkedId == R.id.chipSaturday) selectedDayName = "Saturday";

            renderSelectedDaySchedule();
        });
    }

    private void updatePeriodDuration(int newDurationMinutes) {
        periodDurationMinutes = newDurationMinutes;
        SharedPrefUtil.setPeriodLength(newDurationMinutes);
        binding.periodDurationText.setText(String.format(Locale.getDefault(), "%d mins", periodDurationMinutes));
        renderSelectedDaySchedule();
    }

    private void showPeriodDurationPickerDialog() {
        Dialog dialog = new Dialog(this);
        DialogPeriodDurationBinding dialogBinding = DialogPeriodDurationBinding.inflate(getLayoutInflater());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogBinding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Pre-select matching preset chip if exists
        switch (periodDurationMinutes) {
            case 30: dialogBinding.durationPresetChipGroup.check(R.id.chip30min); break;
            case 40: dialogBinding.durationPresetChipGroup.check(R.id.chip40min); break;
            case 45: dialogBinding.durationPresetChipGroup.check(R.id.chip45min); break;
            case 50: dialogBinding.durationPresetChipGroup.check(R.id.chip50min); break;
            case 55: dialogBinding.durationPresetChipGroup.check(R.id.chip55min); break;
            case 60: dialogBinding.durationPresetChipGroup.check(R.id.chip60min); break;
            case 75: dialogBinding.durationPresetChipGroup.check(R.id.chip75min); break;
            case 90: dialogBinding.durationPresetChipGroup.check(R.id.chip90min); break;
        }

        dialogBinding.customDurationInput.setText(String.valueOf(periodDurationMinutes));

        dialogBinding.durationPresetChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            int selectedDuration = 50;
            if (checkedId == R.id.chip30min) selectedDuration = 30;
            else if (checkedId == R.id.chip40min) selectedDuration = 40;
            else if (checkedId == R.id.chip45min) selectedDuration = 45;
            else if (checkedId == R.id.chip50min) selectedDuration = 50;
            else if (checkedId == R.id.chip55min) selectedDuration = 55;
            else if (checkedId == R.id.chip60min) selectedDuration = 60;
            else if (checkedId == R.id.chip75min) selectedDuration = 75;
            else if (checkedId == R.id.chip90min) selectedDuration = 90;

            dialogBinding.customDurationInput.setText(String.valueOf(selectedDuration));
        });

        dialogBinding.cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.saveDurationBtn.setOnClickListener(v -> {
            String inputStr = dialogBinding.customDurationInput.getText() != null
                    ? dialogBinding.customDurationInput.getText().toString().trim()
                    : "";

            if (inputStr.isEmpty()) {
                ToastUtil.showToastShort("Please enter a period length in minutes");
                return;
            }

            try {
                int duration = Integer.parseInt(inputStr);
                if (duration < 15 || duration > 240) {
                    ToastUtil.showToastShort("Period length must be between 15 and 240 minutes");
                    return;
                }
                updatePeriodDuration(duration);
                dialog.dismiss();
                ToastUtil.showToastShort(String.format(Locale.getDefault(), "Period length set to %d mins", duration));
            } catch (NumberFormatException e) {
                ToastUtil.showToastShort("Please enter a valid number");
            }
        });

        int accentColor = ThemeManager.getAccentColorInt();
        dialogBinding.saveDurationBtn.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        dialogBinding.customDurationLayout.setBoxStrokeColor(accentColor);
        dialogBinding.customDurationLayout.setHintTextColor(ColorStateList.valueOf(accentColor));

        dialog.show();
    }

    private void observeTheme() {
        ThemeManager.getAccentColorLiveData().observe(this, colorHex -> {
            if (colorHex == null || binding == null) return;
            try {
                int color = Color.parseColor(colorHex);
                binding.totalWeeklyClassesText.setTextColor(color);
                binding.periodDurationText.setTextColor(color);
                binding.editSlotsBtn.setTextColor(color);
                periodAdapter.notifyDataSetChanged();
                subjectSummaryAdapter.notifyDataSetChanged();
            } catch (Exception ignored) {}
        });

        // Apply immediately
        int color = ThemeManager.getAccentColorInt();
        binding.totalWeeklyClassesText.setTextColor(color);
        binding.periodDurationText.setTextColor(color);
        binding.editSlotsBtn.setTextColor(color);
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user == null) return;
            currentUser = user;

            // Configure working days & Saturday chip visibility
            boolean is6Days = (user.getWorkingDays() == 6);
            binding.chipSaturday.setVisibility(is6Days ? View.VISIBLE : View.GONE);
            binding.workingDaysText.setText(is6Days ? "6 days" : "5 days");
            binding.subtitleText.setText("Weekly Schedule Grid (" + (is6Days ? "6-Day" : "5-Day") + " Working Week)");

            // First class time
            long startMs = user.getFirstPeriodStartTime();
            if (startMs > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(startMs);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int min = cal.get(Calendar.MINUTE);
                String amPm = hour >= 12 ? "PM" : "AM";
                int displayHour = hour % 12;
                if (displayHour == 0) displayHour = 12;
                binding.firstClassTimeText.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, min, amPm));
            } else {
                binding.firstClassTimeText.setText("09:00 AM");
            }

            selectDayChip(selectedDayName);
            renderSelectedDaySchedule();
            renderSubjectSummary();
        });

        viewModel.getDaysLiveData().observe(this, days -> {
            daysList.clear();
            if (days != null) {
                daysList.addAll(days);
            }
            renderSelectedDaySchedule();
            renderSubjectSummary();
        });

        viewModel.getSchedulesLiveData().observe(this, schedules -> {
            schedulesList.clear();
            if (schedules != null) {
                schedulesList.addAll(schedules);
            }
            renderSelectedDaySchedule();
            renderSubjectSummary();
        });

        viewModel.getSubjectsLiveData().observe(this, subjects -> {
            subjectsList.clear();
            subjectMap.clear();
            if (subjects != null) {
                subjectsList.addAll(subjects);
                for (SubjectSchema s : subjects) {
                    subjectMap.put(s.getSid(), s);
                }
            }
            renderSelectedDaySchedule();
            renderSubjectSummary();
        });
    }

    private void selectDayChip(String dayName) {
        if ("Monday".equalsIgnoreCase(dayName)) binding.dayChipGroup.check(R.id.chipMonday);
        else if ("Tuesday".equalsIgnoreCase(dayName)) binding.dayChipGroup.check(R.id.chipTuesday);
        else if ("Wednesday".equalsIgnoreCase(dayName)) binding.dayChipGroup.check(R.id.chipWednesday);
        else if ("Thursday".equalsIgnoreCase(dayName)) binding.dayChipGroup.check(R.id.chipThursday);
        else if ("Friday".equalsIgnoreCase(dayName)) binding.dayChipGroup.check(R.id.chipFriday);
        else if ("Saturday".equalsIgnoreCase(dayName)) {
            if (currentUser != null && currentUser.getWorkingDays() == 6) {
                binding.dayChipGroup.check(R.id.chipSaturday);
            } else {
                selectedDayName = "Monday";
                binding.dayChipGroup.check(R.id.chipMonday);
            }
        }
    }

    private String getTodayDayName() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            default: return "Monday";
        }
    }

    private void renderSelectedDaySchedule() {
        TimeTableDaySchema matchingDay = null;
        for (TimeTableDaySchema day : daysList) {
            if (day.getDayName() != null && day.getDayName().equalsIgnoreCase(selectedDayName)) {
                matchingDay = day;
                break;
            }
        }

        if (matchingDay == null) {
            binding.selectedDayTitle.setText(selectedDayName + " Schedule");
            binding.selectedDayClassCount.setText("0 classes");
            periodAdapter.setPeriods(new ArrayList<>());
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            return;
        }

        binding.emptyStateLayout.setVisibility(View.GONE);

        // Find matching DayScheduleSchema
        DayScheduleSchema matchingSchedule = null;
        for (DayScheduleSchema s : schedulesList) {
            if (s.getDid() == matchingDay.getDid()) {
                matchingSchedule = s;
                break;
            }
        }

        long startMs = currentUser != null ? currentUser.getFirstPeriodStartTime() : 0;
        List<TimeTablePeriodModel> periods = new ArrayList<>();
        int assignedCount = 0;

        for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
            String timeRange = TimeTableViewModel.getPeriodTimeRange(startMs, p - 1, periodDurationMinutes);
            Integer sid = TimeTableViewModel.getPeriodValue(matchingSchedule, p);
            SubjectSchema subject = (sid != null) ? subjectMap.get(sid) : null;

            if (sid != null && subject != null) {
                assignedCount++;
            }

            periods.add(new TimeTablePeriodModel(
                    p,
                    matchingDay.getDid(),
                    matchingDay.getDayName(),
                    timeRange,
                    sid,
                    subject
            ));
        }

        binding.selectedDayTitle.setText(matchingDay.getDayName() + " Schedule");
        binding.selectedDayClassCount.setText(assignedCount + (assignedCount == 1 ? " class" : " classes"));
        periodAdapter.setPeriods(periods);
    }

    private void renderSubjectSummary() {
        if (subjectsList.isEmpty()) {
            binding.emptySubjectsSummaryText.setVisibility(View.VISIBLE);
            binding.rvSubjectSummary.setVisibility(View.GONE);
            binding.totalWeeklyClassesText.setText("Total: 0 slots / wk");
            subjectSummaryAdapter.setItems(new ArrayList<>());
            return;
        }

        binding.emptySubjectsSummaryText.setVisibility(View.GONE);
        binding.rvSubjectSummary.setVisibility(View.VISIBLE);

        // Count slot assignments per subject
        Map<Integer, Integer> slotCountMap = new HashMap<>();
        int totalWeeklySlots = 0;

        for (DayScheduleSchema s : schedulesList) {
            for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
                Integer sid = TimeTableViewModel.getPeriodValue(s, p);
                if (sid != null && subjectMap.containsKey(sid)) {
                    slotCountMap.put(sid, slotCountMap.getOrDefault(sid, 0) + 1);
                    totalWeeklySlots++;
                }
            }
        }

        List<TimeTableSubjectSummaryModel> summaryList = new ArrayList<>();
        for (SubjectSchema subject : subjectsList) {
            int scheduled = slotCountMap.getOrDefault(subject.getSid(), 0);
            summaryList.add(new TimeTableSubjectSummaryModel(subject, scheduled));
        }

        binding.totalWeeklyClassesText.setText("Total: " + totalWeeklySlots + " slots / wk");
        subjectSummaryAdapter.setItems(summaryList);
    }

    private void onPeriodClicked(TimeTablePeriodModel periodModel) {
        showAssignSubjectBottomSheet(periodModel);
    }

    private void showAssignSubjectBottomSheet(TimeTablePeriodModel periodModel) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetAssignPeriodBinding sheetBinding = BottomSheetAssignPeriodBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.dialogTitle.setText(String.format("Assign Period %d • %s", periodModel.getPeriodNumber(), periodModel.getDayName()));
        sheetBinding.dialogSubTitle.setText(periodModel.getTimeRange());

        // Handle close button
        sheetBinding.closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Handle clear / free period option
        sheetBinding.clearPeriodCard.setOnClickListener(v -> {
            viewModel.updatePeriodSubjectWithValidation(periodModel.getDid(), periodModel.getPeriodNumber(), null, new TimeTableViewModel.PeriodAssignmentCallback() {
                @Override
                public void onSuccess() {
                    ToastUtil.showToastShort(String.format("Period %d marked as Free", periodModel.getPeriodNumber()));
                }

                @Override
                public void onLimitExceeded(int maxAllowed, int currentCount, boolean isLab) {
                    // Not applicable when clearing
                }
            });
            dialog.dismiss();
        });

        // Compute current booked slot counts per subject
        Map<Integer, Integer> slotCountMap = new HashMap<>();
        for (DayScheduleSchema s : schedulesList) {
            for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
                Integer sid = TimeTableViewModel.getPeriodValue(s, p);
                if (sid != null) {
                    slotCountMap.put(sid, slotCountMap.getOrDefault(sid, 0) + 1);
                }
            }
        }

        // Setup subjects recyclerview
        AssignSubjectAdapter assignAdapter = new AssignSubjectAdapter(subjectsList, periodModel.getSid(), slotCountMap, subject -> {
            viewModel.updatePeriodSubjectWithValidation(periodModel.getDid(), periodModel.getPeriodNumber(), subject.getSid(), new TimeTableViewModel.PeriodAssignmentCallback() {
                @Override
                public void onSuccess() {
                    ToastUtil.showToastShort(String.format("Assigned %s to Period %d", subject.getSubjectName(), periodModel.getPeriodNumber()));
                }

                @Override
                public void onLimitExceeded(int maxAllowed, int currentCount, boolean isLab) {
                    String type = isLab ? "labs" : "classes";
                    ToastUtil.showToastShort(String.format("%s reached weekly limit of %d %s", subject.getSubjectName(), maxAllowed, type));
                }
            });
            dialog.dismiss();
        });

        sheetBinding.rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        sheetBinding.rvSubjects.setAdapter(assignAdapter);

        dialog.show();
    }
}