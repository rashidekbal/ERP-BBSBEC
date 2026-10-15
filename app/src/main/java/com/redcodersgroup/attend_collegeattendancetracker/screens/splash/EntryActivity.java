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
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (!TokenManager.isLoggedIn()) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }, 1200);
    }
}