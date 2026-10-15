package com.redcodersgroup.attend_collegeattendancetracker.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.content.ContextCompat;

import com.redcodersgroup.attend_collegeattendancetracker.R;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.model.TodayClassModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AttendanceWidgetFactory(getApplicationContext());
    }

    public static class AttendanceWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

        private final Context context;
        private final List<TodayClassModel> classList = new ArrayList<>();

        public AttendanceWidgetFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            loadData();
        }

        @Override
        public void onDataSetChanged() {
            loadData();
        }

        private void loadData() {
            classList.clear();
            try {
                Database db = Database.getInstance();
                UserSchema currentUser = db.getUserDao().getUser();
                List<SubjectSchema> subjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
                List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
                List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

                List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
                long now = System.currentTimeMillis();

                // If today is a holiday, college is off - show empty state
                if (AttendanceScheduleUtil.isHoliday(holidays, now)) {
                    return;
                }

                String todayDayName = new SimpleDateFormat("EEEE", Locale.US).format(new Date());
                String todayDateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

                TimeTableDaySchema todayDay = null;
                if (days != null) {
                    for (TimeTableDaySchema d : days) {
                        if (d.getDayName() != null && d.getDayName().equalsIgnoreCase(todayDayName)) {
                            todayDay = d;
                            break;
                        }
                    }
                }

                DayScheduleSchema todaySchedule = null;
                if (todayDay != null && schedules != null) {
                    for (DayScheduleSchema s : schedules) {
                        if (s.getDid() == todayDay.getDid()) {
                            todaySchedule = s;
                            break;
                        }
                    }
                }

                Map<Integer, SubjectSchema> subjectMap = new HashMap<>();
                if (subjects != null) {
                    for (SubjectSchema s : subjects) {
                        subjectMap.put(s.getSid(), s);
                    }
                }

                int durationMinutes = SharedPrefUtil.getPeriodLength();
                long startTime = currentUser != null ? currentUser.getFirstPeriodStartTime() : 0;

                if (todaySchedule != null) {
                    for (int p = 1; p <= TimeTableViewModel.TOTAL_PERIODS; p++) {
                        Integer sid = TimeTableViewModel.getPeriodValue(todaySchedule, p);
                        if (sid != null && sid > 0) {
                            SubjectSchema sub = subjectMap.get(sid);
                            if (sub != null) {
                                boolean isLab = sub.isLab();
                                String timeRange = "Period " + p + (isLab ? " (Lab)" : "") + " · "
                                        + TimeTableViewModel.getPeriodTimeRange(startTime, p - 1, durationMinutes);
                                AttendanceSchema att = db.getAttendanceDao().getAttendanceRecord(sid, todayDateStr, isLab);
                                String status = att != null ? att.getStatus() : null;

                                classList.add(new TodayClassModel(
                                        p,
                                        timeRange,
                                        sid,
                                        isLab,
                                        sub.getSubjectName(),
                                        sub.getUniqueColorCode(),
                                        status,
                                        sub
                                ));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDestroy() {
            classList.clear();
        }

        @Override
        public int getCount() {
            return classList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= classList.size()) {
                return null;
            }

            TodayClassModel item = classList.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item_today_class);

            // Set Title & Time Range
            String displayTitle = item.getSubjectName() != null ? item.getSubjectName() : "Subject";
            if (item.isLab()) {
                displayTitle += " (Lab)";
            }
            views.setTextViewText(R.id.widget_item_title, displayTitle);

            // Set Faculty & Room
            SubjectSchema sub = item.getSubject();
            String faculty = (sub != null && sub.getTeacherName() != null) ? sub.getTeacherName().trim() : null;
            String room = (sub != null && sub.getRoomNo() != null) ? sub.getRoomNo().trim() : null;

            StringBuilder facultyRoom = new StringBuilder();
            if (faculty != null && !faculty.isEmpty()) {
                facultyRoom.append(faculty);
            }
            if (room != null && !room.isEmpty()) {
                if (facultyRoom.length() > 0) facultyRoom.append(" · Room ");
                else facultyRoom.append("Room ");
                facultyRoom.append(room);
            }

            if (facultyRoom.length() > 0) {
                views.setTextViewText(R.id.widget_item_faculty_room, facultyRoom.toString());
                views.setViewVisibility(R.id.widget_item_faculty_room, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_item_faculty_room, android.view.View.GONE);
            }

            views.setTextViewText(R.id.widget_item_time, item.getTimeRange() != null ? item.getTimeRange() : "");

            // Set Subject Color Bar
            int color = ThemeManager.getAccentColorInt();
            if (item.getColorCode() != null && !item.getColorCode().isEmpty()) {
                try {
                    color = Color.parseColor(item.getColorCode());
                } catch (Exception ignored) {
                }
            }
            views.setInt(R.id.widget_item_color_bar, "setBackgroundColor", color);

            // Status Text
            String status = item.getStatus() != null ? item.getStatus().toUpperCase(Locale.US) : "";
            switch (status) {
                case "PRESENT":
                    views.setTextViewText(R.id.widget_item_status_text, "Present");
                    views.setTextColor(R.id.widget_item_status_text, ContextCompat.getColor(context, R.color.success));
                    break;
                case "ABSENT":
                case "MISSED":
                    views.setTextViewText(R.id.widget_item_status_text, "Absent");
                    views.setTextColor(R.id.widget_item_status_text, ContextCompat.getColor(context, R.color.danger));
                    break;
                case "CANCELLED":
                    views.setTextViewText(R.id.widget_item_status_text, "Cancelled");
                    views.setTextColor(R.id.widget_item_status_text, ContextCompat.getColor(context, R.color.text_dim));
                    break;
                default:
                    views.setTextViewText(R.id.widget_item_status_text, "Scheduled");
                    views.setTextColor(R.id.widget_item_status_text, ContextCompat.getColor(context, R.color.text_dim));
                    break;
            }

            // Fill-in intent for container tap (opens app)
            Intent fillContainer = new Intent();
            views.setOnClickFillInIntent(R.id.widget_item_root, fillContainer);

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
