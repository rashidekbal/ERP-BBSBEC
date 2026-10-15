package com.redcodersgroup.attend_collegeattendancetracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // Create notification channels
            NotificationHelper.createNotificationChannels(context);

            // Re-arm alarms if setup is complete
            if (SharedPrefUtil.isSetupComplete()) {
                NotificationScheduler.scheduleDailyReminder(context);
            }
        }
    }
}
