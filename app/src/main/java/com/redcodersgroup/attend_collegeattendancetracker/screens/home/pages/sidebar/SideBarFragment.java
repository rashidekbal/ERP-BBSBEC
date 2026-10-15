package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.sidebar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.FragmentSideBarBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.screens.holiday.HolidayActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.profile.ProfileActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.setting.SettingActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.TimeTableActivity;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

public class SideBarFragment extends Fragment {
    private FragmentSideBarBinding binding;
    private HomeActivity activity;

    public SideBarFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSideBarBinding.inflate(inflater, container, false);
        init();
        setEventListeners();
        setupDynamicVersion();
        observeUserData();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        String colorHex = ThemeManager.getAccentColor();
        if (colorHex != null) {
            try {
                int color = Color.parseColor(colorHex);
                binding.viewProfile.setTextColor(color);
                binding.profileCard.setCardBackgroundColor(color);
            } catch (Exception ignored) {}
        }
    }

    private void init() {
        activity = (HomeActivity) requireActivity();
    }

    private void setupDynamicVersion() {
        String appName = getString(R.string.app_name);
        try {
            String versionName = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
            binding.appVersionText.setText(appName + " · v" + versionName);
        } catch (Exception e) {
            binding.appVersionText.setText(appName + " · v1.0.0");
        }
    }

    private void observeUserData() {
        Database.getInstance().getUserDao().getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.userName.setText(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Student");
                binding.usernameInitials.setText(getInitials(user.getName()));
                binding.userSubtitle.setText("sem-" + user.getSemester());
                String colorStr = user.getThemeColor();
                if (colorStr == null || colorStr.isEmpty()) {
                    colorStr = ThemeManager.getAccentColor();
                }
                try {
                    int color = Color.parseColor(colorStr);
                    binding.profileCard.setCardBackgroundColor(color);
                } catch (Exception ignored) {}
            }
        });

        ThemeManager.getAccentColorLiveData().observe(getViewLifecycleOwner(), colorHex -> {
            if (colorHex != null) {
                try {
                    int color = Color.parseColor(colorHex);
                    binding.viewProfile.setTextColor(color);
                    binding.profileCard.setCardBackgroundColor(color);
                } catch (Exception ignored) {}
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AT";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].length() >= 2 ? parts[0].substring(0, 2).toUpperCase() : parts[0].toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void setEventListeners() {
        binding.settingBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), SettingActivity.class));
            activity.closeDrawer();
        });
        binding.holidayBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), HolidayActivity.class));
            activity.closeDrawer();
        });

        binding.timeTableBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), TimeTableActivity.class));
            activity.closeDrawer();
        });
        binding.semesterRegistrationBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), com.redcodersgroup.attend_collegeattendancetracker.screens.registration.SemesterRegistrationActivity.class));
            activity.closeDrawer();
        });
        binding.feesPaymentBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), com.redcodersgroup.attend_collegeattendancetracker.screens.fees.FeesPaymentActivity.class));
            activity.closeDrawer();
        });
        binding.webmailBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), com.redcodersgroup.attend_collegeattendancetracker.screens.webmail.WebmailActivity.class));
            activity.closeDrawer();
        });
        binding.notificationsBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), com.redcodersgroup.attend_collegeattendancetracker.screens.notifications.NotificationsActivity.class));
            activity.closeDrawer();
        });
        binding.profileBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ProfileActivity.class));
            activity.closeDrawer();
        });
    }
}