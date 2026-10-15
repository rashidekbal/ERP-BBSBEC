package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentAttendanceBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.adapter.SubjectRiskAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.DayOfWeekAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.OverallAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.SubjectRiskModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.viewmodel.AnalyticsViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionItem;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppOptionsModalBottomSheet;
import com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.CompleteProfileBottomSheet;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter.AttendanceCalendarAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.adapter.PendingSetupAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.viewmodel.AttendancePageViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.AddSubjectDialogFragment;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.SubjectInfoActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.viewmodel.HomeViewModel;

public class AttendanceFragment extends Fragment {
    private FragmentAttendanceBinding binding;
    private HomeActivity activity;

    private List<SubjectSchema> pendingSetupSubjectList;
    private PendingSetupAdapter pendingSetupAdapter;

    private SubjectRiskAdapter subjectRiskAdapter;

    private AttendancePageViewModel viewModel;
    private AnalyticsViewModel analyticsViewModel;
    private HomeViewModel homeViewModel;
    private UserSchema currentUser;

    private int lastOverallPct = 0;
    private boolean waitingForStableRender = true;
    private final Runnable stableChartAnimRunnable = () -> {
        if (binding == null || !isAdded()) return;
        waitingForStableRender = false;
        ProgressAnimationUtil.animateProgress(
                binding.overallProgressIndicator, Math.max(0, lastOverallPct), binding.overallPercentageText);
        binding.weekdayDistributionView.animateBars();
        if (subjectRiskAdapter != null) {
            subjectRiskAdapter.animateAllVisible(binding.attendanceCardRecyclerView);
        }
    };

    // What-If Simulation state
    private int baseAttended = 0;
    private int baseConducted = 0;
    private int baseCriteria = 75;
    private int baseRemaining = 0;
    private int simAttend = 0;
    private int simBunk = 0;

