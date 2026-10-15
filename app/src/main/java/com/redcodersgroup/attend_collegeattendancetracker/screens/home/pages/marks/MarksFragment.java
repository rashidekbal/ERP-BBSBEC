package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentMarksBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.adapter.MarksAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.viewmodel.MarksViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ProgressAnimationUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class MarksFragment extends Fragment {
    private FragmentMarksBinding binding;
    private HomeActivity activity;
    private MarksViewModel marksViewModel;
    private List<MarksSchema> marksSchemaList;
    private MarksAdapter marksAdapter;
    private boolean isCompareMode = false;
    private int maxMarks = 24;
    private int currentTargetP1 = 0;
    private int currentTargetP2 = 0;
    private int currentTargetP3 = 0;
    private boolean waitingForStableRender = false;

    private final Runnable stableChartAnimRunnable = () -> {
        if (!isAdded() || binding == null) return;
        waitingForStableRender = false;
        binding.multiTermGraphView.animateGraph();
        ProgressAnimationUtil.animateProgress(
                binding.term1Progress, currentTargetP1, null);
        ProgressAnimationUtil.animateProgress(
                binding.term2Progress, currentTargetP2, null);
        ProgressAnimationUtil.animateProgress(
                binding.term3Progress, currentTargetP3, null);
    };

    public MarksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMarksBinding.inflate(inflater, container, false);
        init();
        setupRecyclerView();
        observeData();
        observeAccentColor();
        setEventListeners();
        setupModeToggle();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            waitingForStableRender = true;
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
            binding.multiTermGraphView.resetToZero();
            binding.term1Progress.setProgress(0);
            binding.term2Progress.setProgress(0);
            binding.term3Progress.setProgress(0);
            binding.getRoot().postDelayed(stableChartAnimRunnable, 200);
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

    private void setupModeToggle() {
        binding.modeTableBtn.setOnClickListener(v -> setViewMode(false));
        binding.modeCompareBtn.setOnClickListener(v -> setViewMode(true));
        updateModeToggleUI(isCompareMode);
    }

    private void setViewMode(boolean compareMode) {
        this.isCompareMode = compareMode;
        updateModeToggleUI(compareMode);

        if (compareMode) {
            binding.marksTableViewContainer.setVisibility(View.GONE);
            binding.marksCompareViewContainer.setVisibility(View.VISIBLE);
            waitingForStableRender = true;
            binding.getRoot().removeCallbacks(stableChartAnimRunnable);
            binding.multiTermGraphView.resetToZero();
            binding.term1Progress.setProgress(0);
            binding.term2Progress.setProgress(0);
            binding.term3Progress.setProgress(0);
            updateCompareView();
            binding.getRoot().postDelayed(stableChartAnimRunnable, 120);
        } else {
            binding.marksTableViewContainer.setVisibility(View.VISIBLE);
            binding.marksCompareViewContainer.setVisibility(View.GONE);
        }
    }

    private void updateModeToggleUI(boolean compareMode) {
        if (getContext() == null || binding == null) return;
        int surface = ContextCompat.getColor(requireContext(), R.color.surface);
        int border = ContextCompat.getColor(requireContext(), R.color.border);
        int textDim = ContextCompat.getColor(requireContext(), R.color.text_dim);
        int accent = ThemeManager.getAccentColorInt();
        float elevationPx = 2 * getResources().getDisplayMetrics().density;
        int strokePx = (int) (1 * getResources().getDisplayMetrics().density);

        if (compareMode) {
            // Inactive: Table
            binding.modeTableBtn.setCardBackgroundColor(Color.TRANSPARENT);
            binding.modeTableBtn.setCardElevation(0f);
            binding.modeTableBtn.setStrokeWidth(0);
            binding.modeTableText.setTextColor(textDim);
            binding.modeTableText.setTypeface(null, Typeface.NORMAL);

            // Active: Compare
            binding.modeCompareBtn.setCardBackgroundColor(surface);
            binding.modeCompareBtn.setCardElevation(elevationPx);
            binding.modeCompareBtn.setStrokeWidth(strokePx);
            binding.modeCompareBtn.setStrokeColor(border);
            binding.modeCompareText.setTextColor(accent);
            binding.modeCompareText.setTypeface(null, Typeface.BOLD);
        } else {
            // Active: Table
            binding.modeTableBtn.setCardBackgroundColor(surface);
            binding.modeTableBtn.setCardElevation(elevationPx);
            binding.modeTableBtn.setStrokeWidth(strokePx);
            binding.modeTableBtn.setStrokeColor(border);
            binding.modeTableText.setTextColor(accent);
            binding.modeTableText.setTypeface(null, Typeface.BOLD);

            // Inactive: Compare
            binding.modeCompareBtn.setCardBackgroundColor(Color.TRANSPARENT);
            binding.modeCompareBtn.setCardElevation(0f);
            binding.modeCompareBtn.setStrokeWidth(0);
            binding.modeCompareText.setTextColor(textDim);
            binding.modeCompareText.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void updateCompareSpinner() {
        if (getContext() == null || binding == null) return;
        List<String> subjectNames = new ArrayList<>();
        for (MarksSchema m : marksSchemaList) {
            if (m.getSubjectName() != null && !m.getSubjectName().isEmpty()) {
                subjectNames.add(m.getSubjectName());
            }
        }
        if (subjectNames.isEmpty()) {
            subjectNames.add("No Subjects Available");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_spinner_selected,
                subjectNames
        );
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.compareSubjectSpinner.setAdapter(spinnerAdapter);

        binding.compareSubjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < marksSchemaList.size()) {
                    displaySubjectComparison(marksSchemaList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!marksSchemaList.isEmpty()) {
            displaySubjectComparison(marksSchemaList.get(0));
        }
    }

    private void displaySubjectComparison(MarksSchema marks) {
        if (binding == null || marks == null) return;

        binding.compareSubjectTitle.setText(marks.getSubjectName());

        double m1 = marks.getMid1();
        double m2 = marks.getMid2();
        double m3 = marks.getMid3();
        int max = maxMarks > 0 ? maxMarks : 24;

        float p1 = m1 >= 0 ? (float) (m1 * 100.0 / max) : 0f;
        float p2 = m2 >= 0 ? (float) (m2 * 100.0 / max) : 0f;
        float p3 = m3 >= 0 ? (float) (m3 * 100.0 / max) : 0f;

        String l1 = m1 >= 0 ? formatScore(m1) + "/" + max : "—";
        String l2 = m2 >= 0 ? formatScore(m2) + "/" + max : "—";
        String l3 = m3 >= 0 ? formatScore(m3) + "/" + max : "—";

        currentTargetP1 = (int) p1;
        currentTargetP2 = (int) p2;
        currentTargetP3 = (int) p3;

        binding.multiTermGraphView.setTermScores(p1, p2, p3, l1, l2, l3);
        binding.term1ScoreText.setText(m1 >= 0 ? (formatScore(m1) + "/" + max + " (" + (int) p1 + "%)") : "—");
        binding.term2ScoreText.setText(m2 >= 0 ? (formatScore(m2) + "/" + max + " (" + (int) p2 + "%)") : "—");
        binding.term3ScoreText.setText(m3 >= 0 ? (formatScore(m3) + "/" + max + " (" + (int) p3 + "%)") : "—");

        if (waitingForStableRender || !isResumed()) {
            binding.multiTermGraphView.resetToZero();
            binding.term1Progress.setProgress(0);
            binding.term2Progress.setProgress(0);
            binding.term3Progress.setProgress(0);
        } else {
            binding.multiTermGraphView.animateGraph();
            ProgressAnimationUtil.animateProgress(
                    binding.term1Progress, currentTargetP1, null);
            ProgressAnimationUtil.animateProgress(
                    binding.term2Progress, currentTargetP2, null);
            ProgressAnimationUtil.animateProgress(
                    binding.term3Progress, currentTargetP3, null);
        }

        // Calculate growth percentage
        if (m1 >= 0 && m3 >= 0 && m1 > 0) {
            double growth = ((m3 - m1) / m1) * 100.0;
            if (growth >= 0) {
                binding.growthBadgeText.setText(String.format("+%.1f%% Growth", growth));
                binding.growthBadgeText.setTextColor(Color.parseColor("#22C55E"));
                binding.compareInsightText.setText(String.format("Consistent upward momentum: %.1f%% overall growth from Mid Sem 1 to Mid Sem 3 in %s.", growth, marks.getSubjectName()));
            } else {
                binding.growthBadgeText.setText(String.format("%.1f%% Drop", growth));
                binding.growthBadgeText.setTextColor(Color.parseColor("#F5654A"));
                binding.compareInsightText.setText(String.format("Performance decreased by %.1f%% across terms in %s. Recommended to review core topics.", Math.abs(growth), marks.getSubjectName()));
            }
        } else if (m1 >= 0 && m2 >= 0 && m1 > 0) {
            double growth = ((m2 - m1) / m1) * 100.0;
            binding.growthBadgeText.setText(String.format(growth >= 0 ? "+%.1f%% Growth" : "%.1f%% Drop", growth));
            binding.growthBadgeText.setTextColor(growth >= 0 ? Color.parseColor("#22C55E") : Color.parseColor("#F5654A"));
            binding.compareInsightText.setText(String.format("Term 1 to Term 2 progress logged for %s.", marks.getSubjectName()));
        } else {
            binding.growthBadgeText.setText("Steady");
            binding.growthBadgeText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dim));
            binding.compareInsightText.setText("Log scores for all 3 Mid Sem terms to visualize comprehensive growth analytics.");
        }
    }

    private String formatScore(double val) {
        if (val == (long) val) return String.format("%d", (long) val);
        return String.format("%.1f", val);
    }

    private void updateCompareView() {
        if (!marksSchemaList.isEmpty()) {
            int selectedPos = binding.compareSubjectSpinner.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < marksSchemaList.size()) {
                displaySubjectComparison(marksSchemaList.get(selectedPos));
            } else {
                displaySubjectComparison(marksSchemaList.get(0));
            }
        }
    }

    private void observeAccentColor() {
        applyAccentColor(ThemeManager.getAccentColor());
        ThemeManager.getAccentColorLiveData().observe(getViewLifecycleOwner(), this::applyAccentColor);
    }

    private void applyAccentColor(String colorHex) {
        if (colorHex == null || binding == null) return;
        try {
            int color = Color.parseColor(colorHex);
            binding.addSubjectBtn.setTextColor(color);
            binding.emptyMarksIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            updateModeToggleUI(isCompareMode);
        } catch (Exception ignored) {}
    }

    private void observeData() {
        marksViewModel.getMarks().observe(getViewLifecycleOwner(), marksList -> {
            if (marksList != null) {
                marksSchemaList.clear();
                marksSchemaList.addAll(marksList);
                marksAdapter.notifyDataSetChanged();

                boolean isEmpty = marksList.isEmpty();
                binding.marksTableHeader.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.marksRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.emptyMarksLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                updateCompareSpinner();
            }
        });

        marksViewModel.getMarksSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                binding.mid1Avg.setText(summary.getMid1Avg());
                binding.mid2Avg.setText(summary.getMid2Avg());
                binding.mid3Avg.setText(summary.getMid3Avg());
                binding.marksTrend.setText(summary.getTrendText());
                this.maxMarks = summary.getMaxMarks();
                marksAdapter.setMaxMarks(summary.getMaxMarks());
            } else {
                binding.mid1Avg.setText("—");
                binding.mid2Avg.setText("—");
                binding.mid3Avg.setText("—");
                binding.marksTrend.setText("Add marks across your subjects to generate insights and trends.");
            }
        });
    }

    private void setupRecyclerView() {
        binding.marksRecyclerView.setAdapter(marksAdapter);
        binding.marksRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
    }

    private void setEventListeners() {
        binding.sideBarBtn.setOnClickListener(this::handleSideBarCLick);
        binding.addSubjectBtn.setVisibility(View.GONE);
    }

    private void handleSideBarCLick(View view) {
        if (activity != null) activity.openDrawer();
    }

    private void init() {
        activity = (HomeActivity) requireActivity();
        marksViewModel = new ViewModelProvider(this).get(MarksViewModel.class);
        marksSchemaList = new ArrayList<>();
        marksAdapter = new MarksAdapter(activity, marksSchemaList);
    }
}