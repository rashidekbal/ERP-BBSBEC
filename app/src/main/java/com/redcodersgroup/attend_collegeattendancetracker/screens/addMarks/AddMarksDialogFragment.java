package com.redcodersgroup.attend_collegeattendancetracker.screens.addMarks;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAddMarksBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddMarksDialogFragment extends DialogFragment {

    public static final String TAG = "AddMarksDialogFragment";
    private static final String ARG_SUBJECT_NAME = "subjectName";
    private static final String ARG_MID_NUMBER = "midNumber";
    private static final String ARG_MID_TYPE = "midType";

    public static AddMarksDialogFragment newInstance() {
        return new AddMarksDialogFragment();
    }

    public static AddMarksDialogFragment newInstance(String subjectName, int midNumber) {
        AddMarksDialogFragment fragment = new AddMarksDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_NAME, subjectName);
        args.putInt(ARG_MID_NUMBER, midNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddMarksDialogFragment newInstance(String subjectName, String midType) {
        AddMarksDialogFragment fragment = new AddMarksDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_NAME, subjectName);
        args.putString(ARG_MID_TYPE, midType);
        fragment.setArguments(args);
        return fragment;
    }

    public static void showDialog(FragmentManager fragmentManager) {
        AddMarksDialogFragment fragment = newInstance();
        fragment.show(fragmentManager, TAG);
    }

    public static void showDialog(FragmentManager fragmentManager, String subjectName, int midNumber) {
        AddMarksDialogFragment fragment = newInstance(subjectName, midNumber);
        fragment.show(fragmentManager, TAG);
    }

    private DialogAddMarksBinding binding;
    private List<SubjectSchema> subjectList;
    private List<String> subjectNameList;
    private ArrayAdapter<String> spinnerAdapter;
    private int maxMarks = 24;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAddMarksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setUpSubjectSpinner();
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
        subjectList = new ArrayList<>();
        subjectNameList = new ArrayList<>();
        if (getContext() != null) {
            spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, subjectNameList);
            spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        }
        applyAccentColor();
    }

    private void applyAccentColor() {
        if (getContext() == null) return;
        int accent = ThemeManager.getAccentColorInt();
        binding.saveBtn.setBackgroundColor(accent);
        int surfaceColor = ContextCompat.getColor(requireContext(), R.color.surface);
        ColorStateList chipCsl = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, surfaceColor}
        );
        for (int i = 0; i < binding.midChipGroup.getChildCount(); i++) {
            View child = binding.midChipGroup.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                ((com.google.android.material.chip.Chip) child).setChipBackgroundColor(chipCsl);
            }
        }
    }

    private void setUpSubjectSpinner() {
        DbHelper.execute(() -> {
            UserSchema user = Database.getInstance().getUserDao().getUser();
            if (user != null && user.getMaxMarks() > 0) {
                maxMarks = user.getMaxMarks();
            }

            subjectList.clear();
            List<SubjectSchema> allSubjects = Database.getInstance().getSubjectDao().getSubjectsSync();
            for (SubjectSchema subjectSchema : allSubjects) {
                if (!subjectSchema.isLab()) {
                    subjectList.add(subjectSchema);
                }
            }
            subjectNameList.clear();
            for (SubjectSchema subjectSchema : subjectList) {
                subjectNameList.add(subjectSchema.getSubjectName());
            }
            subjectNameList.add(0, "None");

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.marksInput.setHint("eg. 20 (Max: " + maxMarks + ")");
                    binding.subjectSpinner.setAdapter(spinnerAdapter);
                    spinnerAdapter.notifyDataSetChanged();

                    if (getArguments() != null) {
                        String preSelected = getArguments().getString(ARG_SUBJECT_NAME);
                        if (preSelected != null && !preSelected.isEmpty()) {
                            int idx = subjectNameList.indexOf(preSelected);
                            if (idx != -1) {
                                binding.subjectSpinner.setSelection(idx);
                            }
                        }

                        int midNumber = getArguments().getInt(ARG_MID_NUMBER, 0);
                        String midType = getArguments().getString(ARG_MID_TYPE);
                        if (midNumber == 2 || "mid2".equalsIgnoreCase(midType)) {
                            binding.midChipGroup.check(R.id.mid2);
                        } else if (midNumber == 3 || "mid3".equalsIgnoreCase(midType)) {
                            binding.midChipGroup.check(R.id.mid3);
                        } else if (midNumber == 1 || "mid1".equalsIgnoreCase(midType)) {
                            binding.midChipGroup.check(R.id.mid1);
                        }
                    }

                    updateMarksInputForCurrentSelection();
                });
            }
        });
    }

    private void setEventListeners() {
        binding.closeBtn.setOnClickListener(v -> dismiss());
        binding.cancelBtn.setOnClickListener(v -> dismiss());
        binding.saveBtn.setOnClickListener(this::handleSaveBtnClick);
        binding.clearMarksBtn.setOnClickListener(this::handleClearMarksBtnClick);

        binding.subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMarksInputForCurrentSelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.midChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            updateMarksInputForCurrentSelection();
        });
    }

    private void updateMarksInputForCurrentSelection() {
        if (binding.subjectSpinner.getSelectedItem() == null) return;
        String subjectName = binding.subjectSpinner.getSelectedItem().toString().trim();
        int sid = getSid(subjectName);
        if (sid == 0) {
            binding.marksInput.setText("");
            binding.clearMarksBtnCard.setVisibility(View.GONE);
            return;
        }

        int checkedChipId = binding.midChipGroup.getCheckedChipId();
        int midNumber = (checkedChipId == R.id.mid2) ? 2 : (checkedChipId == R.id.mid3 ? 3 : 1);

        DbHelper.execute(() -> {
            MarksSchema schema = Database.getInstance().getMarksDao().getMarks(sid);
            double score = -1;
            if (schema != null) {
                if (midNumber == 1) score = schema.getMid1();
                else if (midNumber == 2) score = schema.getMid2();
                else if (midNumber == 3) score = schema.getMid3();
            }

            final double finalScore = score;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (finalScore >= 0) {
                        String scoreStr = (finalScore == (long) finalScore) ? String.valueOf((long) finalScore) : String.valueOf(finalScore);
                        binding.marksInput.setText(scoreStr);
                        binding.marksInput.setSelection(scoreStr.length());
                        binding.clearMarksBtnCard.setVisibility(View.VISIBLE);
                        binding.clearMarksBtn.setText("Clear Mid " + midNumber + " Marks");
                    } else {
                        binding.marksInput.setText("");
                        binding.clearMarksBtnCard.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void handleClearMarksBtnClick(View view) {
        if (getContext() == null || binding.subjectSpinner.getSelectedItem() == null) return;
        String subjectName = binding.subjectSpinner.getSelectedItem().toString().trim();
        int sid = getSid(subjectName);
        if (sid == 0) {
            ToastUtil.showToastShort("Please select a subject");
            return;
        }

        int checkedChipId = binding.midChipGroup.getCheckedChipId();
        int midNumber = (checkedChipId == R.id.mid2) ? 2 : (checkedChipId == R.id.mid3 ? 3 : 1);
        String midName = "Mid " + midNumber;

        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getChildFragmentManager(),
                "Clear Marks",
                "Are you sure you want to clear " + midName + " marks for " + subjectName + "?",
                "Clear",
                "Cancel",
                R.drawable.ic_delete,
                true,
                () -> {
                    DbHelper.clearMidMarks(sid, midNumber, () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ToastUtil.showToastShort(midName + " marks cleared for " + subjectName);
                                dismissAllowingStateLoss();
                            });
                        }
                    });
                }
        );
    }

    private void handleSaveBtnClick(View view) {
        String unparsedMarks = Objects.requireNonNull(binding.marksInput.getText()).toString().trim();
        if (unparsedMarks.isBlank()) {
            binding.marksInput.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.marksInput.setError("Marks must not be empty");
            return;
        }
        double marks;
        try {
            marks = Double.parseDouble(unparsedMarks);
        } catch (NumberFormatException e) {
            binding.marksInput.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.marksInput.setError("Invalid marks");
            return;
        }

        if (marks < 0 || marks > maxMarks) {
            binding.marksInput.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.marksInput.setError("Marks must be between 0 and " + maxMarks);
            return;
        }

        int checkedChipId = binding.midChipGroup.getCheckedChipId();
        if (checkedChipId == -1) {
            binding.midChipGroup.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("Please select the mid sem");
            return;
        }
        String marksType;
        if (checkedChipId == R.id.mid1) {
            marksType = "mid1";
        } else if (checkedChipId == R.id.mid2) {
            marksType = "mid2";
        } else {
            marksType = "mid3";
        }

        String subjectName = binding.subjectSpinner.getSelectedItem() != null ? binding.subjectSpinner.getSelectedItem().toString().trim() : "";
        int sid = getSid(subjectName);
        if (sid == 0) {
            ToastUtil.showToastShort("Please select the subject");
            binding.saveBtn.setEnabled(true);
            return;
        }
        saveMarks(sid, subjectName, marks, marksType);
    }

    private void saveMarks(int sid, String subjectName, double marks, String marksType) {
        DbHelper.execute(() -> {
            MarksSchema marksSchema = Database.getInstance().getMarksDao().getMarks(sid);
            if (marksSchema == null) {
                marksSchema = new MarksSchema(sid, subjectName,
                        marksType.equals("mid1") ? marks : -1,
                        marksType.equals("mid2") ? marks : -1,
                        marksType.equals("mid3") ? marks : -1);
            } else {
                if (marksType.equals("mid1")) {
                    marksSchema.setMid1(marks);
                } else if (marksType.equals("mid2")) {
                    marksSchema.setMid2(marks);
                } else {
                    marksSchema.setMid3(marks);
                }
            }

            Database.getInstance().getMarksDao().addMarks(marksSchema);
            if (getActivity() != null) {
                androidx.fragment.app.FragmentActivity act = getActivity();
                act.runOnUiThread(() -> {
                    ToastUtil.showToastShort("Marks saved successfully");
                    dismissAllowingStateLoss();
                });
            }
        });
    }

    private int getSid(String subjectName) {
        for (SubjectSchema subjectSchema : subjectList) {
            if (subjectSchema.getSubjectName().equals(subjectName)) {
                return subjectSchema.getSid();
            }
        }
        return 0;
    }
}
