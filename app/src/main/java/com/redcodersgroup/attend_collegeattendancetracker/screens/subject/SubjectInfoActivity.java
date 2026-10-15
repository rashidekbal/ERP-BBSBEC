package com.redcodersgroup.attend_collegeattendancetracker.screens.subject;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivitySubjectInfoBinding;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogCalendarDayInfoBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.adapter.AttendanceHistoryAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.adapter.SubjectCalendarAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.model.DayScheduleInfo;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.viewmodel.SubjectInfoViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubjectInfoActivity extends AppCompatActivity {

    public static final String EXTRA_SID = "sid";
    public static final String EXTRA_IS_LAB = "isLab";
    public static final String EXTRA_SUBJECT_NAME = "subjectName";

    private ActivitySubjectInfoBinding binding;
    private SubjectInfoViewModel viewModel;

    private int sid;
    private boolean isLab;
    private String passedSubjectName;

    private List<CalendarDayModel> calendarDaysList;
    private SubjectCalendarAdapter calendarAdapter;

    private List<AttendanceSchema> historyList;
    private AttendanceHistoryAdapter historyAdapter;

    // Bunk Simulator State
    private int simAttend = 0;
    private int simBunk = 0;
    private int baseAttended = 0;
    private int baseConducted = 0;
    private int baseCriteria = 75;
    private int baseRemaining = 0;

    public static Intent getSubjectInfoIntent(Context context, int sid, boolean isLab, String subjectName) {
        Intent intent = new Intent(context, SubjectInfoActivity.class);
        intent.putExtra(EXTRA_SID, sid);
        intent.putExtra(EXTRA_IS_LAB, isLab);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySubjectInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        extractIntentData();
        initViews();
        setupObservers();
        setEventListeners();

        viewModel.init(sid, isLab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadAllData();
        }
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            sid = intent.getIntExtra(EXTRA_SID, -1);
            isLab = intent.getBooleanExtra(EXTRA_IS_LAB, false);
            passedSubjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME);
        }
    }

    private void initViews() {
        viewModel = new ViewModelProvider(this).get(SubjectInfoViewModel.class);

        // Format today's date for display
        SimpleDateFormat todaySdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        binding.todayDateText.setText(todaySdf.format(new Date()));

        if (passedSubjectName != null && !passedSubjectName.isEmpty()) {
            binding.subjectTitle.setText(passedSubjectName + (isLab && !passedSubjectName.toLowerCase().contains("lab") ? " Lab" : ""));
        }
        binding.subjectTypeBadge.setText(isLab ? "Laboratory Practical" : "Theory Subject");

        // Simulator setup
        binding.simAttendLabel.setText(isLab ? "Attend Lab" : "Attend Class");
        binding.simBunkLabel.setText(isLab ? "Bunk Lab" : "Bunk Class");
        binding.simCardSubtitle.setText(isLab ? "Simulate attending or bunking this lab" : "Simulate attending or bunking this subject");

        // Calendar setup
        calendarDaysList = new ArrayList<>();
        calendarAdapter = new SubjectCalendarAdapter(this, calendarDaysList);
        binding.subjectCalendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        binding.subjectCalendarRecyclerView.setAdapter(calendarAdapter);
        calendarAdapter.setOnDayClickListener(this::handleCalendarDayClick);

        // History setup
        historyList = new ArrayList<>();
        historyAdapter = new AttendanceHistoryAdapter(this, historyList);
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.historyRecyclerView.setAdapter(historyAdapter);
        historyAdapter.setOnDeleteClickListener(this::handleDeleteHistoryItem);

        // Swipe Refresh
        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.syncImmediately(new com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (viewModel != null) {
                            viewModel.loadAllData();
                        }
                        com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Subject attendance updated");
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Sync error: " + message);
                    }
                }
            });
        });
    }

    private void setupObservers() {
        viewModel.getSubject().observe(this, subjectSchema -> {
            if (subjectSchema != null) {
                String name = subjectSchema.getSubjectName();
                if (isLab && !name.toLowerCase().contains("lab")) {
                    name += " Lab";
                }
                binding.subjectTitle.setText(name);

                if (subjectSchema.getUniqueColorCode() != null && !subjectSchema.getUniqueColorCode().isEmpty()) {
                    try {
                        int color = Color.parseColor(subjectSchema.getUniqueColorCode());
                        binding.progressIndicator.setIndicatorColor(color);
                    } catch (Exception ignored) {
                    }
                }

                StringBuilder details = new StringBuilder();
                if (subjectSchema.getTeacherName() != null && !subjectSchema.getTeacherName().trim().isEmpty()) {
                    details.append("Faculty: ").append(subjectSchema.getTeacherName().trim());
                }
                if (subjectSchema.getRoomNo() != null && !subjectSchema.getRoomNo().trim().isEmpty()) {
                    if (details.length() > 0) details.append(" · ");
                    details.append("Room: ").append(subjectSchema.getRoomNo().trim());
                }
                if (details.length() > 0) {
                    binding.subjectFacultyAndRoom.setText(details.toString());
                    binding.subjectFacultyAndRoom.setVisibility(View.VISIBLE);
                } else {
                    binding.subjectFacultyAndRoom.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getAdditionalInfo().observe(this, addInfo -> {
            updateStatsCards();
        });

        viewModel.getAttendedCount().observe(this, attended -> {
            baseAttended = attended != null ? attended : 0;
            updateStatsCards();
            updateSimulationDisplay();
        });

        viewModel.getConductedCount().observe(this, conducted -> {
            baseConducted = conducted != null ? conducted : 0;
            updateStatsCards();
            updateSimulationDisplay();
        });

        viewModel.getTotalCount().observe(this, total -> {
            updateStatsCards();
        });

        viewModel.getMissedCount().observe(this, missed -> {
            binding.missedCount.setText((missed != null ? missed : 0) + " missed");
            updateStatsCards();
        });

        viewModel.getCriteria().observe(this, crit -> {
            baseCriteria = crit != null && crit > 0 ? crit : 75;
            binding.criteriaText.setText("Required Target: " + crit + "%");
            binding.subjectTypeBadge.setText((isLab ? "Laboratory Practical" : "Theory Subject") + " · " + crit + "% Criteria");
            updateSimulationDisplay();
        });

        viewModel.getRemainingCount().observe(this, remaining -> {
            baseRemaining = remaining != null ? remaining : 0;
            updateSimulationDisplay();
        });

        viewModel.getPercentage().observe(this, pct -> {
            int target = pct != null ? pct : 0;
            com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil.animateProgress(
                    binding.progressIndicator, target, binding.overallPercentage);
        });

        viewModel.getIsOnTrack().observe(this, onTrack -> {
            int conducted = viewModel.getConductedCount().getValue() != null ? viewModel.getConductedCount().getValue() : 0;
            int accentColor = ThemeManager.getAccentColorInt();
            if (conducted == 0) {
                binding.attendanceStatusTitle.setText("No Records");
                binding.attendanceStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.text));
                binding.overallPercentage.setTextColor(ContextCompat.getColor(this, R.color.text));
                binding.bunkSafeStatus.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
                binding.progressIndicator.setIndicatorColor(accentColor);
            } else if (Boolean.TRUE.equals(onTrack)) {
                binding.attendanceStatusTitle.setText("On Track");
                binding.attendanceStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.text));
                binding.overallPercentage.setTextColor(ContextCompat.getColor(this, R.color.text));
                binding.bunkSafeStatus.setTextColor(ContextCompat.getColor(this, R.color.text));
                binding.progressIndicator.setIndicatorColor(accentColor);
            } else {
                binding.attendanceStatusTitle.setText("At Risk / Below Target");
                binding.attendanceStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.danger));
                binding.overallPercentage.setTextColor(ContextCompat.getColor(this, R.color.danger));
                binding.bunkSafeStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
                binding.progressIndicator.setIndicatorColor(ContextCompat.getColor(this, R.color.danger));
            }
        });

        viewModel.getBunkSafeStatus().observe(this, status -> {
            binding.bunkSafeStatus.setText(status != null ? status : "");
        });

        viewModel.getTodayAttendanceStatus().observe(this, status -> {
            updateTodayActionButtons(status);
        });

        viewModel.getTodayScheduleInfo().observe(this, scheduleInfo -> {
            binding.markTodayCard.setVisibility(View.GONE);
        });

        viewModel.getCurrentCalendarMonth().observe(this, cal -> {
            if (cal != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                binding.currentMonthText.setText(sdf.format(cal.getTime()));
            }
        });

        viewModel.getCalendarDays().observe(this, days -> {
            if (days != null) {
                calendarDaysList.clear();
                calendarDaysList.addAll(days);
                calendarAdapter.notifyDataSetChanged();
            }
        });

        viewModel.getAttendanceHistory().observe(this, history -> {
            if (history != null && !history.isEmpty()) {
                historyList.clear();
                historyList.addAll(history);
                historyAdapter.setScheduleContext(viewModel.getCachedDays(), viewModel.getCachedSchedules());
                binding.historyCountText.setText(history.size() + (history.size() == 1 ? " record" : " records"));
                binding.noHistoryLayout.setVisibility(View.GONE);
                binding.historyRecyclerView.setVisibility(View.VISIBLE);
            } else {
                historyList.clear();
                historyAdapter.setScheduleContext(viewModel.getCachedDays(), viewModel.getCachedSchedules());
                binding.historyCountText.setText("0 records");
                binding.noHistoryLayout.setVisibility(View.VISIBLE);
                binding.historyRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void updateStatsCards() {
        var subj = viewModel.getSubject().getValue();
        int attended = viewModel.getAttendedCount().getValue() != null ? viewModel.getAttendedCount().getValue() : 0;
        int missed = viewModel.getMissedCount().getValue() != null ? viewModel.getMissedCount().getValue() : 0;
        int conducted = viewModel.getConductedCount().getValue() != null ? viewModel.getConductedCount().getValue() : (attended + missed);

        String unit = isLab ? "labs" : "classes";
        binding.attendanceRatioText.setText(attended + " / " + conducted + " " + unit + " attended");

        if (isLab) {
            binding.classAttendedLabel.setText("Lab Subject");
            binding.classAttendedCount.setText(subj != null ? subj.getSubjectName() : "Practical");
            binding.labAttendedLabel.setText("Labs Attended");
            binding.labAttendedCount.setText(attended + " / " + conducted);
            binding.conductedLabel.setText("Conducted Labs");
            binding.conductedCount.setText(conducted + " labs");
        } else {
            binding.classAttendedLabel.setText("Classes Attended");
            binding.classAttendedCount.setText(attended + " / " + conducted);

            binding.labAttendedLabel.setText("Weekly Schedule");
            binding.labAttendedCount.setText((subj != null ? subj.getClassesPerWeek() : 0) + " / week");

            binding.conductedLabel.setText("Conducted Classes");
            binding.conductedCount.setText(conducted + " classes");
        }
    }

    private void updateTodayActionButtons(String status) {
        binding.markPresentBtn.setStrokeWidth(0);
        binding.markAbsentBtn.setStrokeWidth(0);
        binding.markCancelledBtn.setStrokeWidth(0);

        if (AttendanceConstants.isPresent(status)) {
            binding.markPresentBtn.setStrokeWidth(4);
            binding.markPresentBtn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.all_white)));
        } else if (AttendanceConstants.isAbsent(status)) {
            binding.markAbsentBtn.setStrokeWidth(4);
            binding.markAbsentBtn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.all_white)));
        } else if (AttendanceConstants.isCancelled(status)) {
            binding.markCancelledBtn.setStrokeWidth(4);
            binding.markCancelledBtn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.all_white)));
        }
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());

        binding.prevMonthBtn.setOnClickListener(v -> viewModel.prevMonth());
        binding.nextMonthBtn.setOnClickListener(v -> viewModel.nextMonth());

        // Subject Bunk Simulator Controls
        binding.plusAttendBtn.setOnClickListener(v -> {
            int maxLimit = baseRemaining > 0 ? baseRemaining : 99;
            if (simAttend + simBunk < maxLimit) {
                simAttend++;
                binding.simAttendValueText.setText(String.valueOf(simAttend));
                updateSimulationDisplay();
            }
        });

        binding.minusAttendBtn.setOnClickListener(v -> {
            if (simAttend > 0) {
                simAttend--;
                binding.simAttendValueText.setText(String.valueOf(simAttend));
                updateSimulationDisplay();
            }
        });

        binding.plusBunkBtn.setOnClickListener(v -> {
            int maxLimit = baseRemaining > 0 ? baseRemaining : 99;
            if (simAttend + simBunk < maxLimit) {
                simBunk++;
                binding.simBunkValueText.setText(String.valueOf(simBunk));
                updateSimulationDisplay();
            }
        });

        binding.minusBunkBtn.setOnClickListener(v -> {
            if (simBunk > 0) {
                simBunk--;
                binding.simBunkValueText.setText(String.valueOf(simBunk));
                updateSimulationDisplay();
            }
        });

        binding.resetSimBtn.setOnClickListener(v -> {
            simAttend = 0;
            simBunk = 0;
            binding.simAttendValueText.setText("0");
            binding.simBunkValueText.setText("0");
            updateSimulationDisplay();
        });
    }

    private void updateSimulationDisplay() {
        int maxLimit = baseRemaining > 0 ? baseRemaining : 99;
        if (simAttend > maxLimit) {
            simAttend = maxLimit;
            binding.simAttendValueText.setText(String.valueOf(simAttend));
        }
        if (simAttend + simBunk > maxLimit) {
            simBunk = Math.max(0, maxLimit - simAttend);
            binding.simBunkValueText.setText(String.valueOf(simBunk));
        }

        boolean canIncrement = (simAttend + simBunk) < maxLimit;
        binding.plusAttendBtn.setAlpha(canIncrement ? 1.0f : 0.4f);
        binding.plusBunkBtn.setAlpha(canIncrement ? 1.0f : 0.4f);
        binding.minusAttendBtn.setAlpha(simAttend > 0 ? 1.0f : 0.4f);
        binding.minusBunkBtn.setAlpha(simBunk > 0 ? 1.0f : 0.4f);

        int normalTextColor = ContextCompat.getColor(this, R.color.text);
        int dangerColor = ContextCompat.getColor(this, R.color.danger);
        int successColor = ContextCompat.getColor(this, R.color.success);

        String unitSingle = isLab ? "lab" : "class";
        String unitPlural = isLab ? "labs" : "classes";

        if (baseConducted == 0) {
            binding.simProjectedPctText.setText("--%");
            binding.simProjectedPctText.setTextColor(normalTextColor);
            binding.simStatusHeadline.setText("No attendance history to simulate");
            binding.simStatusHeadline.setTextColor(normalTextColor);
            binding.simStatusDeltaText.setText("Mark attendance first to activate projections");
            return;
        }

        double currentRaw = (baseAttended * 100.0) / baseConducted;
        int currentPct = (int) Math.round(currentRaw);

        if (simAttend == 0 && simBunk == 0) {
            binding.simProjectedPctText.setText(currentPct + "%");
            binding.simProjectedPctText.setTextColor(currentPct < baseCriteria ? dangerColor : successColor);
            binding.simStatusHeadline.setText("Current attendance baseline");
            binding.simStatusHeadline.setTextColor(normalTextColor);
            binding.simStatusDeltaText.setText(baseRemaining > 0
                    ? ("Tap + to test upcoming " + unitPlural + " (" + baseRemaining + " left in semester)")
                    : ("Tap + to simulate upcoming " + unitPlural + " or bunks"));
            return;
        }

        int projAttended = baseAttended + simAttend;
        int projConducted = baseConducted + simAttend + simBunk;
        double projRaw = (projAttended * 100.0) / projConducted;
        int projPct = (int) Math.round(projRaw);
        int diff = projPct - currentPct;

        boolean isIncreasing = projRaw > currentRaw + 0.001 || (simAttend > 0 && simBunk == 0 && currentPct < 100);
        boolean isDecreasing = projRaw < currentRaw - 0.001 || (simBunk > 0 && simAttend == 0 && currentPct > 0);

        binding.simProjectedPctText.setText(projPct + "%");

        if (projPct < baseCriteria) {
            binding.simProjectedPctText.setTextColor(dangerColor);
            binding.simStatusHeadline.setTextColor(dangerColor);
            if (isIncreasing) {
                binding.simStatusHeadline.setText("Improves to " + projPct + "% (Still below " + baseCriteria + "% target)");
            } else if (isDecreasing) {
                binding.simStatusHeadline.setText("Drops to " + projPct + "% (Below " + baseCriteria + "% target)");
            } else {
                binding.simStatusHeadline.setText("Stays at " + projPct + "% (Below " + baseCriteria + "% target)");
            }
        } else {
            binding.simProjectedPctText.setTextColor(successColor);
            binding.simStatusHeadline.setTextColor(successColor);
            if (currentPct < baseCriteria) {
                binding.simStatusHeadline.setText("Recovers to " + projPct + "% (Meets " + baseCriteria + "% target)");
            } else if (isIncreasing) {
                binding.simStatusHeadline.setText("Improves to " + projPct + "% (Above " + baseCriteria + "% target)");
            } else if (isDecreasing) {
                binding.simStatusHeadline.setText("Drops to " + projPct + "% (Still above " + baseCriteria + "% target)");
            } else {
                binding.simStatusHeadline.setText("Remains safe at " + projPct + "% (Above " + baseCriteria + "% target)");
            }
        }

        String diffStr = (diff >= 0 ? "+" : "") + diff + "% from baseline (" + currentPct + "% ➔ " + projPct + "%)";
        if (baseRemaining > 0) {
            int leftAfterSim = Math.max(0, baseRemaining - (simAttend + simBunk));
            diffStr += " · " + leftAfterSim + " " + (leftAfterSim == 1 ? unitSingle : unitPlural) + " left in semester";
        }
        binding.simStatusDeltaText.setText(diffStr);

    }

    private boolean validateSemesterDurationForAction(Calendar dateCal, String actionLabel) {
        UserSchema user = viewModel.getCachedUser();
        if (user == null) return true;

        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        long semStart = user.getSemStartDate();
        long semEnd = user.getSemEndDate();

        if (semStart > 0) {
            Calendar semStartCal = Calendar.getInstance();
            semStartCal.setTimeInMillis(semStart);
            semStartCal.set(Calendar.HOUR_OF_DAY, 0);
            semStartCal.set(Calendar.MINUTE, 0);
            semStartCal.set(Calendar.SECOND, 0);
            semStartCal.set(Calendar.MILLISECOND, 0);

            Calendar checkEnd = (Calendar) dateCal.clone();
            checkEnd.set(Calendar.HOUR_OF_DAY, 23);
            checkEnd.set(Calendar.MINUTE, 59);
            checkEnd.set(Calendar.SECOND, 59);
            checkEnd.set(Calendar.MILLISECOND, 999);

            if (checkEnd.before(semStartCal)) {
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showInfo(
                        getSupportFragmentManager(),
                        "Outside Semester Duration",
                        "Cannot " + actionLabel + " before the semester start date (" + df.format(semStartCal.getTime()) + ")."
                );
                return false;
            }
        }

        if (semEnd > 0) {
            Calendar semEndCal = Calendar.getInstance();
            semEndCal.setTimeInMillis(semEnd);
            semEndCal.set(Calendar.HOUR_OF_DAY, 23);
            semEndCal.set(Calendar.MINUTE, 59);
            semEndCal.set(Calendar.SECOND, 59);
            semEndCal.set(Calendar.MILLISECOND, 999);

            Calendar checkStart = (Calendar) dateCal.clone();
            checkStart.set(Calendar.HOUR_OF_DAY, 0);
            checkStart.set(Calendar.MINUTE, 0);
            checkStart.set(Calendar.SECOND, 0);
            checkStart.set(Calendar.MILLISECOND, 0);

            if (checkStart.after(semEndCal)) {
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showInfo(
                        getSupportFragmentManager(),
                        "Outside Semester Duration",
                        "Cannot " + actionLabel + " after the semester end date (" + df.format(semEndCal.getTime()) + ")."
                );
                return false;
            }
        }

        return true;
    }

    private void handleCalendarDayClick(CalendarDayModel day) {
        if (day == null || day.getDayNumber() <= 0) return;

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this, R.style.TransparentBottomSheet);
        DialogCalendarDayInfoBinding dialogBinding = DialogCalendarDayInfoBinding.inflate(getLayoutInflater());
        bottomSheet.setContentView(dialogBinding.getRoot());

        Calendar cellCal = Calendar.getInstance();
        cellCal.setTimeInMillis(day.getDateMs());
        cellCal.set(Calendar.HOUR_OF_DAY, 0);
        cellCal.set(Calendar.MINUTE, 0);
        cellCal.set(Calendar.SECOND, 0);
        cellCal.set(Calendar.MILLISECOND, 0);

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        Calendar todayEndCal = Calendar.getInstance();
        todayEndCal.set(Calendar.HOUR_OF_DAY, 23);
        todayEndCal.set(Calendar.MINUTE, 59);
        todayEndCal.set(Calendar.SECOND, 59);
        todayEndCal.set(Calendar.MILLISECOND, 999);

        boolean isFuture = cellCal.after(todayEndCal);
        boolean isToday = cellCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && cellCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);

        DayScheduleInfo dayInfo = viewModel.getDayScheduleInfoForDate(day.getDateMs());
        String dayName = dayInfo != null ? dayInfo.getDayName() : "";

        // 1. Set Header
        dialogBinding.dialogDateTitle.setText(day.getDateString());
        dialogBinding.dialogDaySubtitle.setText(dayName + (isToday ? " • Today" : ""));
        dialogBinding.closeBtn.setOnClickListener(v -> bottomSheet.dismiss());

        // 2. Set Timetable / Schedule Info
        if (dayInfo != null && dayInfo.isHoliday()) {
            String holName = dayInfo.getHolidayName().isEmpty() ? "Campus Closed" : dayInfo.getHolidayName();
            dialogBinding.scheduleIcon.setImageResource(R.drawable.holiday);
            dialogBinding.scheduleIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.danger));
            dialogBinding.scheduleHeader.setText("College Holiday");
            dialogBinding.scheduleDetail.setText(holName);
        } else if (dayInfo != null && dayInfo.isHasClass()) {
            dialogBinding.scheduleIcon.setImageResource(R.drawable.time_table);
            dialogBinding.scheduleIcon.setImageTintList(ColorStateList.valueOf(ThemeManager.getAccentColorInt()));
            dialogBinding.scheduleHeader.setText("Timetable Schedule");
            dialogBinding.scheduleDetail.setText(dayInfo.getStatusDetail());
        } else if (dayInfo != null && dayInfo.isWeekend()) {
            dialogBinding.scheduleIcon.setImageResource(R.drawable.time_table);
            dialogBinding.scheduleIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.danger));
            dialogBinding.scheduleHeader.setText("Weekend Off-Day");
            dialogBinding.scheduleDetail.setText("No " + (isLab ? "labs" : "classes") + " scheduled");
        } else {
            dialogBinding.scheduleIcon.setImageResource(R.drawable.time_table);
            dialogBinding.scheduleIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.text_dim));
            dialogBinding.scheduleHeader.setText("No " + (isLab ? "Lab" : "Class") + " Scheduled");
            dialogBinding.scheduleDetail.setText("Not scheduled in timetable for " + (dayName.isEmpty() ? "this day" : dayName + "s"));
        }

        // 3. Set Current Attendance Status
        boolean hasExistingRecord = false;
        if (day.getPresentCount() > 0) {
            dialogBinding.statusDotIndicator.setBackgroundResource(R.drawable.dot_shape_present);
            String label = day.getPresentCount() > 1
                    ? "Marked Present (" + day.getPresentCount() + " " + (isLab ? "labs" : "classes") + ")"
                    : "Marked Present";
            dialogBinding.statusDetailText.setText(label);
            dialogBinding.statusDetailText.setTextColor(ContextCompat.getColor(this, R.color.success));
            hasExistingRecord = true;
        } else if (day.getAbsentCount() > 0) {
            dialogBinding.statusDotIndicator.setBackgroundResource(R.drawable.dot_shape_absent);
            String label = day.getAbsentCount() > 1
                    ? "Marked Absent (" + day.getAbsentCount() + " " + (isLab ? "labs" : "classes") + ")"
                    : "Marked Absent";
            dialogBinding.statusDetailText.setText(label);
            dialogBinding.statusDetailText.setTextColor(ContextCompat.getColor(this, R.color.danger));
            hasExistingRecord = true;
        } else if (day.getCancelledCount() > 0) {
            dialogBinding.statusDotIndicator.setBackgroundResource(R.drawable.dot_shape_cancelled);
            String label = day.getCancelledCount() > 1
                    ? "Marked Cancelled (" + day.getCancelledCount() + " " + (isLab ? "labs" : "classes") + ")"
                    : "Marked Cancelled";
            dialogBinding.statusDetailText.setText(label);
            dialogBinding.statusDetailText.setTextColor(ContextCompat.getColor(this, R.color.color_teal));
            hasExistingRecord = true;
        } else {
            dialogBinding.statusDotIndicator.setBackgroundResource(R.drawable.dot_shape);
            dialogBinding.statusDotIndicator.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_dim));
            dialogBinding.statusDetailText.setText("No attendance logged yet");
            dialogBinding.statusDetailText.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
        }

        // 4. Validate if markable or restricted
        UserSchema user = viewModel.getCachedUser();
        long semStart = user != null ? user.getSemStartDate() : 0;
        long semEnd = user != null ? user.getSemEndDate() : 0;
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        boolean isBeforeSem = false;
        boolean isAfterSem = false;

        if (semStart > 0) {
            Calendar semStartCal = Calendar.getInstance();
            semStartCal.setTimeInMillis(semStart);
            semStartCal.set(Calendar.HOUR_OF_DAY, 0);
            semStartCal.set(Calendar.MINUTE, 0);
            semStartCal.set(Calendar.SECOND, 0);
            semStartCal.set(Calendar.MILLISECOND, 0);

            Calendar checkEnd = (Calendar) cellCal.clone();
            checkEnd.set(Calendar.HOUR_OF_DAY, 23);
            checkEnd.set(Calendar.MINUTE, 59);
            checkEnd.set(Calendar.SECOND, 59);
            checkEnd.set(Calendar.MILLISECOND, 999);
            isBeforeSem = checkEnd.before(semStartCal);
        }

        if (semEnd > 0) {
            Calendar semEndCal = Calendar.getInstance();
            semEndCal.setTimeInMillis(semEnd);
            semEndCal.set(Calendar.HOUR_OF_DAY, 23);
            semEndCal.set(Calendar.MINUTE, 59);
            semEndCal.set(Calendar.SECOND, 59);
            semEndCal.set(Calendar.MILLISECOND, 999);
            isAfterSem = cellCal.after(semEndCal);
        }

        dialogBinding.btnDone.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccentColorInt()));
        dialogBinding.btnDone.setOnClickListener(v -> bottomSheet.dismiss());
        dialogBinding.restrictedNoticeCard.setVisibility(View.VISIBLE);
        if (hasExistingRecord) {
            String status = day.getPresentCount() > 0 ? "Present" : (day.getAbsentCount() > 0 ? "Absent" : "Cancelled");
            dialogBinding.restrictedNoticeText.setText("ERP Record: " + status + " (" + day.getDateString() + ")");
        } else {
            dialogBinding.restrictedNoticeText.setText("No official ERP record logged for " + day.getDateString() + ".");
        }

        bottomSheet.show();
    }

    private void handleDeleteHistoryItem(AttendanceSchema item) {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                getSupportFragmentManager(),
                "Delete Record?",
                "Remove attendance record for " + item.getDate() + "?",
                () -> {
                    viewModel.deleteAttendance(item.getAid());
                    Toast.makeText(this, "Record removed", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showEditSubjectNameDialog() {
        SubjectSchema currentSubject = viewModel.getSubject().getValue();
        String currentName = currentSubject != null ? currentSubject.getSubjectName() : (passedSubjectName != null ? passedSubjectName : "");

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.TransparentBottomSheet);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject_name, null);
        dialog.setContentView(dialogView);

        com.google.android.material.textfield.TextInputEditText nameInput = dialogView.findViewById(R.id.subjectNameInput);
        com.google.android.material.textfield.TextInputLayout inputLayout = dialogView.findViewById(R.id.subjectNameInputLayout);
        View cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        View saveBtn = dialogView.findViewById(R.id.saveBtn);

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

            passedSubjectName = newName;
            viewModel.updateSubjectName(newName);
            Toast.makeText(this, "Subject name updated", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDeleteSubjectConfirmationDialog() {
        SubjectSchema currentSubject = viewModel.getSubject().getValue();
        String currentName = currentSubject != null ? currentSubject.getSubjectName() : (passedSubjectName != null ? passedSubjectName : "this subject");
        if (isLab && !currentName.toLowerCase().contains("lab")) {
            currentName += " Lab";
        }

        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showDeleteConfirm(
                getSupportFragmentManager(),
                "Delete Subject?",
                "Are you sure you want to delete \"" + currentName + "\"?\n\nThis will permanently delete all associated attendance records, assignments, and marks. This action cannot be undone.",
                this::performDeleteSubject
        );
    }

    private void performDeleteSubject() {
        DbHelper.removeSubject(sid, () -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Subject deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

}