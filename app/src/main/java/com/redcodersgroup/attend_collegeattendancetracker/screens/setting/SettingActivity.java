package com.redcodersgroup.attend_collegeattendancetracker.screens.setting;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.card.MaterialCardView;
import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.databinding.ActivitySettingBinding;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.notifications.NotificationScheduler;
import com.redcodersgroup.attend_collegeattendancetracker.screens.privacy.PrivacyPolicyActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.profile.ProfileActivity;
import com.redcodersgroup.attend_collegeattendancetracker.services.ExportService;
import com.redcodersgroup.attend_collegeattendancetracker.services.ImportService;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingActivity extends AppCompatActivity {
    private ActivitySettingBinding binding;
    private UserSchema currentUser;
    ExecutorService executorService;
    private MaterialCardView[] swatchCards;
    private final String[] SWATCH_HEXES = new String[]{
            "#F5A623", // Amber
            "#22C55E", // Green
            "#F5654A", // Coral
            "#3B82F6", // Blue
            "#8B5CF6", // Purple
            "#EC4899", // Pink
            "#06B6D4", // Cyan
            "#6366F1"  // Indigo
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied. Notifications will not be shown.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> exportDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null) return;
                performExportToUri(uri);
            });

    private final ActivityResultLauncher<String[]> importDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                showImportConfirmDialog(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        executorService = Executors.newSingleThreadExecutor();
        initSwatches();
        setupEventListeners();
        setupSwitches();
        observeThemeAndUser();
        setupVersionInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!NotificationScheduler.canScheduleExactAlarms(this)) {
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                        getSupportFragmentManager(),
                        "Enable Precise Reminders",
                        "To ensure campus notifications and assignment reminders arrive exactly on schedule without battery optimization delays, please enable 'Allow setting alarms and reminders'.",
                        "Open Settings",
                        "Not Now",
                        R.drawable.arrow_right,
                        false,
                        () -> NotificationScheduler.openExactAlarmSettings(SettingActivity.this)
                );
            }
        }
    }

    private void initSwatches() {
        swatchCards = new MaterialCardView[]{
                binding.swatchAmber,
                binding.swatchGreen,
                binding.swatchCoral,
                binding.swatchBlue,
                binding.swatchPurple,
                binding.swatchPink,
                binding.swatchCyan,
                binding.swatchIndigo
        };

        for (int i = 0; i < swatchCards.length; i++) {
            final String colorHex = SWATCH_HEXES[i];
            swatchCards[i].setOnClickListener(v -> {
                String current = ThemeManager.getAccentColor();
                if (colorHex.equalsIgnoreCase(current)) {
                    return;
                }
                ThemeManager.setAccentColor(colorHex);
                highlightActiveSwatch(colorHex);
                Toast.makeText(SettingActivity.this, "Theme updated successfully!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void highlightActiveSwatch(String activeHex) {
        if (swatchCards == null) return;
        int activeStrokeColor = getResources().getColor(R.color.text, null);
        int inactiveStrokeColor = getResources().getColor(R.color.border, null);
        int activeWidth = (int) (3 * getResources().getDisplayMetrics().density);
        int inactiveWidth = (int) (1 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < swatchCards.length; i++) {
            if (SWATCH_HEXES[i].equalsIgnoreCase(activeHex)) {
                swatchCards[i].setStrokeColor(activeStrokeColor);
                swatchCards[i].setStrokeWidth(activeWidth);
            } else {
                swatchCards[i].setStrokeColor(inactiveStrokeColor);
                swatchCards[i].setStrokeWidth(inactiveWidth);
            }
        }
    }

    private void setupEventListeners() {
        binding.backBtn.setOnClickListener(v -> finish());

        binding.thresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = (int) value;
            binding.thresholdText.setText(intVal + "%");
        });

        binding.thresholdSlider.addOnSliderTouchListener(new com.google.android.material.slider.Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(com.google.android.material.slider.Slider slider) {}

            @Override
            public void onStopTrackingTouch(com.google.android.material.slider.Slider slider) {
                int newCriteria = (int) slider.getValue();
                if (currentUser != null && currentUser.getAttendanceCriteria() != newCriteria) {
                    currentUser.setAttendanceCriteria(newCriteria);
                    DbHelper.execute(() -> {
                        Database.getInstance().getUserDao().updateUser(currentUser);
                        AttendanceWidgetProvider.refreshAllWidgets(SettingActivity.this);
                    });
                    Toast.makeText(SettingActivity.this, "Target threshold updated to " + newCriteria + "%", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.workingDaysRow.setOnClickListener(v -> showWorkingDaysDialog());
        binding.periodLengthRow.setOnClickListener(v -> showPeriodLengthDialog());
        binding.semesterDatesRow.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        binding.privacyPolicyRow.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        binding.termsRow.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, com.redcodersgroup.attend_collegeattendancetracker.screens.terms.TermsActivity.class);
            startActivity(intent);
        });

        binding.contactUsRow.setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://rasidekbal.vercel.app/apps/attend/support"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show();
            }
        });


        binding.exportDataRow.setOnClickListener(v -> {
            String defaultFileName = "attend_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
            exportDocumentLauncher.launch(defaultFileName);
        });

        binding.importDataRow.setOnClickListener(v -> {
            importDocumentLauncher.launch(new String[]{"application/json", "*/*"});
        });

        binding.clearDataRow.setOnClickListener(v -> showClearDataConfirmDialog());
        binding.logoutRow.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    private void showLogoutConfirmDialog() {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getSupportFragmentManager(),
                "Log Out from BBSBEC ERP?",
                "Are you sure you want to log out of your student account? You will return to the login screen.",
                "Log Out",
                "Cancel",
                R.drawable.arrow_left,
                true,
                this::performLogout
        );
    }

    private void performLogout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            db.getAttendanceDao().deleteAll();
            db.getAssignmentDao().deleteAll();
            db.getMarksDao().deleteAll();
            db.getSubjectAdditionalInfoDao().deleteAll();
            db.getDayScheduleDao().deleteAll();
            db.getTimeTableDayDao().deleteAll();
            db.getSubjectDao().deleteAll();
            db.getHolidayDao().deleteAll();
            db.getUserDao().deleteAll();

            com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager.clearSession();
            SharedPrefUtil.setSetupComplete(false);
            AttendanceWidgetProvider.refreshAllWidgets(SettingActivity.this);

            runOnUiThread(() -> {
                Intent intent = new Intent(this, com.redcodersgroup.attend_collegeattendancetracker.screens.auth.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void performExportToUri(Uri uri) {
        Toast.makeText(this, "Exporting backup...", Toast.LENGTH_SHORT).show();
        ExportService.exportDataToUri(this, uri, new ExportService.ExportCallback() {
            @Override
            public void onSuccess(Uri fileUri, String fileName) {
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showSuccess(
                        getSupportFragmentManager(),
                        "Export Successful",
                        "Backup saved successfully to your selected location.",
                        null
                );
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(SettingActivity.this, "Export failed: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showClearDataConfirmDialog() {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getSupportFragmentManager(),
                "Clear All App Data?",
                "This will permanently delete all attendance logs, marks records, assignments, and timetable schedules. This action CANNOT be undone.",
                "Clear Everything",
                "Cancel",
                R.drawable.ic_delete,
                true,
                this::performClearData
        );
    }

    private void performClearData() {
        Toast.makeText(this, "Clearing all app data...", Toast.LENGTH_SHORT).show();
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            db.getAttendanceDao().deleteAll();
            db.getAssignmentDao().deleteAll();
            db.getMarksDao().deleteAll();
            db.getSubjectAdditionalInfoDao().deleteAll();
            db.getDayScheduleDao().deleteAll();
            db.getTimeTableDayDao().deleteAll();
            db.getSubjectDao().deleteAll();
            db.getHolidayDao().deleteAll();

            UserSchema user = db.getUserDao().getUser();
            if (user != null) {
                user.setName("");
                user.setCollegeName("");
                user.setDepartment("");
                user.setBranch("");
                user.setSemester(1);
                user.setSemStartDate(0);
                user.setSemEndDate(0);
                user.setWorkingDays(5);
                user.setAttendanceCriteria(75);
                user.setRollNumber("");
                user.setPhone("");
                user.setEmail("");
                db.getUserDao().updateUser(user);
            }

            com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager.clearSession();
            SharedPrefUtil.setSetupComplete(false);
            AttendanceWidgetProvider.refreshAllWidgets(SettingActivity.this);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showSuccess(
                        getSupportFragmentManager(),
                        "Data Cleared & Logged Out",
                        "All database records and login sessions have been reset. The app will now return to login.",
                        "Sign In",
                        false,
                        this::restartApp
                );
            });
        });
    }

    private void showImportConfirmDialog(Uri uri) {
        com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.show(
                getSupportFragmentManager(),
                "Restore Backup?",
                "This will overwrite ALL your current data with the backup file. This action cannot be undone.",
                "Restore",
                "Cancel",
                R.drawable.ic_cloud_upload,
                false,
                () -> performImport(uri)
        );
    }

    private void performImport(Uri uri) {
        Toast.makeText(this, "Importing data...", Toast.LENGTH_SHORT).show();
        ImportService.importData(this, uri, new ImportService.ImportCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog.AppConfirmationDialogFragment.showSuccess(
                        getSupportFragmentManager(),
                        "Import Successful",
                        "Data restored successfully. The app will now restart.",
                        "Restart",
                        false,
                        SettingActivity.this::restartApp
                );
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(SettingActivity.this, "Import failed: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void restartApp() {
        Intent intent = new Intent(this, com.redcodersgroup.attend_collegeattendancetracker.screens.auth.LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void setupSwitches() {
        binding.darkModeSwitch.setChecked(SharedPrefUtil.isDarkModeEnabled());
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SharedPrefUtil.isDarkModeEnabled() != isChecked) {
                SharedPrefUtil.setDarkModeEnabled(isChecked);
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        SharedPrefUtil.setAttendanceReminderEnabled(false);
        NotificationScheduler.cancelDailyReminder(this);

        binding.assignmentReminderSwitch.setChecked(SharedPrefUtil.isAssignmentReminderEnabled());
        binding.assignmentReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefUtil.setAssignmentReminderEnabled(isChecked);
            if (isChecked) {
                checkAndRequestNotificationPermission();
                checkAndRequestExactAlarmPermission();
            }
        });

        binding.holidayAlertSwitch.setChecked(SharedPrefUtil.isHolidayAlertEnabled());
        binding.holidayAlertSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefUtil.setHolidayAlertEnabled(isChecked);
            if (isChecked) {
                checkAndRequestNotificationPermission();
                checkAndRequestExactAlarmPermission();
            }
        });
    }

    private void observeThemeAndUser() {
        // Observe accent color
        ThemeManager.getAccentColorLiveData().observe(this, colorHex -> {
            if (colorHex != null) {
                highlightActiveSwatch(colorHex);
                applyAccentColor(colorHex);
            }
        });
        highlightActiveSwatch(ThemeManager.getAccentColor());
        applyAccentColor(ThemeManager.getAccentColor());

        // Observe user details
        Database.getInstance().getUserDao().getUserLiveData().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                binding.workingDaysText.setText(user.getWorkingDays() + " days (" + (user.getWorkingDays() == 6 ? "Mon - Sat" : "Mon - Fri") + ")");
                binding.thresholdText.setText(user.getAttendanceCriteria() + "%");

                float criteriaVal = (float) user.getAttendanceCriteria();
                if (criteriaVal < 60f) criteriaVal = 60f;
                if (criteriaVal > 90f) criteriaVal = 90f;
                if (binding.thresholdSlider.getValue() != criteriaVal) {
                    binding.thresholdSlider.setValue(criteriaVal);
                }

                if (user.getSemStartDate() > 0 && user.getSemEndDate() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                    String range = sdf.format(new Date(user.getSemStartDate())) + " - " + sdf.format(new Date(user.getSemEndDate()));
                    binding.semesterDatesText.setText(range);
                } else {
                    binding.semesterDatesText.setText("Edit in Profile");
                }
            }
        });

        binding.periodLengthText.setText(SharedPrefUtil.getPeriodLength() + " mins");
    }

    private void applyAccentColor(String colorHex) {
        if (colorHex == null || binding == null) return;
        try {
            int color = Color.parseColor(colorHex);
            binding.accentColorTitle.setTextColor(color);
            binding.workingDaysText.setTextColor(color);
            binding.thresholdText.setTextColor(color);
            binding.periodLengthText.setTextColor(color);
            binding.semesterDatesText.setTextColor(color);

            binding.thresholdSlider.setThumbTintList(ColorStateList.valueOf(color));
            binding.thresholdSlider.setTrackActiveTintList(ColorStateList.valueOf(color));

            int trackOff = ContextCompat.getColor(this, R.color.surface2);
            ColorStateList trackCsl = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{color, trackOff}
            );
            binding.darkModeSwitch.setTrackTintList(trackCsl);
            binding.attendanceReminderSwitch.setTrackTintList(trackCsl);
            binding.assignmentReminderSwitch.setTrackTintList(trackCsl);
            binding.holidayAlertSwitch.setTrackTintList(trackCsl);
        } catch (Exception ignored) {}
    }

    private void setupVersionInfo() {
        String appName = getString(R.string.app_name);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            binding.appVersionText.setText(appName + " · v" + versionName);
        } catch (Exception e) {
            binding.appVersionText.setText(appName + " · v1.0.0");
        }
    }

    private void showWorkingDaysDialog() {
        if (currentUser == null) return;
        String[] options = new String[]{"5 Days (Monday - Friday)", "6 Days (Monday - Saturday)"};
        int checkedItem = currentUser.getWorkingDays() == 6 ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("Select Working Days")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    int newDays = which == 1 ? 6 : 5;
                    int oldDays = currentUser.getWorkingDays();
                    currentUser.setWorkingDays(newDays);

                    DbHelper.execute(() -> {
                        Database.getInstance().getUserDao().updateUser(currentUser);
                        syncSaturdaySchedule(oldDays, newDays);
                        AttendanceWidgetProvider.refreshAllWidgets(SettingActivity.this);
                    });

                    dialog.dismiss();
                    Toast.makeText(this, "Working days updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void syncSaturdaySchedule(int oldDays, int newDays) {
        if (oldDays == newDays) return;
        List<TimeTableDaySchema> days = Database.getInstance().getTimeTableDayDao().getDays();
        if (newDays == 6) {
            boolean hasSaturday = false;
            if (days != null) {
                for (TimeTableDaySchema day : days) {
                    if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                        hasSaturday = true;
                        break;
                    }
                }
            }
            if (!hasSaturday) {
                TimeTableDaySchema saturday = new TimeTableDaySchema("Saturday");
                Database.getInstance().getTimeTableDayDao().insert(saturday);
                List<TimeTableDaySchema> updatedDays = Database.getInstance().getTimeTableDayDao().getDays();
                for (TimeTableDaySchema day : updatedDays) {
                    if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                        Database.getInstance().getDayScheduleDao().insert(
                                new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null)
                        );
                        break;
                    }
                }
            }
        } else if (newDays == 5) {
            if (days != null) {
                for (TimeTableDaySchema day : days) {
                    if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                        Database.getInstance().getTimeTableDayDao().deleteDay(day.getDid());
                        break;
                    }
                }
            }
        }
    }

    private void showThresholdDialog() {
        if (currentUser == null) return;
        String[] options = new String[]{"65%", "70%", "75%", "80%", "85%", "90%"};
        int[] values = new int[]{65, 70, 75, 80, 85, 90};
        int current = currentUser.getAttendanceCriteria();
        int checkedItem = 2; // Default 75%
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Attendance Criteria")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    currentUser.setAttendanceCriteria(values[which]);
                    DbHelper.execute(() -> {
                        Database.getInstance().getUserDao().updateUser(currentUser);
                        AttendanceWidgetProvider.refreshAllWidgets(SettingActivity.this);
                    });
                    dialog.dismiss();
                    Toast.makeText(this, "Attendance criteria updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPeriodLengthDialog() {
        String[] options = new String[]{"30 mins", "40 mins", "45 mins", "50 mins", "55 mins", "60 mins"};
        int[] values = new int[]{30, 40, 45, 50, 55, 60};
        int current = SharedPrefUtil.getPeriodLength();
        int checkedItem = 3; // Default 50 mins
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Period Length")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    SharedPrefUtil.setPeriodLength(values[which]);
                    binding.periodLengthText.setText(values[which] + " mins");
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}