    public AttendanceFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        init();
        setUpPendingSubjectRecyclerView();
        setUpAttendanceSubjectRecyclerView();
        observePendingSetUpSubjects();
        setupAnalyticsObservers();
        loadUserInfo();
        setEventListeners();
        observeAccentColor();

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
        }
        lastOverallPct = 0;
        binding = null;
    }

    private void init() {
        activity = (HomeActivity) requireActivity();

        // Pending Setups
        pendingSetupSubjectList = new ArrayList<>();
        pendingSetupAdapter = new PendingSetupAdapter(requireActivity(), pendingSetupSubjectList);

        // Subject Risk Adapter
        subjectRiskAdapter = new SubjectRiskAdapter(requireContext());
        subjectRiskAdapter.setListener(item -> {
            if (item != null && item.getSubject() != null) {
                Intent intent = SubjectInfoActivity.getSubjectInfoIntent(
                        requireContext(),
                        item.getSubject().getSid(),
                        item.isLab(),
                        item.getSubject().getSubjectName()
                );
                startActivity(intent);
            }
        });
        subjectRiskAdapter.setLongClickListener(this::showSubjectOptionsDialog);

        viewModel = new ViewModelProvider(requireActivity()).get(AttendancePageViewModel.class);
        analyticsViewModel = new ViewModelProvider(requireActivity()).get(AnalyticsViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Swipe Refresh for ERP Data
        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            ErpSyncManager.syncImmediately(new ErpSyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    if (analyticsViewModel != null) {
                        analyticsViewModel.loadAllAnalytics();
                    }
                    if (homeViewModel != null) {
                        homeViewModel.loadAllData();
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "ERP Attendance updated", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Sync error: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void setUpPendingSubjectRecyclerView() {
        binding.pendingSetupRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false));
        binding.pendingSetupRecyclerView.setAdapter(pendingSetupAdapter);
    }

    private void setUpAttendanceSubjectRecyclerView() {
        binding.attendanceCardRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false));
        binding.attendanceCardRecyclerView.setAdapter(subjectRiskAdapter);
    }

    private void setupAnalyticsObservers() {
        analyticsViewModel.getOverallAnalytics().observe(getViewLifecycleOwner(), this::bindOverallAnalytics);
        analyticsViewModel.getWeekdayAnalytics().observe(getViewLifecycleOwner(), this::bindWeekdayAnalytics);
        analyticsViewModel.getSubjectRisks().observe(getViewLifecycleOwner(), this::bindSubjectRisks);
    }

    private void bindOverallAnalytics(OverallAnalyticsModel model) {
        if (model == null || binding == null || !isAdded()) return;

        this.baseAttended = model.getTotalAttended();
        this.baseConducted = model.getTotalConducted();
        this.baseCriteria = model.getTargetCriteria() > 0 ? model.getTargetCriteria() : 75;
        this.baseRemaining = Math.max(0, model.getTotalRemaining());

        if (simAttend > baseRemaining) {
            simAttend = baseRemaining;
            binding.simAttendValueText.setText(String.valueOf(simAttend));
        }
        if (simAttend + simBunk > baseRemaining) {
            simBunk = Math.max(0, baseRemaining - simAttend);
            binding.simBunkValueText.setText(String.valueOf(simBunk));
        }

        int pct = model.getOverallPercentage();
        int criteria = baseCriteria;
        int accentColor = ThemeManager.getAccentColorInt();
        int dangerColor = ContextCompat.getColor(requireContext(), R.color.danger);
        int successColor = ContextCompat.getColor(requireContext(), R.color.success);

        this.lastOverallPct = pct;
        if (waitingForStableRender || !isResumed()) {
            binding.overallPercentageText.setText("0%");
            binding.overallProgressIndicator.setProgressCompat(0, false);
        } else {
            ProgressAnimationUtil.animateProgress(
                    binding.overallProgressIndicator, pct, binding.overallPercentageText);
        }

        int normalText = ContextCompat.getColor(requireContext(), R.color.text);
        binding.overallStatusChipText.setText(model.getStatusChipText());
        binding.heroBunkHeadline.setText(model.getHeadlineText());
        binding.targetCriteriaNote.setText(model.getSubheadlineText());

        if (model.getTotalConducted() == 0) {
            binding.overallProgressIndicator.setIndicatorColor(accentColor);
            binding.overallPercentageText.setTextColor(normalText);
            binding.overallStatusChip.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface2));
            binding.overallStatusChipText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dim));
            binding.heroBunkHeadline.setTextColor(normalText);
        } else if (model.isTargetUnreachable()) {
            binding.overallProgressIndicator.setIndicatorColor(dangerColor);
            binding.overallPercentageText.setTextColor(dangerColor);
            binding.overallStatusChip.setCardBackgroundColor(Color.argb(35, Color.red(dangerColor), Color.green(dangerColor), Color.blue(dangerColor)));
            binding.overallStatusChipText.setTextColor(dangerColor);
            binding.heroBunkHeadline.setTextColor(dangerColor);
        } else if (pct < criteria) {
            int warningAmber = Color.parseColor("#D97706");
            binding.overallProgressIndicator.setIndicatorColor(warningAmber);
            binding.overallPercentageText.setTextColor(normalText);
            binding.overallStatusChip.setCardBackgroundColor(Color.argb(35, Color.red(warningAmber), Color.green(warningAmber), Color.blue(warningAmber)));
            binding.overallStatusChipText.setTextColor(warningAmber);
            binding.heroBunkHeadline.setTextColor(normalText);
        } else {
            binding.overallProgressIndicator.setIndicatorColor(successColor);
            binding.overallPercentageText.setTextColor(normalText);
            binding.overallStatusChip.setCardBackgroundColor(Color.argb(35, Color.red(successColor), Color.green(successColor), Color.blue(successColor)));
            binding.overallStatusChipText.setTextColor(successColor);
            binding.heroBunkHeadline.setTextColor(normalText);
        }

        // Streak (no emoji, vector bolt icon is in layout)
        if (model.getCurrentStreak() > 0) {
            binding.streakText.setText(model.getCurrentStreak() + "-Day Streak · Best: " + model.getBestStreak() + " Days");
        } else {
            binding.streakText.setText("Best Streak: " + model.getBestStreak() + " Days");
        }

        // 4-Column Grid
        binding.totalAttendedText.setText(String.valueOf(model.getTotalAttended()));
        binding.totalConductedText.setText(String.valueOf(model.getTotalConducted()));
        binding.totalMissedText.setText(String.valueOf(model.getTotalMissed()));
        binding.totalCancelledText.setText(String.valueOf(model.getTotalCancelled()));

        // Past 7 Days & 30 Days
        binding.last7DaysPctText.setText(model.getLast7DaysPct() + "%");
        binding.last30DaysPctText.setText(model.getLast30DaysPct() + "%");

        if ("UP".equalsIgnoreCase(model.getVelocity())) {
            binding.velocityIndicatorText.setText("↗ Improving");
            binding.velocityIndicatorText.setTextColor(successColor);
        } else if ("DOWN".equalsIgnoreCase(model.getVelocity())) {
            binding.velocityIndicatorText.setText("↘ Declining");
            binding.velocityIndicatorText.setTextColor(dangerColor);
        } else {
            binding.velocityIndicatorText.setText("→ Steady");
            binding.velocityIndicatorText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dim));
        }

        updateSemesterSubtitle(pct);
        updateSimulationDisplay();
    }

    private void bindWeekdayAnalytics(List<DayOfWeekAnalyticsModel> list) {
        if (list == null || list.isEmpty() || binding == null || !isAdded()) return;
        binding.weekdayDistributionView.setDays(list);
        if (waitingForStableRender || !isResumed()) {
            binding.weekdayDistributionView.resetToZero();
        } else {
            binding.weekdayDistributionView.animateBars();
        }

        DayOfWeekAnalyticsModel bestDay = null;
        DayOfWeekAnalyticsModel worstDay = null;

        for (DayOfWeekAnalyticsModel m : list) {
            if (m.isBestDay()) bestDay = m;
            if (m.isWorstDay()) worstDay = m;
        }

        if (bestDay != null && bestDay.getConducted() > 0) {
            if (worstDay != null && worstDay.getConducted() > 0 && worstDay.getPercentage() < 75) {
                binding.weekdayInsightText.setText("Best: " + bestDay.getFullDayName() + " (" + bestDay.getPercentage() + "%) · Watch out for " + worstDay.getFullDayName() + " (" + worstDay.getPercentage() + "%)");
            } else {
                binding.weekdayInsightText.setText("Highest attendance rate on " + bestDay.getFullDayName() + " (" + bestDay.getPercentage() + "%)");
            }
        } else {
            binding.weekdayInsightText.setText("Compare attendance consistency across weekdays");
        }
    }

    private void bindSubjectRisks(List<SubjectRiskModel> list) {
        if (binding == null || !isAdded()) return;

        binding.attendanceSubjectListSection.setVisibility(View.VISIBLE);
        if (list == null || list.isEmpty()) {
            binding.attendanceCardRecyclerView.setVisibility(View.GONE);
            binding.emptySubjectsLayout.setVisibility(View.VISIBLE);
            return;
        }

        binding.emptySubjectsLayout.setVisibility(View.GONE);
        binding.attendanceCardRecyclerView.setVisibility(View.VISIBLE);
        if (waitingForStableRender || !isResumed()) {
            subjectRiskAdapter.resetToZero(binding.attendanceCardRecyclerView);
        }
        subjectRiskAdapter.setItems(list);
    }

    private void updateSimulationDisplay() {
        if (binding == null || !isAdded()) return;

        boolean canIncrement = (simAttend + simBunk) < 50;
        binding.plusAttendBtn.setAlpha(canIncrement ? 1.0f : 0.4f);
        binding.plusBunkBtn.setAlpha(canIncrement ? 1.0f : 0.4f);
        binding.minusAttendBtn.setAlpha(simAttend > 0 ? 1.0f : 0.4f);
        binding.minusBunkBtn.setAlpha(simBunk > 0 ? 1.0f : 0.4f);

        int normalTextColor = ContextCompat.getColor(requireContext(), R.color.text);
        int dangerColor = ContextCompat.getColor(requireContext(), R.color.danger);
        int successColor = ContextCompat.getColor(requireContext(), R.color.success);

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
            binding.simStatusDeltaText.setText("Tap + to simulate upcoming attendance or bunks");
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
        binding.simStatusDeltaText.setText(diffStr);
    }

    private void observePendingSetUpSubjects() {
        viewModel.getPendingAttendanceSetUpSubjects().observe(getViewLifecycleOwner(), subjectSchemas -> {
            if (binding != null) {
                binding.setupGroup.setVisibility(View.GONE);
            }
            pendingSetupSubjectList.clear();
            pendingSetupAdapter.notifyDataSetChanged();
        });
    }

    private void loadUserInfo() {
        DbHelper.execute(() -> {
            currentUser = Database.getInstance().getUserDao().getUser();
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (analyticsViewModel != null) {
                        analyticsViewModel.loadAllAnalytics();
                    }
                });
            }
        });
    }

    private void updateSemesterSubtitle(int overallPct) {
        if (currentUser != null && binding != null) {
            String semText = "Semester " + currentUser.getSemester();
            if (currentUser.getSemStartDate() > 0 && currentUser.getSemEndDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                String startMonth = sdf.format(new Date(currentUser.getSemStartDate()));
                String endMonth = sdf.format(new Date(currentUser.getSemEndDate()));
                semText += " · " + startMonth + " – " + endMonth;
            }
            semText += " · " + overallPct + "% overall";
            binding.semesterInfo.setText(semText);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            waitingForStableRender = true;
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
            binding.overallProgressIndicator.setProgressCompat(0, false);
            binding.overallPercentageText.setText("0%");
            binding.weekdayDistributionView.resetToZero();
            if (subjectRiskAdapter != null) {
                subjectRiskAdapter.resetToZero(binding.attendanceCardRecyclerView);
            }
            binding.getRoot().postDelayed(stableChartAnimRunnable, 200);
        }
        if (analyticsViewModel != null) {
            analyticsViewModel.loadAllAnalytics();
        }
    }

    private void setEventListeners() {
        binding.sideBarBtn.setOnClickListener(this::handleSideBarClick);

        // What-If Simulator Steppers (simulate up to 50 classes)
        binding.plusAttendBtn.setOnClickListener(v -> {
            if (simAttend + simBunk < 50) {
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
            if (simAttend + simBunk < 50) {
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

    private void observeAccentColor() {
        ThemeManager.getAccentColorLiveData().observe(getViewLifecycleOwner(), colorHex -> {
            if (colorHex == null || binding == null) return;
            try {
                int color = Color.parseColor(colorHex);
                ColorStateList csl = ColorStateList.valueOf(color);
                binding.emptyAttendanceIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                binding.streakIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                binding.streakText.setTextColor(color);
                if (binding.pendingSetupCountText != null) {
                    binding.pendingSetupCountText.setTextColor(color);
                }
                if (binding.pendingSetupCountBadge != null) {
                    int tintBg = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color));
                    binding.pendingSetupCountBadge.setCardBackgroundColor(tintBg);
                }
                if (pendingSetupAdapter != null) {
                    pendingSetupAdapter.notifyDataSetChanged();
                }
                OverallAnalyticsModel cur = analyticsViewModel != null ? analyticsViewModel.getOverallAnalytics().getValue() : null;
                if (cur != null) {
                    bindOverallAnalytics(cur);
                } else {
                    binding.overallProgressIndicator.setIndicatorColor(color);
                }
                if (binding.weekdayDistributionView != null) {
                    binding.weekdayDistributionView.invalidate();
                }
            } catch (Exception ignored) {}
        });

        String current = ThemeManager.getAccentColor();
        if (current != null && binding != null) {
            try {
                int color = Color.parseColor(current);
                ColorStateList csl = ColorStateList.valueOf(color);
                binding.emptyAttendanceIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                binding.streakIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                binding.streakText.setTextColor(color);
                if (binding.pendingSetupCountText != null) {
                    binding.pendingSetupCountText.setTextColor(color);
                }
                if (binding.pendingSetupCountBadge != null) {
                    int tintBg = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color));
                    binding.pendingSetupCountBadge.setCardBackgroundColor(tintBg);
                }
                OverallAnalyticsModel cur = analyticsViewModel != null ? analyticsViewModel.getOverallAnalytics().getValue() : null;
                if (cur != null) {
                    bindOverallAnalytics(cur);
                } else {
                    binding.overallProgressIndicator.setIndicatorColor(color);
                }
            } catch (Exception ignored) {}
        }
    }

    private void handleSideBarClick(View view) {
        if (activity != null) activity.openDrawer();
    }

    private void showSubjectOptionsDialog(SubjectRiskModel item) {
        if (item == null || item.getSubject() == null) return;
        String finalName = item.getSubject().getSubjectName();

        List<AppOptionItem> options = new ArrayList<>();
        options.add(new AppOptionItem(
                "view", "View Details", "Attendance history and records", R.drawable.greater_than, false
        ));
        options.add(new AppOptionItem(
                "delete", "Delete Subject", "Remove subject and all records", R.drawable.ic_delete, true
        ));

        AppOptionsModalBottomSheet.show(
                getChildFragmentManager(),
                finalName,
                item.isLab() ? "Lab Subject" : "Theory Subject",
                options,
                optionId -> {
                    if ("view".equals(optionId)) {
                        Intent intent = SubjectInfoActivity.getSubjectInfoIntent(
                                requireContext(),
                                item.getSubject().getSid(),
                                item.isLab(),
                                item.getSubject().getSubjectName()
                        );
                        startActivity(intent);
                    } else if ("delete".equals(optionId)) {
                        confirmDeleteSubject(item.getSubject().getSid(), finalName);
                    }
                }
        );
    }

    private void confirmDeleteSubject(int sid, String subjectName) {
        AppConfirmationDialogFragment.showDeleteConfirm(
                getChildFragmentManager(),
                "Delete Subject?",
                "Are you sure you want to delete \"" + subjectName + "\"?\n\nThis will permanently delete all associated attendance records, assignments, and marks. This action cannot be undone.",
                () -> {
                    DbHelper.removeSubject(sid, () -> {
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Subject deleted successfully", Toast.LENGTH_SHORT).show();
                                if (analyticsViewModel != null) {
                                    analyticsViewModel.loadAllAnalytics();
                                }
                            });
                        }
                    });
                }
        );
    }
}
