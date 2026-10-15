package com.redcodersgroup.attend_collegeattendancetracker.screens.subject;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAddSubjectBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddSubjectDialogFragment extends DialogFragment {

    public static final String TAG = "AddSubjectDialogFragment";

    public static AddSubjectDialogFragment newInstance() {
        return new AddSubjectDialogFragment();
    }

    public static void showDialog(FragmentManager fragmentManager) {
        AddSubjectDialogFragment fragment = newInstance();
        fragment.show(fragmentManager, TAG);
    }

    private DialogAddSubjectBinding binding;
    private String selectedColorTag;
    private ExecutorService executorService;

    private final String[] SUBJECT_COLORS = new String[]{
            "#F5A623",
            "#22C55E",
            "#F5654A",
            "#3B82F6",
            "#8B5CF6",
            "#EC4899",
            "#06B6D4",
            "#FB923C"
    };

    private com.google.android.material.card.MaterialCardView[] colorCards;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAddSubjectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil.isProfileCompletedBareMinimum()) {
            com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Please complete profile basics before adding subjects");
            dismissAllowingStateLoss();
            com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.CompleteProfileBottomSheet.show(getParentFragmentManager(), () -> {
                AddSubjectDialogFragment.showDialog(getParentFragmentManager());
            });
            return;
        }
        init();
        setEventListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void init() {
        executorService = Executors.newSingleThreadExecutor();
        selectedColorTag = "#F5A623";

        colorCards = new com.google.android.material.card.MaterialCardView[]{
                binding.colorCard1,
                binding.colorCard2,
                binding.colorCard3,
                binding.colorCard4,
                binding.colorCard5,
                binding.colorCard6,
                binding.colorCard7,
                binding.colorCard8
        };

        selectColor(0);
        applyAccentColor();
    }

    private void applyAccentColor() {
        if (getContext() == null) return;
        int accent = ThemeManager.getAccentColorInt();
        binding.saveBtn.setBackgroundColor(accent);
        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.text_dim);
        ColorStateList checkCsl = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, surfaceColor}
        );
        binding.hasLabCheckBox.setButtonTintList(checkCsl);
    }

    private void setEventListeners() {
        binding.closeBtn.setOnClickListener(v -> dismiss());
        binding.cancelBtn.setOnClickListener(v -> dismiss());

        binding.hasLabCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.labGroup.setVisibility(View.VISIBLE);
            } else {
                binding.labGroup.setVisibility(View.GONE);
            }
        });

        binding.colorCard1View.setOnClickListener(v -> selectColor(0));
        binding.colorCard2View.setOnClickListener(v -> selectColor(1));
        binding.colorCard3View.setOnClickListener(v -> selectColor(2));
        binding.colorCard4View.setOnClickListener(v -> selectColor(3));
        binding.colorCard5View.setOnClickListener(v -> selectColor(4));
        binding.colorCard6View.setOnClickListener(v -> selectColor(5));
        binding.colorCard7View.setOnClickListener(v -> selectColor(6));
        binding.colorCard8View.setOnClickListener(v -> selectColor(7));

        binding.saveBtn.setOnClickListener(this::handleSaveSubject);
    }

    private void selectColor(int index) {
        if (index < 0 || index >= SUBJECT_COLORS.length || getContext() == null) return;
        selectedColorTag = SUBJECT_COLORS[index];

        int activeStrokeColor = getResources().getColor(R.color.text, null);
        int inactiveStrokeColor = getResources().getColor(R.color.border, null);
        int activeWidth = (int) (3 * getResources().getDisplayMetrics().density);
        int inactiveWidth = (int) (1 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < colorCards.length; i++) {
            if (i == index) {
                colorCards[i].setStrokeColor(activeStrokeColor);
                colorCards[i].setStrokeWidth(activeWidth);
            } else {
                colorCards[i].setStrokeColor(inactiveStrokeColor);
                colorCards[i].setStrokeWidth(inactiveWidth);
            }
        }
    }

    private void handleSaveSubject(View view) {
        String subjectName = Objects.requireNonNull(binding.subjectName.getText()).toString().trim();
        if (subjectName.isEmpty()) {
            binding.subjectName.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.subjectName.setError("Enter subject name");
            return;
        }
        String classesPerWeekString = (Objects.requireNonNull(binding.cpWeek.getText()).toString().trim());
        if (classesPerWeekString.isBlank()) {
            binding.cpWeek.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.cpWeek.setError("Enter classes per week");
            return;
        }
        int classesPerWeek;
        try {
            classesPerWeek = Integer.parseInt(classesPerWeekString);
        } catch (NumberFormatException e) {
            binding.cpWeek.setError("Invalid number");
            return;
        }
        if (classesPerWeek <= 0) {
            binding.cpWeek.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.cpWeek.setError("number of classes per week must at least be 1");
            return;
        }
        boolean hasLab = binding.hasLabCheckBox.isChecked();
        int labsPerWeek;
        if (hasLab) {
            String labsperWeekString = Objects.requireNonNull(binding.lPweek.getText()).toString().trim();
            if (labsperWeekString.isBlank()) {
                binding.lPweek.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
                binding.lPweek.setError("Enter labs per week");
                return;
            }
            try {
                labsPerWeek = Integer.parseInt(labsperWeekString);
            } catch (NumberFormatException e) {
                binding.lPweek.setError("Invalid number");
                return;
            }
            if (labsPerWeek <= 0) {
                binding.lPweek.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
                binding.lPweek.setError("number of labs per week must at least be 1");
                return;
            }
        } else {
            labsPerWeek = 0;
        }
        int selectedAttendanceThresholdChipId = binding.attendanceThreshold.getCheckedChipId();
        int attendanceThreshold = selectedAttendanceThresholdChipId == R.id.chip_65 ? 65 : selectedAttendanceThresholdChipId == R.id.chip_75 ? 75 : 80;

        binding.saveBtn.setEnabled(false);
        executorService.execute(() -> {
            SubjectDao subjectDao = Database.getInstance().getSubjectDao();
            // Add theory subject (isLab = false)
            subjectDao.addSubject(new SubjectSchema(subjectName, false, classesPerWeek, 0, attendanceThreshold, selectedColorTag, false, false));
            SubjectSchema s = subjectDao.getSubjectByName(subjectName);
            if (s != null) {
                DbHelper.addMarks(new MarksSchema(s.getSid(), s.getSubjectName(), -1, -1, -1));
            }

            // If hasLab is checked, create separate Lab entity (isLab = true, no marks)
            if (hasLab) {
                String labName = subjectName.toLowerCase().endsWith("lab") ? subjectName : subjectName + " Lab";
                subjectDao.addSubject(new SubjectSchema(labName, true, labsPerWeek, labsPerWeek, attendanceThreshold, selectedColorTag, false, false));
            }

            if (getContext() != null) {
                com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider.refreshAllWidgets(requireContext());
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ToastUtil.showToastShort(hasLab ? "Subject and Lab added successfully" : "Subject added successfully");
                    dismissAllowingStateLoss();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}
