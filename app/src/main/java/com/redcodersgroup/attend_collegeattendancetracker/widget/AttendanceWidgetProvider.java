package com.redcodersgroup.attend_collegeattendancetracker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.HomeActivity;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_MARK_ATTENDANCE = "com.rtechnologies.attend_collegeattendancetracker.ACTION_MARK_ATTENDANCE";
    public static final String ACTION_MARK_ALL_PRESENT = "com.rtechnologies.attend_collegeattendancetracker.ACTION_MARK_ALL_PRESENT";
    public static final String ACTION_REFRESH_WIDGET = "com.rtechnologies.attend_collegeattendancetracker.ACTION_REFRESH_WIDGET";

    public static final String EXTRA_SID = "extra_sid";
    public static final String EXTRA_IS_LAB = "extra_is_lab";
    public static final String EXTRA_TARGET_STATUS = "extra_target_status";
    public static final String EXTRA_CURRENT_STATUS = "extra_current_status";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @SuppressWarnings("deprecation")
    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_attendance);

        // 1. Current Date & Accent Colors
        String dateStr = new SimpleDateFormat("EEEE, d MMM", Locale.US).format(new Date());
        views.setTextViewText(R.id.widget_date_text, dateStr);
        int accentColor = ThemeManager.getAccentColorInt();
        views.setTextViewText(R.id.widget_app_title, context.getString(R.string.app_name));
        views.setTextColor(R.id.widget_app_title, accentColor);

        // 2. Set RemoteAdapter for ListView
        Intent serviceIntent = new Intent(context, AttendanceWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_class_listview, serviceIntent);
        views.setEmptyView(R.id.widget_class_listview, R.id.widget_empty_view);

        // 3. PendingIntent Template for List Items (item row clicks open app)
        Intent itemClickIntent = new Intent(context, AttendanceWidgetProvider.class);
        itemClickIntent.setAction(ACTION_MARK_ATTENDANCE);
        itemClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setPendingIntentTemplate(R.id.widget_class_listview, itemClickPendingIntent);

        // 5. Refresh Action
        Intent refreshIntent = new Intent(context, AttendanceWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_WIDGET);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 2000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent);

        // 6. App Launch Intent (Header title, empty state, footer)
        Intent appIntent = new Intent(context, HomeActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 3000,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_header_title_container, appPendingIntent);
        views.setOnClickPendingIntent(R.id.widget_empty_view, appPendingIntent);
        views.setOnClickPendingIntent(R.id.widget_footer, appPendingIntent);

        // Initial widget layout push
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 7. Async stats and dynamic empty state update
        DbHelper.execute(() -> {
            try {
                Database db = Database.getInstance();
                UserSchema currentUser = db.getUserDao().getUser();
                int userCriteria = (currentUser != null && currentUser.getAttendanceCriteria() > 0)
                        ? currentUser.getAttendanceCriteria() : 75;

                List<SubjectSchema> subjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
                List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
                List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
                List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

                long now = System.currentTimeMillis();
                String todayDayName = new SimpleDateFormat("EEEE", Locale.US).format(new Date());

                // Check if today is a holiday
                HolidaySchema todayHoliday = null;
                if (holidays != null) {
                    for (HolidaySchema h : holidays) {
                        if (h != null && AttendanceScheduleUtil.isHoliday(Collections.singletonList(h), now)) {
                            todayHoliday = h;
                            break;
                        }
                    }
                }

                // Check scheduled classes today
                TimeTableDaySchema todayDay = null;
                if (days != null) {
                    for (TimeTableDaySchema d : days) {
                        if (d != null && d.getDayName() != null && d.getDayName().equalsIgnoreCase(todayDayName)) {
                            todayDay = d;
                            break;
                        }
                    }
                }

                int todayClassesCount = 0;
                if (todayHoliday == null && todayDay != null && schedules != null) {
                    for (DayScheduleSchema s : schedules) {
                        if (s != null && s.getDid() == todayDay.getDid()) {
                            for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
                                Integer sid = TimeTableViewModel.getPeriodValue(s, p);
                                if (sid != null && sid > 0) {
                                    todayClassesCount++;
                                }
                            }
                            break;
                        }
                    }
                }

                // Configure Empty State View and Header Actions based on state
                if (todayHoliday != null) {
                    String holName = (todayHoliday.getEventName() != null && !todayHoliday.getEventName().trim().isEmpty())
                            ? todayHoliday.getEventName().trim() : "Holiday";
                    views.setImageViewResource(R.id.widget_empty_icon, R.drawable.ic_empty_holiday);
                    views.setTextViewText(R.id.widget_empty_title, holName + " 🌴");
                    views.setTextViewText(R.id.widget_empty_subtitle, "Auto-excluded from attendance · Tap to open");
                    views.setTextViewText(R.id.widget_date_text, dateStr + " · Holiday");
                } else if (subjects == null || subjects.isEmpty()) {
                    views.setImageViewResource(R.id.widget_empty_icon, R.drawable.ic_empty_classes);
                    views.setTextViewText(R.id.widget_empty_title, "No timetable setup yet");
                    views.setTextViewText(R.id.widget_empty_subtitle, "Tap to setup subjects & timetable");
                    views.setTextViewText(R.id.widget_date_text, dateStr);
                } else if (todayClassesCount == 0) {
                    views.setImageViewResource(R.id.widget_empty_icon, R.drawable.ic_empty_classes);
                    views.setTextViewText(R.id.widget_empty_title, "No classes scheduled today");
                    views.setTextViewText(R.id.widget_empty_subtitle, "Enjoy your " + todayDayName + "! · Tap to open");
                    views.setTextViewText(R.id.widget_date_text, dateStr + " · Free day");
                } else {
                    views.setTextViewText(R.id.widget_date_text, dateStr + " · " + todayClassesCount + (todayClassesCount == 1 ? " class" : " classes"));
                }

                // Compute overall stats
                int totalAttended = 0;
                int totalConducted = 0;

                if (subjects != null) {
                    for (SubjectSchema subject : subjects) {
                        boolean isLab = subject.isLab();
                        List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(subject.getSid(), isLab);
                        AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                                currentUser, subject, isLab, records, holidays, days, schedules);

                        totalAttended += stats.getAttended();
                        totalConducted += stats.getConducted();
                    }
                }

                int overallPct;
                if (totalConducted > 0) {
                    overallPct = (int) Math.round((totalAttended * 100.0) / totalConducted);
                } else {
                    overallPct = (subjects != null && !subjects.isEmpty()) ? 100 : 0;
                }

                String statusStr;
                if (totalConducted == 0) {
                    statusStr = "On track";
                } else if (overallPct >= userCriteria) {
                    int bunkSafe = (100 * totalAttended - userCriteria * totalConducted) / userCriteria;
                    statusStr = "Bunk-safe: " + bunkSafe + (bunkSafe == 1 ? " class" : " classes");
                } else {
                    int needed = (int) Math.ceil((userCriteria * totalConducted - 100.0 * totalAttended) / (100.0 - userCriteria));
                    statusStr = "Attend next " + needed + (needed == 1 ? " class" : " classes");
                }

                views.setTextViewText(R.id.widget_footer_pct, overallPct + "% Overall");
                views.setTextColor(R.id.widget_footer_pct, ThemeManager.getAccentColorInt());
                views.setTextViewText(R.id.widget_footer_status, statusStr);

                appWidgetManager.updateAppWidget(appWidgetId, views);
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_class_listview);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, AttendanceWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            for (int id : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id);
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_class_listview);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_MARK_ATTENDANCE.equals(action)) {
            Intent appIntent = new Intent(context, HomeActivity.class);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(appIntent);
        } else if (ACTION_MARK_ALL_PRESENT.equals(action)) {
            Intent appIntent = new Intent(context, HomeActivity.class);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(appIntent);
        } else if (ACTION_REFRESH_WIDGET.equals(action)) {
            refreshAllWidgets(context);
        }

        super.onReceive(context, intent);
    }
}
