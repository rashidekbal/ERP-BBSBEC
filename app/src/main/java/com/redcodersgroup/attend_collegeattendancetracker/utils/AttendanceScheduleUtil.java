package com.redcodersgroup.attend_collegeattendancetracker.utils;

import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceScheduleUtil {

    public static class AttendanceStats {
        private final int attended;
        private final int missed;
        private final int cancelled;
        private final int conducted;
        private final int percentage;

        public AttendanceStats(int attended, int missed, int cancelled, int conducted, int percentage) {
            this.attended = attended;
            this.missed = missed;
            this.cancelled = cancelled;
            this.conducted = conducted;
            this.percentage = percentage;
        }

        public int getAttended() {
            return attended;
        }

        public int getMissed() {
            return missed;
        }

        public int getCancelled() {
            return cancelled;
        }

        public int getConducted() {
            return conducted;
        }

        public int getPercentage() {
            return percentage;
        }
    }

    /**
     * Calculates the number of scheduled periods for a subject on a specific date string ("dd/MM/yyyy").
     * If the subject appears multiple times in the day's timetable (e.g. 2 lab sessions),
     * returns the exact count of periods (e.g. 2).
     * If no timetable slot is found or timetable is not configured, returns 1 as fallback.
     */
    public static int getScheduledPeriodsOnDate(
            String dateStr,
            int targetSid,
            List<TimeTableDaySchema> days,
            List<DayScheduleSchema> schedules
    ) {
        if (dateStr == null || targetSid <= 0 || days == null || schedules == null) return 1;
        try {
            long ms = DateUtil.getDateMs(dateStr);
            if (ms <= 0) return 1;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ms);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = getDayNameFromDayOfWeek(dayOfWeek);

            TimeTableDaySchema targetDay = null;
            for (TimeTableDaySchema d : days) {
                if (d != null && d.getDayName() != null && d.getDayName().equalsIgnoreCase(dayName)) {
                    targetDay = d;
                    break;
                }
            }
            if (targetDay == null) return 1;

            for (DayScheduleSchema s : schedules) {
                if (s != null && s.getDid() == targetDay.getDid()) {
                    int count = countPeriodsForSubject(s, targetSid);
                    return count > 0 ? count : 1;
                }
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    /**
     * Calculates complete attendance stats (attended, missed, cancelled, conducted, percentage)
     * for a subject taking into account multiple periods / labs on the same day.
     */
    public static AttendanceStats calculateSubjectAttendanceStats(
            UserSchema user,
            SubjectSchema subject,
            boolean isLab,
            List<AttendanceSchema> records,
            List<HolidaySchema> holidays,
            List<TimeTableDaySchema> days,
            List<DayScheduleSchema> schedules
    ) {
        if (subject == null) {
            return new AttendanceStats(0, 0, 0, 0, 0);
        }

        int attended = 0;
        int missed = 0;
        int cancelled = 0;

        if (records != null) {
            for (AttendanceSchema rec : records) {
                int periodsOnDay = (rec.getHour() != null && !rec.getHour().isEmpty())
                        ? 1
                        : getScheduledPeriodsOnDate(rec.getDate(), subject.getSid(), days, schedules);
                if (AttendanceConstants.isPresent(rec.getStatus())) {
                    attended += periodsOnDay;
                } else if (AttendanceConstants.isAbsent(rec.getStatus())) {
                    missed += periodsOnDay;
                } else if (AttendanceConstants.isCancelled(rec.getStatus())) {
                    cancelled += periodsOnDay;
                }
            }
        }

        int scheduledConducted = calculateScheduledConductedTillToday(user, subject, isLab, holidays, days, schedules);
        int netScheduledConducted = Math.max(0, scheduledConducted - cancelled);
        int conducted = Math.max(attended + missed, netScheduledConducted);

        int percentage;
        if (conducted > 0) {
            percentage = (int) Math.round(((double) attended / conducted) * 100);
        } else {
            percentage = (records != null && !records.isEmpty()) ? 0 : 100;
        }

        return new AttendanceStats(attended, missed, cancelled, conducted, percentage);
    }

    /**
     * Calculates the number of classes/labs that were scheduled to be conducted
     * between the semester start date and today, accounting for holidays,
     * weekends, and the timetable day schedules.
     */
    public static int calculateScheduledConductedTillToday(
            UserSchema user,
            SubjectSchema subject,
            boolean isLab,
            List<HolidaySchema> holidays,
            List<TimeTableDaySchema> days,
            List<DayScheduleSchema> schedules
    ) {
        if (user == null || subject == null) return 0;
        long semStart = user.getSemStartDate();
        if (semStart <= 0) return 0;

        long now = System.currentTimeMillis();
        // If semester start is in future, 0 classes conducted
        if (semStart > now) return 0;

        int workingDays = user.getWorkingDays() > 0 ? user.getWorkingDays() : 5;
        int targetSid = subject.getSid();

        // Build mapping of dayName -> DayScheduleSchema
        Map<String, DayScheduleSchema> dayNameToSchedule = new HashMap<>();
        if (days != null && schedules != null) {
            for (TimeTableDaySchema d : days) {
                if (d == null || d.getDayName() == null) continue;
                for (DayScheduleSchema s : schedules) {
                    if (s != null && s.getDid() == d.getDid()) {
                        dayNameToSchedule.put(d.getDayName().toLowerCase().trim(), s);
                        break;
                    }
                }
            }
        }

        Calendar cur = Calendar.getInstance();
        cur.setTimeInMillis(semStart);
        cur.set(Calendar.HOUR_OF_DAY, 0);
        cur.set(Calendar.MINUTE, 0);
        cur.set(Calendar.SECOND, 0);
        cur.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(now);
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        today.set(Calendar.MILLISECOND, 999);

        // Also respect semEndDate if past
        if (user.getSemEndDate() > 0 && user.getSemEndDate() < today.getTimeInMillis()) {
            today.setTimeInMillis(user.getSemEndDate());
            today.set(Calendar.HOUR_OF_DAY, 23);
            today.set(Calendar.MINUTE, 59);
            today.set(Calendar.SECOND, 59);
            today.set(Calendar.MILLISECOND, 999);
        }

        int scheduledCount = 0;
        int workingDaysCount = 0;
        boolean hasTimetableEntries = false;

        while (!cur.after(today)) {
            long dateMs = cur.getTimeInMillis();
            boolean isHoliday = isHoliday(holidays, dateMs);

            if (!isHoliday) {
                int dayOfWeek = cur.get(Calendar.DAY_OF_WEEK);
                boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

                if (!isWeekend) {
                    workingDaysCount++;
                    String dayName = getDayNameFromDayOfWeek(dayOfWeek);
                    DayScheduleSchema schedule = dayNameToSchedule.get(dayName.toLowerCase());

                    if (schedule != null) {
                        int countOnDay = countPeriodsForSubject(schedule, targetSid);
                        if (countOnDay > 0) {
                            hasTimetableEntries = true;
                            scheduledCount += countOnDay;
                        }
                    }
                }
            }
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Fallback: If no timetable slots were assigned yet, but classesPerWeek/labsPerWeek is defined
        if (!hasTimetableEntries && workingDaysCount > 0) {
            int perWeek = isLab ? subject.getLabsPerWeek() : subject.getClassesPerWeek();
            if (perWeek > 0) {
                double weeks = workingDaysCount / (double) workingDays;
                return (int) Math.max(0, Math.round(weeks * perWeek));
            }
        }

        return scheduledCount;
    }

    /**
     * Calculates the number of classes/labs scheduled to occur after today until the semester end date,
     * accounting for holidays, weekends, and the timetable day schedules (with fallback to weekly counts).
     */
    public static int calculateRemainingScheduledClasses(
            UserSchema user,
            SubjectSchema subject,
            boolean isLab,
            List<HolidaySchema> holidays,
            List<TimeTableDaySchema> days,
            List<DayScheduleSchema> schedules
    ) {
        if (user == null || subject == null) return 0;
        long semEnd = user.getSemEndDate();
        if (semEnd <= 0) return 0;

        int workingDays = user.getWorkingDays() > 0 ? user.getWorkingDays() : 5;
        int targetSid = subject.getSid();

        // Build mapping of dayName -> DayScheduleSchema & check if subject is placed anywhere in timetable
        Map<String, DayScheduleSchema> dayNameToSchedule = new HashMap<>();
        boolean subjectInTimetable = false;
        if (days != null && schedules != null) {
            for (TimeTableDaySchema d : days) {
                if (d == null || d.getDayName() == null) continue;
                for (DayScheduleSchema s : schedules) {
                    if (s != null && s.getDid() == d.getDid()) {
                        dayNameToSchedule.put(d.getDayName().toLowerCase().trim(), s);
                        if (countPeriodsForSubject(s, targetSid) > 0) {
                            subjectInTimetable = true;
                        }
                        break;
                    }
                }
            }
        }

        // Start from tomorrow at 00:00:00 (or semStartDate if semester starts in the future)
        Calendar cur = Calendar.getInstance();
        cur.add(Calendar.DAY_OF_MONTH, 1);
        cur.set(Calendar.HOUR_OF_DAY, 0);
        cur.set(Calendar.MINUTE, 0);
        cur.set(Calendar.SECOND, 0);
        cur.set(Calendar.MILLISECOND, 0);

        if (user.getSemStartDate() > cur.getTimeInMillis()) {
            cur.setTimeInMillis(user.getSemStartDate());
            cur.set(Calendar.HOUR_OF_DAY, 0);
            cur.set(Calendar.MINUTE, 0);
            cur.set(Calendar.SECOND, 0);
            cur.set(Calendar.MILLISECOND, 0);
        }

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(semEnd);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        if (cur.after(endCal)) {
            return 0;
        }

        int scheduledCount = 0;
        int workingDaysCount = 0;

        while (!cur.after(endCal)) {
            long dateMs = cur.getTimeInMillis();
            boolean isHoliday = isHoliday(holidays, dateMs);

            if (!isHoliday) {
                int dayOfWeek = cur.get(Calendar.DAY_OF_WEEK);
                boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

                if (!isWeekend) {
                    workingDaysCount++;
                    if (subjectInTimetable) {
                        String dayName = getDayNameFromDayOfWeek(dayOfWeek);
                        DayScheduleSchema schedule = dayNameToSchedule.get(dayName.toLowerCase());
                        if (schedule != null) {
                            scheduledCount += countPeriodsForSubject(schedule, targetSid);
                        }
                    }
                }
            }
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (!subjectInTimetable && workingDaysCount > 0) {
            int perWeek = isLab ? subject.getLabsPerWeek() : subject.getClassesPerWeek();
            if (perWeek > 0) {
                double weeks = workingDaysCount / (double) workingDays;
                return (int) Math.max(0, Math.round(weeks * perWeek));
            }
        }

        return scheduledCount;
    }

    public static class HeroRiskSummary {
        private final int overallPercentage;
        private final int totalAttended;
        private final int totalConducted;
        private final int totalRemaining;
        private final int maxPossiblePercentage;
        private final int classesNeeded;
        private final int bunkSafeCount;
        private final boolean targetUnreachable;
        private final String statusChipText;
        private final String headlineText;
        private final String subheadlineText;

        public HeroRiskSummary(
                int overallPercentage,
                int totalAttended,
                int totalConducted,
                int totalRemaining,
                int maxPossiblePercentage,
                int classesNeeded,
                int bunkSafeCount,
                boolean targetUnreachable,
                String statusChipText,
                String headlineText,
                String subheadlineText
        ) {
            this.overallPercentage = overallPercentage;
            this.totalAttended = totalAttended;
            this.totalConducted = totalConducted;
            this.totalRemaining = totalRemaining;
            this.maxPossiblePercentage = maxPossiblePercentage;
            this.classesNeeded = classesNeeded;
            this.bunkSafeCount = bunkSafeCount;
            this.targetUnreachable = targetUnreachable;
            this.statusChipText = statusChipText;
            this.headlineText = headlineText;
            this.subheadlineText = subheadlineText;
        }

        public int getOverallPercentage() { return overallPercentage; }
        public int getTotalAttended() { return totalAttended; }
        public int getTotalConducted() { return totalConducted; }
        public int getTotalRemaining() { return totalRemaining; }
        public int getMaxPossiblePercentage() { return maxPossiblePercentage; }
        public int getClassesNeeded() { return classesNeeded; }
        public int getBunkSafeCount() { return bunkSafeCount; }
        public boolean isTargetUnreachable() { return targetUnreachable; }
        public String getStatusChipText() { return statusChipText; }
        public String getHeadlineText() { return headlineText; }
        public String getSubheadlineText() { return subheadlineText; }
    }

    /**
     * Computes the unified Risk Calculator summary for the Home and Attendance Hero Cards,
     * factoring in remaining classes in the semester and maximum reachable percentage.
     */
    public static HeroRiskSummary calculateHeroRiskSummary(
            UserSchema user,
            int subjectCount,
            int totalAttended,
            int totalConducted,
            int totalRemaining
    ) {
        int criteria = (user != null && user.getAttendanceCriteria() > 0)
                ? user.getAttendanceCriteria() : 75;
        boolean hasSemEnd = (user != null && user.getSemEndDate() > 0);

        if (subjectCount <= 0) {
            return new HeroRiskSummary(
                    0, 0, 0, 0, 0, 0, 0, false,
                    "No Subjects",
                    "Add subjects to start",
                    "Set your timetable and target criteria"
            );
        }

        if (totalConducted <= 0) {
            String sub = (hasSemEnd && totalRemaining > 0)
                    ? (totalRemaining + (totalRemaining == 1 ? " class left" : " classes left") + " in semester · Target: " + criteria + "%")
                    : ("Target: " + criteria + "% · Mark attendance as classes happen");
            return new HeroRiskSummary(
                    0, 0, 0, totalRemaining, 100, 0, 0, false,
                    "Fresh Term",
                    "No classes conducted yet",
                    sub
            );
        }

        int overallPct = (int) Math.round((totalAttended * 100.0) / totalConducted);
        int finalConducted = totalConducted + totalRemaining;
        int finalMaxAttended = totalAttended + totalRemaining;
        int maxPossiblePct = finalConducted > 0
                ? (int) Math.round((finalMaxAttended * 100.0) / finalConducted)
                : overallPct;

        if (overallPct < criteria) {
            int needed;
            if (criteria >= 100) {
                needed = totalRemaining + 1;
            } else {
                needed = (int) Math.ceil((criteria * totalConducted - 100.0 * totalAttended) / (100.0 - criteria));
                if (needed <= 0) needed = 1;
            }

            boolean unreachable = hasSemEnd && (needed > totalRemaining || maxPossiblePct < criteria);
            if (unreachable) {
                String headline = (totalRemaining > 0)
                        ? ("Attend all " + totalRemaining + (totalRemaining == 1 ? " class left" : " classes left"))
                        : "No classes left in semester";
                String sub = "Cannot reach " + criteria + "% target · Max possible is " + maxPossiblePct + "%";
                return new HeroRiskSummary(
                        overallPct, totalAttended, totalConducted, totalRemaining,
                        maxPossiblePct, needed, 0, true,
                        "Target Unreachable",
                        headline,
                        sub
                );
            } else {
                String headline = "Attend next " + needed + (needed == 1 ? " class" : " classes");
                String sub = hasSemEnd
                        ? (totalRemaining + (totalRemaining == 1 ? " class left" : " classes left") + " in semester · Max possible: " + maxPossiblePct + "%")
                        : ("Target: " + criteria + "% (" + (overallPct - criteria) + "% deficit)");
                return new HeroRiskSummary(
                        overallPct, totalAttended, totalConducted, totalRemaining,
                        maxPossiblePct, needed, 0, false,
                        "Action Needed",
                        headline,
                        sub
                );
            }
        } else {
            int bunkSafe = criteria > 0
                    ? (int) Math.floor((100.0 * totalAttended - criteria * totalConducted) / criteria)
                    : 0;
            if (bunkSafe < 0) bunkSafe = 0;
            if (hasSemEnd && totalRemaining > 0 && bunkSafe > totalRemaining) {
                bunkSafe = totalRemaining;
            }

            String headline = (bunkSafe > 0)
                    ? ("Can safely skip " + bunkSafe + (bunkSafe == 1 ? " class" : " classes"))
                    : ("Right on target (" + overallPct + "%)");
            String sub = hasSemEnd
                    ? (totalRemaining + (totalRemaining == 1 ? " class left" : " classes left") + " in semester · Max possible: " + maxPossiblePct + "%")
                    : ("Target: " + criteria + "% (+" + (overallPct - criteria) + "% margin)");
            return new HeroRiskSummary(
                    overallPct, totalAttended, totalConducted, totalRemaining,
                    maxPossiblePct, 0, bunkSafe, false,
                    "Safe Margin",
                    headline,
                    sub
            );
        }
    }

    public static String getDayNameFromDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "Monday";
        }
    }

    public static boolean isHoliday(List<HolidaySchema> holidays, long dateMs) {
        if (holidays == null || holidays.isEmpty()) return false;
        Calendar hStartCal = Calendar.getInstance();
        Calendar hEndCal = Calendar.getInstance();

        for (HolidaySchema h : holidays) {
            if (h == null) continue;
            hStartCal.setTimeInMillis(h.getStartDate());
            hStartCal.set(Calendar.HOUR_OF_DAY, 0);
            hStartCal.set(Calendar.MINUTE, 0);
            hStartCal.set(Calendar.SECOND, 0);
            hStartCal.set(Calendar.MILLISECOND, 0);

            hEndCal.setTimeInMillis(h.getEndDate());
            hEndCal.set(Calendar.HOUR_OF_DAY, 23);
            hEndCal.set(Calendar.MINUTE, 59);
            hEndCal.set(Calendar.SECOND, 59);
            hEndCal.set(Calendar.MILLISECOND, 999);

            if (dateMs >= hStartCal.getTimeInMillis() && dateMs <= hEndCal.getTimeInMillis()) {
                return true;
            }
        }
        return false;
    }

    public static int countPeriodsForSubject(DayScheduleSchema schedule, int targetSid) {
        if (schedule == null) return 0;
        int count = 0;
        for (int p = 1; p <= 9; p++) {
            Integer val = TimeTableViewModel.getPeriodValue(schedule, p);
            if (val != null && val == targetSid) {
                count++;
            }
        }
        return count;
    }
}
