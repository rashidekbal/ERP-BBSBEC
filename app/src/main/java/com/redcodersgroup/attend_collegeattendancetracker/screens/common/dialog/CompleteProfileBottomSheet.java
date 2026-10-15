package com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.BottomSheetCompleteProfileBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CompleteProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CompleteProfileBottomSheet";

    public interface OnProfileCompletedListener {
        void onProfileCompleted();
    }

    private BottomSheetCompleteProfileBinding binding;
    private OnProfileCompletedListener listener;

    private UserSchema existingUser;
    private long selectedStartDateMs = 0;
    private long selectedEndDateMs = 0;
    private long selectedStartTimeMs = 0;

    public static void show(FragmentManager fragmentManager, OnProfileCompletedListener listener) {
        if (fragmentManager == null || fragmentManager.isStateSaved()) return;
        CompleteProfileBottomSheet sheet = new CompleteProfileBottomSheet();
        sheet.setOnProfileCompletedListener(listener);
        sheet.show(fragmentManager, TAG);
    }

    public static void showIfNeeded(FragmentManager fragmentManager, OnProfileCompletedListener listener) {
        if (ProfileUtil.isProfileCompletedBareMinimum()) return;
        show(fragmentManager, listener);
    }

    public void setOnProfileCompletedListener(OnProfileCompletedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCompleteProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initDefaultTimes();
        applyAccentColor();
        loadExistingUserData();
        setupEventListeners();
    }

    private void initDefaultTimes() {
        // Default first class start time to 09:00 AM
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartTimeMs = cal.getTimeInMillis();
    }

    private void applyAccentColor() {
        String accentHex = ThemeManager.getAccentColor();
        if (accentHex == null) return;
        try {
            int accent = Color.parseColor(accentHex);
            ColorStateList csl = ColorStateList.valueOf(accent);
            binding.stepProgressBar.setIndicatorColor(accent);
            binding.btnNextStep.setBackgroundTintList(csl);
            binding.btnFinishSetup.setBackgroundTintList(csl);
            binding.stepNumberBadge.setTextColor(accent);
            binding.stepPercentText.setTextColor(accent);
        } catch (Exception ignored) {}
    }

    private void loadExistingUserData() {
        DbHelper.execute(() -> {
            existingUser = Database.getInstance().getUserDao().getUser();
            if (existingUser != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (existingUser.getName() != null && !existingUser.getName().isEmpty()) {
                        binding.nameInput.setText(existingUser.getName());
                    }

                    // Semester chip
                    int sem = existingUser.getSemester();
                    selectSemesterChip(sem > 0 ? sem : 1);

                    // Dates
                    if (existingUser.getSemStartDate() > 0) {
                        selectedStartDateMs = existingUser.getSemStartDate();
                        binding.startDateText.setText(formatDate(selectedStartDateMs));
                    }
                    if (existingUser.getSemEndDate() > 0) {
                        selectedEndDateMs = existingUser.getSemEndDate();
                        binding.endDateText.setText(formatDate(selectedEndDateMs));
                    }

                    // Working Days
                    if (existingUser.getWorkingDays() == 6) {
                        binding.workingDaysChipGroup.check(R.id.chip6Days);
                    } else {
                        binding.workingDaysChipGroup.check(R.id.chip5Days);
                    }

                    // Period start time
                    if (existingUser.getFirstPeriodStartTime() > 0) {
                        selectedStartTimeMs = existingUser.getFirstPeriodStartTime();
                        binding.periodTimeText.setText(formatTime(selectedStartTimeMs));
                    }

                    // Criteria
                    int criteria = existingUser.getAttendanceCriteria();
                    if (criteria == 65) {
                        binding.criteriaChipGroup.check(R.id.criteria65);
                    } else if (criteria == 80) {
                        binding.criteriaChipGroup.check(R.id.criteria80);
                    } else {
                        binding.criteriaChipGroup.check(R.id.criteria75);
                    }
                });
            }
        });
    }

    private void setupEventListeners() {
        binding.closeBtn.setOnClickListener(v -> dismiss());

        // Step 1: Dates
        binding.startDateBox.setOnClickListener(v -> showDatePicker(true));
        binding.endDateBox.setOnClickListener(v -> showDatePicker(false));

        // Step 1 -> Step 2
        binding.btnNextStep.setOnClickListener(v -> validateAndProceedToStep2());

        // Step 2: Time
        binding.periodTimeBox.setOnClickListener(v -> showTimePicker());

        // Step 2 -> Step 1
        binding.btnBackStep.setOnClickListener(v -> goToStep1());

        // Step 2 -> Finish
        binding.btnFinishSetup.setOnClickListener(v -> validateAndFinishSetup());
    }

    private void showDatePicker(boolean isStartDate) {
        if (getContext() == null) return;
        Calendar cal = Calendar.getInstance();
        long initialMs = isStartDate ? selectedStartDateMs : selectedEndDateMs;
        if (initialMs > 0) {
            cal.setTimeInMillis(initialMs);
        }
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, y);
            selected.set(Calendar.MONTH, m);
            selected.set(Calendar.DAY_OF_MONTH, d);
            selected.set(Calendar.HOUR_OF_DAY, 0);
            selected.set(Calendar.MINUTE, 0);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            long ms = selected.getTimeInMillis();
            if (isStartDate) {
                selectedStartDateMs = ms;
                binding.startDateText.setText(formatDate(ms));
            } else {
                selectedEndDateMs = ms;
                binding.endDateText.setText(formatDate(ms));
            }
        }, year, month, day);

        picker.show();
    }

    private void showTimePicker() {
        if (getContext() == null) return;
        Calendar cal = Calendar.getInstance();
        if (selectedStartTimeMs > 0) {
            cal.setTimeInMillis(selectedStartTimeMs);
        }
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog picker = new TimePickerDialog(requireContext(), (view, h, m) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, h);
            selected.set(Calendar.MINUTE, m);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            selectedStartTimeMs = selected.getTimeInMillis();
            binding.periodTimeText.setText(formatTime(selectedStartTimeMs));
        }, hour, minute, false);

        picker.show();
    }

    private void validateAndProceedToStep2() {
        String name = binding.nameInput.getText() != null ? binding.nameInput.getText().toString().trim() : "";
        if (name.isEmpty()) {
            binding.nameInput.setError("Please enter your name");
            binding.nameInput.requestFocus();
            ToastUtil.showToastShort("Please enter your name");
            return;
        }

        if (selectedStartDateMs <= 0) {
            ToastUtil.showToastShort("Please select semester start date");
            return;
        }

        if (selectedEndDateMs <= 0) {
            ToastUtil.showToastShort("Please select semester end date");
            return;
        }

        if (selectedEndDateMs <= selectedStartDateMs) {
            ToastUtil.showToastShort("End date must be after start date");
            return;
        }

        // Transition to Step 2
        binding.step1Layout.setVisibility(View.GONE);
        binding.step2Layout.setVisibility(View.VISIBLE);
        binding.stepNumberBadge.setText("STEP 2 OF 2: SCHEDULE & TIMINGS");
        binding.stepPercentText.setText("100%");
        binding.stepProgressBar.setProgress(100);
    }

    private void goToStep1() {
        binding.step2Layout.setVisibility(View.GONE);
        binding.step1Layout.setVisibility(View.VISIBLE);
        binding.stepNumberBadge.setText("STEP 1 OF 2: SEMESTER INFO");
        binding.stepPercentText.setText("50%");
        binding.stepProgressBar.setProgress(50);
    }

    private void validateAndFinishSetup() {
        int workingDays = getSelectedWorkingDays();
        if (workingDays != 5 && workingDays != 6) {
            ToastUtil.showToastShort("Please select working days");
            return;
        }

        if (selectedStartTimeMs <= 0) {
            ToastUtil.showToastShort("Please select first period start time");
            return;
        }

        int criteria = getSelectedCriteria();
        String name = binding.nameInput.getText() != null ? binding.nameInput.getText().toString().trim() : "Student";
        int semester = getSelectedSemester();

        binding.btnFinishSetup.setEnabled(false);
        binding.btnBackStep.setEnabled(false);

        DbHelper.execute(() -> {
            try {
                UserSchema user = Database.getInstance().getUserDao().getUser();
                String themeColor = ThemeManager.getAccentColor();
                if (themeColor == null || themeColor.isEmpty()) themeColor = "#F5A623";

                if (user == null) {
                    user = new UserSchema(
                            name,
                            null,
                            null,
                            null,
                            semester,
                            selectedStartDateMs,
                            selectedEndDateMs,
                            workingDays,
                            selectedStartTimeMs,
                            criteria,
                            themeColor,
                            24
                    );
                    Database.getInstance().getUserDao().addUser(user);
                } else {
                    user.setName(name);
                    user.setSemester(semester);
                    user.setSemStartDate(selectedStartDateMs);
                    user.setSemEndDate(selectedEndDateMs);
                    user.setWorkingDays(workingDays);
                    user.setFirstPeriodStartTime(selectedStartTimeMs);
                    user.setAttendanceCriteria(criteria);
                    if (user.getThemeColor() == null || user.getThemeColor().isEmpty()) {
                        user.setThemeColor(themeColor);
                    }
                    Database.getInstance().getUserDao().updateUser(user);
                }

                // Setup timetable days (5 or 6 days)
                ProfileUtil.setupTimetableDays(workingDays);

                // Mark profile complete
                SharedPrefUtil.setProfileComplete(true);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ToastUtil.showToastShort("Profile setup completed! You can now add subjects.");
                        dismissAllowingStateLoss();
                        if (listener != null) {
                            listener.onProfileCompleted();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ToastUtil.showToastShort("Error saving profile: " + e.getMessage());
                        binding.btnFinishSetup.setEnabled(true);
                        binding.btnBackStep.setEnabled(true);
                    });
                }
            }
        });
    }

    private int getSelectedSemester() {
        int checkedId = binding.semesterChipGroup.getCheckedChipId();
        if (checkedId == R.id.sem1) return 1;
        if (checkedId == R.id.sem2) return 2;
        if (checkedId == R.id.sem3) return 3;
        if (checkedId == R.id.sem4) return 4;
        if (checkedId == R.id.sem5) return 5;
        if (checkedId == R.id.sem6) return 6;
        if (checkedId == R.id.sem7) return 7;
        if (checkedId == R.id.sem8) return 8;
        return 1;
    }

    private void selectSemesterChip(int semester) {
        if (semester == 1) binding.semesterChipGroup.check(R.id.sem1);
        else if (semester == 2) binding.semesterChipGroup.check(R.id.sem2);
        else if (semester == 3) binding.semesterChipGroup.check(R.id.sem3);
        else if (semester == 4) binding.semesterChipGroup.check(R.id.sem4);
        else if (semester == 5) binding.semesterChipGroup.check(R.id.sem5);
        else if (semester == 6) binding.semesterChipGroup.check(R.id.sem6);
        else if (semester == 7) binding.semesterChipGroup.check(R.id.sem7);
        else if (semester == 8) binding.semesterChipGroup.check(R.id.sem8);
    }

    private int getSelectedWorkingDays() {
        int checkedId = binding.workingDaysChipGroup.getCheckedChipId();
        if (checkedId == R.id.chip6Days) return 6;
        return 5;
    }

    private int getSelectedCriteria() {
        int checkedId = binding.criteriaChipGroup.getCheckedChipId();
        if (checkedId == R.id.criteria65) return 65;
        if (checkedId == R.id.criteria80) return 80;
        return 75;
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(ms));
    }

    private String formatTime(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, min, amPm);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
