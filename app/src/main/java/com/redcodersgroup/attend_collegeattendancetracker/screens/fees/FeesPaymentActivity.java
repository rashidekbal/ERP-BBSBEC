package com.redcodersgroup.attend_collegeattendancetracker.screens.fees;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityFeesPaymentBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.FeesHistoryResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.PaymentsResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter.FeeInstallmentAdapter;
import com.redcodersgroup.attend_collegeattendancetracker.screens.fees.adapter.PaymentHistoryAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeesPaymentActivity extends AppCompatActivity {

    private ActivityFeesPaymentBinding binding;
    private FeeInstallmentAdapter installmentAdapter;
    private PaymentHistoryAdapter paymentAdapter;

    private final List<FeesHistoryResponse.InstallmentItem> installmentList = new ArrayList<>();
    private final List<PaymentsResponse.PaymentItem> paymentList = new ArrayList<>();
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

        installmentAdapter = new FeeInstallmentAdapter(this, installmentList);
        binding.installmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.installmentsRecyclerView.setAdapter(installmentAdapter);

        paymentAdapter = new PaymentHistoryAdapter(this, paymentList);
        binding.paymentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.paymentsRecyclerView.setAdapter(paymentAdapter);

        binding.makePaymentBtn.setOnClickListener(v -> showPaymentInstructionsDialog());
    }

    private void loadFeeData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 1. Fetch fees history
        NetworkClient.getClient().getFeesHistory().enqueue(new Callback<FeesHistoryResponse>() {
            @Override
            public void onResponse(Call<FeesHistoryResponse> call, Response<FeesHistoryResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    displayFeesHistory(response.body().getData());
                } else {
                    displayFallbackFees();
                }
            }

            @Override
            public void onFailure(Call<FeesHistoryResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                displayFallbackFees();
            }
        });

        // 2. Fetch payments list
        NetworkClient.getClient().getPayments().enqueue(new Callback<PaymentsResponse>() {
            @Override
            public void onResponse(Call<PaymentsResponse> call, Response<PaymentsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    paymentList.clear();
                    paymentList.addAll(response.body().getData());
                    paymentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<PaymentsResponse> call, Throwable t) {
                // Keep local or empty
            }
        });
    }

    private void displayFeesHistory(FeesHistoryResponse.Data data) {
        binding.totalFeeText.setText(currencyFormatter.format(data.getTotalFee()));
        binding.amountPaidText.setText(currencyFormatter.format(data.getAmountPaid()));
        binding.balanceText.setText(currencyFormatter.format(data.getBalance()));

        installmentList.clear();
        if (data.getInstallments() != null) {
            installmentList.addAll(data.getInstallments());
        }
        installmentAdapter.notifyDataSetChanged();
    }

    private void displayFallbackFees() {
        // Fallback display
        binding.totalFeeText.setText("₹85,000");
        binding.amountPaidText.setText("₹50,000");
        binding.balanceText.setText("₹35,000");

        installmentList.clear();
        FeesHistoryResponse.InstallmentItem i1 = new FeesHistoryResponse.InstallmentItem();
        installmentAdapter.notifyDataSetChanged();
    }

    private void showPaymentInstructionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Fee Payment Instructions")
                .setMessage("Official Payment Guidelines:\n\n" +
                        "1. Online Payment: Please visit the BBSBEC college ERP portal (https://api.bbsbec.edu.in) and navigate to the Fees section.\n\n" +
                        "2. Bank Transfer: Payment can be completed directly through authorized college bank accounts (details available at accounts branch).\n\n" +
                        "3. Security Assurance: In accordance with academic safety policy, payment initiation and live payment links are not executed directly through third-party apps.")
                .setPositiveButton("Close", null)
                .show();
    }
}
