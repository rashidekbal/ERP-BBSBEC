package com.redcodersgroup.attend_collegeattendancetracker.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.redcodersgroup.attend_collegeattendancetracker.App;

public class TokenManager {
    private static final String PREF_NAME = "bbsbec_auth_pref";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_STUDENT_EMAIL = "student_email";
    private static final String KEY_STUDENT_NAME = "student_name";
    private static final String KEY_STUDENT_ROLL = "student_roll";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static SharedPreferences getPrefs() {
        return App.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void saveSession(String accessToken, String refreshToken, String email, String name) {
        getPrefs().edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_STUDENT_EMAIL, email)
                .putString(KEY_STUDENT_NAME, name)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public static synchronized void updateAccessToken(String newAccessToken) {
        getPrefs().edit()
                .putString(KEY_ACCESS_TOKEN, newAccessToken)
                .apply();
    }

    public static synchronized void saveStudentRoll(String roll) {
        getPrefs().edit().putString(KEY_STUDENT_ROLL, roll).apply();
    }

    public static String getAccessToken() {
        return getPrefs().getString(KEY_ACCESS_TOKEN, null);
    }

    public static String getRefreshToken() {
        return getPrefs().getString(KEY_REFRESH_TOKEN, null);
    }

    public static String getStudentEmail() {
        return getPrefs().getString(KEY_STUDENT_EMAIL, "");
    }

    public static String getStudentName() {
        return getPrefs().getString(KEY_STUDENT_NAME, "Student");
    }

    public static String getStudentRoll() {
        return getPrefs().getString(KEY_STUDENT_ROLL, "");
    }

    public static boolean isLoggedIn() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty() && getPrefs().getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static synchronized void clearSession() {
        getPrefs().edit().clear().apply();
    }
}
