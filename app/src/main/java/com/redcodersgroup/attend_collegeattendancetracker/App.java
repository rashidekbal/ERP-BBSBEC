package com.redcodersgroup.attend_collegeattendancetracker;

import android.app.Application;
import android.content.Context;



public class App extends Application {
    public static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply persisted Dark Mode theme
        boolean isDarkMode = com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil.isDarkModeEnabled();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        com.redcodersgroup.attend_collegeattendancetracker.notifications.NotificationHelper.createNotificationChannels(this);
        com.redcodersgroup.attend_collegeattendancetracker.notifications.NotificationScheduler.scheduleDailyReminder(this);

    }

    public static Context getContext(){
        return instance.getApplicationContext();
    }
    public static App getInstance(){
        return instance;
    }
}

