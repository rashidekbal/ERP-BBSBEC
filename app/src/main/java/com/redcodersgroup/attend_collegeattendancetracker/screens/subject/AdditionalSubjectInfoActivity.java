package com.redcodersgroup.attend_collegeattendancetracker.screens.subject;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityAdditionalSubjectInfoBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectAdditionalInfoSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class AdditionalSubjectInfoActivity extends AppCompatActivity {
    private ActivityAdditionalSubjectInfoBinding binding;
    private int sid;
    private SubjectSchema currentSubject;
    private UserSchema currentUser;
    private SubjectAdditionalInfoSchema currentAdditionalInfo;

    // Maps day name (e.g. "Monday") to its DayScheduleSchema and TimeTableDaySchema
    private final Map<String, TimeTableDaySchema> daySchemaMap = new HashMap<>();
    private final Map<String, DayScheduleSchema> dayScheduleMap = new HashMap<>();

    // Selected period slots per day (e.g. "Monday" -> {1, 2})
    private final Map<String, Set<Integer>> selectedSlotsMap = new HashMap<>();

    private boolean isBindingChips = false;

    public static Intent getAdditionalSubjectInfoActivityIntent(AppCompatActivity activity, int sid) {
        Intent intent = new Intent(activity, AdditionalSubjectInfoActivity.class);
        intent.putExtra("sid", sid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityAdditionalSubjectInfoBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        sid = getIntent().getIntExtra("sid", -1);
        if (sid == -1) {
            ToastUtil.showToastShort("Invalid subject ID");
            finish();
            return;
        }

        initSelectedSlotsMap();
        applyAccentColor();
        setEventListeners();
        loadData();
    }

    private void applyAccentColor() {
        int accent = ThemeManager.getAccentColorInt();
        binding.calcClassesBtn.setBackgroundColor(accent);
        binding.calcLabsBtn.setBackgroundColor(accent);
        binding.saveBtn.setBackgroundColor(accent);
    }

    private void initSelectedSlotsMap() {
        selectedSlotsMap.put("Monday", new HashSet<>());
        selectedSlotsMap.put("Tuesday", new HashSet<>());
        selectedSlotsMap.put("Wednesday", new HashSet<>());
        selectedSlotsMap.put("Thursday", new HashSet<>());
        selectedSlotsMap.put("Friday", new HashSet<>());
        selectedSlotsMap.put("Saturday", new HashSet<>());
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.subjectName.setOnClickListener(v -> showEditSubjectNameDialog());
        binding.calcClassesBtn.setOnClickListener(v -> handleAutoCalculateClasses());
        binding.calcLabsBtn.setOnClickListener(v -> handleAutoCalculateLabs());
        binding.saveBtn.setOnClickListener(v -> handleSaveBtnClick());

        TextWatcher slotLimitWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateSlotCountersAndBadges();
            }
        };

        binding.classesPerWeek.addTextChangedListener(slotLimitWatcher);
        binding.labsPerWeek.addTextChangedListener(slotLimitWatcher);
    }

    private void loadData() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            currentSubject = db.getSubjectDao().getSubjectById(sid);
            if (currentSubject == null) {
                runOnUiThread(() -> {
                    ToastUtil.showToastShort("Subject not found");
                    finish();
                });
                return;
            }

            currentUser = db.getUserDao().getUser();
            currentAdditionalInfo = db.getSubjectAdditionalInfoDao().getSubjectAdditionalInfoBySid(sid);

            // Ensure timetable days exist
            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            int workingDays = currentUser != null ? currentUser.getWorkingDays() : 5;
            if (days == null || days.isEmpty()) {
                List<String> dayNames = (workingDays == 6)
                        ? List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                        : List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
                List<TimeTableDaySchema> newDays = new ArrayList<>();
                for (String name : dayNames) {
                    newDays.add(new TimeTableDaySchema(name));
                }
                db.getTimeTableDayDao().insertMulti(newDays);
                days = db.getTimeTableDayDao().getDays();

                List<DayScheduleSchema> newSchedules = new ArrayList<>();
                for (TimeTableDaySchema d : days) {
                    newSchedules.add(new DayScheduleSchema(d.getDid(), null, null, null, null, null, null, null, null, null));
                }
                db.getDayScheduleDao().insertMulti(newSchedules);
            }

            daySchemaMap.clear();
            dayScheduleMap.clear();
            for (TimeTableDaySchema day : days) {
                daySchemaMap.put(day.getDayName(), day);
                DayScheduleSchema schedule = db.getDayScheduleDao().getScheduleByDid(day.getDid());
                if (schedule == null) {
                    schedule = new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null);
                    db.getDayScheduleDao().insert(schedule);
                    schedule = db.getDayScheduleDao().getScheduleByDid(day.getDid());
                }
                dayScheduleMap.put(day.getDayName(), schedule);
            }

            // Populate selected slots from existing schedules
            for (Map.Entry<String, DayScheduleSchema> entry : dayScheduleMap.entrySet()) {
                String dayName = entry.getKey();
                DayScheduleSchema schedule = entry.getValue();
                Set<Integer> slots = selectedSlotsMap.get(dayName);
                if (slots != null) {
                    slots.clear();
                    for (int p = 1; p <= 9; p++) {
                        Integer assignedSid = getPeriodValue(schedule, p);
                        if (assignedSid != null && assignedSid == sid) {
                            slots.add(p);
                        }
                    }
                }
            }

            runOnUiThread(this::bindDataToUi);
        });
    }

    private void bindDataToUi() {
        if (currentSubject == null) return;

        // Set subject title
        binding.subjectName.setText(currentSubject.getSubjectName());

        // Set sub heading
        binding.subHeading.setText(getString(R.string.sub_additional_ingo_add_page_sub_heading));

        boolean isLab = currentSubject.isLab();
        binding.classesSection.setVisibility(isLab ? View.GONE : View.VISIBLE);
        binding.labsSection.setVisibility(isLab ? View.VISIBLE : View.GONE);

        // Show/hide Saturday based on 6 working days
        int workingDays = currentUser != null ? currentUser.getWorkingDays() : 5;
        boolean is6Days = (workingDays == 6);
        binding.saturdayClassGroup.setVisibility(is6Days && !isLab ? View.VISIBLE : View.GONE);
        binding.saturdayLabGroup.setVisibility(is6Days && isLab ? View.VISIBLE : View.GONE);

        // Pre-fill classes per week or labs per week
        if (isLab) {
            int lpw = currentSubject.getLabsPerWeek() > 0 ? currentSubject.getLabsPerWeek() : currentSubject.getClassesPerWeek();
            binding.labsPerWeek.setText(String.valueOf(lpw > 0 ? lpw : 1));
        } else {
            int cpw = currentSubject.getClassesPerWeek();
            binding.classesPerWeek.setText(String.valueOf(cpw > 0 ? cpw : 4));
        }

        // Pre-fill total classes or total labs if already saved
        if (currentAdditionalInfo != null) {
            if (isLab) {
                int labs = currentAdditionalInfo.getTotalLabs() > 0 ? currentAdditionalInfo.getTotalLabs() : currentAdditionalInfo.getTotalClasses();
                binding.totalLabs.setText(String.valueOf(labs));
            } else {
                binding.totalClasses.setText(String.valueOf(currentAdditionalInfo.getTotalClasses()));
            }
        }

        // Setup Day Chips
        setupAllDayChips();
        updateSlotCountersAndBadges();
    }

    private boolean isLabSubject() {
        return currentSubject != null && currentSubject.isLab();
    }

    private int getMaxWeeklySlots() {
        if (isLabSubject()) {
            String lpwStr = binding.labsPerWeek.getText() != null ? binding.labsPerWeek.getText().toString().trim() : "";
            if (!lpwStr.isEmpty()) {
                try {
                    int val = Integer.parseInt(lpwStr);
                    if (val > 0) return val;
                } catch (NumberFormatException ignored) {}
            }
            return currentSubject != null && currentSubject.getLabsPerWeek() > 0 ? currentSubject.getLabsPerWeek() : 1;
        } else {
            String cpwStr = binding.classesPerWeek.getText() != null ? binding.classesPerWeek.getText().toString().trim() : "";
            if (!cpwStr.isEmpty()) {
                try {
                    int val = Integer.parseInt(cpwStr);
                    if (val > 0) return val;
                } catch (NumberFormatException ignored) {}
            }
            return currentSubject != null && currentSubject.getClassesPerWeek() > 0 ? currentSubject.getClassesPerWeek() : 4;
        }
    }

    private int getTotalSelectedSlots() {
        int count = 0;
        for (Set<Integer> slots : selectedSlotsMap.values()) {
            count += slots.size();
        }
        return count;
    }

    private void setupAllDayChips() {
        isBindingChips = true;
        boolean isLab = isLabSubject();

        if (isLab) {
            setupDayChips("Monday", getLabChipsForMonday());
            setupDayChips("Tuesday", getLabChipsForTuesday());
            setupDayChips("Wednesday", getLabChipsForWednesday());
            setupDayChips("Thursday", getLabChipsForThursday());
            setupDayChips("Friday", getLabChipsForFriday());
            setupDayChips("Saturday", getLabChipsForSaturday());
        } else {
            setupDayChips("Monday", getClassChipsForMonday());
            setupDayChips("Tuesday", getClassChipsForTuesday());
            setupDayChips("Wednesday", getClassChipsForWednesday());
            setupDayChips("Thursday", getClassChipsForThursday());
            setupDayChips("Friday", getClassChipsForFriday());
            setupDayChips("Saturday", getClassChipsForSaturday());
        }
        isBindingChips = false;
    }

    private void setupDayChips(String dayName, List<Chip> chips) {
        DayScheduleSchema schedule = dayScheduleMap.get(dayName);
        Set<Integer> selectedSlots = selectedSlotsMap.computeIfAbsent(dayName, k -> new HashSet<>());

        for (int p = 1; p <= 9 && p <= chips.size(); p++) {
            Chip chip = chips.get(p - 1);
            final int periodNumber = p;

            Integer assignedSid = getPeriodValue(schedule, p);
            boolean isAssignedToOther = (assignedSid != null && assignedSid != sid);

            if (isAssignedToOther) {
                chip.setEnabled(false);
                chip.setChecked(false);
                chip.setAlpha(0.4f);
            } else {
                chip.setEnabled(true);
                chip.setAlpha(1.0f);
                boolean isChecked = selectedSlots.contains(periodNumber);
                chip.setChecked(isChecked);

                chip.setOnCheckedChangeListener((buttonView, isCheckedNow) -> {
                    if (isBindingChips) return;
                    handleChipToggle(dayName, periodNumber, isCheckedNow, chip);
                });
            }
        }
    }

    private void handleChipToggle(String dayName, int periodNumber, boolean isChecked, Chip chip) {
        Set<Integer> daySlots = selectedSlotsMap.computeIfAbsent(dayName, k -> new HashSet<>());

        if (isChecked) {
            int maxSlots = getMaxWeeklySlots();
            int currentTotal = getTotalSelectedSlots();

            if (currentTotal >= maxSlots) {
                isBindingChips = true;
                chip.setChecked(false);
                isBindingChips = false;
                String type = isLabSubject() ? "lab slots" : "classes";
                ToastUtil.showToastShort("Cannot book more than " + maxSlots + " " + type + " per week");
                return;
            }
            daySlots.add(periodNumber);
        } else {
            daySlots.remove(periodNumber);
        }

        updateSlotCountersAndBadges();
    }

    private void updateSlotCountersAndBadges() {
        int totalBooked = getTotalSelectedSlots();
        int maxSlots = getMaxWeeklySlots();
        int accent = ThemeManager.getAccentColorInt();

        if (isLabSubject()) {
            binding.labSlotCounterText.setText(totalBooked + " / " + maxSlots + " slots booked");
            if (totalBooked == maxSlots) {
                binding.labSlotCounterText.setTextColor(ContextCompat.getColor(this, R.color.success));
            } else {
                binding.labSlotCounterText.setTextColor(accent);
            }

            updateDayBadge(binding.mondayLabBadge, selectedSlotsMap.get("Monday"));
            updateDayBadge(binding.tuesdayLabBadge, selectedSlotsMap.get("Tuesday"));
            updateDayBadge(binding.wednesdayLabBadge, selectedSlotsMap.get("Wednesday"));
            updateDayBadge(binding.thursdayLabBadge, selectedSlotsMap.get("Thursday"));
            updateDayBadge(binding.fridayLabBadge, selectedSlotsMap.get("Friday"));
            updateDayBadge(binding.saturdayLabBadge, selectedSlotsMap.get("Saturday"));
        } else {
            binding.classSlotCounterText.setText(totalBooked + " / " + maxSlots + " slots booked");
            if (totalBooked == maxSlots) {
                binding.classSlotCounterText.setTextColor(ContextCompat.getColor(this, R.color.success));
            } else {
                binding.classSlotCounterText.setTextColor(accent);
            }

            updateDayBadge(binding.mondayClassBadge, selectedSlotsMap.get("Monday"));
            updateDayBadge(binding.tuesdayClassBadge, selectedSlotsMap.get("Tuesday"));
            updateDayBadge(binding.wednesdayClassBadge, selectedSlotsMap.get("Wednesday"));
            updateDayBadge(binding.thursdayClassBadge, selectedSlotsMap.get("Thursday"));
            updateDayBadge(binding.fridayClassBadge, selectedSlotsMap.get("Friday"));
            updateDayBadge(binding.saturdayClassBadge, selectedSlotsMap.get("Saturday"));
        }
    }

    private void updateDayBadge(TextView badgeView, Set<Integer> slots) {
        if (badgeView == null) return;
        if (slots == null || slots.isEmpty()) {
            badgeView.setText("No slots");
            badgeView.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
        } else {
            List<Integer> sorted = new ArrayList<>(new TreeSet<>(slots));
            StringBuilder sb = new StringBuilder();
            sb.append(sorted.size()).append(sorted.size() == 1 ? " slot (" : " slots (");
            for (int i = 0; i < sorted.size(); i++) {
                sb.append("P").append(sorted.get(i));
                if (i < sorted.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            badgeView.setText(sb.toString());
            badgeView.setTextColor(ThemeManager.getAccentColorInt());
        }
    }

    private List<Chip> getClassChipsForMonday() {
        return List.of(binding.mondayClassP1, binding.mondayClassP2, binding.mondayClassP3, binding.mondayClassP4, binding.mondayClassP5, binding.mondayClassP6, binding.mondayClassP7, binding.mondayClassP8, binding.mondayClassP9);
    }

    private List<Chip> getClassChipsForTuesday() {
        return List.of(binding.tuesdayClassP1, binding.tuesdayClassP2, binding.tuesdayClassP3, binding.tuesdayClassP4, binding.tuesdayClassP5, binding.tuesdayClassP6, binding.tuesdayClassP7, binding.tuesdayClassP8, binding.tuesdayClassP9);
    }

    private List<Chip> getClassChipsForWednesday() {
        return List.of(binding.wednesdayClassP1, binding.wednesdayClassP2, binding.wednesdayClassP3, binding.wednesdayClassP4, binding.wednesdayClassP5, binding.wednesdayClassP6, binding.wednesdayClassP7, binding.wednesdayClassP8, binding.wednesdayClassP9);
    }

    private List<Chip> getClassChipsForThursday() {
        return List.of(binding.thursdayClassP1, binding.thursdayClassP2, binding.thursdayClassP3, binding.thursdayClassP4, binding.thursdayClassP5, binding.thursdayClassP6, binding.thursdayClassP7, binding.thursdayClassP8, binding.thursdayClassP9);
    }

    private List<Chip> getClassChipsForFriday() {
        return List.of(binding.fridayClassP1, binding.fridayClassP2, binding.fridayClassP3, binding.fridayClassP4, binding.fridayClassP5, binding.fridayClassP6, binding.fridayClassP7, binding.fridayClassP8, binding.fridayClassP9);
    }

    private List<Chip> getClassChipsForSaturday() {
        return List.of(binding.saturdayClassP1, binding.saturdayClassP2, binding.saturdayClassP3, binding.saturdayClassP4, binding.saturdayClassP5, binding.saturdayClassP6, binding.saturdayClassP7, binding.saturdayClassP8, binding.saturdayClassP9);
    }

    private List<Chip> getLabChipsForMonday() {
        return List.of(binding.mondayLabP1, binding.mondayLabP2, binding.mondayLabP3, binding.mondayLabP4, binding.mondayLabP5, binding.mondayLabP6, binding.mondayLabP7, binding.mondayLabP8, binding.mondayLabP9);
    }

    private List<Chip> getLabChipsForTuesday() {
        return List.of(binding.tuesdayLabP1, binding.tuesdayLabP2, binding.tuesdayLabP3, binding.tuesdayLabP4, binding.tuesdayLabP5, binding.tuesdayLabP6, binding.tuesdayLabP7, binding.tuesdayLabP8, binding.tuesdayLabP9);
    }

    private List<Chip> getLabChipsForWednesday() {
        return List.of(binding.wednesdayLabP1, binding.wednesdayLabP2, binding.wednesdayLabP3, binding.wednesdayLabP4, binding.wednesdayLabP5, binding.wednesdayLabP6, binding.wednesdayLabP7, binding.wednesdayLabP8, binding.wednesdayLabP9);
    }

    private List<Chip> getLabChipsForThursday() {
        return List.of(binding.thursdayLabP1, binding.thursdayLabP2, binding.thursdayLabP3, binding.thursdayLabP4, binding.thursdayLabP5, binding.thursdayLabP6, binding.thursdayLabP7, binding.thursdayLabP8, binding.thursdayLabP9);
    }

    private List<Chip> getLabChipsForFriday() {
        return List.of(binding.fridayLabP1, binding.fridayLabP2, binding.fridayLabP3, binding.fridayLabP4, binding.fridayLabP5, binding.fridayLabP6, binding.fridayLabP7, binding.fridayLabP8, binding.fridayLabP9);
    }

    private List<Chip> getLabChipsForSaturday() {
        return List.of(binding.saturdayLabP1, binding.saturdayLabP2, binding.saturdayLabP3, binding.saturdayLabP4, binding.saturdayLabP5, binding.saturdayLabP6, binding.saturdayLabP7, binding.saturdayLabP8, binding.saturdayLabP9);
    }

    private void handleAutoCalculateClasses() {
        if (currentUser == null || currentSubject == null) return;
        long start = currentUser.getSemStartDate();
        long end = currentUser.getSemEndDate();

        if (start <= 0 || end <= 0 || end <= start) {
            ToastUtil.showToastShort("Please set valid semester start and end dates in profile");
            return;
        }

        long diffMs = end - start;
        long totalDays = (diffMs / (24L * 60 * 60 * 1000L)) + 1;
        int weeks = (int) Math.max(1, Math.round((double) totalDays / 7.0));

        int cpw = getMaxWeeklySlots();
        int calculatedClasses = weeks * cpw;
        binding.totalClasses.setText(String.valueOf(calculatedClasses));
    }

    private void handleAutoCalculateLabs() {
        if (currentUser == null || currentSubject == null) return;
        long start = currentUser.getSemStartDate();
        long end = currentUser.getSemEndDate();

        if (start <= 0 || end <= 0 || end <= start) {
            ToastUtil.showToastShort("Please set valid semester start and end dates in profile");
            return;
        }

        long diffMs = end - start;
        long totalDays = (diffMs / (24L * 60 * 60 * 1000L)) + 1;
        int weeks = (int) Math.max(1, Math.round((double) totalDays / 7.0));

        int lpw = getMaxWeeklySlots();
        int calculatedLabs = weeks * lpw;
        binding.totalLabs.setText(String.valueOf(calculatedLabs));
    }

    private void handleSaveBtnClick() {
        if (currentSubject == null) return;
        boolean isLab = currentSubject.isLab();

        int totalClasses = 0;
        int totalLabs = 0;
        int maxSlots = getMaxWeeklySlots();

        if (isLab) {
            String totalLabsStr = Objects.requireNonNull(binding.totalLabs.getText()).toString().trim();
            if (totalLabsStr.isEmpty()) {
                binding.totalLabs.setError("Total labs is required");
                return;
            }
            try {
                totalLabs = Integer.parseInt(totalLabsStr);
                if (totalLabs < 0) {
                    binding.totalLabs.setError("Total labs cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                binding.totalLabs.setError("Invalid number");
                return;
            }
            totalClasses = totalLabs;
        } else {
            String totalClassesStr = Objects.requireNonNull(binding.totalClasses.getText()).toString().trim();
            if (totalClassesStr.isEmpty()) {
                binding.totalClasses.setError("Total classes is required");
                return;
            }
            try {
                totalClasses = Integer.parseInt(totalClassesStr);
                if (totalClasses < 0) {
                    binding.totalClasses.setError("Total classes cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                binding.totalClasses.setError("Invalid number");
                return;
            }
        }

        int totalBooked = getTotalSelectedSlots();
        if (totalBooked > maxSlots) {
            ToastUtil.showToastShort("Booked slots (" + totalBooked + ") exceed weekly limit (" + maxSlots + ")");
            return;
        }

        binding.saveBtn.setEnabled(false);

        final int finalTotalClasses = totalClasses;
        final int finalTotalLabs = totalLabs;
        final int finalWeeklySlots = maxSlots;

        DbHelper.execute(() -> {
            Database db = Database.getInstance();

            // 1. Update Subject weekly slots
            if (isLab) {
                currentSubject.setLabsPerWeek(finalWeeklySlots);
                currentSubject.setClassesPerWeek(finalWeeklySlots);
            } else {
                currentSubject.setClassesPerWeek(finalWeeklySlots);
            }

            // 2. Save or update SubjectAdditionalInfo
            SubjectAdditionalInfoSchema additionalInfo = db.getSubjectAdditionalInfoDao().getSubjectAdditionalInfoBySid(sid);
            if (additionalInfo != null) {
                additionalInfo.setTotalClasses(finalTotalClasses);
                additionalInfo.setTotalLabs(finalTotalLabs);
                db.getSubjectAdditionalInfoDao().update(additionalInfo);
            } else {
                additionalInfo = new SubjectAdditionalInfoSchema(sid, finalTotalClasses, finalTotalLabs);
                db.getSubjectAdditionalInfoDao().insert(additionalInfo);
            }

            // 3. Update DaySchedule for each day
            for (Map.Entry<String, DayScheduleSchema> entry : dayScheduleMap.entrySet()) {
                String dayName = entry.getKey();
                DayScheduleSchema schedule = entry.getValue();
                Set<Integer> selectedSlots = selectedSlotsMap.get(dayName);

                // Update periods 1..9
                for (int p = 1; p <= 9; p++) {
                    if (selectedSlots != null && selectedSlots.contains(p)) {
                        setPeriodValue(schedule, p, sid);
                    } else if (Objects.equals(getPeriodValue(schedule, p), sid)) {
                        setPeriodValue(schedule, p, null);
                    }
                }

                db.getDayScheduleDao().insert(schedule);
            }

            // 4. Mark Subject setup as complete
            currentSubject.setAttendanceSetUpComplete(true);
            currentSubject.setTimeTableSetUpComplete(true);
            db.getSubjectDao().updateSubject(currentSubject);

            runOnUiThread(() -> {
                ToastUtil.showToastShort(isLab ? "Lab setup completed" : "Subject setup completed");
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private Integer getPeriodValue(DayScheduleSchema schedule, int periodNumber) {
        if (schedule == null) return null;
        switch (periodNumber) {
            case 1: return schedule.getP1();
            case 2: return schedule.getP2();
            case 3: return schedule.getP3();
            case 4: return schedule.getP4();
            case 5: return schedule.getP5();
            case 6: return schedule.getP6();
            case 7: return schedule.getP7();
            case 8: return schedule.getP8();
            case 9: return schedule.getP9();
            default: return null;
        }
    }

    private void setPeriodValue(DayScheduleSchema schedule, int periodNumber, Integer value) {
        if (schedule == null) return;
        switch (periodNumber) {
            case 1: schedule.setP1(value); break;
            case 2: schedule.setP2(value); break;
            case 3: schedule.setP3(value); break;
            case 4: schedule.setP4(value); break;
            case 5: schedule.setP5(value); break;
            case 6: schedule.setP6(value); break;
            case 7: schedule.setP7(value); break;
            case 8: schedule.setP8(value); break;
            case 9: schedule.setP9(value); break;
        }
    }

    private void showEditSubjectNameDialog() {
        String currentName = currentSubject != null ? currentSubject.getSubjectName() : "";

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.TransparentBottomSheet);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject_name, null);
        dialog.setContentView(dialogView);

        com.google.android.material.textfield.TextInputEditText nameInput = dialogView.findViewById(R.id.subjectNameInput);
        com.google.android.material.textfield.TextInputLayout inputLayout = dialogView.findViewById(R.id.subjectNameInputLayout);
        View cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        View saveBtn = dialogView.findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccentColorInt()));
        }

        nameInput.setText(currentName);
        if (currentName != null && !currentName.isEmpty()) {
            nameInput.setSelection(currentName.length());
        }

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String newName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                inputLayout.setError("Subject name cannot be empty");
                return;
            }

            inputLayout.setError(null);
            dialog.dismiss();

            if (currentSubject != null) {
                currentSubject.setSubjectName(newName);
            }
            binding.subjectName.setText(newName);
            DbHelper.updateSubjectName(sid, newName, this::loadData);
            ToastUtil.showToastShort("Subject name updated");
        });

        dialog.show();
    }
}