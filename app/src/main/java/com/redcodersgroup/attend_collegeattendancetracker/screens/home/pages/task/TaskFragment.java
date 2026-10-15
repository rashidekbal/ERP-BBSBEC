package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentTaskBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task.adapter.TaskAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.task.viewModel.TaskViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskFragment extends Fragment {
    private FragmentTaskBinding binding;
    private HomeActivity activity;
    private List<AssignmentSchema> assignmentSchemaList;
    private TaskAdapter adapter;
    private TaskViewModel taskViewModel;

    public TaskFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        init();
        setUpRecyclerView();
        observeAssignment();
        observeAccentColor();
        setEventListeners();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void observeAccentColor() {
        applyAccentColor(ThemeManager.getAccentColor());
        ThemeManager.getAccentColorLiveData().observe(getViewLifecycleOwner(), this::applyAccentColor);
    }

    private void applyAccentColor(String colorHex) {
        if (colorHex == null || binding == null) return;
        try {
            int color = Color.parseColor(colorHex);
            binding.btnCreateTask.setBackgroundTintList(ColorStateList.valueOf(color));
            binding.emptyTasksIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        } catch (Exception ignored) {}
    }

    private void observeAssignment() {
        taskViewModel.getAssignments().observe(getViewLifecycleOwner(), assignmentSchemas -> {
            if (assignmentSchemas != null) {
                assignmentSchemaList.clear();
                assignmentSchemaList.addAll(assignmentSchemas);
                adapter.notifyDataSetChanged();

                boolean isEmpty = assignmentSchemas.isEmpty();
                binding.taskRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.emptyTaskLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                updateSubtitle(assignmentSchemas);
            }
        });
    }

    private void updateSubtitle(List<AssignmentSchema> list) {
        if (list == null || list.isEmpty()) {
            binding.taskSubtitle.setText("No pending tasks");
            return;
        }

        Calendar todayMidnight = Calendar.getInstance();
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);
        long today = todayMidnight.getTimeInMillis();

        int overdue = 0;
        int pending = 0;

        for (AssignmentSchema item : list) {
            if (item.isSubmitted()) {
                continue;
            }
            if (item.getDueDate() < today) {
                overdue++;
            } else {
                pending++;
            }
        }

        if (overdue > 0 && pending > 0) {
            binding.taskSubtitle.setText(overdue + " overdue · " + pending + " pending");
        } else if (overdue > 0) {
            binding.taskSubtitle.setText(overdue + " overdue tasks");
        } else if (pending > 0) {
            binding.taskSubtitle.setText(pending + " pending tasks");
        } else {
            binding.taskSubtitle.setText("All tasks completed 🎉");
        }
    }

    private void setEventListeners() {
        binding.addTaskBtn.setOnClickListener(v -> com.redcodersgroup.attend_collegeattendancetracker.screens.addAssignment.AddAssignmentDialogFragment.showDialog(getChildFragmentManager()));
        binding.btnCreateTask.setOnClickListener(v -> com.redcodersgroup.attend_collegeattendancetracker.screens.addAssignment.AddAssignmentDialogFragment.showDialog(getChildFragmentManager()));
    }

    private void setUpRecyclerView() {
        binding.taskRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.taskRecyclerView.setAdapter(adapter);
    }

    private void init() {
        activity = (HomeActivity) requireActivity();
        assignmentSchemaList = new ArrayList<>();
        adapter = new TaskAdapter(requireContext(), assignmentSchemaList);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
    }
}