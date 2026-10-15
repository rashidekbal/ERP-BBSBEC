package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentHomeBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.CompleteProfileBottomSheet;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.DayAttendanceDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter.AttendanceCalendarAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter.PendingSetupAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.viewmodel.AttendancePageViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.adapter.TodayClassesAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.model.TodayClassModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.viewmodel.HomeViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.profile.ProfileActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.AddSubjectDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.TimeTableActivity;
import com.redcodersgroup.attend_collegeattendancetracker.services.ImportService;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeActivity activity;
    private HomeViewModel homeViewModel;
    private AttendancePageViewModel attendancePageViewModel;
    private final List<TodayClassModel> todayClassesList = new ArrayList<>();
    private TodayClassesAdapter adapter;
    private final List<CalendarDayModel> calendarDaysList = new ArrayList<>();
    private AttendanceCalendarAdapter calendarAdapter;
    private final List<SubjectSchema> pendingSetupSubjectList = new ArrayList<>();
    private PendingSetupAdapter pendingSetupAdapter;
    private int currentUpcomingCount = 0;
    private int currentOverallPct = 0;
    private boolean waitingForStableRender = false;
    private final Runnable stableChartAnimRunnable = () -> {
        if (!isAdded() || binding == null) return;
        waitingForStableRender = false;
        ProgressAnimationUtil.animateProgress(
                binding.progressIndicator, currentOverallPct, binding.overallPercentageText);
    };

    private final ActivityResultLauncher<String[]> importFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && getContext() != null) {
                    showImportConfirmDialog(uri);
                }
            });

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        init();
        setUpRecyclerView();
        observeViewModel();
        setEventListeners();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            waitingForStableRender = true;
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
            binding.progressIndicator.setProgress(0);
            binding.overallPercentageText.setText("0%");
            binding.getRoot().postDelayed(stableChartAnimRunnable, 200);
        }
        if (homeViewModel != null) {
            homeViewModel.loadAllData();
        }
        if (attendancePageViewModel != null) {
            attendancePageViewModel.loadCalendarDays();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
        }
        waitingForStableRender = false;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
        }
        super.onDestroyView();
    }

    private void init() {
        activity = (HomeActivity) requireActivity();
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        attendancePageViewModel = new ViewModelProvider(requireActivity()).get(AttendancePageViewModel.class);

        adapter = new TodayClassesAdapter(requireContext(), todayClassesList, model -> {
            if (model != null && model.getSubject() != null) {
                Intent intent = com.redcodersgroup.attend_collegeattendancetracker.screens.subject.SubjectInfoActivity.getSubjectInfoIntent(
                        requireContext(),
                        model.getSubject().getSid(),
                        model.isLab(),
                        model.getSubject().getSubjectName()
                );
                startActivity(intent);
            }
        });

        pendingSetupAdapter = new PendingSetupAdapter(requireActivity(), pendingSetupSubjectList);

        calendarAdapter = new AttendanceCalendarAdapter(requireActivity(), calendarDaysList);
        calendarAdapter.setOnDayClickListener(this::handleCalendarDayClick);
    }

    private void setUpRecyclerView() {
        binding.homePendingSetupRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.homePendingSetupRecyclerView.setAdapter(pendingSetupAdapter);

        binding.todayClassesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.todayClassesRecyclerView.setAdapter(adapter);

        binding.calendarRecyclerView.setLayoutManager(new GridLayoutManager(requireActivity(), 7));
        binding.calendarRecyclerView.setAdapter(calendarAdapter);
    }

    private void observeViewModel() {
        // Current date in top header
        String todayHeaderStr = new SimpleDateFormat("EEEE, dd MMM", Locale.US).format(new Date());
        binding.currentDay.setText(todayHeaderStr);

        // User info
        homeViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String fullName = user.getName() != null ? user.getName().trim() : "";
                String firstName = !fullName.isEmpty() ? fullName.split(" ")[0] : "Student";
                binding.greetingText.setText(getString(R.string.hey_user, firstName));

                // Initials for profile icon
                String initials = getInitials(fullName);
                binding.profileInitials.setText(initials);

                String colorStr = user.getThemeColor();
                if (colorStr == null || colorStr.isEmpty()) {
                    colorStr = ThemeManager.getAccentColor();
                }
                try {
                    int color = Color.parseColor(colorStr);
                    binding.profileBtn.setCardBackgroundColor(color);
                } catch (Exception ignored) {}

                Integer pct = homeViewModel.getOverallPercentage().getValue();
                updateHeroCardStatus(pct != null ? pct : 0);
            } else {
                binding.greetingText.setText("Hey Student");
                binding.profileInitials.setText("U");
            }
        });

        ThemeManager.getAccentColorLiveData().observe(getViewLifecycleOwner(), this::applyAccentColorToViews);

        // Apply initial accent color immediately
        applyAccentColorToViews(ThemeManager.getAccentColor());

        // Subject count & Empty states handling
        homeViewModel.getSubjectCount().observe(getViewLifecycleOwner(), count -> {
            int subCount = count != null ? count : 0;
            binding.subjectCount.setText(String.valueOf(subCount));
            updateEmptyAndClassesState();
            Integer pct = homeViewModel.getOverallPercentage().getValue();
            updateHeroCardStatus(pct != null ? pct : 0);

        });

        // Today's classes
        homeViewModel.getTodayClasses().observe(getViewLifecycleOwner(), classes -> {
            if (classes != null) {
                todayClassesList.clear();
                todayClassesList.addAll(classes);
                adapter.notifyDataSetChanged();
                updateEmptyAndClassesState();
                updateSubtitle();
            }
        });

        // Upcoming assignments count
        homeViewModel.getUpcomingAssignmentsCount().observe(getViewLifecycleOwner(), count -> {
            currentUpcomingCount = count != null ? count : 0;
            updateSubtitle();
        });

        // Upcoming assignment card
        homeViewModel.getUpcomingAssignment().observe(getViewLifecycleOwner(), assignment -> {
            if (assignment == null) {
                binding.assignmentSection.setVisibility(View.GONE);
            } else {
                binding.assignmentSection.setVisibility(View.VISIBLE);
                bindUpcomingAssignment(assignment);
            }
        });

        // Overall attendance percentage
        homeViewModel.getOverallPercentage().observe(getViewLifecycleOwner(), percentage -> {
            int pct = percentage != null ? percentage : 0;
            currentOverallPct = pct;
            if (waitingForStableRender || !isResumed()) {
                binding.progressIndicator.setProgress(0);
                binding.overallPercentageText.setText("0%");
            } else {
                ProgressAnimationUtil.animateProgress(
                        binding.progressIndicator, pct, binding.overallPercentageText);
            }
            updateHeroCardStatus(pct);
        });

        // Bunk-safe status
        homeViewModel.getBunkStatusText().observe(getViewLifecycleOwner(), statusText -> {
            if (statusText != null) {
                binding.bunkSafeStatusText.setText(statusText);
            }
        });

        // Unified Hero Risk Summary (Remaining semester classes + Max possible %)
        homeViewModel.getHeroRiskSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateHeroCardStatus(summary.getOverallPercentage());
            }
        });

        // Bunk this week
        homeViewModel.getBunkThisWeek().observe(getViewLifecycleOwner(), count -> {
            binding.bunkCount.setText(String.valueOf(count != null ? count : 0));
        });

        // Next holiday
        homeViewModel.getNextHoliday().observe(getViewLifecycleOwner(), holiday -> {
            binding.holiday.setText(holiday != null ? holiday : "None");
        });

        // Today holiday
        homeViewModel.getTodayHolidayEventName().observe(getViewLifecycleOwner(), holiday -> {
            updateEmptyAndClassesState();
            updateSubtitle();
        });

        // Calendar History Observers
        if (attendancePageViewModel != null) {
            attendancePageViewModel.getCurrentCalendarMonth().observe(getViewLifecycleOwner(), cal -> {
                if (cal != null && binding != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    binding.currentMonth.setText(sdf.format(cal.getTime()));
                }
            });

            attendancePageViewModel.getCalendarDays().observe(getViewLifecycleOwner(), days -> {
                if (days != null) {
                    calendarDaysList.clear();
                    calendarDaysList.addAll(days);
                    calendarAdapter.notifyDataSetChanged();
                }
            });

            attendancePageViewModel.getPendingAttendanceSetUpSubjects().observe(getViewLifecycleOwner(), subjectSchemas -> {
                if (binding == null) return;
                binding.homePendingSetupGroup.setVisibility(View.GONE);
                pendingSetupSubjectList.clear();
                pendingSetupAdapter.notifyDataSetChanged();
                updateEmptyAndClassesState();
            });
        }

        homeViewModel.getIsProfileComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (binding != null) {
                binding.profileSetupBannerCard.setVisibility(View.GONE);
            }
        });
    }

    private void updateHeroCardStatus(int pct) {
        if (binding == null || !isAdded()) return;
        try {
            UserSchema currentUser = homeViewModel.getUser().getValue();
            int criteria = (currentUser != null && currentUser.getAttendanceCriteria() > 0)
                    ? currentUser.getAttendanceCriteria() : 75;
            Integer subCountObj = homeViewModel.getSubjectCount().getValue();
            int subCount = subCountObj != null ? subCountObj : 0;

            AttendanceScheduleUtil.HeroRiskSummary summary =
                    homeViewModel.getHeroRiskSummary().getValue();

            if (summary != null) {
                binding.statusChipText.setText(summary.getStatusChipText());
                binding.bunkSafeStatusText.setText(summary.getHeadlineText());
                binding.criteriaDescriptionText.setText(summary.getSubheadlineText());
            }

            int accentColor = ThemeManager.getAccentColorInt();
            int trackColor = Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            int normalText = ContextCompat.getColor(requireContext(), R.color.text);

            if (subCount == 0 || (summary != null && summary.getTotalConducted() == 0)) {
                if (summary == null) {
                    binding.statusChipText.setText("No Subjects");
                }
                binding.statusChip.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface2));
                binding.statusChipText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dim));
                binding.bunkSafeStatusText.setTextColor(normalText);
                binding.overallPercentageText.setTextColor(normalText);
                binding.progressIndicator.setIndicatorColor(accentColor);
                binding.progressIndicator.setTrackColor(trackColor);
            } else if (summary != null && summary.isTargetUnreachable()) {
                int dangerRed = ContextCompat.getColor(requireContext(), R.color.danger);
                int dangerBg = Color.argb(40, Color.red(dangerRed), Color.green(dangerRed), Color.blue(dangerRed));
                binding.statusChip.setCardBackgroundColor(dangerBg);
                binding.statusChipText.setTextColor(dangerRed);
                binding.bunkSafeStatusText.setTextColor(dangerRed);
                binding.overallPercentageText.setTextColor(dangerRed);
                binding.progressIndicator.setIndicatorColor(dangerRed);
                binding.progressIndicator.setTrackColor(Color.argb(35, Color.red(dangerRed), Color.green(dangerRed), Color.blue(dangerRed)));
            } else if (pct >= criteria) {
                if (summary == null) {
                    binding.statusChipText.setText("Safe Margin");
                }
                int successGreen = ContextCompat.getColor(requireContext(), R.color.success);
                int successBg = Color.argb(40, Color.red(successGreen), Color.green(successGreen), Color.blue(successGreen));
                binding.statusChip.setCardBackgroundColor(successBg);
                binding.statusChipText.setTextColor(successGreen);
                binding.bunkSafeStatusText.setTextColor(normalText);
                binding.overallPercentageText.setTextColor(normalText);
                binding.progressIndicator.setIndicatorColor(successGreen);
                binding.progressIndicator.setTrackColor(Color.argb(35, Color.red(successGreen), Color.green(successGreen), Color.blue(successGreen)));
            } else {
                if (summary == null) {
                    binding.statusChipText.setText("Action Needed");
                }
                int warningAmber = Color.parseColor("#D97706");
                int warningBg = Color.argb(40, Color.red(warningAmber), Color.green(warningAmber), Color.blue(warningAmber));
                binding.statusChip.setCardBackgroundColor(warningBg);
                binding.statusChipText.setTextColor(warningAmber);
                binding.bunkSafeStatusText.setTextColor(normalText);
                binding.overallPercentageText.setTextColor(normalText);
                binding.progressIndicator.setIndicatorColor(warningAmber);
                binding.progressIndicator.setTrackColor(Color.argb(35, Color.red(warningAmber), Color.green(warningAmber), Color.blue(warningAmber)));
            }
        } catch (Exception ignored) {}
    }

    private void updateEmptyAndClassesState() {
        Integer subCountObj = homeViewModel.getSubjectCount().getValue();
        int subCount = subCountObj != null ? subCountObj : 0;
        String todayHoliday = homeViewModel.getTodayHolidayEventName().getValue();

        if (todayHoliday != null && !todayHoliday.trim().isEmpty()) {
            binding.noSubjectsEmptyLayout.setVisibility(View.GONE);
            binding.todayClassesEmptyLayout.setVisibility(View.VISIBLE);
            binding.todayClassesRecyclerView.setVisibility(View.GONE);
            binding.legendLayout.setVisibility(View.GONE);
            binding.markAllPresentBtn.setVisibility(View.GONE);
            binding.emptyClassesIcon.setImageResource(R.drawable.ic_empty_holiday);
            binding.emptyClassesTitle.setText("College Holiday");
            binding.emptyClassesSubtitle.setText(todayHoliday + " · College is closed today. Enjoy your day!");
            return;
        }

        binding.emptyClassesIcon.setImageResource(R.drawable.ic_empty_classes);
        binding.emptyClassesTitle.setText(R.string.no_classes_scheduled_today);
        binding.emptyClassesSubtitle.setText(R.string.enjoy_your_free_time_revise_your_notes_o);

        if (subCount == 0 && pendingSetupSubjectList.isEmpty()) {
            binding.noSubjectsEmptyLayout.setVisibility(View.VISIBLE);
            binding.todayClassesEmptyLayout.setVisibility(View.GONE);
            binding.todayClassesRecyclerView.setVisibility(View.GONE);
            binding.legendLayout.setVisibility(View.GONE);
            binding.markAllPresentBtn.setVisibility(View.GONE);
        } else {
            binding.noSubjectsEmptyLayout.setVisibility(View.GONE);
            boolean hasClasses = !todayClassesList.isEmpty();
            binding.todayClassesEmptyLayout.setVisibility(hasClasses ? View.GONE : View.VISIBLE);
            binding.todayClassesRecyclerView.setVisibility(hasClasses ? View.VISIBLE : View.GONE);
            binding.legendLayout.setVisibility(View.GONE);
            binding.markAllPresentBtn.setVisibility(View.GONE);
        }
    }

    private void updateSubtitle() {
        String todayHoliday = homeViewModel.getTodayHolidayEventName().getValue();
        String classText;
        if (todayHoliday != null && !todayHoliday.trim().isEmpty()) {
            classText = "Holiday (" + todayHoliday + ")";
        } else {
            int classCount = todayClassesList.size();
            classText = classCount + (classCount == 1 ? " class today" : " classes today");
        }
        String assignmentText;
        if (currentUpcomingCount == 0) {
            assignmentText = "No upcoming assignments";
        } else if (currentUpcomingCount == 1) {
            assignmentText = "1 upcoming assignment";
        } else {
            assignmentText = currentUpcomingCount + " upcoming assignments";
        }
        binding.subtitleText.setText(classText + " · " + assignmentText);
    }

    private void bindUpcomingAssignment(@NonNull AssignmentSchema assignment) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(assignment.getDueDate());

        binding.assignmentDayText.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
        binding.assignmentMonthText.setText(new SimpleDateFormat("MMM", Locale.US).format(cal.getTime()).toUpperCase(Locale.US));

        binding.taskTitle.setText(assignment.getAssignmentTitle());

        String reminder = assignment.getReminderSchedule();
        String subtitle = assignment.getSubjectName() != null ? assignment.getSubjectName() : "";
        if (reminder != null && !reminder.isEmpty() && !"None".equalsIgnoreCase(reminder)) {
            String reminderText;
            if ("T_0".equalsIgnoreCase(reminder) || "0".equals(reminder)) {
                reminderText = "on due date";
            } else if ("T_1".equalsIgnoreCase(reminder) || "1".equals(reminder)) {
                reminderText = "1 day before";
            } else if ("T_2".equalsIgnoreCase(reminder) || "2".equals(reminder)) {
                reminderText = "2 days before";
            } else {
                reminderText = reminder;
            }
            subtitle += " · Reminder: " + reminderText;
        }
        binding.subjectName.setText(subtitle);

        Calendar todayMidnight = Calendar.getInstance();
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);

        if (assignment.isSubmitted()) {
            binding.assignmentStatusText.setText("Submitted");
            binding.assignmentStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
            binding.assignmentStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface2));
        } else if (assignment.getDueDate() < todayMidnight.getTimeInMillis()) {
            binding.assignmentStatusText.setText("Overdue");
            binding.assignmentStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
            binding.assignmentStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface2));
        } else {
            int accentColor = ThemeManager.getAccentColorInt();
            binding.assignmentStatusText.setText("Pending");
            binding.assignmentStatusText.setTextColor(accentColor);
            binding.assignmentStatusCard.setCardBackgroundColor(Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        }
    }

    private void applyAccentColorToViews(String colorHex) {
        if (colorHex == null || binding == null || !isAdded()) return;
        try {
            int color = Color.parseColor(colorHex);
            ColorStateList colorStateList = ColorStateList.valueOf(color);
            binding.currentDay.setTextColor(color);
            binding.holiday.setTextColor(color);
            binding.markAllPresentBtn.setTextColor(color);
            binding.profileBtn.setCardBackgroundColor(color);
            binding.addSubjectPromptBtn.setBackgroundTintList(colorStateList);
            binding.navigateToTasks.setTextColor(color);
            binding.viewAnalyticsPrompt.setTextColor(color);
            binding.viewAnalyticsArrow.setColorFilter(color);
            binding.fullCalanderBtn.setTextColor(color);
            binding.profileSetupBannerCard.setStrokeColor(color);
            binding.setupActionRequiredText.setTextColor(color);
            binding.btnCompleteProfileBanner.setBackgroundTintList(colorStateList);
            binding.btnImportBackupBanner.setIconTint(colorStateList);
            binding.btnImportBackupEmpty.setIconTint(colorStateList);
            binding.setupStepBadge.setTextColor(color);
            binding.homePendingSetupCountText.setTextColor(color);
            binding.homePendingSetupBadge.setCardBackgroundColor(Color.argb(35, Color.red(color), Color.green(color), Color.blue(color)));
            if (pendingSetupAdapter != null) {
                pendingSetupAdapter.notifyDataSetChanged();
            }
            if (calendarAdapter != null) {
                calendarAdapter.notifyDataSetChanged();
            }
            AssignmentSchema currentAssignment = homeViewModel.getUpcomingAssignment().getValue();
            if (currentAssignment != null) {
                bindUpcomingAssignment(currentAssignment);
            }
            // Tint empty state illustrations
            binding.emptySubjectsIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            binding.emptyClassesIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

            Integer pct = homeViewModel.getOverallPercentage().getValue();
            updateHeroCardStatus(pct != null ? pct : 0);
        } catch (Exception ignored) {}
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        } else {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.US);
        }
    }

    private void setEventListeners() {
        binding.heroAttendanceCard.setOnClickListener(v -> {
            if (activity != null) {
                activity.findViewById(R.id.attendance).performClick();
            }
        });
        binding.sideBarBtn.setOnClickListener(this::handleSideBarClick);
        binding.profileBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ProfileActivity.class));
        });

        binding.profileSetupBannerCard.setOnClickListener(v -> {
            CompleteProfileBottomSheet.show(getChildFragmentManager(), () -> {
                if (homeViewModel != null) {
                    homeViewModel.loadAllData();
                }
            });
        });

        binding.btnCompleteProfileBanner.setOnClickListener(v -> {
            CompleteProfileBottomSheet.show(getChildFragmentManager(), () -> {
                if (homeViewModel != null) {
                    homeViewModel.loadAllData();
                }
            });
        });

        binding.btnImportBackupBanner.setVisibility(View.GONE);
        binding.btnImportBackupEmpty.setVisibility(View.GONE);

        binding.addSubjectPromptBtn.setOnClickListener(v -> {
            ToastUtil.showToastShort("Syncing ERP subjects...");
            com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.syncAll(new com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    ToastUtil.showToastShort("ERP subjects synced successfully");
                    if (homeViewModel != null) homeViewModel.loadAllData();
                }
                @Override
                public void onError(String msg) {
                    ToastUtil.showToastShort("Sync error: " + msg);
                }
            });
        });

        binding.markAllPresentBtn.setVisibility(View.GONE);

        // Navigate to Task fragment
        binding.navigateToTasks.setOnClickListener(v -> {
            if (activity != null) {
                activity.navigateToTask();
            }
        });

        binding.assignmentCard.setOnClickListener(v -> {
            if (activity != null) {
                activity.navigateToTask();
            }
        });

        // Calendar Month Navigation
        binding.prevMonthBtn.setOnClickListener(v -> {
            if (attendancePageViewModel != null) {
                attendancePageViewModel.prevMonth();
            }
        });
        binding.nextMonthBtn.setOnClickListener(v -> {
            if (attendancePageViewModel != null) {
                attendancePageViewModel.nextMonth();
            }
        });
        binding.fullCalanderBtn.setOnClickListener(v -> {
            // Affordance to open Timetable / Calendar
            startActivity(new Intent(requireActivity(), TimeTableActivity.class));
        });
    }

    private void handleCalendarDayClick(CalendarDayModel day) {
        if (day == null || day.getDayNumber() <= 0) return;

        if (day.isFuture()) {
            ToastUtil.showToastShort("Upcoming date · Attendance not available");
            return;
        }

        String dateStr = day.getDateString();
        if ((dateStr == null || dateStr.isEmpty()) && day.getDateMs() > 0) {
            dateStr = DateUtil.getDateString(day.getDateMs());
        }

        DayAttendanceDialogFragment.showDialog(
                getChildFragmentManager(),
                dateStr,
                day.getDateMs(),
                day.isHoliday(),
                day.getHolidayName(),
                day.isWeekend(),
                () -> {
                    if (attendancePageViewModel != null) {
                        attendancePageViewModel.loadCalendarDays();
                    }
                    if (homeViewModel != null) {
                        homeViewModel.loadAllData();
                    }
                }
        );
    }

    private void handleSideBarClick(View view) {
        if (activity != null) {
            activity.openDrawer();
        }
    }

    private void showRestoreOptionsDialog() {
        if (getContext() == null || isStateSaved()) return;
        importFileLauncher.launch(new String[]{"application/json", "*/*"});
    }

    private void showImportConfirmDialog(Uri uri) {
        if (getContext() == null || isStateSaved()) return;
        AppConfirmationDialogFragment.show(
                getChildFragmentManager(),
                "Restore Backup Data?",
                "This will restore your subjects, attendance history, assignments, and settings from the selected backup file.",
                "Restore",
                "Cancel",
                R.drawable.ic_cloud_upload,
                false,
                () -> performImport(uri)
        );
    }

    private void performImport(Uri uri) {
        if (getContext() == null) return;
        ToastUtil.showToastShort("Importing data...");
        ImportService.importData(requireContext(), uri, new ImportService.ImportCallback() {
            @Override
            public void onSuccess() {
                if (getContext() == null || isStateSaved()) return;
                AppConfirmationDialogFragment.showSuccess(
                        getChildFragmentManager(),
                        "Import Successful",
                        "Your backup data has been restored successfully.",
                        "OK",
                        false,
                        () -> {
                            if (getActivity() != null) {
                                Intent intent = new Intent(requireActivity(), HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        }
                );
            }

            @Override
            public void onError(String msg) {
                Log.e("HomeFragment", "Import failed: " + msg);
                if (getContext() == null) return;
                ToastUtil.showToastLong("Import failed: " + msg);
            }
        });
    }
}