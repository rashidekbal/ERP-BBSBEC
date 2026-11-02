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
    private static final String KEY_SAVED_USERNAME = "saved_username";
    private static final String KEY_SAVED_PASSWORD = "saved_password";

    private static SharedPreferences getPrefs() {
        return App.getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void saveSession(String accessToken, String refreshToken, String email, String password, String name) {
        getPrefs().edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_STUDENT_EMAIL, email)
                .putString(KEY_SAVED_USERNAME, email)
                .putString(KEY_SAVED_PASSWORD, password)
                .putString(KEY_STUDENT_NAME, name)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public static synchronized void saveSession(String accessToken, String refreshToken, String email, String name) {
        saveSession(accessToken, refreshToken, email, null, name);
    }

    public static synchronized void saveCredentials(String username, String password) {
        getPrefs().edit()
                .putString(KEY_SAVED_USERNAME, username)
                .putString(KEY_SAVED_PASSWORD, password)
                .apply();
    }

    public static synchronized void updateAccessToken(String newAccessToken) {
        getPrefs().edit()
                .putString(KEY_ACCESS_TOKEN, newAccessToken)
                .apply();
    }

    public static synchronized void updateRefreshToken(String newRefreshToken) {
        getPrefs().edit()
                .putString(KEY_REFRESH_TOKEN, newRefreshToken)
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

    public static String getSavedUsername() {
        String username = getPrefs().getString(KEY_SAVED_USERNAME, null);
        if (username == null || username.trim().isEmpty()) {
            username = getStudentEmail();
        }
        return username;
    }

    public static String getSavedPassword() {
        return getPrefs().getString(KEY_SAVED_PASSWORD, null);
    }

    public static boolean hasSavedCredentials() {
        String u = getSavedUsername();
        String p = getSavedPassword();
        return u != null && !u.trim().isEmpty() && p != null && !p.trim().isEmpty();
    }

    public static String getStudentName() {
        return getPrefs().getString(KEY_STUDENT_NAME, "Student");
    }

    public static String getStudentRoll() {
        return getPrefs().getString(KEY_STUDENT_ROLL, "");
    }

    public static boolean isLoggedIn() {
        if (hasSavedCredentials()) {
            return getPrefs().getBoolean(KEY_IS_LOGGED_IN, true);
        }
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty() && getPrefs().getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static synchronized void clearSession() {
        getPrefs().edit().clear().apply();
    }
}
