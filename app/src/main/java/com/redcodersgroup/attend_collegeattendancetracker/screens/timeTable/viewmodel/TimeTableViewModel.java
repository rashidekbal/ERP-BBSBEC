package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimeTableViewModel extends AndroidViewModel {
    private final LiveData<List<TimeTableDaySchema>> daysLiveData;
    private final LiveData<List<DayScheduleSchema>> schedulesLiveData;
    private final LiveData<List<SubjectSchema>> subjectsLiveData;
    private final LiveData<UserSchema> userLiveData;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static final int PERIOD_DURATION_MINUTES = 50;
    public static final int TOTAL_PERIODS = 9;

    public interface PeriodAssignmentCallback {
        void onSuccess();
        void onLimitExceeded(int maxAllowed, int currentCount, boolean isLab);
    }

    public TimeTableViewModel(@NonNull Application application) {
        super(application);
        Database db = Database.getInstance();
        daysLiveData = db.getTimeTableDayDao().getDaysLiveData();
        schedulesLiveData = db.getDayScheduleDao().getAllSchedulesLiveData();
        subjectsLiveData = db.getSubjectDao().getSubjects();
        userLiveData = db.getUserDao().getUserLiveData();

        ensureDaysAndSchedulesExist();
    }

    public LiveData<List<TimeTableDaySchema>> getDaysLiveData() {
        return daysLiveData;
    }

    public LiveData<List<DayScheduleSchema>> getSchedulesLiveData() {
        return schedulesLiveData;
    }

    public LiveData<List<SubjectSchema>> getSubjectsLiveData() {
        return subjectsLiveData;
    }

    public LiveData<UserSchema> getUserLiveData() {
        return userLiveData;
    }

    public void ensureDaysAndSchedulesExist() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema user = db.getUserDao().getUser();
            int workingDays = user != null && user.getWorkingDays() > 0 ? user.getWorkingDays() : 5;

            List<TimeTableDaySchema> existingDays = db.getTimeTableDayDao().getDays();
            if (existingDays == null || existingDays.isEmpty()) {
                List<String> dayNames = (workingDays == 6)
                        ? List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                        : List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

                List<TimeTableDaySchema> newDays = new ArrayList<>();
                for (String name : dayNames) {
                    newDays.add(new TimeTableDaySchema(name));
                }
                db.getTimeTableDayDao().insertMulti(newDays);
                existingDays = db.getTimeTableDayDao().getDays();
            }

            for (TimeTableDaySchema day : existingDays) {
                DayScheduleSchema schedule = db.getDayScheduleDao().getScheduleByDid(day.getDid());
                if (schedule == null) {
                    db.getDayScheduleDao().insert(new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null));
                }
            }
        });
    }

    public void updatePeriodSubject(int did, int periodNumber, Integer sid, Runnable onComplete) {
        updatePeriodSubjectWithValidation(did, periodNumber, sid, new PeriodAssignmentCallback() {
            @Override
            public void onSuccess() {
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onLimitExceeded(int maxAllowed, int currentCount, boolean isLab) {
                // If caller used basic method, still complete or ignore
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    public void updatePeriodSubjectWithValidation(int did, int periodNumber, Integer sid, PeriodAssignmentCallback callback) {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            DayScheduleSchema schedule = db.getDayScheduleDao().getScheduleByDid(did);

            if (sid == null) {
                if (schedule != null) {
                    setPeriodValue(schedule, periodNumber, null);
                    db.getDayScheduleDao().update(schedule);
                }
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
                return;
            }

            SubjectSchema subject = db.getSubjectDao().getSubjectById(sid);
            if (subject == null) {
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
                return;
            }

            boolean isLab = subject.isLab();
            int maxSlots = isLab
                    ? (subject.getLabsPerWeek() > 0 ? subject.getLabsPerWeek() : subject.getClassesPerWeek())
                    : (subject.getClassesPerWeek() > 0 ? subject.getClassesPerWeek() : 1);
            if (maxSlots <= 0) maxSlots = 1;

            // Check if this slot already has this subject assigned
            Integer existingSid = schedule != null ? getPeriodValue(schedule, periodNumber) : null;
            boolean isAlreadyAssignedHere = existingSid != null && existingSid.equals(sid);

            if (!isAlreadyAssignedHere) {
                List<DayScheduleSchema> allSchedules = db.getDayScheduleDao().getAllSchedules();
                int currentBooked = 0;
                if (allSchedules != null) {
                    for (DayScheduleSchema s : allSchedules) {
                        for (int p = 1; p <= TOTAL_PERIODS; p++) {
                            Integer val = getPeriodValue(s, p);
                            if (val != null && val.equals(sid)) {
                                currentBooked++;
                            }
                        }
                    }
                }

                if (currentBooked >= maxSlots) {
                    int finalMax = maxSlots;
                    int finalCount = currentBooked;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onLimitExceeded(finalMax, finalCount, isLab));
                    }
                    return;
                }
            }

            if (schedule == null) {
                schedule = new DayScheduleSchema(did, null, null, null, null, null, null, null, null, null);
                setPeriodValue(schedule, periodNumber, sid);
                db.getDayScheduleDao().insert(schedule);
            } else {
                setPeriodValue(schedule, periodNumber, sid);
                db.getDayScheduleDao().update(schedule);
            }

            if (callback != null) {
                mainHandler.post(callback::onSuccess);
            }
        });
    }

    public static Integer getPeriodValue(DayScheduleSchema schedule, int periodNumber) {
        if (schedule == null) return null;
        switch (periodNumber) {
            case 1: return schedule.getP1();
            case 2: return schedule.getP2();
            case 3: return schedule.getP3();
            case 4: return schedule.getP4();
            case 5: return schedule.getP5();
            case 6: return schedule.getP6();
            case 7: return schedule.getP7();
            case 8: return schedule.getP8();
            case 9: return schedule.getP9();
            default: return null;
        }
    }

    public static void setPeriodValue(DayScheduleSchema schedule, int periodNumber, Integer value) {
        if (schedule == null) return;
        switch (periodNumber) {
            case 1: schedule.setP1(value); break;
            case 2: schedule.setP2(value); break;
            case 3: schedule.setP3(value); break;
            case 4: schedule.setP4(value); break;
            case 5: schedule.setP5(value); break;
            case 6: schedule.setP6(value); break;
            case 7: schedule.setP7(value); break;
            case 8: schedule.setP8(value); break;
            case 9: schedule.setP9(value); break;
        }
    }

    public static String getPeriodTimeRange(long firstPeriodStartTimeMs, int periodIndex, int durationMinutes) {
        Calendar cal = Calendar.getInstance();
        if (firstPeriodStartTimeMs > 0) {
            cal.setTimeInMillis(firstPeriodStartTimeMs);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }

        cal.add(Calendar.MINUTE, periodIndex * durationMinutes);
        int startHour = cal.get(Calendar.HOUR_OF_DAY);
        int startMin = cal.get(Calendar.MINUTE);

        cal.add(Calendar.MINUTE, durationMinutes);
        int endHour = cal.get(Calendar.HOUR_OF_DAY);
        int endMin = cal.get(Calendar.MINUTE);

        return formatTime(startHour, startMin) + " - " + formatTime(endHour, endMin);
    }

    private static String formatTime(int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm);
    }
}
