package com.redcodersgroup.attend_collegeattendancetracker.screens.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityLoginBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager;
import com.redcodersgroup.attend_collegeattendancetracker.network.NetworkClient;
import com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginResponse;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.loginBtn.setOnClickListener(v -> performLogin());

        binding.forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://erp.bbsbec.edu.in/"));
            startActivity(intent);
        });
    }

    private void performLogin() {
        String email = binding.emailEditText.getText() != null ? binding.emailEditText.getText().toString().trim() : "";
        String password = binding.passwordEditText.getText() != null ? binding.passwordEditText.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.emailInputLayout.setError("Enter your BBSBEC email");
            return;
        } else {
            binding.emailInputLayout.setError(null);
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.setError("Enter your password");
            return;
        } else {
            binding.passwordInputLayout.setError(null);
        }

        setLoading(true);

        WebLoginRequest request = new WebLoginRequest(email, password);
        NetworkClient.getApiService().webLogin(NetworkClient.WEB_LOGIN_URL, request).enqueue(new Callback<WebLoginResponse>() {
            @Override
            public void onResponse(Call<WebLoginResponse> call, Response<WebLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    WebLoginResponse.Data data = response.body().getData();
                    String token = data.getToken();
                    String refresh = data.getRefresh();
                    String studentName = data.getProfile() != null && data.getProfile().getName() != null
                            ? data.getProfile().getName() : "Student";

                    if (token != null && !token.trim().isEmpty()) {
                        TokenManager.saveSession(token, refresh, email, studentName);
                        SharedPrefUtil.setSetupComplete(true);

                        // Trigger full sync
                        syncAndNavigate();
                        return;
                    }
                }

                setLoading(false);
                String errorMsg = "Login failed. Please check your credentials.";
                if (response.code() == 401 || response.code() == 403) {
                    errorMsg = "Invalid email or password.";
                }
                showError(errorMsg);
            }

            @Override
            public void onFailure(Call<WebLoginResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Unable to reach server"));
            }
        });
    }

    private void syncAndNavigate() {
        binding.loginBtn.setText("Syncing ERP Data...");
        ErpSyncManager.syncAll(new ErpSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(LoginActivity.this, "Welcome to BBSBEC-ERP!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                // Even if initial sync fails on one endpoint, proceed to home
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginBtn.setEnabled(!loading);
        binding.emailEditText.setEnabled(!loading);
        binding.passwordEditText.setEnabled(!loading);
        if (loading) {
            binding.errorText.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        binding.errorText.setText(msg);
        binding.errorText.setVisibility(View.VISIBLE);
    }
}
