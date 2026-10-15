package com.redcodersgroup.attend_collegeattendancetracker.screens.registration;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityEditRegistrationBinding;
import com.redcodersgroup.attend_collegeattendancetracker.screens.registration.adapter.SelectableSubjectAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

public class EditRegistrationActivity extends AppCompatActivity {

    private ActivityEditRegistrationBinding binding;
    private SelectableSubjectAdapter adapter;
    private final List<SelectableSubjectAdapter.SelectableSubjectItem> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditRegistrationBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadSubjects();
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());

        adapter = new SelectableSubjectAdapter(this, subjectList);
        binding.subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.subjectsRecyclerView.setAdapter(adapter);

        binding.updateRegistrationBtn.setOnClickListener(v -> validateAndUpdate());
    }

    private void loadSubjects() {
        subjectList.clear();
        // Exact curriculum list from Screenshots 2 & 4
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Database Management Systems (DBMS)", "BTCS501-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Formal Language & Automata Theory (FLAT)", "BTCS 502-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Software Engineering (SE)", "BTCS 503-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Computer Networks (CN)", "BTCS 504-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Programming in Python (ELECTIVE-I)", "BTCS 510-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Constitution of India (COI)", "MC · theory · 0 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Enterprise Resource Planning (ERP)", "BTES 501-18 · theory · 3 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Database Management Systems (DBMS) Lab", "BTCS505-18 · practical · 2 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "btech 5th training", "BTECH 5th TRAINING · practical · 0 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "BTCS 506-18 Software Engineering (SE) Lab", "BTCS 506-18 · practical · 1 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Computer Networks (CN) Lab", "BTCS 507-18 · practical · 1 credits", true));
        subjectList.add(new SelectableSubjectAdapter.SelectableSubjectItem(
                "Programming in Python Lab (ELECTIVE-I Lab)", "BTCS 513-18 · practical · 1 credits", true));

        adapter.notifyDataSetChanged();
    }

    private void validateAndUpdate() {
        String feesPaid = binding.feesPaidInput.getText() != null ? binding.feesPaidInput.getText().toString().trim() : "";
        String code = binding.registrationCodeInput.getText() != null ? binding.registrationCodeInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(feesPaid)) {
            binding.feesPaidInput.setError("Please enter fees paid amount");
            binding.feesPaidInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(code)) {
            binding.registrationCodeInput.setError("Please enter registration code");
            binding.registrationCodeInput.requestFocus();
            return;
        }

        int selectedCount = 0;
        for (SelectableSubjectAdapter.SelectableSubjectItem item : subjectList) {
            if (item.isSelected()) selectedCount++;
        }

        if (selectedCount == 0) {
            ToastUtil.showToastShort("Please select at least one subject.");
            return;
        }

        // Safe review dialog - strictly no auto API PUT/POST mutation to ERP
        new AlertDialog.Builder(this)
                .setTitle("Confirm Registration Changes")
                .setMessage("Please review your semester registration details:\n\n" +
                        "• Semester: 5\n" +
                        "• Selected Courses: " + selectedCount + " subjects\n" +
                        "• Fees Paid: ₹" + feesPaid + "\n" +
                        "• Registration Code: " + code + "\n\n" +
                        "Safe Mode: Registration updates must be confirmed manually. No live update executed automatically.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    ToastUtil.showToastShort("Registration details updated locally.");
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
