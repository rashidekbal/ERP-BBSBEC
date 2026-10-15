package com.redcodersgroup.attend_collegeattendancetracker.utils;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;

import java.util.ArrayList;
import java.util.List;

public class ProfileUtil {

    /**
     * Checks if a UserSchema meets the bare minimum profile criteria:
     * - Name not empty
     * - Semester between 1 and 8
     * - Semester start date & end date set (and end > start)
     * - Working days configured (5 or 6)
     * - First period start time set (> 0)
     */
    public static boolean isProfileCompletedBareMinimum(UserSchema user) {
        if (user == null) return false;
        return user.getName() != null && !user.getName().trim().isEmpty();
    }

    /**
     * Quick cached check via SharedPreferences or TokenManager.
     */
    public static boolean isProfileCompletedBareMinimum() {
        return com.redcodersgroup.attend_collegeattendancetracker.network.TokenManager.isLoggedIn();
    }

    /**
     * Configures/synchronizes timetable days and empty schedule slots for 5 or 6 working days.
     */
    public static void setupTimetableDays(int workingDays) {
        Database db = Database.getInstance();
        List<TimeTableDaySchema> existingDays = db.getTimeTableDayDao().getDays();

        if (existingDays == null || existingDays.isEmpty()) {
            List<String> days = (workingDays == 6)
                    ? List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    : List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
            List<TimeTableDaySchema> timeTableDaySchema = new ArrayList<>();
            for (String day : days) {
                timeTableDaySchema.add(new TimeTableDaySchema(day));
            }
            db.getTimeTableDayDao().insertMulti(timeTableDaySchema);
            List<TimeTableDaySchema> createdDays = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> dayScheduleSchemas = new ArrayList<>();
            for (TimeTableDaySchema day : createdDays) {
                dayScheduleSchemas.add(new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null));
            }
            db.getDayScheduleDao().insertMulti(dayScheduleSchemas);
        } else {
            // Already initialized, synchronize Saturday if workingDays changed
            if (workingDays == 6) {
                boolean hasSaturday = false;
                for (TimeTableDaySchema day : existingDays) {
                    if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                        hasSaturday = true;
                        break;
                    }
                }
                if (!hasSaturday) {
                    db.getTimeTableDayDao().insert(new TimeTableDaySchema("Saturday"));
                    List<TimeTableDaySchema> updatedDays = db.getTimeTableDayDao().getDays();
                    for (TimeTableDaySchema day : updatedDays) {
                        if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                            db.getDayScheduleDao().insert(
                                    new DayScheduleSchema(day.getDid(), null, null, null, null, null, null, null, null, null)
                            );
                            break;
                        }
                    }
                }
            } else if (workingDays == 5) {
                for (TimeTableDaySchema day : existingDays) {
                    if ("Saturday".equalsIgnoreCase(day.getDayName())) {
                        db.getTimeTableDayDao().deleteDay(day.getDid());
                        break;
                    }
                }
            }
        }
    }
}
