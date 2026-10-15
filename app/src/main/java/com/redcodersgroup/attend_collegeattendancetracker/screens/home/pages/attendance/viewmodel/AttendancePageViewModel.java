package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
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
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model.SubjectAttendanceModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendancePageViewModel extends AndroidViewModel {
    private final MediatorLiveData<List<SubjectAttendanceModel>> subjectAttendanceList = new MediatorLiveData<>();
    private final MediatorLiveData<List<CalendarDayModel>> calendarDays = new MediatorLiveData<>();
    private final MutableLiveData<Calendar> currentCalendarMonth = new MutableLiveData<>(Calendar.getInstance());

    public AttendancePageViewModel(@NonNull Application application) {
        super(application);
        setupMediators();
    }

    private void setupMediators() {
        Database db = Database.getInstance();
        LiveData<List<SubjectSchema>> completedSubjectsLiveData = db.getSubjectDao().getSetupCompletedSubjects();
        LiveData<List<AttendanceSchema>> allAttendanceLiveData = db.getAttendanceDao().getAllAttendance();
        LiveData<List<HolidaySchema>> holidaysLiveData = db.getHolidayDao().getHolidays();
        LiveData<UserSchema> userLiveData = db.getUserDao().getUserLiveData();

        subjectAttendanceList.addSource(completedSubjectsLiveData, subjects -> loadSubjectAttendanceData());
        subjectAttendanceList.addSource(allAttendanceLiveData, attendances -> {
            loadSubjectAttendanceData();
            loadCalendarDays();
        });
        subjectAttendanceList.addSource(userLiveData, user -> loadSubjectAttendanceData());

        calendarDays.addSource(holidaysLiveData, holidays -> loadCalendarDays());
        calendarDays.addSource(currentCalendarMonth, cal -> loadCalendarDays());
        calendarDays.addSource(userLiveData, user -> loadCalendarDays());
    }

    public void loadSubjectAttendanceData() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema user = db.getUserDao().getUser();
            int userCriteria = (user != null && user.getAttendanceCriteria() > 0) ? user.getAttendanceCriteria() : 75;

            List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

            List<SubjectSchema> completedSubjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
            List<SubjectAttendanceModel> result = new ArrayList<>();

            if (completedSubjects != null) {
                for (SubjectSchema subject : completedSubjects) {
                    boolean isLab = subject.isLab();
                    SubjectAdditionalInfoSchema additionalInfo = db.getSubjectAdditionalInfoDao().getSubjectAdditionalInfoBySid(subject.getSid());
                    int totalPlanned = (additionalInfo != null) ? (isLab && additionalInfo.getTotalLabs() > 0 ? additionalInfo.getTotalLabs() : additionalInfo.getTotalClasses()) : 0;

                    List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(subject.getSid(), isLab);
                    AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                            user, subject, isLab, records, holidays, days, schedules);

                    int attended = stats.getAttended();
                    int conducted = stats.getConducted();
                    int subjectCriteria = (subject.getAttendanceCriteria() > 0) ? subject.getAttendanceCriteria() : userCriteria;

                    result.add(new SubjectAttendanceModel(
                            subject,
                            subject.getSubjectName(),
                            isLab,
                            conducted,
                            attended,
                            totalPlanned,
                            subjectCriteria
                    ));
                }
            }
            subjectAttendanceList.postValue(result);
        });
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

    public void loadCalendarDays() {
        DbHelper.execute(() -> {
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

            UserSchema user = db.getUserDao().getUser();
            int workingDays = (user != null && user.getWorkingDays() > 0) ? user.getWorkingDays() : 6;

            List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();
            List<AttendanceSchema> allAttendances = db.getAttendanceDao().getAllAttendanceSync();

            Map<String, List<AttendanceSchema>> attendanceByDate = new HashMap<>();
            if (allAttendances != null) {
                for (AttendanceSchema att : allAttendances) {
                    if (att.getDate() != null) {
                        String rawDate = att.getDate().trim();
                        attendanceByDate.computeIfAbsent(rawDate, k -> new ArrayList<>()).add(att);
                        long ms = com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateMs(rawDate);
                        if (ms > 0) {
                            String normalized = com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateString(ms);
                            if (!normalized.equals(rawDate)) {
                                attendanceByDate.computeIfAbsent(normalized, k -> new ArrayList<>()).add(att);
                            }
                        }
                    }
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            List<CalendarDayModel> dayModels = new ArrayList<>();

            Calendar todayEndCal = Calendar.getInstance();
            todayEndCal.set(Calendar.HOUR_OF_DAY, 23);
            todayEndCal.set(Calendar.MINUTE, 59);
            todayEndCal.set(Calendar.SECOND, 59);
            todayEndCal.set(Calendar.MILLISECOND, 999);
            long todayEndMs = todayEndCal.getTimeInMillis();

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
                    boolean isFuture = dayMs > todayEndMs;

                    HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
                    boolean isHoliday = matchingHoliday != null;
                    String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

                    boolean hasAttendance = false;
                    int presentCount = 0;
                    int absentCount = 0;
                    int cancelledCount = 0;
                    if (!isFuture) {
                        List<AttendanceSchema> dayAtt = attendanceByDate.get(dateStr);
                        if (dayAtt != null && !dayAtt.isEmpty()) {
                            hasAttendance = true;
                            for (AttendanceSchema att : dayAtt) {
                                int periods = Math.max(1, AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, att.getSid(), days, schedules));
                                if (AttendanceConstants.isPresent(att.getStatus())) {
                                    presentCount += periods;
                                } else if (AttendanceConstants.isAbsent(att.getStatus())) {
                                    absentCount += periods;
                                } else if (AttendanceConstants.isCancelled(att.getStatus())) {
                                    cancelledCount += periods;
                                }
                            }
                        }
                    }

                    CalendarDayModel model = new CalendarDayModel(d, prevMonth, prevYear, dayMs, dateStr, false, false, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount);
                    model.setFuture(isFuture);
                    dayModels.add(model);
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
                boolean isFuture = dayMs > todayEndMs;

                HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
                boolean isHoliday = matchingHoliday != null;
                String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

                boolean hasAttendance = false;
                int presentCount = 0;
                int absentCount = 0;
                int cancelledCount = 0;
                if (!isFuture) {
                    List<AttendanceSchema> dayAtt = attendanceByDate.get(dateStr);
                    if (dayAtt != null && !dayAtt.isEmpty()) {
                        hasAttendance = true;
                        for (AttendanceSchema att : dayAtt) {
                            int periods = Math.max(1, AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, att.getSid(), days, schedules));
                            if (AttendanceConstants.isPresent(att.getStatus())) {
                                presentCount += periods;
                            } else if (AttendanceConstants.isAbsent(att.getStatus())) {
                                absentCount += periods;
                            } else if (AttendanceConstants.isCancelled(att.getStatus())) {
                                cancelledCount += periods;
                            }
                        }
                    }
                }

                CalendarDayModel model = new CalendarDayModel(d, month, year, dayMs, dateStr, true, isToday, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount);
                model.setFuture(isFuture);
                dayModels.add(model);
            }

            // 3. Trailing dates from next month to complete the 7-column row grid
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
                int nextDay = 1;

                while (dayModels.size() % 7 != 0) {
                    nextMonthCal.set(Calendar.DAY_OF_MONTH, nextDay);
                    long dayMs = nextMonthCal.getTimeInMillis();
                    String dateStr = sdf.format(nextMonthCal.getTime());
                    int dayOfWeek = nextMonthCal.get(Calendar.DAY_OF_WEEK);
                    boolean isWeekend = (dayOfWeek == Calendar.SUNDAY) || (workingDays <= 5 && dayOfWeek == Calendar.SATURDAY);
                    boolean isFuture = dayMs > todayEndMs;

                    HolidaySchema matchingHoliday = findMatchingHoliday(holidays, dayMs);
                    boolean isHoliday = matchingHoliday != null;
                    String holidayName = isHoliday ? matchingHoliday.getEventName() : "";

                    boolean hasAttendance = false;
                    int presentCount = 0;
                    int absentCount = 0;
                    int cancelledCount = 0;
                    if (!isFuture) {
                        List<AttendanceSchema> dayAtt = attendanceByDate.get(dateStr);
                        if (dayAtt != null && !dayAtt.isEmpty()) {
                            hasAttendance = true;
                            for (AttendanceSchema att : dayAtt) {
                                int periods = Math.max(1, AttendanceScheduleUtil.getScheduledPeriodsOnDate(dateStr, att.getSid(), days, schedules));
                                if (AttendanceConstants.isPresent(att.getStatus())) {
                                    presentCount += periods;
                                } else if (AttendanceConstants.isAbsent(att.getStatus())) {
                                    absentCount += periods;
                                } else if (AttendanceConstants.isCancelled(att.getStatus())) {
                                    cancelledCount += periods;
                                }
                            }
                        }
                    }

                    CalendarDayModel model = new CalendarDayModel(nextDay, nextMonth, nextYear, dayMs, dateStr, false, false, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, cancelledCount);
                    model.setFuture(isFuture);
                    dayModels.add(model);
                    nextDay++;
                }
            }

            calendarDays.postValue(dayModels);
        });
    }

    public void nextMonth() {
        Calendar current = currentCalendarMonth.getValue();
        if (current == null) current = Calendar.getInstance();
        Calendar next = (Calendar) current.clone();
        next.add(Calendar.MONTH, 1);
        currentCalendarMonth.setValue(next);
    }

    public void prevMonth() {
        Calendar current = currentCalendarMonth.getValue();
        if (current == null) current = Calendar.getInstance();
        Calendar prev = (Calendar) current.clone();
        prev.add(Calendar.MONTH, -1);
        currentCalendarMonth.setValue(prev);
    }

    public LiveData<List<SubjectSchema>> getPendingAttendanceSetUpSubjects() {
        return Database.getInstance().getSubjectDao().getAttendanceSetupPendingSubjects();
    }

    public LiveData<List<SubjectAttendanceModel>> getSubjectAttendanceList() {
        return subjectAttendanceList;
    }

    public LiveData<List<CalendarDayModel>> getCalendarDays() {
        return calendarDays;
    }

    public LiveData<Calendar> getCurrentCalendarMonth() {
        return currentCalendarMonth;
    }
}
