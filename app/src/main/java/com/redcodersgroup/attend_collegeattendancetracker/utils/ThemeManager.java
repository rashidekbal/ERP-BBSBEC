package com.redcodersgroup.attend_collegeattendancetracker.utils;

import android.content.Context;
import android.graphics.Color;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redcodersgroup.attend_collegeattendancetracker.App;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.util.Arrays;
import java.util.List;

public class ThemeManager {

    public static final String DEFAULT_ACCENT_COLOR = "#F5A623";

    // 8 Curated Accent Colors
    public static final List<String> ACCENT_COLORS = Arrays.asList(
            "#F5A623", // Amber Gold
            "#22C55E", // Emerald Green
            "#F5654A", // Coral Flame
            "#3B82F6", // Electric Blue
            "#8B5CF6", // Violet Purple
            "#EC4899", // Radiant Pink
            "#06B6D4", // Cyan Teal
            "#6366F1"  // Royal Indigo
    );

    // 8 Curated Subject Colors
    public static final List<String> SUBJECT_COLORS = Arrays.asList(
            "#F5A623", // Amber Gold
            "#22C55E", // Emerald Green
            "#F5654A", // Coral Red
            "#3B82F6", // Ocean Blue
            "#8B5CF6", // Purple
            "#EC4899", // Hot Pink
            "#06B6D4", // Turquoise
            "#FB923C"  // Bright Orange
    );

    private static final MutableLiveData<String> accentColorLiveData;

    static {
        // Initialize with saved preference or default safely (supports background thread class loading)
        String initialColor = DEFAULT_ACCENT_COLOR;
        try {
            String saved = SharedPrefUtil.getAccentColor();
            if (saved != null && !saved.trim().isEmpty()) {
                initialColor = saved.trim();
            }
        } catch (Exception ignored) {
        }
        accentColorLiveData = new MutableLiveData<>(initialColor);
    }

    public static LiveData<String> getAccentColorLiveData() {
        return accentColorLiveData;
    }

    public static String getAccentColor() {
        String color = accentColorLiveData.getValue();
        if (color == null || color.isEmpty()) {
            color = SharedPrefUtil.getAccentColor();
            if (color == null || color.isEmpty()) {
                color = DEFAULT_ACCENT_COLOR;
            }
        }
        return color;
    }

    public static int getAccentColorInt() {
        try {
            return Color.parseColor(getAccentColor());
        } catch (Exception e) {
            return Color.parseColor(DEFAULT_ACCENT_COLOR);
        }
    }

    public static void setAccentColor(String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) return;
        String formatted = colorHex.trim();
        if (!formatted.startsWith("#")) {
            formatted = "#" + formatted;
        }

        // Validate hex color
        try {
            Color.parseColor(formatted);
        } catch (Exception e) {
            return;
        }

        SharedPrefUtil.setAccentColor(formatted);
        accentColorLiveData.postValue(formatted);

        // Also sync with database user record
        String finalColor = formatted;
        DbHelper.execute(() -> {
            try {
                UserSchema user = Database.getInstance().getUserDao().getUser();
                if (user != null && !finalColor.equalsIgnoreCase(user.getThemeColor())) {
                    user.setThemeColor(finalColor);
                    Database.getInstance().getUserDao().updateUser(user);
                }
            } catch (Exception ignored) {
            }
        });

        try {
            Context ctx = App.getContext();
            if (ctx != null) {
                AttendanceWidgetProvider.refreshAllWidgets(ctx);
            }
        } catch (Exception ignored) {
        }
    }

    public static int parseColorSafe(String colorHex, String fallbackHex) {
        try {
            if (colorHex != null && !colorHex.isEmpty()) {
                return Color.parseColor(colorHex);
            }
        } catch (Exception ignored) {}
        try {
            return Color.parseColor(fallbackHex);
        } catch (Exception e) {
            return Color.parseColor(DEFAULT_ACCENT_COLOR);
        }
    }
}
