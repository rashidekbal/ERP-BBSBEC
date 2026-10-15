package com.redcodersgroup.attend_collegeattendancetracker.screens.addAssignment;

import android.app.DatePickerDialog;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.constants.ScheduleConstants;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.DialogAddAssignmentBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAssignmentDialogFragment extends DialogFragment {

    public static final String TAG = "AddAssignmentDialogFragment";
    private static final String ARG_ASSIGNMENT_ID = "ASSIGNMENT_ID";

    public static AddAssignmentDialogFragment newInstance() {
        return new AddAssignmentDialogFragment();
    }

    public static AddAssignmentDialogFragment newInstance(int assignmentId) {
        AddAssignmentDialogFragment fragment = new AddAssignmentDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ASSIGNMENT_ID, assignmentId);
        fragment.setArguments(args);
        return fragment;
    }

    public static void showDialog(FragmentManager fragmentManager) {
        AddAssignmentDialogFragment fragment = newInstance();
        fragment.show(fragmentManager, TAG);
    }

    public static void showDialog(FragmentManager fragmentManager, int assignmentId) {
        AddAssignmentDialogFragment fragment = newInstance(assignmentId);
        fragment.show(fragmentManager, TAG);
    }

    private DialogAddAssignmentBinding binding;
    private List<SubjectSchema> subjectList;
    private List<String> subjectNameList;
    private ArrayAdapter<String> spinnerAdapter;
    private ExecutorService executorService;

    private int editAssignmentId = -1;
    private AssignmentSchema existingAssignment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAddAssignmentBinding.inflate(inflater, container, false);
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
        executorService = Executors.newSingleThreadExecutor();
        applyAccentColor();

        binding.subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    binding.subjectSpinner.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.spinner_box_bg, null));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (getArguments() != null) {
            editAssignmentId = getArguments().getInt(ARG_ASSIGNMENT_ID, -1);
        }
        if (editAssignmentId != -1) {
            binding.headerTitle.setText("Edit Assignment");
            binding.saveBtn.setText("Update");
            loadExistingAssignment();
        }
    }

    private void setUpSubjectSpinner() {
        executorService.execute(() -> {
            subjectList.clear();
            subjectList.addAll(Database.getInstance().getSubjectDao().getSubjectsSync());
            subjectNameList.clear();
            for (SubjectSchema subjectSchema : subjectList) {
                subjectNameList.add(subjectSchema.getSubjectName());
            }
            subjectNameList.add(0, "None");

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.subjectSpinner.setAdapter(spinnerAdapter);
                    spinnerAdapter.notifyDataSetChanged();
                    if (existingAssignment != null) {
                        selectSubjectInSpinner(existingAssignment.getSubjectName());
                    }
                });
            }
        });
    }

    private void selectSubjectInSpinner(String subjectName) {
        if (subjectName == null) return;
        for (int i = 0; i < subjectNameList.size(); i++) {
            if (subjectNameList.get(i).equals(subjectName)) {
                binding.subjectSpinner.setSelection(i);
                break;
            }
        }
    }

    private void loadExistingAssignment() {
        executorService.execute(() -> {
            existingAssignment = Database.getInstance().getAssignmentDao().getAssignmentById(editAssignmentId);
            if (existingAssignment != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.title.setText(existingAssignment.getAssignmentTitle());
                    if (existingAssignment.getAssignmentDesc() != null) {
                        binding.editTextTextMultiLine.setText(existingAssignment.getAssignmentDesc());
                    }
                    binding.dueDateTextView.setText(DateUtil.getDateString(existingAssignment.getDueDate()));

                    String rem = existingAssignment.getReminderSchedule();
                    if ("T_0".equalsIgnoreCase(rem) || "0".equals(rem)) {
                        binding.reminderChipGroup.check(R.id.t_0);
                    } else if ("T_1".equalsIgnoreCase(rem) || "1".equals(rem)) {
                        binding.reminderChipGroup.check(R.id.t_1);
                    } else {
                        binding.reminderChipGroup.check(R.id.t_2);
                    }
                    selectSubjectInSpinner(existingAssignment.getSubjectName());
                });
            }
        });
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
        for (int i = 0; i < binding.reminderChipGroup.getChildCount(); i++) {
            View child = binding.reminderChipGroup.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                ((com.google.android.material.chip.Chip) child).setChipBackgroundColor(chipCsl);
            }
        }
    }

    private void setEventListeners() {
        binding.closeBtn.setOnClickListener(v -> dismiss());
        binding.cancelBtn.setOnClickListener(v -> dismiss());
        binding.dueDate.setOnClickListener(this::handleDueDateClick);
        binding.saveBtn.setOnClickListener(this::handleSaveBtnClick);
    }

    private void handleDueDateClick(View view) {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view1, selectedYear, selectedMonth, selectedDay) -> {
            binding.dueDateTextView.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
        }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void handleSaveBtnClick(View view) {
        binding.saveBtn.setEnabled(false);
        String title = Objects.requireNonNull(binding.title.getText()).toString().trim();
        String description = binding.editTextTextMultiLine.getText() != null ? binding.editTextTextMultiLine.getText().toString().trim() : "";
        String dueDate = binding.dueDateTextView.getText().toString().trim();
        String subjectName = binding.subjectSpinner.getSelectedItem() != null ? binding.subjectSpinner.getSelectedItem().toString().trim() : "";
        if (!isFormValid(subjectName, title, dueDate)) {
            binding.saveBtn.setEnabled(true);
            return;
        }
        int checkedChipId = binding.reminderChipGroup.getCheckedChipId();
        String reminderSchedule;
        if (checkedChipId == R.id.t_0) {
            reminderSchedule = ScheduleConstants.T_0.toString();
        } else if (checkedChipId == R.id.t_1) {
            reminderSchedule = ScheduleConstants.T_1.toString();
        } else {
            reminderSchedule = ScheduleConstants.T_2.toString();
        }
        int sid = getSid(subjectName);
        if (sid == 0) {
            ToastUtil.showToastShort("Please select the subject");
            binding.saveBtn.setEnabled(true);
            return;
        }
        if (editAssignmentId != -1) {
            boolean submitted = existingAssignment != null && existingAssignment.isSubmitted();
            AssignmentSchema updated = new AssignmentSchema(editAssignmentId, sid, subjectName, title, description, DateUtil.getDateMs(dueDate), reminderSchedule, submitted);
            DbHelper.updateAssignment(updated);
            ToastUtil.showToastShort("Assignment updated successfully");
        } else {
            DbHelper.addAssignment(new AssignmentSchema(sid, subjectName, title, description, DateUtil.getDateMs(dueDate), reminderSchedule, false));
            ToastUtil.showToastShort("Assignment added successfully");
        }
        dismissAllowingStateLoss();
    }

    private int getSid(String subjectName) {
        for (SubjectSchema subjectSchema : subjectList) {
            if (subjectSchema.getSubjectName().equals(subjectName)) {
                return subjectSchema.getSid();
            }
        }
        return 0;
    }

    private boolean isFormValid(String subjectName, String title, String dueDate) {
        if (subjectName.isEmpty() || "None".equalsIgnoreCase(subjectName)) {
            binding.subjectSpinner.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("Please select the subject");
            return false;
        }
        if (title.isEmpty()) {
            binding.title.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            binding.title.setError("Title must not be empty");
            return false;
        }
        if (dueDate.isEmpty() || dueDate.equalsIgnoreCase("Select Due Date")) {
            binding.dueDate.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.danger_surface2_card_bg, null));
            ToastUtil.showToastShort("Please select due date");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}
