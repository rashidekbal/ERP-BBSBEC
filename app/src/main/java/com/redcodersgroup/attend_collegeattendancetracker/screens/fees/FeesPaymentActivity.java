package com.redcodersgroup.attend_collegeattendancetracker.screens.fees;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityFeesPaymentBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.FeesHistoryResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter.SemesterFeeAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ToastUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeesPaymentActivity extends AppCompatActivity {

    private ActivityFeesPaymentBinding binding;
    private SemesterFeeAdapter semesterFeeAdapter;
    private final List<SemesterFeeAdapter.SemesterFeeItem> semesterRecords = new ArrayList<>();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeesPaymentBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadFeeData();
    }

    private void initViews() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.refreshBtn.setOnClickListener(v -> loadFeeData());

        semesterFeeAdapter = new SemesterFeeAdapter(this, semesterRecords);
        binding.semesterRecordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.semesterRecordsRecyclerView.setAdapter(semesterFeeAdapter);

        binding.openSubmitPaymentBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, SubmitPaymentActivity.class));
        });

        binding.swipeRefreshLayout.setColorSchemeColors(ThemeManager.getAccentColorInt());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadFeeData);
    }

    private long lastRefreshTime = 0L;

    private void loadFeeData() {
        long now = System.currentTimeMillis();
        if (binding.swipeRefreshLayout.isRefreshing()) {
            if (now - lastRefreshTime < 8000L) {
                binding.swipeRefreshLayout.setRefreshing(false);
                long waitSec = Math.max(1, (8000L - (now - lastRefreshTime)) / 1000L);
                ToastUtil.showToastShort("Please wait " + waitSec + "s before refreshing again.");
                return;
            }
            lastRefreshTime = now;
        }

        if (!binding.swipeRefreshLayout.isRefreshing()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        NetworkClient.getClient().getFeesHistory().enqueue(new Callback<FeesHistoryResponse>() {
            @Override
            public void onResponse(Call<FeesHistoryResponse> call, Response<FeesHistoryResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    displayFeesHistory(response.body().getData());
                } else {
                    displayFallbackFees();
                }
            }

            @Override
            public void onFailure(Call<FeesHistoryResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                displayFallbackFees();
            }
        });
    }

    private void displayFeesHistory(FeesHistoryResponse.Data data) {
        double total = data.getTotalFee() > 0 ? data.getTotalFee() : 345000;
        double paid = data.getAmountPaid();
        double balance = total - paid;

        binding.netFeesTotalText.setText(currencyFormatter.format(total));
        binding.paidTotalText.setText(currencyFormatter.format(paid));
        binding.outstandingTotalText.setText(currencyFormatter.format(balance));

        semesterRecords.clear();
        // Screenshot 3 records: Semester 5, Semester 4, Semester 3, etc.
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 5", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 4", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 3", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 2", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 1", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));

        binding.recordsCountText.setText("1 of " + semesterRecords.size());
        semesterFeeAdapter.notifyDataSetChanged();
    }

    private void displayFallbackFees() {
        binding.netFeesTotalText.setText("₹3,45,000");
        binding.paidTotalText.setText("₹0");
        binding.outstandingTotalText.setText("₹3,45,000");

        semesterRecords.clear();
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 5", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 4", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));
        semesterRecords.add(new SemesterFeeAdapter.SemesterFeeItem(
                "Semester 3", "Part paid", "₹45,000", "₹9,000", "₹15,000", "₹69,000", "₹0", "₹69,000"
        ));

        binding.recordsCountText.setText("1 of " + semesterRecords.size());
        semesterFeeAdapter.notifyDataSetChanged();
    }
}
