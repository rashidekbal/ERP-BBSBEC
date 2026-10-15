package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityAnalyticsBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.adapter.SubjectRiskAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.DayOfWeekAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.OverallAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.SubjectRiskModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.viewmodel.AnalyticsViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.SubjectInfoActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;
    private SubjectRiskAdapter adapter;

    private List<SubjectRiskModel> allRiskItems = new ArrayList<>();
    private boolean showOnlyAtRisk = false;
    private int lastOverallPct = -1;

    // Simulation state
    private int baseAttended = 0;
    private int baseConducted = 0;
    private int baseCriteria = 75;
    private int simAttend = 0;
    private int simBunk = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupObservers();
        setEventListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadAllAnalytics();
        }
        applyAccentColor(ThemeManager.getAccentColor());
    }

    private void initViews() {
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        adapter = new SubjectRiskAdapter(this);
        adapter.setListener(item -> {
            if (item != null && item.getSubject() != null) {
                Intent intent = SubjectInfoActivity.getSubjectInfoIntent(
                        this,
                        item.getSubject().getSid(),
                        item.isLab(),
                        item.getSubject().getSubjectName()
                );
                startActivity(intent);
            }
        });

        binding.subjectRiskRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.subjectRiskRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getOverallAnalytics().observe(this, this::bindOverallAnalytics);
        viewModel.getWeekdayAnalytics().observe(this, this::bindWeekdayAnalytics);
        viewModel.getSubjectRisks().observe(this, this::bindSubjectRisks);
        ThemeManager.getAccentColorLiveData().observe(this, this::applyAccentColor);
    }

    private void bindOverallAnalytics(OverallAnalyticsModel model) {
        if (model == null) return;

        baseAttended = model.getTotalAttended();
        baseConducted = model.getTotalConducted();
        baseCriteria = model.getTargetCriteria();

        // 1. Overall Ring
        int pct = model.getOverallPercentage();
        if (lastOverallPct != pct) {
            lastOverallPct = pct;
            com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil.animateProgress(
                    binding.overallProgressIndicator, pct, binding.overallPercentageText);
        }

        // Status Chip & Headline
        int accentColor = ThemeManager.getAccentColorInt();
        int dangerColor = ContextCompat.getColor(this, R.color.danger);
        int successColor = ContextCompat.getColor(this, R.color.success);
        int normalText = ContextCompat.getColor(this, R.color.text);

        int criteria = model.getTargetCriteria();
        binding.overallStatusChipText.setText(model.getStatusChipText());
        binding.heroBunkHeadline.setText(model.getHeadlineText());
        binding.targetCriteriaNote.setText(model.getSubheadlineText());

        if (model.getTotalConducted() == 0) {
            binding.overallProgressIndicator.setIndicatorColor(accentColor);
            binding.overallPercentageText.setTextColor(normalText);
            binding.overallStatusChip.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface2));
            binding.overallStatusChipText.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
            binding.heroBunkHeadline.setTextColor(normalText);
        } else if (model.isTargetUnreachable()) {
            binding.overallProgressIndicator.setIndicatorColor(dangerColor);
            binding.overallPercentageText.setTextColor(dangerColor);
            binding.overallStatusChip.setCardBackgroundColor(ContextCompat.getColor(this, R.color.danger_dim));
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
            binding.overallStatusChip.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_dim));
            binding.overallStatusChipText.setTextColor(successColor);
            binding.heroBunkHeadline.setTextColor(normalText);
        }

        // Streak
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
            binding.velocityIndicatorText.setTextColor(ContextCompat.getColor(this, R.color.text_dim));
        }

        updateSimulationDisplay();
    }

    private void bindWeekdayAnalytics(List<DayOfWeekAnalyticsModel> list) {
        if (list == null || list.isEmpty()) return;
        binding.weekdayDistributionView.setDays(list);

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
        this.allRiskItems = (list != null) ? list : new ArrayList<>();

        int atRiskCount = 0;
        for (SubjectRiskModel m : allRiskItems) {
            if (m.getRiskCategory() == SubjectRiskModel.RiskCategory.AT_RISK) {
                atRiskCount++;
            }
        }

        binding.filterAtRiskText.setText("At Risk (" + atRiskCount + ")");
        applyRiskFilter();
    }

    private void applyRiskFilter() {
        if (allRiskItems.isEmpty()) {
            binding.emptySubjectsLayout.setVisibility(View.VISIBLE);
            binding.subjectRiskRecyclerView.setVisibility(View.GONE);
            return;
        }

        binding.emptySubjectsLayout.setVisibility(View.GONE);
        binding.subjectRiskRecyclerView.setVisibility(View.VISIBLE);

        List<SubjectRiskModel> filtered = new ArrayList<>();
        for (SubjectRiskModel m : allRiskItems) {
            if (!showOnlyAtRisk || m.getRiskCategory() == SubjectRiskModel.RiskCategory.AT_RISK) {
                filtered.add(m);
            }
        }

        adapter.setItems(filtered);

        int textNormal = ContextCompat.getColor(this, R.color.text);
        int textDim = ContextCompat.getColor(this, R.color.text_dim);
        int surface = ContextCompat.getColor(this, R.color.surface);
        int accent = ThemeManager.getAccentColorInt();

        if (showOnlyAtRisk) {
            binding.filterAllText.setTextColor(textDim);
            binding.filterAllBtn.setStrokeWidth(dpToPx(1));
            binding.filterAtRiskText.setTextColor(accent);
            binding.filterAtRiskBtn.setStrokeWidth(dpToPx(2));
        } else {
            binding.filterAllText.setTextColor(accent);
            binding.filterAllBtn.setStrokeWidth(dpToPx(2));
            binding.filterAtRiskText.setTextColor(textDim);
            binding.filterAtRiskBtn.setStrokeWidth(dpToPx(1));
        }
    }

    private void setEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.refreshBtn.setOnClickListener(v -> {
            if (viewModel != null) viewModel.loadAllAnalytics();
        });

        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.syncImmediately(new com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        if (viewModel != null) {
                            viewModel.loadAllAnalytics();
                        }
                        com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Analytics updated");
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort(message);
                    }
                }
            });
        });

        // Filters
        binding.filterAllBtn.setOnClickListener(v -> {
            showOnlyAtRisk = false;
            applyRiskFilter();
        });

        binding.filterAtRiskBtn.setOnClickListener(v -> {
            showOnlyAtRisk = true;
            applyRiskFilter();
        });

        // Simulator Controls
        binding.plusAttendBtn.setOnClickListener(v -> {
            simAttend++;
            binding.simAttendValueText.setText(String.valueOf(simAttend));
            updateSimulationDisplay();
        });

        binding.minusAttendBtn.setOnClickListener(v -> {
            if (simAttend > 0) {
                simAttend--;
                binding.simAttendValueText.setText(String.valueOf(simAttend));
                updateSimulationDisplay();
            }
        });

        binding.plusBunkBtn.setOnClickListener(v -> {
            simBunk++;
            binding.simBunkValueText.setText(String.valueOf(simBunk));
            updateSimulationDisplay();
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
        if (baseConducted == 0) {
            binding.simProjectedPctText.setText("--%");
            binding.simStatusHeadline.setText("No attendance history to simulate");
            binding.simStatusDeltaText.setText("Mark attendance first to activate projections");
            return;
        }

        int currentPct = (int) Math.round((baseAttended * 100.0) / baseConducted);

        if (simAttend == 0 && simBunk == 0) {
            binding.simProjectedPctText.setText(currentPct + "%");
            binding.simStatusHeadline.setText("Current attendance baseline");
            binding.simStatusDeltaText.setText("Tap + to simulate upcoming attendance or bunks");
            return;
        }

        int projAttended = baseAttended + simAttend;
        int projConducted = baseConducted + simAttend + simBunk;
        int projPct = (int) Math.round((projAttended * 100.0) / projConducted);
        int diff = projPct - currentPct;

        binding.simProjectedPctText.setText(projPct + "%");

        int dangerColor = ContextCompat.getColor(this, R.color.danger);
        int successColor = ContextCompat.getColor(this, R.color.success);

        if (projPct < baseCriteria) {
            binding.simProjectedPctText.setTextColor(dangerColor);
            binding.simStatusHeadline.setText("Drops to " + projPct + "% (Below " + baseCriteria + "% target)");
            binding.simStatusHeadline.setTextColor(dangerColor);
        } else {
            binding.simProjectedPctText.setTextColor(successColor);
            binding.simStatusHeadline.setText("Remains safe at " + projPct + "% (Above " + baseCriteria + "% target)");
            binding.simStatusHeadline.setTextColor(successColor);
        }

        String diffStr = (diff >= 0 ? "+" : "") + diff + "% from baseline (" + currentPct + "% ➔ " + projPct + "%)";
        binding.simStatusDeltaText.setText(diffStr);
    }

    private void applyAccentColor(String colorHex) {
        if (colorHex == null) return;
        try {
            int accent = Color.parseColor(colorHex);
            binding.streakIcon.setColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN);
            binding.streakText.setTextColor(accent);
            OverallAnalyticsModel cur = viewModel != null ? viewModel.getOverallAnalytics().getValue() : null;
            if (cur != null) {
                bindOverallAnalytics(cur);
            } else {
                binding.overallProgressIndicator.setIndicatorColor(accent);
            }
            if (binding.weekdayDistributionView != null) {
                binding.weekdayDistributionView.invalidate();
            }
        } catch (Exception ignored) {}
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
