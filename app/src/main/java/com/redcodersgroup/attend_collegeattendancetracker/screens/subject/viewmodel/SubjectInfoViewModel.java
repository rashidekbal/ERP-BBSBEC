package com.redcodersgroup.attend_collegeattendancetracker.screens.subject.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectAdditionalInfoSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.CalendarDayModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.subject.model.DayScheduleInfo;
import com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.viewmodel.TimeTableViewModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubjectInfoViewModel extends AndroidViewModel {

    private int sid;
    private boolean isLab;

    private final MutableLiveData<SubjectSchema> subject = new MutableLiveData<>();
    private final MutableLiveData<SubjectAdditionalInfoSchema> additionalInfo = new MutableLiveData<>();
    private final MutableLiveData<UserSchema> user = new MutableLiveData<>();
    private final MutableLiveData<List<AttendanceSchema>> attendanceHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CalendarDayModel>> calendarDays = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Calendar> currentCalendarMonth = new MutableLiveData<>(Calendar.getInstance());
    private final MutableLiveData<String> todayAttendanceStatus = new MutableLiveData<>(null);
    private final MutableLiveData<DayScheduleInfo> todayScheduleInfo = new MutableLiveData<>(null);

    // Cached schedule context
    private UserSchema cachedUser;
    private List<HolidaySchema> cachedHolidays = new ArrayList<>();
    private List<TimeTableDaySchema> cachedDays = new ArrayList<>();
    private List<DayScheduleSchema> cachedSchedules = new ArrayList<>();
    private int cachedPeriodLength = 50;

    // Calculated metrics
    private final MutableLiveData<Integer> attendedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> conductedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> missedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> cancelledCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> percentage = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> criteria = new MutableLiveData<>(75);
    private final MutableLiveData<String> bunkSafeStatus = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isOnTrack = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> remainingCount = new MutableLiveData<>(0);

    public SubjectInfoViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(int sid, boolean isLab) {
        this.sid = sid;
        this.isLab = isLab;
        loadAllData();
    }

    public void loadAllData() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema userSchema = db.getUserDao().getUser();
            SubjectSchema subjectSchema = db.getSubjectDao().getSubjectById(sid);
            SubjectAdditionalInfoSchema addInfo = db.getSubjectAdditionalInfoDao().getSubjectAdditionalInfoBySid(sid);
            List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(sid, isLab);
            List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();
            int periodLength = SharedPrefUtil.getPeriodLength();

            cachedUser = userSchema;
            cachedHolidays = holidays != null ? holidays : new ArrayList<>();
            cachedDays = days != null ? days : new ArrayList<>();
            cachedSchedules = schedules != null ? schedules : new ArrayList<>();
            cachedPeriodLength = periodLength > 0 ? periodLength : 50;

            user.postValue(userSchema);
            subject.postValue(subjectSchema);
            additionalInfo.postValue(addInfo);
            attendanceHistory.postValue(records != null ? records : new ArrayList<>());

            // Compute today's schedule availability
            DayScheduleInfo todayInfo = computeScheduleInfo(Calendar.getInstance(), cachedUser, cachedHolidays, cachedDays, cachedSchedules, cachedPeriodLength);
            todayScheduleInfo.postValue(todayInfo);

            // Check today's status
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            String todayStr = sdf.format(new Date());
            AttendanceSchema todayRecord = db.getAttendanceDao().getAttendanceRecord(sid, todayStr, isLab);
            todayAttendanceStatus.postValue(todayRecord != null ? todayRecord.getStatus() : null);

            // Compute statistics
            int userCriteria = (userSchema != null && userSchema.getAttendanceCriteria() > 0) ? userSchema.getAttendanceCriteria() : 75;
            int subjCriteria = (subjectSchema != null && subjectSchema.getAttendanceCriteria() > 0) ? subjectSchema.getAttendanceCriteria() : userCriteria;
            criteria.postValue(subjCriteria);

            int total = 0;
            if (addInfo != null) {
                total = isLab ? addInfo.getTotalLabs() : addInfo.getTotalClasses();
            }
            totalCount.postValue(total);

            AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                    userSchema, subjectSchema, isLab, records, cachedHolidays, cachedDays, cachedSchedules);

            int attended = stats.getAttended();
            int missed = stats.getMissed();
            int cancelled = stats.getCancelled();
            int conducted = stats.getConducted();
            int pct = stats.getPercentage();

            attendedCount.postValue(attended);
            missedCount.postValue(missed);
            cancelledCount.postValue(cancelled);
            conductedCount.postValue(conducted);
            percentage.postValue(pct);

            double threshold = subjCriteria / 100.0;
            String unitSingle = isLab ? "lab" : "class";
            String unitPlural = isLab ? "labs" : "classes";

            if (conducted == 0) {
                isOnTrack.postValue(true);
                bunkSafeStatus.postValue("No attendance marked yet");
            } else if (pct >= subjCriteria) {
                isOnTrack.postValue(true);
                int bunkSafe = (int) Math.floor((attended - threshold * conducted) / threshold);
                if (bunkSafe > 0) {
                    bunkSafeStatus.postValue("Bunk-safe: " + bunkSafe + " " + (bunkSafe == 1 ? unitSingle : unitPlural));
                } else {
                    bunkSafeStatus.postValue("On track (" + pct + "%)");
                }
            } else {
                isOnTrack.postValue(false);
                int needToAttend = (int) Math.ceil((threshold * conducted - attended) / (1.0 - threshold));
                if (needToAttend <= 0) needToAttend = 1;
                bunkSafeStatus.postValue("Attend next " + needToAttend + " " + (needToAttend == 1 ? unitSingle : unitPlural));
            }

            loadCalendarDaysInternal();
            int remaining = AttendanceScheduleUtil.calculateRemainingScheduledClasses(
                    userSchema, subjectSchema, isLab, cachedHolidays, cachedDays, cachedSchedules);
            remainingCount.postValue(remaining);
            AttendanceWidgetProvider.refreshAllWidgets(getApplication());
        });
    }

    public void markTodayAttendance(String status) {
        // Attendance records are read-only and synced from college ERP
    }

    public void markAttendance(String date, String status) {
        // Attendance records are read-only and synced from college ERP
    }

    public void deleteAttendance(int aid) {
        // Attendance records are read-only and synced from college ERP
    }

    public void updateSubjectName(String newName) {
        DbHelper.updateSubjectName(sid, newName, this::loadAllData);
    }

    public void prevMonth() {
        Calendar cal = currentCalendarMonth.getValue();
        if (cal == null) cal = Calendar.getInstance();
        Calendar newCal = (Calendar) cal.clone();
        newCal.add(Calendar.MONTH, -1);
        currentCalendarMonth.setValue(newCal);
        loadCalendarDaysInternalAsync();
    }

    public void nextMonth() {
        Calendar cal = currentCalendarMonth.getValue();
        if (cal == null) cal = Calendar.getInstance();
        Calendar newCal = (Calendar) cal.clone();
        newCal.add(Calendar.MONTH, 1);
        currentCalendarMonth.setValue(newCal);
        loadCalendarDaysInternalAsync();
    }

    private void loadCalendarDaysInternalAsync() {
        DbHelper.execute(this::loadCalendarDaysInternal);
    }

    private void loadCalendarDaysInternal() {
        Database db = Database.getInstance();
        Calendar currentCal = currentCalendarMonth.getValue();
        if (currentCal == null) {
            currentCal = Calendar.getInstance();
        }

        Calendar cal = (Calendar) currentCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Sunday = 1
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar todayCal = Calendar.getInstance();
        int todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
        int todayMonth = todayCal.get(Calendar.MONTH);
        int todayYear = todayCal.get(Calendar.YEAR);

        UserSchema userSchema = db.getUserDao().getUser();
        int workingDays = (userSchema != null && userSchema.getWorkingDays() > 0) ? userSchema.getWorkingDays() : 6;

        List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
        List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(sid, isLab);

        Map<String, AttendanceSchema> attendanceMap = new HashMap<>();
        SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        if (records != null) {
            for (AttendanceSchema att : records) {
                if (att.getDate() != null) {
                    attendanceMap.put(att.getDate(), att);
                    long ms = com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateMs(att.getDate());
                    if (ms > 0) {
                        attendanceMap.put(com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateString(ms), att);
                        attendanceMap.put(isoSdf.format(new Date(ms)), att);
                    }
                }
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        List<CalendarDayModel> dayModels = new ArrayList<>();

        // 1. Leading dates from previous month
        int emptySlotsBefore = firstDayOfWeek - Calendar.SUNDAY;
        if (emptySlotsBefore > 0) {
            Calendar prevMonthCal = (Calendar) cal.clone();
            prevMonthCal.add(Calendar.MONTH, -1);
            int prevMonth = prevMonthCal.get(Calendar.MONTH);
            int prevYear = prevMonthCal.get(Calendar.YEAR);
            int prevMaxDays = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int startDay = prevMaxDays - emptySlotsBefore + 1;

            for (int d = startDay; d <= prevMaxDays; d++) {
                prevMonthCal.set(Calendar.DAY_OF_MONTH, d);
                long dayMs = prevMonthCal.getTimeInMillis();
                String dateStr = sdf.format(prevMonthCal.getTime());
                int dayOfWeek = prevMonthCal.get(Calendar.DAY_OF_WEEK);
                boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

                HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
                boolean isHoliday = matchingHoliday != null;
                String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

                AttendanceSchema dayAtt = attendanceMap.get(dateStr);
                if (dayAtt == null) {
                    dayAtt = attendanceMap.get(isoSdf.format(prevMonthCal.getTime()));
                }
                boolean hasAttendance = dayAtt != null;
                int presentCount = 0;
                int absentCount = 0;
                int cancelledCount = 0;
                if (hasAttendance) {
                    int periodsOnDay = AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, sid, cachedDays, cachedSchedules);
                    if (periodsOnDay <= 0) periodsOnDay = 1;
                    if (AttendanceConstants.isPresent(dayAtt.getStatus())) {
                        presentCount = periodsOnDay;
                    } else if (AttendanceConstants.isAbsent(dayAtt.getStatus())) {
                        absentCount = periodsOnDay;
                    } else if (AttendanceConstants.isCancelled(dayAtt.getStatus())) {
                        cancelledCount = periodsOnDay;
                    }
                }

                dayModels.add(new CalendarDayModel(d, prevMonth, prevYear, dayMs, dateStr, false, false, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount));
            }
        }

        // 2. Current month dates
        for (int d = 1; d <= maxDays; d++) {
            cal.set(Calendar.DAY_OF_MONTH, d);
            long dayMs = cal.getTimeInMillis();
            String dateStr = sdf.format(cal.getTime());
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            boolean isToday = (d == todayDay && month == todayMonth && year == todayYear);
            boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

            HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
            boolean isHoliday = matchingHoliday != null;
            String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

            AttendanceSchema dayAtt = attendanceMap.get(dateStr);
            if (dayAtt == null) {
                dayAtt = attendanceMap.get(isoSdf.format(cal.getTime()));
            }
            boolean hasAttendance = dayAtt != null;
            int presentCount = 0;
            int absentCount = 0;
            int cancelledCount = 0;
            if (hasAttendance) {
                int periodsOnDay = AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, sid, cachedDays, cachedSchedules);
                if (periodsOnDay <= 0) periodsOnDay = 1;
                if (AttendanceConstants.isPresent(dayAtt.getStatus())) {
                    presentCount = periodsOnDay;
                } else if (AttendanceConstants.isAbsent(dayAtt.getStatus())) {
                    absentCount = periodsOnDay;
                } else if (AttendanceConstants.isCancelled(dayAtt.getStatus())) {
                    cancelledCount = periodsOnDay;
                }
            }

            dayModels.add(new CalendarDayModel(d, month, year, dayMs, dateStr, true, isToday, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount));
        }

        // 3. Trailing dates from next month to complete the 7-column grid
        if (dayModels.size() % 7 != 0) {
            Calendar nextMonthCal = (Calendar) currentCal.clone();
            nextMonthCal.set(Calendar.DAY_OF_MONTH, 1);
            nextMonthCal.set(Calendar.HOUR_OF_DAY, 0);
            nextMonthCal.set(Calendar.MINUTE, 0);
            nextMonthCal.set(Calendar.SECOND, 0);
            nextMonthCal.set(Calendar.MILLISECOND, 0);
            nextMonthCal.add(Calendar.MONTH, 1);

            int nextMonth = nextMonthCal.get(Calendar.MONTH);
            int nextYear = nextMonthCal.get(Calendar.YEAR);
            int trailingCount = 7 - (dayModels.size() % 7);

            for (int d = 1; d <= trailingCount; d++) {
                nextMonthCal.set(Calendar.DAY_OF_MONTH, d);
                long dayMs = nextMonthCal.getTimeInMillis();
                String dateStr = sdf.format(nextMonthCal.getTime());
                int dayOfWeek = nextMonthCal.get(Calendar.DAY_OF_WEEK);
                boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

                HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
                boolean isHoliday = matchingHoliday != null;
                String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

                AttendanceSchema dayAtt = attendanceMap.get(dateStr);
                if (dayAtt == null) {
                    dayAtt = attendanceMap.get(isoSdf.format(nextMonthCal.getTime()));
                }
                boolean hasAttendance = dayAtt != null;
                int presentCount = 0;
                int absentCount = 0;
                int cancelledCount = 0;
                if (hasAttendance) {
                    int periodsOnDay = AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, sid, cachedDays, cachedSchedules);
                    if (periodsOnDay <= 0) periodsOnDay = 1;
                    if (AttendanceConstants.isPresent(dayAtt.getStatus())) {
                        presentCount = periodsOnDay;
                    } else if (AttendanceConstants.isAbsent(dayAtt.getStatus())) {
                        absentCount = periodsOnDay;
                    } else if (AttendanceConstants.isCancelled(dayAtt.getStatus())) {
                        cancelledCount = periodsOnDay;
                    }
                }

                dayModels.add(new CalendarDayModel(d, nextMonth, nextYear, dayMs, dateStr, false, false, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount));
            }
        }

        calendarDays.postValue(dayModels);
    }

    private HolidaySchema findMatchingHoliday(List<HolidaySchema> holidays, long dayMs) {
        if (holidays == null || holidays.isEmpty()) return null;

        Calendar hStartCal = Calendar.getInstance();
        Calendar hEndCal = Calendar.getInstance();

        for (HolidaySchema h : holidays) {
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

            if (dayMs >= hStartCal.getTimeInMillis() && dayMs <= hEndCal.getTimeInMillis()) {
                return h;
            }
        }
        return null;
    }

    public LiveData<SubjectSchema> getSubject() {
        return subject;
    }

    public LiveData<SubjectAdditionalInfoSchema> getAdditionalInfo() {
        return additionalInfo;
    }

    public LiveData<UserSchema> getUser() {
        return user;
    }

    public UserSchema getCachedUser() {
        return cachedUser != null ? cachedUser : user.getValue();
    }

    public LiveData<List<AttendanceSchema>> getAttendanceHistory() {
        return attendanceHistory;
    }

    public LiveData<List<CalendarDayModel>> getCalendarDays() {
        return calendarDays;
    }

    public LiveData<Calendar> getCurrentCalendarMonth() {
        return currentCalendarMonth;
    }

    public LiveData<String> getTodayAttendanceStatus() {
        return todayAttendanceStatus;
    }

    public LiveData<Integer> getAttendedCount() {
        return attendedCount;
    }

    public LiveData<Integer> getConductedCount() {
        return conductedCount;
    }

    public LiveData<Integer> getTotalCount() {
        return totalCount;
    }

    public LiveData<Integer> getMissedCount() {
        return missedCount;
    }

    public LiveData<Integer> getCancelledCount() {
        return cancelledCount;
    }

    public LiveData<Integer> getPercentage() {
        return percentage;
    }

    public LiveData<Integer> getCriteria() {
        return criteria;
    }

    public LiveData<String> getBunkSafeStatus() {
        return bunkSafeStatus;
    }

    public LiveData<Boolean> getIsOnTrack() {
        return isOnTrack;
    }

    public LiveData<Integer> getRemainingCount() {
        return remainingCount;
    }

    public LiveData<DayScheduleInfo> getTodayScheduleInfo() {
        return todayScheduleInfo;
    }

    public DayScheduleInfo getDayScheduleInfoForDate(Calendar cal) {
        return computeScheduleInfo(cal, cachedUser, cachedHolidays, cachedDays, cachedSchedules, cachedPeriodLength);
    }

    public DayScheduleInfo getDayScheduleInfoForDate(long dateMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMs);
        return computeScheduleInfo(cal, cachedUser, cachedHolidays, cachedDays, cachedSchedules, cachedPeriodLength);
    }

    public DayScheduleInfo computeScheduleInfo(Calendar cal, UserSchema userSchema,
                                               List<HolidaySchema> holidays,
                                               List<TimeTableDaySchema> days,
                                               List<DayScheduleSchema> schedules,
                                               int periodLength) {
        if (cal == null) cal = Calendar.getInstance();

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String dayName;
        switch (dayOfWeek) {
            case Calendar.MONDAY: dayName = "Monday"; break;
            case Calendar.TUESDAY: dayName = "Tuesday"; break;
            case Calendar.WEDNESDAY: dayName = "Wednesday"; break;
            case Calendar.THURSDAY: dayName = "Thursday"; break;
            case Calendar.FRIDAY: dayName = "Friday"; break;
            case Calendar.SATURDAY: dayName = "Saturday"; break;
            case Calendar.SUNDAY: dayName = "Sunday"; break;
            default: dayName = "Monday"; break;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        String formattedDate = sdf.format(cal.getTime());

        Calendar todayCal = Calendar.getInstance();
        boolean isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && cal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
                && cal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH);

        HolidaySchema matchingHoliday = findMatchingHoliday(holidays, cal.getTimeInMillis());
        boolean isHoliday = matchingHoliday != null;
        String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

        int workingDays = (userSchema != null && userSchema.getWorkingDays() > 0) ? userSchema.getWorkingDays() : 5;
        boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);

        List<Integer> periodNumbers = new ArrayList<>();
        List<String> periodTimeRanges = new ArrayList<>();
        long firstClassStartTimeMs = userSchema != null ? userSchema.getFirstPeriodStartTime() : 0;

        if (days != null && schedules != null) {
            TimeTableDaySchema targetDay = null;
            for (TimeTableDaySchema d : days) {
                if (dayName.equalsIgnoreCase(d.getDayName())) {
                    targetDay = d;
                    break;
                }
            }

            if (targetDay != null) {
                DayScheduleSchema targetSchedule = null;
                for (DayScheduleSchema s : schedules) {
                    if (s.getDid() == targetDay.getDid()) {
                        targetSchedule = s;
                        break;
                    }
                }

                if (targetSchedule != null) {
                    for (int p = 1; p <= 9; p++) {
                        Integer assignedSid = TimeTableViewModel.getPeriodValue(targetSchedule, p);
                        if (assignedSid != null && assignedSid == sid) {
                            periodNumbers.add(p);
                            String timeRange = TimeTableViewModel.getPeriodTimeRange(firstClassStartTimeMs, p - 1, periodLength > 0 ? periodLength : 50);
                            periodTimeRanges.add(timeRange);
                        }
                    }
                }
            }
        }

        String unit = isLab ? "Lab" : "Class";
        String unitPlural = isLab ? "Labs" : "Classes";

        if (isHoliday) {
            String title = "Holiday · " + (holidayName != null && !holidayName.isEmpty() ? holidayName : "Campus Closed");
            String detail = "College holiday · No " + unitPlural.toLowerCase() + " scheduled";
            return new DayScheduleInfo(dayName, formattedDate, isToday, true, holidayName, isWeekend, isLab, false, periodNumbers, periodTimeRanges, title, detail);
        }

        if (isWeekend) {
            String title = "Weekend · Off Day";
            String detail = "No " + unitPlural.toLowerCase() + " scheduled on " + dayName + "s";
            return new DayScheduleInfo(dayName, formattedDate, isToday, false, "", true, isLab, false, periodNumbers, periodTimeRanges, title, detail);
        }

        if (periodNumbers.isEmpty()) {
            String title = "No " + unit + " Scheduled";
            String detail = "No " + unit.toLowerCase() + " slot in timetable for " + dayName + "s";
            return new DayScheduleInfo(dayName, formattedDate, isToday, false, "", false, isLab, false, periodNumbers, periodTimeRanges, title, detail);
        } else {
            String title = (isToday ? (unit + " Scheduled Today") : (unit + " Scheduled"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < periodNumbers.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("Period ").append(periodNumbers.get(i)).append(" (").append(periodTimeRanges.get(i)).append(")");
            }
            String detail = sb.toString();
            return new DayScheduleInfo(dayName, formattedDate, isToday, false, "", false, isLab, true, periodNumbers, periodTimeRanges, title, detail);
        }
    }

    public int getSid() {
        return sid;
    }

    public boolean isLab() {
        return isLab;
    }

    public List<TimeTableDaySchema> getCachedDays() {
        return cachedDays;
    }

    public List<DayScheduleSchema> getCachedSchedules() {
        return cachedSchedules;
    }
}
