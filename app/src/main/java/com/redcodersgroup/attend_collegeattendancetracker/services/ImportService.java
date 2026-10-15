package com.redcodersgroup.attend_collegeattendancetracker.services;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportService {

    public interface ImportCallback {
        void onSuccess();
        void onError(String message);
    }

    private static final int SUPPORTED_VERSION = 1;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void importData(Context context, Uri fileUri, ImportCallback callback) {
        executorService.execute(() -> {
            try {
                String json = readFromUri(context, fileUri);
                JSONObject root = new JSONObject(json);

                int version = root.optInt("version", -1);
                if (version != SUPPORTED_VERSION) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Unsupported backup version: " + version));
                    return;
                }

                // Validate the db key exists BEFORE wiping anything
                if (!root.has("db")) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Invalid backup file: missing 'db' section"));
                    return;
                }
                JSONObject dbObj = root.getJSONObject("db");

                restoreDatabase(dbObj);
                restoreSharedPrefs(root.optJSONObject("sharedPrefs"));

                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Unknown error during import"));
            }
        });
    }

    public static void importDataFromJsonString(String json, ImportCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject root = new JSONObject(json);

                int version = root.optInt("version", -1);
                if (version != SUPPORTED_VERSION) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Unsupported backup version: " + version));
                    return;
                }

                if (!root.has("db")) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Invalid backup file: missing 'db' section"));
                    return;
                }
                JSONObject dbObj = root.getJSONObject("db");

                restoreDatabase(dbObj);
                restoreSharedPrefs(root.optJSONObject("sharedPrefs"));

                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Unknown error during import"));
            }
        });
    }

    private static String readFromUri(Context context, Uri uri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) throw new Exception("Cannot open file: ContentResolver returned null stream");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    /** Returns null if the JSON field is absent or JSON null; otherwise the String value. */
    private static String optNullableString(JSONObject obj, String key) {
        if (obj.isNull(key)) return null;
        String val = obj.optString(key, null);
        return (val == null || val.equals("null")) ? null : val;
    }

    private static void restoreDatabase(JSONObject dbObj) throws Exception {
        Database db = Database.getInstance();

        // Clear order matters — respect FK constraints
        // attendance/assignments/marks reference subject -> clear them first
        db.getAttendanceDao().deleteAll();
        db.getAssignmentDao().deleteAll();
        db.getMarksDao().deleteAll();
        db.getSubjectAdditionalInfoDao().deleteAll();
        // daySchedule references timeTable_day and subject
        db.getDayScheduleDao().deleteAll();
        // timeTable_day standalone
        db.getTimeTableDayDao().deleteAll();
        // subject standalone
        db.getSubjectDao().deleteAll();
        // user standalone
        db.getUserDao().deleteAll();
        // holiday standalone
        db.getHolidayDao().deleteAll();

        // Restore users
        JSONArray users = dbObj.optJSONArray("users");
        if (users != null) {
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                UserSchema schema = new UserSchema(
                        u.getInt("uid"),
                        optNullableString(u, "name"),
                        optNullableString(u, "collegeName"),
                        optNullableString(u, "department"),
                        optNullableString(u, "branch"),
                        u.optInt("semester", 1),
                        u.optLong("semStartDate", 0),
                        u.optLong("semEndDate", 0),
                        u.optInt("workingDays", 5),
                        u.optLong("firstPeriodStartTime", 0),
                        u.optInt("attendanceCriteria", 75),
                        optNullableString(u, "themeColor") != null ? optNullableString(u, "themeColor") : "#F5A623",
                        u.optInt("maxMarks", 24),
                        optNullableString(u, "rollNumber"),
                        optNullableString(u, "phone"),
                        optNullableString(u, "email")
                );
                db.getUserDao().addUser(schema);
            }
        }

        // Restore subjects (must be before attendance/marks/assignments)
        JSONArray subjects = dbObj.optJSONArray("subjects");
        if (subjects != null) {
            for (int i = 0; i < subjects.length(); i++) {
                JSONObject s = subjects.getJSONObject(i);
                SubjectSchema schema = new SubjectSchema(
                        s.getInt("sid"),
                        optNullableString(s, "subjectName") != null ? optNullableString(s, "subjectName") : "",
                        s.optBoolean("isLab", false),
                        s.optInt("labsPerWeek", 0),
                        s.optInt("classesPerWeek", 0),
                        s.optInt("attendanceCriteria", 75),
                        optNullableString(s, "uniqueColorCode") != null ? optNullableString(s, "uniqueColorCode") : "#F5A623",
                        s.optBoolean("timeTableSetupComplete", false),
                        s.optBoolean("attendanceSetupComplete", false)
                );
                db.getSubjectDao().addSubject(schema);
            }
        }

        // Restore attendance
        JSONArray attendance = dbObj.optJSONArray("attendance");
        if (attendance != null) {
            for (int i = 0; i < attendance.length(); i++) {
                JSONObject a = attendance.getJSONObject(i);
                AttendanceSchema schema = new AttendanceSchema(
                        a.getInt("aid"),
                        a.getInt("sid"),
                        a.optString("date", ""),
                        a.optString("status", ""),
                        a.optBoolean("isLab", false)
                );
                db.getAttendanceDao().addAttendance(schema);
            }
        }

        // Restore assignments
        JSONArray assignments = dbObj.optJSONArray("assignments");
        if (assignments != null) {
            for (int i = 0; i < assignments.length(); i++) {
                JSONObject a = assignments.getJSONObject(i);
                AssignmentSchema schema = new AssignmentSchema(
                        a.getInt("aid"),
                        a.getInt("sid"),
                        optNullableString(a, "assignmentTitle") != null ? optNullableString(a, "assignmentTitle") : "",
                        optNullableString(a, "subjectName") != null ? optNullableString(a, "subjectName") : "",
                        optNullableString(a, "assignmentDesc"),
                        a.optLong("dueDate", 0),
                        optNullableString(a, "reminderSchedule"),
                        a.optBoolean("submitted", false)
                );
                db.getAssignmentDao().addAssignment(schema);
            }
        }

        // Restore marks
        JSONArray marks = dbObj.optJSONArray("marks");
        if (marks != null) {
            for (int i = 0; i < marks.length(); i++) {
                JSONObject m = marks.getJSONObject(i);
                MarksSchema schema = new MarksSchema(
                        m.getInt("marksId"),
                        m.getInt("sid"),
                        m.optString("subjectName", ""),
                        m.optDouble("mid1", -1),
                        m.optDouble("mid2", -1),
                        m.optDouble("mid3", -1)
                );
                db.getMarksDao().addMarks(schema);
            }
        }

        // Restore holidays
        JSONArray holidays = dbObj.optJSONArray("holidays");
        if (holidays != null) {
            for (int i = 0; i < holidays.length(); i++) {
                JSONObject h = holidays.getJSONObject(i);
                HolidaySchema schema = new HolidaySchema(
                        h.getInt("hid"),
                        optNullableString(h, "eventName") != null ? optNullableString(h, "eventName") : "",
                        h.optLong("startDate", 0),
                        h.optLong("endDate", 0),
                        optNullableString(h, "reminderSchedule")
                );
                db.getHolidayDao().addHoliday(schema);
            }
        }

        // Restore timetable days
        JSONArray ttDays = dbObj.optJSONArray("timeTableDays");
        if (ttDays != null) {
            for (int i = 0; i < ttDays.length(); i++) {
                JSONObject d = ttDays.getJSONObject(i);
                TimeTableDaySchema schema = new TimeTableDaySchema(
                        d.getInt("did"),
                        d.optString("dayName", "")
                );
                db.getTimeTableDayDao().insert(schema);
            }
        }

        // Restore day schedules
        JSONArray schedules = dbObj.optJSONArray("daySchedules");
        if (schedules != null) {
            for (int i = 0; i < schedules.length(); i++) {
                JSONObject s = schedules.getJSONObject(i);
                DayScheduleSchema schema = new DayScheduleSchema(
                        s.getInt("scheduleId"),
                        s.getInt("did"),
                        s.isNull("p1") ? null : s.getInt("p1"),
                        s.isNull("p2") ? null : s.getInt("p2"),
                        s.isNull("p3") ? null : s.getInt("p3"),
                        s.isNull("p4") ? null : s.getInt("p4"),
                        s.isNull("p5") ? null : s.getInt("p5"),
                        s.isNull("p6") ? null : s.getInt("p6"),
                        s.isNull("p7") ? null : s.getInt("p7"),
                        s.isNull("p8") ? null : s.getInt("p8"),
                        s.isNull("p9") ? null : s.getInt("p9")
                );
                db.getDayScheduleDao().insert(schema);
            }
        }

        // Restore subject additional info
        JSONArray additionalInfos = dbObj.optJSONArray("subjectAdditionalInfo");
        if (additionalInfos != null) {
            for (int i = 0; i < additionalInfos.length(); i++) {
                JSONObject info = additionalInfos.getJSONObject(i);
                SubjectAdditionalInfoSchema schema = new SubjectAdditionalInfoSchema(
                        info.getInt("said"),
                        info.getInt("sid"),
                        info.optInt("totalClasses", 0),
                        info.optInt("totalLabs", 0)
                );
                db.getSubjectAdditionalInfoDao().insert(schema);
            }
        }
    }

    private static void restoreSharedPrefs(JSONObject prefs) {
        if (prefs == null) return;
        try {
            android.content.SharedPreferences.Editor editor = SharedPref.getSharedPrefs().edit();
            java.util.Iterator<String> keys = prefs.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = prefs.get(key);
                if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                else if (val instanceof Integer) editor.putInt(key, (Integer) val);
                else if (val instanceof Long) editor.putLong(key, (Long) val);
                else if (val instanceof Double) editor.putFloat(key, ((Double) val).floatValue());
                else if (val instanceof String) editor.putString(key, (String) val);
            }
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
