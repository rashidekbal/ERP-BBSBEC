package com.redcodersgroup.attend_collegeattendancetracker.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;

public class NotificationHelper {

    public static final String CHANNEL_ATTENDANCE_ID = "attendance_channel";
    public static final String CHANNEL_ATTENDANCE_NAME = "Attendance Reminders";

    public static final String CHANNEL_ASSIGNMENT_ID = "assignment_channel";
    public static final String CHANNEL_ASSIGNMENT_NAME = "Assignment Reminders";

    public static final String CHANNEL_HOLIDAY_ID = "holiday_channel";
    public static final String CHANNEL_HOLIDAY_NAME = "Holiday Alerts";

    public static final int NOTIFICATION_ID_ATTENDANCE = 1001;
    public static final int NOTIFICATION_ID_HOLIDAY = 1002;
    public static final int NOTIFICATION_ID_ASSIGNMENT_BASE = 2000;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel attChannel = new NotificationChannel(
                    CHANNEL_ATTENDANCE_ID,
                    CHANNEL_ATTENDANCE_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            attChannel.setDescription("Daily reminders to log your class attendance");

            NotificationChannel asgnChannel = new NotificationChannel(
                    CHANNEL_ASSIGNMENT_ID,
                    CHANNEL_ASSIGNMENT_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            asgnChannel.setDescription("Due date alerts and deadline reminders for assignments");

            NotificationChannel holChannel = new NotificationChannel(
                    CHANNEL_HOLIDAY_ID,
                    CHANNEL_HOLIDAY_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            holChannel.setDescription("Alerts when holidays cancel scheduled classes");

            manager.createNotificationChannel(attChannel);
            manager.createNotificationChannel(asgnChannel);
            manager.createNotificationChannel(holChannel);
        }
    }

    public static void showAttendanceReminder(Context context, String title, String body) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_ATTENDANCE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ATTENDANCE, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showAssignmentReminder(Context context, int notificationId, String title, String body) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra("NAVIGATE_TO_TASK", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ASSIGNMENT_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showHolidayAlert(Context context, String title, String body) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_HOLIDAY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_HOLIDAY_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_HOLIDAY, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
