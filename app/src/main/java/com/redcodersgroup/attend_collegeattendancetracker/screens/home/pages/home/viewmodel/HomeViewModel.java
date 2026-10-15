package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
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
import com.redcodersgroup.attend_collegeattendancetracker.widget.AttendanceWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeViewModel extends AndroidViewModel {

    private final MediatorLiveData<List<TodayClassModel>> todayClasses = new MediatorLiveData<>();
    private final MediatorLiveData<AssignmentSchema> upcomingAssignment = new MediatorLiveData<>();
    private final MutableLiveData<Integer> upcomingAssignmentsCount = new MutableLiveData<>(0);
    private final MutableLiveData<UserSchema> user = new MutableLiveData<>();
    private final MutableLiveData<Integer> overallPercentage = new MutableLiveData<>(100);
    private final MutableLiveData<String> bunkStatusText = new MutableLiveData<>("On track");
    private final MutableLiveData<Integer> subjectCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> bunkThisWeek = new MutableLiveData<>(0);
    private final MutableLiveData<String> nextHoliday = new MutableLiveData<>("None");
    private final MutableLiveData<String> todayHolidayEventName = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> profileComplete = new MutableLiveData<>(false);
    private final MutableLiveData<AttendanceScheduleUtil.HeroRiskSummary> heroRiskSummary = new MutableLiveData<>();

    public LiveData<String> getTodayHolidayEventName() {
        return todayHolidayEventName;
    }

    public HomeViewModel(@NonNull Application application) {
        super(application);
        setupObservers();
    }

    private void setupObservers() {
        Database db = Database.getInstance();
        LiveData<List<SubjectSchema>> subjectsLiveData = db.getSubjectDao().getSetupCompletedSubjects();
        LiveData<List<AttendanceSchema>> attendanceLiveData = db.getAttendanceDao().getAllAttendance();
        LiveData<List<AssignmentSchema>> assignmentsLiveData = db.getAssignmentDao().getAssignments();
        LiveData<List<HolidaySchema>> holidaysLiveData = db.getHolidayDao().getHolidays();
        LiveData<UserSchema> userLiveData = db.getUserDao().getUserLiveData();

        todayClasses.addSource(subjectsLiveData, s -> loadAllData());
        todayClasses.addSource(attendanceLiveData, a -> loadAllData());
        todayClasses.addSource(assignmentsLiveData, a -> loadAllData());
        todayClasses.addSource(holidaysLiveData, h -> loadAllData());
        todayClasses.addSource(userLiveData, u -> loadAllData());

        loadAllData();
    }

    public void loadAllData() {
        DbHelper.execute(() -> {
            Database db = Database.getInstance();
            UserSchema currentUser = db.getUserDao().getUser();
            user.postValue(currentUser);
            boolean isComplete = com.redcodersgroup.attend_collegeattendancetracker.utils.ProfileUtil.isProfileCompletedBareMinimum(currentUser);
            com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil.setProfileComplete(isComplete);
            profileComplete.postValue(isComplete);

            int userCriteria = (currentUser != null && currentUser.getAttendanceCriteria() > 0)
                    ? currentUser.getAttendanceCriteria() : 75;

            List<SubjectSchema> subjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
            List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
            List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
            List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();
            List<AttendanceSchema> allAttendances = db.getAttendanceDao().getAllAttendanceSync();
            List<AssignmentSchema> allAssignments = db.getAssignmentDao().getAssignmentsSync();

            // 1. Today's classes and labs
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

            long now = System.currentTimeMillis();
            HolidaySchema todayHoliday = null;
            if (holidays != null) {
                for (HolidaySchema h : holidays) {
                    if (h != null && AttendanceScheduleUtil.isHoliday(Collections.singletonList(h), now)) {
                        todayHoliday = h;
                        break;
                    }
                }
            }
            todayHolidayEventName.postValue(todayHoliday != null ? todayHoliday.getEventName() : null);

            List<TodayClassModel> todayClassList = new ArrayList<>();
            if (todayHoliday == null && todaySchedule != null) {
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

                            todayClassList.add(new TodayClassModel(
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
            todayClasses.postValue(todayClassList);

            // 2. Upcoming Assignment
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayStartMs = cal.getTimeInMillis();

            AssignmentSchema upcoming = null;
            int upcomingCount = 0;

            if (allAssignments != null && !allAssignments.isEmpty()) {
                List<AssignmentSchema> sortedAssignments = new ArrayList<>(allAssignments);
                Collections.sort(sortedAssignments, Comparator.comparingLong(AssignmentSchema::getDueDate));

                List<AssignmentSchema> futureUnsubmitted = new ArrayList<>();
                List<AssignmentSchema> allUnsubmitted = new ArrayList<>();

                for (AssignmentSchema a : sortedAssignments) {
                    if (!a.isSubmitted()) {
                        allUnsubmitted.add(a);
                        if (a.getDueDate() >= todayStartMs) {
                            futureUnsubmitted.add(a);
                        }
                    }
                }

                upcomingCount = futureUnsubmitted.size();

                if (!futureUnsubmitted.isEmpty()) {
                    upcoming = futureUnsubmitted.get(0);
                } else if (!allUnsubmitted.isEmpty()) {
                    upcoming = allUnsubmitted.get(0);
                } else {
                    // All submitted: pick the closest future or latest assignment
                    for (AssignmentSchema a : sortedAssignments) {
                        if (a.getDueDate() >= todayStartMs) {
                            upcoming = a;
                            break;
                        }
                    }
                    if (upcoming == null) {
                        upcoming = sortedAssignments.get(sortedAssignments.size() - 1);
                    }
                }
            }

            upcomingAssignment.postValue(upcoming);
            upcomingAssignmentsCount.postValue(upcomingCount);

            // 3. Overall Attendance & Bunk status
            int totalAttended = 0;
            int totalConducted = 0;
            int totalRemaining = 0;

            if (subjects != null) {
                for (SubjectSchema subject : subjects) {
                    boolean isLab = subject.isLab();
                    List<AttendanceSchema> records = db.getAttendanceDao().getAttendanceBySidAndLabSync(subject.getSid(), isLab);
                    AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                            currentUser, subject, isLab, records, holidays, days, schedules);
                    int remaining = AttendanceScheduleUtil.calculateRemainingScheduledClasses(
                            currentUser, subject, isLab, holidays, days, schedules);

                    totalAttended += stats.getAttended();
                    totalConducted += stats.getConducted();
                    totalRemaining += remaining;
                }
            }

            int subCount = subjects != null ? subjects.size() : 0;
            AttendanceScheduleUtil.HeroRiskSummary summary = AttendanceScheduleUtil.calculateHeroRiskSummary(
                    currentUser, subCount, totalAttended, totalConducted, totalRemaining);

            overallPercentage.postValue(summary.getOverallPercentage());
            bunkStatusText.postValue(summary.getHeadlineText());
            heroRiskSummary.postValue(summary);

            // 4. Stats: Subject count, Bunked this week, Next holiday
            subjectCount.postValue(subCount);

            // Bunked this week
            Calendar weekCal = Calendar.getInstance();
            weekCal.set(Calendar.HOUR_OF_DAY, 0);
            weekCal.set(Calendar.MINUTE, 0);
            weekCal.set(Calendar.SECOND, 0);
            weekCal.set(Calendar.MILLISECOND, 0);

            int currentDayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon...
            int daysFromMonday = (currentDayOfWeek == Calendar.SUNDAY) ? 6 : (currentDayOfWeek - Calendar.MONDAY);
            weekCal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
            long weekStartMs = weekCal.getTimeInMillis();

            Calendar endCal = (Calendar) weekCal.clone();
            endCal.add(Calendar.DAY_OF_YEAR, 7);
            long weekEndMs = endCal.getTimeInMillis();
            int bunkThisWeekCount = 0;
            if (allAttendances != null) {
                for (AttendanceSchema att : allAttendances) {
                    if (att != null && att.getDate() != null && com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants.isAbsent(att.getStatus())) {
                        long attTime = com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil.getDateMs(att.getDate());
                        if (attTime >= weekStartMs && attTime < weekEndMs) {
                            if (subjectMap.isEmpty() || subjectMap.containsKey(att.getSid())) {
                                bunkThisWeekCount++;
                            }
                        }
                    }
                }
            }
            bunkThisWeek.postValue(bunkThisWeekCount);

            // Next holiday
            String nextHolidayStr = "None";
            if (holidays != null && !holidays.isEmpty()) {
                List<HolidaySchema> sortedHolidays = new ArrayList<>(holidays);
                Collections.sort(sortedHolidays, Comparator.comparingLong(HolidaySchema::getStartDate));
                for (HolidaySchema h : sortedHolidays) {
                    if (todayStartMs >= h.getStartDate() && todayStartMs <= h.getEndDate()) {
                        nextHolidayStr = "Today";
                        break;
                    } else if (h.getStartDate() >= todayStartMs) {
                        Calendar hStart = Calendar.getInstance();
                        hStart.setTimeInMillis(h.getStartDate());
                        nextHolidayStr = new SimpleDateFormat("MMM dd", Locale.US).format(hStart.getTime());
                        break;
                    }
                }
            }
            nextHoliday.postValue(nextHolidayStr);
            AttendanceWidgetProvider.refreshAllWidgets(getApplication());
        });
    }

    public void markAttendance(TodayClassModel model, String targetStatus) {
        // Attendance records are read-only and synced from college ERP
    }

    public void markAllPresent() {
        // Attendance records are read-only and synced from college ERP
    }

    public LiveData<List<TodayClassModel>> getTodayClasses() {
        return todayClasses;
    }

    public LiveData<AssignmentSchema> getUpcomingAssignment() {
        return upcomingAssignment;
    }

    public LiveData<Integer> getUpcomingAssignmentsCount() {
        return upcomingAssignmentsCount;
    }

    public LiveData<UserSchema> getUser() {
        return user;
    }

    public LiveData<Integer> getOverallPercentage() {
        return overallPercentage;
    }

    public LiveData<String> getBunkStatusText() {
        return bunkStatusText;
    }

    public LiveData<Integer> getSubjectCount() {
        return subjectCount;
    }

    public LiveData<Integer> getBunkThisWeek() {
        return bunkThisWeek;
    }

    public LiveData<String> getNextHoliday() {
        return nextHoliday;
    }

    public LiveData<Boolean> getIsProfileComplete() {
        return profileComplete;
    }

    public LiveData<AttendanceScheduleUtil.HeroRiskSummary> getHeroRiskSummary() {
        return heroRiskSummary;
    }
}
