package com.redcodersgroup.attend_collegeattendancetracker.screens.terms;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivityTermsBinding;

public class TermsActivity extends AppCompatActivity {
    private ActivityTermsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityTermsBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        binding.backBtn.setOnClickListener(v -> finish());

        binding.portfolioCard.setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://rasidekbal.in"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
