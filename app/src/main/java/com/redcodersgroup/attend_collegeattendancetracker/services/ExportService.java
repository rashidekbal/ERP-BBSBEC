package com.redcodersgroup.attend_collegeattendancetracker.services;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import com.redcodersgroup.attend_collegeattendancetracker.core.SharedPref;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectAdditionalInfoSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportService {

    public interface ExportCallback {
        void onSuccess(Uri fileUri, String fileName);
        void onError(String message);
    }

    private static final int BACKUP_VERSION = 1;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void exportData(Context context, ExportCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject root = buildExportJson();
                String jsonString = root.toString(2);
                String fileName = "attend_backup_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
                Uri fileUri = writeToDownloads(context, jsonString, fileName);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(fileUri, fileName));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static String buildExportJsonString() throws Exception {
        return buildExportJson().toString(2);
    }

    public static void exportDataToUri(Context context, Uri targetUri, ExportCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject root = buildExportJson();
                try (OutputStream os = context.getContentResolver().openOutputStream(targetUri)) {
                    if (os == null) throw new Exception("Failed to open output stream for selected location");
                    os.write(root.toString(2).getBytes("UTF-8"));
                    os.flush();
                }
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(targetUri, "attend_backup.json"));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Export error"));
            }
        });
    }

    private static JSONObject buildExportJson() throws Exception {
        Database db = Database.getInstance();
        JSONObject root = new JSONObject();
        root.put("version", BACKUP_VERSION);
        root.put("exportedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

        // --- SharedPreferences ---
        JSONObject prefs = new JSONObject();
        Map<String, ?> allPrefs = SharedPref.getSharedPrefs().getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Boolean) prefs.put(entry.getKey(), (boolean) val);
            else if (val instanceof Integer) prefs.put(entry.getKey(), (int) val);
            else if (val instanceof Long) prefs.put(entry.getKey(), (long) val);
            else if (val instanceof Float) prefs.put(entry.getKey(), (float) val);
            else if (val instanceof String) prefs.put(entry.getKey(), (String) val);
        }
        root.put("sharedPrefs", prefs);

        // --- Database ---
        JSONObject dbObj = new JSONObject();

        // users
        JSONArray users = new JSONArray();
        UserSchema user = db.getUserDao().getUser();
        if (user != null) {
            JSONObject u = new JSONObject();
            u.put("uid", user.getUid());
            u.put("name", user.getName() != null ? user.getName() : JSONObject.NULL);
            u.put("collegeName", user.getCollegeName() != null ? user.getCollegeName() : JSONObject.NULL);
            u.put("department", user.getDepartment() != null ? user.getDepartment() : JSONObject.NULL);
            u.put("branch", user.getBranch() != null ? user.getBranch() : JSONObject.NULL);
            u.put("semester", user.getSemester());
            u.put("semStartDate", user.getSemStartDate());
            u.put("semEndDate", user.getSemEndDate());
            u.put("workingDays", user.getWorkingDays());
            u.put("firstPeriodStartTime", user.getFirstPeriodStartTime());
            u.put("attendanceCriteria", user.getAttendanceCriteria());
            u.put("themeColor", user.getThemeColor() != null ? user.getThemeColor() : JSONObject.NULL);
            u.put("maxMarks", user.getMaxMarks());
            u.put("rollNumber", user.getRollNumber() != null ? user.getRollNumber() : JSONObject.NULL);
            u.put("phone", user.getPhone() != null ? user.getPhone() : JSONObject.NULL);
            u.put("email", user.getEmail() != null ? user.getEmail() : JSONObject.NULL);
            users.put(u);
        }
        dbObj.put("users", users);

        // subjects
        JSONArray subjects = new JSONArray();
        List<SubjectSchema> subjectList = db.getSubjectDao().getSubjectsSync();
        if (subjectList != null) {
            for (SubjectSchema s : subjectList) {
                JSONObject sj = new JSONObject();
                sj.put("sid", s.getSid());
                sj.put("subjectName", s.getSubjectName() != null ? s.getSubjectName() : JSONObject.NULL);
                sj.put("isLab", s.isLab());
                sj.put("classesPerWeek", s.getClassesPerWeek());
                sj.put("labsPerWeek", s.getLabsPerWeek());
                sj.put("attendanceCriteria", s.getAttendanceCriteria());
                sj.put("uniqueColorCode", s.getUniqueColorCode() != null ? s.getUniqueColorCode() : JSONObject.NULL);
                sj.put("timeTableSetupComplete", s.isTimeTableSetUpComplete());
                sj.put("attendanceSetupComplete", s.isAttendanceSetUpComplete());
                subjects.put(sj);
            }
        }
        dbObj.put("subjects", subjects);

        // attendance
        JSONArray attendance = new JSONArray();
        List<AttendanceSchema> attendanceList = db.getAttendanceDao().getAllAttendanceSync();
        if (attendanceList != null) {
            for (AttendanceSchema a : attendanceList) {
                JSONObject aj = new JSONObject();
                aj.put("aid", a.getAid());
                aj.put("sid", a.getSid());
                aj.put("date", a.getDate());
                aj.put("status", a.getStatus());
                aj.put("isLab", a.isLab());
                attendance.put(aj);
            }
        }
        dbObj.put("attendance", attendance);

        // assignments
        JSONArray assignments = new JSONArray();
        List<AssignmentSchema> assignmentList = db.getAssignmentDao().getAssignmentsSync();
        if (assignmentList != null) {
            for (AssignmentSchema a : assignmentList) {
                JSONObject aj = new JSONObject();
                aj.put("aid", a.getAid());
                aj.put("sid", a.getSid());
                aj.put("subjectName", a.getSubjectName() != null ? a.getSubjectName() : JSONObject.NULL);
                aj.put("assignmentTitle", a.getAssignmentTitle() != null ? a.getAssignmentTitle() : JSONObject.NULL);
                aj.put("assignmentDesc", a.getAssignmentDesc() != null ? a.getAssignmentDesc() : JSONObject.NULL);
                aj.put("dueDate", a.getDueDate());
                aj.put("reminderSchedule", a.getReminderSchedule() != null ? a.getReminderSchedule() : JSONObject.NULL);
                aj.put("submitted", a.isSubmitted());
                assignments.put(aj);
            }
        }
        dbObj.put("assignments", assignments);

        // marks
        JSONArray marks = new JSONArray();
        List<MarksSchema> marksList = db.getMarksDao().getAllMarksSync();
        if (marksList != null) {
            for (MarksSchema m : marksList) {
                JSONObject mj = new JSONObject();
                mj.put("marksId", m.getMarksId());
                mj.put("sid", m.getSid());
                mj.put("subjectName", m.getSubjectName());
                mj.put("mid1", m.getMid1());
                mj.put("mid2", m.getMid2());
                mj.put("mid3", m.getMid3());
                marks.put(mj);
            }
        }
        dbObj.put("marks", marks);

        // holidays
        JSONArray holidays = new JSONArray();
        List<HolidaySchema> holidayList = db.getHolidayDao().getHolidaysSync();
        if (holidayList != null) {
            for (HolidaySchema h : holidayList) {
                JSONObject hj = new JSONObject();
                hj.put("hid", h.getHid());
                hj.put("eventName", h.getEventName() != null ? h.getEventName() : JSONObject.NULL);
                hj.put("startDate", h.getStartDate());
                hj.put("endDate", h.getEndDate());
                hj.put("reminderSchedule", h.getReminderSchedule() != null ? h.getReminderSchedule() : JSONObject.NULL);
                holidays.put(hj);
            }
        }
        dbObj.put("holidays", holidays);

        // timeTableDays
        JSONArray ttDays = new JSONArray();
        List<TimeTableDaySchema> dayList = db.getTimeTableDayDao().getDays();
        if (dayList != null) {
            for (TimeTableDaySchema d : dayList) {
                JSONObject dj = new JSONObject();
                dj.put("did", d.getDid());
                dj.put("dayName", d.getDayName());
                ttDays.put(dj);
            }
        }
        dbObj.put("timeTableDays", ttDays);

        // daySchedules
        JSONArray schedules = new JSONArray();
        List<DayScheduleSchema> scheduleList = db.getDayScheduleDao().getAllSchedules();
        if (scheduleList != null) {
            for (DayScheduleSchema s : scheduleList) {
                JSONObject sj = new JSONObject();
                sj.put("scheduleId", s.getScheduleId());
                sj.put("did", s.getDid());
                sj.put("p1", s.getP1() != null ? s.getP1() : JSONObject.NULL);
                sj.put("p2", s.getP2() != null ? s.getP2() : JSONObject.NULL);
                sj.put("p3", s.getP3() != null ? s.getP3() : JSONObject.NULL);
                sj.put("p4", s.getP4() != null ? s.getP4() : JSONObject.NULL);
                sj.put("p5", s.getP5() != null ? s.getP5() : JSONObject.NULL);
                sj.put("p6", s.getP6() != null ? s.getP6() : JSONObject.NULL);
                sj.put("p7", s.getP7() != null ? s.getP7() : JSONObject.NULL);
                sj.put("p8", s.getP8() != null ? s.getP8() : JSONObject.NULL);
                sj.put("p9", s.getP9() != null ? s.getP9() : JSONObject.NULL);
                schedules.put(sj);
            }
        }
        dbObj.put("daySchedules", schedules);

        // subjectAdditionalInfo
        JSONArray additionalInfos = new JSONArray();
        List<SubjectAdditionalInfoSchema> infoList = db.getSubjectAdditionalInfoDao().getAllSync();
        if (infoList != null) {
            for (SubjectAdditionalInfoSchema info : infoList) {
                JSONObject ij = new JSONObject();
                ij.put("said", info.getSaid());
                ij.put("sid", info.getSid());
                ij.put("totalClasses", info.getTotalClasses());
                ij.put("totalLabs", info.getTotalLabs());
                additionalInfos.put(ij);
            }
        }
        dbObj.put("subjectAdditionalInfo", additionalInfos);

        root.put("db", dbObj);
        return root;
    }

    private static Uri writeToDownloads(Context context, String content, String fileName) throws Exception {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Attend Backups");
            uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Failed to create file in Downloads");
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    context.getContentResolver().delete(uri, null, null);
                    throw new Exception("Failed to open output stream for writing");
                }
                os.write(content.getBytes("UTF-8"));
                os.flush();
            }
        } else {
            // Fallback for API < 29 (minSdk is 31 so this is dead code, kept for safety)
            throw new Exception("Unsupported Android version");
        }
        return uri;
    }
}
