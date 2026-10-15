package com.redcodersgroup.attend_collegeattendancetracker.utils;

import com.redcodersgroup.attend_collegeattendancetracker.constants.SharedPrefKeys;

import com.redcodersgroup.attend_collegeattendancetracker.core.SharedPref;


public class SharedPrefUtil {


    public static boolean isSetupComplete(){
       return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.IS_SETUP_COMPLETE.toString(),false);
    }
    public static void setSetupComplete(boolean val){
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.IS_SETUP_COMPLETE.toString(),val).apply();
    }

    public static int getPeriodLength() {
        return SharedPref.getSharedPrefs().getInt(SharedPrefKeys.PERIOD_LENGTH.toString(), 50);
    }

    public static void setPeriodLength(int lengthMinutes) {
        SharedPref.getSharedPrefs().edit().putInt(SharedPrefKeys.PERIOD_LENGTH.toString(), lengthMinutes).apply();
    }

    public static String getAccentColor() {
        return SharedPref.getSharedPrefs().getString(SharedPrefKeys.ACCENT_COLOR.toString(), "#F5A623");
    }

    public static void setAccentColor(String colorHex) {
        SharedPref.getSharedPrefs().edit().putString(SharedPrefKeys.ACCENT_COLOR.toString(), colorHex).apply();
    }

    public static boolean isAttendanceReminderEnabled() {
        return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.ATTENDANCE_REMINDERS.toString(), true);
    }

    public static void setAttendanceReminderEnabled(boolean enabled) {
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.ATTENDANCE_REMINDERS.toString(), enabled).apply();
    }

    public static boolean isAssignmentReminderEnabled() {
        return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.ASSIGNMENT_REMINDERS.toString(), true);
    }

    public static void setAssignmentReminderEnabled(boolean enabled) {
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.ASSIGNMENT_REMINDERS.toString(), enabled).apply();
    }

    public static boolean isHolidayAlertEnabled() {
        return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.HOLIDAY_ALERTS.toString(), true);
    }

    public static void setHolidayAlertEnabled(boolean enabled) {
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.HOLIDAY_ALERTS.toString(), enabled).apply();
    }

    public static boolean isDarkModeEnabled() {
        return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.DARK_MODE.toString(), false);
    }

    public static void setDarkModeEnabled(boolean enabled) {
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.DARK_MODE.toString(), enabled).apply();
    }





    public static boolean isProfileComplete() {
        return SharedPref.getSharedPrefs().getBoolean(SharedPrefKeys.IS_PROFILE_COMPLETE.toString(), false);
    }

    public static void setProfileComplete(boolean val) {
        SharedPref.getSharedPrefs().edit().putBoolean(SharedPrefKeys.IS_PROFILE_COMPLETE.toString(), val).apply();
    }

    // 4 hours in milliseconds (4 * 60 * 60 * 1000)
    public static final long FULL_SYNC_INTERVAL_MS = 4 * 60 * 60 * 1000L;

    public static long getLastFullSyncTime() {
        return SharedPref.getSharedPrefs().getLong(SharedPrefKeys.LAST_FULL_SYNC_TIMESTAMP.toString(), 0L);
    }

    public static void setLastFullSyncTime(long timestamp) {
        SharedPref.getSharedPrefs().edit().putLong(SharedPrefKeys.LAST_FULL_SYNC_TIMESTAMP.toString(), timestamp).apply();
    }

    /**
     * Returns true if 4 hours have elapsed since the last full sync, or if sync never ran.
     */
    public static boolean isFullSyncDue() {
        long last = getLastFullSyncTime();
        if (last <= 0L) {
            return true;
        }
        return (System.currentTimeMillis() - last) >= FULL_SYNC_INTERVAL_MS;
    }

    public static java.util.Set<String> getNotifiedAnnouncementIds() {
        return SharedPref.getSharedPrefs().getStringSet("notified_announcement_ids", new java.util.HashSet<>());
    }

    public static void addNotifiedAnnouncementId(int id) {
        java.util.Set<String> set = new java.util.HashSet<>(getNotifiedAnnouncementIds());
        set.add(String.valueOf(id));
        SharedPref.getSharedPrefs().edit().putStringSet("notified_announcement_ids", set).apply();
    }
}
