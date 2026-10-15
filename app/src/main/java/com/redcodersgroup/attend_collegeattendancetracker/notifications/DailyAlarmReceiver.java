package com.redcodersgroup.attend_collegeattendancetracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DailyAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ensure channels exist
        NotificationHelper.createNotificationChannels(context);

        // Reschedule for next day
        NotificationScheduler.scheduleDailyReminder(context);

        // Perform checks in background
        DbHelper.execute(() -> {
            try {
                if (!SharedPrefUtil.isSetupComplete()) return;

                UserSchema user = Database.getInstance().getUserDao().getUser();
                if (user == null) return;

                Calendar now = Calendar.getInstance();
                long nowMs = now.getTimeInMillis();

                Calendar todayStart = Calendar.getInstance();
                todayStart.set(Calendar.HOUR_OF_DAY, 0);
                todayStart.set(Calendar.MINUTE, 0);
                todayStart.set(Calendar.SECOND, 0);
                todayStart.set(Calendar.MILLISECOND, 0);
                long todayStartMs = todayStart.getTimeInMillis();

                Calendar todayEnd = Calendar.getInstance();
                todayEnd.set(Calendar.HOUR_OF_DAY, 23);
                todayEnd.set(Calendar.MINUTE, 59);
                todayEnd.set(Calendar.SECOND, 59);
                todayEnd.set(Calendar.MILLISECOND, 999);
                long todayEndMs = todayEnd.getTimeInMillis();

                int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                int workingDays = user.getWorkingDays() > 0 ? user.getWorkingDays() : 5;
                boolean isWorkingDayByWeek = (dayOfWeek != Calendar.SUNDAY) && (workingDays >= 6 || dayOfWeek != Calendar.SATURDAY);

                String todayDateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

                // 1. Holiday Check
                List<HolidaySchema> holidays = Database.getInstance().getHolidayDao().getHolidaysSync();
                HolidaySchema todayHoliday = null;
                if (holidays != null) {
                    for (HolidaySchema h : holidays) {
                        if (h.getStartDate() <= todayEndMs && h.getEndDate() >= todayStartMs) {
                            todayHoliday = h;
                            break;
                        }
                    }
                }

                if (todayHoliday != null) {
                    // It's a holiday today!
                    // If it is a working day by week, mark scheduled classes as CANCELLED for today
                    if (isWorkingDayByWeek) {
                        String dayName = AttendanceScheduleUtil.getDayNameFromDayOfWeek(dayOfWeek);
                        List<TimeTableDaySchema> days = Database.getInstance().getTimeTableDayDao().getDays();
                        int did = -1;
                        if (days != null) {
                            for (TimeTableDaySchema d : days) {
                                if (d.getDayName() != null && d.getDayName().equalsIgnoreCase(dayName)) {
                                    did = d.getDid();
                                    break;
                                }
                            }
                        }

                        if (did != -1) {
                            List<DayScheduleSchema> schedules = Database.getInstance().getDayScheduleDao().getAllSchedules();
                            DayScheduleSchema todaySchedule = null;
                            if (schedules != null) {
                                for (DayScheduleSchema s : schedules) {
                                    if (s.getDid() == did) {
                                        todaySchedule = s;
                                        break;
                                    }
                                }
                            }

                            if (todaySchedule != null) {
                                Integer[] periods = new Integer[]{
                                        todaySchedule.getP1(), todaySchedule.getP2(), todaySchedule.getP3(),
                                        todaySchedule.getP4(), todaySchedule.getP5(), todaySchedule.getP6(),
                                        todaySchedule.getP7(), todaySchedule.getP8(), todaySchedule.getP9()
                                };

                                // Holiday notification only; do not mutate official ERP attendance records
                            }
                        }
                    }

                    if (SharedPrefUtil.isHolidayAlertEnabled()) {
                        String eventName = todayHoliday.getEventName() != null ? todayHoliday.getEventName() : "Holiday";
                        NotificationHelper.showHolidayAlert(
                                context,
                                "Holiday: " + eventName,
                                "Today is a holiday (" + eventName + "). Classes are cancelled. Enjoy your day!"
                        );
                    }
                    AttendanceWidgetProvider.refreshAllWidgets(context);

                } else if (isWorkingDayByWeek) {
                    // Not a holiday & is working day - Attendance reminders removed as requested
                }

                // 2. Assignment Due Reminders
                if (SharedPrefUtil.isAssignmentReminderEnabled()) {
                    List<AssignmentSchema> assignments = Database.getInstance().getAssignmentDao().getAssignmentsSync();
                    if (assignments != null) {
                        for (AssignmentSchema a : assignments) {
                            if (a.isSubmitted()) continue;

                            Calendar dueCal = Calendar.getInstance();
                            dueCal.setTimeInMillis(a.getDueDate());
                            dueCal.set(Calendar.HOUR_OF_DAY, 0);
                            dueCal.set(Calendar.MINUTE, 0);
                            dueCal.set(Calendar.SECOND, 0);
                            dueCal.set(Calendar.MILLISECOND, 0);

                            long diffMs = dueCal.getTimeInMillis() - todayStartMs;
                            int daysRemaining = (int) Math.round((double) diffMs / (1000 * 60 * 60 * 24));

                            String reminderSchedule = a.getReminderSchedule() != null ? a.getReminderSchedule().toUpperCase(Locale.US) : "T_1";

                            boolean shouldNotify = false;
                            String message = "";

                            if (daysRemaining == 0) {
                                // Due today
                                shouldNotify = true;
                                message = "Due today: " + a.getAssignmentTitle() + " (" + (a.getSubjectName() != null ? a.getSubjectName() : "Assignment") + ")";
                            } else if (daysRemaining == 1) {
                                // 1 day before
                                if (reminderSchedule.contains("T_1") || reminderSchedule.contains("T_2") || "1".equals(reminderSchedule)) {
                                    shouldNotify = true;
                                    message = "Due tomorrow: " + a.getAssignmentTitle() + " (" + (a.getSubjectName() != null ? a.getSubjectName() : "Assignment") + ")";
                                }
                            } else if (daysRemaining == 2) {
                                // 2 days before
                                if (reminderSchedule.contains("T_2") || "2".equals(reminderSchedule)) {
                                    shouldNotify = true;
                                    message = "Due in 2 days: " + a.getAssignmentTitle() + " (" + (a.getSubjectName() != null ? a.getSubjectName() : "Assignment") + ")";
                                }
                            }

                            if (shouldNotify) {
                                NotificationHelper.showAssignmentReminder(
                                        context,
                                        NotificationHelper.NOTIFICATION_ID_ASSIGNMENT_BASE + a.getAid(),
                                        "Assignment Reminder",
                                        message
                                );
                            }
                        }
                    }
                }

            } catch (Exception ignored) {
            }
        });
    }
}
