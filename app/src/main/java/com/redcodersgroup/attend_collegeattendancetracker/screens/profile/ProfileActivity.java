package com.redcodersgroup.attend_collegeattendancetracker.screens.profile;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityProfileBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.profile.viewmodel.ProfileViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;

    private UserSchema currentUser;
    private String selectedThemeColor = "#F5A623";
    private long selectedStartTimeMs = 0;
    private long selectedStartDateMs = 0;
    private long selectedEndDateMs = 0;
    private int initialWorkingDays = 5;
    private boolean isInitialDataLoaded = false;
    private boolean isUpdatingMaxMarks = false;

    private final String[] SWATCH_COLORS = new String[]{
            "#F5A623",
            "#22C55E",
            "#F5654A",
            "#3B82F6",
            "#8B5CF6",
            "#EC4899",
            "#06B6D4",
            "#6366F1"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        selectThemeColor(ThemeManager.getAccentColor());
        setupEventListeners();
        observeViewModel();
        loadAcademicSnapshotData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null && !isInitialDataLoaded) {
                currentUser = user;
                populateUserData(user);
                isInitialDataLoaded = true;
            }
        });
    }

    private void populateUserData(UserSchema user) {
        String name = user.getName() != null ? user.getName() : "Student";
        binding.nameInput.setText(user.getName());
        binding.collegeInput.setText(user.getCollegeName());

        String branch = user.getBranch();
        if (branch == null || branch.isEmpty()) {
            branch = user.getDepartment();
        }
        binding.branchInput.setText(branch);

        binding.courseInput.setText(user.getCourse());
        binding.classGroupInput.setText(user.getClassGroup());
        binding.rollNumberInput.setText(user.getRollNumber());
        binding.universityRollNumberInput.setText(user.getUniversityRollNumber());
        binding.erpStudentIdInput.setText(user.getErpStudentId() > 0 ? String.valueOf(user.getErpStudentId()) : "");
        binding.phoneInput.setText(user.getPhone());
        binding.emailInput.setText(user.getEmail());

        // Personal Details
        binding.dobInput.setText(user.getDob());
        binding.genderInput.setText(user.getGender());
        binding.categoryInput.setText(user.getCategory());
        binding.bloodGroupInput.setText(user.getBloodGroup());
        binding.statusInput.setText(user.getStatus());
        binding.aadharInput.setText(user.getAadhar());

        // Family Details
        binding.fatherNameInput.setText(user.getFatherName());
        binding.fatherProfessionInput.setText(user.getFatherProfession());
        binding.motherNameInput.setText(user.getMotherName());
        binding.motherProfessionInput.setText(user.getMotherProfession());
        binding.parentMobileInput.setText(user.getParentMobile());

        // Academic & Batch Details
        binding.batchInput.setText(user.getBatch());
        binding.practicalGroupInput.setText(user.getPracticalGroup());
        binding.admissionTypeInput.setText(user.getAdmissionType());
        binding.scholarshipInput.setText(user.getScholarship());

        // Address
        binding.communicationAddressInput.setText(user.getCommunicationAddress());
        binding.permanentAddressInput.setText(user.getPermanentAddress());

        // Education History
        StringBuilder hs = new StringBuilder();
        if (!user.getHighSchoolBoard().isEmpty()) hs.append(user.getHighSchoolBoard());
        if (!user.getHighSchoolPassingYear().isEmpty()) hs.append(" (").append(user.getHighSchoolPassingYear()).append(")");
        if (!user.getHighSchoolMarks().isEmpty()) hs.append("\nMarks: ").append(user.getHighSchoolMarks());
        if (!user.getHighSchoolMaxMarks().isEmpty()) hs.append("/").append(user.getHighSchoolMaxMarks());
        if (!user.getHighSchoolPercentage().isEmpty()) hs.append(" (").append(user.getHighSchoolPercentage()).append("%)");
        binding.highSchoolDetailsInput.setText(hs.toString().trim());

        StringBuilder inter = new StringBuilder();
        if (!user.getInterBoard().isEmpty()) inter.append(user.getInterBoard());
        if (!user.getInterPassingYear().isEmpty()) inter.append(" (").append(user.getInterPassingYear()).append(")");
        if (!user.getInterMarks().isEmpty()) inter.append("\nMarks: ").append(user.getInterMarks());
        if (!user.getInterMaxMarks().isEmpty()) inter.append("/").append(user.getInterMaxMarks());
        if (!user.getInterPercentage().isEmpty()) inter.append(" (").append(user.getInterPercentage()).append("%)");
        binding.interDetailsInput.setText(inter.toString().trim());

        // Hero card labels
        binding.profileHeroName.setText(name);
        String sub = (branch != null && !branch.isEmpty() ? branch : "Academic Hub") + " · Sem " + user.getSemester();
        binding.profileHeroSub.setText(sub);
        binding.profSemTag.setText("Semester " + user.getSemester());
        String rollTag = user.getRollNumber() != null && !user.getRollNumber().isEmpty() 
                ? "Roll #" + user.getRollNumber() 
                : (user.getUniversityRollNumber() != null && !user.getUniversityRollNumber().isEmpty() ? "Uni #" + user.getUniversityRollNumber() : "ID #" + user.getUid());
        binding.profRollTag.setText(rollTag);

        binding.profileInitials.setText(getInitials(name));

        if (user.getThemeColor() != null && !user.getThemeColor().isEmpty()) {
            selectedThemeColor = user.getThemeColor();
        }
        selectThemeColor(selectedThemeColor);

        setSelectedSemester(user.getSemester());

        initialWorkingDays = user.getWorkingDays() > 0 ? user.getWorkingDays() : 5;
        setSelectedWorkingDays(initialWorkingDays);

        int attendanceCrit = user.getAttendanceCriteria() > 0 ? user.getAttendanceCriteria() : 75;
        setSelectedAttendanceCriteria(attendanceCrit);
        binding.statTargetVal.setText(attendanceCrit + "%");

        int maxMarks = user.getMaxMarks() > 0 ? user.getMaxMarks() : 24;
        setSelectedMaxMarks(maxMarks);

        selectedStartTimeMs = user.getFirstPeriodStartTime();
        if (selectedStartTimeMs > 0) {
            binding.firstClassStartTimeInput.setText(formatTime(selectedStartTimeMs));
        }

        selectedStartDateMs = user.getSemStartDate();
        if (selectedStartDateMs > 0) {
            binding.semStartDateInput.setText(DateUtil.getDateString(selectedStartDateMs));
        }

        selectedEndDateMs = user.getSemEndDate();
        if (selectedEndDateMs > 0) {
            binding.semEndDateInput.setText(DateUtil.getDateString(selectedEndDateMs));
        }
    }

    private void loadAcademicSnapshotData() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema user = db.getUserDao().getUser();
            List<SubjectSchema> subjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
            int subjectCount = subjects != null ? subjects.size() : 0;

            int totalConducted = 0;
            int totalAttended = 0;

            if (subjects != null && !subjects.isEmpty()) {
                List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
                List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
                List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

                for (SubjectSchema s : subjects) {
                    boolean isLab = s.isLab();
                    List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(s.getSid(), isLab);
                    AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                            user, s, isLab, records, holidays, days, schedules);
                    totalConducted += stats.getConducted();
                    totalAttended += stats.getAttended();
                }
            }

            int overallPct = totalConducted > 0 ? (int) Math.round((double) totalAttended / totalConducted * 100.0) :0;
            int target = (user != null && user.getAttendanceCriteria() > 0) ? user.getAttendanceCriteria() : 75;

            runOnUiThread(() -> {
                if (binding != null) {
                    binding.statAttendanceVal.setText(overallPct + "%");
                    binding.statAttendanceVal.setTextColor(ContextCompat.getColor(this, overallPct >= target ? R.color.success : R.color.danger));
                    binding.statSubjectsVal.setText(String.valueOf(subjectCount));
                    binding.statTargetVal.setText(target + "%");
                }
            });
        });
    }

    private void setupEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());

        MaterialCardView[] swatchCards = new MaterialCardView[]{
                binding.colorSwatch1,
                binding.colorSwatch2,
                binding.colorSwatch3,
                binding.colorSwatch4,
                binding.colorSwatch5,
                binding.colorSwatch6,
                binding.colorSwatch7,
                binding.colorSwatch8
        };

        for (int i = 0; i < SWATCH_COLORS.length; i++) {
            final String colorHex = SWATCH_COLORS[i];
            MaterialCardView swatchCard = swatchCards[i];
            if (swatchCard != null) {
                swatchCard.setOnClickListener(v -> {
                    if (colorHex.equalsIgnoreCase(selectedThemeColor)) {
                        return;
                    }
                    selectThemeColor(colorHex);
                    ToastUtil.showToastShort("Theme updated successfully!");
                });
            }
        }

        disableChipGroupClicks(binding.semesterChipGroup);
        disableChipGroupClicks(binding.workingDaysChipGroup);
        disableChipGroupClicks(binding.attendanceCriteriaChipGroup);
        disableChipGroupClicks(binding.maxMarksChipGroup);

        binding.profileLogoutBtn.setOnClickListener(v -> showLogoutConfirm());
    }

    private void disableChipGroupClicks(com.google.android.material.chip.ChipGroup chipGroup) {
        if (chipGroup == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child != null) {
                child.setClickable(false);
                child.setFocusable(false);
            }
        }
    }

    private void showLogoutConfirm() {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getSupportFragmentManager(),
                "Log Out from BBSBEC ERP?",
                "Are you sure you want to sign out of your BBSBEC student account? You will return to the login screen.",
                "Log Out",
                "Cancel",
                R.drawable.arrow_left,
                true,
                this::performLogout
        );
    }

    private void performLogout() {
        com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager.clearSession();
        com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil.setSetupComplete(false);
        com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper.execute(() -> {
            com.redcodersgroup.attend_collegeattendancetracker.db.Database.getInstance().clearAllTables();
            runOnUiThread(() -> {
                Intent intent = new Intent(this, com.redcodersgroup.attend_collegeattendancetracker.screens.auth.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void setupNameWatcher() {
        binding.nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String name = s != null ? s.toString().trim() : "";
                binding.profileHeroName.setText(!name.isEmpty() ? name : "Student");
                binding.profileInitials.setText(getInitials(name));
            }
        });

        binding.branchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateHeroSubtitle();
            }
        });

        binding.rollNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String roll = s != null ? s.toString().trim() : "";
                binding.profRollTag.setText(!roll.isEmpty() ? "Roll #" + roll : "ID #1");
            }
        });
    }

    private void updateHeroSubtitle() {
        String branch = binding.branchInput.getText() != null ? binding.branchInput.getText().toString().trim() : "";
        int sem = getSelectedSemester();
        binding.profileHeroSub.setText((!branch.isEmpty() ? branch : "Academic Hub") + " · Sem " + sem);
    }

    private void setupMaxMarksSync() {
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

    private void showTimePickerDialog(View v) {
        Calendar calendar = Calendar.getInstance();
        if (selectedStartTimeMs > 0) {
            calendar.setTimeInMillis(selectedStartTimeMs);
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, selectedHour);
            cal.set(Calendar.MINUTE, selectedMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedStartTimeMs = cal.getTimeInMillis();
            binding.firstClassStartTimeInput.setText(formatTime(selectedStartTimeMs));
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void showStartDatePickerDialog(View v) {
        Calendar calendar = Calendar.getInstance();
        if (selectedStartDateMs > 0) {
            calendar.setTimeInMillis(selectedStartDateMs);
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String dateStr = String.format(Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            selectedStartDateMs = DateUtil.getDateMs(dateStr);
            binding.semStartDateInput.setText(dateStr);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showEndDatePickerDialog(View v) {
        Calendar calendar = Calendar.getInstance();
        if (selectedEndDateMs > 0) {
            calendar.setTimeInMillis(selectedEndDateMs);
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String dateStr = String.format(Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            selectedEndDateMs = DateUtil.getDateMs(dateStr);
            binding.semEndDateInput.setText(dateStr);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void selectThemeColor(String colorHex) {
        selectedThemeColor = colorHex;
        ThemeManager.setAccentColor(colorHex);
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema u = db.getUserDao().getUser();
            if (u != null) {
                u.setThemeColor(colorHex);
                db.getUserDao().updateUser(u);
            }
        });
        try {
            int color = Color.parseColor(colorHex);
            binding.profileCard.setCardBackgroundColor(color);
            binding.saveTopBtn.setTextColor(color);
            binding.saveBottomBtn.setCardBackgroundColor(color);

            int surfaceColor = ContextCompat.getColor(this, R.color.surface);
            ColorStateList chipCsl = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{color, surfaceColor}
            );
            applyChipGroupColor(binding.semesterChipGroup, chipCsl);
            applyChipGroupColor(binding.workingDaysChipGroup, chipCsl);
            applyChipGroupColor(binding.attendanceCriteriaChipGroup, chipCsl);
            applyChipGroupColor(binding.maxMarksChipGroup, chipCsl);
        } catch (Exception ignored) {}

        MaterialCardView[] cards = new MaterialCardView[]{
                binding.colorSwatch1,
                binding.colorSwatch2,
                binding.colorSwatch3,
                binding.colorSwatch4,
                binding.colorSwatch5,
                binding.colorSwatch6,
                binding.colorSwatch7,
                binding.colorSwatch8
        };

        int activeStrokeColor = getResources().getColor(R.color.text, null);
        int inactiveStrokeColor = getResources().getColor(R.color.border, null);

        for (int i = 0; i < SWATCH_COLORS.length; i++) {
            if (SWATCH_COLORS[i].equalsIgnoreCase(colorHex)) {
                cards[i].setStrokeColor(activeStrokeColor);
                cards[i].setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            } else {
                cards[i].setStrokeColor(inactiveStrokeColor);
                cards[i].setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }
        }
    }

    private void applyChipGroupColor(com.google.android.material.chip.ChipGroup chipGroup, ColorStateList chipCsl) {
        if (chipGroup == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                ((com.google.android.material.chip.Chip) child).setChipBackgroundColor(chipCsl);
            }
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(parts[0].length(), 2)).toUpperCase(Locale.getDefault());
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.getDefault());
        }
    }

    private String formatTime(long timeMs) {
        if (timeMs <= 0) return "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMs);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm);
    }

    private int getSelectedSemester() {
        int id = binding.semesterChipGroup.getCheckedChipId();
        if (id != View.NO_ID) {
            View chip = binding.semesterChipGroup.findViewById(id);
            if (chip != null && chip.getTag() != null) {
                try {
                    return Integer.parseInt(chip.getTag().toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 1;
    }

    private void setSelectedSemester(int semester) {
        int chipId;
        switch (semester) {
            case 2:
                chipId = R.id.sem2;
                break;
            case 3:
                chipId = R.id.sem3;
                break;
            case 4:
                chipId = R.id.sem4;
                break;
            case 5:
                chipId = R.id.sem5;
                break;
            case 6:
                chipId = R.id.sem6;
                break;
            case 7:
                chipId = R.id.sem7;
                break;
            case 8:
                chipId = R.id.sem8;
                break;
            default:
                chipId = R.id.sem1;
                break;
        }
        binding.semesterChipGroup.check(chipId);
    }

    private int getSelectedWorkingDays() {
        int id = binding.workingDaysChipGroup.getCheckedChipId();
        if (id == R.id.chip6Days) {
            return 6;
        }
        return 5;
    }

    private void setSelectedWorkingDays(int workingDays) {
        if (workingDays == 6) {
            binding.workingDaysChipGroup.check(R.id.chip6Days);
        } else {
            binding.workingDaysChipGroup.check(R.id.chip5Days);
        }
    }

    private int getSelectedAttendanceCriteria() {
        int id = binding.attendanceCriteriaChipGroup.getCheckedChipId();
        if (id == R.id.chip65) return 65;
        if (id == R.id.chip80) return 80;
        if (id == R.id.chip85) return 85;
        return 75;
    }

    private void setSelectedAttendanceCriteria(int criteria) {
        if (criteria == 65) binding.attendanceCriteriaChipGroup.check(R.id.chip65);
        else if (criteria == 80) binding.attendanceCriteriaChipGroup.check(R.id.chip80);
        else if (criteria == 85) binding.attendanceCriteriaChipGroup.check(R.id.chip85);
        else binding.attendanceCriteriaChipGroup.check(R.id.chip75);
    }

    private int getSelectedMaxMarks() {
        String text = binding.maxMarksInput.getText() != null ? binding.maxMarksInput.getText().toString().trim() : "";
        if (!text.isEmpty()) {
            try {
                int val = Integer.parseInt(text);
                if (val > 0) return val;
            } catch (NumberFormatException ignored) {}
        }
        return 24;
    }

    private void setSelectedMaxMarks(int maxMarks) {
        isUpdatingMaxMarks = true;
        int m = maxMarks > 0 ? maxMarks : 24;
        binding.maxMarksInput.setText(String.valueOf(m));
        if (m == 20) binding.maxMarksChipGroup.check(R.id.m_20);
        else if (m == 24) binding.maxMarksChipGroup.check(R.id.m_24);
        else if (m == 25) binding.maxMarksChipGroup.check(R.id.m_25);
        else if (m == 30) binding.maxMarksChipGroup.check(R.id.m_30);
        else if (m == 50) binding.maxMarksChipGroup.check(R.id.m_50);
        else binding.maxMarksChipGroup.clearCheck();
        isUpdatingMaxMarks = false;
    }

    private void handleSaveProfile() {
        // App has unidirectional read-only ERP permissions; no local profile changes are allowed.
    }
}