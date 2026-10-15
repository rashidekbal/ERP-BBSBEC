package com.redcodersgroup.attend_collegeattendancetracker.screens.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityEntryBinding;
import com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager;
import com.redcodersgroup.attend_collegeattendancetracker.screens.auth.LoginActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

public class EntryActivity extends AppCompatActivity {

    private ActivityEntryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        handleNavigation();
    }

    private void handleNavigation() {
        if (!TokenManager.isLoggedIn()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }, 1000);
            return;
        }

        final boolean[] navigated = {false};
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable navigateHome = () -> {
            if (navigated[0] || isFinishing() || isDestroyed()) return;
            navigated[0] = true;
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        };

        // Guarantee maximum splash duration of 2500ms
        handler.postDelayed(navigateHome, 2500);

        com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.syncOnLaunchIfNeeded(new com.redcodersgroup.attend_collegeattendancetracker.network.ErpSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> handler.postDelayed(navigateHome, 800));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> handler.postDelayed(navigateHome, 800));
            }
        });
    }
}