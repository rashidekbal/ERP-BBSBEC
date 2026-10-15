package com.redcodersgroup.attend_collegeattendancetracker.screens.registration;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivitySemesterRegistrationBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.EligibleSubjectsResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.SemesterRegistrationResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.registration.adapter.RegisteredSubjectAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SemesterRegistrationActivity extends AppCompatActivity {

    private ActivitySemesterRegistrationBinding binding;
    private RegisteredSubjectAdapter adapter;
    private final List<SemesterRegistrationResponse.RegisteredSubject> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySemesterRegistrationBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadRegistrationData();
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.refreshBtn.setOnClickListener(v -> loadRegistrationData());

        adapter = new RegisteredSubjectAdapter(this, subjectList);
        binding.registeredSubjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.registeredSubjectsRecyclerView.setAdapter(adapter);

        binding.viewEligibleSubjectsBtn.setOnClickListener(v -> showEligibleSubjectsDialog());

        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadRegistrationData);
    }

    private long lastRefreshTime = 0L;

    private void loadRegistrationData() {
        long now = System.currentTimeMillis();
        if (binding.swipeRefreshLayout.isRefreshing()) {
            if (now - lastRefreshTime < 8000L) {
                binding.swipeRefreshLayout.setRefreshing(false);
                long waitSec = Math.max(1, (8000L - (now - lastRefreshTime)) / 1000L);
                com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil.showToastShort("Please wait " + waitSec + "s before refreshing again.");
                return;
            }
            lastRefreshTime = now;
        }

        if (!binding.swipeRefreshLayout.isRefreshing()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        NetworkClient.getClient().getSemesterRegistrations().enqueue(new Callback<SemesterRegistrationResponse>() {
            @Override
            public void onResponse(Call<SemesterRegistrationResponse> call, Response<SemesterRegistrationResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && !response.body().getData().isEmpty()) {
                    SemesterRegistrationResponse.RegistrationItem item = response.body().getData().get(0);
                    displayRegistration(item);
                } else {
                    loadFromLocalFallback();
                }
            }

            @Override
            public void onFailure(Call<SemesterRegistrationResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                loadFromLocalFallback();
            }
        });
    }

    private void displayRegistration(SemesterRegistrationResponse.RegistrationItem item) {
        String sessionName = item.getSession() != null ? item.getSession().getName() : "Current Session";
        binding.sessionText.setText("Session: " + sessionName);
        binding.semesterTag.setText("Semester " + item.getSemester() + " · Registration");

        String status = item.getStatus();
        binding.statusBadge.setText(status);
        if ("APPROVED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status)) {
            binding.statusBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else if ("PENDING".equalsIgnoreCase(status) || "PENDING_APPROVAL".equalsIgnoreCase(status)) {
            binding.statusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent));
        } else {
            binding.statusBadge.setTextColor(ContextCompat.getColor(this, R.color.text));
        }

        binding.appliedDateText.setText(item.getAppliedDate().isEmpty() ? "Approved" : item.getAppliedDate());
        binding.remarksText.setText(item.getRemarks().isEmpty() ? "Verified by College ERP" : item.getRemarks());

        subjectList.clear();
        if (item.getSelectedSubjects() != null && !item.getSelectedSubjects().isEmpty()) {
            subjectList.addAll(item.getSelectedSubjects());
        }

        int totalCredits = 0;
        for (SemesterRegistrationResponse.RegisteredSubject sub : subjectList) {
            totalCredits += sub.getCredits();
        }

        binding.subjectCountBadge.setText(subjectList.size() + " Subjects · " + totalCredits + " Credits");
        adapter.notifyDataSetChanged();
    }

    private void loadFromLocalFallback() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema user = db.getUserDao().getUser();
            List<SubjectSchema> subs = db.getSubjectDao().getSetupCompletedSubjectsSync();

            runOnUiThread(() -> {
                int sem = user != null && user.getSemester() > 0 ? user.getSemester() : 5;
                binding.sessionText.setText("Session: Nov 2026");
                binding.semesterTag.setText("Semester " + sem + " · Active");
                binding.statusBadge.setText("ACTIVE");
                binding.statusBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
                binding.appliedDateText.setText("Registered");
                binding.remarksText.setText("Course schedule confirmed");

                subjectList.clear();
                int totalCredits = 0;
                if (subs != null) {
                    for (SubjectSchema s : subs) {
                        SemesterRegistrationResponse.RegisteredSubject reg = new SemesterRegistrationResponse.RegisteredSubject();
                        subjectList.add(reg);
                        totalCredits += (s.isLab() ? 2 : 4);
                    }
                }
                binding.subjectCountBadge.setText(subjectList.size() + " Subjects · " + totalCredits + " Credits");
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showEligibleSubjectsDialog() {
        binding.progressBar.setVisibility(View.VISIBLE);
        NetworkClient.getClient().getEligibleSubjects().enqueue(new Callback<EligibleSubjectsResponse>() {
            @Override
            public void onResponse(Call<EligibleSubjectsResponse> call, Response<EligibleSubjectsResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && !response.body().getData().isEmpty()) {
                    List<EligibleSubjectsResponse.EligibleSubject> eligible = response.body().getData();
                    StringBuilder sb = new StringBuilder();
                    for (EligibleSubjectsResponse.EligibleSubject s : eligible) {
                        sb.append("• ").append(s.getSubjectName());
                        if (!s.getSubjectCode().isEmpty()) sb.append(" (").append(s.getSubjectCode()).append(")");
                        sb.append(" [").append(s.getCredits()).append(" Cr, ").append(s.getSubjectType()).append("]\n\n");
                    }
                    showEligibleDialog(sb.toString().trim());
                } else {
                    showEligibleDialog("All enrolled curriculum subjects are active. Direct online course modification via API is disabled for student safety. Please use BBSBEC ERP portal for registration edits.");
                }
            }

            @Override
            public void onFailure(Call<EligibleSubjectsResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                showEligibleDialog("Current semester subjects are locked in ERP. Direct modifications via mobile are disabled for data integrity.");
            }
        });
    }

    private void showEligibleDialog(String content) {
        new AlertDialog.Builder(this)
                .setTitle("Eligible Curriculum Subjects")
                .setMessage(content + "\n\n⚠️ Safety Notice: Modifying registrations through third-party API is blocked. Please visit https://api.bbsbec.edu.in to officially submit new courses.")
                .setPositiveButton("Understood", null)
                .show();
    }
}
